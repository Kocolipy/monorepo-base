# SCIM 2.0 account-management research

**Date:** 2026-09-25
**Scope:** Convert the current database-backed Account feature into an inbound SCIM 2.0 service provider while retaining password Login and Redis-backed application sessions.

## Executive finding

The settled target is broadly compatible with SCIM 2.0. RFC 7643 explicitly allows a service provider to expose only the core attributes it needs, treats `externalId` as belonging to the provisioning domain, defines `password` as optional and write-only, leaves Group authorization semantics to the service provider, and permits direct-only membership. RFC 7644 makes PATCH, filtering, sorting, ETags, and Bulk independently discoverable capabilities, so supporting the first four while advertising Bulk as unsupported is valid.

The two RFC-derived decisions are resolved:

1. **Deleted identifier reuse.** RFC 7644 section 3.6 says a deleted resource **SHOULD NOT** participate in conflict calculation. Privacy-minimal tombstones remain, but their keyed identifier hashes are retained only for redacted historical correlation and never participate in uniqueness checks. Former `userName` and connector-scoped `externalId` values may be reused.
2. **Connector-token lifetime.** RFC 7644 section 7.4 says bearer tokens **MUST** have a limited lifetime determinable by the service provider. Every token expires 365 days after issue; 365 days is both the default and hard maximum. Rotation overlap is configurable up to 14 days and never extends the old token beyond its original expiry.

A third standards tension does not need a product decision: RFC 7644 examples use weak ETags with `If-Match`, while HTTP conditional requests require strong comparison for `If-Match`. The implementation should issue **strong** opaque ETags so mandatory conditional writes work unambiguously. `meta.version` must contain exactly the same entity-tag value as the `ETag` response header.

## Primary sources

