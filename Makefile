# Root orchestration for the monorepo. Per-app detail stays in frontend/ and
# backend/; this file only composes their existing entry points.
#
#   make bootstrap          install frontend deps, prepare backend tooling
#   make infra-up           start Postgres + Redis
#   make infra-down         stop them
#   make dev                run backend + frontend together
#   make verify-frontend    the frontend app's own baseline gate
#   make verify-backend     the backend app's own baseline gate
#   make verify             both gates, serially
#   make integration-test   deps + both apps + Playwright
#   make package            build the SPA into the Spring Boot JAR
#   make container          build the deployable integrated image
#
# Toolchain versions are pinned in /.nvmrc, /.tool-versions,
# frontend/package.json and backend/.mvn/wrapper/maven-wrapper.properties;
# scripts/lib.sh checks the active Node/npm/JDK against them before doing work.
# Activate them from the repo root with 'nvm use' / 'mise install'.

SHELL := /usr/bin/env bash
.SHELLFLAGS := -euo pipefail -c
.DEFAULT_GOAL := help
# Every gate here shells out to a tool that already parallelises internally, and
# `verify` is specified as serial — so never let -j interleave targets.
.NOTPARALLEL:

NPM ?= npm
# The checked-in wrapper, not whatever `mvn` happens to be on PATH: it downloads
# and checksum-verifies exactly the Maven release pinned in
# backend/.mvn/wrapper/maven-wrapper.properties. Absolute, because it is both
# run from backend/ here and exported to scripts/ that cd elsewhere.
MVN ?= $(CURDIR)/backend/mvnw
COMPOSE ?= docker compose -f backend/compose.yaml
IMAGE ?= monorepo-base
TAG ?= local

export NPM
export MVN

.PHONY: help bootstrap infra-up infra-down infra-logs dev dev-stop \
        verify-frontend verify-backend verify integration-test package container clean

help: ## Show the available targets
	@grep -hE '^[a-z][a-z-]*:.*?## ' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "} {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

bootstrap: ## Install frontend dependencies and prepare backend tooling
	@scripts/bootstrap.sh

infra-up: ## Start PostgreSQL and Redis, waiting until both are healthy
	$(COMPOSE) up -d --wait

infra-down: ## Stop PostgreSQL and Redis (volumes are kept)
	$(COMPOSE) down

infra-logs: ## Tail the dependency logs
	$(COMPOSE) logs -f

dev: ## Run backend and frontend together (Ctrl-C stops both)
	@scripts/dev.sh

dev-stop: ## Kill leftover backend/Vite processes from an earlier dev run
	@scripts/dev-stop.sh

# Each app owns its own baseline gate; these targets only invoke it, so the
# sub-gate list lives in one place per app and cannot drift from the app's docs.
# Serial on purpose: each app's failure should be the thing that stops the run.
verify-frontend: ## Frontend gate: frontend/ baseline (npm run verify)
	cd frontend && $(NPM) run verify

verify-backend: ## Backend gate: backend/ baseline (scripts/verify.sh)
	cd backend && ./scripts/verify.sh

verify: verify-frontend verify-backend ## Run both app gates, serially

integration-test: ## Start dependencies + both apps, then run Playwright
	@scripts/integration-test.sh

package: ## Build the SPA and package it into the Spring Boot JAR
	@scripts/package.sh

container: package ## Build the deployable integrated image
	cd backend && docker build -t $(IMAGE):$(TAG) .
	@echo "built $(IMAGE):$(TAG) — run with: docker run --rm -p 8080:8080 --env-file backend/.env $(IMAGE):$(TAG)"

clean: ## Remove build output from both apps
	cd backend && $(MVN) -q -B clean
	rm -rf frontend/dist frontend/coverage frontend/playwright-report frontend/test-results
