package com.example.backend.scim.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import com.example.backend.audit.CapturedLog;
import com.example.backend.audit.RecordingAuditTrail;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.auth.InMemoryAccountRepository;
import com.example.backend.auth.MutableClock;
import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRole;
import com.example.backend.observability.LogEvent;
import com.example.backend.scim.InMemoryScimConnectorRepository;
import com.example.backend.scim.InMemoryScimConnectorTokenRepository;
import com.example.backend.scim.InMemoryScimExternalIdRepository;
import com.example.backend.scim.domain.ConnectorTokenPolicy;
import com.example.backend.scim.domain.ConnectorTokenScope;
import com.example.backend.scim.domain.ConnectorTokenSecret;
import com.example.backend.scim.domain.InvalidConnectorTokenLifetimeException;
import com.example.backend.scim.domain.ScimConnectorToken;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The connector and token lifecycle, against in-memory stores.
 *
 * <p>At this level the claims are about what the use case decides: which events it
 * records, what the overlap arithmetic does to the stored rows, and that a delete
 * takes tokens and aliases with it. That a request arriving over HTTP produces those
 * decisions, and that the rows genuinely land in Postgres, is
 * {@code ScimConnectorLifecycleIntegrationTests}' claim rather than this one's.
 */
class ConnectorAdministrationServiceTests {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static final String ADMIN = "an-admin";

    private final InMemoryScimConnectorRepository connectors =
            new InMemoryScimConnectorRepository();

    private final InMemoryScimConnectorTokenRepository tokens =
            new InMemoryScimConnectorTokenRepository();

    private final InMemoryScimExternalIdRepository aliases =
            new InMemoryScimExternalIdRepository();

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();

    private final RecordingAuditTrail audit = new RecordingAuditTrail();

    private final MutableClock clock = new MutableClock(NOW);

    private ConnectorAdministrationService service;

    private UUID adminId;

    @BeforeEach
    void setUp() {
        Account admin = accounts.save(
                new Account(ADMIN, "hash", AccountRole.ADMIN, 0, null, true, NOW));
        adminId = admin.id();
        service = new ConnectorAdministrationService(
                connectors, tokens, aliases, accounts, audit, new SecureRandom(), clock);
    }

    @Nested
    class Connectors {

        @Test
        void a_created_connector_is_listed_and_holds_no_tokens() {
            ConnectorSummary created = service.create("Okta", ADMIN);

            assertThat(created.displayName()).isEqualTo("Okta");
            assertThat(created.createdAt()).isEqualTo(NOW);
            assertThat(created.tokens()).isEmpty();
            assertThat(service.listConnectors()).extracting(ConnectorSummary::id)
                    .containsExactly(created.id());
        }

        @Test
        void creation_records_one_event_naming_the_connector_and_the_admin() {
            ConnectorSummary created = service.create("Okta", ADMIN);

            assertThat(audit.of(AuditOperation.CONNECTOR_CREATE))
                    .singleElement()
                    .satisfies(event -> {
                        assertThat(event.actorId()).isEqualTo(adminId);
                        assertThat(event.subjectId()).isEqualTo(created.id());
                    });
        }

        @Test
        void the_listing_is_alphabetical_and_omits_a_deleted_connector() {
            service.create("Zeta", ADMIN);
            ConnectorSummary alpha = service.create("Alpha", ADMIN);
            ConnectorSummary middle = service.create("Middle", ADMIN);

            service.delete(middle.id(), ADMIN);

            assertThat(service.listConnectors()).extracting(ConnectorSummary::displayName)
                    .containsExactly("Alpha", "Zeta");
            assertThat(alpha.id()).isNotEqualTo(middle.id());
        }

        /** The criterion, as one transaction's observable effect. */
        @Test
        void deleting_a_connector_revokes_every_token_and_removes_every_alias() {
            ConnectorSummary connector = service.create("Okta", ADMIN);
            service.issueToken(connector.id(), ConnectorTokenScope.READ_ONLY, null, ADMIN);
            service.issueToken(connector.id(), ConnectorTokenScope.READ_WRITE, null, ADMIN);
            aliases.seed(connector.id(), "okta-user-1");
            aliases.seed(connector.id(), "okta-user-2");

            service.delete(connector.id(), ADMIN);

            assertThat(tokens.all()).isNotEmpty().allMatch(ScimConnectorToken::isRevoked);
            assertThat(aliases.all()).isEmpty();
        }

