# Local identity provider (Keycloak)

`realm-dispute-portal.json` is imported when the `sso` profile is started. It is for **local testing only**:

- the demo user's password is public in this repository and must never be used outside a laptop;
- `sslRequired` is `none`, and `start-dev` mode is used.

Realm contents:

| Client | Purpose | Flows |
| --- | --- | --- |
| `dispute-portal-web` | Browser SPA | Authorization code with PKCE (S256) only |
| `dispute-portal-tests` | Bruno and CI API tests | Resource owner password (test use only) |

Both clients add the `dispute-portal-api` audience to access tokens, which the API requires.

Demo users:

| Username | Password | Subject | Role | Purpose |
| --- | --- | --- | --- | --- |
| `demo.customer` | `demo-password` | `6f1c2b7e-4d1a-4c8e-9a2b-000000000001` | customer | Owns the seeded demo transactions |
| `ops.reviewer` | `reviewer-password` | `6f1c2b7e-4d1a-4c8e-9a2b-000000000003` | `DISPUTE_REVIEWER` | Reviews and decides disputes |

For a real deployment, replace this realm with the bank's identity provider configuration and remove the test client.
