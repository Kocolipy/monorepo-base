---
inclusion: always
---

# Kiro: graphify enforcement

Kiro-specific wiring for the graph-refresh rule in `/AGENTS.md`. That file states
the rule and when it binds; this file states how Kiro enforces it and which
commands to run. Other runtimes carry their own copy of this (Claude Code in
`/CLAUDE.md` + `/.claude/settings.json`, Codex in `/.codex/hooks.json`) — do not
assume their wiring applies here.

## The hooks

Wired through the `hooks` block of the **active agent config**, not the project,
because that is where kiro-cli reads them from. Both scripts resolve the repo
root with `git rev-parse` and exit 0 outside a graphify checkout, so a global
agent config can carry them without affecting unrelated projects.

| Script | Trigger | Job |
| ------ | ------- | --- |
| `/.kiro/hooks/graphify-guard.sh` | `preToolUse` on shell, grep, read, glob | Advisory reminder to query the graph before raw search. Never blocks a call. |
| `/.kiro/hooks/graphify-refresh.sh` | `stop` (end of every turn) | Runs `update .` when, and only when, the turn changed code or docs. |

`stop` is the trigger that matters: it fires when you finish responding, which is
the "before you report done" boundary AGENTS.md asks for. Nothing is expected of
you by hand — if the hook is active, the refresh has already happened by the time
your turn lands.

`graphify-refresh.sh` exits silently unless `git status` reports a changed source
or doc file newer than `graph.json`, so a question-answering turn costs one cheap
subprocess. It holds an `flock` so overlapping turns cannot race `graph.json`,
and it fails open — missing interpreter or absent graph exits 0 rather than
wedging the turn. Its log is `graphify-out/.graphify_refresh.log` (gitignored).
Set `GRAPHIFY_REFRESH_DRY_RUN=1` to see what it would refresh without rebuilding.

**Hooks are read when a session spawns.** After editing an agent config, the
change applies to the NEXT session, not the current one — refresh the graph by
hand for the rest of a session in which you changed the wiring.

## When the refresh fails

The hook exits nonzero and surfaces its stderr rather than retrying. Do not
reach for `update --force` to clear it: a rebuild with fewer nodes is graphify's
shrink guard working as designed, and forcing past it unattended can drop nodes
silently. Dispatch the runner agent, which is built to make exactly that call:

```bash
kiro-cli chat --agent graphify-runner --no-interactive "refresh the graph; the stop hook failed"
```

## graphify-runner

`/.kiro/agents/graphify-runner.json` — a deliberately cheap, single-purpose agent
for graph maintenance that needs judgment: `qwen3-coder-next` (the lowest credit
multiplier available), `shell`/`read`/`grep` only, and git write commands denied
in `toolsSettings` so it cannot commit, stage, or reset.

The **routine** refresh does NOT go through it. The `stop` hook runs the command
directly as a subprocess, so the common path spends no model tokens at all. Use
the runner only for a failed refresh, a legitimate `--force` after real
deletions, a stale prune, or an explicitly requested re-label.

Everyday graph reads (`query`, `path`, `explain`, `affected`) stay in your own
session — handing those to a subagent adds a round-trip and buys nothing.
