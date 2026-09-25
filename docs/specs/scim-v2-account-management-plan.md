# SCIM 2.0 account-management specification plan

**Status:** Proposed for implementation
**Date:** 2026-09-25
**Decision state:** Design tree closed; no unresolved product decisions
**Research:** [SCIM 2.0 account-management research](../research/scim-v2-account-management-research.md)

## Problem statement

The application currently models identity as an `Account`: a username-keyed password credential with one `USER` or `ADMIN` role, enabled and lockout state, and Redis-backed sessions. Administrators can list, disable, enable and unlock Accounts through a session-authenticated page.

That model cannot be provisioned by a standards-based external directory. It has no stable identity independent of username, no Group resources, no SCIM discovery or schema contract, no connector authentication, no conditional writes for multiple provisioning clients, and no standard filtering, PATCH or error model. Username is also used as the principal key for sessions and application data, so allowing SCIM to rename it would orphan state.

The application must become an inbound SCIM 2.0 service provider without turning SCIM into an end-user Login protocol. SCIM owns identity and Group membership; password Login, failure runs, lockout and browser sessions remain application capabilities over the provisioned User.

## Goals

1. Expose one deployment-wide SCIM 2.0 directory containing Users and Groups.
2. Replace Account with SCIM User as the domain identity and stable owner of authentication state.
3. Let multiple independently authenticated connectors read and, when scoped, write the shared directory safely.
4. Derive application Admin authority from direct membership in one reserved Admin Group.
5. Preserve password Login, lockout, CSRF and Redis-backed browser sessions as a separate interface.
6. Provide practical generic RFC 7643/7644 conformance without vendor-specific behavior or Bulk.
7. Keep the Accounts page operational: SCIM-owned data is visible but read-only; application-owned Unlock, connector tokens and audit remain actionable.
8. Make every security-sensitive identity change revoke existing sessions before the affected User can continue acting under stale identity or authority.
9. Enforce application-owned credential hygiene — inactivity deactivation, dormant authority revocation, first-login and Admin-forced password change with a grace period, password history, and end-user self-service password change — without moving any of it into the SCIM contract.
10. Emit operational telemetry for the provisioning surface so connector failure is observable without a user report.

## Actors

- **SCIM connector:** an external directory integration authenticated by its own opaque bearer token.
- **Read-only connector:** may discover, retrieve and search Users and Groups but cannot mutate them.
- **Read-write connector:** has read capability plus User and Group create, replace, patch and delete.
- **User:** a provisioned identity that may authenticate with a password when active and credentialed.
- **Admin:** an active User directly assigned to the reserved Admin Group.
- **Bootstrap Admin:** the local recovery User, visible but immutable through SCIM and permanently assigned to the Admin Group.
- **Operator:** configures deployment secrets, retention and transport security.

## User stories

1. As a connector, I want to discover supported SCIM features and schemas without credentials, so that I can configure interoperability before a token is installed.
2. As a connector, I want standard User and Group resource endpoints, so that I do not need application-specific provisioning code.
3. As a connector, I want my own `externalId` namespace, so that another connector cannot collide with or observe my identifiers.
4. As a read-only connector, I want to retrieve and search the directory without being able to mutate it, so that synchronization can follow least privilege.
5. As a read-write connector, I want to create Users with or without passwords, so that provisioning is not coupled to password onboarding.
6. As a read-write connector, I want to change a User's password without ever reading it back, so that credentials can be provisioned without disclosure.
7. As a read-write connector, I want to rename `userName` without changing `id`, so that directory renames preserve identity and application data.
8. As a read-write connector, I want to deactivate or delete a User, so that access ends immediately and all existing sessions are revoked.
9. As a read-write connector, I want to create and maintain Groups containing direct User members, so that authorization assignments can be provisioned.
10. As a read-write connector, I want Admin authority to follow membership in the reserved Admin Group, so that no separate role field can drift from directory state.
11. As a connector, I want protected Bootstrap Admin and Admin Group resources to remain visible, so that directory reads explain the application's effective authority.
12. As a connector, I want standard PUT and atomic PATCH behavior, so that complete and partial synchronization both work.
13. As a connector, I want standard filtering, sorting, projection and pagination, so that I can synchronize large directories efficiently.
14. As a connector, I want POST search, so that filters containing personal information need not be placed in URLs.
15. As one of several writers, I want strong ETags and mandatory conditional mutations, so that I cannot silently overwrite another connector's change.
16. As a connector, I want deleted resource ids to return `404` and disappear from queries, so that deletion has normal SCIM semantics.
17. As a connector, I want former `userName` and connector-scoped `externalId` values to be reusable after deletion, so that reprovisioning follows RFC 7644 guidance.
18. As an Admin, I want to see Users, Groups, credential presence, active state and lockout state, so that I can diagnose access without editing SCIM-owned identity.
19. As an Admin, I want to unlock a User without changing SCIM `active`, so that lockout and provisioning state remain separate capabilities.
20. As an Admin, I want to create read-only or read-write connectors, so that integrations receive only the authority they need.
21. As an Admin, I want token plaintext shown exactly once, so that the service never becomes a secret-retrieval system.
22. As an Admin, I want to rotate a token with bounded overlap, so that an integration can switch credentials without downtime.
23. As an Admin, I want to revoke tokens and delete connectors, so that compromised or retired integrations immediately lose access and their aliases are removed.
24. As an Admin, I want a redacted audit trail, so that provisioning, token activity and refusals can be investigated without exposing credentials or profile values.
25. As the Bootstrap Admin, I want my recovery identity and Admin membership protected from SCIM writes, so that an external directory cannot remove every recovery path.
26. As an active credentialed User, I want existing password Login and session behavior preserved, so that provisioning does not become authentication.
27. As a credentialless or inactive User, I want Login to return the same bare `401` as other rejected credentials, so that account state cannot be enumerated.
28. As an operator, I want audit retention configurable with a one-year default, so that storage and governance requirements can be balanced.
29. As an operator, I want Users deactivated after 90 days without authentication, so that dormant access does not persist indefinitely.
30. As an Admin, I want to force a password change on a User I suspect is compromised, so that the current credential stops working without waiting for the external directory.
31. As a User required to change my password, I want a self-service page that accepts my current and new password, so that I can restore access myself.
32. As an operator, I want request rate, latency, error class and saturation signals for the SCIM surface, so that a broken or probing connector is visible before it is reported.
33. As an operator, I want elevated authority removed from long-dormant Users, so that an unused Admin account is not a standing risk.
34. As a User, I want a password chosen for me by a connector to require replacement before I can do anything else, so that no credential someone else transported stays in use.

## Domain and authority model

### SCIM User replaces Account

A SCIM User is the single identity aggregate. It owns:

- stable server-issued SCIM `id`;
- mutable unique `userName` and supported profile attributes;
- SCIM `active` state and resource version;
- optional encoded password;
- application-owned failure run and lockout;
- creation and modification metadata;
- direct Group memberships through the membership relation.

The stable SCIM User `id`, not `userName`, is the authenticated principal name and key for application-owned data. The session principal also carries current `userName` and authorities for rendering. Counter ownership and Redis session lookup move from username to User id before username mutation is enabled.

Password Login continues through `LoginService` as required by ADR 0001. Spring Security remains responsible for credential authentication and account-status enforcement. A User can log in only when all are true:

- the User exists and is not deleted;
- `active=true`;
- an encoded password exists;
- no lockout is in force;
- the submitted password is accepted.

Every refusal remains an empty `401`; callers cannot distinguish unknown, inactive, credentialless, locked or wrong-password cases.

A User with a pending forced password change authenticates normally; the refusal happens at authorization, not authentication, so this state is not enumerable pre-login.

### Inactivity deactivation

