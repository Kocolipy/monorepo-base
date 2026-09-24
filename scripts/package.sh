#!/usr/bin/env bash
# Build the SPA and package it into the Spring Boot JAR.
#
# The contract (see /AGENTS.md and backend/pom.xml):
#
#   frontend source -> frontend/dist -> backend/target/classes/static -> JAR
#
# The copy is the backend's `with-frontend` Maven profile, which is off by
# default so `./mvnw clean verify` in backend/ stays a pure backend build. This
# script builds the SPA and then activates that profile with an explicit
# -Dfrontend.dist.dir, rather than leaning on the POM's parent-relative default.
#
# Nothing generated is copied back into a source directory: the profile reads
# frontend/dist and writes only into backend/target. The profile's validate-phase
# enforcer fails the build if index.html is absent from that directory, so a
# missing or half-built SPA is an error and never a silently stale one.
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"

require_node
require_maven

SKIP_TESTS="${PACKAGE_SKIP_TESTS:-1}"
DIST_DIR="$FRONTEND_DIR/dist"

log "building the SPA"
(cd "$FRONTEND_DIR" && "$NPM" run build)

# The enforcer would catch this too; failing here names the frontend build as the
# cause instead of surfacing it as a Maven rule violation.
[[ -f $DIST_DIR/index.html ]] || die "frontend/dist/index.html missing after 'npm run build'"

mvn_args=(-B -Pwith-frontend "-Dfrontend.dist.dir=$DIST_DIR" package)
if [[ $SKIP_TESTS == 1 ]]; then
  # `make verify-backend` is the test gate; packaging just produces the artifact.
  mvn_args+=(-DskipTests)
  log "packaging the JAR with the SPA (tests skipped — set PACKAGE_SKIP_TESTS=0 to run them)"
else
  log "packaging the JAR with the SPA, running tests"
fi
(cd "$BACKEND_DIR" && "$MVN" "${mvn_args[@]}")

# -print -quit rather than `find ... | head -1`: head closing the pipe early
# SIGPIPEs find, and `set -o pipefail` then reports 141 for a successful search.
jar="$(find "$BACKEND_DIR/target" -maxdepth 1 -name 'backend-*.jar' ! -name '*-plain.jar' -print -quit)"
[[ -n $jar ]] || die "no backend-*.jar produced in backend/target"

# Prove the SPA actually landed in the artifact rather than trusting the profile
# ran: a JAR without static/index.html serves a blank page at runtime. Spring
# Boot's repackage nests classes, so the entry is BOOT-INF/classes/static/.
#
# `unzip -Z1 <jar> <entry>` and not `unzip -l | grep -q`: grep exits on its first
# match and SIGPIPEs the unzip still streaming a ~70MB archive, which under
# `set -o pipefail` makes the pipeline exit 141 and fails a build that succeeded.
if command -v unzip >/dev/null 2>&1; then
  if entry="$(unzip -Z1 "$jar" 'BOOT-INF/classes/static/index.html' 2>/dev/null)" && [[ -n $entry ]]; then
    log "verified $entry inside the JAR"
  else
    die "packaged $jar but it contains no BOOT-INF/classes/static/index.html — the with-frontend profile did not apply"
  fi
fi

log "packaged $jar"