        /** Another connector's aliases are somebody else's namespace. */
        @Test
        void deleting_a_connector_leaves_another_connectors_aliases_alone() {
            ConnectorSummary doomed = service.create("Okta", ADMIN);
            ConnectorSummary survivor = service.create("Entra", ADMIN);
            aliases.seed(doomed.id(), "okta-user-1");
            aliases.seed(survivor.id(), "entra-user-1");

            service.delete(doomed.id(), ADMIN);

            assertThat(aliases.all()).extracting(
                            InMemoryScimExternalIdRepository.Alias::externalId)
                    .containsExactly("entra-user-1");
        }

        /**
         * One revocation event per token taken down, plus the delete itself. The
         * per-token events carry no actor: the delete is the administrative act, and
         * repeating the actor on each would read as several separate decisions.
         */
        @Test
        void deletion_records_the_delete_and_one_revocation_per_token_it_took_down() {
            ConnectorSummary connector = service.create("Okta", ADMIN);
            service.issueToken(connector.id(), ConnectorTokenScope.READ_ONLY, null, ADMIN);
            service.issueToken(connector.id(), ConnectorTokenScope.READ_WRITE, null, ADMIN);
            audit.reset();

            service.delete(connector.id(), ADMIN);

            assertThat(audit.of(AuditOperation.CONNECTOR_TOKEN_REVOKE)).hasSize(2)
                    .allSatisfy(event -> {
                        assertThat(event.actorId()).isNull();
                        assertThat(event.subjectId()).isEqualTo(connector.id());
                    });
            assertThat(audit.of(AuditOperation.CONNECTOR_DELETE)).singleElement()
                    .satisfies(event -> {
                        assertThat(event.actorId()).isEqualTo(adminId);
                        assertThat(event.subjectId()).isEqualTo(connector.id());
                    });
        }

        @Test
        void deleting_a_connector_twice_is_refused_the_second_time() {
            ConnectorSummary connector = service.create("Okta", ADMIN);
            service.delete(connector.id(), ADMIN);

            assertThatThrownBy(() -> service.delete(connector.id(), ADMIN))
                    .isInstanceOf(UnknownConnectorException.class);
        }

        @Test
        void an_unknown_connector_id_is_refused() {
            assertThatThrownBy(() -> service.delete(UUID.randomUUID(), ADMIN))
                    .isInstanceOf(UnknownConnectorException.class);
        }
    }

    @Nested
    class Tokens {

        @Test
        void an_issued_token_discloses_its_plaintext_and_stores_only_a_digest() {
            ConnectorSummary connector = service.create("Okta", ADMIN);

            IssuedConnectorToken issued = service.issueToken(
                    connector.id(), ConnectorTokenScope.READ_WRITE, null, ADMIN);

            assertThat(issued.presentedValue()).isNotBlank();
            assertThat(issued.connectorId()).isEqualTo(connector.id());
            assertThat(issued.scope()).isEqualTo(ConnectorTokenScope.READ_WRITE);
            assertThat(issued.issuedAt()).isEqualTo(NOW);
            assertThat(issued.expiresAt())
                    .isEqualTo(NOW.plus(ConnectorTokenPolicy.DEFAULT_LIFETIME));

            ScimConnectorToken stored = tokens.findById(issued.tokenId()).orElseThrow();
            assertThat(stored.digest().matches(
                            ConnectorTokenSecret.parse(issued.presentedValue())
                                    .orElseThrow()
                                    .digest()))
                    .isTrue();
        }

        /**
         * The listing an Admin reads carries no field the plaintext could occupy, which
         * is checked here by reflecting over the projection rather than by reading it:
         * a value assertion would pass again the day somebody added such a field.
         */
        @Test
        void no_token_projection_field_can_hold_the_plaintext() {
            ConnectorSummary connector = service.create("Okta", ADMIN);
            IssuedConnectorToken issued = service.issueToken(
                    connector.id(), ConnectorTokenScope.READ_ONLY, null, ADMIN);

            ConnectorSummary listed = service.listConnectors().getFirst();

            assertThat(listed.tokens()).singleElement().satisfies(token -> {
                assertThat(token.toString()).doesNotContain(issued.presentedValue());
                assertThat(ConnectorTokenSummary.class.getRecordComponents())
                        .noneMatch(component ->
                                component.getType().equals(String.class)
                                        || component.getType().equals(byte[].class));
            });
        }