The User records `lastAuthenticatedAt`, set on every successful password Login. A scheduled application job deactivates any User whose inactivity window has elapsed:

- the window is 90 days, deployment-configurable;
- inactivity is measured from `lastAuthenticatedAt`, falling back to `meta.created` for a User that has never authenticated, so a credentialless User is not deactivated the moment it is provisioned;
- deactivation sets `active=false`, increments the User's SCIM version, revokes that User's sessions and appends an audit event whose actor is the system job;
- the Bootstrap Admin is exempt, because deactivating the local recovery identity can remove the last access path exactly when the external directory is unavailable;
- a connector may re-activate a deactivated User through ordinary SCIM writes; reactivation resets the window by setting `lastAuthenticatedAt` to the reactivation time.

The job's deactivation takes priority over the directory's assertion of `active`. A connector cannot hold a dormant User active by re-asserting `active=true` on every sync: only an explicit transition from `false` to `true` reactivates a User and resets the window, while a write asserting `active=true` on an already-active User remains the no-op the version rules already make it, and resets nothing. A connector that reactivates a User who is still dormant at the next evaluation sees it deactivated again; that is the intended outcome, not a loop to suppress.

This is application-owned state. `lastAuthenticatedAt` is not a SCIM attribute, is absent from `/Schemas`, and is not filterable; it is visible only in the Admin operational projection.

### Dormant authority revocation

A second stage removes elevated authority from Users who remain dormant well past deactivation:

- the window is 180 days, deployment-configurable, measured on the same `lastAuthenticatedAt` basis as deactivation;
- the job removes the User's direct membership of the Admin Group, dropping `ROLE_ADMIN` and leaving baseline `ROLE_USER` intact. Ordinary Group memberships are untouched, because they carry no authority in this model;
- removal increments the Group's and the affected User's SCIM versions and revokes that User's sessions, exactly as a connector-driven membership removal does;
- the Bootstrap Admin is exempt, because revoking the local recovery identity's authority can remove the last administrative path precisely when the external directory is unavailable;
- as with deactivation, the job's removal takes priority: a connector may re-add the membership, and the job removes it again while the User stays dormant.

### Forced and self-service password change

The User carries an application-owned `mustChangePassword` flag, also absent from the SCIM schema.

- Any Admin may set the flag on a credentialed User from the Accounts page. Setting it revokes that User's sessions immediately.
- The flag cannot be set on a credentialless User, which already cannot log in, and cannot be set on the Bootstrap Admin by anyone other than the Bootstrap Admin itself.
- While the flag is set, an authenticated session receives only the authority to read its own change-password requirement, submit a password change and log out. Every other application endpoint returns `403`, including `/api/admin/**`, so a flagged Admin cannot act until the credential is replaced.
- The SPA route `/change-password` is reachable by any authenticated User and is the only landing target offered while the flag is set. It submits current password plus new password over the session/CSRF boundary.
- A submission is accepted only when the current password verifies and the new password satisfies the password policy and differs from the current one. A wrong current password returns `401` and counts toward the same per-User failure run and lockout as Login. At the configured threshold, that one lockout blocks both Login and further self-service password-change attempts until an Admin uses Unlock; a policy violation returns `400` naming the unmet rule without echoing either value.
- A successful change hashes the new password, clears the flag, revokes all of that User's sessions including the one that submitted it, increments the User's SCIM version and appends a redacted audit event. The SPA returns the User to Login.
- A connector setting `password` through SCIM **sets** the flag rather than clearing it. A credential chosen and transported by a third party is known outside the User, so it must be replaced before it is used for anything else — this is the first-login change requirement, and it applies equally to the first password a provisioned User receives and to any later connector-set password, including one sent to displace a credential an Admin distrusted.
- A User provisioned without a password is not flagged; the flag is set when a password first arrives.
- The flag records `mustChangePasswordSince`. A scheduled job deactivates any flagged User who has not changed the password within the grace period — 30 days, deployment-configurable — setting `active=false`, revoking sessions and appending an audit event whose actor is the system job. The Bootstrap Admin is exempt, for the same reason it is exempt from inactivity deactivation.
- Reactivating a credentialed User sets the flag, because a credential that sat unused across a deactivation should not be trusted on return.

All three scheduled jobs — inactivity deactivation, grace-period deactivation and dormant authority revocation — are serialized per job name so that no two runs of the same job overlap across instances.

Admins never see, choose or transport a User's password: forcing a change invalidates the existing credential, it does not disclose or replace it.

### Authority

Every active authenticated User receives baseline `ROLE_USER`. Direct membership in the seeded Admin Group additionally yields `ROLE_ADMIN`. No role column remains.

The Admin Group is recognized only by its stable id. Its `displayName` is fixed as `Admin`, but duplicate ordinary Group names do not grant authority. SCIM may add or remove ordinary direct User memberships; removing a User's Admin membership revokes that User's sessions after commit.

#### Authorization matrix

Two concerns are split deliberately. **SCIM holds membership** — who has which authority — and that stays the directory's to write. **Configuration holds reachability** — which authority may call which path and method — and that is not expressible through SCIM at all.

Reachability is therefore declared as an explicit authorization matrix loaded from deployment configuration, not inlined in filter-chain code:

- the matrix maps each authority to the path patterns and HTTP methods it may reach, and is the single source of truth for that mapping;
- the set of authorities it may name is fixed and closed — `ROLE_USER` and `ROLE_ADMIN` — because authority is derived from Group membership rather than declared in configuration. A matrix naming an unknown authority fails fast at startup, as does a duplicate or malformed rule;
- the matrix is read-only at runtime. No endpoint creates, edits or deletes a rule, and a changed matrix takes effect on the next application start;
- it is loaded before the application serves traffic, and the security chains are built from it, so an unmatched request is denied rather than defaulting to permitted;
- it governs the application chain only. The SCIM chain's authorization is the connector token's scope, which is a credential property rather than a role.

Public paths and authenticated-but-unprivileged paths are declared in the same configuration, so the whole reachability picture is readable in one place instead of inferred from code.

### Protected recovery resources

On a fresh database the server seeds:

- one Bootstrap Admin SCIM User from external deployment configuration;
- one Admin Group;
- their immutable membership.

The Bootstrap Admin is always active, locally credentialed and visible through SCIM. SCIM cannot PUT, PATCH or DELETE it. It is seeded with `mustChangePassword` set, so the credential that came from deployment configuration must be replaced at first login and never becomes the standing one; it is exempt from the grace-period deactivation that flag normally carries, because deactivating the recovery identity is the outcome that flag exists to avoid. The Admin Group cannot be renamed or deleted, and no operation may remove the Bootstrap Admin membership. These instance-level policy refusals return `403` without `scimType`; they do not change the core schema's mutability for ordinary resources.

## SCIM base and endpoint contract

The Base URI is `/scim/v2`. `/scim` is a Reserved server path and never falls through to the SPA shell.

| Endpoint | Methods | Authentication | Contract |
| --- | --- | --- | --- |
| `/scim/v2/ServiceProviderConfig` | GET | Public | Advertises exact capabilities and bearer scheme |
| `/scim/v2/ResourceTypes`, `/scim/v2/ResourceTypes/{id}` | GET | Public | User and Group resource definitions |
| `/scim/v2/Schemas`, `/scim/v2/Schemas/{schemaUri}` | GET | Public | Exact supported schema subset |
| `/scim/v2/Users` | GET, POST | Connector | Query or create Users |
| `/scim/v2/Users/{id}` | GET, PUT, PATCH, DELETE | Connector | Retrieve or mutate one User |
| `/scim/v2/Users/.search` | POST | Connector | User search without URL parameters |
| `/scim/v2/Groups` | GET, POST | Connector | Query or create Groups |
| `/scim/v2/Groups/{id}` | GET, PUT, PATCH, DELETE | Connector | Retrieve or mutate one Group |
| `/scim/v2/Groups/.search` | POST | Connector | Group search without URL parameters |
| `/scim/v2/.search` | POST | Connector | Search Users and Groups together |
| `/scim/v2/Bulk` | none | N/A | Not implemented; discovery advertises unsupported |
| `/scim/v2/Me` | none | N/A | Returns `501`; connectors do not represent end Users |

