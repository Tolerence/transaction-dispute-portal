# Common tasks. Run `make help` for the list.
SHELL := /bin/bash
.DEFAULT_GOAL := help

COMPOSE := docker compose
COMPOSE_SSO := docker compose -f docker-compose.yml -f docker-compose.sso.yml

.PHONY: help
help: ## List available targets
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "} {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

.PHONY: env
env: ## Create .env and generate local secrets (never overwrites values)
	./scripts/bootstrap-env.sh

.PHONY: up
up: env ## Start the local stack: database, API (dev profile) and web app, on http://localhost:8080
	$(COMPOSE) up -d --build --wait

.PHONY: up-sso
up-sso: env ## Start the stack with a local Keycloak and real OIDC validation (demo profile), on http://localhost:8080
	$(COMPOSE_SSO) up -d --build --wait

.PHONY: down
down: ## Stop the stack (keeps data)
	$(COMPOSE_SSO) down

.PHONY: reset
reset: ## Stop the stack and delete its database volume
	$(COMPOSE_SSO) down -v

.PHONY: logs
logs: ## Follow logs for all services
	$(COMPOSE_SSO) logs -f

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

.PHONY: check
check: test test-api ## Everything: unit, integration, and API tests (stack must be running)
