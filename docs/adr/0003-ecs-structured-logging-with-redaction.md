# 3. ECS-structured logging with redaction enforced structurally

Date: 2026-09-25

## Status

Accepted.

## Context

The service had no logging configuration and no application log statements at all:
whatever Boot's default pattern layout printed was the whole log stream. Two
things are wanted of it before the SCIM surface lands.

First, a collector has to be able to read it. The pattern layout is a sentence,
so every value in a record is text at an offset, and a reader either writes a
regular expression per message or gives up on querying.

Second, and the reason this is not a formatting preference: the account surface
handles values that must not be written to a log store. A `userName` is half a
credential and, on a failed login, very often a mistyped password. A SCIM filter
expression carries whatever attribute values the caller searched on. A password,
a bearer token, a hash and a cookie value are secrets outright. A log store is
read by more people than the database, retained longer, and shipped further, so a
value copied into it has a wider blast radius than the record it came from.

"Do not log those" is a property of every call site at once, which is exactly the
kind of rule review does not hold. The leak that matters is the log line added
next year in a flow no test drives, by someone who has not read this file.

## Decision

Three parts, each chosen so the rule is checkable rather than remembered.

**ECS JSON on stdout.** `logging.structured.format.console: ecs`, in its own
`logging.yaml` document imported by `application.yaml` — the same arrangement
`session.yaml` uses, and for the same reason: the test resources'
`application.yaml` shadows the main one entirely, so a setting stated only there
is a setting no test can assert. A record's variable parts are therefore named
fields, and a message is a constant.

**One writer for the logging context.** `LogContext` exposes named setters
— `http.request.id`, `scim.connector.id`, `scim.resource.id`, and since #67
`user.id` (see the addendum) — and no general-purpose one. All of them are ids: the readable identifiers are precisely
what must not be logged. Every value passes through a sanitizer that replaces
control characters and truncates, so a value that reached a connector id from a
token cannot end the current record and forge the next (CWE-117). `ArchUnit`
(`mdc_is_only_touched_by_the_log_context`) and Semgrep (`be-mdc-direct-access`)
both hold `LogContext` to being the only production class that touches `MDC`.

**Correlation ids minted here, never accepted from the caller.**
`RequestIdFilter` runs ahead of the security chain, so a request refused before it
reaches a handler is still logged under an id, and it stores the id on the request
so the container's error dispatch reports the same exchange rather than a second
one. It ignores `X-Request-Id` and friends: honouring a caller-supplied value
would let a client merge unrelated requests in a log search by repeating one, and
this service sits behind no proxy whose header it has agreed to trust. When such a
contract exists, trusting it becomes a deliberate change here.

The gates are two, deliberately overlapping:

- `EcsLogFormatTests` drives an accepted login, a refused login and an
  administrative change through the real filter chain, encodes every resulting
  record with the production encoder, parses each as ECS JSON, and asserts that no
  fixture credential or identifier appears anywhere in the encoded bytes.
- `be-log-message-concatenation` and `be-log-sensitive-value` reject, at scan
  time, a value concatenated into a message and a value whose name says it is an
  identifier or a secret being passed to a logging call — including as a `{}`
  parameter, which is the idiomatic form of this leak and the one a formatting
  rule would miss.

The test can only speak for the flows it drives; the scan speaks for call sites
that do not exist yet. Neither alone is the property.

## Consequences

Local development output is JSON by default. `LOG_STRUCTURED_FORMAT` set empty
restores the human-readable pattern; the redaction rule does not depend on the
format either way, since nothing is permitted to log those values under any
layout.

The records this change adds name no subject. `LoginService` logs the outcome of
an attempt and the type of a refusal; `AccountAdministrationService` logs which
administrative action was applied or refused. Neither names the account, because
an account is currently identified by `username` alone and there is nothing else
to name it by. That is a real gap — "an administrative change happened" without
"to whom" — and it closes when the account aggregate gains a stable id: the id
goes in the logging context as `scim.resource.id`, and the audit trail, not this
stream, becomes what authoritatively records who changed what.

A structured field remains available for a client-influenced value that genuinely
must be recorded — a PATCH attribute path, a `scimType` — provided it is emitted
as a field rather than concatenated, and sanitized on the way in.