Successful and error bodies use UTF-8 `application/scim+json`. The server accepts `application/scim+json` and `application/json`, defaulting responses to `application/scim+json`. Every resource or message includes the correct `schemas` array. Resource creation returns `201`, `Location`, strong `ETag` and the canonical resource. PUT and PATCH return `200`, `Location`, new `ETag` and the canonical resource. DELETE returns `204` with no body.

### ServiceProviderConfig

Advertise:

- `patch.supported=true`
- `bulk.supported=false`, `maxOperations=0`, `maxPayloadSize=0`
- `filter.supported=true`, `maxResults=200`
- `changePassword.supported=true`
- `sort.supported=true`
- `etag.supported=true`
- one `authenticationSchemes` entry of type `oauthbearertoken`, documenting the locally issued connector token

Discovery collection responses use SCIM `ListResponse`. Filtering, sorting and pagination are ignored on discovery endpoints as RFC 7644 requires; a supplied discovery `filter` receives `403` so an ignored filter cannot be mistaken for a match.

## Supported schemas

### User

Schema URI: `urn:ietf:params:scim:schemas:core:2.0:User`.

| Attribute | Required | Mutability / returned | Local behavior |
| --- | --- | --- | --- |
| `schemas` | yes | read-only / always | Exact core User schema URI only |
| `id` | server | read-only / always | Opaque, stable, non-reassignable, globally unique across Users and Groups |
| `externalId` | no | read-write / default | Connector-scoped, case-exact alias; only the calling connector's value is visible; uniqueness is `none` |
| `userName` | yes | read-write / default | PRECIS-normalized, case-insensitive, unique among live Users, mutable |
| `name` | no | read-write / default | `formatted`, `familyName`, `givenName`, `middleName`, `honorificPrefix`, `honorificSuffix` |
| `displayName` | no | read-write / default | Primary human-readable label |
| `preferredLanguage` | no | read-write / default | Valid language-priority value |
| `locale` | no | read-write / default | Valid language tag |
| `timezone` | no | read-write / default | Valid IANA time-zone identifier |
| `active` | no | read-write / default | Defaults true on create; false blocks Login and revokes sessions |
| `password` | no | write-only / never | PRECIS-processed, policy-validated and hashed immediately; never returned, filtered, logged or audited as a value |
| `emails` | no | read-write / default | `value`, `type`, `primary`; at most one primary; `(type,value)` duplicates removed |
| `groups` | no | read-only / default | Computed direct memberships with `value`, `$ref`, `display`, `type=direct`; writes occur through Group |
| `meta` | server | read-only / default | `resourceType`, `created`, `lastModified`, `location`, `version` |

Unsupported core User attributes and every extension are omitted from `/Schemas` and rejected when asserted as write targets. Read-only fields included in POST or PUT are ignored per RFC 7644. Password filters return `400 invalidFilter`.

A missing password is valid. Credential presence is visible only to the Admin application projection as a boolean; neither SCIM nor the Admin interface exposes the hash.

### Group

Schema URI: `urn:ietf:params:scim:schemas:core:2.0:Group`.

| Attribute | Required | Mutability / returned | Local behavior |
| --- | --- | --- | --- |
| `schemas` | yes | read-only / always | Exact core Group schema URI only |
| `id` | server | read-only / always | Same global id namespace as Users |
| `externalId` | no | read-write / default | Connector-scoped case-exact alias; uniqueness is `none` |
| `displayName` | yes | read-write / default | Not globally unique; Admin Group instance is immutable |
| `members` | no | read-write / default | Direct live User ids only; `value`, `$ref`, `type=User`; member sub-attributes immutable |
| `meta` | server | read-only / default | Standard metadata and version |

Nested Groups are unsupported. A member resolving to a Group, deleted User or unknown id returns `400 invalidValue`. User `groups` remains the read-only reverse view.

## Connector identity and token lifecycle

A connector has a stable id, display name, enabled/deleted state, creation metadata and one or more token records during rotation. Token scope is `READ_ONLY` or `READ_WRITE`; write implies read.

A token consists of a non-secret lookup id plus at least 256 bits of random secret material in one opaque bearer value. Persist only a cryptographic hash of the complete value and compare it in constant time. Plaintext is returned only by issue and rotation responses, both marked `Cache-Control: no-store`.

- Default token lifetime: 365 days.
- Hard maximum lifetime: 365 days; an Admin may choose less.
- Maximum rotation overlap: 14 days.
- The overlap end is the earlier of requested overlap and the old token's original expiry.
- Rotation never extends an old token's lifetime.
- Revocation and expiry are effective immediately.
- Deleting a connector revokes all its tokens and deletes all its `externalId` aliases in one transaction; Users and Groups remain.

Only `Authorization: Bearer <token>` is accepted. Missing credentials return `401` and `WWW-Authenticate: Bearer`. Expired, revoked or malformed tokens return `401` with `error="invalid_token"`. Read-only mutation attempts return `403` with `error="insufficient_scope"`. Tokens are never accepted through query strings, form bodies, cookies or browser sessions.

## Security boundaries

Implement two ordered Spring Security chains:

1. **SCIM chain:** matches `/scim/v2/**`, is stateless, creates no HTTP session, disables browser CSRF only for this namespace, permits discovery GETs and authenticates every resource/search request with the connector bearer filter.
2. **Application chain:** retains the current session repository, login path, CSRF double-submit, SPA behavior and security headers, and builds its path/method authorization from the configured authorization matrix rather than inline rules.

SCIM traffic must use TLS at the deployment edge. Production deployment must not expose the service over plaintext or forward bearer tokens through an untrusted hop. Request/access logging must redact `Authorization`, `password`, cookies and request bodies carrying SCIM secrets.

Rate limiting is a deployment-edge responsibility, not an application one. The edge must throttle `/scim/v2/**` per source and per credential, and must throttle the Login and change-password endpoints, so that bearer authentication has a brute-force and exhaustion deterrent even though the application performs no counting of its own. Per-account throttling is the deterrent that matters here; per-source throttling is left to the edge's own policy and is not assumed, because a deployment behind a corporate proxy or NAT gateway presents many legitimate Users as one address. A deployment that exposes the service without edge throttling is misconfigured; the requirement belongs in the deployment documentation and in `infra/`, not in the request path.

The application therefore contains no request-rate limiter. It still enforces per-request safety limits, which are not rate limits:

- request body: 1 MiB;
- filter text: 8 KiB;
- parsed filter depth: 20;
- filter expression nodes: 100;
- PATCH operations: 100;
- response page: 200 resources.

Limit failures use the closest standard SCIM/HTTP error (`413`, `400 invalidFilter`, or `400 invalidValue`) and never partially mutate a resource.

## Credential and cryptographic policy

### Password policy

One policy governs every path that sets a password — SCIM `password` on POST, PUT and PATCH, and the self-service `/change-password` flow:

- minimum 12 characters, maximum 256;
- no composition rules, no expiry, and no password hints or knowledge-based recovery questions, because forced complexity, forced rotation and hint mechanisms all reduce real-world strength;
- every printable character is accepted, including the space and any Unicode code point; one code point counts as one character, and no character is stripped, substituted or rejected for being "special";
- PRECIS normalization before validation, comparison and hashing, so a normalized duplicate cannot slip past either;
- rejected when it equals or contains the `userName` case-insensitively;
- rejected when it appears in a blocklist of commonly used, expected or known-compromised values, checked after normalization. The blocklist is deployment-configurable and loaded from a local corpus; no candidate password, and no hash or prefix of one, is sent to an external service;
- rejected when it matches any of the User's last **3** passwords, the current one included;
- a violation returns `400 invalidValue` through SCIM and `400` naming the unmet rule through the application chain, never echoing the submitted value.

