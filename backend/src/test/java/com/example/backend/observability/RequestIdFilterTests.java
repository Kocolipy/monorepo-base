package com.example.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.DispatcherType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.WebUtils;

/**
 * What the filter has to get right: an id is in scope for the handler, it is the
 * service's own, it does not survive the request, and the error dispatch reports
 * the same exchange rather than a second one.
 */
class RequestIdFilterTests {

    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void clearContext() {
        MDC.clear();
    }

    @Test
    void theHandlerRunsWithARequestIdInScope() throws Exception {
        String observed = idSeenDuringChain(new MockHttpServletRequest("GET", "/api/auth/me"));

        assertThat(observed).isNotBlank();
    }

    /**
     * The context is thread-local and the thread is returned to a pool, so an id
     * left behind would be attributed to whatever request is served next.
     */
    @Test
    void theRequestIdDoesNotOutliveTheRequest() throws Exception {
        idSeenDuringChain(new MockHttpServletRequest("GET", "/api/auth/me"));

        assertThat(MDC.get(LogContext.REQUEST_ID)).isNull();
    }

    @Test
    void eachRequestGetsItsOwnId() throws Exception {
        String first = idSeenDuringChain(new MockHttpServletRequest("GET", "/api/auth/me"));
        String second = idSeenDuringChain(new MockHttpServletRequest("GET", "/api/auth/me"));

        assertThat(first).isNotEqualTo(second);
    }

    /**
     * A caller-supplied correlation header is ignored. Honouring one would let a
     * client merge unrelated requests in a log search by repeating a value, or
     * inject one of its own choosing into this service's records.
     */
    @Test
    void anIdOfferedByTheCallerIsNotUsed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader("X-Request-Id", "chosen-by-the-caller");
        request.addHeader("X-Correlation-Id", "chosen-by-the-caller");
        request.addHeader("traceparent", "chosen-by-the-caller");

        assertThat(idSeenDuringChain(request)).isNotEqualTo("chosen-by-the-caller");
    }

    /**
     * The error dispatch is a second pass over the same exchange. Minting a fresh
     * id there would file the response the client actually received under an id
     * that appears nowhere else.
     */
    @Test
    void theErrorDispatchReportsTheSameIdAsTheRequestThatFailed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        String duringRequest = idSeenDuringChain(request);

        // Both are what the container sets on an error dispatch, and both are what
        // OncePerRequestFilter consults before deciding to skip one: without the
        // error-dispatch override this filter carries, the chain below would run
        // with no id in scope at all.
        request.setDispatcherType(DispatcherType.ERROR);
        request.setAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE, "/api/auth/login");
        String duringErrorDispatch = idSeenDuringChain(request);

        assertThat(duringErrorDispatch).isEqualTo(duringRequest);
    }

    /**
     * The stashed value is type-checked, not cast. The attribute name is this
     * filter's own, but a request attribute namespace is shared with every filter
     * and framework in the container, so a collision must degrade to minting a
     * fresh id rather than failing the request with a cast error.
     */
    @Test
    void anAttributeOfTheWrongTypeIsIgnoredRatherThanCast() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.setAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE, 42);

        assertThat(idSeenDuringChain(request)).isNotBlank();
    }

    /**
     * Runs the filter over one request and reports the id that was in scope while
     * the chain was executing — which is the only moment a handler could log
     * under it.
     */
    private String idSeenDuringChain(MockHttpServletRequest request) throws Exception {
        List<String> seen = new ArrayList<>();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(
                    jakarta.servlet.ServletRequest servletRequest,
                    jakarta.servlet.ServletResponse servletResponse) {
                seen.add(MDC.get(LogContext.REQUEST_ID));
            }
        };

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(seen).hasSize(1);
        return seen.getFirst();
    }
}
