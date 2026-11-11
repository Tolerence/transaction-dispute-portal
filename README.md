# Transaction Disputes Portal

Customers view their transactions, raise a dispute on a transaction, and review the full history of their
disputes, including closed and withdrawn cases. Each dispute has an audit timeline of every status change.

| Part | Technology |
| --- | --- |
| API (`backend/`) | Java 25, Spring Boot 3.5, Spring Security (OAuth2 resource server, OIDC), Spring Data JPA, Flyway, PostgreSQL |
| Web app (`frontend/`) | React 18, TypeScript, Vite, React Router, OIDC sign-in with PKCE |
| Identity (local only, `infra/keycloak/`) | Keycloak 26 with a pre-configured realm |
| Packaging | Docker (multi-stage, non-root, layered), Docker Compose, nginx |
| Quality gates | JUnit 5, AssertJ, Mockito, Vitest, Bruno API tests, GitHub Actions, Trivy scan |
| API documentation | OpenAPI 3 with Swagger UI (dev and demo profiles) |

---

## 1. Prerequisites

| Tool | Version | Used for |
| --- | --- | --- |
| Docker (Desktop or Engine with Compose v2) | recent | Running the stack |
| JDK | 25 | `make build` compiles the API on your machine |
| Maven | 3.9+ | `make build` and `make test-backend` |
| Node.js | 22 | `make build` builds the web app; also Bruno CLI |
| openssl, perl | any | `make env` generates secrets |

Check your setup:

```bash
docker --version && docker compose version
java -version        # expect 25.x
mvn -v | head -1     # expect 3.9+
node -v              # expect v22.x
```

---

## 2. Quick start

From the repository root:

```bash
make up
```

This command:

1. creates `.env` from `.env.example` and generates the database password, JWT signing key, and Keycloak admin password (existing values are never overwritten);
2. runs `make build`, which compiles the API jar and the web bundle on your machine (Maven and npm);
3. builds the Docker images from those artifacts and starts PostgreSQL, the API, and the web app, and waits until all are healthy.

Open **http://localhost:8080** and choose **Sign in as demo customer (local only)**. The dev profile seeds eight transactions and three historical disputes for the demo customer.

To run with a real OIDC provider locally (recommended before you change authentication code):

```bash
make up-sso
```

This also starts Keycloak on **http://localhost:8180**. Open http://localhost:8080 and choose **Sign in**. Log in as `demo.customer` / `demo-password`.

Stop the stack with `make down` (keeps data) or `make reset` (deletes the database).

---

## 3. Build

Docker images do not compile anything. The API and the web app are built on your machine, then packaged into images. This keeps Maven and npm downloads on your own network, where your trust settings apply, and keeps the project free of certificates.

| Artifact | Command | Output |
| --- | --- | --- |
| Backend jar and web bundle | `make build` | `backend/target/dispute-portal-backend-0.1.0.jar` and `frontend/dist/` |
| Backend image | `docker compose build backend` (after `make build`) | Java 25 JRE, non-root user, layered jar, health check |
| Frontend image | `docker compose build frontend` (after `make build`) | `dist/` served by unprivileged nginx on port 8080 |
| Full stack | `make up` (runs `make build`, then Compose) | All services |

The web settings (`VITE_*` in `.env`) are compiled into the bundle. After changing them, run `make build` again before `make up`.

---

## 4. Test

| Layer | Command | What it covers |
| --- | --- | --- |
| Backend unit | `make test-backend` (or `cd backend && mvn verify`) | Dispute eligibility rules, status state machine, rate limiter, OIDC configuration fail-fast, role mapping from tokens |
| Backend integration | (included above) | REST API against H2 with real Flyway migrations, JWT validation (signature, expiry, audience), problem-detail errors, review workflow, role enforcement, self-review ban, concurrent-safe decisions, customer identity masking |
| Frontend | `make test-frontend` | Form validation, formatting, PKCE challenge against the RFC 7636 example, type-check, production build |
| API (dev stack) | `make up` then `make test-api` | 25 requests end to end: sign-in, list, filter, detail, submit, duplicate rule, validation, cross-customer isolation, history, withdraw, the full review lifecycle, role enforcement, and actor masking |
| API (real OIDC) | `make up-sso` then `make test-api-sso` | Same collection with tokens issued by Keycloak (customer and reviewer), validated by the API against the realm's keys |
| Everything | `make check` | Runs all of the above (stack must be running for the API layer) |

Tests are repeatable: the Bruno collection withdraws the dispute it creates. If a run is interrupted, reset with `make reset && make up`.