        @Test
        void a_shorter_lifetime_is_honoured_and_one_past_the_ceiling_is_refused() {
            ConnectorSummary connector = service.create("Okta", ADMIN);

            IssuedConnectorToken short30 = service.issueToken(
                    connector.id(), ConnectorTokenScope.READ_ONLY, Duration.ofDays(30), ADMIN);
            assertThat(short30.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));

            assertThatThrownBy(() -> service.issueToken(
                            connector.id(),
                            ConnectorTokenScope.READ_ONLY,
                            Duration.ofDays(400),
                            ADMIN))
                    .isInstanceOf(InvalidConnectorTokenLifetimeException.class);
        }

        @Test
        void issuing_records_one_event_naming_the_connector() {
            ConnectorSummary connector = service.create("Okta", ADMIN);
            audit.reset();

            service.issueToken(connector.id(), ConnectorTokenScope.READ_ONLY, null, ADMIN);

            assertThat(audit.of(AuditOperation.CONNECTOR_TOKEN_ISSUE)).singleElement()
                    .satisfies(event -> {
                        assertThat(event.actorId()).isEqualTo(adminId);
                        assertThat(event.subjectId()).isEqualTo(connector.id());
                    });
        }

        @Test
        void a_token_may_not_be_issued_for_a_deleted_connector() {
            ConnectorSummary connector = service.create("Okta", ADMIN);
            service.delete(connector.id(), ADMIN);

            assertThatThrownBy(() -> service.issueToken(
                            connector.id(), ConnectorTokenScope.READ_ONLY, null, ADMIN))
                    .isInstanceOf(UnknownConnectorException.class);
        }

        @Test
        void rotation_keeps_the_scope_and_gives_the_replacement_a_full_lifetime() {
            IssuedConnectorToken original = issuedReadWriteToken();

            IssuedConnectorToken replacement =
                    service.rotateToken(original.tokenId(), Duration.ofDays(7), ADMIN);

            assertThat(replacement.tokenId()).isNotEqualTo(original.tokenId());
            assertThat(replacement.scope()).isEqualTo(ConnectorTokenScope.READ_WRITE);
            assertThat(replacement.presentedValue()).isNotEqualTo(original.presentedValue());
            assertThat(replacement.expiresAt())
                    .isEqualTo(NOW.plus(ConnectorTokenPolicy.DEFAULT_LIFETIME));
        }

