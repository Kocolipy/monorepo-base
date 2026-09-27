package com.example.backend.scim.controller;

import com.example.backend.scim.domain.DuplicateDisplayNameException;
import com.example.backend.scim.domain.DuplicateUserNameException;
import com.example.backend.scim.domain.InvalidPreconditionException;
import com.example.backend.scim.domain.PasswordHistoryPolicy;
import com.example.backend.scim.domain.PasswordReusedException;
import com.example.backend.scim.domain.PreconditionFailedException;
import com.example.backend.scim.domain.PreconditionRequiredException;
import com.example.backend.scim.domain.ProtectedResourceException;
import com.example.backend.scim.domain.ScimPatchRefusedException;
import com.example.backend.scim.domain.UnknownGroupMemberException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Renders every refusal this slice's handlers produce as the one SCIM error document.
 *
 * <p>Scoped to this package by {@code basePackages}, deliberately. The application
 * chain's errors keep their existing shape — a SCIM error body on a browser request
 * would be a change to the SPA's contract — so this advice must not be global. What
 * that scoping costs is that a refusal raised BEFORE a handler is selected (an
 * unsupported method on a mapped path, an unsupported content type) is not rendered
 * here; those are refusals of the dispatcher rather than of these endpoints, and the
 * conformance-fixture ticket owns making the whole namespace's error surface uniform.
 *
 * <p>The body is built as an ordered map rather than a record so the field order is
 * {@code schemas}, {@code status}, {@code scimType}, {@code detail} as RFC 7644's
 * examples have it, and so {@code scimType} can be omitted entirely — rather than
 * rendered as null — where the RFC defines none for the condition.
 */
@RestControllerAdvice(basePackages = "com.example.backend.scim.controller")
class ScimExceptionHandler {

    /** Every refusal this slice raises deliberately. */
    @ExceptionHandler(ScimErrorException.class)
    ResponseEntity<Map<String, Object>> handle(ScimErrorException refusal) {
        return render(refusal);
    }

    /**
     * A {@code userName} already taken, translated at the boundary.
     *
     * <p>The domain exception carries no detail naming the value, and neither does this:
     * the caller knows which {@code userName} it sent, and echoing it back would put it
     * in a response body and in every log that records one.
     */
    @ExceptionHandler(DuplicateUserNameException.class)
    ResponseEntity<Map<String, Object>> handle(DuplicateUserNameException duplicate) {
        return render(ScimErrorException.uniqueness(
                "A User with the requested userName already exists."));
    }

    /**
     * A Group {@code displayName} already taken.
     *
     * <p>RFC 7643 does not make {@code displayName} unique; this directory does, because a Group's
     * membership confers authority and two Groups an administrator reads as the same name is how
     * membership of the wrong one gets granted. Advertised in the Group schema as
     * {@code uniqueness=server}, so a connector is told rather than discovering it here.
     */
    @ExceptionHandler(DuplicateDisplayNameException.class)
    ResponseEntity<Map<String, Object>> handle(DuplicateDisplayNameException duplicate) {
        return render(ScimErrorException.uniqueness(
                "A Group with the requested displayName already exists."));
    }

    /**
     * A member that is not a live User.
     *
     * <p>One message for three causes — an id naming a Group, a deleted User, or nothing at all —
     * because distinguishing them would disclose the existence and the type of resources the caller
     * has not been shown. The id is not echoed: the caller sent it, and an error body is also a log
     * line.
     */
    @ExceptionHandler(UnknownGroupMemberException.class)
    ResponseEntity<Map<String, Object>> handle(UnknownGroupMemberException unknownMember) {
        return render(ScimErrorException.invalidValue(
                "Every Group member must reference a live User."));
    }

    /**
     * A write against an existing resource with no {@code If-Match}. The detail says what to do,
     * because a client that has never sent the header needs to learn the contract, not just that
     * it broke it.
     */
    @ExceptionHandler(PreconditionRequiredException.class)
    ResponseEntity<Map<String, Object>> handle(PreconditionRequiredException missing) {
        return render(ScimErrorException.preconditionRequired(
                "This write requires an If-Match precondition: GET the resource and retry with"
                        + " its ETag in an If-Match header."));
    }

    /** An {@code If-Match} that is a wildcard, a list, repeated, or not an entity tag. */
    @ExceptionHandler(InvalidPreconditionException.class)
    ResponseEntity<Map<String, Object>> handle(InvalidPreconditionException invalid) {
        return render(ScimErrorException.invalidValue(
                "If-Match must carry exactly one strong ETag; '*' and lists are not accepted."));
    }

    /** An {@code If-Match} naming a version another write has already replaced. */
    @ExceptionHandler(PreconditionFailedException.class)
    ResponseEntity<Map<String, Object>> handle(PreconditionFailedException stale) {
        return render(ScimErrorException.preconditionFailed(
                "The resource has changed since that ETag was issued; GET it and retry."));
    }

    /** A password the User has used recently. Names the rule, never the value. */
    @ExceptionHandler(PasswordReusedException.class)
    ResponseEntity<Map<String, Object>> handle(PasswordReusedException reused) {
        return render(ScimErrorException.invalidValue(
                "The password matches one of the User's " + PasswordHistoryPolicy.RETAINED
                        + " most recent passwords; a new value is required."));
    }

    /** A PATCH operation the stored resource cannot accept. */
    @ExceptionHandler(ScimPatchRefusedException.class)
    ResponseEntity<Map<String, Object>> handle(ScimPatchRefusedException refused) {
        return render(switch (refused.reason()) {
            case MUTABILITY -> ScimErrorException.mutability(refused.getMessage());
            case NO_TARGET -> ScimErrorException.noTarget(refused.getMessage());
        });
    }

    /**
     * A write aimed at a resource reserved for deployment recovery.
     *
     * <p>{@code mutability}, which is SCIM's {@code scimType} for an attempt to change something
     * that cannot be changed, with a {@code 400}. Deliberately NOT a {@code 403}: the caller's token
     * may be a perfectly valid read-write one, and the refusal is about the target rather than about
     * the credential — answering 403 would send an integrator to re-check its token scope.
     *
     * <p>The detail says which kind of resource was protected but not which resource, and nothing
     * about why this deployment reserves it.
     */
    @ExceptionHandler(ProtectedResourceException.class)
    ResponseEntity<Map<String, Object>> handle(ProtectedResourceException protectedResource) {
        return render(ScimErrorException.mutability(
                switch (protectedResource.reservedName()) {
                    case BOOTSTRAP_ADMIN ->
                            "This User is reserved for deployment recovery; its attributes and its"
                                    + " Group membership cannot be changed.";
                    case ADMIN_GROUP ->
                            "This Group is reserved for deployment recovery; it cannot be renamed"
                                    + " or deleted, though its ordinary membership may change.";
                }));
    }

    private static ResponseEntity<Map<String, Object>> render(ScimErrorException refusal) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("schemas", List.of(ScimSchemas.ERROR));
        // A string, not a number: RFC 7644 §3.12 defines status as a string, and a
        // conformance client that parses it as one fails on an integer.
        body.put("status", String.valueOf(refusal.status().value()));
        if (refusal.scimType() != null) {
            body.put("scimType", refusal.scimType());
        }
        body.put("detail", refusal.detail());
        return ResponseEntity.status(refusal.status())
                .header("Content-Type", ScimSchemas.MEDIA_TYPE)
                .body(body);
    }
}