Password history is stored as Argon2id hashes in `scim_user_password_history`, verified by matching the candidate against each retained entry, trimmed to the newest three on every successful change, and deleted with the User. Because one policy governs every path, a connector re-sending a password still held in history is refused with `400 invalidValue`: provisioning must send a genuinely new value, and a PUT intending no credential change omits `password` entirely.

### Hashing and primitives

- Passwords are hashed with **Argon2id** through a `DelegatingPasswordEncoder`, at `m=19456` KiB, `t=2`, `p=1`. The stored value carries its own `{argon2id}` prefix, so the scheme can change later without a schema migration. This requires BouncyCastle on the backend classpath and replaces the current BCrypt encoder; because the database is pre-production there is nothing to migrate.
- Argon2id has no input-length ceiling, which is why the password maximum above is 256 characters rather than bcrypt's 72-byte truncation point.
- Connector token secrets come from `SecureRandom` with at least 256 bits of entropy.
- The token hash is SHA-256 via `MessageDigest`, compared with `MessageDigest.isEqual` for constant time. An unstretched hash is correct **only** because the token is full-entropy random material rather than a password; this must not be read as license to hash passwords the same way.
- No hand-rolled cryptography, no custom KDF, and no cryptographic primitive outside Spring Security Crypto, the JDK and BouncyCastle.
- **No pepper.** A pepper would be the only keyed secret in the system, creating a rotation and migration burden for no gain over Argon2id with per-hash salts.

### Key rotation and storage

- The only rotatable credentials are connector tokens, already bounded at 365 days with 14-day overlap, and the database and Redis passwords, rotated annually through deployment configuration.
- Password hashes are self-describing and unkeyed; Spring Session keeps session state server-side with no signing key. Neither has a key to rotate, and this is a deliberate design outcome rather than an omission.
- Token hashes exist only in `scim_connector_tokens`, reached through a least-privilege application database role. No key material is written to the filesystem or baked into an image, and database backups are encrypted at rest.

### Session lifetime

The existing 15-minute idle timeout is retained and an **8-hour absolute maximum**, measured from authentication, is added. On reaching either bound the session is terminated and re-authentication is required regardless of activity. Both bounds apply to every authenticated session, Admin sessions included.

**One concurrent session per User.** A successful Login invalidates any session that User already holds, so a credential cannot be in use from two places at once and a stolen session cannot outlive the owner's next sign-in.

### Lockout policy

Lockout is **permanent until an Admin lifts it**. **5 consecutive failed attempts** lock the User, and the only exit is Admin Unlock: there is no duration, no automatic lift, and no configuration key expressing one. The attempt threshold stays deployment-configurable with no enforced floor. The failure run is counted per User and never per source address, so rotating addresses cannot dilute it, and it resets to zero on a successful Login or on Unlock.

Imposing a lockout **revokes every live session** of that User, through the same after-commit revocation path as deactivation — a permanent lock that leaves an already-established session working protects nothing against an attacker who holds one.

The **Bootstrap Admin is exempt from lockout entirely**: its failure run is counted and every failed attempt is audited, but it never locks. With no automatic lift, a locked Bootstrap Admin would be an unrecoverable deployment, and this is the one account that exists to recover the others. The accepted cost is unbounded online guessing against that single account, mitigated by Argon2id verification cost, uniform refusal timing and audited failures — not by a lock.

Lock state remains invisible to the caller: every refusal is the same empty `401`, and lockout is surfaced only in the Admin projection on the Accounts page. A User who cannot get in learns nothing by waiting, which is deliberate — the operational answer is an Admin, not the clock.

Slice 0 shipped the time-bound form (`5` attempts, `20m`, automatic lift); Slice 0a below supersedes it.

### Uniform authentication timing

Every authentication refusal returns the same bare `401`, and the work done before refusing is equalized: a Login for an unknown `userName`, an inactive User, or a User with no password still performs a password verification against a dummy Argon2id hash of the same parameters before failing. Without it, the absence of a hash to verify makes those cases measurably faster and turns response time into an account-state oracle that the identical status code was meant to close.

### Response headers

HSTS is sent as `max-age=31536000; includeSubDomains`, at the deployment edge and from the application. `preload` is deliberately **not** used, because submission is effectively irreversible and binds the whole apex domain.

### Log formatting

Application logs use Boot's structured logging in ECS format. MDC carries request id, connector id and resource id only. `userName`, filter text, password, bearer values, hashes and cookies never enter a log line, in MDC or in a message, which is the same rule as the redaction requirement above and is verified in the same tests. Any remaining client-influenced value that is written — an attribute path in a changed-paths list, a `scimType` — is emitted as a structured field with control characters and line breaks stripped, never concatenated into a message, so a crafted PATCH path cannot forge a log record (CWE-117).

## Query contract

### Filtering

Implement the complete RFC 7644 grammar over supported attributes:

- comparisons: `eq`, `ne`, `co`, `sw`, `ew`, `gt`, `ge`, `lt`, `le`;
- presence: `pr`;
- logic: `and`, `or`, `not` with RFC precedence;
- parentheses;
- schema-qualified and unqualified paths;
- complex sub-attributes;
- multi-valued value-path expressions.

Attribute names and operators are case-insensitive. String values follow each attribute's `caseExact`; `externalId` and `id` are case-exact, while supported profile strings follow their schema definitions. Boolean and date comparisons enforce type compatibility. The parser produces an AST; a persistence adapter translates it to parameterized predicates. No filter literal is concatenated into SQL.

Malformed grammar, unsupported paths, password paths, unknown operators and invalid type/operator pairs return `400 invalidFilter`. In a base search spanning Users and Groups, a valid attribute absent from one resource type evaluates as no value for that type.

### Sorting

Support one `sortBy` and optional `sortOrder=ascending|descending`; ascending is the default. Complex values require a sub-attribute. Multi-valued attributes sort by primary value or first value. Missing values sort last ascending and first descending. Add stable `id` as an internal final tie-breaker.

### Pagination

- `startIndex` is one-based and defaults to 1; values below 1 become 1.
- `count` defaults to 100; negative values become 0; values above 200 are capped at 200.
- `count=0` returns no `Resources` but still computes `totalResults`.
- Responses include `schemas`, `totalResults`, `Resources`, `startIndex` and `itemsPerPage` as applicable.

Pagination is stateless, as SCIM specifies; clients must tolerate concurrent changes between pages.

### Attribute projection

Support mutually exclusive `attributes` and `excludedAttributes` on every operation that returns a resource. Always-returned attributes remain present. An invalid combination or path returns `400 invalidValue`. The password is never returned even when requested.

### POST search

Accept `SearchRequest` bodies at User, Group and base `/.search` endpoints. Read-only tokens may call them. This is the preferred path for PII-bearing filters so personal values do not enter URLs and access logs.

## Write semantics

### POST

- Validate the resource's one core schema URI.
- Ignore read-only attributes.
- Apply create defaults (`active=true`; optional fields unassigned).
- Hash a supplied password before persistence.
- Create a connector alias only when `externalId` is supplied.
- Return `201`, full canonical resource, `Location` and `ETag`.
- A live `userName` conflict returns `409 uniqueness`; duplicate `externalId` values are allowed within a connector as RFC 7643 specifies.

### PUT

PUT replaces an existing resource and never creates. Required `userName` or `displayName` must be present. Supplied read-write/write-only values replace current values; omitted optional profile attributes become unassigned. Omitted `password` remains unchanged because a write-only secret cannot be safely round-tripped. Supplied read-only attributes are ignored. Return `200` with the canonical replacement.

