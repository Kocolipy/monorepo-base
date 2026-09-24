package com.example.backend.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

class SessionControllerTests {

    private final SessionController controller = new SessionController();

    @Test
    void updatesAndReadsSessionState() {
        MockHttpSession session = new MockHttpSession();

        SessionController.SessionResponse updated = controller.updateSession(
                new SessionController.UpdateSessionRequest("Ada"), session);
        SessionController.SessionResponse fetched = controller.getSession(session);

        assertThat(updated.displayName()).isEqualTo("Ada");
        assertThat(fetched.displayName()).isEqualTo("Ada");
        assertThat(fetched.id()).isEqualTo(session.getId());
    }

    /**
     * The client shows how long the session has left, so the response has to
     * carry the session's own lifetime figures rather than defaults.
     */
    @Test
    void reportsTheSessionLifetime() {
        Instant beforeCreation = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        MockHttpSession session = new MockHttpSession();
        session.setMaxInactiveInterval(1800);

        SessionController.SessionResponse response = controller.getSession(session);

        assertThat(response.maxInactiveIntervalSeconds()).isEqualTo(1800);
        assertThat(response.createdAt())
                .isBetween(beforeCreation, Instant.now());
    }

    @Test
    void invalidatesExistingSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = (MockHttpSession) request.getSession();

        controller.deleteSession(request);

        assertThat(session.isInvalid()).isTrue();
    }

    /**
     * Logging out twice, or without ever having a session, is not an error and
     * must not bring a session into being on the way out.
     */
    @Test
    void ignoresDeletionWhenThereIsNoSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatCode(() -> controller.deleteSession(request)).doesNotThrowAnyException();

        assertThat(request.getSession(false)).isNull();
    }
}
