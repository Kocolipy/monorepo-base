package com.example.backend.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.auth.application.AccountService;
import com.example.backend.auth.application.AccountSummary;
import com.example.backend.auth.controller.AdminUserController.AdminUserResponse;
import com.example.backend.auth.domain.AccountRole;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminUserControllerTests {

    private static final Instant CREATED_AT = Instant.parse("2026-01-02T03:04:05Z");

    @Test
    void listsEveryAccountTheServiceReports() {
        AdminUserController controller = new AdminUserController(new StubAccountService(List.of(
                new AccountSummary("ada", "ada@example.com", AccountRole.ADMIN, true, CREATED_AT),
                new AccountSummary(
                        "bob", "bob@example.com", AccountRole.USER, false, CREATED_AT))));

        List<AdminUserResponse> response = controller.listUsers();

        assertThat(response).containsExactly(
                new AdminUserResponse("ada", "ada@example.com", AccountRole.ADMIN, true, CREATED_AT),
                new AdminUserResponse(
                        "bob", "bob@example.com", AccountRole.USER, false, CREATED_AT));
    }

    @Test
    void listsNothingWhenNoAccountExists() {
        AdminUserController controller =
                new AdminUserController(new StubAccountService(List.of()));

        assertThat(controller.listUsers()).isEmpty();
    }

    /**
     * The guarantee the story is actually about. Asserting the absence of a
     * password field on the response type covers every future field too: a hash
     * added to the wire shape fails here rather than in review.
     */
    @Test
    void theResponseShapeHasNoFieldThatCouldCarryACredential() {
        assertThat(AdminUserResponse.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .containsExactly("username", "email", "role", "enabled", "createdAt");
    }

    private static final class StubAccountService extends AccountService {

        private final List<AccountSummary> summaries;

        StubAccountService(List<AccountSummary> summaries) {
            super(null, null, null);
            this.summaries = summaries;
        }

        @Override
        public List<AccountSummary> listAccounts() {
            return summaries;
        }
    }
}
