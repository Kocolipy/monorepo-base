package com.example.backend.scim.application;

import com.example.backend.audit.domain.AuditGroupAttribute;
import com.example.backend.audit.domain.AuditUserAttribute;
import com.example.backend.authorization.domain.Role;
import com.example.backend.scim.domain.ScimUserProfile;
import com.example.backend.scim.domain.ScimUserSessions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Everything a SCIM write implies, decided from its before and after state alone: the attributes
 * to audit, the Session revocations it causes, and the Role changes it makes.
 *
 * <p>This is the one place that says which edits end Sessions and which change Roles. The User
 * and Group write use cases keep their locking, preconditions and persistence; they hand the
 * stored state and the written state here and carry out what comes back. Nothing here performs
 * an effect, so <em>when</em> and <em>how</em> each one happens stays with the use case: a
 * revocation is still requested through {@link ScimUserSessions#revokeAfterCommit} (ADR 0002),
 * and an audit append is still the fail-open kind (ADR 0004).
 *
 * <p>The audit attribute set and the Session cause set stay two types, derived side by side. The
 * audit slice owns its vocabulary ({@link AuditUserAttribute}, {@link AuditGroupAttribute}), the
 * session port owns its own ({@link ScimUserSessions.Cause}), and an edit that is audited is not
 * thereby one a live Session must not outlast.
 *
 * <p>What the write must store is a fourth effect, decided beside the other three rather than
 * read back out of the audit set: the audit vocabulary is the audit slice's to change, and a use
 * case that steered its writes by it would start persisting differently the day an attribute
 * was renamed, split or stopped being audited.
 *
 * @param audited     the attributes the write moved, for its audit event; empty when it moved
 *                    nothing a client can read
 * @param revocations the Users whose Sessions end, and why, in the order to request them
 * @param roleChanges the Roles the write granted and revoked, revocations first
 * @param stored      what the write changed in storage: the resource's own columns, the calling
 *                    connector's alias, both, or neither
 * @param <A>         the audit slice's attribute vocabulary for the resource written
 */
record ScimWriteEffects<A extends Enum<A>>(
        Set<A> audited,
        List<SessionRevocation> revocations,
        List<RoleChange> roleChanges,
        Stored stored) {

    ScimWriteEffects {
        audited = Collections.unmodifiableSet(new LinkedHashSet<>(audited));
        revocations = List.copyOf(revocations);
        roleChanges = List.copyOf(roleChanges);
        Objects.requireNonNull(stored, "stored");
    }

    /**
     * What a write changed in storage. The alias is not a column of the User or Group: it lives
     * per connector beside the resource, so it is written separately, and a write that moved only
     * the alias advanced no version through the resource's own replacement.
     *
     * @param columns whether any of the resource's own stored attributes changed
     * @param alias   whether the calling connector's {@code externalId} changed
     */
    record Stored(boolean columns, boolean alias) {

        static final Stored NOTHING = new Stored(false, false);

        /** Whether the write moved nothing at all: nothing to store and no version to advance. */
        boolean nothing() {
            return !columns && !alias;
        }

        /** Whether the alias is all that moved, so the version must be advanced on its own. */
        boolean aliasOnly() {
            return alias && !columns;
        }
    }

    /** One User's Sessions to end once the write commits, and every reason the write gave. */
    record SessionRevocation(UUID userId, Set<ScimUserSessions.Cause> causes) {

        SessionRevocation {
            if (causes.isEmpty()) {
                throw new IllegalArgumentException("a revocation has a cause");
            }
            causes = Collections.unmodifiableSet(EnumSet.copyOf(causes));
        }
    }

    /** A User gaining or losing a mapped Group's Role through that Group's membership. */
    record RoleChange(Kind kind, UUID userId, UUID groupId, Role role) {

        enum Kind {
            GRANTED,
            REVOKED
        }
    }

    /**
     * The state of a User a write compares: its profile, the calling connector's alias, and its
     * credential. The hash, not the request: a write that sets the password it already holds
     * still changed the credential, because each hash is salted afresh, and a removal of a
     * password that was never there changed nothing.
     */
    record UserState(ScimUserProfile profile, String externalId, String passwordHash) {
    }

    /**
     * The state of a Group a write compares: its label, its direct members, and the calling
     * connector's alias — which is not a Group column, so it travels beside the Group.
     */
    record GroupState(String displayName, Set<UUID> memberIds, String externalId) {

        GroupState {
            memberIds = Collections.unmodifiableSet(new LinkedHashSet<>(memberIds));
        }
    }

    /**
     * What a User PUT or PATCH implies.
     *
     * <p>Compared rather than inferred from the request: a PUT resending the stored state moved
     * nothing, and a PATCH whose operations cancel out moved nothing either.
     *
     * <p>Sessions end for the changes a live Session must not outlast, per the specification
     * plan's revocation contract: {@code active} going from true to false, the credential changing,
     * and {@code userName} changing — once, with every cause that applied. Reactivation, staying
     * inactive, and a profile, email or alias change are not among them: none is something a
     * Session was issued against. A User write changes no Role: a Role is conferred by a mapped
     * Group's membership, and a User write cannot move a membership.
     */
    static ScimWriteEffects<AuditUserAttribute> ofUserWrite(
            UUID userId, UserState before, UserState after) {
        ScimUserProfile was = before.profile();
        ScimUserProfile is = after.profile();
        boolean userNameChanged = !was.userName().equals(is.userName());
        boolean passwordChanged = !Objects.equals(before.passwordHash(), after.passwordHash());
        boolean aliasChanged = !Objects.equals(before.externalId(), after.externalId());
        Stored stored = new Stored(!was.equals(is) || passwordChanged, aliasChanged);

        Set<AuditUserAttribute> audited = EnumSet.noneOf(AuditUserAttribute.class);
        addIf(audited, AuditUserAttribute.USER_NAME, userNameChanged);
        addIf(audited, AuditUserAttribute.NAME, !was.name().equals(is.name()));
        addIf(audited, AuditUserAttribute.DISPLAY_NAME,
                !Objects.equals(was.displayName(), is.displayName()));
        addIf(audited, AuditUserAttribute.PREFERRED_LANGUAGE,
                !Objects.equals(was.preferredLanguage(), is.preferredLanguage()));
        addIf(audited, AuditUserAttribute.LOCALE, !Objects.equals(was.locale(), is.locale()));
        addIf(audited, AuditUserAttribute.TIMEZONE,
                !Objects.equals(was.timezone(), is.timezone()));
        addIf(audited, AuditUserAttribute.ACTIVE, was.active() != is.active());
        addIf(audited, AuditUserAttribute.EMAILS, !was.emails().equals(is.emails()));
        addIf(audited, AuditUserAttribute.PASSWORD, passwordChanged);
        addIf(audited, AuditUserAttribute.EXTERNAL_ID, aliasChanged);

        Set<ScimUserSessions.Cause> causes = EnumSet.noneOf(ScimUserSessions.Cause.class);
        addIf(causes, ScimUserSessions.Cause.DEACTIVATED, was.active() && !is.active());
        addIf(causes, ScimUserSessions.Cause.PASSWORD_CHANGED, passwordChanged);
        addIf(causes, ScimUserSessions.Cause.USER_NAME_CHANGED, userNameChanged);

        return new ScimWriteEffects<>(audited, revocationOf(userId, causes), List.of(), stored);
    }

    /**
     * What deleting a User implies: its Sessions end. The deletion's audit event names no
     * attribute, and the User's memberships go with it without being Role changes of a Group.
     */
    static ScimWriteEffects<AuditUserAttribute> ofUserDeletion(UUID userId) {
        return new ScimWriteEffects<>(
                EnumSet.noneOf(AuditUserAttribute.class),
                revocationOf(userId, EnumSet.of(ScimUserSessions.Cause.DELETED)),
                List.of(),
                Stored.NOTHING);
    }

    /**
     * What a Group PUT or PATCH implies.
     *
     * <p>The attributes are compared rather than inferred from the verb, so a PUT resending the
     * stored state audits none.
     *
     * <p>A Group the role mapping names confers its Role on every direct member, so when
     * {@code mappedRole} is present, a membership change is a change of power: a grant for each
     * User added, a revocation for each User removed — and each removed User's Sessions end, since
     * a Session carries the Permissions it was issued with. An addition ends no Session: the User
     * holds the Role from its next sign-in. Decided from the membership before and after, so PUT
     * and every PATCH shape are one rule, a User removed and added back keeps its Role and its
     * Sessions, and a removal naming a non-member changes nothing. An unmapped Group confers no
     * Role, so its membership changes are none of this.
     */
    static ScimWriteEffects<AuditGroupAttribute> ofGroupWrite(
            UUID groupId, GroupState before, GroupState after, Optional<Role> mappedRole) {
        boolean displayNameChanged = !before.displayName().equals(after.displayName());
        boolean membersChanged = !before.memberIds().equals(after.memberIds());
        boolean aliasChanged = !Objects.equals(before.externalId(), after.externalId());

        Set<AuditGroupAttribute> audited = EnumSet.noneOf(AuditGroupAttribute.class);
        addIf(audited, AuditGroupAttribute.DISPLAY_NAME, displayNameChanged);
        addIf(audited, AuditGroupAttribute.MEMBERS, membersChanged);
        addIf(audited, AuditGroupAttribute.EXTERNAL_ID, aliasChanged);
        return membershipEffects(
                audited, groupId, before.memberIds(), after.memberIds(), mappedRole,
                new Stored(displayNameChanged || membersChanged, aliasChanged));
    }

    /**
     * What deleting a Group implies: every member leaves it at once, so a mapped Group's members
     * each lose its Role and their Sessions, exactly as removing them one by one would. The
     * deletion's audit event names no attribute.
     */
    static ScimWriteEffects<AuditGroupAttribute> ofGroupDeletion(
            UUID groupId, Set<UUID> memberIds, Optional<Role> mappedRole) {
        return membershipEffects(
                EnumSet.noneOf(AuditGroupAttribute.class), groupId, memberIds, Set.of(), mappedRole,
                Stored.NOTHING);
    }

    private static ScimWriteEffects<AuditGroupAttribute> membershipEffects(
            Set<AuditGroupAttribute> audited,
            UUID groupId,
            Set<UUID> before,
            Set<UUID> after,
            Optional<Role> mappedRole,
            Stored stored) {
        if (mappedRole.isEmpty()) {
            return new ScimWriteEffects<>(audited, List.of(), List.of(), stored);
        }
        Role role = mappedRole.get();
        List<RoleChange> roleChanges = new ArrayList<>();
        List<SessionRevocation> revocations = new ArrayList<>();
        for (UUID userId : before) {
            if (!after.contains(userId)) {
                roleChanges.add(new RoleChange(RoleChange.Kind.REVOKED, userId, groupId, role));
                revocations.add(new SessionRevocation(
                        userId, EnumSet.of(ScimUserSessions.Cause.ROLE_REVOKED)));
            }
        }
        for (UUID userId : after) {
            if (!before.contains(userId)) {
                roleChanges.add(new RoleChange(RoleChange.Kind.GRANTED, userId, groupId, role));
            }
        }
        return new ScimWriteEffects<>(audited, revocations, roleChanges, stored);
    }

    private static List<SessionRevocation> revocationOf(
            UUID userId, Set<ScimUserSessions.Cause> causes) {
        return causes.isEmpty() ? List.of() : List.of(new SessionRevocation(userId, causes));
    }

    private static <E> void addIf(Set<E> set, E element, boolean applies) {
        if (applies) {
            set.add(element);
        }
    }
}