- [RFC 7643 — SCIM Core Schema](https://www.rfc-editor.org/rfc/rfc7643.html)
- [RFC 7644 — SCIM Protocol](https://www.rfc-editor.org/rfc/rfc7644.html)
- [RFC 6750 — Bearer Token Usage](https://www.rfc-editor.org/rfc/rfc6750.html)
- [RFC 6585 — 428 Precondition Required](https://www.rfc-editor.org/rfc/rfc6585.html)
- [RFC 9110 — HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html)
- [RFC 7613 — PRECIS Username and Password Profiles](https://www.rfc-editor.org/rfc/rfc7613.html), incorporated by reference from RFC 7644 section 5

## Existing application seams

The current design is small but security-sensitive:

- `Account` uses mutable `username` as both the database primary key and the Spring Security principal name. It also owns the password hash, one `USER`/`ADMIN` role, enabled state, login-failure run, lockout and creation time.
- `AccountService` implements `UserDetailsService`, seeds two configured Accounts, and maps one stored role to one Spring authority.
- `LoginService` is the mandatory password-authentication seam. ADR 0001 intentionally records success and failure there rather than through Spring Security events.
- `AccountAdministrationService` owns listing, enable/disable and unlock. It revokes sessions after disabling through the `AccountSessions` port.
- `AccountSessionsAdapter` finds Redis sessions by principal **username**. Mutable SCIM `userName` would therefore make old sessions unreachable unless the session principal changes to the stable SCIM User `id`.
- `UserCounterController` also passes `Principal.getName()` to counter storage, so mutable `userName` would orphan application state. Application-owned data must be keyed by stable SCIM User `id`, not `userName`.
- `SecurityConfig` currently has one session-oriented filter chain. It applies browser CSRF protection and role rules to `/api/**`, while the SPA fallback treats only `/api` and `/actuator` as reserved server paths.
- The Accounts SPA expects a username, one role and enable/disable/unlock actions. The target page must instead render SCIM-owned identity and membership read-only while retaining application-owned Unlock plus connector-token and audit operations.
- The current OpenAPI document globally assumes session-cookie authentication and CSRF on every unsafe request. SCIM machine requests need a separate bearer-authenticated, stateless contract that is not subject to browser CSRF.
- JPA currently relies on `hibernate.ddl-auto: update`; there is no checked-in migration tool. The SCIM relational model is large enough that the implementation plan should introduce explicit versioned migrations rather than entrust schema creation, constraints, and data removal to Hibernate inference.

These seams support an incremental replacement, but this is not a compatibility migration: the application is pre-production and existing non-Bootstrap Accounts need not be preserved.

## RFC requirements and implications

### Base URI, media type and discovery

Use `/scim/v2` as the versioned SCIM base URI. RFC 7644 sections 1.3 and 3.13 permit a path prefix and a `v2` segment. Relative resource endpoints remain `/Users` and `/Groups` under that base.

All SCIM representations use UTF-8 and `application/scim+json`; the server must accept that media type and should also accept `application/json` as required by RFC 7644 section 3.8. Every resource or protocol message carries the appropriate `schemas` array.

Expose these GET endpoints publicly as already decided:

- `/scim/v2/ServiceProviderConfig`
- `/scim/v2/ResourceTypes` and `/scim/v2/ResourceTypes/{id}`
- `/scim/v2/Schemas` and `/scim/v2/Schemas/{schemaUri}`

`ServiceProviderConfig` must advertise `patch.supported=true`, `bulk.supported=false`, `filter.supported=true`, `filter.maxResults=200`, `changePassword.supported=true`, `sort.supported=true`, `etag.supported=true`, and one `oauthbearertoken` authentication scheme. RFC 7643 section 5 recommends public authentication-scheme discovery. RFC 7644 section 4 requires collection forms of ResourceTypes and Schemas to use `ListResponse` and says their filter/sort/pagination parameters are ignored; a discovery `filter` should receive `403` so clients cannot mistake an ignored condition for a match.

The `/scim` prefix must become a Reserved server path so missing or malformed SCIM routes produce SCIM/HTTP errors rather than the SPA shell.

### Authentication and filter-chain separation

SCIM does not define its own authentication scheme. RFC 7644 section 2 permits bearer tokens over TLS and requires the authenticated client to map to an access-control policy. Implement two ordered Spring Security filter chains:

1. A stateless `/scim/v2/**` chain that disables browser CSRF only for this namespace, permits discovery GETs, authenticates resource and search requests from `Authorization: Bearer`, and never creates an application session.
2. The existing session/CSRF chain for `/api/**`, the SPA and actuator behavior.

Only the Authorization header is accepted for connector tokens. RFC 6750 section 2.1 requires resource servers to support it; query-string tokens are not accepted because URLs are routinely logged. Missing, expired, revoked or malformed tokens return `401` with `WWW-Authenticate: Bearer`; a read-only token attempting a mutation returns `403` with `error="insufficient_scope"`. SCIM error bodies still use `urn:ietf:params:scim:api:messages:2.0:Error`.

Read-only scope is operation-based, not HTTP-method-based: it permits GET and POST `/.search`, because search POST is read-only. Read-write adds User and Group POST/PUT/PATCH/DELETE.

Each token needs a random lookup identifier plus at least 256 bits of random secret material. Persist only a cryptographic hash of the complete presented token, its connector, scope, issue/expiry/revocation times and rotation lineage. Compare hashes in constant time. Plaintext is shown once. Every token expires 365 days after issue; administrators may choose a shorter lifetime but never a longer one. Rotation overlap is configurable up to 14 days and is truncated at the old token's original expiry. TLS is mandatory at the deployment edge, and any path between a terminating proxy and the service must remain trusted and protected.

### Supported User profile

Expose the core User schema URI `urn:ietf:params:scim:schemas:core:2.0:User`, with exactly the supported subset described by `/Schemas`:

- common: `schemas`, `id`, calling connector's `externalId`, `meta`
- required: `userName`
- optional singular: `active`, `displayName`, `preferredLanguage`, `locale`, `timezone`, `password`
- optional `name`: `formatted`, `familyName`, `givenName`, `middleName`, `honorificPrefix`, `honorificSuffix`
- optional multi-valued `emails`: `value`, `type`, `primary`, and server-rendered `display` only if needed
- read-only multi-valued `groups`: `value`, `$ref`, `display`, `type="direct"`

Do not advertise or accept the Enterprise User extension, custom extensions, or unsupported core attributes. RFC 7644 section 3.1 explicitly permits a service provider to support a subset of RFC 7643. Attribute names and operators are case-insensitive; value comparisons follow each attribute's `caseExact` definition.

`id` is server-issued, opaque, stable, non-reassignable and unique across **both** Users and Groups, not merely within one table (RFC 7643 section 3.1). `userName` is required, case-insensitive, mutable and unique across live Users. Normalize and compare `userName` and passwords through the RFC 7613 PRECIS profiles before uniqueness checks or authentication, as required by RFC 7644 section 5. A missing `active` defaults to `true` for authenticated provisioning; explicit `false` is authoritative.

`password` is optional, write-only and `returned: never`. Hash it immediately with the application's password encoder; never retain, return, filter on, audit, or log its cleartext or encoded value. Reject password filtering as an unsupported attribute/operator combination. A User without a password may be active for provisioning and authorization data purposes but password Login always gives the same bare `401` used for all refused credentials.

`emails.primary=true` may occur at most once. Email values should be canonicalized for comparison without inventing server uniqueness: RFC 7643 defines email uniqueness as `none`.

### Connector-scoped externalId

RFC 7643 section 3.1 says `externalId` is issued by the provisioning client and must always be interpreted as scoped to the provisioning domain. The connector is the local representation of that domain, so one alias row per `(connector_id, resource_id)` is conformant. RFC 7643 assigns `externalId` uniqueness `none`, so duplicate values on different resources within the same connector remain valid and a filter may return multiple matches; index the exact case-sensitive value without a uniqueness constraint.

A response shows only the calling connector's alias. A write changes only that alias but increments the directory resource version because the SCIM representation changed. Deleting a connector deletes all its aliases and tokens in one transaction while audit records retain only the connector's stable identifier and redacted display metadata.

### Groups and authorization

Expose `urn:ietf:params:scim:schemas:core:2.0:Group` with required `displayName`, optional calling-connector `externalId`, mutable `members`, common attributes and metadata. Ordinary Group names need not be unique; authorization is attached to the seeded Admin group's stable `id`, never to a mutable or duplicated display label.

Accept only members resolving to live User ids. Reject a Group member or unknown resource with `400 invalidValue`. User `groups` is computed, read-only and changed only through Group writes, as required by RFC 7643 section 4.1.2. Read-only User `groups` values submitted in POST/PUT are ignored under RFC 7644 mutability rules.

The Admin Group and Bootstrap Admin are instance-level protected resources even though the core schema attributes are generally mutable:

- Bootstrap Admin User PUT/PATCH/DELETE returns `403`.
- Admin Group rename or DELETE returns `403`.
- Removing Bootstrap Admin from the Admin Group returns `403`, and because PATCH is atomic the whole PATCH fails.
- Other direct Admin Group membership remains SCIM-managed.

Do not use `scimType=mutability` for these instance-level policy refusals: the schema attribute remains mutable for ordinary resources. A plain SCIM `403` accurately states that the authenticated connector is not permitted to perform that operation.

Every active User has baseline `ROLE_USER`; direct membership in the stable Admin Group additionally grants `ROLE_ADMIN`. The Login response may keep the current compact role contract, but the authority must be derived from Group membership rather than a role column.

### CRUD, replacement and PATCH

Implement User and Group POST, GET collection/item, PUT, PATCH and DELETE exactly under `/scim/v2/Users` and `/scim/v2/Groups`.

- POST ignores read-only attributes, validates required values, returns `201`, full canonical representation, `Location` and `ETag`; live uniqueness conflicts return `409` with `scimType=uniqueness`.
- PUT never creates. It requires `userName` or `displayName`, ignores read-only fields, replaces supplied read-write/write-only values, and applies one documented policy for omitted optional attributes. Use true replacement semantics: omitted optional profile attributes become unassigned, while omitted `password` remains unchanged because a write-only secret cannot be round-tripped safely.
- PATCH accepts only the `PatchOp` schema and a non-empty ordered `Operations` array. Implement `add`, `remove`, and `replace`, optional paths where the RFC permits them, value-path filters, no-op success for already-present additions/removals, `noTarget`, `invalidPath`, mutability and required-attribute behavior. The complete operation array is atomic.
- Successful PUT returns `200` plus the full canonical resource. Successful PATCH returns `200` plus the resource consistently rather than mixing `200` and `204`; DELETE returns `204`.

Group membership writes must increment the Group version and every affected User version because each affected User's read-only `groups` representation changes. Password, `userName`, `active`, and SCIM profile changes increment the User version. Login failure/lockout activity does not increment the SCIM version because it is operational authentication state not visible in the SCIM representation.

### Search, filtering, sorting and projection

Support GET collection queries and POST searches at `/Users/.search`, `/Groups/.search`, and the base `/.search`. POST search avoids exposing PII-bearing filters in URLs as recommended by RFC 7644 sections 3.4.3 and 7.5.2. Read-only tokens may use it.

Implement the complete RFC 7644 filter grammar selected by the user: `eq`, `ne`, `co`, `sw`, `ew`, `pr`, `gt`, `ge`, `lt`, `le`, `and`, `or`, `not`, parentheses, schema-qualified paths, sub-attributes and value-path expressions. Enforce operator/type compatibility and schema `caseExact`. A syntactically malformed filter, unsupported path, password path, or incompatible comparison returns `400 invalidFilter`; an attribute absent only from one resource type in a base search evaluates as no value rather than making the whole filter invalid.

Translate the parsed AST into parameterized JPA Criteria or a dedicated query adapter; never concatenate literals into SQL. Bound filter length, AST depth, operation count and generated joins even though request-rate limiting is out of scope. These are per-request resource and parser-safety limits, not a rate limiter.

Support `sortBy` and `sortOrder`, defaulting order to ascending, with RFC handling for complex and multi-valued attributes. Always add stable `id` as a final tie-breaker. Support both mutually exclusive `attributes` and `excludedAttributes`; `id` and any `returned: always` fields remain present.

Pagination is one-based. `startIndex < 1` becomes 1; negative `count` becomes 0; `count=0` returns no Resources but still returns `totalResults`. Omitted `count` defaults to 100. A requested count above 200 returns at most 200. List responses include `schemas`, `totalResults`, `Resources`, `startIndex`, and `itemsPerPage` as applicable.

### ETags and multi-writer concurrency

Store a monotonically increasing SCIM resource version and render it as a strong opaque ETag such as `"u-<opaque version>"` or `"g-<opaque version>"`; do not expose a database sequence with cross-resource meaning. The exact value appears in both the `ETag` header and `meta.version`.

Require `If-Match` on every existing-resource PUT, PATCH and DELETE. Missing headers return `428 Precondition Required` with a SCIM error body explaining how to retry; RFC 6585 defines 428 specifically for lost-update prevention. A non-matching condition returns `412 Precondition Failed` and performs no state or audit-success write. Evaluate authorization and resource existence before preconditions as required by HTTP semantics. Accept `If-Match: *` only if intentionally treating it as "any current representation"; the safer multi-writer profile should require the exact returned ETag.

### Deletion, tombstones and audit

RFC 7644 section 3.6 permits non-physical deletion, but all later operations for the deleted id must return `404`, and collections must omit it. Deleting a User revokes its sessions and removes memberships. Deleting an ordinary Group removes memberships and updates affected User representations. The Admin Group and Bootstrap Admin cannot be deleted.

A privacy-minimal tombstone retains resource type, stable id, deletion time and keyed hashes of normalized former identifiers. It must not retain profile, password, Group membership or connector-token data. The hashes support redacted historical correlation only and never participate in uniqueness checks, so former `userName` and connector-scoped `externalId` values may be reused as RFC 7644 recommends.

Audit events are append-only and redact all secrets and profile values. Record actor connector/token id, operation, resource type/id, outcome, changed attribute paths, HTTP/SCIM error classification and timestamp. Success events belong in the same transaction as the state mutation. Failed requests need a separate append path after rollback so failure auditing cannot accidentally commit a partial mutation. Retention is configurable with a one-year default; tombstone retention is separate.

### Session and application identity

Make stable SCIM User `id` the authenticated principal name and the key for Redis session lookup and application-owned data such as counters. Expose current mutable `userName` separately in the principal and Login response. This prevents a rename from orphaning sessions or counter state and lets session revocation target one stable identity.

Revoke all sessions after successful changes to `active`, `password`, `userName`, deletion, or Admin Group membership. Perform revocation only after the database transaction commits so a rolled-back SCIM write never signs the User out. The existing after-commit application seam and indexed-session adapter remain the right architecture; rename the capability around User id rather than Account username.

`active=false` blocks new Login independently of lockout. Credentialless Users are also refused. Preserve ADR 0001: all password Login still flows through `LoginService`, and the failure run remains application-owned. Unlock remains an Admin UI capability and does not alter SCIM `active`.

## Recommended implementation order

1. **Persistence foundation:** introduce explicit schema migrations and normalized User, email, Group, membership, connector, token, alias, tombstone and audit tables; seed Bootstrap Admin and Admin Group by stable configured ids.
2. **Stable application identity:** replace username-keyed principals, counters and session revocation with SCIM User id while preserving the current Login behavior and role-shaped frontend contract.
3. **SCIM security and discovery:** add the stateless bearer chain, Reserved `/scim` path and public configuration/schema/resource-type endpoints.
4. **User vertical slice:** create/read/query User with canonical serialization, connector aliases, strong ETags and SCIM errors; then PUT/PATCH/DELETE and session side effects.
5. **Group vertical slice:** create/read/query Group, direct membership, derived Admin authority, protected seeded resources and affected-User version/session updates.
6. **Query completeness:** full filter parser/AST translation, POST search, sorting, projection and pagination limits.
7. **Admin operations:** replace the Accounts page and `/api/admin/accounts` projection with operational User/Group status, Unlock, connector/token lifecycle and audit views. SCIM-owned fields stay read-only.
8. **Contract and hardening:** expand OpenAPI, add conformance fixtures, run backend/frontend full gates, Semgrep, targeted PIT and Stryker, and end-to-end Redis/session checks.

Each slice should leave the application green and demonstrable. Because the application is pre-production, no dual-write Account compatibility layer or data cutover belongs in the plan.

## Testing strategy

Prefer deployed-wire behavior seams over implementation tests:

- MockMvc application-context tests for each SCIM endpoint through the real filter chain, media type, serializer and error mapper.
- Parameterized RFC fixtures for schemas, CRUD status/header/body requirements, attribute mutability and error codes.
- Parser/semantic tests generated from the filter and PATCH ABNFs, including precedence, Unicode, invalid syntax, deep-expression limits and value-path cases.
- PostgreSQL integration tests for uniqueness, connector alias isolation, row locking, atomic PATCH, version checks and membership cascades; H2 alone is not authoritative for PostgreSQL constraints or query semantics.
- Redis integration tests proving revocation by stable User id after each security-sensitive mutation and proving rollback/refused writes revoke nothing.
- Concurrency tests with two connectors reading one ETag: one succeeds, the stale writer gets `412`; missing `If-Match` gets `428`.
- Security tests proving token expiry/revocation/scope, no token/password leakage, public discovery only, SCIM CSRF separation, and unchanged browser `401`/`403` behavior.
- Frontend component and Playwright coverage for read-only identity, Unlock, one-time token display, rotation/revocation, audit rendering and authority loss after Admin-membership change.
- Targeted PIT for security/domain services and scoped Stryker for changed frontend behavior, alongside both apps' full baseline/security/architecture gates.

## Resolved RFC decisions

- Deleted `userName` and connector-scoped `externalId` values are reusable. Tombstone hashes remain for redacted historical correlation only and do not create uniqueness conflicts.
- Connector tokens expire after 365 days by default and at most. Rotation overlap is configurable up to 14 days and cannot extend the old token's original expiry.

The design-tree frontier is empty and the research has no unresolved factual prerequisite. The resulting implementation plan is recorded in `docs/specs/scim-v2-account-management-plan.md`.