Test files live beside the code: `backend/src/test/java`, `frontend/src/**/*.test.ts`, and `bruno/`.

---

## 5. Run in each mode

| Mode | How | Authentication | Data | Use for |
| --- | --- | --- | --- | --- |
| **dev** (default) | `make up` | Local HS256 tokens from `POST /api/dev/token` | Demo data | Day-to-day development |
| **demo** (SSO) | `make up-sso` | Real OIDC tokens from Keycloak, validated against its keys | Demo data | Testing real authentication and the PKCE sign-in flow |
| **production** | Deploy the image with production configuration (section 6) | Your identity provider | Your data | Live service |

The profiles behave differently on purpose:

- **dev** enables `/api/dev/token` and a local signing key. Never deploy it.
- **demo** validates real tokens but ships a public demo user. Never deploy it.
- **production** (no profile) has neither, and refuses to start without `OIDC_ISSUER_URI`.

### Demo credentials (public, local only)

The values below are committed to this repository on purpose. They exist so anyone can run the portal locally and in tests. They are not secrets, and they must never protect a real system.

| Where | Value | Purpose |
| --- | --- | --- |
| Dev database fallback (`application-dev.yml`) | user `dispute`, password `dispute` | Used only when `DB_PASSWORD` is unset. `make up` sets a generated one in `.env` |
| Dev JWT fallback (`application-dev.yml`) | `local-development-only-secret-key-change-me-32b` | Used only when `JWT_SECRET` is unset. `make env` generates one in `.env` |
| Keycloak demo users (`infra/keycloak/README.md`) | `demo.customer` / `demo-password`, `ops.reviewer` / `reviewer-password` | Sign-in in the demo (SSO) profile and the Bruno tests |
| Keycloak test client (`dispute-portal-tests`) | Resource owner password grant | Used only by the Bruno API tests |

Rules:

- **Only the values above are public.** Real secrets are generated into `.env` by `make env` (database password, JWT secret, Keycloak admin password). `.env` is git-ignored and must never be committed or copied to a server.
- **Production uses none of these.** The production profile has no dev token endpoint, no demo users, and refuses to start without `OIDC_ISSUER_URI`. Remove the Keycloak test client and demo users from any production realm.
- **Changing a demo value** is only needed if you expose the local stack beyond your laptop, which you should not do.

---

## 6. Configuration

Settings come from environment variables. Secrets are never stored in the image.

### API

| Variable | Required | Default | Purpose |
| --- | --- | --- | --- |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | yes | none | PostgreSQL connection |
| `OIDC_ISSUER_URI` | yes (outside dev) | none | Issuer exactly as it appears in token `iss` claims |
| `OIDC_JWK_SET_URI` | no | discovered from issuer | Where to fetch signing keys when the API reaches the provider on another address (typical in containers) |
| `APP_AUDIENCE` | no | `dispute-portal-api` | Tokens must list this value in `aud` |
| `CORS_ALLOWED_ORIGINS` | only for cross-origin browsers | empty | Comma-separated origins. Empty when the web app is served from the same origin |
| `TRUST_FORWARDED_FOR` | no | `false` | Use `X-Forwarded-For` for rate limiting. Enable only behind a proxy you control |
| `RATE_LIMIT_API_PER_MINUTE` | no | `120` | Per client address, all `/api/**` |
| `RATE_LIMIT_DISPUTE_PER_MINUTE` | no | `10` | Per client address, dispute submissions |
| `PORT` | no | `8080` | HTTP port |
| `SPRING_PROFILES_ACTIVE` | no | empty | `dev` or `demo` only in local environments |
| `JWT_SECRET` | `dev` only | none | Local HS256 signing key, at least 32 bytes |

### Web app (build-time, public values)

| Variable | Purpose |
| --- | --- |
| `VITE_OIDC_ISSUER` | Identity provider issuer. Enables **Sign in** |
| `VITE_OIDC_CLIENT_ID` | Public client ID, default `dispute-portal-web` |
| `VITE_DEV_LOGIN` | `true` shows the local demo button (dev profile only) |

### Containers

| Variable | Purpose |
| --- | --- |
| `OIDC_ORIGIN` | Identity provider origin allowed in the content security policy |

Every variable is listed in `.env.example`.

---

## 7. Deploy to production

Checklist before the first production deployment. Items marked **bank** need your organisation's process.

