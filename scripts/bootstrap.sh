#!/usr/bin/env bash
# One-time (or after-pull) setup: frontend dependencies, Playwright browsers,
# the Maven distribution the wrapper pins, backend dependencies, and a
# backend/.env to start from.
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"

require_node
require_maven

log "installing frontend dependencies (npm ci — never 'install', which rewrites the lockfile)"
(cd "$FRONTEND_DIR" && "$NPM" ci)

log "installing Playwright's Chromium"
# Browsers only; --with-deps needs sudo, so system libraries stay the operator's
# job (frontend/README.md covers what Playwright asks for if any are missing).
(cd "$FRONTEND_DIR" && npx playwright install chromium)

# First wrapper invocation downloads and checksum-verifies the pinned Maven
# release, so this both prepares the tooling and resolves dependencies.
log "preparing Maven $(pinned_version maven) via the wrapper and resolving backend dependencies"
(cd "$BACKEND_DIR" && "$MVN" -q -B dependency:go-offline)

if [[ -f $BACKEND_DIR/.env ]]; then
  log "backend/.env already exists — left untouched"
else
  cp "$BACKEND_DIR/.env.example" "$BACKEND_DIR/.env"
  log "created backend/.env from .env.example"
  warn "backend/.env carries the example credentials, and this remote is public — change APP_PASSWORD and APP_SECONDARY_PASSWORD before exposing the service."
fi

command -v semgrep >/dev/null 2>&1 ||
  warn "semgrep not on PATH — 'make verify-backend' and the frontend security gate will fail. Install with: pip install semgrep"
docker compose version >/dev/null 2>&1 ||
  warn "'docker compose' unavailable — 'make infra-up' and 'make integration-test' will fail."

log "bootstrap complete"
