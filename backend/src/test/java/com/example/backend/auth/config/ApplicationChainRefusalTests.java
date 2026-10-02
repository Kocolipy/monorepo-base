package com.example.backend.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.example.backend.audit.CapturedLog;
import com.example.backend.observability.AccessRefusalLog;
import com.example.backend.observability.LogEvent;
import com.example.backend.observability.RouteTemplates;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.MissingCsrfTokenException;

/**
 * The application chain's two refusal answers, each driven directly: what the caller receives,
 * and the one record it leaves.
 */
class ApplicationChainRefusalTests {

    private final AccessRefusalLog refusals = new AccessRefusalLog(new RouteTemplates(() -> null));

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    // ---- 401 ---------------------------------------------------------------------------------

    @Test
    void a_request_without_a_session_is_401_recorded_as_no_session() throws Exception {
        ILoggingEvent record = onlyRecord(() -> entryPoint().commence(
                request("GET", "/api/self"), response,
                new InsufficientAuthenticationException("anonymous")));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(SessionAuthenticationEntryPoint.CLEAR_SITE_DATA_HEADER))
                .isNull();
        assertThat(record.getLevel()).isEqualTo(Level.WARN);
        assertThat(CapturedLog.fields(record)).containsEntry(LogEvent.REASON, "no-session");
    }

    /** A session id naming no live session: the idle timeout, or a revocation. */
    @Test
    void a_session_id_naming_no_live_session_is_recorded_as_expired() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/self");
        request.setRequestedSessionId("gone");
        request.setRequestedSessionIdValid(false);

        ILoggingEvent record = onlyRecord(() -> entryPoint().commence(request, response, null));

        assertThat(CapturedLog.fields(record)).containsEntry(LogEvent.REASON, "session-expired");
    }

    /** A live session that was never signed in to ended nothing. */
    @Test
    void a_live_guest_session_is_no_session() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/self");
        request.setRequestedSessionId("guest");
        request.setRequestedSessionIdValid(true);

        ILoggingEvent record = onlyRecord(() -> entryPoint().commence(request, response, null));

        assertThat(CapturedLog.fields(record)).containsEntry(LogEvent.REASON, "no-session");
    }

    /** The absolute lifetime ended this request's session just before the chain refused it. */
    @Test
    void a_session_the_absolute_lifetime_just_ended_is_expired() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/self");
        request.setAttribute(AbsoluteSessionLifetimeFilter.ENDED_ATTRIBUTE, Boolean.TRUE);

        ILoggingEvent record = onlyRecord(() -> entryPoint().commence(request, response, null));

        assertThat(CapturedLog.fields(record)).containsEntry(LogEvent.REASON, "session-expired");
    }

    /** A sessionless logout is still a browser signing out, so it is told to clear its data. */
    @Test
    void a_sessionless_logout_is_401_with_clear_site_data_and_is_recorded() throws Exception {
        ILoggingEvent record = onlyRecord(() -> entryPoint().commence(
                request("DELETE", "/api/auth/logout"), response, null));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(SessionAuthenticationEntryPoint.CLEAR_SITE_DATA_HEADER))
                .isEqualTo("\"cache\",\"cookies\",\"storage\"");
        assertThat(CapturedLog.fields(record)).containsEntry(LogEvent.REASON, "no-session");
    }

    /** The logout path is matched within the application, not as the deployed URI. */
    @Test
    void a_sessionless_logout_under_a_context_path_still_clears_site_data() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/app/api/auth/logout");
        request.setContextPath("/app");
        request.setRequestURI("/app/api/auth/logout");

        entryPoint().commence(request, response, null);

        assertThat(response.getHeader(SessionAuthenticationEntryPoint.CLEAR_SITE_DATA_HEADER))
                .isEqualTo("\"cache\",\"cookies\",\"storage\"");
    }

    /** No session id at all ended nothing, whatever the validity flag says. */
    @Test
    void no_session_id_is_no_session_even_when_flagged_invalid() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/self");
        request.setRequestedSessionIdValid(false);

        ILoggingEvent record = onlyRecord(() -> entryPoint().commence(request, response, null));

        assertThat(CapturedLog.fields(record)).containsEntry(LogEvent.REASON, "no-session");
    }

    @Test
    void only_a_delete_of_the_logout_path_clears_site_data() throws Exception {
        MockHttpServletResponse get = new MockHttpServletResponse();
        entryPoint().commence(request("GET", "/api/auth/logout"), get, null);
        MockHttpServletResponse elsewhere = new MockHttpServletResponse();
        entryPoint().commence(request("DELETE", "/api/auth/logout/x"), elsewhere, null);

        assertThat(get.getHeader(SessionAuthenticationEntryPoint.CLEAR_SITE_DATA_HEADER)).isNull();
        assertThat(elsewhere.getHeader(SessionAuthenticationEntryPoint.CLEAR_SITE_DATA_HEADER))
                .isNull();
    }

    // ---- 403 ---------------------------------------------------------------------------------

    @Test
    void an_authorization_refusal_is_403_recorded_as_access_denied() throws Exception {
        ILoggingEvent record = onlyRecord(() -> deniedHandler().handle(
                request("GET", "/api/admin/accounts"), response,
                new AccessDeniedException("Access Denied: requires ROLE_ADMIN")));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(record.getLevel()).isEqualTo(Level.WARN);
        assertThat(CapturedLog.fields(record)).containsEntry(LogEvent.REASON, "access-denied");
        assertThat(record.getThrowableProxy()).isNull();
        assertThat(CapturedLog.fields(record).toString() + record.getFormattedMessage())
                .doesNotContain("ROLE_").doesNotContain("ADMIN");
    }

    @Test
    void a_missing_or_invalid_csrf_token_is_403_recorded_as_csrf() throws Exception {
        ILoggingEvent missing = onlyRecord(() -> deniedHandler().handle(
                request("POST", "/api/count/increment"), response,
                new MissingCsrfTokenException("x")));
        MockHttpServletResponse second = new MockHttpServletResponse();
        ILoggingEvent invalid = onlyRecord(() -> deniedHandler().handle(
                request("POST", "/api/count/increment"), second,
                new InvalidCsrfTokenException(
                        new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "expected"), "actual")));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(second.getStatus()).isEqualTo(403);
        assertThat(CapturedLog.fields(missing)).containsEntry(LogEvent.REASON, "csrf");
        assertThat(CapturedLog.fields(invalid)).containsEntry(LogEvent.REASON, "csrf");
        assertThat(invalid.getFormattedMessage() + CapturedLog.fields(invalid))
                .doesNotContain("expected").doesNotContain("actual");
    }

    private SessionAuthenticationEntryPoint entryPoint() {
        return new SessionAuthenticationEntryPoint(refusals);
    }

    private RefusalLoggingAccessDeniedHandler deniedHandler() {
        return new RefusalLoggingAccessDeniedHandler(refusals);
    }

    private static ILoggingEvent onlyRecord(Action action) throws Exception {
        try (CapturedLog captured = CapturedLog.attach()) {
            action.run();
            List<ILoggingEvent> records = captured.withAction(Level.TRACE, LogEvent.KIND, "event");
            assertThat(records).hasSize(1);
            return records.getFirst();
        }
    }

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        return request;
    }

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }
}
