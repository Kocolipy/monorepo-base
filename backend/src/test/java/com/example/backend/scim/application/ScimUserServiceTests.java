package com.example.backend.scim.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.audit.RecordingAuditTrail;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.scim.InMemoryScimExternalIdRepository;
import com.example.backend.scim.InMemoryScimGroupRepository;
import com.example.backend.scim.InMemoryScimPasswordHistoryRepository;
import com.example.backend.scim.InMemoryScimQueryRepository;
import com.example.backend.scim.InMemoryScimTombstoneRepository;
import com.example.backend.scim.InMemoryScimUserRepository;
import com.example.backend.scim.ScimIdentities;
import com.example.backend.scim.domain.AuthenticatedConnector;
import com.example.backend.scim.domain.ConnectorTokenScope;
import com.example.backend.scim.domain.DuplicateUserNameException;
import com.example.backend.scim.domain.PasswordReusedException;
import com.example.backend.scim.domain.PreconditionFailedException;
import com.example.backend.scim.domain.PreconditionRequiredException;
import com.example.backend.scim.domain.ProtectedResourceException;
import com.example.backend.scim.domain.ReservedResourceName;
import com.example.backend.scim.domain.ScimEmail;
import com.example.backend.scim.domain.ScimEmailFilter;
import com.example.backend.scim.domain.ScimEmailPart;
import com.example.backend.scim.domain.ScimName;
import com.example.backend.scim.domain.ScimPatchRefusedException;
import com.example.backend.scim.domain.ScimUser;
import com.example.backend.scim.domain.ScimUserPatchOperation;
import com.example.backend.scim.domain.ScimUserPatchOperation.RemoveEmails;
import com.example.backend.scim.domain.ScimUserPatchOperation.RemovePassword;
import com.example.backend.scim.domain.ScimUserPatchOperation.RemoveText;
import com.example.backend.scim.domain.ScimUserPatchOperation.SetActive;
import com.example.backend.scim.domain.ScimUserPatchOperation.SetPassword;
import com.example.backend.scim.domain.ScimUserPatchOperation.SetText;
import com.example.backend.scim.domain.ScimUserPatchOperation.TextAttribute;
import com.example.backend.scim.domain.ScimUserProfile;
import com.example.backend.scim.domain.ScimUserSessions;
import com.example.backend.scim.domain.ScimUserSessions.Cause;
import com.example.backend.scim.domain.ScimVersionPrecondition;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The User write use cases over in-memory ports: replacement and partial-update semantics, the
 * order of the refusals, the password history, which changes end sessions, and what each write
 * records. The same behaviour against Postgres and Redis is in
 * {@code ScimConditionalWriteIntegrationTests}.
 */
class ScimUserServiceTests {

    private static final AuthenticatedConnector CONNECTOR = new AuthenticatedConnector(
            UUID.randomUUID(), UUID.randomUUID(), ConnectorTokenScope.READ_WRITE);

    private static final AuthenticatedConnector OTHER_CONNECTOR = new AuthenticatedConnector(
            UUID.randomUUID(), UUID.randomUUID(), ConnectorTokenScope.READ_WRITE);

    private static final Instant LATER = ScimIdentities.NOW.plusSeconds(60);

    /** Revocations requested of the session port, in order. */
    private record Revocation(UUID connectorId, UUID userId, Set<Cause> causes) {
    }

    /**
     * A salted-looking fake: each encode yields a distinct string, as Argon2id's random salt does,
     * so "the credential changed" cannot be faked by an equal hash, and matching compares the
     * plaintext part only. Counts matches, so a test can see the history was actually consulted.
     */
    private static final class FakeSaltedEncoder implements PasswordEncoder {

        private int salt;
        private int matches;

        @Override
        public String encode(CharSequence raw) {
            return "{fake}" + (salt++) + ":" + raw;
        }

        @Override
        public boolean matches(CharSequence raw, String encoded) {
            matches++;
            return encoded.substring(encoded.indexOf(':') + 1).equals(raw.toString());
        }
    }

    private final InMemoryScimUserRepository users = new InMemoryScimUserRepository();

    private final InMemoryScimGroupRepository groups = new InMemoryScimGroupRepository(users);

    private final InMemoryScimExternalIdRepository aliases = new InMemoryScimExternalIdRepository();

    private final InMemoryScimPasswordHistoryRepository history =
            new InMemoryScimPasswordHistoryRepository();

