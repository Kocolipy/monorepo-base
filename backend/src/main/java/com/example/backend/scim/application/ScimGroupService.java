package com.example.backend.scim.application;

import com.example.backend.audit.domain.AuditGroupAttribute;
import com.example.backend.audit.domain.AuditScimRefusal;
import com.example.backend.audit.domain.AuditTrail;
import com.example.backend.scim.domain.AuthenticatedConnector;
import com.example.backend.scim.domain.DuplicateDisplayNameException;
import com.example.backend.scim.domain.ProtectedResourceException;
import com.example.backend.scim.domain.ReservedResourceName;
import com.example.backend.scim.domain.ScimExternalIdRepository;
import com.example.backend.scim.domain.ScimGroup;
import com.example.backend.scim.domain.ScimGroupMember;
import com.example.backend.scim.domain.ScimGroupRepository;
import com.example.backend.scim.domain.ScimPageRequest;
import com.example.backend.scim.domain.ScimResourceType;
import com.example.backend.scim.domain.ScimTombstoneRepository;
import com.example.backend.scim.domain.ScimUser;
import com.example.backend.scim.domain.ScimUserRepository;
import com.example.backend.scim.domain.UnknownGroupMemberException;
import com.example.backend.scim.domain.ScimVersionPrecondition;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creating, reading, changing and deleting SCIM Groups, as a connector asks for it.
 *
 * <p>Every method takes the {@link AuthenticatedConnector} rather than reading it from a
 * security context, for the two reasons {@link ScimUserService} does: an {@code externalId} is
 * connector-scoped, so a read that did not know who was asking could not resolve the alias,
 * and an audit event names the acting connector, so making the actor a parameter means no code
 * path exists that records a SCIM operation with no actor.
 *
 * <h2>What a Group write may not do</h2>
 *
 * <p>Two resources are reserved for the deployment's recovery, and the refusals below are the
 * whole of their protection on this surface:
 *
 * <ul>
 *   <li>The <strong>Admin group</strong> may not be renamed and may not be deleted. Its
 *       ordinary membership stays writable, because granting and revoking administrative
 *       access is exactly what external provisioning is for.
 *   <li>The <strong>Bootstrap Admin's</strong> membership is frozen — it cannot be removed
 *       from the Admin group, and it cannot be added to or removed from any other Group.
 *       The glossary's rule is that no SCIM operation may mutate that User, and a membership
 *       change does mutate it: it moves the User's computed {@code groups} attribute and
 *       advances its version, which a client reads.
 * </ul>
 *
 * <p>Every refusal is raised before anything is written, so a refused write changes nothing —
 * which is what makes "verified by re-reading unchanged" a property of this class rather than
 * of a test's luck. Each is audited, and the audit append is deliberately the fail-open kind:
 * the refusal rolls this transaction back, so an append that joined it would be rolled back
 * with it, and an attempt to provision the recovery authority away would leave no trace.
 */
@Service
public class ScimGroupService {

    private final ScimGroupRepository groups;
    private final ScimUserRepository users;
    private final ScimExternalIdRepository aliases;
    private final ScimTombstoneRepository tombstones;
    private final AuditTrail audit;
    private final Clock clock;

