package com.example.backend.scim.config;

import com.example.backend.observability.AccessRefusalLog;
import com.example.backend.observability.AccessRefusalLog.Refusal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * The SCIM chain's answer to a request that presented no bearer credential: the bare
 * {@code Bearer} challenge, after one refusal record. A request whose credential was presented
 * and refused never reaches here — {@link ScimBearerAuthenticationFilter} answers and records
 * that one itself.
 */
class ScimBearerEntryPoint implements AuthenticationEntryPoint {

    private final AccessRefusalLog refusals;

    ScimBearerEntryPoint(AccessRefusalLog refusals) {
        this.refusals = refusals;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) {
        refusals.record(request, Refusal.BEARER_MISSING);
        ScimBearerChallenge.missingCredential(response);
    }
}