    private final List<Revocation> revocations = new ArrayList<>();

    private final ScimUserSessions sessions = (connectorId, userId, causes) ->
            revocations.add(new Revocation(connectorId, userId, Set.copyOf(causes)));

    private final RecordingAuditTrail audit = new RecordingAuditTrail();

    private final FakeSaltedEncoder encoder = new FakeSaltedEncoder();

    private final Clock clock = Clock.fixed(LATER, ZoneOffset.UTC);

    private final InMemoryScimTombstoneRepository tombstones =
            new InMemoryScimTombstoneRepository();

    private final ScimUserService service = new ScimUserService(
            users, groups, aliases, history, sessions, tombstones, audit, encoder, clock,
            new InMemoryScimQueryRepository(users, groups));

    private ScimUserResource ada;

    private int writesAtSetUp;

    @BeforeEach
    void createAda() {
        ada = service.create(CONNECTOR, new NewScimUser(
                new ScimUserProfile("ada", new ScimName(null, "King", "Ada", null, null, null),
                        "Ada", "en", "en-GB", "Europe/London", true,
                        List.of(new ScimEmail("ada@work.example", "work", true))),
                "first-password-1",
                "ext-ada"));
        audit.reset();
        writesAtSetUp = users.writes();
    }

    /** Writes the store took after the fixture's own create. */
    private int writesSinceSetUp() {
        return users.writes() - writesAtSetUp;
    }

    private ScimVersionPrecondition current() {
        return ScimVersionPrecondition.ofIfMatch(
                List.of("\"" + users.findById(ada.id()).orElseThrow().version() + "\""));
    }

    private ScimUser stored() {
        return users.findById(ada.id()).orElseThrow();
    }

    /** A PUT; the third argument is the externalId the body carried. */
    private ScimUserResource put(ScimUserProfile profile, String password, String sentExternalId) {
        return service.replace(CONNECTOR, ada.id(), current(),
                new ScimUserReplacement(profile, password, sentExternalId)).orElseThrow();
    }

    private ScimUserResource patch(ScimUserPatchOperation... operations) {
        return service.patch(CONNECTOR, ada.id(), current(), List.of(operations)).orElseThrow();
    }

    private static ScimUserProfile minimal(String userName, boolean active) {
        return ScimIdentities.profile(userName, active);
    }

    // ---- creation starts the history ------------------------------------------------------

    @Test
    void a_created_password_starts_the_history_so_it_cannot_be_set_again() {
        assertThat(history.findRecentHashes(ada.id())).containsExactly(stored().login().passwordHash());
    }

    @Test
    void a_credentialless_create_records_no_history() {
        ScimUserResource grace = service.create(
                CONNECTOR, new NewScimUser(minimal("grace", true), null, null));

        assertThat(history.findRecentHashes(grace.id())).isEmpty();
    }

    // ---- full replacement -----------------------------------------------------------------

    /** Everything the document omits is unassigned, except the password, which is kept. */
    @Test
    void a_put_replaces_the_profile_clears_omitted_optionals_and_keeps_an_omitted_password() {
        String hashBefore = stored().login().passwordHash();

        ScimUserResource written = put(minimal("ada", true), null, "ext-ada");

        assertThat(written.profile()).isEqualTo(minimal("ada", true));
        assertThat(stored().profile().displayName()).isNull();
        assertThat(stored().profile().emails()).isEmpty();
        assertThat(stored().profile().name()).isEqualTo(ScimName.NONE);
        assertThat(stored().login().passwordHash())
                .as("an omitted password leaves the credential usable")
                .isEqualTo(hashBefore);
        assertThat(revocations).as("a profile-only change ends no session").isEmpty();
    }

    @Test
    void a_changing_write_advances_the_version_exactly_once_and_stamps_last_modified() {
        long before = stored().version();

        ScimUserResource written = put(minimal("ada", false), "second-password-2", null);

        assertThat(written.version()).isEqualTo(before + 1);
        assertThat(stored().version()).isEqualTo(before + 1);
        assertThat(stored().lastModifiedAt()).isEqualTo(LATER);
        assertThat(writesSinceSetUp()).as("one write, however many attributes moved").isEqualTo(1);
    }

