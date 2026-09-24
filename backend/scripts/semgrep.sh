#!/usr/bin/env bash
# Baseline static-analysis gate: pinned Semgrep rulesets over the whole repository.
# Exits non-zero on any finding, so green means zero findings.
set -euo pipefail

RULESETS=(
  p/java
  p/security-audit
  p/owasp-top-ten
)

if ! command -v semgrep >/dev/null 2>&1; then
  echo "semgrep not found on PATH. Install it with: pip install semgrep" >&2
  exit 127
fi

config_args=()
for ruleset in "${RULESETS[@]}"; do
  config_args+=(--config "${ruleset}")
done

cd "$(dirname "$0")/.."
exec semgrep scan "${config_args[@]}" --error "$@"
