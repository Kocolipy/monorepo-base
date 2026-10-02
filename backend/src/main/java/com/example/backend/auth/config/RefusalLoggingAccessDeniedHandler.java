package com.example.backend.auth.config;

import com.example.backend.observability.AccessRefusalLog;
import com.example.backend.observability.AccessRefusalLog.Refusal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CsrfException;

/**
 * The application chain's {@code 403}: Spring Security's own answer, after one refusal
 * record.
 *
 * <p>Two refusals reach here and are recorded apart, because they mean different things: an
 * authorization rule turning an authenticated caller away — a missing role, or a session
 * confined by a required password change — and an unsafe request arriving without its
 * session's CSRF token, which says nothing about what the caller may do. The reason for the
 * first is the same word whichever rule refused it, so the record is no map of the rules.
 */
public class RefusalLoggingAccessDeniedHandler implements AccessDeniedHandler {

    private final AccessRefusalLog refusals;

    private final AccessDeniedHandler answer = new AccessDeniedHandlerImpl();

    public RefusalLoggingAccessDeniedHandler(AccessRefusalLog refusals) {
        this.refusals = refusals;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException refused) throws IOException, ServletException {
        refusals.record(request,
                refused instanceof CsrfException ? Refusal.CSRF : Refusal.ACCESS_DENIED);
        answer.handle(request, response, refused);
    }
}
