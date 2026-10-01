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
import com.example.backend.scim.domain.PasswordPolicy;
import com.example.backend.scim.domain.PasswordPolicyRefusedException;
import com.example.backend.scim.domain.PasswordReusedException;
import com.example.backend.scim.domain.PreconditionFailedException;
import com.example.backend.scim.domain.PreconditionRequiredException;
import com.example.backend.scim.domain.ProtectedResourceException;
import com.example.backend.scim.domain.ReservedResourceName;
import com.example.backend.scim.domain.ScimEmail;
import com.example.backend.scim.domain.ScimEmailFilter;
import com.example.backend.scim.domain.ScimEmailPart;
import com.example.backend.scim.domain.ScimLoginState;
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
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

    /** A PUT; the third argument is the externalId the body carried, null when it omitted it. */
    private ScimUserResource put(ScimUserProfile profile, String password, String externalId) {
        return service.replace(CONNECTOR, ada.id(), current(),
                new ScimUserReplacement(profile, password, externalId)).orElseThrow();
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

    // ---- a connector-set password requires a change ----------------------------------------

    /**
     * A credential chosen and transported by a connector is known outside the User, so it must be
     * replaced before it is used for anything else: a create carrying one flags the User as of
     * the create.
     */
    @Test
    void a_create_with_a_password_requires_a_change() {
        assertThat(stored().login().passwordChangeRequiredSince()).isEqualTo(LATER);
        assertThat(stored().createdAt()).as("flagging keeps the creation stamps").isEqualTo(LATER);
        assertThat(stored().lastModifiedAt()).isEqualTo(LATER);
        assertThat(ada.createdAt()).isEqualTo(LATER);
    }

    @Test
    void a_write_that_sets_no_password_does_not_flag_an_unflagged_user() {
        ScimUserResource grace = service.create(
                CONNECTOR, new NewScimUser(minimal("grace", true), null, null));
        UUID id = grace.id();

        service.replace(CONNECTOR, id, versionOf(id),
                new ScimUserReplacement(minimal("grace", false), null, null));

        assertThat(users.findById(id).orElseThrow().login().isPasswordChangeRequired()).isFalse();
    }

    @Test
    void a_create_without_a_password_requires_no_change() {
        ScimUserResource grace = service.create(
                CONNECTOR, new NewScimUser(minimal("grace", true), null, null));

        assertThat(users.findById(grace.id()).orElseThrow().login().isPasswordChangeRequired())
                .isFalse();
    }

    @Test
    void a_put_or_patch_setting_a_password_requires_a_change() {
        ScimUserResource grace = service.create(
                CONNECTOR, new NewScimUser(minimal("grace", true), null, null));
        UUID id = grace.id();
        service.replace(CONNECTOR, id, versionOf(id),
                new ScimUserReplacement(minimal("grace", true), "a-put-password", null));
        assertThat(users.findById(id).orElseThrow().login().isPasswordChangeRequired())
                .as("PUT with a password").isTrue();

        ScimUser hopper =
                users.given(ScimIdentities.userWithLoginState("hopper", ScimLoginState.of("hash")));
        service.patch(CONNECTOR, hopper.id(), versionOf(hopper.id()),
                List.of(new SetPassword("a-patch-password")));
        assertThat(users.require("hopper").login().passwordChangeRequiredSince())
                .as("PATCH with a password").isEqualTo(LATER);
    }

    /** Neither omitting the password, removing it, nor any other attribute clears the flag. */
    @Test
    void no_connector_write_clears_the_requirement() {
        put(minimal("ada", true), null, "ext-ada");
        assertThat(stored().login().isPasswordChangeRequired()).as("PUT omitting it").isTrue();
        patch(new SetText(TextAttribute.DISPLAY_NAME, "Countess"));
        assertThat(stored().login().isPasswordChangeRequired()).as("a profile change").isTrue();
        patch(new RemovePassword());
        assertThat(stored().login().isPasswordChangeRequired())
                .as("removing the password").isTrue();
    }

    private ScimVersionPrecondition versionOf(UUID id) {
        return ScimVersionPrecondition.ofIfMatch(
                List.of("\"" + users.findById(id).orElseThrow().version() + "\""));
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
     * {@code externalId} is read-write: a PUT carrying a different value re-keys the CALLING
     * connector's alias, advances the version and stamps {@code lastModified} like any other
     * change, and leaves another connector's alias for the same User as it was.
     */
    @Test
    void a_put_with_a_different_alias_changes_only_the_callers_alias() {
        aliases.put(OTHER_CONNECTOR.connectorId(), ada.id(), "theirs");
        long before = stored().version();

        ScimUserResource written = put(stored().profile(), null, "ext-ada-2");

        assertThat(written.externalId()).isEqualTo("ext-ada-2");
        assertThat(written.version()).isEqualTo(before + 1);
        assertThat(stored().lastModifiedAt()).isEqualTo(LATER);
        assertThat(aliases.find(CONNECTOR.connectorId(), ada.id())).contains("ext-ada-2");
        assertThat(aliases.find(OTHER_CONNECTOR.connectorId(), ada.id())).contains("theirs");
        assertThat(audit.of(AuditOperation.SCIM_USER_REPLACE)).singleElement()
                .extracting(RecordingAuditTrail.Recorded::detail).isEqualTo("EXTERNAL_ID");
    }

    /**
     * RFC 7644 §3.5.1: an omitted read-write attribute is unassigned by a replacement, so a PUT
     * with no {@code externalId} removes the caller's alias — and only the caller's.
     */
    @Test
    void a_put_omitting_the_alias_removes_only_the_callers_alias() {
        aliases.put(OTHER_CONNECTOR.connectorId(), ada.id(), "theirs");
        long before = stored().version();

        ScimUserResource written = put(stored().profile(), null, null);

        assertThat(written.externalId()).isNull();
        assertThat(written.version()).isEqualTo(before + 1);
        assertThat(aliases.find(CONNECTOR.connectorId(), ada.id())).isEmpty();
        assertThat(aliases.find(OTHER_CONNECTOR.connectorId(), ada.id())).contains("theirs");
        assertThat(audit.of(AuditOperation.SCIM_USER_REPLACE)).singleElement()
                .extracting(RecordingAuditTrail.Recorded::detail).isEqualTo("EXTERNAL_ID");
    }

    /** Restating the stored alias is the no-op any restated attribute is. */
    @Test
    void a_put_restating_the_alias_changes_nothing() {
        long before = stored().version();

        assertThat(put(stored().profile(), null, "ext-ada").externalId()).isEqualTo("ext-ada");

        assertThat(stored().version()).isEqualTo(before);
        assertThat(writesSinceSetUp()).isZero();
        assertThat(aliases.find(CONNECTOR.connectorId(), ada.id())).contains("ext-ada");
    }

    /** PATCH sets, re-keys and removes the caller's alias, and only the caller's. */
    @Test
    void a_patch_sets_and_removes_only_the_callers_alias() {
        aliases.put(OTHER_CONNECTOR.connectorId(), ada.id(), "theirs");
        long before = stored().version();

        ScimUserResource set = patch(new ScimUserPatchOperation.SetExternalId("ext-ada-2"));

        assertThat(set.externalId()).isEqualTo("ext-ada-2");
        assertThat(set.version()).isEqualTo(before + 1);
        assertThat(aliases.find(CONNECTOR.connectorId(), ada.id())).contains("ext-ada-2");

        ScimUserResource removed = patch(new ScimUserPatchOperation.RemoveExternalId());

        assertThat(removed.externalId()).isNull();
        assertThat(removed.version()).isEqualTo(before + 2);
        assertThat(aliases.find(CONNECTOR.connectorId(), ada.id())).isEmpty();
        assertThat(aliases.find(OTHER_CONNECTOR.connectorId(), ada.id())).contains("theirs");
        assertThat(audit.of(AuditOperation.SCIM_USER_REPLACE))
                .extracting(RecordingAuditTrail.Recorded::detail)
                .containsExactly("EXTERNAL_ID", "EXTERNAL_ID");
    }

    /** A write that moves no alias writes no alias — not even the stored value again. */
    @Test
    void a_write_that_leaves_the_alias_alone_does_not_write_it() {
        int before = aliases.writes();

        patch(new SetText(TextAttribute.DISPLAY_NAME, "Countess"));
        put(minimal("ada", false), null, "ext-ada");

        assertThat(aliases.writes()).isEqualTo(before);
        assertThat(aliases.find(CONNECTOR.connectorId(), ada.id())).contains("ext-ada");
    }

    /**
     * The other connector's write reaches its own namespace: a connector with no alias for Ada
     * that sets one leaves the creating connector's alias unchanged.
     */
    @Test
    void another_connector_setting_its_alias_does_not_touch_the_creators() {
        ScimUserResource seenByOther = service.patch(OTHER_CONNECTOR, ada.id(), current(),
                List.of(new ScimUserPatchOperation.SetExternalId("theirs"))).orElseThrow();

        assertThat(seenByOther.externalId()).isEqualTo("theirs");
        assertThat(aliases.find(CONNECTOR.connectorId(), ada.id())).contains("ext-ada");
        assertThat(service.findById(CONNECTOR, ada.id()).orElseThrow().externalId())
                .isEqualTo("ext-ada");
    }

    /**
     * The Bootstrap Admin keeps its protection: an alias write is a write to that User — it
     * advances its version — so it is refused like any other, and no alias is stored.
     */
    @Test
    void the_bootstrap_admin_alias_cannot_be_written() {
        ScimUser reserved = users.createReserved(
                ScimIdentities.user("root"), ReservedResourceName.BOOTSTRAP_ADMIN);
        ScimVersionPrecondition itsVersion = ScimVersionPrecondition.ofIfMatch(
                List.of("\"" + reserved.version() + "\""));

        assertThatThrownBy(() -> service.patch(CONNECTOR, reserved.id(), itsVersion,
                List.of(new ScimUserPatchOperation.SetExternalId("root-alias"))))
                .isInstanceOf(ProtectedResourceException.class);

        assertThat(aliases.find(CONNECTOR.connectorId(), reserved.id())).isEmpty();
        assertThat(users.findById(reserved.id()).orElseThrow()).isEqualTo(reserved);
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
                minimal("legacy", true), encoder.encode("pre-history-pass-1"), ScimIdentities.NOW));

        assertThatThrownBy(() -> service.patch(CONNECTOR, legacy.id(),
                ScimVersionPrecondition.ofIfMatch(List.of("\"" + legacy.version() + "\"")),
                List.of(new SetPassword("pre-history-pass-1"))))
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

    // ---- password policy ------------------------------------------------------------------

    /**
     * Sub-policy values, each with the rule it breaks. The userName rule's value contains both the
     * fixture's {@code ada} and {@code ada-lovelace}, the userName the create tests provision.
     */
    static Stream<Arguments> subPolicyPasswords() {
        return Stream.of(
                Arguments.of("short-pw-1", PasswordPolicy.Rule.TOO_SHORT),
                Arguments.of("x".repeat(PasswordPolicy.MAX_LENGTH + 1), PasswordPolicy.Rule.TOO_LONG),
                Arguments.of("i-am-ADA-LOVELACE-really", PasswordPolicy.Rule.CONTAINS_USER_NAME));
    }

    private static void assertRefusedFor(
            ThrowingCallable write, String candidate, PasswordPolicy.Rule rule) {
        assertThatThrownBy(write).isInstanceOfSatisfying(
                PasswordPolicyRefusedException.class, refused -> {
                    assertThat(refused.rule()).isEqualTo(rule);
                    assertThat(refused.getMessage()).isEqualTo(rule.message())
                            .doesNotContain(candidate);
                });
    }

    @ParameterizedTest
    @MethodSource("subPolicyPasswords")
    void a_create_with_a_sub_policy_password_is_refused_writes_nothing_and_is_recorded(
            String candidate, PasswordPolicy.Rule rule) {
        assertRefusedFor(() -> service.create(CONNECTOR, new NewScimUser(
                minimal("ada-lovelace", true), candidate, "ext-new")), candidate, rule);

        assertThat(writesSinceSetUp()).isZero();
        assertThat(audit.of(AuditOperation.SCIM_USER_CREATE)).singleElement()
                .satisfies(event -> {
                    assertThat(event.detail()).isEqualTo("INVALID_VALUE");
                    assertThat(event.subjectId()).isNull();
                });
    }

    @ParameterizedTest
    @MethodSource("subPolicyPasswords")
    void a_put_with_a_sub_policy_password_is_refused_and_changes_nothing(
            String candidate, PasswordPolicy.Rule rule) {
        ScimUser before = stored();

        assertRefusedFor(() -> put(before.profile(), candidate, "ext-ada"), candidate, rule);

        assertWriteRefusedAndNothingChanged(before);
    }

    @ParameterizedTest
    @MethodSource("subPolicyPasswords")
    void a_patch_with_a_sub_policy_password_is_refused_and_changes_nothing(
            String candidate, PasswordPolicy.Rule rule) {
        ScimUser before = stored();

        assertRefusedFor(() -> patch(new SetPassword(candidate)), candidate, rule);

        assertWriteRefusedAndNothingChanged(before);
    }

    private void assertWriteRefusedAndNothingChanged(ScimUser before) {
        assertThat(stored()).isEqualTo(before);
        assertThat(writesSinceSetUp()).isZero();
        assertThat(history.findRecentHashes(ada.id())).hasSize(1);
        assertThat(revocations).isEmpty();
        assertThat(encoder.matches).as("a sub-policy value is never compared to the history")
                .isZero();
        assertThat(audit.of(AuditOperation.SCIM_USER_REPLACE)).singleElement()
                .satisfies(event -> {
                    assertThat(event.detail()).isEqualTo("INVALID_VALUE");
                    assertThat(event.subjectId()).as("the refusal names the User it targeted")
                            .isEqualTo(ada.id());
                });
    }

    /**
     * The policy is checked against the userName the write leaves the User with, so a PUT renaming
     * the User cannot carry a password containing its new name.
     */
    @Test
    void a_write_renaming_the_user_checks_the_password_against_the_new_user_name() {
        String candidate = "grace-hopper-passphrase";

        assertRefusedFor(() -> put(minimal("grace", true), candidate, "ext-ada"),
                candidate, PasswordPolicy.Rule.CONTAINS_USER_NAME);
        assertRefusedFor(() -> patch(new SetText(TextAttribute.USER_NAME, "grace"),
                        new SetPassword(candidate)),
                candidate, PasswordPolicy.Rule.CONTAINS_USER_NAME);
        assertThat(stored().profile().userName()).isEqualTo("ada");
    }

    /**
     * The order of {@code /change-password}: the intrinsic rules, then reuse. A value that is both
     * too short and the current credential is refused for its length, as the self-service change
     * refuses it, and the history is never consulted for it.
     */
    @Test
    void a_value_both_sub_policy_and_reused_is_refused_by_the_policy_first_on_both_paths() {
        ScimUser legacy = users.create(ScimUser.created(UUID.randomUUID(),
                minimal("legacy", true), encoder.encode("old-pw"), ScimIdentities.NOW));

        assertRefusedFor(() -> service.patch(CONNECTOR, legacy.id(), versionOf(legacy.id()),
                        List.of(new SetPassword("old-pw"))),
                "old-pw", PasswordPolicy.Rule.TOO_SHORT);
        assertRefusedFor(() -> service.replace(CONNECTOR, legacy.id(), versionOf(legacy.id()),
                        new ScimUserReplacement(legacy.profile(), "old-pw", null)),
                "old-pw", PasswordPolicy.Rule.TOO_SHORT);
        assertThat(encoder.matches).isZero();
    }

    @Test
    void a_valid_password_passes_the_policy_and_is_still_checked_for_reuse() {
        assertThatThrownBy(() -> patch(new SetPassword("first-password-1")))
                .isInstanceOf(PasswordReusedException.class);
        assertThat(encoder.matches).isPositive();
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
