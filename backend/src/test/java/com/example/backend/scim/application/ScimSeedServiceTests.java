package com.example.backend.scim.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.audit.RecordingAuditTrail;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.scim.InMemoryScimGroupRepository;
import com.example.backend.scim.InMemoryScimUserRepository;
import com.example.backend.scim.ScimIdentities;
import com.example.backend.scim.application.ScimSeedService.SeededIdentity;
import com.example.backend.scim.domain.NormalizedUserName;
import com.example.backend.scim.domain.ReservedResourceName;
import com.example.backend.scim.domain.ScimGroup;
import com.example.backend.scim.domain.ScimGroupMember;
import com.example.backend.scim.domain.ScimUser;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeding the deployment's recovery path.
 *
 * <p>This is the ticket's fourth acceptance criterion at the level below HTTP: on a fresh database a
 * Bootstrap Admin and an Admin group exist, with the Bootstrap Admin an immutable member.
 *
 * <p>The idempotence assertions matter more than they look. Seeding runs on every startup, so a
 * version of it that overwrote would reset the recovery credential on every restart — undoing a
 * deliberate rotation — and one that created rather than found would accumulate Admin groups.
 */
class ScimSeedServiceTests {

    private static final SeededIdentity ORDINARY = new SeededIdentity("user", "user-password");

    private static final SeededIdentity RECOVERY = new SeededIdentity("admin", "admin-password");

    private final InMemoryScimUserRepository users = new InMemoryScimUserRepository();

    private final InMemoryScimGroupRepository groups = new InMemoryScimGroupRepository(users);

    private final RecordingAuditTrail audit = new RecordingAuditTrail();

    private final CountingPasswordEncoder passwordEncoder = new CountingPasswordEncoder();

    private final ScimSeedService seeding = new ScimSeedService(
            users, groups, audit, passwordEncoder, Clock.fixed(ScimIdentities.NOW, ZoneOffset.UTC));

    @Test
    void a_fresh_database_gets_both_configured_identities() {
        seeding.seed(ORDINARY, RECOVERY);

        assertThat(users.findByNormalizedUserName(NormalizedUserName.of("user"))).isPresent();
        assertThat(users.findByReservedName(ReservedResourceName.BOOTSTRAP_ADMIN)).isPresent();
    }

    @Test
    void the_recovery_identity_is_the_secondary_one_and_is_reserved_as_the_bootstrap_admin() {
        seeding.seed(ORDINARY, RECOVERY);

        ScimUser bootstrapAdmin =
                users.findByReservedName(ReservedResourceName.BOOTSTRAP_ADMIN).orElseThrow();
        assertThat(bootstrapAdmin.profile().userName()).isEqualTo("admin");
        assertThat(bootstrapAdmin.isProtectedFromWrites()).isTrue();
        assertThat(bootstrapAdmin.isExemptFromLockout()).isTrue();
    }

    /**
     * The ordinary identity is NOT reserved and is in no Group, so it holds baseline access and
     * nothing more. A seeding that reserved both would give the deployment two unprotectable
     * identities and no ordinary one to exercise the non-administrative paths with.
     */
    @Test
    void the_ordinary_identity_is_unreserved_and_in_no_group() {
        seeding.seed(ORDINARY, RECOVERY);

        ScimUser ordinary = users.require("user");
        assertThat(ordinary.isProtectedFromWrites()).isFalse();
        assertThat(groups.findGroupsOfUser(ordinary.id())).isEmpty();
    }

    @Test
    void a_fresh_database_gets_an_admin_group_with_the_bootstrap_admin_in_it() {
        seeding.seed(ORDINARY, RECOVERY);

        ScimUser bootstrapAdmin =
                users.findByReservedName(ReservedResourceName.BOOTSTRAP_ADMIN).orElseThrow();
        ScimGroup adminGroup =
                groups.findByReservedName(ReservedResourceName.ADMIN_GROUP).orElseThrow();

        assertThat(adminGroup.isProtectedFromWrites()).isTrue();
        assertThat(adminGroup.hasMember(bootstrapAdmin.id())).isTrue();
    }

