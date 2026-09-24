#!/usr/bin/env bash
# Kiro preToolUse hook: remind the agent to consult the knowledge graph before
# it reaches for raw grep/read. Mirrors the Claude Code wiring in
# .claude/settings.json so both runtimes behave the same way.
#
# Usage (from an agent config's hooks.preToolUse):
#   .kiro/hooks/graphify-guard.sh search   # matcher: shell|grep
#   .kiro/hooks/graphify-guard.sh read     # matcher: read|glob
#
# Contract: the hook event JSON arrives on stdin and is forwarded untouched.
# This hook NEVER blocks a tool call -- graphify's hook-guard signals by
# printing a reminder, and anything unexpected fails open with exit 0 so a
# broken graph can never wedge the agent.

set -uo pipefail

mode="${1:-search}"

root="$(git rev-parse --show-toplevel 2>/dev/null)" || exit 0
interp_file="$root/graphify-out/.graphify_python"

# Inert in any checkout that has no graph or no recorded interpreter.
[ -r "$interp_file" ] || exit 0
[ -r "$root/graphify-out/graph.json" ] || exit 0

interp="$(cat "$interp_file")"
[ -x "$interp" ] || exit 0

# Forward stdin; swallow a nonzero status so the guard is advisory only.
"$interp" -m graphify hook-guard "$mode" || true
exit 0
