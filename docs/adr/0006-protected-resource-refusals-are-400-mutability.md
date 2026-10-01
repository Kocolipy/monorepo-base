# 6. Protected-resource refusals are `400 mutability`, not `403`

Date: 2026-10-01

## Status

Accepted. Records a departure from the SCIM plan that spans #14 (Group CRUD and
protected recovery resources), #16 (DELETE) and #24 (conformance fixtures).

## Context

Two resources exist to keep a deployment recoverable: the Bootstrap Admin, and
the Admin group's Bootstrap membership. The plan
(`/docs/specs/scim-v2-account-management-plan.md`) said SCIM writes against them
return `403` with no `scimType`. That rule appears in its Bootstrap Admin section, its error table and the
acceptance list for Slice 4 (Groups and Admin authority).

#14 implemented the refusal as `400` with `scimType: mutability`, and #16 extended
it to `DELETE`. `ScimGroupProvisioningIntegrationTests` and
`backend/docs/openapi.yaml` pin that response, and #24's OpenAPI contract check
holds the document to the implementation. The plan was never updated to match,
and #49 left the mismatch to this sweep.

## Decision

A SCIM write aimed at a reserved resource returns `400` with
`scimType: mutability`. This covers modifying or deleting the Bootstrap Admin,
renaming or deleting the Admin group, and removing the Bootstrap Admin's
membership. The detail says which kind of resource was protected, but not which
resource or why. `ScimExceptionHandler` maps `ProtectedResourceException` to it.

`mutability` is RFC 7644's error for an attempt to change something that cannot
be changed, and that is what happened. A `403` is about the credential. A
connector that got one from a valid read-write token would re-check its token
scope, which is the wrong investigation. Here the refusal concerns the target.

## Consequences

- The same token gets `403` from SCIM only as Bearer `insufficient_scope`, a
  read-only token attempting a mutation. A `403` from SCIM therefore always means
  the credential, and a `400 mutability` always means the target.
- Connectors that treat `mutability` as non-retryable stop retrying, which is
  correct: the refusal never succeeds later.
- The plan's three `403` statements now point here instead of stating the old rule.
