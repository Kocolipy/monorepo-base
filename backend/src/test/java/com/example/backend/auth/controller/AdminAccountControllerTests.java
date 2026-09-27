package com.example.backend.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.auth.application.IdentityAdministrationService;
import com.example.backend.auth.application.IdentitySummary;
import java.lang.reflect.RecordComponent;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminAccountControllerTests {

    private static final Instant CREATED_AT = Instant.parse("2026-01-02T03:04:05Z");

    private static final UUID ADA = UUID.fromString("00000000-0000-0000-0000-00000000ada0");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-0000000000b0");

    private final Principal principal = () -> "ada";

    @Test
    void listsEveryAccountTheServiceReports() {
        AdminAccountController controller = new AdminAccountController(new RecordingService(List.of(
                summary(ADA, "ada", true, true, false),
                summary(BOB, "bob", false, false, true))));

        List<IdentitySummary> response = controller.listAccounts();

        assertThat(response).containsExactly(
                new IdentitySummary(ADA, "ada", true, true, false, true, CREATED_AT),
                new IdentitySummary(BOB, "bob", false, false, true, true, CREATED_AT));
    }

    @Test
    void listsNothingWhenNoAccountExists() {
        AdminAccountController controller = new AdminAccountController(new RecordingService(List.of()));

        assertThat(controller.listAccounts()).isEmpty();
    }

    /**
     * The caller's own name reaches the service, which is what lets it refuse an
     * administrator disabling themselves. Taken from the authenticated principal
     * rather than from the request, so it cannot be spoofed by the body.
     */
    @Test
    void disablingNamesBothTheTargetAndTheRequester() {
        RecordingService service = new RecordingService(List.of());
        AdminAccountController controller = new AdminAccountController(service);

        IdentitySummary response = controller.disable("bob", principal);

        assertThat(service.calls).containsExactly("disable:bob:ada");
        assertThat(response.userName()).isEqualTo("bob");
    }

    @Test
    void enablingAndUnlockingReachTheirOwnOperations() {
        RecordingService service = new RecordingService(List.of());
        AdminAccountController controller = new AdminAccountController(service);

        controller.enable("bob", principal);
        controller.unlock("bob", principal);

        assertThat(service.calls).containsExactly("enable:bob:ada", "unlock:bob:ada");
    }

    /**
     * The guarantee the whole endpoint exists to respect. It is asserted here, on
     * the adapter that publishes the type, because this is where a field reaching
     * a client becomes a disclosure: a hash added to {@link IdentitySummary} fails
     * here rather than in review.
     *
     * <p>{@code hasPassword} is a boolean saying whether a credential is set at
     * all, which is not the credential; the component that could carry one is
     * absent, and that is what this pins.
     */
    @Test
    void theResponseShapeHasNoFieldThatCouldCarryACredential() {
        assertThat(IdentitySummary.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .containsExactly(
                        "id", "userName", "admin", "active", "locked", "hasPassword", "createdAt");
    }

    private static IdentitySummary summary(
            UUID id, String userName, boolean admin, boolean active, boolean locked) {
        return new IdentitySummary(id, userName, admin, active, locked, true, CREATED_AT);
    }

    private static final class RecordingService extends IdentityAdministrationService {

        private final List<IdentitySummary> summaries;
        private final List<String> calls = new java.util.ArrayList<>();

        RecordingService(List<IdentitySummary> summaries) {
            super(null, null, null, null, null, null);
            this.summaries = summaries;
        }

        @Override
        public List<IdentitySummary> listIdentities() {
            return summaries;
        }

        @Override
        public IdentitySummary deactivate(String userName, String requestedBy) {
            calls.add("disable:" + userName + ":" + requestedBy);
            return summary(BOB, userName, false, false, false);
        }

        @Override
        public IdentitySummary activate(String userName, String requestedBy) {
            calls.add("enable:" + userName + ":" + requestedBy);
            return summary(BOB, userName, false, true, false);
        }

        @Override
        public IdentitySummary unlock(String userName, String requestedBy) {
            calls.add("unlock:" + userName + ":" + requestedBy);
            return summary(BOB, userName, false, true, false);
        }
    }
}
