package com.example.backend.observability;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Whether the current request's fault already has its {@code ERROR} record.
 *
 * <p>A handler that logs the fault it answers — with the exception attached and the error
 * classified — says so here, and the request record {@link RequestIdFilter} writes at the end
 * of the same request is then written at {@code WARN} rather than as a second {@code ERROR}
 * for the one failure. A fault no handler recorded keeps its {@code ERROR} request record,
 * which is then the only one there is.
 *
 * <p>Kept on the request rather than in the logging context: the mark has to outlive the
 * handler's own scope and be read by the filter after the chain returns, and it must never
 * be written into a record.
 */
public final class RequestFault {

    static final String RECORDED_ATTRIBUTE = RequestFault.class.getName() + ".recorded";

    private RequestFault() {
    }

    /** Marks the request being served on this thread, if there is one. */
    public static void recorded() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes current) {
            current.getRequest().setAttribute(RECORDED_ATTRIBUTE, Boolean.TRUE);
        }
    }

    static boolean isRecorded(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute(RECORDED_ATTRIBUTE));
    }
}
