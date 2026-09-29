package com.example.backend.scim.controller;

import com.example.backend.scim.application.NewScimUser;
import com.example.backend.scim.application.ScimUserListing;
import com.example.backend.scim.application.ScimUserReplacement;
import com.example.backend.scim.application.ScimUserResource;
import com.example.backend.scim.application.ScimUserService;
import com.example.backend.scim.domain.AuthenticatedConnector;
import com.example.backend.scim.domain.ScimPageRequest;
import com.example.backend.scim.domain.ScimUserPatchOperation;
import com.example.backend.scim.domain.ScimVersionPrecondition;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tools.jackson.databind.JsonNode;

/**
 * The SCIM User endpoints a connector calls: create, retrieve one, retrieve a page, replace,
 * patch.
 *
 * <p>Authentication and write scope are already decided when a request reaches here — the
 * namespace's filter chain authenticated the bearer token and turned a read-only token's
 * mutation away before any handler ran — so there is no credential check in this class and
 * there must not be one: a per-handler check would cover the handlers whose author
 * remembered it.
 *
 * <p>The connector arrives as the authenticated principal and is passed on to the use case
 * rather than read from a static context there, because an {@code externalId} is
 * connector-scoped and an audit event names the actor. Both would be silent defects if the
 * identity were optional.
 */
@RestController
@RequestMapping(ScimSchemas.BASE_PATH + "/Users")
class ScimUserController {

    /** Query parameters this ticket does not implement, each advertised as unsupported. */
    private static final String FILTERING_UNSUPPORTED =
            "This service does not support filtering; ServiceProviderConfig advertises"
                    + " filter.supported as false.";

    private static final String SORTING_UNSUPPORTED =
            "This service does not support sorting; ServiceProviderConfig advertises"
                    + " sort.supported as false.";

    private final ScimUserService users;

    ScimUserController(ScimUserService users) {
        this.users = users;
    }

