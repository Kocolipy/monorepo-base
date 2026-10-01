package com.example.backend.scim.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.audit.RecordingAuditTrail;
import com.example.backend.audit.domain.AuditGroupAttribute;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.audit.domain.AuditScimRefusal;
import com.example.backend.scim.InMemoryScimExternalIdRepository;
import com.example.backend.scim.InMemoryScimGroupRepository;
import com.example.backend.scim.InMemoryScimQueryRepository;
import com.example.backend.scim.InMemoryScimTombstoneRepository;
import com.example.backend.scim.InMemoryScimUserRepository;
import com.example.backend.scim.ScimIdentities;
import com.example.backend.scim.domain.AuthenticatedConnector;
import com.example.backend.scim.domain.ConnectorTokenScope;
import com.example.backend.scim.domain.DuplicateDisplayNameException;
import com.example.backend.scim.domain.ProtectedResourceException;
import com.example.backend.scim.domain.ReservedResourceName;
import com.example.backend.scim.domain.ScimGroup;
import com.example.backend.scim.domain.ScimGroupMember;
import com.example.backend.scim.domain.ScimPageRequest;
import com.example.backend.scim.domain.ScimQuery;
import com.example.backend.scim.domain.ScimResourceType;
import com.example.backend.scim.domain.ScimUser;
import com.example.backend.scim.domain.ScimVersionPrecondition;
import com.example.backend.scim.domain.UnknownGroupMemberException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The Group write and read use cases, and the refusals that protect the deployment's recovery path.
 *
 * <p>These cover the ticket's first, third, fourth and seventh acceptance criteria: direct-User-only
 * membership with a Group, a deleted User or an unknown id refused; a PATCH that fails partway
 * applying nothing; version increments on the Group and on every affected User; the Admin group and
 * the Bootstrap Admin refusing every write that would change them, verified by re-reading unchanged;
 * and an audit event for each write and each refusal.
 */
class ScimGroupServiceTests {

    private static final AuthenticatedConnector CONNECTOR = new AuthenticatedConnector(
            UUID.randomUUID(), UUID.randomUUID(), ConnectorTokenScope.READ_WRITE);

    private final InMemoryScimUserRepository users = new InMemoryScimUserRepository();

    private final InMemoryScimGroupRepository groups = new InMemoryScimGroupRepository(users);

    private final InMemoryScimExternalIdRepository aliases =
            new InMemoryScimExternalIdRepository();

    private final RecordingAuditTrail audit = new RecordingAuditTrail();

    private final InMemoryScimTombstoneRepository tombstones =
            new InMemoryScimTombstoneRepository();

    private final ScimGroupService service = new ScimGroupService(
            groups,
            users,
            aliases,
            tombstones,
            audit,
            Clock.fixed(ScimIdentities.NOW, ZoneOffset.UTC),
            new InMemoryScimQueryRepository(users, groups));

    private ScimUser alice;

    private ScimUser bob;

    @BeforeEach
    void storeUsers() {
        alice = users.create(ScimIdentities.user("alice"));
        bob = users.create(ScimIdentities.user("bob"));
    }

    /**
     * The precondition a well-behaved connector sends: the Group's current version, or any
     * well-formed tag when there is no Group to read one from. These tests are about what a
     * write does, not about preconditions — those are pinned in their own tests.
     */
    private ScimVersionPrecondition current(UUID id) {
        long version = groups.findById(id).map(ScimGroup::version).orElse(ScimUser.INITIAL_VERSION);
        return ScimVersionPrecondition.ofIfMatch(List.of("\"" + version + "\""));
    }

    // ---- create -------------------------------------------------------------------------------

    @Test
    void a_created_group_starts_at_version_one_and_carries_its_members_labels() {
        ScimGroupResource created = service.create(
                CONNECTOR, new NewScimGroup("Engineering", List.of(alice.id()), null));

        assertThat(created.displayName()).isEqualTo("Engineering");
        assertThat(created.version()).isEqualTo(ScimUser.INITIAL_VERSION);
        assertThat(created.members()).extracting(ScimGroupMember::userId).containsExactly(alice.id());
        assertThat(created.members()).extracting(ScimGroupMember::display).containsExactly("alice");
    }

    @Test
    void an_empty_group_is_legitimate() {
        assertThat(service.create(CONNECTOR, new NewScimGroup("Empty", List.of(), null)).members())
                .isEmpty();
    }

    @Test
    void a_created_group_records_one_create_event_naming_it() {
        ScimGroupResource created = service.create(
                CONNECTOR, new NewScimGroup("Engineering", List.of(), null));

        assertThat(audit.of(AuditOperation.SCIM_GROUP_CREATE)).singleElement()
                .satisfies(event -> {
                    assertThat(event.actorId()).isEqualTo(CONNECTOR.connectorId());
                    assertThat(event.subjectId()).isEqualTo(created.id());
                    assertThat(event.detail()).isNull();
                });
    }