    public ScimGroupService(
            ScimGroupRepository groups,
            ScimUserRepository users,
            ScimExternalIdRepository aliases,
            ScimTombstoneRepository tombstones,
            AuditTrail audit,
            Clock clock) {
        this.groups = groups;
        this.users = users;
        this.aliases = aliases;
        this.tombstones = tombstones;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * Creates a Group, its connector alias and its audit event in one transaction.
     *
     * <p>A {@code displayName} already taken arrives as {@link DuplicateDisplayNameException}
     * from the failed statement rather than from a prior read, and a member that is not a live
     * User as {@link UnknownGroupMemberException} from the failed membership insert. Both are
     * audited in a transaction of their own — this one is already doomed — and re-thrown for
     * the adapter to render.
     */
    @Transactional
    public ScimGroupResource create(AuthenticatedConnector connector, NewScimGroup command) {
        refuseFrozenMembershipChange(connector, List.of(), command.memberIds());
        ScimGroup group = ScimGroup.created(
                UUID.randomUUID(),
                command.displayName(),
                references(command.memberIds()),
                clock.instant());
        ScimGroup created;
        try {
            created = groups.create(group);
        } catch (DuplicateDisplayNameException duplicate) {
            audit.recordScimGroupCreateRejected(
                    connector.connectorId(), AuditScimRefusal.UNIQUENESS);
            throw duplicate;
        } catch (UnknownGroupMemberException unknownMember) {
            audit.recordScimGroupCreateRejected(
                    connector.connectorId(), AuditScimRefusal.INVALID_VALUE);
            throw unknownMember;
        }
        if (command.externalId() != null) {
            aliases.put(connector.connectorId(), created.id(), command.externalId());
        }
        audit.recordScimGroupCreated(connector.connectorId(), created.id());
        return ScimGroupResource.of(created, command.externalId());
    }

    /**
     * One Group by its stable id, as this connector sees it.
     *
     * <p>Deliberately not audited, for the reason a single User read is not: a single-resource
     * retrieval is the ordinary unit of provisioning traffic, and recording it would bury the
     * collection reads that indicate an enumeration. The absence of a call here is where that
     * distinction is enforced.
     */
    @Transactional(readOnly = true)
    public Optional<ScimGroupResource> findById(AuthenticatedConnector connector, UUID id) {
        return groups.findById(id).map(group -> projection(connector, group));
    }

    /**
     * A page of Groups, and the total the page came from.
     *
     * <p>Audited as a bulk read before the page is returned, whatever the page size and
     * whatever comes back. The audit call is inside the transaction that reads, so a trail that
     * cannot record the read is a read that does not complete.
     */
    @Transactional
    public ScimGroupListing list(AuthenticatedConnector connector, ScimPageRequest page) {
        long total = groups.countAll();
        List<ScimGroupResource> resources = page.count() == 0
                ? List.of()
                : groups.findPage(page).stream()
                        .map(group -> projection(connector, group))
                        .toList();
        audit.recordScimGroupsListed(connector.connectorId());
        return new ScimGroupListing(resources, total, page);
    }

    /**
     * Replaces a Group's label and its whole membership — a PUT.
     *
     * <p>An attribute the submitted document did not mention has already become its empty value
     * at the adapter, because that is what replacing a resource means. So a PUT with no
     * {@code members} empties the Group, and the refusals below see that as the removal it is.
     */
    @Transactional
    public Optional<ScimGroupResource> replace(
            AuthenticatedConnector connector,
            UUID id,
            ScimVersionPrecondition precondition,
            ScimGroupReplacement replacement) {
        return apply(
                connector,
                id,
                precondition,
                current -> current.replacedWith(
                        replacement.displayName(),
                        references(replacement.memberIds()),
                        clock.instant()));
    }

    /**
     * Applies a sequence of PATCH operations to a Group.
     *
     * <p><strong>Partial failure rolls back entirely.</strong> The operations are folded onto
     * the Group in memory and written once, so a sequence whose third operation is refused
     * leaves the first two unapplied — there is no intermediate state to roll back, because
     * none was ever written. That is stronger than a transactional rollback and is the reason
     * the fold happens here rather than one write per operation.
     *
     * <p>The refusals are evaluated against the RESULT of the whole sequence, not against each
     * step: a PATCH that removes the Bootstrap Admin and adds it back is not a violation,
     * because the User it must not mutate ends up as it started.
     */
    @Transactional
    public Optional<ScimGroupResource> patch(
            AuthenticatedConnector connector,
            UUID id,
            ScimVersionPrecondition precondition,
            List<ScimGroupPatchOperation> operations) {
        return apply(connector, id, precondition, current -> folded(current, operations));
    }

    /**
     * The Group the operations describe, computed without writing anything.
     *
     * <p>A method rather than the body of a lambda because the fold reassigns what it is
     * building, which a lambda's captured locals cannot do — and because naming it says what it
     * is: the whole PATCH resolved to a single desired state, which is what makes partial
     * failure unrepresentable.
     */
    private ScimGroup folded(ScimGroup current, List<ScimGroupPatchOperation> operations) {
        String displayName = current.displayName();
        List<UUID> memberIds = new ArrayList<>(
                current.members().stream().map(ScimGroupMember::userId).toList());
        for (ScimGroupPatchOperation operation : operations) {
            switch (operation) {
                case ScimGroupPatchOperation.SetDisplayName set -> displayName = set.displayName();
                case ScimGroupPatchOperation.AddMembers add -> memberIds.addAll(add.userIds());
                case ScimGroupPatchOperation.RemoveMembers remove ->
                        memberIds.removeAll(remove.userIds());
                case ScimGroupPatchOperation.ReplaceMembers replace -> {
                    memberIds.clear();
                    memberIds.addAll(replace.userIds());
                }
                case ScimGroupPatchOperation.RemoveAllMembers ignored -> memberIds.clear();
            }
        }
        return current.replacedWith(displayName, references(memberIds), clock.instant());
    }

    /**
     * Deletes a Group.
     *
     * <p>The Admin group cannot be deleted, so the refusal is checked before anything is
     * removed. An ordinary Group containing the Bootstrap Admin needs no special case: such a
     * Group cannot exist, because no write that would have added the User to it is accepted.
     *
     * <p>A tombstone holding only the id, the type and the time is written in the same
     * transaction; it is never consulted for uniqueness, so the former {@code displayName} is
     * free for the next create.
     *
     * <p>Reports whether a Group was there to delete, so the adapter renders absence as a
     * {@code 404} rather than this throwing an exception the caller cannot distinguish from a
     * failure.
     */
    @Transactional
    public boolean delete(
            AuthenticatedConnector connector, UUID id, ScimVersionPrecondition precondition) {
        Optional<ScimGroup> stored = groups.findByIdForUpdate(id);
        if (stored.isEmpty()) {
            return false;
        }
        ScimGroup group = stored.get();
        precondition.requireSatisfiedBy(group.version());
        if (group.isProtectedFromWrites()) {
            throw refuse(connector, id, group.reservedName());
        }
        Instant now = clock.instant();
        boolean deleted = groups.deleteById(id, now);
        if (deleted) {
            // Only for a row this transaction removed: a Group deleted by another between the
            // read and the delete already has its tombstone, and a second would collide on it.
            tombstones.record(ScimResourceType.GROUP, id, now);
        }
        audit.recordScimGroupDeleted(connector.connectorId(), id);
        return deleted;
    }

    /**
     * The shared shape of a Group write: read the stored Group, compute what it should become,
     * refuse what the reservations forbid, write it, audit what moved.
     *
     * <p>The Group is read under its resource lock and the {@code If-Match} precondition checked
     * against that version, so two writers holding the same precondition produce one success and
     * one {@code 412} rather than a lost update.
     *
     * <p>One method for PUT and PATCH because everything except the computation is identical,
     * and because the refusals must not be able to differ between the two verbs — a protection
     * that held for PUT and not for PATCH would be no protection at all.
     */
    private Optional<ScimGroupResource> apply(
            AuthenticatedConnector connector,
            UUID id,
            ScimVersionPrecondition precondition,
            UnaryOperator<ScimGroup> change) {
        Optional<ScimGroup> stored = groups.findByIdForUpdate(id);
        if (stored.isEmpty()) {
            return Optional.empty();
        }
        ScimGroup current = stored.get();
        // After existence, before anything is computed: a missing or stale precondition changes
        // nothing, and the lock above is what makes "stale" exact under concurrent writers.
        precondition.requireSatisfiedBy(current.version());
        ScimGroup desired = change.apply(current);

        if (current.isProtectedFromWrites()
                && !current.displayName().equals(desired.displayName())) {
            throw refuse(connector, id, current.reservedName());
        }
        refuseFrozenMembershipChange(connector, id, current, desired);

        ScimGroup written;
        try {
            written = groups.replace(desired).orElse(null);
        } catch (DuplicateDisplayNameException duplicate) {
            audit.recordScimGroupWriteRejected(
                    connector.connectorId(), id, AuditScimRefusal.UNIQUENESS);
            throw duplicate;
        } catch (UnknownGroupMemberException unknownMember) {
            audit.recordScimGroupWriteRejected(
                    connector.connectorId(), id, AuditScimRefusal.INVALID_VALUE);
            throw unknownMember;
        }
        if (written == null) {
            // Deleted between the read and the write. The caller sees the same 404 it would
            // have seen a moment earlier, which is the truthful answer either way.
            return Optional.empty();
        }
        audit.recordScimGroupReplaced(
                connector.connectorId(), id, changedAttributes(current, written));
        return Optional.of(projection(connector, written));
    }

    /**
     * Refuses a write that would add the Bootstrap Admin to a Group or remove it from one.
     *
     * <p>Evaluated on the resulting membership rather than on the operations, so a sequence
     * that removes and re-adds the User is allowed: the rule protects the User's state, not the
     * shape of the request.
     */
    private void refuseFrozenMembershipChange(
            AuthenticatedConnector connector, UUID id, ScimGroup current, ScimGroup desired) {
        Optional<UUID> bootstrapAdminId = bootstrapAdminId();
        if (bootstrapAdminId.isEmpty()) {
            return;
        }
        UUID protectedUserId = bootstrapAdminId.get();
        if (current.hasMember(protectedUserId) != desired.hasMember(protectedUserId)) {
            throw refuse(connector, id, ReservedResourceName.BOOTSTRAP_ADMIN);
        }
    }

    /**
     * The create-time form of the same rule: a new Group starts with no members, so any
     * membership naming the Bootstrap Admin is adding it.
     *
     * <p>Audited as a refused CREATE rather than a refused write, because there is no resource
     * to name — a Group that was never created has no id, so {@code recordScimGroupCreateRejected}
     * records the connector and the reason and nothing else.
     *
     * <p>It audits HERE rather than leaving it to the caller. An earlier version of this method
     * raised without auditing and its javadoc claimed "the caller records it", which was simply
     * untrue: the only caller invokes it as the first statement of {@link #create}, outside the
     * try block that audits the two persistence refusals, so an attempt to provision the recovery
     * authority away left NO trace — precisely what this class's own contract says must never
     * happen. The test covering it asserted only the exception type, so nothing failed.
     */
    private void refuseFrozenMembershipChange(
            AuthenticatedConnector connector, List<UUID> current, List<UUID> desired) {
        Optional<UUID> bootstrapAdminId = bootstrapAdminId();
        if (bootstrapAdminId.isEmpty()) {
            return;
        }
        UUID protectedUserId = bootstrapAdminId.get();
        if (current.contains(protectedUserId) != desired.contains(protectedUserId)) {
            audit.recordScimGroupCreateRejected(
                    connector.connectorId(), AuditScimRefusal.MUTABILITY);
            throw new ProtectedResourceException(ReservedResourceName.BOOTSTRAP_ADMIN);
        }
    }

    /**
     * The Bootstrap Admin's id, or empty before seeding has run.
     *
     * <p>Empty is not an error and does not open the protection: with no Bootstrap Admin there
     * is no User whose membership could be frozen, and every Group write is therefore
     * unaffected. It happens exactly once per deployment, between the first migration and the
     * first startup's seeding.
     */
    private Optional<UUID> bootstrapAdminId() {
        return users.findByReservedName(ReservedResourceName.BOOTSTRAP_ADMIN).map(ScimUser::id);
    }

    /** Records the refusal and returns the exception, so neither can happen without the other. */
    private ProtectedResourceException refuse(
            AuthenticatedConnector connector, UUID groupId, ReservedResourceName reservedName) {
        audit.recordScimGroupWriteRejected(
                connector.connectorId(), groupId, AuditScimRefusal.MUTABILITY);
        return new ProtectedResourceException(reservedName);
    }

    /**
     * Which attributes the write actually moved, for the event's changed paths.
     *
     * <p>Compared rather than inferred from the verb: a PUT that resends the stored state moved
     * nothing, and recording it as a change to both attributes would make the trail unusable
     * for finding the writes that mattered.
     */
    private static Set<AuditGroupAttribute> changedAttributes(ScimGroup before, ScimGroup after) {
        Set<AuditGroupAttribute> changed = EnumSet.noneOf(AuditGroupAttribute.class);
        if (!before.displayName().equals(after.displayName())) {
            changed.add(AuditGroupAttribute.DISPLAY_NAME);
        }
        if (!memberIdSet(before).equals(memberIdSet(after))) {
            changed.add(AuditGroupAttribute.MEMBERS);
        }
        return changed;
    }

    private static Set<UUID> memberIdSet(ScimGroup group) {
        return group.members().stream()
                .map(ScimGroupMember::userId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Submitted ids as membership values, with no label — the label is read-only. */
    private static List<ScimGroupMember> references(List<UUID> userIds) {
        return userIds.stream().map(ScimGroupMember::reference).toList();
    }

    /** The stored Group as this connector sees it, alias included. */
    private ScimGroupResource projection(AuthenticatedConnector connector, ScimGroup group) {
        return ScimGroupResource.of(
                group, aliases.find(connector.connectorId(), group.id()).orElse(null));
    }
}
