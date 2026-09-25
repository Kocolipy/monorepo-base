package com.example.backend.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every inbound request a correlation id and puts it in the logging
 * context for the duration of that request.
 *
 * <p>Ordered ahead of everything, the security chain included, so a request
 * refused before it reaches a handler — a failed login, a missing CSRF token, a
 * wrong role — is logged under an id too. Those are the records an investigation
 * starts from, and they are exactly the ones an ordering mistake would leave
 * uncorrelated.
 *
 * <p>The id is minted here and never read from the request. A caller-supplied
 * correlation header would let a client stamp its own value on this service's
 * records — repeat one to merge unrelated requests in a log search, or pick one
 * that collides with another tenant's — and this service sits behind no proxy
 * whose header it has agreed to trust. When such a contract exists, trusting it
 * is a deliberate change here, not a default.
 *
 * <p>The id is also stashed on the request, so a container error dispatch, which
 * runs after this filter's own scope has closed, reports the same id as the
 * request that failed rather than minting a second one for the same exchange.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    /**
     * Where the minted id is kept for the error dispatch. Not a header: it is this
     * service's internal correlation handle and no client is promised it.
     */
    static final String REQUEST_ID_ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String requestId = existingRequestId(request);
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
            request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        }

        try (LogContext.Scope scope = LogContext.requestId(requestId)) {
            chain.doFilter(request, response);
        }
    }

    /**
     * Runs on the error dispatch as well. Without this the response a client
     * actually receives — the error page, and anything logged while rendering it —
     * would carry no id at all.
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    private static String existingRequestId(HttpServletRequest request) {
        return request.getAttribute(REQUEST_ID_ATTRIBUTE) instanceof String existing
                ? existing
                : null;
    }
}