### PATCH

Require the `PatchOp` schema and a non-empty `Operations` array. Apply ordered `add`, `remove` and `replace` operations using RFC paths and value-path filters. The whole request is one transaction:

- no-op add/remove operations succeed without changing version or `lastModified`;
- removing required data returns `400 mutability`;
- malformed paths return `400 invalidPath`;
- filtered paths matching nothing return `400 noTarget` where RFC 7644 requires it;
- incompatible values return `400 invalidValue`;
- any failed operation restores the original resource.

### DELETE and tombstones

DELETE returns `204`, removes the live resource from every read/query, and makes every later operation for that id return `404`. User deletion revokes sessions and removes all memberships. Ordinary Group deletion removes memberships and updates affected User versions.

The tombstone retains only resource type, stable id, deletion time and keyed hashes of normalized former unique identifiers. It contains no readable profile, password, memberships or connector aliases. Hashes support redacted historical correlation only; they never participate in uniqueness checks. Former `userName` and connector-scoped `externalId` values are reusable.

## Resource versions and conditional writes

Each live User and Group has an independent monotonically increasing SCIM version. Render it as a strong opaque ETag and return the exact same value in `meta.version`.

Every existing-resource PUT, PATCH and DELETE requires one exact `If-Match` ETag:

- absent header: `428 Precondition Required` with a SCIM error body explaining that the client must GET and retry with `If-Match`;
- wildcard or malformed validator: `400 invalidValue`;
- non-current validator: `412 Precondition Failed`;
- current validator: mutation proceeds atomically.

Authorization and resource existence are checked before the precondition. A failed precondition changes no state and creates no success audit event.

Increment the resource version only when its SCIM representation changes:

- User profile, active, password, username or connector alias changes increment the User;
- Group profile, membership or connector alias changes increment the Group;
- membership changes also increment affected Users because their `groups` representation changes;
- Group display-name changes increment member Users because `groups.display` changes;
- login attempts, successful Login, lockout and Unlock do not increment SCIM versions.

## Session-revocation contract

After a successful committed mutation, revoke all Redis sessions indexed by stable User id for:

| Change | Revocation |
| --- | --- |
| `active` true to false | affected User |
| password set, changed or cleared | affected User |
| forced password change set by an Admin | affected User |
| self-service password change | affected User, including the session that submitted it |
| inactivity deactivation by the scheduled job | affected User |
| grace-period deactivation by the scheduled job | affected User |
| dormant authority revocation by the scheduled job | affected User |
| `userName` changed | affected User |
| User deleted | affected User |
| added to or removed from Admin Group | affected User |
| ordinary profile/email change | none |
| ordinary Group membership change | none unless it is the Admin Group |
| Group rename | none |
| Unlock | none |

Revocation executes only after commit. A refused, stale or rolled-back SCIM request revokes nothing. Stable-id indexing ensures username changes cannot hide old sessions from the revoker.

## Persistence model

Introduce explicit versioned PostgreSQL migrations; production correctness must not depend on `hibernate.ddl-auto: update`. The normalized model contains:

| Relation | Purpose and constraints |
| --- | --- |
| `scim_resources` | Global UUID id, resource type, version, created/modified timestamps; enforces ids unique across Users and Groups |
| `scim_users` | User profile, normalized unique username, active, nullable password hash, login failure/lockout state, `last_authenticated_at`, `must_change_password`, `must_change_password_since`, Bootstrap marker |
| `scim_user_password_history` | The User's newest three password hashes with their set timestamps; append-and-trim, never read back as plaintext, deleted with the User |
| `scim_user_emails` | Ordered/canonical email values, `(user,type,value)` uniqueness and at most one primary |
| `scim_groups` | Group display name and immutable Admin marker |
| `scim_group_memberships` | Direct `(group_id,user_id)` primary key; no Group-as-member column |
| `scim_connectors` | Connector identity and lifecycle metadata |
| `scim_connector_tokens` | Lookup id, token hash, scope, issue/expiry/revocation, rotation lineage |
| `scim_external_ids` | One case-exact alias per connector/resource; indexed for connector-scoped filtering, but duplicate values across resources are allowed |
| `scim_tombstones` and identifier hashes | Non-readable deleted-resource identity and correlation data; never queried for uniqueness |
| `scim_audit_events` | Append-only redacted event stream and retention timestamp |
| application-owned relations | Counter and any future ownership foreign-key stable User id, never username |

Use database constraints for uniqueness, one-primary-email, direct membership and referential integrity. Lock the resource/version row during mutations. Group membership and all affected resource-version changes commit together.

Because the application is pre-production, remove the old Account schema and seed behavior rather than build dual-write, migration or compatibility machinery. Fresh database provisioning is the supported starting state.

## Application architecture

Keep the existing inward dependency direction:

- **Identity domain:** SCIM User, Group, membership, connector identity, token metadata, resource version, tombstone and their invariants; no Spring or JSON types.
- **Identity application:** create/read/query/replace/patch/delete use cases, connector lifecycle, token lifecycle, audit, Bootstrap seeding and post-commit session effects.
- **SCIM inbound adapter:** RFC request/response DTOs, schema registry, filter/PATCH parsers, canonical renderer and SCIM exception mapping.
- **Persistence adapters:** normalized JPA/PostgreSQL repositories and query translation.
- **Authentication adapter/application:** preserve LoginService and lockout semantics while reading User credentials/active state and deriving authorities from Group membership.
- **Session adapter:** rename Account session capability around stable User id; retain the indexed Spring Session implementation.
- **Administration adapter:** session-authenticated operational projections and actions for the Accounts page.

A credential or token hash never reaches a web projection. Separate response types make exposure structurally impossible.

## Admin API and Accounts page

Keep the SPA route `/accounts` and the `/api/admin/**` session/CSRF boundary. Replace account identity writes with operational endpoints:

- list User operational projections by stable id;
- list Group projections and membership counts;
- unlock User by stable id;
- force password change by stable id;
- list/create/delete connectors;
- issue/rotate/revoke connector tokens;
- list redacted audit events with pagination/filtering.

Separately from `/api/admin/**`, the application exposes one authenticated end-user endpoint for the self-service password change behind the same session and CSRF boundary, plus the SPA route `/change-password`. It requires no Admin authority, is the only non-logout capability available while `mustChangePassword` is set, and returns no information about any other User.

It also exposes one authenticated self-read returning **only the caller's own** record — `userName`, display name, direct Groups, whether a change is required, and last authentication — resolved from the session's stable User id rather than from any client-supplied identifier, so there is no id to tamper with. Lockout state and failure counts are omitted, because telling a caller how close they are to a lock helps an attacker more than the owner.

An Admin may not Unlock or force a password change on their own account; both refuse with `403`, so recovering from a self-inflicted lock requires another Admin or the Bootstrap Admin.

The page contains four operational views:

1. **Users:** `userName`, display name, active, credential configured, lockout, change-required, last authentication, creation time and direct Groups. Identity, active and membership are read-only. Unlock appears only while locked; Force password change appears only while the User is credentialed and not already flagged.
2. **Groups:** name, member count and protected Admin marker. Read-only.
3. **Connectors:** name, tokens, scope, issued/expires/revoked state, rotate/revoke/delete controls. New token plaintext appears once in a non-persistent disclosure panel.
4. **Audit:** timestamp, connector/token identity, operation, resource id, outcome, changed paths and error classification; no profile or secret values.

Any current Admin, including Bootstrap Admin, may manage connectors and tokens. Authority remains enforced by the backend filter chain, not only by route guards.

## Audit and retention

