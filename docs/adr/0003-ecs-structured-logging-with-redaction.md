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

**One writer for the logging context.** `LogContext` exposes three named setters
— `http.request.id`, `scim.connector.id`, `scim.resource.id` — and no
general-purpose one. All three are ids: the readable identifiers are precisely
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