## Addendum (2026-10-01): `user.id` and the standard event vocabulary

Issue #67. The stable id the Consequences above waited for now exists — the SCIM
User resource id — and the logging standard (`Log_Schema.md` §User, §Event) asks
for it and for a closed event vocabulary.

**`user.id`.** `LogContext` gains a fourth setter, `userId(UUID)`, typed so that a
`userName` cannot be passed. It is set for the rest of a request by
`SessionUserLogContextFilter`, placed after `SecurityContextHolderFilter`, from
the session's principal index — which the login writes with the SCIM id, not the
`Authentication`'s name. It is set explicitly on the login-success record, because
that index is written only after the record is emitted. A refused login carries
no `user.*` field at all, even when the request arrived on an authenticated
session: the identity the attempt named is unresolved, and the session's User is
not whom it was for (User standard §3.4).

**Actor and subject.** `user.id` is always the actor. Where a record concerns a
different User — admin unlock and force-change, applied or refused — the User acted
on is `user.target.id`, following ECS's `user.target.*`. Connector lifecycle
records carry the actor only: their subject is a connector, not a User, and the
connector and token ids stay with the audit trail as before. The scheduled jobs
run with no actor and carry no user field.

**The vocabulary.** `LogEvent` declares the `event.kind`, `event.category`,
`event.type`, `event.action` and `event.severity` members this service uses, plus
`event.duration_ms` (which replaces `audit.retention.duration_ms`). A record is
classified by `LogEvent.classify(record, Operation, Category, Type...)`, and
`LogEvent.Operation` is the single mapping from what this service does onto the
standard's action. Where no action fits, or one action covers several operations,
the operation's own name is kept under `app.event.action` — namespaced under the
service's own `app.` key — rather than a member being invented for the standard's
enum. Semgrep `be-log-event-action-outside-the-vocabulary` holds `classify` to
being the only writer of either key.

| Operation (old `event.action`)            | `event.action`                | `app.event.action`                     | `event.category` | `event.type`                     |
| ----------------------------------------- | ----------------------------- | -------------------------------------- | ---------------- | -------------------------------- |
| `login`, accepted                         | `user-authentication`         | —                                      | `process`        | `user`, `allowed`                |
| `login`, refused                          | `user-authentication`         | —                                      | `process`        | `user`, `denied`                 |
| `identity.unlock`, applied / refused      | `access-control`              | `identity.unlock`                      | `process`        | `admin`, `user`, `change`/`denied` |
| `identity.force_password_change`, applied / refused | `password-change-enforcement` | —                            | `process`        | `admin`, `user`, `change`/`denied` |
| `identity.password_change` (self-service), applied / refused | `user-administration` | `identity.password_change`  | `process`        | `user`, `change`/`denied`        |
| `scim.connector.create`                   | `access-control`              | `scim.connector.create`                | `configuration`  | `admin`, `creation`              |
| `scim.connector.delete`                   | `access-control`              | `scim.connector.delete`                | `configuration`  | `admin`, `deletion`              |
| `scim.connector.token.issue`              | `access-control`              | `scim.connector.token.issue`           | `configuration`  | `admin`, `creation`              |
| `scim.connector.token.rotate`             | `access-control`              | `scim.connector.token.rotate`          | `configuration`  | `admin`, `change`                |
| `scim.connector.token.revoke`             | `access-control`              | `scim.connector.token.revoke`          | `configuration`  | `admin`, `deletion`              |
| `scim.write` (integrity violation)        | `user-provisioning`           | `scim.write`                           | `database`       | `error`                          |
| `identity.inactivity_deactivation`, run   | `user-administration`         | `identity.inactivity_deactivation`     | `batch`          | `job-end`                        |
| `identity.dormant_authority_revocation`, run | `access-control`           | `identity.dormant_authority_revocation` | `batch`         | `job-end`                        |
| `audit.retention`, run                    | — (no action fits)            | `audit.retention`                      | `batch`          | `job-end`                        |
| any job's schedule at startup             | as the job's row              | as the job's row                       | `configuration`  | `info`                           |
| `audit.append` (append failed)            | — (no action fits)            | `audit.append`                         | `database`       | `error`, plus `event.severity` `high` |

