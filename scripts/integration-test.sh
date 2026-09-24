#!/usr/bin/env bash
# Full-stack integration run: dependencies + both applications + Playwright.
#
# Playwright's own `webServer` block starts the Vite dev server (baseURL
# http://localhost:5173) and Vite proxies /api to localhost:8080, so this script
# brings up Postgres, Redis and the backend, then hands the frontend to
# Playwright. Everything this script started is torn down again unless KEEP_UP=1.
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"

require_node
require_maven
require_cmd curl
require_docker

[[ -d $FRONTEND_DIR/node_modules ]] ||
  die "frontend/node_modules missing — run 'make bootstrap' first."

KEEP_UP="${KEEP_UP:-0}"
COMPOSE=(docker compose -f "$BACKEND_DIR/compose.yaml")
backend_pid=""

cleanup() {
  local status=$?
  trap - INT TERM EXIT
  if [[ $KEEP_UP == 1 ]]; then
    log "KEEP_UP=1 — leaving the backend and dependencies running"
  else
    log "tearing down"
    stop_group "$backend_pid"
    "${COMPOSE[@]}" down >/dev/null 2>&1 || true
  fi
  exit "$status"
}
trap cleanup INT TERM EXIT

log "starting Postgres and Redis"
"${COMPOSE[@]}" up -d --wait

load_backend_env
BACKEND_PORT="${SERVER_PORT:-8080}"

# Refuse to run against a leftover backend: the suite would pass or fail against
# code that is not the tree under test.
require_port_free "$BACKEND_PORT" "the backend"
require_port_free 5173 "the Vite dev server Playwright starts"

bg_start backend_pid backend "$BACKEND_DIR" "$MVN" -q -DskipTests spring-boot:run
wait_for_http "http://localhost:$BACKEND_PORT/actuator/health" backend 180 "$backend_pid"

log "running Playwright (it starts the Vite dev server itself)"
(cd "$FRONTEND_DIR" && "$NPM" run test:e2e)
