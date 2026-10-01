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
import com.example.backend.scim.domain.ScimSeedLock;
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

    private final RecordingSeedLock seedLock = new RecordingSeedLock();

    private final ScimSeedService seeding = new ScimSeedService(
            users,
            groups,
            seedLock,
            audit,
            passwordEncoder,
            Clock.fixed(ScimIdentities.NOW, ZoneOffset.UTC));

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
        assertThat(ordinary.login().isPasswordChangeRequired()).isFalse();
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
        // Each event names the resource it seeded, so the trail can be joined to the directory.
        assertThat(audit.of(AuditOperation.SCIM_RESOURCE_SEED))
                .extracting(RecordingAuditTrail.Recorded::subjectId)
                .containsExactlyInAnyOrder(
                        users.findByReservedName(ReservedResourceName.BOOTSTRAP_ADMIN)
                                .orElseThrow().id(),
                        groups.findByReservedName(ReservedResourceName.ADMIN_GROUP)
                                .orElseThrow().id());
    }

    /**
     * Both identities are stamped with the seeding clock and carry their userName as their
     * displayName, which is what the Accounts page lists them by.
     */
    @Test
    void the_seeded_identities_are_created_now_and_displayed_by_their_user_name() {
        seeding.seed(ORDINARY, RECOVERY);

        assertThat(List.of(users.require("user"), users.require("admin")))
                .allSatisfy(seeded -> {
                    assertThat(seeded.createdAt()).isEqualTo(ScimIdentities.NOW);
                    assertThat(seeded.lastModifiedAt()).isEqualTo(ScimIdentities.NOW);
                    assertThat(seeded.profile().displayName())
                            .isEqualTo(seeded.profile().userName());
                });
    }

    /**
     * The Bootstrap Admin's password comes from deployment configuration, with working fallbacks
     * on a public remote: a default credential, which IM8 ac-6 and the spec's
     * "seeded with mustChangePassword set" both require be replaced on first use. Dated by the
     * seeding clock.
     */
    @Test
    void the_bootstrap_admin_is_seeded_with_a_password_change_required() {
        seeding.seed(ORDINARY, RECOVERY);

        ScimUser bootstrapAdmin =
                users.findByReservedName(ReservedResourceName.BOOTSTRAP_ADMIN).orElseThrow();
        assertThat(bootstrapAdmin.login().passwordChangeRequiredSince())
                .isEqualTo(ScimIdentities.NOW);
    }

    /**
     * A restart does not re-impose the change on a Bootstrap Admin that already completed it:
     * that would force a fresh credential on every boot.
     */
    @Test
    void a_later_run_does_not_re_flag_a_bootstrap_admin_that_changed_its_password() {
        seeding.seed(ORDINARY, RECOVERY);
        ScimUser bootstrapAdmin =
                users.findByReservedName(ReservedResourceName.BOOTSTRAP_ADMIN).orElseThrow();
        users.completePasswordChange(bootstrapAdmin.id(), "hashed:rotated", ScimIdentities.NOW);

        seeding.seed(ORDINARY, RECOVERY);

        assertThat(users.findById(bootstrapAdmin.id()).orElseThrow()
                        .login().isPasswordChangeRequired())
                .isFalse();
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

    /**
     * Every run takes the seeding lock, and takes it before it creates anything: the reads decide
     * what to write, so a read taken before the lock could see a state a concurrent seed is about
     * to change. The integration test shows the lock really serializes; this one shows seeding
     * asks for it.
     */
    @Test
    void every_run_takes_the_seeding_lock_before_creating_anything() {
        seeding.seed(ORDINARY, RECOVERY);
        seeding.seed(ORDINARY, RECOVERY);

        assertThat(seedLock.usersSeenAtEachAcquire)
                .as("one acquisition per run, each before that run created anything")
                .containsExactly(0L, 2L);
    }

    /**
     * The ordinary identity is a first-run convenience, not a recovery resource: once seeding has
     * completed it is an ordinary provisionable User, and a restart that recreated it would undo a
     * deliberate deletion.
     */
    @Test
    void a_later_run_does_not_recreate_a_deleted_ordinary_identity() {
        seeding.seed(ORDINARY, RECOVERY);
        users.deleteById(users.require("user").id(), ScimIdentities.NOW);

        seeding.seed(ORDINARY, RECOVERY);

        assertThat(users.findByNormalizedUserName(NormalizedUserName.of("user"))).isEmpty();
    }

    /**
     * A run that finds the recovery path already there does no hashing at all. Hashing is the
     * expensive step, and a count that does not move shows neither identity was even prepared for
     * a write on a restart.
     */
    @Test
    void a_later_run_hashes_nothing() {
        seeding.seed(ORDINARY, RECOVERY);
        int afterFirst = passwordEncoder.encodes;

        seeding.seed(ORDINARY, RECOVERY);

        assertThat(passwordEncoder.encodes).isEqualTo(afterFirst);
    }

    /**
     * A database that already has a User under the ordinary name but no Bootstrap Admin — a User
     * provisioned before seeding first ran — is seeded around it. The existing User is not
     * replaced, and its presence is found by a lookup rather than by a failed INSERT, which against
     * Postgres would abort the seed transaction.
     */
    @Test
    void a_first_run_leaves_an_existing_user_under_the_ordinary_name_alone() {
        ScimUser existing = users.create(ScimIdentities.user("user"));

        seeding.seed(ORDINARY, RECOVERY);

        assertThat(users.require("user").id()).isEqualTo(existing.id());
        assertThat(users.findByReservedName(ReservedResourceName.BOOTSTRAP_ADMIN)).isPresent();
        assertThat(passwordEncoder.encodes)
                .as("only the Bootstrap Admin's password was hashed")
                .isEqualTo(1);
    }

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

    /** Records how many Users existed at each acquisition, so a test can see WHEN it was taken. */
    private final class RecordingSeedLock implements ScimSeedLock {

        private final List<Long> usersSeenAtEachAcquire = new ArrayList<>();

        @Override
        public void acquire() {
            usersSeenAtEachAcquire.add(users.size());
        }
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