Record every SCIM operation and every connector/token lifecycle operation. Events include actor connector/token id or Admin User id, operation, resource type/id, outcome, changed attribute paths, status/scimType and timestamp. Redact request values, authorization headers, password, token plaintext, hashes and cookies.

Authentication and credential events are recorded in the same stream, because a provisioning trail that cannot say who then used the credential explains only half of an incident:

- Login success and failure, and logout;
- lockout set and lockout lift, the lift always carried out by a named Admin through Unlock;
- forced-change set, self-service change outcome, and each scheduled job's deactivation or authority revocation.

Every event identifies its subject by stable User id and never by `userName`, which is also how audit reads resolve an actor or resource. A reused `userName` therefore cannot merge two identities in the trail, and the tombstone's keyed identifier hashes serve redacted correlation only.

Every event also carries the request's HTTP method, request path and request id, so an audited outcome can be tied to the request that produced it and to the log lines sharing that id.

Every request to a SCIM collection endpoint (`GET /Users`, `GET /Groups`) or either `.search` endpoint is a bulk read for audit purposes, regardless of requested count or result count, including an empty result and `count=1`. It is audited as one event carrying the resource type, the result count and the filter's *shape* — never its literal values — so a full-directory sync or zero-result probe by a compromised token is distinguishable from ordinary traffic. Only single-resource retrievals (`GET /Users/{id}`, `GET /Groups/{id}`) are excluded from read auditing.

Successful write events commit in the same transaction as state. Failure events append after rollback through a separate transaction, so audit cannot commit partial resource state. Audit storage is append-only to normal application code; only the retention job may delete expired rows. Append-only is enforced at the database as well as in code: the application's role holds `INSERT` and `SELECT` on `scim_audit_events` and no `UPDATE` or `DELETE`, which the retention job's separate role holds alone.

Auditing a write is **fail-closed**: the audit insert shares the mutation's transaction, so an audit write that cannot commit rolls the mutation back and the caller receives an error rather than an unrecorded change. A failure-event append that itself fails cannot roll anything back, having no state to undo, and instead raises an operational alert; the underlying request keeps its own outcome.

Timestamps stored in and returned by the SCIM contract — `meta.created`, `meta.lastModified`, tombstone deletion time and audit event time — are UTC, as RFC 7643 conventions lead connectors to expect. The UTC+8 convention applies to log `@timestamp` only. These are different fields with different consumers and are not reconciled to one zone.

Audit retention is deployment-configurable and defaults to one year, with a floor of **90 days**: a shorter value is refused at startup rather than silently accepted, because retention below that leaves an incident uninvestigable. The retention job logs its schedule at startup and, on each run, the number of rows it deleted and how long it took, so an operator can tell a job that found nothing from one that never ran. Tombstones are retained independently because they preserve non-reassignable resource ids and redacted correlation history. Traceability of a deleted User lives in this stream rather than in a retained copy of the profile: the events carry actor, resource id, operation and changed paths for the full retention period, and survive the resource they describe.

## Operational telemetry

Audit answers "who changed what"; telemetry answers "is the provisioning surface healthy". Expose both, and never conflate them.

Publish metrics through the existing Spring Boot Actuator/Micrometer surface, tagged by endpoint, HTTP method, resource type and status class — never by `userName`, `externalId`, filter text or connector token value; connector identity is carried as the non-secret connector id only:

- **Traffic:** request count per SCIM endpoint and per connector id.
- **Latency:** request duration distribution with percentiles, not averages, for every SCIM endpoint and for Login.
- **Errors:** rate by status class and, for `4xx`, by `scimType`.
- **Saturation:** database connection pool and Redis connection utilisation, plus rejected-request counts from the per-request safety limits.

Alert on the signals that mean an integration is broken or hostile rather than merely busy: a sustained rise in `401` (dead or probing credentials), in `412` and `428` (writers colliding, or a client ignoring the precondition contract), in `409` (a connector re-creating identities it believes are missing), and on the inactivity job failing to run.

The metrics endpoint is not public. It is bound to the deployment's internal surface, requires Admin authority if served over the application chain, and is never reachable through a connector bearer token.

## Error contract

Every SCIM error body has schema `urn:ietf:params:scim:api:messages:2.0:Error`, string `status`, optional standard `scimType` and safe human-readable `detail`.

| Condition | Response |
| --- | --- |
| Missing/invalid/expired connector token | `401`, Bearer challenge; `invalid_token` when credentials were supplied |
| Read-only token attempts mutation | `403`, Bearer `insufficient_scope` |
| Protected Bootstrap/Admin operation | `403`, no `scimType` |
| Unknown/deleted resource | `404` |
| Duplicate live username | `409 uniqueness` |
| Invalid body/schema/value | `400 invalidSyntax` or `400 invalidValue` |
| Invalid filter or filter/operator pair | `400 invalidFilter` |
| Invalid PATCH path | `400 invalidPath` |
| PATCH target not found | `400 noTarget` |
| Schema mutability violation | `400 mutability` |
| Missing exact precondition | `428` with retry detail |
| Stale ETag | `412` |
| Request body too large | `413` |
| Unsupported `/Me` | `501` |

Never echo secret values or reveal whether a password is configured outside the Admin projection.

Application-chain errors keep their existing shape and are not SCIM error bodies. For the self-service change: a wrong current password returns `401` and advances the failure run; a new password failing policy or matching the current one returns `400` naming the unmet rule without echoing either value; any other endpoint attempted while `mustChangePassword` is set returns `403`.

## Delivery plan

Users and Groups are one release capability even if developed in ordered slices. No slice is deployed as a partial public SCIM product.

### Slice 0 — Persistence and stable-identity prefactor

**Blocked by:** none.
**Delivers:** explicit migrations, global resource ids, fresh Bootstrap/Admin seeding, username-independent principal/session/counter keys, the Argon2id encoder and password policy, the configuration-driven authorization matrix, authentication and lockout audit events, the standard lockout values, the 8-hour absolute session bound, ECS-structured logging, and unchanged end-user Login behavior.

Acceptance: username can be changed directly in a test fixture without orphaning counter state; sessions are findable by User id; a stored hash carries the `{argon2id}` prefix and a sub-policy password is refused on every setting path; a session is terminated at the absolute bound as well as the idle bound; the matrix loads at startup, fails fast on an unknown authority and denies an unmatched request; a User locks on the fifth consecutive failure and unlocks automatically 20 minutes later; Login, logout and lockout transitions appear in audit by stable User id; current Login, lockout, CSRF and route authorization tests remain green.

### Slice 0a — Permanent lockout

**Blocked by:** none (changes code already merged by Slice 0; runs in parallel with Slice 1).
**Delivers:** removal of the lockout duration from configuration and from the domain, Admin Unlock as the only lift, session revocation when a lockout is imposed, and a Bootstrap Admin exempt from lockout.

Acceptance: a User locks on the fifth consecutive failure and is still refused with the correct password after an arbitrary clock advance; Unlock is the only lift and clears the failure run with it; imposing the lockout refuses a request on a session held before it; the Bootstrap Admin never locks and each of its failed attempts is audited; no configuration key or domain field expresses a lockout duration; every `LOCKOUT_LIFT` event carries an Admin actor and no expiry lift can be produced.

### Slice 1 — Connector security and public discovery

**Blocked by:** Slice 0.
**Delivers:** connector/token lifecycle backend, ordered SCIM filter chain, public discovery resources, Reserved `/scim` routing and exact capability/schema advertisement.

Acceptance: discovery is public; resource endpoints challenge without a token; read-only/write scope is enforced; expiry, one-time plaintext, revocation and 365-day/14-day bounds are tested.

### Slice 2 — User create/read/search foundation

**Blocked by:** Slice 1.
**Delivers:** User POST/GET, normalized profile persistence, connector aliases, canonical serialization, strong ETags, projection, basic list/search and SCIM errors.

