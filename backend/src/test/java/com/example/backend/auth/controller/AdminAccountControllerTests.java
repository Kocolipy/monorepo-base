package com.example.backend.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.auth.application.AccountAdministrationService;
import com.example.backend.auth.application.AccountSummary;
import com.example.backend.auth.domain.AccountRole;
import java.lang.reflect.RecordComponent;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminAccountControllerTests {

    private static final Instant CREATED_AT = Instant.parse("2026-01-02T03:04:05Z");

    private final Principal principal = () -> "ada";

    @Test
    void listsEveryAccountTheServiceReports() {
        AdminAccountController controller = new AdminAccountController(new RecordingService(List.of(
                summary("ada", AccountRole.ADMIN, true, false),
                summary("bob", AccountRole.USER, false, true))));

        List<AccountSummary> response = controller.listAccounts();

        assertThat(response).containsExactly(
                new AccountSummary("ada", AccountRole.ADMIN, true, false, null, CREATED_AT),
                new AccountSummary("bob", AccountRole.USER, false, true, null, CREATED_AT));
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

        AccountSummary response = controller.disable("bob", principal);

        assertThat(service.calls).containsExactly("disable:bob:ada");
        assertThat(response.username()).isEqualTo("bob");
    }

    @Test
    void enablingAndUnlockingReachTheirOwnOperations() {
        RecordingService service = new RecordingService(List.of());
        AdminAccountController controller = new AdminAccountController(service);

        controller.enable("bob");
        controller.unlock("bob");

        assertThat(service.calls).containsExactly("enable:bob", "unlock:bob");
    }

    /**
     * The guarantee the whole endpoint exists to respect. It is asserted here, on
     * the adapter that publishes the type, because this is where a field reaching
     * a client becomes a disclosure: a hash added to {@link AccountSummary} fails
     * here rather than in review.
     */
    @Test
    void theResponseShapeHasNoFieldThatCouldCarryACredential() {
        assertThat(AccountSummary.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .containsExactly(
                        "username", "role", "enabled", "locked", "lockedUntil", "createdAt");
    }

    private static AccountSummary summary(
            String username, AccountRole role, boolean enabled, boolean locked) {
        return new AccountSummary(username, role, enabled, locked, null, CREATED_AT);
    }

    private static final class RecordingService extends AccountAdministrationService {

        private final List<AccountSummary> summaries;
        private final List<String> calls = new java.util.ArrayList<>();

        RecordingService(List<AccountSummary> summaries) {
            super(null, null, null, null);
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