    /**
     * The membership is what makes the Bootstrap Admin an administrator: there is no role column, so
     * a seeding that created the User without the membership would produce a recovery identity with
     * nothing to recover with.
     */
    @Test
    void the_bootstrap_admin_derives_administrative_authority_from_that_membership() {
        seeding.seed(ORDINARY, RECOVERY);

        ScimUser bootstrapAdmin =
                users.findByReservedName(ReservedResourceName.BOOTSTRAP_ADMIN).orElseThrow();

        assertThat(groups.isMemberOfReservedGroup(
                        bootstrapAdmin.id(), ReservedResourceName.ADMIN_GROUP))
                .isTrue();
        assertThat(groups.isMemberOfReservedGroup(
                        users.require("user").id(), ReservedResourceName.ADMIN_GROUP))
                .isFalse();
    }

    @Test
    void each_seeded_reserved_resource_records_its_own_event_with_no_actor() {
        seeding.seed(ORDINARY, RECOVERY);

        assertThat(audit.of(AuditOperation.SCIM_RESOURCE_SEED)).hasSize(2)
                .allSatisfy(event -> assertThat(event.actorId())
                        .as("seeding is the deployment acting, not a principal")
                        .isNull())
                .extracting(RecordingAuditTrail.Recorded::detail)
                .containsExactlyInAnyOrder("User", "Group");
    }

    /** The ordinary identity is not a reserved resource, so its creation records no seed event. */
    @Test
    void the_ordinary_identity_records_no_seed_event() {
        seeding.seed(ORDINARY, RECOVERY);

        assertThat(audit.of(AuditOperation.SCIM_RESOURCE_SEED))
                .extracting(RecordingAuditTrail.Recorded::subjectId)
                .doesNotContain(users.require("user").id());
    }

    // ---- idempotence --------------------------------------------------------------------------

    @Test
    void a_second_run_creates_nothing_and_records_nothing() {
        seeding.seed(ORDINARY, RECOVERY);
        long usersAfterFirst = users.size();
        long groupsAfterFirst = groups.size();
        audit.reset();

        seeding.seed(ORDINARY, RECOVERY);

        assertThat(users.size()).isEqualTo(usersAfterFirst);
        assertThat(groups.size()).isEqualTo(groupsAfterFirst);
        assertThat(audit.recorded()).isEmpty();
    }

    /**
     * A restart must not reset the recovery credential to the configured one: an operator who
     * rotated it deliberately would find the rotation silently undone.
     */
    @Test
    void a_second_run_does_not_overwrite_the_recovery_password() {
        seeding.seed(ORDINARY, RECOVERY);
        ScimUser afterFirst =
                users.findByReservedName(ReservedResourceName.BOOTSTRAP_ADMIN).orElseThrow();

        seeding.seed(ORDINARY, new SeededIdentity("admin", "a-different-password"));

        assertThat(users.findByReservedName(ReservedResourceName.BOOTSTRAP_ADMIN).orElseThrow()
                        .login().passwordHash())
                .isEqualTo(afterFirst.login().passwordHash());
    }

    @Test
    void a_second_run_does_not_reset_the_recovery_identitys_failure_run_or_lock() {
        seeding.seed(ORDINARY, RECOVERY);
        ScimUser bootstrapAdmin =
                users.findByReservedName(ReservedResourceName.BOOTSTRAP_ADMIN).orElseThrow();
        users.updateLoginState(
                bootstrapAdmin.id(), bootstrapAdmin.login().withFailureCounted());

        seeding.seed(ORDINARY, RECOVERY);

        assertThat(users.findById(bootstrapAdmin.id()).orElseThrow()
                        .login().failedLoginAttempts())
                .isEqualTo(1);
    }

    /** Ordinary membership provisioning put into the Admin group survives a restart. */
    @Test
    void a_second_run_leaves_ordinary_admin_group_membership_alone() {
        seeding.seed(ORDINARY, RECOVERY);
        ScimGroup adminGroup =
                groups.findByReservedName(ReservedResourceName.ADMIN_GROUP).orElseThrow();
        ScimUser promoted = users.create(ScimIdentities.user("carol"));
        List<ScimGroupMember> withCarol = new ArrayList<>(adminGroup.members());
        withCarol.add(ScimGroupMember.reference(promoted.id()));
        groups.replace(adminGroup.replacedWith(
                adminGroup.displayName(), withCarol, ScimIdentities.NOW));

        seeding.seed(ORDINARY, RECOVERY);

        assertThat(groups.findByReservedName(ReservedResourceName.ADMIN_GROUP).orElseThrow()
                        .hasMember(promoted.id()))
                .isTrue();
    }