1. **Identity:** point `OIDC_ISSUER_URI` at the bank's provider. Register `dispute-portal-web` as a public client with PKCE (S256) and the redirect URI for the web app. Make sure the provider adds `dispute-portal-api` to the `aud` claim. Remove the Keycloak test client from the production realm.
2. **Secrets:** inject `DB_PASSWORD` from your secret store. Do not use `.env` on servers.
3. **Profiles:** leave `SPRING_PROFILES_ACTIVE` empty. Do not set `dev` or `demo`.
4. **Database:** run Flyway migrations from the release pipeline with a least-privilege migration user, and run the application with a separate user limited to DML.
5. **TLS:** terminate TLS at the ingress or load balancer. Set `TRUST_FORWARDED_FOR=true` only if that proxy overwrites the header.
6. **Images:** pin base images by digest, sign images, and deploy only images that pass the Trivy scan in CI.
7. **Observability:** ship the JSON logs (ECS) to your log platform. Scrape `/actuator/prometheus` from inside the cluster only. Alert on `http.server.requests` error rates and on 429 volume.
8. **Rate limits:** the per-instance limits are a backstop. Add shared limits at the gateway (**bank**).
9. **Data:** agree retention for dispute descriptions, which can contain personal data (**bank**, privacy impact assessment).
10. **Security review:** penetration test and threat model before go-live (**bank**).
11. **Operations:** the review API is built and tested, but reviewers have no screens yet. Reviewer work currently happens through the API (or Bruno). A reviewer UI needs its own browser test pass.

---

## 8. Architecture

```
Browser ──HTTPS──► nginx (web app + /api proxy) ──► API (Spring Boot) ──► PostgreSQL
   │                                                  │
   └── OIDC (PKCE) ──► Identity provider ◄── JWKS ────┘
```

- **Authentication:** the web app runs the authorization code flow with PKCE and keeps the access token in session storage. The API validates each token's signature, expiry, issuer, and audience. The `sub` claim is the customer ID. Every query is scoped to that customer.
- **Disputes:** a dispute is allowed when the transaction is posted, inside the 120-day window, and has no open dispute. The transaction row is locked while the dispute is created, so two requests cannot both pass the check. Status changes follow a fixed state machine and are written to an append-only event table.
- **Errors:** every error is an RFC 9457 problem response. Validation errors include a field map.
- **Operations:** request IDs (`X-Request-Id`) are echoed in responses and logs. Rate limits protect the API from floods, including unauthenticated ones.

Key files:

| Concern | Location |
| --- | --- |
| Eligibility rules | `backend/.../disputes/DisputePolicy.java` |
| Status state machine | `backend/.../disputes/DisputeStatus.java`, `Dispute.transitionTo` |
| Token validation | `backend/.../config/OidcJwtConfig.java`, `AudienceValidator.java` |
| Rate limiting | `backend/.../web/RateLimitFilter.java` |
| Schema | `backend/src/main/resources/db/migration/` |
| Sign-in flow | `frontend/src/auth/oidc.ts`, `CallbackPage.tsx` |
| Demo identity | `infra/keycloak/README.md`, `db/dev/V100__dev_seed.sql` |

---

## 9. API reference

All endpoints except `/actuator/health/**` require `Authorization: Bearer <token>`. `/api/review/**` also requires the `DISPUTE_REVIEWER` role. Roles come from the token's `realm_access.roles` (Keycloak) or `roles` claim (dev).

### Operations review screens

Users whose token carries the `DISPUTE_REVIEWER` role see **Review queue** in the navigation. The queue lists every customer's disputes, oldest first, with the customer ID. A reviewer opens a dispute, starts the review, and then accepts or rejects it with a note. The customer sees the outcome and the note, but not the reviewer's identity.

The screens only hide or show options for convenience. The API enforces the role and the self-review ban on every request.

### Operations review

Reviewers work the queue and make decisions. The lifecycle is `SUBMITTED` → `UNDER_REVIEW` → `ACCEPTED` or `REJECTED`. The customer can withdraw only while the dispute is open.

