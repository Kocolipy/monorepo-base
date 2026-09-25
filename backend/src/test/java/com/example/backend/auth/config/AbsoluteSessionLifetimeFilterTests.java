package com.example.backend.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.auth.MutableClock;
import com.example.backend.auth.domain.AbsoluteSessionLifetimePolicy;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AbsoluteSessionLifetimeFilterTests {

    private static final AbsoluteSessionLifetimePolicy POLICY =
            new AbsoluteSessionLifetimePolicy(Duration.ofHours(8));

    /**
     * A request with no session at all: the filter must not create one just to
     * check its age, and must let the request through unchanged.
     */
    @Test
    void doesNothingWhenThereIsNoSession() throws Exception {
        MutableClock clock = new MutableClock(Instant.now());
        AbsoluteSessionLifetimeFilter filter = new AbsoluteSessionLifetimeFilter(POLICY, clock);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(request.getSession(false)).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void leavesASessionYoungerThanTheLifetimeUntouched() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = (MockHttpSession) request.getSession();
        MutableClock clock = new MutableClock(
                Instant.ofEpochMilli(session.getCreationTime()).plus(Duration.ofHours(1)));
        AbsoluteSessionLifetimeFilter filter = new AbsoluteSessionLifetimeFilter(POLICY, clock);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(session.isInvalid()).isFalse();
    }

    /**
     * Past the policy's ceiling, the session is invalidated and the security
     * context cleared before the rest of the chain runs — so a protected path
     * sees the request exactly as it would an unauthenticated one, rather than a
     * distinct failure mode.
     */
    @Test
    void invalidatesASessionPastTheLifetimeAndClearsTheSecurityContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = (MockHttpSession) request.getSession();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("ada", null, null));
        try {
            MutableClock clock = new MutableClock(
                    Instant.ofEpochMilli(session.getCreationTime())
                            .plus(Duration.ofHours(8))
                            .plusMillis(1));
            AbsoluteSessionLifetimeFilter filter =
                    new AbsoluteSessionLifetimeFilter(POLICY, clock);

            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

            assertThat(session.isInvalid()).isTrue();
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void aSessionExactlyAtTheLifetimeIsNotYetInvalidated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = (MockHttpSession) request.getSession();
        MutableClock clock = new MutableClock(
                Instant.ofEpochMilli(session.getCreationTime()).plus(Duration.ofHours(8)));
        AbsoluteSessionLifetimeFilter filter = new AbsoluteSessionLifetimeFilter(POLICY, clock);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(session.isInvalid()).isFalse();
    }
}
