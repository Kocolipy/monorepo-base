package com.example.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.auth.domain.RoleMappingSessions;
import com.example.backend.authorization.TestRoleMappings;
import com.example.backend.authorization.domain.RoleMapping;
import com.example.backend.observability.EcsLogCapture;
import com.example.backend.scim.InMemoryScimUserRepository;
import com.example.backend.scim.ScimIdentities;
import com.example.backend.scim.domain.ScimUser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import tools.jackson.databind.JsonNode;

/**
 * The startup pass that ends sessions issued under another role mapping: every User is checked
 * against the running mapping's hash, and the pass is reported as the spec's two
 * {@code application-startup} records.
 */
class RoleMappingSessionServiceTests {

    private final RoleMapping mapping = TestRoleMappings.superuserOnly();

    private final InMemoryScimUserRepository users = new InMemoryScimUserRepository();

    /** One call the service made: whose sessions it asked about, under which hash. */
    private record Call(List<UUID> userIds, String hash) {
    }

    private final List<Call> calls = new ArrayList<>();

    private int ended;

    private final RoleMappingSessions sessions = new RoleMappingSessions() {
        @Override
        public int revokeIssuedUnderAnotherMapping(Collection<UUID> userIds, String currentHash) {
            calls.add(new Call(List.copyOf(userIds), currentHash));
            return ended;
        }
    };

    private final RoleMappingSessionService service =
            new RoleMappingSessionService(mapping, sessions, users);

    @Test
    void everyUsersSessionsAreCheckedAgainstTheRunningMappingsHash() {
        ScimUser ada = users.create(ScimIdentities.user("ada"));
        ScimUser bea = users.create(ScimIdentities.user("bea"));
        ended = 3;

        assertThat(service.revokeSessionsIssuedUnderAnotherMapping()).isEqualTo(3);

        assertThat(calls).singleElement().satisfies(call -> {
            assertThat(call.hash()).isEqualTo(mapping.hash());
            assertThat(call.userIds()).containsExactlyInAnyOrder(ada.id(), bea.id());
        });
    }

    /**
     * The validated mapping's hash is reported every time ({@code info}); the revocation only when
     * it ended something ({@code change}, with the count). Both {@code INFO}, both classified
     * {@code application-startup}, success.
     */
    @Test
    void theHashIsReportedAndARevocationIsReportedWithItsCount() {
        try (EcsLogCapture ecs = EcsLogCapture.attach(new StandardEnvironment())) {
            ended = 0;
            service.revokeSessionsIssuedUnderAnotherMapping();
            List<JsonNode> quiet = records(ecs);
            ecs.reset();
            ended = 2;
            service.revokeSessionsIssuedUnderAnotherMapping();
            List<JsonNode> revoking = records(ecs);

            assertThat(quiet).extracting(record -> record.at("/message").asString())
                    .containsExactly("Role mapping validated");
            assertThat(revoking).extracting(record -> record.at("/message").asString())
                    .containsExactly("Role mapping validated",
                            "Sessions issued under another role mapping ended");
            assertThat(revoking).allSatisfy(record -> {
                assertThat(record.at("/log/level").asString()).isEqualTo("INFO");
                assertThat(record.at("/event/action").asString()).isEqualTo("application-startup");
                assertThat(record.at("/event/outcome").asString()).isEqualTo("success");
                assertThat(record.at("/app/authorization/mapping_hash").asString())
                        .isEqualTo(mapping.hash());
            });
            assertThat(revoking.get(0).at("/event/type/0").asString()).isEqualTo("info");
            assertThat(revoking.get(1).at("/event/type/0").asString()).isEqualTo("change");
            assertThat(revoking.get(1).at("/session/ended_count").asInt()).isEqualTo(2);
        }
    }

    private static List<JsonNode> records(EcsLogCapture ecs) {
        return ecs.records().stream()
                .filter(record -> "authorization.role_mapping"
                        .equals(record.at("/app/event/action").asString()))
                .toList();
    }
}