Controls:
- **Role required:** customers receive 403 on every review endpoint.
- **No self-review:** a reviewer cannot act on a dispute raised by their own customer ID (403).
- **Serialised decisions:** the dispute row is locked while a decision is written, and the version check rejects stale updates (409).
- **Audit trail:** every review action is logged to the `audit.dispute-review` logger and stored in the dispute timeline with the reviewer's ID. Customers see reviewer actions as `BANK`, so reviewer identities are never exposed to customers.
- **Customer-facing notes:** a decision note is shown to the customer. Write it as customer-safe text.

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/transactions` | Paged list. Query: `page`, `size` (max 100), `status`, `from`, `to` (ISO dates, UTC, inclusive), `sort` |
| GET | `/api/transactions/{id}` | One transaction, with `disputable` and `ineligibilityReason` |
| POST | `/api/transactions/{id}/disputes` | Raise a dispute. Body: `{ "reason", "description" }`. Returns 201 with a `Location` header |
| GET | `/api/disputes` | Dispute history. Query: `page`, `size`, `status`. Includes closed disputes |
| GET | `/api/disputes/{id}` | Dispute detail with the event timeline |
| POST | `/api/disputes/{id}/withdraw` | Withdraw an open dispute |
| GET | `/api/review/disputes` | **Reviewer.** Queue of all customers' disputes, oldest first. Filter: `status`, `page`, `size` |
| GET | `/api/review/disputes/{id}` | **Reviewer.** Dispute with customer ID and full timeline, including reviewer identities |
| POST | `/api/review/disputes/{id}/start` | **Reviewer.** `SUBMITTED` to `UNDER_REVIEW` |
| POST | `/api/review/disputes/{id}/decision` | **Reviewer.** Body: `{ "decision": "ACCEPT" or "REJECT", "note" }`. Note is 10 to 500 characters and is shown to the customer. Allowed only from `UNDER_REVIEW` |
| POST | `/api/dev/token` | **dev profile only.** Local token for a customer ID, optionally with `roles` (for example `DISPUTE_REVIEWER`) |
| GET | `/actuator/health/readiness` | Readiness probe |
| GET | `/actuator/prometheus` | Metrics (restrict at the network layer) |
| GET | `/v3/api-docs`, `/swagger-ui/index.html` | OpenAPI document and Swagger UI. **dev and demo profiles only** (404 in production) |

Dispute reasons: `UNAUTHORISED_TRANSACTION`, `DUPLICATE_CHARGE`, `INCORRECT_AMOUNT`, `NOT_AS_DESCRIBED`, `SERVICE_CANCELLED`, `OTHER`.

---

## 10. Troubleshooting

| Symptom | Cause | Fix |
| --- | --- | --- |
| `Required variable ... is missing` | `.env` is incomplete | `make env` |
| API returns 401 after `make up-sso` | Stack started without `demo` profile, so the API validates against local keys | `make down`, then `make up-sso` |
| Sign-in redirects but the page says the response could not be verified | Browser cleared session storage during redirect, or two tabs signed in | Try again in one tab |
| API tests fail with 422 on the first dispute | An earlier run was interrupted and left a dispute open | `make reset && make up` |
| `Application failed to start` mentioning `OIDC_ISSUER_URI` | Production profile without an issuer | Set `OIDC_ISSUER_URI`, or use `SPRING_PROFILES_ACTIVE=dev` locally |
| Sign-in works but every API call returns 401 | The API was started without the demo profile, so it checks tokens against the local dev key | Use `make up-sso`, which sets the profile in `docker-compose.sso.yml` |
| Dispute submission fails with 403 "Invalid CORS request" | The web origin is not in `CORS_ALLOWED_ORIGINS`. Browsers send `Origin` even on same-origin POSTs | Add the origin to `.env` (local default covers `localhost:8080` and `localhost:5173`) |
| Browser still shows an old version after a rebuild | Cached JavaScript bundle | Reload with cache disabled, or open a new tab |
| Docker build fails with `not found` for `dispute-portal-backend-*.jar` or `dist/` | `make build` has not run since the last clean, or it failed | Run `make build` and fix any Maven or npm error it prints |
| `make build` fails with `PKIX path building failed` | Your network inspects HTTPS and its root certificate is not in your machine's trust store | Install the root in your system keychain, or build on a network without inspection. The project does not carry certificates |
| Port 8080, 8180, or 5432 already in use | Another process | Stop it, or change the port mapping in `docker-compose.yml` |

View logs with `make logs`. Log lines are JSON; search by `requestId` to follow one request across services.

---

## 11. Repository layout

```
backend/              Spring Boot API (Maven, Dockerfile)
frontend/             React web app (Vite, Dockerfile, nginx template)
bruno/                API test collection (Local, Docker, and Sso environments)
infra/keycloak/       Local identity provider realm
scripts/              bootstrap-env.sh
.github/workflows/    CI: backend, frontend, containers and API tests, dependency and secret scan
docker-compose.yml    Stack definition (dev)
docker-compose.sso.yml  Adds Keycloak and the demo profile (SSO)
Makefile              Common tasks (`make help`)
.env.example          Every configurable variable
```