    @Test
    void a_put_restating_the_stored_state_writes_nothing_and_advances_no_version() {
        ScimUser before = stored();

        ScimUserResource written = put(before.profile(), null, "ext-ada");

        assertThat(written.version()).isEqualTo(before.version());
        assertThat(stored()).isEqualTo(before);
        assertThat(writesSinceSetUp()).isZero();
        assertThat(audit.of(AuditOperation.SCIM_USER_REPLACE))
                .singleElement()
                .extracting(RecordingAuditTrail.Recorded::detail)
                .as("recorded, with nothing changed")
                .isEqualTo("");
    }

    @Test
    void a_write_is_audited_with_the_connector_and_exactly_the_attributes_that_moved() {
        put(new ScimUserProfile("ada", new ScimName(null, "King", "Ada", null, null, null),
                "Countess", "en", "en-GB", "Europe/London", true,
                List.of(new ScimEmail("ada@work.example", "work", true))), null, "ext-ada");

        assertThat(audit.of(AuditOperation.SCIM_USER_REPLACE))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.actorId()).isEqualTo(CONNECTOR.connectorId());
                    assertThat(event.subjectId()).isEqualTo(ada.id());
                    assertThat(event.detail()).isEqualTo("DISPLAY_NAME");
                });
    }

    @Test
    void every_attribute_that_moves_is_named_in_the_event() {
        put(new ScimUserProfile("ada2", ScimName.NONE, null, "fr", "fr-FR", "Europe/Paris",
                false, List.of()), "second-password-2", "ext-ada");

        assertThat(audit.of(AuditOperation.SCIM_USER_REPLACE)).singleElement()
                .extracting(RecordingAuditTrail.Recorded::detail)
                .isEqualTo("ACTIVE,DISPLAY_NAME,EMAILS,LOCALE,NAME,PASSWORD,"
                        + "PREFERRED_LANGUAGE,TIMEZONE,USER_NAME");
    }

    // ---- aliases ------------------------------------------------------------------------

    /**
     * The alias is fixed at creation, as a Group's is: a PUT carrying another value or none at
     * all leaves it — and every other connector's — exactly as it was, and moves no version.
     */
    @Test
    void a_put_restating_or_omitting_the_alias_leaves_every_connectors_alias_alone() {
        aliases.put(OTHER_CONNECTOR.connectorId(), ada.id(), "theirs");
        long before = stored().version();

        assertThat(put(stored().profile(), null, "ext-ada").externalId()).isEqualTo("ext-ada");
        ScimUserResource written = put(stored().profile(), null, null);

        assertThat(written.externalId()).isEqualTo("ext-ada");
        assertThat(written.version()).isEqualTo(before);
        assertThat(aliases.find(CONNECTOR.connectorId(), ada.id())).contains("ext-ada");
        assertThat(aliases.find(OTHER_CONNECTOR.connectorId(), ada.id())).contains("theirs");
    }

    /** Refused, not dropped: PATCH answers the same attempt with the same refusal. */
    @Test
    void a_put_asserting_a_different_alias_is_refused_as_mutability_and_changes_nothing() {
        ScimUser before = stored();

        assertThatThrownBy(() -> put(minimal("ada", false), null, "ext-other"))
                .isInstanceOfSatisfying(ScimPatchRefusedException.class, refused ->
                        assertThat(refused.reason())
                                .isEqualTo(ScimPatchRefusedException.Reason.MUTABILITY));

        assertThat(stored()).isEqualTo(before);
        assertThat(aliases.find(CONNECTOR.connectorId(), ada.id())).contains("ext-ada");
        assertThat(audit.of(AuditOperation.SCIM_USER_REPLACE)).singleElement()
                .extracting(RecordingAuditTrail.Recorded::detail).isEqualTo("MUTABILITY");
    }

    // ---- the order of refusals ------------------------------------------------------------

    /** Existence comes before the precondition, so a missing header cannot disclose an id. */
    @Test
    void an_unknown_id_is_absent_whatever_the_precondition_and_nothing_is_recorded() {
        assertThat(service.replace(CONNECTOR, UUID.randomUUID(),
                ScimVersionPrecondition.ofIfMatch(List.of()),
                new ScimUserReplacement(minimal("x", true), null, null))).isEmpty();
        assertThat(service.patch(CONNECTOR, UUID.randomUUID(),
                ScimVersionPrecondition.ofIfMatch(List.of()),
                List.of(new SetActive(false)))).isEmpty();
        assertThat(audit.recorded()).isEmpty();
    }

    @Test
    void a_missing_or_stale_precondition_changes_nothing_and_records_nothing() {
        ScimUser before = stored();

        assertThatThrownBy(() -> service.patch(CONNECTOR, ada.id(),
                ScimVersionPrecondition.ofIfMatch(List.of()), List.of(new SetActive(false))))
                .isInstanceOf(PreconditionRequiredException.class);
        assertThatThrownBy(() -> service.replace(CONNECTOR, ada.id(),
                ScimVersionPrecondition.ofIfMatch(List.of("\"" + (before.version() + 1) + "\"")),
                new ScimUserReplacement(minimal("ada", false), "x-password-9", null)))
                .isInstanceOf(PreconditionFailedException.class);

        assertThat(stored()).isEqualTo(before);
        assertThat(writesSinceSetUp()).isZero();
        assertThat(audit.recorded()).isEmpty();
        assertThat(revocations).isEmpty();
    }

    @Test
    void the_bootstrap_admin_cannot_be_written_and_the_attempt_is_recorded() {
        ScimUser reserved = users.createReserved(
                ScimIdentities.user("root"), ReservedResourceName.BOOTSTRAP_ADMIN);
        ScimVersionPrecondition itsVersion = ScimVersionPrecondition.ofIfMatch(
                List.of("\"" + reserved.version() + "\""));

        assertThatThrownBy(() -> service.patch(CONNECTOR, reserved.id(), itsVersion,
                List.of(new SetActive(false))))
                .isInstanceOf(ProtectedResourceException.class);

        assertThat(users.findById(reserved.id()).orElseThrow()).isEqualTo(reserved);
        assertThat(audit.of(AuditOperation.SCIM_USER_REPLACE)).singleElement()
                .satisfies(event -> {
                    assertThat(event.subjectId()).isEqualTo(reserved.id());
                    assertThat(event.detail()).isEqualTo("MUTABILITY");
                });
    }

    @Test
    void a_taken_user_name_is_refused_as_a_duplicate_and_recorded() {
        service.create(CONNECTOR, new NewScimUser(minimal("grace", true), null, null));
        audit.reset();

        assertThatThrownBy(() -> patch(new SetText(TextAttribute.USER_NAME, "GRACE")))
                .isInstanceOf(DuplicateUserNameException.class);
        assertThat(audit.of(AuditOperation.SCIM_USER_REPLACE)).singleElement()
                .extracting(RecordingAuditTrail.Recorded::detail).isEqualTo("UNIQUENESS");
        assertThat(revocations).isEmpty();
    }

    // ---- partial update -------------------------------------------------------------------

    @Test
    void a_no_op_patch_leaves_the_version_and_last_modified_unchanged() {
        ScimUser before = stored();

        ScimUserResource written = patch(
                new SetActive(true), new SetText(TextAttribute.DISPLAY_NAME, "Ada"));

        assertThat(written.version()).isEqualTo(before.version());
        assertThat(written.lastModifiedAt()).isEqualTo(before.lastModifiedAt());
        assertThat(writesSinceSetUp()).isZero();
    }

    @Test
    void removing_the_user_name_is_refused_changes_nothing_and_is_recorded_as_mutability() {
        ScimUser before = stored();

        assertThatThrownBy(() -> patch(new RemoveText(TextAttribute.USER_NAME)))
                .isInstanceOf(ScimPatchRefusedException.class);

        assertThat(stored()).isEqualTo(before);
        assertThat(audit.of(AuditOperation.SCIM_USER_REPLACE)).singleElement()
                .extracting(RecordingAuditTrail.Recorded::detail).isEqualTo("MUTABILITY");
    }

    /**
     * The first operation would have deactivated the User; the second fails. Re-reading shows the
     * User exactly as it was — the first was never written, and no session was ended for it.
     */
    @Test
    void a_failing_operation_anywhere_in_the_sequence_leaves_the_user_as_it_was() {
        ScimUser before = stored();
        ScimEmailFilter pager = new ScimEmailFilter(
                List.of(new ScimEmailFilter.Condition(ScimEmailPart.TYPE, "pager")));

        assertThatThrownBy(() -> patch(new SetActive(false), new RemoveEmails(pager)))
                .isInstanceOfSatisfying(ScimPatchRefusedException.class, refused ->
                        assertThat(refused.reason())
                                .isEqualTo(ScimPatchRefusedException.Reason.NO_TARGET));

        assertThat(stored()).isEqualTo(before);
        assertThat(writesSinceSetUp()).isZero();
        assertThat(revocations).isEmpty();
        assertThat(audit.of(AuditOperation.SCIM_USER_REPLACE)).singleElement()
                .extracting(RecordingAuditTrail.Recorded::detail).isEqualTo("NO_TARGET");
    }

    // ---- password history -----------------------------------------------------------------

    @Test
    void the_current_password_is_refused_on_both_paths() {
        assertThatThrownBy(() -> patch(new SetPassword("first-password-1")))
                .isInstanceOf(PasswordReusedException.class);
        assertThatThrownBy(() -> put(stored().profile(), "first-password-1", "ext-ada"))
                .isInstanceOf(PasswordReusedException.class);

        assertThat(audit.of(AuditOperation.SCIM_USER_REPLACE))
                .extracting(RecordingAuditTrail.Recorded::detail)
                .containsExactly("INVALID_VALUE", "INVALID_VALUE");
        assertThat(revocations).isEmpty();
        assertThat(history.findRecentHashes(ada.id())).hasSize(1);
    }

    /**
     * Three are remembered, the current one included: after two changes the first is still one of
     * them, after a third it has aged out and may be set again.
     */
    @Test
    void any_of_the_last_three_is_refused_and_the_fourth_most_recent_is_accepted() {
        patch(new SetPassword("second-password-2"));
        patch(new SetPassword("third-password-3"));

        assertThatThrownBy(() -> patch(new SetPassword("first-password-1")))
                .isInstanceOf(PasswordReusedException.class);
        assertThatThrownBy(() -> patch(new SetPassword("second-password-2")))
                .isInstanceOf(PasswordReusedException.class);

        patch(new SetPassword("fourth-password-4"));
        assertThat(history.findRecentHashes(ada.id())).hasSize(3);

        patch(new SetPassword("first-password-1"));
        assertThat(stored().login().passwordHash()).endsWith(":first-password-1");
    }

    /**
     * The seeded Bootstrap Admin and any User whose credential predates the history have no
     * history row for their current password; the current hash is checked regardless.
     */
    @Test
    void the_current_credential_is_refused_even_when_the_history_does_not_hold_it() {
        ScimUser legacy = users.create(ScimUser.created(UUID.randomUUID(),
                minimal("legacy", true), encoder.encode("legacy-password-1"), ScimIdentities.NOW));

        assertThatThrownBy(() -> service.patch(CONNECTOR, legacy.id(),
                ScimVersionPrecondition.ofIfMatch(List.of("\"" + legacy.version() + "\"")),
                List.of(new SetPassword("legacy-password-1"))))
                .isInstanceOf(PasswordReusedException.class);
    }

    @Test
    void a_new_password_is_hashed_stored_remembered_and_never_kept_in_plaintext() {
        patch(new SetPassword("second-password-2"));

        String hash = stored().login().passwordHash();
        assertThat(hash).startsWith("{fake}").endsWith(":second-password-2");
        assertThat(history.findRecentHashes(ada.id()).get(0)).isEqualTo(hash);
        assertThat(encoder.matches).as("the history was consulted").isPositive();
    }

    // ---- which changes end sessions -------------------------------------------------------

    @Test
    void deactivation_ends_the_users_sessions_and_reactivation_does_not() {
        patch(new SetActive(false));
        assertThat(revocations).containsExactly(
                new Revocation(CONNECTOR.connectorId(), ada.id(), Set.of(Cause.DEACTIVATED)));

        revocations.clear();
        patch(new SetActive(true));
        assertThat(revocations).isEmpty();
    }

    @Test
    void a_password_change_or_removal_ends_the_users_sessions() {
        patch(new SetPassword("second-password-2"));
        patch(new RemovePassword());

        assertThat(revocations).extracting(Revocation::causes).containsExactly(
                Set.of(Cause.PASSWORD_CHANGED), Set.of(Cause.PASSWORD_CHANGED));
        assertThat(stored().login().hasPassword()).isFalse();
    }

    /** Removing a password that is not there changes nothing, so it ends nothing. */
    @Test
    void removing_an_absent_password_is_a_no_op() {
        patch(new RemovePassword());
        revocations.clear();
        long version = stored().version();

        patch(new RemovePassword());

        assertThat(revocations).isEmpty();
        assertThat(stored().version()).isEqualTo(version);
    }

    @Test
    void a_user_name_change_ends_the_users_sessions() {
        patch(new SetText(TextAttribute.USER_NAME, "ada.lovelace"));

        assertThat(revocations).extracting(Revocation::causes)
                .containsExactly(Set.of(Cause.USER_NAME_CHANGED));
    }

    @Test
    void one_write_that_changes_several_security_attributes_ends_the_sessions_once() {
        put(minimal("ada.lovelace", false), "second-password-2", "ext-ada");

        assertThat(revocations).singleElement()
                .extracting(Revocation::causes)
                .isEqualTo(Set.of(Cause.DEACTIVATED, Cause.PASSWORD_CHANGED,
                        Cause.USER_NAME_CHANGED));
    }

    @Test
    void an_ordinary_profile_or_email_change_ends_no_session() {
        patch(new SetText(TextAttribute.DISPLAY_NAME, "Countess"),
                new RemoveEmails(ScimEmailFilter.ALL),
                new SetText(TextAttribute.LOCALE, "fr-FR"));

        assertThat(revocations).isEmpty();
    }

    // ---- deletion -------------------------------------------------------------------------

    @Test
    void a_deletion_removes_the_user_leaves_a_tombstone_records_it_and_ends_the_sessions() {
        assertThat(service.delete(CONNECTOR, ada.id(), current())).isTrue();

        assertThat(users.findById(ada.id())).isEmpty();
        assertThat(service.findById(CONNECTOR, ada.id())).isEmpty();
        assertThat(tombstones.recorded()).containsExactly(
                new InMemoryScimTombstoneRepository.Tombstone(
                        com.example.backend.scim.domain.ScimResourceType.USER, ada.id(), LATER));
        assertThat(audit.recorded()).containsExactly(new RecordingAuditTrail.Recorded(
                AuditOperation.SCIM_USER_DELETE, CONNECTOR.connectorId(), ada.id(), null));
        assertThat(revocations).containsExactly(
                new Revocation(CONNECTOR.connectorId(), ada.id(), Set.of(Cause.DELETED)));
    }

    @Test
    void deleting_an_id_that_names_no_user_reports_absence_and_touches_nothing() {
        assertThat(service.delete(CONNECTOR, UUID.randomUUID(), current())).isFalse();

        assertThat(writesSinceSetUp()).isZero();
        assertThat(tombstones.recorded()).isEmpty();
        assertThat(audit.recorded()).isEmpty();
        assertThat(revocations).isEmpty();
    }

    @Test
    void a_deletion_with_a_stale_precondition_is_refused_and_changes_nothing() {
        ScimVersionPrecondition stale = ScimVersionPrecondition.ofIfMatch(
                List.of("\"" + (stored().version() + 1) + "\""));

        assertThatThrownBy(() -> service.delete(CONNECTOR, ada.id(), stale))
                .isInstanceOf(com.example.backend.scim.domain.PreconditionFailedException.class);

        assertThat(users.findById(ada.id())).isPresent();
        assertThat(writesSinceSetUp()).isZero();
        assertThat(tombstones.recorded()).isEmpty();
        assertThat(audit.recorded()).isEmpty();
        assertThat(revocations).isEmpty();
    }

    @Test
    void the_bootstrap_admin_cannot_be_deleted_and_the_attempt_is_recorded() {
        ScimUser reserved = users.createReserved(
                ScimIdentities.user("root"), ReservedResourceName.BOOTSTRAP_ADMIN);
        ScimVersionPrecondition itsVersion = ScimVersionPrecondition.ofIfMatch(
                List.of("\"" + reserved.version() + "\""));

        assertThatThrownBy(() -> service.delete(CONNECTOR, reserved.id(), itsVersion))
                .isInstanceOf(ProtectedResourceException.class);

        assertThat(users.findById(reserved.id()).orElseThrow()).isEqualTo(reserved);
        assertThat(tombstones.recorded()).isEmpty();
        assertThat(revocations).isEmpty();
        assertThat(audit.recorded()).containsExactly(new RecordingAuditTrail.Recorded(
                AuditOperation.SCIM_USER_DELETE, CONNECTOR.connectorId(), reserved.id(),
                "MUTABILITY"));
    }

    @Test
    void a_deleted_users_former_user_name_is_free_for_the_next_create() {
        service.delete(CONNECTOR, ada.id(), current());

        ScimUserResource again = service.create(CONNECTOR, new NewScimUser(
                minimal("ADA", true), null, "ext-ada"));

        assertThat(again.id()).isNotEqualTo(ada.id());
    }
}
