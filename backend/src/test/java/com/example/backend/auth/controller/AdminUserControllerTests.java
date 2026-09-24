package com.example.backend.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.auth.application.AccountAdministrationService;
import com.example.backend.auth.application.AccountSummary;
import com.example.backend.auth.controller.AdminUserController.AdminUserResponse;
import com.example.backend.auth.domain.AccountRole;
import java.lang.reflect.RecordComponent;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminUserControllerTests {

    private static final Instant CREATED_AT = Instant.parse("2026-01-02T03:04:05Z");

    private final Principal principal = () -> "ada";

    @Test
    void listsEveryAccountTheServiceReports() {
        AdminUserController controller = new AdminUserController(new RecordingService(List.of(
                summary("ada", AccountRole.ADMIN, true, false),
                summary("bob", AccountRole.USER, false, true))));

        List<AdminUserResponse> response = controller.listUsers();

        assertThat(response).containsExactly(
                new AdminUserResponse(
                        "ada", "ada@example.com", AccountRole.ADMIN, true, false, null, CREATED_AT),
                new AdminUserResponse(
                        "bob", "bob@example.com", AccountRole.USER, false, true, null, CREATED_AT));
    }

    @Test
    void listsNothingWhenNoAccountExists() {
        AdminUserController controller = new AdminUserController(new RecordingService(List.of()));

        assertThat(controller.listUsers()).isEmpty();
    }

    /**
     * The caller's own name reaches the service, which is what lets it refuse an
     * administrator disabling themselves. Taken from the authenticated principal
     * rather than from the request, so it cannot be spoofed by the body.
     */
    @Test
    void disablingNamesBothTheTargetAndTheRequester() {
        RecordingService service = new RecordingService(List.of());
        AdminUserController controller = new AdminUserController(service);

        AdminUserResponse response = controller.disable("bob", principal);

        assertThat(service.calls).containsExactly("disable:bob:ada");
        assertThat(response.username()).isEqualTo("bob");
    }

    @Test
    void enablingAndUnlockingReachTheirOwnOperations() {
        RecordingService service = new RecordingService(List.of());
        AdminUserController controller = new AdminUserController(service);

        controller.enable("bob");
        controller.unlock("bob");

        assertThat(service.calls).containsExactly("enable:bob", "unlock:bob");
    }

    /**
     * The guarantee the whole endpoint exists to respect. Asserting the shape of
     * the response type covers every future field too: a hash added to the wire
     * shape fails here rather than in review.
     */
    @Test
    void theResponseShapeHasNoFieldThatCouldCarryACredential() {
        assertThat(AdminUserResponse.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .containsExactly(
                        "username", "email", "role", "enabled", "locked", "lockedUntil",
                        "createdAt");
    }

    private static AccountSummary summary(
            String username, AccountRole role, boolean enabled, boolean locked) {
        return new AccountSummary(
                username, username + "@example.com", role, enabled, locked, null, CREATED_AT);
    }

    private static final class RecordingService extends AccountAdministrationService {

        private final List<AccountSummary> summaries;
        private final List<String> calls = new java.util.ArrayList<>();

        RecordingService(List<AccountSummary> summaries) {
            super(null, null);
            this.summaries = summaries;
        }

        @Override
        public List<AccountSummary> listAccounts() {
            return summaries;
        }

        @Override
        public AccountSummary disable(String username, String requestedBy) {
            calls.add("disable:" + username + ":" + requestedBy);
            return summary(username, AccountRole.USER, false, false);
        }

        @Override
        public AccountSummary enable(String username) {
            calls.add("enable:" + username);
            return summary(username, AccountRole.USER, true, false);
        }

        @Override
        public AccountSummary unlock(String username) {
            calls.add("unlock:" + username);
            return summary(username, AccountRole.USER, true, false);
        }
    }
}
