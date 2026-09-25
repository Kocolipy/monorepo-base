package com.example.backend.scim.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The token aggregate's own invariants. */
class ScimConnectorTokenTests {

    private static final Instant ISSUED = Instant.parse("2026-01-01T00:00:00Z");

    private static final Instant EXPIRES = ISSUED.plus(Duration.ofDays(365));

    private static final UUID CONNECTOR = UUID.randomUUID();

    @Test
    void a_newly_issued_token_has_its_issued_expiry_as_its_original() {
        ScimConnectorToken token = issued();

        assertThat(token.expiresAt()).isEqualTo(EXPIRES);
        assertThat(token.originalExpiresAt()).isEqualTo(EXPIRES);
        assertThat(token.revokedAt()).isNull();
        assertThat(token.replacedByTokenId()).isNull();
    }

    @Test
    void a_token_is_usable_before_its_expiry_and_not_after() {
        ScimConnectorToken token = issued();

        assertThat(token.isUsable(ISSUED)).isTrue();
        assertThat(token.isUsable(EXPIRES.minusMillis(1))).isTrue();
        assertThat(token.isUsable(EXPIRES)).isFalse();
        assertThat(token.isUsable(EXPIRES.plusSeconds(1))).isFalse();
    }

    @Test
    void a_revoked_token_is_unusable_immediately_however_far_off_its_expiry_is() {
        ScimConnectorToken revoked = issued().revoked(ISSUED.plusSeconds(60));

        assertThat(revoked.isRevoked()).isTrue();
        assertThat(revoked.isUsable(ISSUED.plusSeconds(61))).isFalse();
    }

    @Test
    void revoking_twice_keeps_the_first_revocation_time() {
        ScimConnectorToken first = issued().revoked(ISSUED.plusSeconds(60));

        ScimConnectorToken second = first.revoked(ISSUED.plusSeconds(120));

        assertThat(second).isSameAs(first);
        assertThat(second.revokedAt()).isEqualTo(ISSUED.plusSeconds(60));
    }

    @Test
    void rotation_brings_the_expiry_forward_and_records_the_replacement() {
        UUID replacement = UUID.randomUUID();
        Instant overlapEnd = ISSUED.plus(Duration.ofDays(14));

        ScimConnectorToken rotated = issued().expiringAt(overlapEnd, replacement);

        assertThat(rotated.expiresAt()).isEqualTo(overlapEnd);
        assertThat(rotated.originalExpiresAt()).isEqualTo(EXPIRES);
        assertThat(rotated.replacedByTokenId()).isEqualTo(replacement);
    }

    /**
     * The aggregate refuses rather than clamps. Clamping here would make the policy's
     * arithmetic unfalsifiable: a caller that computed the wrong window would get a
     * silently corrected token and no test would see the mistake.
     */
    @Test
    void rotation_refuses_an_expiry_later_than_the_current_one() {
        assertThatThrownBy(() -> issued()
                        .expiringAt(EXPIRES.plusSeconds(1), UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("forward");
    }

    /** The same rule stated on the constructor, which is what persistence goes through. */
    @Test
    void a_token_cannot_be_constructed_with_an_expiry_past_the_one_it_was_issued_with() {
        assertThatThrownBy(() -> new ScimConnectorToken(
                        UUID.randomUUID(),
                        CONNECTOR,
                        "lookup",
                        ConnectorTokenDigest.of("lookup.secret"),
                        ConnectorTokenScope.READ_ONLY,
                        ISSUED,
                        EXPIRES.plusSeconds(1),
                        EXPIRES,
                        null,
                        null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void a_connector_is_live_until_it_is_deleted_and_keeps_its_first_deletion_time() {
        ScimConnector connector = ScimConnector.create(CONNECTOR, "Okta", ISSUED);

        assertThat(connector.isDeleted()).isFalse();

        ScimConnector deleted = connector.deleted(ISSUED.plusSeconds(10));
        assertThat(deleted.isDeleted()).isTrue();
        assertThat(deleted.deletedAt()).isEqualTo(ISSUED.plusSeconds(10));

        assertThat(deleted.deleted(ISSUED.plusSeconds(99))).isSameAs(deleted);
    }

    private static ScimConnectorToken issued() {
        return ScimConnectorToken.issue(
                UUID.randomUUID(),
                CONNECTOR,
                "lookup",
                ConnectorTokenDigest.of("lookup.secret"),
                ConnectorTokenScope.READ_WRITE,
                ISSUED,
                EXPIRES);
    }
}