    @Test
    void the_connectors_alias_is_recorded_with_the_group_and_rendered_back() {
        ScimGroupResource created = service.create(
                CONNECTOR, new NewScimGroup("Engineering", List.of(), "eng-1"));

        assertThat(created.externalId()).isEqualTo("eng-1");
        assertThat(aliases.find(CONNECTOR.connectorId(), created.id())).contains("eng-1");
    }

    @Test
    void a_display_name_a_live_group_already_holds_is_refused_and_audited_as_a_uniqueness_failure() {
        service.create(CONNECTOR, new NewScimGroup("Engineering", List.of(), null));

        assertThatThrownBy(() -> service.create(
                        CONNECTOR, new NewScimGroup("engineering", List.of(), null)))
                .isInstanceOf(DuplicateDisplayNameException.class);
        assertThat(refusalDetails(AuditOperation.SCIM_GROUP_CREATE))
                .containsExactly(AuditScimRefusal.UNIQUENESS.name());
    }

    /** AC 1: a Group's id as a member is rejected as invalid — the membership names Users only. */
    @Test
    void a_group_id_as_a_member_is_refused() {
        ScimGroupResource engineering = service.create(
                CONNECTOR, new NewScimGroup("Engineering", List.of(), null));

        assertThatThrownBy(() -> service.create(
                        CONNECTOR, new NewScimGroup("Nested", List.of(engineering.id()), null)))
                .isInstanceOf(UnknownGroupMemberException.class);
    }

    @Test
    void an_unknown_id_as_a_member_is_refused_and_audited_as_an_invalid_value() {
        assertThatThrownBy(() -> service.create(
                        CONNECTOR, new NewScimGroup("Ghosts", List.of(UUID.randomUUID()), null)))
                .isInstanceOf(UnknownGroupMemberException.class);
        assertThat(refusalDetails(AuditOperation.SCIM_GROUP_CREATE))
                .containsExactly(AuditScimRefusal.INVALID_VALUE.name());
    }

    // ---- read ---------------------------------------------------------------------------------

    /** Deliberately unaudited: a single read is ordinary traffic and would bury the bulk reads. */
    @Test
    void retrieving_one_group_records_nothing() {
        ScimGroupResource created = service.create(
                CONNECTOR, new NewScimGroup("Engineering", List.of(), null));
        audit.reset();

        assertThat(service.findById(CONNECTOR, created.id())).isPresent();
        assertThat(audit.recorded()).isEmpty();
    }

    @Test
    void a_collection_read_is_audited_even_when_it_returns_nothing() {
        ScimGroupListing listing = service.query(CONNECTOR, groupQuery(1, 0), BASE_URI);

        assertThat(listing.resources()).isEmpty();
        assertThat(listing.totalResults()).isZero();
        assertThat(audit.of(AuditOperation.SCIM_GROUP_LIST))
                .singleElement()
                .extracting(RecordingAuditTrail.Recorded::detail)
                .isEqualTo("0");
    }

    @Test
    void a_zero_count_page_still_reports_the_total() {
        service.create(CONNECTOR, new NewScimGroup("Engineering", List.of(), null));
        service.create(CONNECTOR, new NewScimGroup("Support", List.of(), null));

        ScimGroupListing listing = service.query(CONNECTOR, groupQuery(1, 0), BASE_URI);

        assertThat(listing.resources()).isEmpty();
        assertThat(listing.totalResults()).isEqualTo(2);
    }

