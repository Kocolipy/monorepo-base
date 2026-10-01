package com.example.backend.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditEventQueryTests {

    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private static final UUID RESOURCE = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");

    private static final Instant TO = Instant.parse("2026-02-01T00:00:00Z");

    @Test
    void keepsEveryFilterItWasGiven() {
        AuditEventQuery query = new AuditEventQuery(
                AuditOperation.LOGIN_FAILURE, AuditOutcome.FAILURE, ACTOR, RESOURCE, FROM, TO, 3, 7);

        assertThat(query.operation()).isEqualTo(AuditOperation.LOGIN_FAILURE);
        assertThat(query.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(query.actorId()).isEqualTo(ACTOR);
        assertThat(query.resourceId()).isEqualTo(RESOURCE);
        assertThat(query.from()).isEqualTo(FROM);
        assertThat(query.to()).isEqualTo(TO);
        assertThat(query.page()).isEqualTo(3);
        assertThat(query.size()).isEqualTo(7);
    }

    @Test
    void theOffsetIsEveryEventOnTheEarlierPages() {
        assertThat(page(0, 50).offset()).isZero();
        assertThat(page(3, 7).offset()).isEqualTo(21);
    }

    /** {@code page * size} in {@code int} arithmetic would wrap negative here. */
    @Test
    void theOffsetOfAFarPageDoesNotOverflow() {
        assertThat(page(Integer.MAX_VALUE, AuditEventQuery.MAX_SIZE).offset())
                .isEqualTo((long) Integer.MAX_VALUE * AuditEventQuery.MAX_SIZE);
    }

    @Test
    void theFirstPageAndTheBoundsOfTheSizeAreAccepted() {
        assertThat(page(0, 1).size()).isEqualTo(1);
        assertThat(page(0, AuditEventQuery.MAX_SIZE).size()).isEqualTo(AuditEventQuery.MAX_SIZE);
    }

    @Test
    void aNegativePageIsRefused() {
        assertThatThrownBy(() -> page(-1, 10))
                .isInstanceOf(InvalidAuditQueryException.class)
                .hasMessage("page must not be negative");
    }

    @Test
    void aSizeOutsideItsBoundsIsRefused() {
        assertThatThrownBy(() -> page(0, 0))
                .isInstanceOf(InvalidAuditQueryException.class)
                .hasMessage("size must be between 1 and 200");
        assertThatThrownBy(() -> page(0, AuditEventQuery.MAX_SIZE + 1))
                .isInstanceOf(InvalidAuditQueryException.class)
                .hasMessage("size must be between 1 and 200");
    }

    @Test
    void theDefaultSizeIsWithinTheBound() {
        assertThat(AuditEventQuery.DEFAULT_SIZE).isEqualTo(50);
        assertThat(AuditEventQuery.MAX_SIZE).isEqualTo(200);
    }

    private static AuditEventQuery page(int page, int size) {
        return new AuditEventQuery(null, null, null, null, null, null, page, size);
    }
}