Acceptance: credentialless and credentialed Users can be created; connector aliases are isolated; password never appears in any response/log/audit fixture; responses conform to discovered schema.

### Slice 3 — User conditional PUT/PATCH/DELETE

**Blocked by:** Slice 2.
**Delivers:** exact `If-Match`, replacement semantics, atomic User PATCH, reusable identifiers, tombstones, post-commit session revocation, password history enforcement, `lastAuthenticatedAt` tracking and the configurable 90-day inactivity deactivation job with its priority over connector-asserted `active`.

Acceptance: two-writer race yields one success and one `412`; missing precondition yields `428`; rename preserves application data and revokes sessions; deleted id yields `404`; former username can be recreated; a User past the inactivity window is deactivated with sessions revoked and an audit event, a never-authenticated User is measured from creation, and the Bootstrap Admin is never deactivated.

### Slice 4 — Groups and Admin authority

**Blocked by:** Slice 3.
**Delivers:** Group CRUD/PATCH, direct User membership, read-only User `groups`, Admin authority derivation, protected recovery resources, membership-triggered version/session effects and the configurable 180-day dormant authority revocation job.

Acceptance: Group members cannot be Groups; Admin access follows direct stable-id membership after re-login; membership changes revoke stale Admin sessions; Bootstrap protections fail atomically with `403`; the revocation job drops Admin membership for a dormant User, leaves baseline authority and ordinary Groups intact, exempts the Bootstrap Admin, and removes a re-added membership while the User stays dormant.

### Slice 5 — Complete query protocol

**Blocked by:** Slices 3 and 4.
**Delivers:** full RFC filter parser and PostgreSQL translation, base and resource POST search, sorting, projection and exact pagination/limit semantics for both resource types.

Acceptance: RFC grammar/precedence/value-path fixtures pass; malicious depth/size inputs are bounded; queries are parameterized; base searches correctly handle attributes absent from one type.

### Slice 6 — Operational Accounts page and audit

**Blocked by:** Slices 1–5.
**Delivers:** read-only User/Group operations, Unlock, Admin-forced password change, the authenticated `/change-password` flow, connector/token management, one-time token disclosure and audit UI/API.

Acceptance: no SCIM-owned identity can be edited from the browser; every backend operation independently requires Admin; token plaintext disappears after navigation/refresh and cannot be retrieved again; a flagged User is confined to change-password and logout with every other endpoint returning `403`, including an Admin's own; a successful change clears the flag, revokes all that User's sessions and appears in audit with no password value.

### Slice 7 — Conformance and release hardening

**Blocked by:** all prior slices.
**Delivers:** completed OpenAPI contract, generic RFC conformance fixtures, security/resource limits, operational telemetry with its alerts, documented edge rate-limiting requirement, documentation and full application verification.

Acceptance: Definition of Done below is green. Only then is the SCIM interface releasable.

## Testing decisions

Tests assert external behavior at the highest practical seam:

- full-context MockMvc tests drive the real SCIM security chain, controllers, JSON codec, media types, headers and exception mapping;
- focused domain/application tests pin atomic PATCH, protected resources, authority and version invariants;
- PostgreSQL integration tests, not H2 alone, validate constraints, locking, query semantics and races;
- Redis integration tests prove revocation by stable User id and prove refused/rolled-back writes revoke nothing;
- parameterized RFC fixtures cover schema discovery, CRUD, error bodies, attribute mutability, filter/PATCH ABNF and pagination;
- property/fuzz tests target filter and PATCH parsers with bounded depth and Unicode inputs;
- frontend component tests pin operational rendering and refusal copy;
- Playwright tests exercise Login, authority changes, User/Group visibility, Unlock, Admin-forced password change, the `/change-password` flow and the connector-token lifecycle end to end.

Security-sensitive code requires mutation evidence. PIT targets all changed identity/auth/SCIM classes and tests with the expanded mutator set; every survivor is killed or justified from the mutated line, and line coverage is checked because unmutated void calls can hide missing behavior. Scoped Stryker covers changed frontend authorization and token-management behavior.

## Definition of Done

### Contract and behavior

- All endpoints, schemas, status codes, headers, media types, projection rules and security requirements above are documented in the backend OpenAPI contract.
- Public discovery exactly matches implemented capabilities.
- User and Group CRUD, PATCH, search, sort, pagination, ETags and errors pass generic RFC fixtures.
- Bulk is advertised unsupported and has no partial implementation.
- Connector aliases are isolated; connector deletion removes them.
- Deleted identifiers are reusable while deleted ids remain non-reassignable and return `404`.
- Token expiry and rotation enforce 365-day and 14-day maxima.
- Stable User id keys sessions and application-owned data.
- Every listed security change revokes sessions after commit and no failed change does.
- Inactivity deactivation runs on schedule, measures from last authentication or creation, exempts the Bootstrap Admin, and revokes sessions with an audit event.
- Deactivation outranks a connector re-asserting `active=true`: only an explicit false-to-true transition reactivates and resets the window.
- Dormant authority revocation drops Admin Group membership at the configured window, leaves baseline authority and ordinary Groups intact, exempts the Bootstrap Admin, and re-applies after a connector re-adds the membership.
- A connector-set password sets `mustChangePassword`, and a flagged User who does not change it within the grace period is deactivated with an audit event.
- A new password is refused when it matches any of the User's newest three password hashes, on the self-service path and on every SCIM path alike.
- The password policy enforces the 12-to-256 length bound, accepts every printable and Unicode character including spaces, imposes no composition or expiry rule, and rejects blocklisted values from a local corpus with no candidate or hash leaving the deployment.
- All three scheduled jobs are serialized per job name and cannot overlap across instances.
- Login success and failure, logout, lockout set and lift, forced-change set, self-service change outcomes and every scheduled-job action appear in the audit stream, identified by stable User id with no `userName`.
- A write whose audit insert cannot commit rolls the mutation back and returns an error; a failing failure-event append raises an alert without altering the request's own outcome.
- SCIM-visible timestamps are UTC while log `@timestamp` is UTC+8, and neither is reconciled to the other.
- The authorization matrix is loaded from configuration before traffic is served, fails fast on an unknown authority or malformed rule, denies unmatched requests, and cannot be mutated through any endpoint.
- A second Login invalidates the User's earlier session; lockout applies at 5 failed attempts, never lifts on its own, revokes the User's sessions when imposed, ends only through Admin Unlock, and never applies to the Bootstrap Admin.
- Login refusals for an unknown, inactive or credentialless User perform an equivalent password verification, so refusal timing does not reveal account state.
- An Admin cannot Unlock or force a change on their own account, and the authenticated self-read returns only the caller's record resolved from the session id, with no lockout or failure-count field.
- Audit events carry HTTP method, path and request id; a bulk read is audited once with its result count and filter shape and no literal filter value; the application database role cannot `UPDATE` or `DELETE` audit rows.
- A configured audit retention below 90 days is refused at startup, and the retention job logs its schedule and each run's deleted-row count and duration.
- Client-influenced values reaching a log line are emitted as structured fields with control characters stripped.
- The Bootstrap Admin is seeded with `mustChangePassword` set and is exempt from grace-period deactivation.
- A set `mustChangePassword` flag confines the session to change-password and logout; a successful self-service change clears the flag, revokes every session of that User and records no password value.
- Telemetry publishes traffic, latency, error-class and saturation signals with no identifier, filter text or secret in any tag, the documented alerts exist, and the metrics endpoint is unreachable with a connector token.
- Edge rate-limiting requirements for `/scim/v2/**`, Login and change-password are documented in `infra/` and the deployment README.
- Passwords are hashed with Argon2id at the stated parameters through a prefixed delegating encoder, the password policy is enforced identically on every setting path, and no BCrypt encoder remains.
- Sessions terminate on the 15-minute idle bound and on the 8-hour absolute bound, and HSTS is sent with a one-year max-age and `includeSubDomains`.
- Logs are ECS-structured with request, connector and resource ids in MDC and no identifier, filter text or secret anywhere in a log line.
- Passwords, bearer values and hashes never appear in responses, audit, logs or frontend persistence.

