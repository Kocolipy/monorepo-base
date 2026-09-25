package com.example.backend.scim.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;

/**
 * The three answers a SCIM request gets when its credential is the problem, as
 * RFC 6750 spells them.
 *
 * <p>One class, so the header a connector parses to find out what went wrong is
 * written in exactly one place. A connector diagnoses itself from
 * {@code WWW-Authenticate}: no {@code error} means "you sent no credential", and
 * {@code invalid_token} means "the one you sent is not accepted". Two call sites
 * spelling that differently would make an integration's failure mode depend on which
 * branch it hit.
 *
 * <p>No response body. The SCIM error document has its own renderer, which arrives
 * with the resource endpoints; a status and a challenge are the whole of what an
 * unauthenticated caller is entitled to, and a body describing the refusal is how a
 * probe learns whether a token ever existed.
 */
final class ScimBearerChallenge {

    private static final String BEARER = "Bearer";

    /** Why the token was refused. Never which of the several reasons it was. */
    private static final String INVALID_TOKEN = "Bearer error=\"invalid_token\"";

    private static final String INSUFFICIENT_SCOPE = "Bearer error=\"insufficient_scope\"";

    private ScimBearerChallenge() {
    }

    /**
     * The challenge for a request that presented no bearer credential: {@code 401}
     * with a bare {@code Bearer} challenge naming the scheme and nothing else.
     */
    static void missingCredential(HttpServletResponse response) {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, BEARER);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * The refusal for a malformed, unknown, expired or revoked token, and for one
     * whose connector has been deleted. All four are this one answer: telling them
     * apart would report whether a credential ever existed and whether it was revoked
     * on purpose.
     */
    static void invalidToken(HttpServletResponse response) {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, INVALID_TOKEN);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * The refusal for a valid read-only token attempting a mutation: {@code 403},
     * because the credential is genuine and it is the action that is not permitted.
     * A {@code 401} here would send a working integration into a credential-refresh
     * loop over a request that will never be allowed.
     */
    static void insufficientScope(HttpServletResponse response) {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, INSUFFICIENT_SCOPE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
}
