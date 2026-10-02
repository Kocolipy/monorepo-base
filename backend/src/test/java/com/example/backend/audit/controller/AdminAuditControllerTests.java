package com.example.backend.audit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.audit.application.AuditEventListingService;
import com.example.backend.audit.domain.AuditEventPage;
import com.example.backend.audit.domain.AuditEventQuery;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.audit.domain.AuditOutcome;
import com.example.backend.audit.domain.InvalidAuditQueryException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AdminAuditControllerTests {

    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private static final UUID RESOURCE = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");

    private static final Instant TO = Instant.parse("2026-02-01T00:00:00Z");

    private final List<AuditEventQuery> asked = new ArrayList<>();

    private final AdminAuditController controller = new AdminAuditController(
            new AuditEventListingService(query -> {
                asked.add(query);
                return AuditEventPage.of(query, List.of(), 0);
            }));

    @Test
    void passesEveryFilterAndThePageToTheListing() {
        AuditEventPage page = controller.list(
                AuditOperation.PASSWORD_CHANGE, AuditOutcome.FAILURE, ACTOR, RESOURCE, FROM, TO, 4, 25);

        assertThat(asked).containsExactly(new AuditEventQuery(
                AuditOperation.PASSWORD_CHANGE, AuditOutcome.FAILURE, ACTOR, RESOURCE, FROM, TO, 4, 25));
        assertThat(page.page()).isEqualTo(4);
        assertThat(page.size()).isEqualTo(25);
    }

    @Test
    void anOutOfRangePageSizeIsRefusedBeforeTheListingIsAsked() {
        assertThatThrownBy(() -> controller.list(null, null, null, null, null, null, 0, 0))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(asked).isEmpty();
    }

    /**
     * The refusal is a 400 — the request itself is malformed, not forbidden or conflicting — and
     * is handed to the app-wide handler as one, so it carries that handler's body. The domain's
     * refusal rides along as the cause, which is what the record names.
     */
    @Test
    void anInvalidQueryIsABadRequest() {
        assertThatThrownBy(() -> controller.list(null, null, null, null, null, null, -1, 5))
                .isInstanceOfSatisfying(ResponseStatusException.class, refused -> {
                    assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(refused.getCause()).isInstanceOf(InvalidAuditQueryException.class);
                    assertThat(refused.getReason()).isNull();
                });
    }
}