        @Test
        void rotation_ends_the_old_token_at_the_requested_overlap() {
            IssuedConnectorToken original = issuedReadWriteToken();

            service.rotateToken(original.tokenId(), Duration.ofDays(7), ADMIN);

            ScimConnectorToken old = tokens.findById(original.tokenId()).orElseThrow();
            assertThat(old.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
            assertThat(old.isUsable(NOW.plus(Duration.ofDays(6)))).isTrue();
            assertThat(old.isUsable(NOW.plus(Duration.ofDays(8)))).isFalse();
        }

        @Test
        void an_overlap_past_fourteen_days_is_clamped_to_fourteen() {
            IssuedConnectorToken original = issuedReadWriteToken();

            service.rotateToken(original.tokenId(), Duration.ofDays(90), ADMIN);

            assertThat(tokens.findById(original.tokenId()).orElseThrow().expiresAt())
                    .isEqualTo(NOW.plus(ConnectorTokenPolicy.MAX_ROTATION_OVERLAP));
        }

        /** The criterion: the overlap never runs past the old token's original expiry. */
        @Test
        void an_overlap_never_outlives_the_old_tokens_original_expiry() {
            ConnectorSummary connector = service.create("Okta", ADMIN);
            IssuedConnectorToken shortLived = service.issueToken(
                    connector.id(), ConnectorTokenScope.READ_ONLY, Duration.ofDays(3), ADMIN);

            service.rotateToken(shortLived.tokenId(), Duration.ofDays(14), ADMIN);

            ScimConnectorToken old = tokens.findById(shortLived.tokenId()).orElseThrow();
            assertThat(old.expiresAt()).isEqualTo(shortLived.expiresAt());
            assertThat(old.expiresAt()).isEqualTo(old.originalExpiresAt());
        }

        @Test
        void a_second_rotation_cannot_give_the_old_token_time_back() {
            IssuedConnectorToken original = issuedReadWriteToken();
            service.rotateToken(original.tokenId(), Duration.ofDays(1), ADMIN);

            service.rotateToken(original.tokenId(), Duration.ofDays(14), ADMIN);

            assertThat(tokens.findById(original.tokenId()).orElseThrow().expiresAt())
                    .isEqualTo(NOW.plus(Duration.ofDays(1)));
        }

        @Test
        void no_overlap_ends_the_old_token_at_the_rotation() {
            IssuedConnectorToken original = issuedReadWriteToken();

            service.rotateToken(original.tokenId(), null, ADMIN);

            ScimConnectorToken old = tokens.findById(original.tokenId()).orElseThrow();
            assertThat(old.expiresAt()).isEqualTo(NOW);
            assertThat(old.isUsable(NOW)).isFalse();
        }

        @Test
        void rotation_records_the_replacement_on_the_old_token() {
            IssuedConnectorToken original = issuedReadWriteToken();

            IssuedConnectorToken replacement =
                    service.rotateToken(original.tokenId(), Duration.ofDays(1), ADMIN);

            assertThat(tokens.findById(original.tokenId()).orElseThrow().replacedByTokenId())
                    .isEqualTo(replacement.tokenId());
        }

        @Test
        void rotation_records_one_event_naming_the_connector() {
            IssuedConnectorToken original = issuedReadWriteToken();
            audit.reset();

            service.rotateToken(original.tokenId(), Duration.ofDays(1), ADMIN);

            assertThat(audit.of(AuditOperation.CONNECTOR_TOKEN_ROTATE)).singleElement()
                    .satisfies(event -> {
                        assertThat(event.actorId()).isEqualTo(adminId);
                        assertThat(event.subjectId()).isEqualTo(original.connectorId());
                    });
        }

        @Test
        void revocation_is_immediate_and_records_one_event() {
            IssuedConnectorToken issued = issuedReadWriteToken();
            audit.reset();

            service.revokeToken(issued.tokenId(), ADMIN);

            ScimConnectorToken revoked = tokens.findById(issued.tokenId()).orElseThrow();
            assertThat(revoked.revokedAt()).isEqualTo(NOW);
            assertThat(revoked.isUsable(NOW)).isFalse();
            assertThat(audit.of(AuditOperation.CONNECTOR_TOKEN_REVOKE)).singleElement()
                    .satisfies(event -> assertThat(event.actorId()).isEqualTo(adminId));
        }

        @Test
        void revoking_twice_keeps_the_first_time_and_still_records_the_second_attempt() {
            IssuedConnectorToken issued = issuedReadWriteToken();
            service.revokeToken(issued.tokenId(), ADMIN);
            clock.advanceBy(Duration.ofMinutes(5));
            audit.reset();

            service.revokeToken(issued.tokenId(), ADMIN);

            assertThat(tokens.findById(issued.tokenId()).orElseThrow().revokedAt())
                    .isEqualTo(NOW);
            assertThat(audit.of(AuditOperation.CONNECTOR_TOKEN_REVOKE)).hasSize(1);
        }

        @Test
        void an_unknown_token_id_is_refused_by_both_rotate_and_revoke() {
            UUID unknown = UUID.randomUUID();

            assertThatThrownBy(() -> service.rotateToken(unknown, null, ADMIN))
                    .isInstanceOf(UnknownConnectorException.class);
            assertThatThrownBy(() -> service.revokeToken(unknown, ADMIN))
                    .isInstanceOf(UnknownConnectorException.class);
        }

        @Test
        void the_listing_reports_a_tokens_state_as_of_now() {
            IssuedConnectorToken issued = issuedReadWriteToken();

            List<ConnectorTokenSummary> before = service.listConnectors().getFirst().tokens();
            assertThat(before).singleElement().satisfies(token -> {
                assertThat(token.id()).isEqualTo(issued.tokenId());
                assertThat(token.scope()).isEqualTo(ConnectorTokenScope.READ_WRITE);
                assertThat(token.issuedAt()).isEqualTo(NOW);
                assertThat(token.expiresAt()).isEqualTo(issued.expiresAt());
                assertThat(token.originalExpiresAt()).isEqualTo(issued.expiresAt());
                assertThat(token.revokedAt()).isNull();
                assertThat(token.active()).isTrue();
            });

            service.revokeToken(issued.tokenId(), ADMIN);

            assertThat(service.listConnectors().getFirst().tokens())
                    .singleElement()
                    .satisfies(token -> {
                        assertThat(token.active()).isFalse();
                        assertThat(token.revokedAt()).isEqualTo(NOW);
                    });
        }

        /**
         * A rotated token's projection reports the SHORTENED expiry beside the issued
         * one, which is the pair an Admin reads to see an overlap window in force.
         */
        @Test
        void the_listing_shows_a_shortened_overlap_beside_the_issued_expiry() {
            IssuedConnectorToken original = issuedReadWriteToken();
            service.rotateToken(original.tokenId(), Duration.ofDays(7), ADMIN);

            assertThat(service.listConnectors().getFirst().tokens())
                    .filteredOn(token -> token.id().equals(original.tokenId()))
                    .singleElement()
                    .satisfies(token -> {
                        assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
                        assertThat(token.originalExpiresAt()).isEqualTo(original.expiresAt());
                    });
        }
    }

