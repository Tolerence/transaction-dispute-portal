# Common tasks. Run `make help` for the list.
SHELL := /bin/bash
.DEFAULT_GOAL := help

COMPOSE := docker compose
COMPOSE_SSO := docker compose -f docker-compose.yml -f docker-compose.sso.yml

# Web app settings are compiled into the bundle, so the build reads them from .env (same defaults as compose).
-include .env
VITE_OIDC_ISSUER ?= http://localhost:8180/realms/dispute-portal
VITE_OIDC_CLIENT_ID ?= dispute-portal-web
VITE_DEV_LOGIN ?= false
export VITE_OIDC_ISSUER VITE_OIDC_CLIENT_ID VITE_DEV_LOGIN

.PHONY: help
help: ## List available targets
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "} {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

.PHONY: env
env: ## Create .env and generate local secrets (never overwrites values)
	./scripts/bootstrap-env.sh

.PHONY: build
build: ## Build the API jar and web bundle on this machine (Maven and npm; no Docker, no certificates)
	cd backend && mvn -B -DskipTests package
	cd frontend && npm ci --no-audit --no-fund && npm run build

# Sub-makes re-read .env, which `env` may have just created.
.PHONY: up
up: env ## Start the local stack: database, API (dev profile) and web app, on http://localhost:8080
	$(MAKE) build
	$(COMPOSE) up -d --build --wait

.PHONY: up-sso
up-sso: env ## Start the stack with a local Keycloak and real OIDC validation (demo profile), on http://localhost:8080
	# The SSO API has no dev token endpoint, so the local-only demo buttons are left out of this build.
	# The two sign-in buttons pre-fill the local Keycloak demo users (see infra/keycloak/README.md).
	$(MAKE) build VITE_DEV_LOGIN=false VITE_SSO_CUSTOMER_HINT=demo.customer VITE_SSO_REVIEWER_HINT=ops.reviewer
	$(COMPOSE_SSO) up -d --build --wait

.PHONY: up-monitoring
up-monitoring: env ## Start the stack plus Grafana, Prometheus, Loki and Alloy (Grafana on http://127.0.0.1:3000)
	$(MAKE) build
	$(COMPOSE) --profile monitoring up -d --build --wait

.PHONY: down
down: ## Stop the stack and any monitoring containers (keeps data)
	$(COMPOSE_SSO) --profile monitoring down

.PHONY: reset
reset: ## Stop the stack and delete all its volumes, including monitoring data
	$(COMPOSE_SSO) --profile monitoring down -v

.PHONY: logs
logs: ## Follow logs for all services
	$(COMPOSE_SSO) --profile monitoring logs -f

.PHONY: test
test: test-backend test-frontend ## Unit and integration tests for backend and frontend

.PHONY: test-backend
test-backend: ## Backend build, unit and integration tests (Maven, Java 25)
	cd backend && mvn -B verify

.PHONY: test-frontend
test-frontend: ## Frontend type-check, unit tests and production build
	cd frontend && npm ci --no-audit --no-fund && npm run build && npm test

.PHONY: test-api
test-api: ## API tests against the running dev stack (Bruno)
	./scripts/reset-api-test-state.sh && cd bruno && npx --yes @usebruno/cli@latest run --env Docker -r

.PHONY: test-api-sso
test-api-sso: ## API tests against the running SSO stack (Bruno, real OIDC tokens)
	cd bruno && npx --yes @usebruno/cli@latest run --env Sso -r

.PHONY: backup
backup: ## Dump the database to backups/ (gzipped SQL; the stack must be running)
	./scripts/backup-db.sh

.PHONY: restore-drill
restore-drill: ## Restore the newest backup into a scratch database and compare row counts
	./scripts/restore-drill.sh

.PHONY: check
check: test test-api ## Everything: unit, integration, and API tests (stack must be running)
