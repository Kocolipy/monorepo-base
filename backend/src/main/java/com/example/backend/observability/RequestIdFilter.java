package com.example.backend.observability;

import com.example.backend.observability.LogEvent.Category;
import com.example.backend.observability.LogEvent.ErrorCategory;
import com.example.backend.observability.LogEvent.Operation;
import com.example.backend.observability.LogEvent.Type;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Gives every inbound request a correlation id, puts it in the logging context for
 * the duration of that request, and writes the request's one record when it ends.
 *
 * <p>Ordered ahead of the security chain, so a request refused before it reaches a
 * handler — a failed login, a missing CSRF token, a wrong role — is logged under an
 * id, and gets its request record, too. Those are the records an investigation
 * starts from, and they are exactly the ones an ordering mistake would leave
 * uncorrelated. It runs immediately INSIDE Boot's {@code ServerHttpObservationFilter}
 * ({@code HIGHEST_PRECEDENCE + 1}), not ahead of it: that filter opens the request's
 * span, and only inside the span's scope does the request record carry the
 * {@code trace.id} every other record of the request carries.
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
 *
 * <h2>The request record</h2>
 *
 * <p>One per request, written from the {@code finally} of the request dispatch, so a
 * refusal by the security chain and an exception escaping a handler are recorded as
 * surely as a success. Never on the error dispatch: that is the same exchange's
 * second pass, and recording it would count one request twice. It carries the
 * method, the matched route TEMPLATE, the status, the duration and the outcome, at
 * {@code INFO} below {@code 400}, {@code WARN} for a {@code 4xx} and {@code ERROR} for a
 * {@code 5xx} — a {@code 5xx} carrying the {@code error.*} classification, or at
 * {@code WARN} when the handler that answered it already wrote its {@code ERROR}
 * ({@link RequestFault}).
 *
 * <p>What it never carries is the request's own text: no raw path, query string,
 * header, cookie, body or client address. The route is the template Spring matched
 * ({@code /scim/v2/Users/{id}}) or {@link #UNMATCHED} when nothing did, so an id or
 * a filter expression in the URL has no way in, and the method is published only
 * from a fixed set because a client chooses it.
 *
 * <p>The liveness and readiness probes and the Prometheus scrape get no record: they
 * arrive every few seconds from infrastructure rather than from a caller, and a
 * record for each would bury the traffic the stream exists to show. Their status
 * and timing are already on {@code http.server.requests}.
 *
 * <p>There is no separate start record. The end record carries everything a start
 * record would — method and route — plus the duration only the end can know, and
 * one record per request rather than two keeps the stream half the size; see
 * {@code /docs/adr/0003-ecs-structured-logging-with-redaction.md}.
 */
@Component
@Order(RequestIdFilter.ORDER)
public class RequestIdFilter extends OncePerRequestFilter {

    /**
     * Immediately inside {@code ServerHttpObservationFilter}, which Boot registers at
     * {@code HIGHEST_PRECEDENCE + 1}, and far ahead of the security chain.
     */
    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 2;

    /** The route of a request no handler mapping matched, the security chain's refusals among them. */
    static final String UNMATCHED = "unmatched";

    /** The method of a request whose method is none of {@link #METHODS}. */
    static final String OTHER_METHOD = "_OTHER";

    /** The methods a record may name. Anything else a client sends is {@link #OTHER_METHOD}. */
    static final Set<String> METHODS = Set.of(
            "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE", "CONNECT");

    /**
     * Where the minted id is kept for the error dispatch. Not a header: it is this
     * service's internal correlation handle and no client is promised it.
     */
    static final String REQUEST_ID_ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";

    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

    private static final String HEALTH = "/actuator/health";

    private static final String PROMETHEUS = "/actuator/prometheus";

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
            long started = System.nanoTime();
            boolean escaped = true;
            try {
                chain.doFilter(request, response);
                escaped = false;
            } finally {
                if (recorded(request)) {
                    record(request, escaped ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                            : response.getStatus(), started);
                }
            }
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

    /**
     * Writes the request's record. An exception escaping the chain has not set a status
     * yet — the container answers {@code 500} after this returns — so it is recorded as
     * the {@code 500} the client receives.
     *
     * <p>A {@code 5xx} is the request's {@code ERROR}, classified as an {@code application}
     * error needing follow-up — unless a handler already wrote the fault's {@code ERROR}
     * record ({@link RequestFault}), when this one is {@code WARN} so the one failure is not
     * reported twice.
     */
    private static void record(HttpServletRequest request, int status, long started) {
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        LogEvent.classify(opened(status, RequestFault.isRecorded(request)), Operation.HTTP_REQUEST,
                        Category.NETWORK, Type.ACCESS, Type.END)
                .addKeyValue(LogEvent.HTTP_METHOD, method(request))
                .addKeyValue(LogEvent.HTTP_ROUTE, route(request))
                .addKeyValue(LogEvent.HTTP_STATUS_CODE, status)
                .addKeyValue(LogEvent.DURATION_MS, durationMs)
                .addKeyValue(LogEvent.OUTCOME, status < 400 ? LogEvent.SUCCESS : LogEvent.FAILURE)
                .log("HTTP request completed");
    }

    /** The record at the level {@link #level} gives, classified when that is {@code ERROR}. */
    private static LoggingEventBuilder opened(int status, boolean faultRecorded) {
        return switch (level(status, faultRecorded)) {
            case ERROR -> LogEvent.atError(log, status, ErrorCategory.APPLICATION, true);
            case WARN -> log.atWarn();
            default -> log.atInfo();
        };
    }

    static Level level(int status, boolean faultRecorded) {
        if (status >= 500 && !faultRecorded) {
            return Level.ERROR;
        }
        return status >= 400 ? Level.WARN : Level.INFO;
    }

    /**
     * Whether this dispatch gets a record: the request dispatch alone, and not for the
     * probes or the scrape.
     */
    static boolean recorded(HttpServletRequest request) {
        if (request.getDispatcherType() != DispatcherType.REQUEST) {
            return false;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !(isUnder(path, HEALTH) || isUnder(path, PROMETHEUS));
    }

    /** The template Spring matched, as Spring wrote it; {@link #UNMATCHED} if nothing did. */
    static String route(HttpServletRequest request) {
        return request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
                        instanceof String template
                ? template
                : UNMATCHED;
    }

    static String method(HttpServletRequest request) {
        return METHODS.contains(request.getMethod()) ? request.getMethod() : OTHER_METHOD;
    }

    /** Exactly {@code prefix}, or {@code prefix} followed by a sub-path. */
    private static boolean isUnder(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    private static String existingRequestId(HttpServletRequest request) {
        return request.getAttribute(REQUEST_ID_ATTRIBUTE) instanceof String existing
                ? existing
                : null;
    }
}