    /**
     * If something outside SCIM removed the Bootstrap Admin from the Admin group, seeding restores
     * it — the SCIM surface refuses to remove that membership, so its absence means the deployment's
     * authority was broken by something else, and leaving it broken serves nobody.
     */
    @Test
    void a_later_run_restores_the_bootstrap_admins_membership_if_it_went_missing() {
        seeding.seed(ORDINARY, RECOVERY);
        ScimGroup adminGroup =
                groups.findByReservedName(ReservedResourceName.ADMIN_GROUP).orElseThrow();
        ScimUser bootstrapAdmin =
                users.findByReservedName(ReservedResourceName.BOOTSTRAP_ADMIN).orElseThrow();
        groups.given(new ScimGroup(
                adminGroup.id(),
                adminGroup.displayName(),
                List.of(),
                ReservedResourceName.ADMIN_GROUP,
                adminGroup.version(),
                adminGroup.createdAt(),
                adminGroup.lastModifiedAt()));

        seeding.seed(ORDINARY, RECOVERY);

        assertThat(groups.findByReservedName(ReservedResourceName.ADMIN_GROUP).orElseThrow()
                        .hasMember(bootstrapAdmin.id()))
                .isTrue();

        // The restore must leave a RECORD, not just a restored membership. This assertion was
        // missing, so the test passed while a write that changes who holds Admin authority emitted
        // nothing: an operator would find neither the outside-SCIM removal nor the startup that
        // undid it. Recorded against the Group, whose representation is what changed.
        assertThat(audit.of(AuditOperation.SCIM_RESOURCE_SEED))
                .as("restoring the recovery identity's Admin membership is an audited write")
                .anySatisfy(recorded -> {
                    assertThat(recorded.subjectId()).isEqualTo(adminGroup.id());
                    assertThat(recorded.detail()).isEqualTo("members-restored");
                });
    }

    /** A run that finds the membership intact restores nothing, so it records no restore. */
    @Test
    void a_later_run_that_finds_the_membership_intact_records_no_restore() {
        seeding.seed(ORDINARY, RECOVERY);
        audit.reset();

        seeding.seed(ORDINARY, RECOVERY);

        assertThat(audit.of(AuditOperation.SCIM_RESOURCE_SEED))
                .as("an idempotent restart is not an authority change and must not read as one")
                .noneSatisfy(recorded ->
                        assertThat(recorded.detail()).isEqualTo("members-restored"));
    }

    /**
     * The seeded passwords are hashed, never stored as submitted. Asserted through the encoder's
     * invocation count and the stored value differing from the plaintext, because the real encoder's
     * output cannot be predicted.
     */
    @Test
    void the_seeded_passwords_are_hashed_before_they_are_stored() {
        seeding.seed(ORDINARY, RECOVERY);

        assertThat(passwordEncoder.encodes).isEqualTo(2);
        assertThat(users.require("admin").login().passwordHash()).isNotEqualTo("admin-password");
        assertThat(users.require("user").login().passwordHash()).isNotEqualTo("user-password");
    }

    /**
     * A {@link PasswordEncoder} that counts, so a test can assert hashing HAPPENED rather than
     * assert a value it would have to predict.
     */
    private static final class CountingPasswordEncoder implements PasswordEncoder {

        private int encodes;

        @Override
        public String encode(CharSequence rawPassword) {
            encodes++;
            return hash(rawPassword);
        }

        /**
         * Deliberately does NOT go through {@link #encode}, which would inflate the count this
         * class exists to report.
         */
        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return hash(rawPassword).equals(encodedPassword);
        }

        private static String hash(CharSequence rawPassword) {
            return "hashed:" + UUID.nameUUIDFromBytes(rawPassword.toString().getBytes());
        }
    }
}