`event.kind` is `event` on every record. Two operations have no standard action:
the enum offers nothing for deleting aged-out audit rows or for an audit write
failing, and `access-control` or `user-administration` would mislabel them for a
search on those values, so they carry `app.event.action` alone. The job records
are `job-end` only: a run emits one record, at its end, carrying
`event.duration_ms` where it measures one; `job-start` records arrive with #70,
which emits the scheduled jobs' start/end pair using this vocabulary.

## Addendum (2026-10-01): the record envelope — trace ids, service fields, UTC+8, log file

Issue #66. The logging standard (`Structured_Logging_Application_Standard.md` §3.1,
§3.5, §4, §6) requires fields on every record that the Decision above does not
produce, and a durable local file for a forwarding agent.

**Trace and span ids.** `micrometer-tracing-bridge-otel`, wired by Boot's
`spring-boot-micrometer-tracing-opentelemetry` module, gives each observation a
span: every HTTP request (Boot's `ServerHttpObservationFilter`) and, through an
observation opened in `ScheduledJobMetrics.instrument`, every scheduled-job run.
All three jobs now run through `instrument`, so every job record is correlated.
`TraceLogCorrelationConfig` replaces Boot's `Slf4JEventListener` with one writing
the ECS keys `trace.id` and `span.id` — the defaults (`traceId`, `spanId`) would
land as top-level fields no ECS query selects on. These are the first context keys
`LogContext` does not write. That is acceptable for the reason the class exists:
the values are tracer-minted hex ids, not values anything else supplies.

The tracing is for correlation only. No exporter is on the classpath, and
`management.tracing.export.enabled: false` (`telemetry.yaml`) keeps it that way if
one arrives transitively. The same switch makes Boot install a no-op propagator, so
an inbound `traceparent` is ignored and every trace id is minted here — the same
rule the Decision applies to `X-Request-Id`, for the same reason. Sampling does not
gate the ids: an unsampled span still has them. `TraceExportTests` holds all of it.

**Service fields.** `logging.structured.ecs.service.*` in `logging.yaml`: `name`
stated as `backend` (not inherited from `spring.application.name`), `version`
from the build — `logging.yaml` is the one resource-filtered document, so
`@project.version@` becomes the artefact's version — and `environment` from
`APP_ENVIRONMENT`, default `local`. The test configuration now imports
`logging.yaml` as well, so tests record what a deployment records.

**`@timestamp` in UTC+8, the JVM zone untouched.** Boot's ECS formatter writes the
event's `Instant` as UTC and takes no zone. Rather than a formatter of our own — a
copy of Boot's that would drift from it — `EcsTimestampCustomizer`, a
`StructuredLoggingJsonMembersCustomizer` registered through
`logging.structured.json.customizer`, rewrites the top-level `@timestamp` member
alone as `yyyy-MM-dd'T'HH:mm:ss.SSS+08:00` in `Asia/Singapore`
(`ServiceTimeZone`). It is the same instant, so ordering and parsing downstream
are unaffected. The JVM's default zone is deliberately not set: that would move
SCIM `meta` times, audit times, cron evaluation and the injected `Clock`, and the
SCIM wire stays UTC. The cron triggers instead name `ServiceTimeZone.ZONE`
explicitly, so a job's schedule is evaluated in the zone its records are read in.

**Log file.** `logging.file.name: ${LOG_FILE:}` with `logging.structured.format.file:
ecs` and Boot's size-and-time rolling policy (daily or 50 MB, 14 days, 1 GB total).
Unset means no file, which is what local development and the tests run with. The
EC2 deployment sets `LOG_FILE=/var/log/backend/backend.json` and its CloudWatch
agent ships that file into a log group the stack creates with explicit retention;
the service itself never sends a log over the network. `LogFileTests` starts the
configuration in a child JVM, because logging is JVM-global.

## Addendum (2026-10-01): request records and lifecycle records

Issue #68. The standard (`Structured_Logging_Application_Standard.md` §2 #0 and #1,
§3.1, §3.4) asks for a record per request and for startup and shutdown records; the
service wrote neither.