    /**
     * An empty token list rather than a null one, so a caller iterating a connector's
     * tokens never has to check. Asserted directly on the projection because no use
     * case produces the null — and a defensive coercion nothing exercises is a
     * coercion that quietly stops working.
     */
    @Test
    void a_connector_projection_normalises_a_missing_token_list_to_an_empty_one() {
        ConnectorSummary nullTokens =
                new ConnectorSummary(UUID.randomUUID(), "Okta", NOW, null);

        assertThat(nullTokens.tokens()).isEmpty();
    }

    /**
     * Each lifecycle write reports itself to the log stream, as a named action and
     * outcome and nothing else.
     *
     * <p>Asserted rather than assumed because a void call leaves no return value a
     * mutation could be traced through: {@code succeeded(...)} could be deleted from
     * all five use cases and every other test here would still pass. What is checked
     * is also what must NOT be there — no connector id, no token id, and no token
     * value, because the log stream is read by more people and kept longer than the
     * audit trail that names a subject properly.
     */
    @Test
    void every_lifecycle_write_reports_its_action_to_the_log_stream() {
        try (CapturedLog log = CapturedLog.attach()) {
            ConnectorSummary connector = service.create("Okta", ADMIN);
            IssuedConnectorToken issued = service.issueToken(
                    connector.id(), ConnectorTokenScope.READ_WRITE, null, ADMIN);
            IssuedConnectorToken rotated =
                    service.rotateToken(issued.tokenId(), Duration.ofDays(1), ADMIN);
            service.revokeToken(rotated.tokenId(), ADMIN);
            service.delete(connector.id(), ADMIN);

            List<String> actions = List.of(
                    "scim.connector.create",
                    "scim.connector.token.issue",
                    "scim.connector.token.rotate",
                    "scim.connector.token.revoke",
                    "scim.connector.delete");
            for (String action : actions) {
                assertThat(log.withAction(Level.INFO, LogEvent.ACTION, action))
                        .as("%s", action)
                        .singleElement()
                        .satisfies(record -> {
                            assertThat(CapturedLog.fields(record))
                                    .containsEntry(LogEvent.OUTCOME, LogEvent.SUCCESS);
                            assertThat(record.getMessage())
                                    .isEqualTo("SCIM connector lifecycle change applied");
                            assertThat(record.getFormattedMessage())
                                    .doesNotContain(connector.id().toString())
                                    .doesNotContain(issued.tokenId().toString())
                                    .doesNotContain(issued.presentedValue())
                                    .doesNotContain(rotated.presentedValue());
                            assertThat(CapturedLog.fields(record).values())
                                    .allSatisfy(value -> assertThat(String.valueOf(value))
                                            .doesNotContain(issued.presentedValue())
                                            .doesNotContain(rotated.presentedValue()));
                        });
            }
        }
    }

    /**
     * An administrator whose username resolves to no account still gets the event,
     * with no actor. The alternative — refusing the operation — would make a rename
     * between authentication and this call fail an administrative action.
     */
    @Test
    void an_unresolvable_administrator_leaves_the_actor_empty_rather_than_failing() {
        ConnectorSummary created = service.create("Okta", "not-an-account");

        assertThat(audit.of(AuditOperation.CONNECTOR_CREATE)).singleElement()
                .satisfies(event -> {
                    assertThat(event.actorId()).isNull();
                    assertThat(event.subjectId()).isEqualTo(created.id());
                });
    }

    private IssuedConnectorToken issuedReadWriteToken() {
        ConnectorSummary connector = service.create("Okta", ADMIN);
        return service.issueToken(
                connector.id(), ConnectorTokenScope.READ_WRITE, null, ADMIN);
    }
}
