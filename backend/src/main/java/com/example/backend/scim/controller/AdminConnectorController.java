package com.example.backend.scim.controller;

import com.example.backend.scim.application.ConnectorAdministrationService;
import com.example.backend.scim.application.ConnectorSummary;
import com.example.backend.scim.application.IssuedConnectorToken;
import com.example.backend.scim.application.UnknownConnectorException;
import com.example.backend.scim.domain.InvalidConnectorTokenLifetimeException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.example.backend.scim.domain.ConnectorTokenScope;

/**
 * Inbound HTTP adapter for connector and token administration.
 *
 * <p>Under {@code /api/admin}, so it is on the APPLICATION chain — session
 * authenticated, CSRF protected, restricted to {@code ROLE_ADMIN} by the filter
 * chain. That is deliberate and is the opposite of the namespace it manages
 * credentials for: minting a SCIM token is an administrator's browser action, not
 * something a connector may do for itself, so no bearer token reaches this
 * controller and no connector can issue itself a wider one. Authorization is not
 * expressed here, for the reason {@code AdminAccountController} does not express it
 * either: every access rule stays readable in one place.
 *
 * <p>Two response shapes, and the difference is the whole point.
 * {@link ConnectorSummary} has no field a credential could occupy and is what a
 * listing returns; {@link IssuedConnectorToken} carries the plaintext and is returned
 * only by issue and rotate, on a response marked {@code Cache-Control: no-store} so
 * it is not written to a shared cache on its way to the browser.
 */
@RestController
@RequestMapping("/api/admin/connectors")
public class AdminConnectorController {

    private final ConnectorAdministrationService connectors;

    public AdminConnectorController(ConnectorAdministrationService connectors) {
        this.connectors = connectors;
    }

    /** A connector to create. */
    public record CreateConnectorRequest(
            @NotBlank @Size(max = 200) String displayName) {
    }

    /**
     * A token to mint.
     *
     * @param lifetimeDays how long it should live, or {@code null} for the default of
     *                     365 days; the policy refuses more, so a request for two
     *                     years is a {@code 400} rather than a silently shortened token
     */
    public record IssueTokenRequest(
            @NotNull ConnectorTokenScope scope,
            @Positive Integer lifetimeDays) {
    }

    /**
     * A rotation.
     *
     * @param overlapDays how long the old token should keep working, clamped to 14 and
     *                    to whatever life the old token had left; {@code null} or zero
     *                    ends it immediately
     */
    public record RotateTokenRequest(Integer overlapDays) {
    }

    @GetMapping
    public List<ConnectorSummary> listConnectors() {
        return connectors.listConnectors();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConnectorSummary create(
            @Valid @RequestBody CreateConnectorRequest request, Principal principal) {
        return connectors.create(request.displayName(), principal.getName());
    }

    /**
     * Deletes a connector, revoking every token it holds and removing every alias it
     * owns. Users and Groups are untouched.
     */
    @DeleteMapping("/{connectorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID connectorId, Principal principal) {
        connectors.delete(connectorId, principal.getName());
    }

    /**
     * Mints a token. The plaintext in this response is the only copy that will ever
     * exist — there is no endpoint that returns it again.
     */
    @PostMapping("/{connectorId}/tokens")
    public ResponseEntity<IssuedConnectorToken> issueToken(
            @PathVariable UUID connectorId,
            @Valid @RequestBody IssueTokenRequest request,
            Principal principal) {
        return disclosed(connectors.issueToken(
                connectorId, request.scope(), days(request.lifetimeDays()), principal.getName()));
    }

    /**
     * Replaces a token, leaving the old one usable until the overlap window ends. The
     * old token's expiry only ever moves earlier.
     */
    @PostMapping("/{connectorId}/tokens/{tokenId}/rotate")
    public ResponseEntity<IssuedConnectorToken> rotateToken(
            @PathVariable UUID connectorId,
            @PathVariable UUID tokenId,
            @Valid @RequestBody(required = false) RotateTokenRequest request,
            Principal principal) {
        Integer overlapDays = request == null ? null : request.overlapDays();
        return disclosed(
                connectors.rotateToken(tokenId, days(overlapDays), principal.getName()));
    }

    /** Revokes a token, effective immediately. */
    @PostMapping("/{connectorId}/tokens/{tokenId}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeToken(
            @PathVariable UUID connectorId, @PathVariable UUID tokenId, Principal principal) {
        connectors.revokeToken(tokenId, principal.getName());
    }

    /**
     * The one response shape that carries plaintext, and the one place
     * {@code no-store} is set.
     *
     * <p>A single helper rather than the header on each of the two methods, so a third
     * disclosing endpoint cannot be added without going through it. {@code no-store}
     * rather than {@code no-cache}: the latter permits storing the response and
     * revalidating, which is storing a credential.
     */
    private static ResponseEntity<IssuedConnectorToken> disclosed(IssuedConnectorToken token) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(token);
    }

    private static Duration days(Integer days) {
        return days == null ? null : Duration.ofDays(days);
    }

    @ExceptionHandler(UnknownConnectorException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void unknownConnector() {
        // The caller is already an administrator, so naming what is missing reveals
        // nothing they could not read from the listing.
    }

    /**
     * A lifetime past the ceiling is a bad request rather than a conflict: the request
     * itself is out of range, and reporting it lets the Admin see that the token they
     * would have received is not the one they asked for.
     */
    @ExceptionHandler(InvalidConnectorTokenLifetimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void invalidLifetime() {
    }
}