### Backend gates

Run from `backend/` with Java 25 through `./mvnw`:

1. `./mvnw clean verify`
2. `./scripts/semgrep.sh`
3. targeted PIT over every changed auth/identity/SCIM class and test using the expanded mutator set
4. PostgreSQL and Redis integration suites against the local dependencies

All ArchUnit rules, tests and security scans must pass. PIT survivors require explicit semantic-equivalence justification; no-coverage lines in security behavior are failures.

### Frontend gates

Run from `frontend/` with the pinned Node version and installed lockfile:

1. `npm run typecheck`
2. `npm test`
3. `npm run test:arch`
4. `npm run test:e2e`
5. `npm run test:security`
6. `npx fallow audit`
7. scoped Stryker for changed authorization and operational-page behavior
8. `npm run build`

### Documentation and graph

- Update `CONTEXT.md`, backend and frontend `AGENTS.md` contracts only where runtime behavior actually changes.
- Record any implementation-time architectural decision that changes this plan as an ADR.
- Refresh Graphify after every code or documentation change as required by repository policy.

## Out of scope

- Migration, cutover, dual-write or preservation of existing non-Bootstrap Accounts.
- SCIM Bulk.
- Enterprise User or custom schema extensions.
- Nested Groups, transitive membership or dynamic Groups.
- Multi-tenancy or multiple directories per deployment.
- Vendor-specific Microsoft Entra ID, Okta or other connector workarounds.
- OAuth authorization-server flows, refresh tokens or JWT access tokens; connector tokens are locally issued opaque credentials.
- `/Me` self-service.
- SCIM-driven end-user Login or session creation.
- Admin editing of SCIM-owned User, Group or membership data.
- Application-layer request-rate limiting; throttling is enforced at the deployment edge instead.
- MFA and step-up authentication for privileged accounts and actions.
- Periodic access review of Admin membership or connector scope against a declared permission baseline.
- Forced credential change on first login after an admin-issued or seeded credential.
- A public vulnerability-disclosure channel.

## Accepted policy deviations

Reviewed against the IM8 application control catalog on 2026-09-25; the audit is
`artifacts/spec-compliance/spec-compliance-scim-2-0-account-management-specification-plan-2026-09-25-0935.html`.
The following controls are knowingly not met. They are recorded so implementation does not silently re-litigate them, and so a later reviewer sees a decision rather than an oversight.

| Control | Deviation | Rationale |
| --- | --- | --- |
| as-4 Authentication rate-limiting | No application-layer limiter | Enforced at the deployment edge for `/scim/v2/**`, Login and change-password; the application keeps only per-request safety bounds |
| as-8 Secrets management | No secret-store requirement | Deferred by decision: secrets stay deployment environment configuration for now. A store-backed injection path (SSM SecureString or equivalent) and fail-fast on missing values remain open work |
| dp-3 Data in transit encryption | TLS configuration unspecified beyond "TLS at the edge" | Deferred by decision: no minimum version, cipher policy or internal-hop TLS is specified in this plan |
| ac-2 MFA enforcement | Privileged login stays single-factor | Accepted by design; password Login is the deliberate authentication model for this application |
| ac-4 Access review | No periodic privilege re-attestation | Accepted; Admin authority is directory-derived and every change is audited, but no review cycle is specified |
| ac-6 Default credentials | No forced change after a seeded or admin-issued credential | Accepted; the Bootstrap Admin is an operator-held recovery credential and connector-set passwords come from the authoritative directory |
| ac-12 SSO for internal services | No organisational IdP authentication | Accepted by design; SCIM provisions identity while password Login remains the application's own authentication interface |
| st-3 Public vulnerability disclosure | No `security.txt` or reporting channel | Accepted; out of scope for this capability |

Controls raised by the same audit and now specified rather than deviated: as-4 edge enforcement, ac-3 inactivity deactivation, as-15 forced password change, lm-16 operational telemetry, and the credential and cryptographic policy above (as-5, as-6, as-10, as-11, as-14, lm-15, ck-2, ck-4).

## Settled deviations and interpretations

- Tombstones are retained, but unlike the earlier draft they do not reserve former identifiers; this follows RFC 7644 section 3.6.
- Bearer tokens are operator-managed and long-lived relative to browser tokens, but remain finite as RFC 7644 requires: 365 days maximum with 14 days maximum overlap.
- Strong ETags are used even though RFC 7644 examples show weak tags, because HTTP `If-Match` uses strong comparison and this profile requires exact lost-update protection.
- PUT omission clears optional profile values but not an omitted password, because a write-only password cannot be safely read-modify-written by a client.
- Full filter grammar means complete syntax and semantics over the attributes this service actually advertises; it does not make unsupported schema attributes or password values queryable.
- Per-request parser, body and page limits are safety bounds, not rate limiting; rate limiting itself is an edge responsibility documented above.

### Deviations from the Standalone User Access Control standard

These are conscious departures from the organisational standard, distinct from the RFC interpretations above. Each is accepted with its reasoning recorded here rather than left as an apparent oversight.

- **No admin-initiated password reset tokens.** The standard requires paired token-issuance and token-redemption endpoints. Admin-forced change plus the authenticated `/change-password` flow achieves the same outcome — an Admin restores access without ever learning or transporting the credential — with strictly less secret handling, since no reset token exists to be issued, displayed, logged or replayed. The accepted cost is that a User who has forgotten their password cannot self-recover and needs a connector to set a new one; there is no email-based recovery path.
- **`userName` is mutable.** The standard rejects a username change outright. SCIM's core schema defines `userName` as read-write and directory renames are a routine event, so rejecting them would break interoperability. The standard's underlying concern — renames orphaning state — is answered structurally instead: the stable SCIM `id` is the principal and the key for all application-owned data, Redis sessions are id-indexed, and a rename revokes the User's sessions.
- **Email addresses are not unique across Users.** The standard rejects a duplicate email. Its rationale is that email serves as a login and password-reset identifier; here it is neither, because Login is `userName`-only and no email recovery flow exists. RFC 7643 additionally defines `emails` uniqueness as `none`. Uniqueness is therefore enforced only within a User, as at most one primary and no duplicate `(type,value)` pair.
- **A deleted `userName` is reusable.** The standard blocks reuse by consulting deleted-user tombstones. This profile follows RFC 7644 section 3.6 instead, and answers the standard's audit-confusion concern directly: audit resolves every actor and resource by stable id and never by `userName`, so a reused name cannot merge two identities in the trail.
- **Deletion is not a soft delete and the tombstone holds no profile.** The standard requires retaining the deleted user's details. This profile keeps a privacy-minimal tombstone and locates traceability in the append-only audit stream, which retains actor, resource id, operation and changed paths for the full retention period independently of the resource. Retaining a readable profile after a SCIM DELETE would keep personal data the deletion is understood to have removed.
- **A read-write connector can grant Admin authority.** The standard reserves role assignment to role administrators. This is intended here: the directory is authoritative for membership, and Admin authority is defined as direct membership of the Admin Group, so a connector with write scope necessarily can confer it. The consequence is accepted deliberately — a compromised read-write token is a privilege-escalation path — and is bounded by token scoping, the 365-day/14-day lifetime limits, immediate revocation, session revocation on every membership change, and an audit event for each one. Authority assignment is not gated by any additional local switch.
- **No account-owner notifications.** Deferred rather than settled; the standard's notification requirement is not yet addressed and no channel exists.