    /** The recorded count is what the response carried, not the total that matched. */
    @Test
    void a_collection_read_records_how_many_groups_the_page_returned() {
        service.create(CONNECTOR, new NewScimGroup("Engineering", List.of(), null));
        service.create(CONNECTOR, new NewScimGroup("Support", List.of(), null));
        audit.reset();

        ScimGroupListing listing = service.query(CONNECTOR, groupQuery(2, 5), BASE_URI);

        assertThat(listing.resources()).extracting(ScimGroupResource::displayName)
                .containsExactly("Support");
        assertThat(listing.totalResults()).isEqualTo(2);
        assertThat(audit.of(AuditOperation.SCIM_GROUP_LIST))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.actorId()).isEqualTo(CONNECTOR.connectorId());
                    assertThat(event.detail()).isEqualTo("1");
                });
    }

    private static final String BASE_URI = "https://scim.example/scim/v2";

    private static ScimQuery groupQuery(int startIndex, int count) {
        return new ScimQuery(
                Set.of(ScimResourceType.GROUP), null, null, new ScimPageRequest(startIndex, count));
    }

    // ---- replace and patch --------------------------------------------------------------------

    /** AC 3: a membership change advances the Group's version and the affected Users' versions. */
    @Test
    void adding_a_member_advances_the_groups_version_and_that_members_version() {
        ScimGroupResource created = service.create(
                CONNECTOR, new NewScimGroup("Engineering", List.of(), null));
        long aliceBefore = users.require("alice").version();
        long bobBefore = users.require("bob").version();

        ScimGroupResource replaced = service.replace(
                CONNECTOR,
                created.id(), current(created.id()),
                new ScimGroupReplacement("Engineering", List.of(alice.id()), null)).orElseThrow();

        assertThat(replaced.version()).isEqualTo(created.version() + 1);
        assertThat(users.require("alice").version()).isEqualTo(aliceBefore + 1);
        assertThat(users.require("bob").version())
                .as("a User who was never a member is unaffected")
                .isEqualTo(bobBefore);
    }

    @Test
    void removing_a_member_advances_the_removed_users_version() {
        ScimGroupResource created = service.create(
                CONNECTOR, new NewScimGroup("Engineering", List.of(alice.id()), null));
        long aliceBefore = users.require("alice").version();

        service.replace(CONNECTOR, created.id(), current(created.id()), new ScimGroupReplacement("Engineering", List.of(), null));

        assertThat(users.require("alice").version()).isEqualTo(aliceBefore + 1);
    }

    /** AC 3: a display-name change advances every CURRENT member's version. */
    @Test
    void renaming_a_group_advances_every_current_members_version() {
        ScimGroupResource created = service.create(
                CONNECTOR,
                new NewScimGroup("Engineering", List.of(alice.id(), bob.id()), null));
        long aliceBefore = users.require("alice").version();
        long bobBefore = users.require("bob").version();

        service.replace(
                CONNECTOR,
                created.id(), current(created.id()),
                new ScimGroupReplacement("Platform", List.of(alice.id(), bob.id()), null));

        assertThat(users.require("alice").version()).isEqualTo(aliceBefore + 1);
        assertThat(users.require("bob").version()).isEqualTo(bobBefore + 1);
    }

    /**
     * A no-op write reports no changed attributes. Compared rather than inferred from the verb,
     * because a trail that recorded every PUT as changing both attributes could not be used to find
     * the writes that mattered.
     */
    @Test
    void a_replacement_that_changes_nothing_records_no_changed_attributes() {
        ScimGroupResource created = service.create(
                CONNECTOR, new NewScimGroup("Engineering", List.of(alice.id()), null));
        audit.reset();

        ScimGroupResource replayed = service.replace(
                CONNECTOR, created.id(), current(created.id()), new ScimGroupReplacement("Engineering", List.of(alice.id()), null))
                .orElseThrow();

        assertThat(audit.of(AuditOperation.SCIM_GROUP_REPLACE)).singleElement()
                .satisfies(event -> assertThat(event.detail()).isEmpty());
        // And the VERSION did not move either — the two halves of "this write changed nothing"
        // have to agree. They previously did not: the audit recorded no changed attribute while
        // the version advanced anyway, so a connector's idempotent re-send invalidated its own
        // cached copy. Asserting only the audit detail is what let that through.
        assertThat(replayed.version())
                .as("a write that changed nothing must not move the resource's validator")
                .isEqualTo(created.version());
        assertThat(users.require("alice").version())
                .as("nor any member's")
                .isEqualTo(alice.version() + 1);
    }

    @Test
    void a_write_that_moves_both_attributes_records_both() {
        ScimGroupResource created = service.create(
                CONNECTOR, new NewScimGroup("Engineering", List.of(), null));
        audit.reset();

        service.replace(
                CONNECTOR, created.id(), current(created.id()), new ScimGroupReplacement("Platform", List.of(alice.id()), null));

        // Split on the separator rather than compared to a joined literal: the previous assertion
        // pinned EnumSet iteration order and the joining format, so it would have failed on a
        // change to either while saying nothing more about the behaviour. What matters is that
        // BOTH attributes were recorded, in any order.
        assertThat(audit.of(AuditOperation.SCIM_GROUP_REPLACE)).singleElement()
                .satisfies(event -> assertThat(event.detail().split(","))
                        .containsExactlyInAnyOrder(
                                AuditGroupAttribute.DISPLAY_NAME.name(),
                                AuditGroupAttribute.MEMBERS.name()));
    }

    @Test
    void a_patch_adds_a_member_without_restating_the_membership() {
        ScimGroupResource created = service.create(
                CONNECTOR, new NewScimGroup("Engineering", List.of(alice.id()), null));

        ScimGroupResource patched = service.patch(
                CONNECTOR,
                created.id(), current(created.id()),
                List.of(new ScimGroupPatchOperation.AddMembers(List.of(bob.id())))).orElseThrow();

        assertThat(patched.members()).extracting(ScimGroupMember::userId)
                .containsExactlyInAnyOrder(alice.id(), bob.id());
    }

    @Test
    void a_patch_removing_a_member_who_is_not_one_is_not_an_error() {
        ScimGroupResource created = service.create(
                CONNECTOR, new NewScimGroup("Engineering", List.of(alice.id()), null));

        ScimGroupResource patched = service.patch(
                CONNECTOR,
                created.id(), current(created.id()),
                List.of(new ScimGroupPatchOperation.RemoveMembers(List.of(bob.id())))).orElseThrow();

        assertThat(patched.members()).extracting(ScimGroupMember::userId).containsExactly(alice.id());
    }

    @Test
    void a_patch_can_clear_the_whole_membership() {
        ScimGroupResource created = service.create(
                CONNECTOR, new NewScimGroup("Engineering", List.of(alice.id(), bob.id()), null));

        ScimGroupResource patched = service.patch(
                CONNECTOR,
                created.id(), current(created.id()),
                List.of(new ScimGroupPatchOperation.RemoveAllMembers())).orElseThrow();

        assertThat(patched.members()).isEmpty();
    }

    /**
     * AC 1: partial-failure PATCH rolls back entirely. The operations are folded into one desired
     * state and written once, so a sequence whose later operation is refused leaves the earlier ones
     * unapplied — there was never an intermediate state to roll back.
     */
    @Test
    void a_patch_whose_later_operation_is_refused_applies_none_of_the_earlier_ones() {
        ScimGroupResource created = service.create(
                CONNECTOR, new NewScimGroup("Engineering", List.of(), null));

        assertThatThrownBy(() -> service.patch(CONNECTOR, created.id(), current(created.id()), List.of(
                        new ScimGroupPatchOperation.SetDisplayName("Platform"),
                        new ScimGroupPatchOperation.AddMembers(List.of(alice.id())),
                        new ScimGroupPatchOperation.AddMembers(List.of(UUID.randomUUID())))))
                .isInstanceOf(UnknownGroupMemberException.class);

        ScimGroupResource reread = service.findById(CONNECTOR, created.id()).orElseThrow();
        assertThat(reread.displayName()).isEqualTo("Engineering");
        assertThat(reread.members()).isEmpty();
        assertThat(reread.version()).isEqualTo(created.version());
    }

    @Test
    void a_write_to_a_group_that_does_not_exist_reports_absence_rather_than_throwing() {
        assertThat(service.replace(
                        CONNECTOR,
                        UUID.randomUUID(), current(UUID.randomUUID()),
                        new ScimGroupReplacement("Ghost", List.of(), null))).isEmpty();
    }

    /**
     * A write naming a member that does not exist is audited as an invalid value, on the REPLACE
     * operation rather than the create one.
     *
     * <p>Separate from the create-time case above because they are separate call sites with
     * separate audit calls, and the write one was reachable by tests that asserted only the thrown
     * exception: mutation testing removed the audit call on this path and every test still passed.
     * A refused provisioning write that leaves no trace is the failure mode the audit trail exists
     * to prevent — an operator investigating why a connector's membership never converged would
     * find nothing recorded at all.
     */
    @Test
    void a_write_naming_an_unknown_member_is_audited_as_an_invalid_value_on_the_write() {
        ScimGroupResource created = service.create(
                CONNECTOR, new NewScimGroup("Engineering", List.of(), null));

        assertThatThrownBy(() -> service.replace(
                        CONNECTOR,
                        created.id(), current(created.id()),
                        new ScimGroupReplacement("Engineering", List.of(UUID.randomUUID()), null)))
                .isInstanceOf(UnknownGroupMemberException.class);

        assertThat(refusalDetails(AuditOperation.SCIM_GROUP_REPLACE))
                .containsExactly(AuditScimRefusal.INVALID_VALUE.name());
        assertThat(audit.of(AuditOperation.SCIM_GROUP_REPLACE))
                .singleElement()
                .satisfies(recorded -> assertThat(recorded.subjectId()).isEqualTo(created.id()));
    }

    /** The same refusal reached through PATCH, which shares the one write path. */
    @Test
    void a_patch_naming_an_unknown_member_is_audited_as_an_invalid_value() {
        ScimGroupResource created = service.create(
                CONNECTOR, new NewScimGroup("Engineering", List.of(), null));

        assertThatThrownBy(() -> service.patch(CONNECTOR, created.id(), current(created.id()), List.of(
                        new ScimGroupPatchOperation.AddMembers(List.of(UUID.randomUUID())))))
                .isInstanceOf(UnknownGroupMemberException.class);

        assertThat(refusalDetails(AuditOperation.SCIM_GROUP_REPLACE))
                .containsExactly(AuditScimRefusal.INVALID_VALUE.name());
    }

    /**
     * A rename onto a displayName another live Group holds is refused as a uniqueness failure on
     * the write, naming the Group it was aimed at — the replace-path counterpart of the create-time
     * uniqueness refusal, with its own audit call that mutation testing found unreached.
     */
    @Test
    void a_rename_onto_a_held_display_name_is_audited_as_a_uniqueness_failure_on_the_write() {
        service.create(CONNECTOR, new NewScimGroup("Engineering", List.of(), null));
        ScimGroupResource platform = service.create(
                CONNECTOR, new NewScimGroup("Platform", List.of(), null));

        assertThatThrownBy(() -> service.replace(
                        CONNECTOR,
                        platform.id(), current(platform.id()),
                        new ScimGroupReplacement("ENGINEERING", List.of(), null)))
                .isInstanceOf(DuplicateDisplayNameException.class);

        assertThat(refusalDetails(AuditOperation.SCIM_GROUP_REPLACE))
                .containsExactly(AuditScimRefusal.UNIQUENESS.name());
        assertThat(audit.of(AuditOperation.SCIM_GROUP_REPLACE))
                .singleElement()
                .satisfies(recorded -> assertThat(recorded.subjectId()).isEqualTo(platform.id()));
    }

    // ---- externalId -----------------------------------------------------------------------------

    private static final AuthenticatedConnector OTHER_CONNECTOR = new AuthenticatedConnector(
            UUID.randomUUID(), UUID.randomUUID(), ConnectorTokenScope.READ_WRITE);

    private ScimGroupResource aliased() {
        ScimGroupResource created = service.create(
                CONNECTOR, new NewScimGroup("Engineering", List.of(alice.id()), "eng-1"));
        aliases.put(OTHER_CONNECTOR.connectorId(), created.id(), "theirs");
        audit.reset();
        return created;
    }

    /**
     * A PUT with a different {@code externalId} re-keys the caller's alias — it was silently
     * discarded before — advances the Group's version alone, and leaves the other connector's.
     */
    @Test
    void a_put_with_a_different_alias_changes_only_the_callers_alias() {
        ScimGroupResource created = aliased();
        long aliceVersion = users.findById(alice.id()).orElseThrow().version();

        ScimGroupResource replaced = service.replace(CONNECTOR, created.id(),
                current(created.id()),
                new ScimGroupReplacement("Engineering", List.of(alice.id()), "eng-2"))
                .orElseThrow();

        assertThat(replaced.externalId()).isEqualTo("eng-2");
        assertThat(replaced.version()).isEqualTo(created.version() + 1);
        assertThat(groups.findById(created.id()).orElseThrow().version())
                .isEqualTo(created.version() + 1);
        assertThat(users.findById(alice.id()).orElseThrow().version())
                .as("a member renders no alias, so its version stays").isEqualTo(aliceVersion);
        assertThat(aliases.find(CONNECTOR.connectorId(), created.id())).contains("eng-2");
        assertThat(aliases.find(OTHER_CONNECTOR.connectorId(), created.id())).contains("theirs");
        assertThat(audit.of(AuditOperation.SCIM_GROUP_REPLACE)).singleElement()
                .extracting(RecordingAuditTrail.Recorded::detail).isEqualTo("EXTERNAL_ID");
    }

    /** RFC 7644 §3.5.1: a PUT omitting {@code externalId} removes the caller's alias. */
    @Test
    void a_put_omitting_the_alias_removes_only_the_callers_alias() {
        ScimGroupResource created = aliased();

        ScimGroupResource replaced = service.replace(CONNECTOR, created.id(),
                current(created.id()),
                new ScimGroupReplacement("Engineering", List.of(alice.id()), null))
                .orElseThrow();

        assertThat(replaced.externalId()).isNull();
        assertThat(replaced.version()).isEqualTo(created.version() + 1);
        assertThat(aliases.find(CONNECTOR.connectorId(), created.id())).isEmpty();
        assertThat(aliases.find(OTHER_CONNECTOR.connectorId(), created.id())).contains("theirs");
    }

    /** Restating the stored alias, with nothing else changed, writes nothing. */
    @Test
    void a_put_restating_the_alias_advances_no_version() {
        ScimGroupResource created = aliased();

        ScimGroupResource replaced = service.replace(CONNECTOR, created.id(),
                current(created.id()),
                new ScimGroupReplacement("Engineering", List.of(alice.id()), "eng-1"))
                .orElseThrow();

        assertThat(replaced.externalId()).isEqualTo("eng-1");
        assertThat(replaced.version()).isEqualTo(created.version());
        assertThat(audit.of(AuditOperation.SCIM_GROUP_REPLACE)).singleElement()
                .extracting(RecordingAuditTrail.Recorded::detail).isEqualTo("");
    }

    /**
     * An alias change alongside a column change advances the version ONCE: the replacement
     * already advanced it, so no second bump is added for the alias.
     */
    @Test
    void a_rename_and_an_alias_change_together_advance_the_version_once() {
        ScimGroupResource created = aliased();

        ScimGroupResource replaced = service.replace(CONNECTOR, created.id(),
                current(created.id()),
                new ScimGroupReplacement("Platform", List.of(alice.id()), "eng-2"))
                .orElseThrow();

        assertThat(replaced.version()).isEqualTo(created.version() + 1);
        assertThat(replaced.externalId()).isEqualTo("eng-2");
        assertThat(audit.of(AuditOperation.SCIM_GROUP_REPLACE)).singleElement()
                .extracting(RecordingAuditTrail.Recorded::detail)
                .isEqualTo("DISPLAY_NAME,EXTERNAL_ID");
    }

    /** PATCH sets and removes the caller's alias, and never the other connector's. */
    @Test
    void a_patch_sets_and_removes_only_the_callers_alias() {
        ScimGroupResource created = aliased();

        ScimGroupResource set = service.patch(CONNECTOR, created.id(), current(created.id()),
                List.of(new ScimGroupPatchOperation.SetExternalId("eng-2"))).orElseThrow();

        assertThat(set.externalId()).isEqualTo("eng-2");
        assertThat(set.version()).isEqualTo(created.version() + 1);

        ScimGroupResource removed = service.patch(CONNECTOR, created.id(), current(created.id()),
                List.of(new ScimGroupPatchOperation.RemoveExternalId())).orElseThrow();

        assertThat(removed.externalId()).isNull();
        assertThat(removed.version()).isEqualTo(created.version() + 2);
        assertThat(aliases.find(CONNECTOR.connectorId(), created.id())).isEmpty();
        assertThat(aliases.find(OTHER_CONNECTOR.connectorId(), created.id())).contains("theirs");
        assertThat(service.findById(OTHER_CONNECTOR, created.id()).orElseThrow().externalId())
                .isEqualTo("theirs");
    }

    /**
     * The Admin group may not be renamed, but the calling connector's own alias for it is
     * writable: it is that connector's name for the Group, read by no other, and changing it
     * neither renames the recovery authority nor changes who holds it.
     */
    @Test
    void the_callers_alias_on_the_admin_group_is_writable() {
        ScimGroup adminGroup = seedAdminGroup();

        ScimGroupResource set = service.patch(CONNECTOR, adminGroup.id(),
                current(adminGroup.id()),
                List.of(new ScimGroupPatchOperation.SetExternalId("admins-1"))).orElseThrow();

        assertThat(set.externalId()).isEqualTo("admins-1");
        assertThat(set.displayName()).isEqualTo("Admins");
        assertThat(set.version()).isEqualTo(adminGroup.version() + 1);
        assertThat(aliases.find(CONNECTOR.connectorId(), adminGroup.id())).contains("admins-1");
    }

    // ---- delete -------------------------------------------------------------------------------

    @Test
    void deleting_a_group_advances_every_former_members_version_and_is_audited() {
        ScimGroupResource created = service.create(
                CONNECTOR, new NewScimGroup("Engineering", List.of(alice.id()), null));
        long aliceBefore = users.require("alice").version();

        assertThat(service.delete(CONNECTOR, created.id(), current(created.id()))).isTrue();

        assertThat(users.require("alice").version()).isEqualTo(aliceBefore + 1);
        assertThat(audit.of(AuditOperation.SCIM_GROUP_DELETE)).singleElement()
                .satisfies(event -> assertThat(event.subjectId()).isEqualTo(created.id()));
        assertThat(tombstones.recorded()).containsExactly(
                new InMemoryScimTombstoneRepository.Tombstone(
                        ScimResourceType.GROUP, created.id(), ScimIdentities.NOW));
    }

    @Test
    void deleting_a_group_that_is_not_there_reports_absence() {
        assertThat(service.delete(CONNECTOR, UUID.randomUUID(), current(UUID.randomUUID()))).isFalse();
    }

    /**
     * A Group removed between the read and the delete reports absence, not success.
     *
     * <p>The delete reads the Group first — it has to, because a protected one must be refused
     * before anything is removed — which opens a window in which another transaction can delete
     * the row. So the method must report what the repository actually did rather than assume the
     * read result still holds; mutation testing replaced that returned value with a constant
     * {@code true} and no test noticed, because in every other case the row was still there.
     *
     * <p>It matters at the edge: the controller turns {@code false} into a {@code 404}, so a
     * hard-coded success would answer {@code 204} to a connector whose delete removed nothing,
     * and a connector converging on a desired state would record the Group as gone.
     */
    @Test
    void a_group_removed_between_the_read_and_the_delete_reports_absence() {
        ScimGroupResource created = service.create(
                CONNECTOR, new NewScimGroup("Engineering", List.of(alice.id()), null));
        groups.vanishBeforeNextDelete(created.id());

        assertThat(service.delete(CONNECTOR, created.id(), current(created.id())))
                .as("the delete must report what the repository did, not what the read found")
                .isFalse();
        assertThat(tombstones.recorded())
                .as("the transaction that removed the row wrote its tombstone; a second collides")
                .isEmpty();
    }

    // ---- the reserved resources ---------------------------------------------------------------

    /** AC 4: the Admin group cannot be renamed, and the refusal changes nothing. */
    @Test
    void renaming_the_admin_group_is_refused_and_leaves_it_unchanged_on_re_read() {
        ScimGroup adminGroup = seedAdminGroup();

        assertThatThrownBy(() -> service.replace(
                        CONNECTOR,
                        adminGroup.id(), current(adminGroup.id()),
                        new ScimGroupReplacement("Not Admins", List.of(bootstrapAdmin().id()), null)))
                .isInstanceOf(ProtectedResourceException.class)
                .satisfies(refusal -> assertThat(
                                ((ProtectedResourceException) refusal).reservedName())
                        .isEqualTo(ReservedResourceName.ADMIN_GROUP));

        ScimGroupResource reread = service.findById(CONNECTOR, adminGroup.id()).orElseThrow();
        assertThat(reread.displayName()).isEqualTo("Admins");
        assertThat(reread.version()).isEqualTo(adminGroup.version());
    }

    /** AC 4: the Admin group cannot be deleted. */
    @Test
    void deleting_the_admin_group_is_refused_and_it_is_still_there() {
        ScimGroup adminGroup = seedAdminGroup();

        assertThatThrownBy(() -> service.delete(CONNECTOR, adminGroup.id(), current(adminGroup.id())))
                .isInstanceOf(ProtectedResourceException.class);
        assertThat(tombstones.recorded()).isEmpty();

        assertThat(service.findById(CONNECTOR, adminGroup.id())).isPresent();
    }

    /** AC 4: the Bootstrap Admin's membership of the Admin group cannot be removed. */
    @Test
    void removing_the_bootstrap_admins_membership_is_refused_and_it_is_still_a_member() {
        ScimGroup adminGroup = seedAdminGroup();
        ScimUser recovery = bootstrapAdmin();

        assertThatThrownBy(() -> service.patch(
                        CONNECTOR,
                        adminGroup.id(), current(adminGroup.id()),
                        List.of(new ScimGroupPatchOperation.RemoveMembers(List.of(recovery.id())))))
                .isInstanceOf(ProtectedResourceException.class)
                .satisfies(refusal -> assertThat(
                                ((ProtectedResourceException) refusal).reservedName())
                        .isEqualTo(ReservedResourceName.BOOTSTRAP_ADMIN));

        assertThat(service.findById(CONNECTOR, adminGroup.id()).orElseThrow().members())
                .extracting(ScimGroupMember::userId)
                .contains(recovery.id());
    }

    /**
     * The refusals are evaluated against the RESULT of the whole sequence, not each step: the rule
     * protects the User's state, not the shape of the request.
     */
    @Test
    void a_patch_that_removes_and_re_adds_the_bootstrap_admin_is_allowed() {
        ScimGroup adminGroup = seedAdminGroup();
        ScimUser recovery = bootstrapAdmin();

        ScimGroupResource patched = service.patch(CONNECTOR, adminGroup.id(), current(adminGroup.id()), List.of(
                new ScimGroupPatchOperation.RemoveMembers(List.of(recovery.id())),
                new ScimGroupPatchOperation.AddMembers(List.of(recovery.id())))).orElseThrow();

        assertThat(patched.members()).extracting(ScimGroupMember::userId).contains(recovery.id());
    }

    /**
     * The Bootstrap Admin's membership is frozen in BOTH directions: it cannot be added to an
     * ordinary Group either, because a membership change moves its computed {@code groups} attribute
     * and its version — which the glossary's "no SCIM operation may mutate the User" forbids.
     */
    @Test
    void adding_the_bootstrap_admin_to_an_ordinary_group_is_refused_at_create() {
        seedAdminGroup();
        ScimUser recovery = bootstrapAdmin();

        assertThatThrownBy(() -> service.create(
                        CONNECTOR, new NewScimGroup("Engineering", List.of(recovery.id()), null)))
                .isInstanceOf(ProtectedResourceException.class);

        // The refusal must be AUDITED, not merely thrown. This assertion is the one that was
        // missing: the test asserted the exception type alone, so it passed while the create-time
        // refusal recorded nothing at all — an attempt to provision the recovery authority away
        // left no trace, which is the exact outcome this class's contract forbids. Recorded as a
        // refused CREATE with no resource id, because no Group came into existence to name.
        assertThat(refusalDetails(AuditOperation.SCIM_GROUP_CREATE))
                .containsExactly(AuditScimRefusal.MUTABILITY.name());
    }

    @Test
    void adding_the_bootstrap_admin_to_an_existing_ordinary_group_is_refused() {
        seedAdminGroup();
        ScimUser recovery = bootstrapAdmin();
        ScimGroupResource engineering = service.create(
                CONNECTOR, new NewScimGroup("Engineering", List.of(), null));

        assertThatThrownBy(() -> service.patch(
                        CONNECTOR,
                        engineering.id(), current(engineering.id()),
                        List.of(new ScimGroupPatchOperation.AddMembers(List.of(recovery.id())))))
                .isInstanceOf(ProtectedResourceException.class);
    }

    /** AC 7: every protected-resource refusal produces its own audit event. */
    @Test
    void a_protected_resource_refusal_is_audited_as_a_mutability_failure_naming_the_group() {
        ScimGroup adminGroup = seedAdminGroup();
        audit.reset();

        assertThatThrownBy(() -> service.delete(CONNECTOR, adminGroup.id(), current(adminGroup.id())))
                .isInstanceOf(ProtectedResourceException.class);

        assertThat(audit.of(AuditOperation.SCIM_GROUP_REPLACE)).singleElement()
                .satisfies(event -> {
                    assertThat(event.actorId()).isEqualTo(CONNECTOR.connectorId());
                    assertThat(event.subjectId()).isEqualTo(adminGroup.id());
                    assertThat(event.detail()).isEqualTo(AuditScimRefusal.MUTABILITY.name());
                });
    }

    /** Ordinary membership of the Admin group stays writable — that is what provisioning is for. */
    @Test
    void an_ordinary_user_can_be_added_to_and_removed_from_the_admin_group() {
        ScimGroup adminGroup = seedAdminGroup();
        ScimUser recovery = bootstrapAdmin();

        ScimGroupResource granted = service.patch(
                CONNECTOR,
                adminGroup.id(), current(adminGroup.id()),
                List.of(new ScimGroupPatchOperation.AddMembers(List.of(alice.id())))).orElseThrow();
        assertThat(granted.members()).extracting(ScimGroupMember::userId)
                .containsExactlyInAnyOrder(recovery.id(), alice.id());

        ScimGroupResource revoked = service.patch(
                CONNECTOR,
                adminGroup.id(), current(adminGroup.id()),
                List.of(new ScimGroupPatchOperation.RemoveMembers(List.of(alice.id()))))
                .orElseThrow();
        assertThat(revoked.members()).extracting(ScimGroupMember::userId)
                .containsExactly(recovery.id());
    }

    /**
     * Before seeding there is no Bootstrap Admin, so there is no User whose membership could be
     * frozen and every Group write is unaffected. This happens once per deployment, between the
     * first migration and the first startup's seeding.
     */
    @Test
    void with_no_bootstrap_admin_seeded_no_membership_is_frozen() {
        assertThat(users.findByReservedName(ReservedResourceName.BOOTSTRAP_ADMIN)).isEmpty();

        assertThat(service.create(
                        CONNECTOR, new NewScimGroup("Engineering", List.of(alice.id()), null))
                .members())
                .hasSize(1);
    }

    // ---- fixtures -----------------------------------------------------------------------------

    /**
     * Seeds the recovery path through the PORTS, not through a fixture that fabricates a reserved
     * resource: production has no such path, and a test that used one would prove the wrong thing.
     */
    private ScimGroup seedAdminGroup() {
        ScimUser recovery = users.createReserved(
                ScimIdentities.user("bootstrap"), ReservedResourceName.BOOTSTRAP_ADMIN);
        return groups.createReserved(
                ScimGroup.created(
                        UUID.randomUUID(),
                        "Admins",
                        List.of(ScimGroupMember.reference(recovery.id())),
                        ScimIdentities.NOW),
                ReservedResourceName.ADMIN_GROUP);
    }

    private ScimUser bootstrapAdmin() {
        return users.findByReservedName(ReservedResourceName.BOOTSTRAP_ADMIN).orElseThrow();
    }

    /** The detail of every refused event of one operation — the closed-set reason it recorded. */
    private List<String> refusalDetails(AuditOperation operation) {
        return audit.of(operation).stream()
                .map(RecordingAuditTrail.Recorded::detail)
                .filter(detail -> detail != null)
                .toList();
    }
}