    /**
     * Creates a User.
     *
     * <p>{@code 201} with the canonical resource, its {@code Location} and a strong
     * {@code ETag} — the three things RFC 7644 §3.3 requires of a create — and no password,
     * whatever was sent, because the projection this renders has no field for one.
     */
    @PostMapping(
            consumes = {ScimSchemas.MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE},
            produces = {ScimSchemas.MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Map<String, Object>> create(
            @AuthenticationPrincipal AuthenticatedConnector connector,
            @RequestBody JsonNode body,
            @RequestParam(required = false) String attributes,
            @RequestParam(required = false) String excludedAttributes) {
        ScimAttributeProjection projection =
                ScimAttributeProjection.ofUser(attributes, excludedAttributes);
        NewScimUser command = ScimUserRequestReader.readCreate(body);
        ScimUserResource created = users.create(connector, command);
        String baseUri = baseUri();
        return ResponseEntity.created(java.net.URI.create(
                        ScimUserRenderer.location(baseUri, created)))
                .eTag(ScimUserRenderer.etag(created.version()))
                .body(projection.apply(ScimUserRenderer.render(created, baseUri)));
    }

    /**
     * One User by its id.
     *
     * <p>Not audited, deliberately: a single-resource read is the ordinary unit of
     * provisioning traffic, and recording it would bury the collection reads that indicate
     * an enumeration. The absence of an audit call is in the use case, where the read is.
     */
    @GetMapping(
            path = "/{id}",
            produces = {ScimSchemas.MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Map<String, Object>> byId(
            @AuthenticationPrincipal AuthenticatedConnector connector,
            @PathVariable String id,
            @RequestParam(required = false) String attributes,
            @RequestParam(required = false) String excludedAttributes) {
        ScimAttributeProjection projection =
                ScimAttributeProjection.ofUser(attributes, excludedAttributes);
        ScimUserResource user = users.findById(connector, resourceId(id))
                .orElseThrow(() -> ScimErrorException.notFound("No User has that id."));
        String baseUri = baseUri();
        return ResponseEntity.ok()
                .eTag(ScimUserRenderer.etag(user.version()))
                .header(HttpHeaders.LOCATION, ScimUserRenderer.location(baseUri, user))
                .body(projection.apply(ScimUserRenderer.render(user, baseUri)));
    }

    /**
     * A page of Users.
     *
     * <p>{@code filter}, {@code sortBy} and {@code sortOrder} are refused rather than
     * ignored while unimplemented, because an ignored one returns every User to a caller
     * that asked for some and believes the answer was selected for it. Discovery advertises
     * both as unsupported, so the refusal is what a connector reading discovery expects.
     *
     * <p>{@code startIndex} and {@code count} are read as strings and parsed here so a
     * non-numeric value is a SCIM {@code 400 invalidValue} rather than a binder's error in
     * some other shape.
     */
    @GetMapping(produces = {ScimSchemas.MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Map<String, Object>> page(
            @AuthenticationPrincipal AuthenticatedConnector connector,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) String startIndex,
            @RequestParam(required = false) String count,
            @RequestParam(required = false) String attributes,
            @RequestParam(required = false) String excludedAttributes) {
        if (filter != null) {
            throw ScimErrorException.unsupportedQuery(FILTERING_UNSUPPORTED);
        }
        if (sortBy != null || sortOrder != null) {
            throw ScimErrorException.unsupportedQuery(SORTING_UNSUPPORTED);
        }
        ScimAttributeProjection projection =
                ScimAttributeProjection.ofUser(attributes, excludedAttributes);
        ScimPageRequest page = ScimPageRequest.of(
                integer(startIndex, "startIndex"), integer(count, "count"));
        ScimUserListing listing = users.list(connector, page);
        return ResponseEntity.ok(
                ScimUserRenderer.renderList(listing, baseUri(), projection));
    }

    /**
     * Replaces a User — PUT.
     *
     * <p>Requires exactly one current {@code If-Match}; see {@link ScimVersionPrecondition}. The
     * header is captured here and evaluated by the use case once the User is found, so an id that
     * names nothing is a {@code 404} whatever the header says. The body is read BEFORE the User is
     * looked up, so a malformed body is refused without touching the directory.
     *
     * <p>{@code 200} with the canonical resource, its {@code Location} and its new {@code ETag}.
     */
    @PutMapping(
            path = "/{id}",
            consumes = {ScimSchemas.MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE},
            produces = {ScimSchemas.MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Map<String, Object>> replace(
            @AuthenticationPrincipal AuthenticatedConnector connector,
            @PathVariable String id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) List<String> ifMatch,
            @RequestBody JsonNode body,
            @RequestParam(required = false) String attributes,
            @RequestParam(required = false) String excludedAttributes) {
        ScimAttributeProjection projection =
                ScimAttributeProjection.ofUser(attributes, excludedAttributes);
        UUID userId = resourceId(id);
        ScimUserReplacement replacement = ScimUserRequestReader.readReplace(body);
        ScimUserResource written = users.replace(
                        connector, userId, ScimVersionPrecondition.ofIfMatch(ifMatch), replacement)
                .orElseThrow(ScimUserController::noSuchUser);
        return ok(written, projection);
    }

    /**
     * Applies PATCH operations to a User, all or nothing — PATCH.
     *
     * <p>{@code 200} with the patched resource rather than a {@code 204}, which RFC 7644 §3.5.2
     * permits either of: a client that received no body would have to re-read the resource to
     * learn the new {@code ETag} before its next conditional write.
     */
    @PatchMapping(
            path = "/{id}",
            consumes = {ScimSchemas.MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE},
            produces = {ScimSchemas.MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Map<String, Object>> patch(
            @AuthenticationPrincipal AuthenticatedConnector connector,
            @PathVariable String id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) List<String> ifMatch,
            @RequestBody JsonNode body,
            @RequestParam(required = false) String attributes,
            @RequestParam(required = false) String excludedAttributes) {
        ScimAttributeProjection projection =
                ScimAttributeProjection.ofUser(attributes, excludedAttributes);
        UUID userId = resourceId(id);
        List<ScimUserPatchOperation> operations = ScimUserPatchReader.readPatch(body);
        ScimUserResource written = users.patch(
                        connector, userId, ScimVersionPrecondition.ofIfMatch(ifMatch), operations)
                .orElseThrow(ScimUserController::noSuchUser);
        return ok(written, projection);
    }

    /**
     * Deletes a User — DELETE.
     *
     * <p>{@code 204} with no body, as RFC 7644 §3.6 requires, under the same {@code If-Match} rules
     * as PUT and PATCH. Every later operation on the id is a {@code 404}, and the former
     * {@code userName} is free for the next create.
     */
    @DeleteMapping(path = "/{id}")
    ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedConnector connector,
            @PathVariable String id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) List<String> ifMatch) {
        if (!users.delete(connector, resourceId(id), ScimVersionPrecondition.ofIfMatch(ifMatch))) {
            throw noSuchUser();
        }
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Map<String, Object>> ok(
            ScimUserResource user, ScimAttributeProjection projection) {
        String baseUri = baseUri();
        return ResponseEntity.ok()
                .eTag(ScimUserRenderer.etag(user.version()))
                .header(HttpHeaders.LOCATION, ScimUserRenderer.location(baseUri, user))
                .body(projection.apply(ScimUserRenderer.render(user, baseUri)));
    }

    /** The one refusal for every "no User here" case, so a caller cannot tell them apart. */
    private static ScimErrorException noSuchUser() {
        return ScimErrorException.notFound("No User has that id.");
    }

    /**
     * The id as a resource id.
     *
     * <p>A malformed id is a {@code 404} rather than a {@code 400}: every SCIM id this
     * service issues is a UUID, so a value that is not one names no resource, and telling a
     * caller that its id was the wrong SHAPE reveals what shape the real ones have. The
     * answer for "this id names nothing" is the same either way.
     */
    private static UUID resourceId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException notAnId) {
            throw ScimErrorException.notFound("No User has that id.");
        }
    }

    private static Integer integer(String value, String parameter) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException notANumber) {
            throw ScimErrorException.invalidValue(parameter + " must be an integer.");
        }
    }

    /**
     * This service's SCIM base URI, absolute, taken from the request.
     *
     * <p>From the request rather than from configuration so a deployment behind a
     * host-rewriting proxy renders the URI its clients actually use, and so there is no
     * second place the base path is written.
     */
    private static String baseUri() {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(ScimSchemas.BASE_PATH)
                .build()
                .toUriString();
    }
}