**One record per request, at its end.** `RequestIdFilter` writes it from the
`finally` of the request dispatch, so a refusal by the security chain and an
exception escaping a handler get one as surely as a success; the error dispatch,
the same exchange's second pass, writes none. It carries `http.request.method`
(from a fixed set of methods, `_OTHER` beyond it, because a client chooses the
method), `http.route`, `http.response.status_code`, `event.duration_ms` and
`event.outcome` (`success` below 400). The level follows the status: `INFO`
below 400, `WARN` for a 4xx, `ERROR` for a 5xx. An exception escaping the chain
is recorded as the `500` the container then answers.

`http.route` is the template Spring's handler mapping matched
(`/scim/v2/Users/{id}`), or `unmatched` when none did — which is every refusal
the security chain makes before dispatch. Neither ECS nor `Log_Schema.md` has a
template field, so the name is OpenTelemetry's. The raw path, the query string,
headers, cookies, the body and the client address are never read into the record
at all, so an id or a filter expression in the URL has no way into it.
`/actuator/health` (and its probe sub-paths) and `/actuator/prometheus` get no
record: they are infrastructure polling every few seconds, and their status and
timing are already on `http.server.requests`.

**No start record.** The standard says to log a request's start. The end record
carries everything a start record would — method and route — plus the status,
duration and outcome only the end can know, so a start record would add no field
an investigation lacks while doubling the stream. One at `DEBUG` would satisfy
the letter of the rule while being off in every deployment (§3.3: DEBUG is not
for permanent production use), so this service writes none and treats the end
record as satisfying "log the request's start". A request that never ends — a
hung thread — is the case this gives up, and the thread dump and
`http.server.requests`' active-request gauge are what find that.

**Filter order.** For the request record to carry the `trace.id` every other
record of its request carries, it must be written inside the request's span, so
`RequestIdFilter` now runs at `HIGHEST_PRECEDENCE + 2`, immediately inside Boot's
`ServerHttpObservationFilter` (`+ 1`) rather than ahead of it. It is still far
ahead of the security chain, which is what the Decision above relies on.
`EcsLogFormatTests` reads the three registrations' orders.

**Startup and shutdown.** `ApplicationLifecycleLog` (in its own `lifecycle`
slice, because it reads the SCIM, auth and audit slices' settings and each of
those depends on `observability`) writes:

- on `ApplicationReadyEvent`, `application-startup` with `host.name`, `host.ip`,
  `spring.profiles.active`, and the effective value of each non-secret setting
  that changes behaviour: `app.scim.enabled`,
  `app.dormancy.deactivation.window`, `app.dormancy.authority_revocation.window`,
  `app.audit.retention.period`. Effective rather than configured: a window whose
  default belongs to a domain policy is read from that policy, so an unset
  setting shows the default it resolved to. `service.*` comes from the formatter,
  as on every record. No datasource, Redis, credential or identity setting is
  read at all.
- on `ContextClosedEvent`, `application-shutdown` with the context's uptime as
  `event.duration_ms`.

Each answers only for its own context, so a management child context closing does
not log a second shutdown. A host that cannot resolve its own name gets no
`host.*` fields rather than a placeholder.

The session timeouts and the lockout threshold are deliberately absent, although
the ticket asked for them: §2 #0 of the standard says not to log "timeout or retry
values for authentication flows", and they are exactly that. The standard wins.
An operator checking them after a deploy reads them from the deployment's
configuration (`APP_SESSION_ABSOLUTE_LIFETIME`, `APP_LOCKOUT_MAX_ATTEMPTS`,
`server.servlet.session.timeout`), not from the log. The lifecycle tests assert
the keys stay off the record.

| Operation               | `event.action`         | `app.event.action` | `event.category` | `event.type`     |
| ----------------------- | ---------------------- | ------------------ | ---------------- | ---------------- |
| inbound HTTP request    | — (no action fits)     | `http.request`     | `network`        | `access`, `end`  |
| application ready       | `application-startup`  | —                  | `process`        | `start`          |
| application context closing | `application-shutdown` | —              | `process`        | `end`            |

Both lifecycle records carry `event.outcome` `success` and `event.severity` `low`,
as the standard's lifecycle recipe has them.
