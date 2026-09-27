package com.example.backend.auth.application;

import com.example.backend.audit.domain.AuditAdministrativeRefusal;
import com.example.backend.audit.domain.AuditTrail;
import com.example.backend.auth.domain.AccountSessions;
import com.example.backend.observability.LogEvent;
import com.example.backend.scim.domain.NormalizedUserName;
import com.example.backend.scim.domain.ReservedResourceName;
import com.example.backend.scim.domain.ScimGroup;
import com.example.backend.scim.domain.ScimGroupRepository;
import com.example.backend.scim.domain.ScimLoginState;
import com.example.backend.scim.domain.ScimUser;
import com.example.backend.scim.domain.ScimUserRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The identity use cases an administrator drives: reviewing who has access, and changing whether
 * an identity may be used.
 *
 * <p>Separate from {@link LoginIdentityService}, which serves the login path. Keeping them apart
 * means the authentication path does not depend on a class that also mutates identities, and the
 * administrative guards below live beside nothing the login path could accidentally bypass.
 *
 * <p>Reactivating and unlocking are separate operations here as well as in the domain. Either one
 * alone leaves the other in force, so restoring a deactivated identity that also locked itself out
 * takes both calls — deliberately, since an administrator should say which of the two they mean.
 *
 * <h2>Why this is here and not in the scim slice</h2>
 *
 * <p>Deactivation has to reach the sessions the identity is already holding, and the port that
 * ends them ({@link AccountSessions}) is this slice's: a session is a fact about the login
 * surface, not about the directory. So the use case that needs both the directory and the
 * sessions lives on the side that owns the sessions and reaches the directory through its ports,
 * which is also the direction that keeps the two slices acyclic.
 */
@Service
public class IdentityAdministrationService {

    private static final Logger log = LoggerFactory.getLogger(IdentityAdministrationService.class);

    private static final String DEACTIVATE_ACTION = "identity.deactivate";
    private static final String ACTIVATE_ACTION = "identity.activate";
    private static final String UNLOCK_ACTION = "identity.unlock";

    private final ScimUserRepository users;
    private final ScimGroupRepository groups;
    private final AccountSessions sessions;
    private final AfterCommit afterCommit;
    private final AuditTrail audit;
    private final Clock clock;

    public IdentityAdministrationService(
            ScimUserRepository users,
            ScimGroupRepository groups,
            AccountSessions sessions,
            AfterCommit afterCommit,
            AuditTrail audit,
            Clock clock) {
        this.users = users;
        this.groups = groups;
        this.sessions = sessions;
        this.afterCommit = afterCommit;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * Every identity, for administrative review. Never carries a password hash.
     *
     * <p>The Admin group is read ONCE and membership answered from it in memory, rather than asked
     * per identity: the per-identity question is one statement each, and a listing of a thousand
     * identities would be a thousand of them to compute a column.
     */
    @Transactional(readOnly = true)
    public List<IdentitySummary> listIdentities() {
        Optional<ScimGroup> adminGroup =
                groups.findByReservedName(ReservedResourceName.ADMIN_GROUP);
        return users.findAllOrderedByNormalizedUserName().stream()
                .map(user -> summarize(user, isAdmin(adminGroup, user)))
                .toList();
    }

    /**
     * Closes an identity to logins and ends the sessions it is already holding, so it stops acting
     * now rather than when those sessions expire. The lockout is untouched: this is not a penalty
     * and says nothing about the failure run.
     *
     * <p>Three refusals guard against an administrator removing the only means of reversing this.
     * None is about authorization — the caller is an admin, and the action is what is refused. A
     * refused deactivation revokes nothing: every check runs before anything is written or ended.
     *
     * <p>The third is the Bootstrap Admin, which cannot be deactivated at all. Its exemption from
     * lockout is what keeps every other identity's permanent lock recoverable, and that exemption
     * is from <em>locking</em> only — a deactivated Bootstrap Admin cannot log in, so deactivating
     * it while the other administrators are locked out would leave a deployment no principal can
     * enter and nothing but direct database access can repair. It is the recovery identity whether
     * or not it is the last active administrator, so this check does not depend on how many others
     * there are.
     *
     * <p>Every refusal is audited as well as logged, because the log line deliberately names
     * neither party: a run of attempts to disable the recovery identity is a signal only the trail
     * can carry.
     *
     * <p>The revocation happens after the transaction commits, so an identity whose row could not
     * be written keeps its sessions — and so does one whose write was rolled back after this method
     * returned, which is the reason it is not simply the last statement here: Redis is not in the
     * transaction, and a revocation already performed cannot be undone by a rollback. See
     * {@code /docs/adr/0002-revoke-sessions-after-commit.md}.
     *
     * <p>It is still not a lock. A login already in flight reads {@code active} as it was before
     * this transaction committed, and a session it mints is revoked only if it commits before the
     * revocation runs; deferring to after the commit bounds that window at the commit rather than
     * straddling it, which is as narrow as it gets without holding a lock on the row.
     */
    @Transactional
    public IdentitySummary deactivate(String userName, String requestedBy) {
        ScimUser user = require(userName);
        Optional<ScimGroup> adminGroup =
                groups.findByReservedName(ReservedResourceName.ADMIN_GROUP);
        UUID actorId = actorId(requestedBy);

        if (isSelf(user, requestedBy)) {
            throw refuse(
                    DEACTIVATE_ACTION,
                    AuditAdministrativeRefusal.SELF_DISABLE,
                    actorId,
                    user.id(),
                    "An identity cannot deactivate itself");
        }
        if (user.isProtectedFromWrites()) {
            throw refuse(
                    DEACTIVATE_ACTION,
                    AuditAdministrativeRefusal.PROTECTED_RESOURCE,
                    actorId,
                    user.id(),
                    "The bootstrap administrator is the deployment's recovery identity and"
                            + " cannot be deactivated");
        }
        if (isLastActiveAdministrator(adminGroup, user)) {
            throw refuse(
                    DEACTIVATE_ACTION,
                    AuditAdministrativeRefusal.LAST_ENABLED_ADMINISTRATOR,
                    actorId,
                    user.id(),
                    "Deactivating the last active administrator would leave nobody able to"
                            + " reactivate it");
        }

        IdentitySummary deactivated = applyActive(user, false, adminGroup);
        audit.recordAccountDisabled(actorId, user.id());
        afterCommit.run(() -> sessions.revokeAll(user.id()));
        succeeded(DEACTIVATE_ACTION);
        return deactivated;
    }

    /**
     * Reopens an identity to logins. A lockout it is serving is left standing, and standing is
     * where it stays until {@link #unlock} lifts it — restoring access is not a finding that the
     * failed logins did not happen.
     *
     * <p>Sessions are not given back. {@link #deactivate} ended them, and a session is not a thing
     * an administrator can hand over — the identity signs in again.
     */
    @Transactional
    public IdentitySummary activate(String userName, String requestedBy) {
        ScimUser user = require(userName);
        IdentitySummary activated = applyActive(
                user, true, groups.findByReservedName(ReservedResourceName.ADMIN_GROUP));
        audit.recordAccountEnabled(actorId(requestedBy), user.id());
        succeeded(ACTIVATE_ACTION);
        return activated;
    }

    /**
     * Ends a lockout, clearing the failure run with it. Says nothing about whether the identity is
     * active — a deactivated identity can be unlocked, and stays deactivated.
     *
     * <p>The only way a lockout ends. Nothing expires it and no other operation lifts it, so an
     * identity that locked itself out stays locked until an administrator performs exactly this.
     *
     * <p>Idempotent: an identity serving no lockout is returned unchanged and nothing is written.
     * The version is not advanced either way, because the failure run is not a SCIM attribute —
     * unlocking changes nothing a connector can read.
     */
    @Transactional
    public IdentitySummary unlock(String userName, String requestedBy) {
        ScimUser user = require(userName);
        ScimLoginState cleared = user.login().withFailureRunCleared();
        if (cleared != user.login()) {
            users.updateLoginState(user.id(), cleared);
        }
        audit.recordLockoutLiftedByUnlock(actorId(requestedBy), user.id());
        succeeded(UNLOCK_ACTION);
        return summarize(
                new ScimUser(
                        user.id(),
                        user.profile(),
                        cleared,
                        user.reservedName(),
                        user.version(),
                        user.createdAt(),
                        user.lastModifiedAt()),
                isAdmin(groups.findByReservedName(ReservedResourceName.ADMIN_GROUP), user));
    }

    /**
     * Whether the caller is the identity they are acting on.
     *
     * <p>Compared on the NORMALIZED userName, not on the raw strings. The subject was looked up by
     * its normalized form, so a raw comparison answers a different question than the lookup did:
     * {@code deactivate("ada", "ADA")} resolves the same identity, and a raw check would let an
     * administrator whose session carries a differently-cased spelling of their own name deactivate
     * themselves — with only the last-active-administrator guard left to stop them.
     *
     * <p>A blank or unresolvable caller is not the subject. It cannot be: a name that does not
     * normalize names nobody, so there is no identity for it to be equal to.
     */
    private static boolean isSelf(ScimUser user, String requestedBy) {
        return normalized(requestedBy)
                .map(name -> name.equals(user.profile().normalizedUserName()))
                .orElse(false);
    }

    /**
     * The stable id behind the administrator's userName, for the event's actor reference.
     *
     * <p>{@code null} when the name resolves to no identity, which is not a case worth refusing the
     * operation over: the caller is an authenticated administrator whose own row could have been
     * renamed between authentication and this call, and an event recorded with no actor is more use
     * than no event at all. What it never becomes is the userName itself.
     *
     * <p>A name that cannot be normalized at all — null, or blank — is the same answer rather than
     * an exception. It reaches here only if the web adapter's own guarantees were bypassed, and
     * failing the whole administrative operation with a {@code 500} over an unidentifiable ACTOR
     * would be a worse answer than recording the change with no actor named.
     */
    private UUID actorId(String requestedBy) {
        return normalized(requestedBy)
                .flatMap(users::findByNormalizedUserName)
                .map(ScimUser::id)
                .orElse(null);
    }

    /**
     * The submitted name in the form lookups and comparisons use, or empty when it has no such form.
     *
     * <p>Normalization throws on a blank value, and every caller here wants "then it names nobody"
     * rather than a propagated failure — so the conversion happens once, in one place, instead of at
     * three call sites that could each get the guard wrong.
     */
    private static Optional<NormalizedUserName> normalized(String userName) {
        if (userName == null || userName.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(NormalizedUserName.of(userName));
    }

    /**
     * Writes {@code active} through the port, which advances the version because {@code active} is
     * a SCIM attribute a connector reads.
     *
     * <p>Skips the write when the value already stands, so a repeated deactivation does not move
     * the ETag of a resource whose representation did not change.
     */
    private IdentitySummary applyActive(
            ScimUser user, boolean shouldBeActive, Optional<ScimGroup> adminGroup) {
        if (user.profile().active() == shouldBeActive) {
            return summarize(user, isAdmin(adminGroup, user));
        }
        ScimUser updated = users.updateActive(user.id(), shouldBeActive, clock.instant())
                .orElseThrow(UnknownIdentityException::new);
        return summarize(updated, isAdmin(adminGroup, updated));
    }

    /**
     * Whether this identity is the only administrator that could still perform administrative work.
     * A deactivated admin cannot log in, so it does not count.
     *
     * <p>A <em>locked</em> one does count, even though a lock no longer ends on its own: the
     * deployment's recovery path does not depend on it, because the Bootstrap Admin can never be
     * locked and can unlock anyone. Excluding a locked admin here would refuse deactivations that
     * leave the deployment perfectly recoverable.
     *
     * <p>That argument holds only because the Bootstrap Admin is also undeactivatable —
     * {@link #deactivate} refuses it outright. Were it deactivatable, every other administrator
     * could be locked out permanently with no principal left to unlock them, and a locked admin
     * would have to count as unavailable here instead.
     *
     * <p>Answered from the Admin group's own membership rather than by scanning every identity,
     * which is what the derivation makes possible: the set of administrators is now a list this
     * directory holds, not a predicate over every row.
     */
    private boolean isLastActiveAdministrator(Optional<ScimGroup> adminGroup, ScimUser user) {
        if (!isAdmin(adminGroup, user) || !user.profile().active()) {
            return false;
        }
        return adminGroup.orElseThrow().members().stream()
                .filter(member -> !member.userId().equals(user.id()))
                .map(member -> users.findById(member.userId()))
                .flatMap(Optional::stream)
                .noneMatch(other -> other.profile().active());
    }

    private static boolean isAdmin(Optional<ScimGroup> adminGroup, ScimUser user) {
        return adminGroup.map(group -> group.hasMember(user.id())).orElse(false);
    }

    private ScimUser require(String userName) {
        return normalized(userName)
                .flatMap(users::findByNormalizedUserName)
                .orElseThrow(UnknownIdentityException::new);
    }

    /**
     * Records an administrative write that went through.
     *
     * <p>The record names the action and nothing else. It deliberately identifies neither the
     * identity acted on nor the administrator who acted: a {@code userName} is not something this
     * service writes to a log, and the stable ids belong in the audit trail, which records them
     * beside the operation. A log line says an administrative change happened and when, which is
     * what an operator watching for unexpected activity needs; the API response says which identity
     * to the caller who is entitled to know.
     */
    private static void succeeded(String action) {
        log.atInfo()
                .addKeyValue(LogEvent.ACTION, action)
                .addKeyValue(LogEvent.OUTCOME, LogEvent.SUCCESS)
                .log("Administrative identity change applied");
    }

    /**
     * Records a refused administrative write — to the log and to the audit trail — and returns the
     * exception to throw, so the refusal cannot be logged without being raised or raised without
     * being logged.
     *
     * <p>{@code reason} is a closed-set member, never the message: a message is written for a human
     * and is free to grow a submitted value in it later.
     */
    private UnsafeIdentityChangeException refuse(
            String action,
            AuditAdministrativeRefusal reason,
            UUID actorId,
            UUID subjectId,
            String message) {
        log.atWarn()
                .addKeyValue(LogEvent.ACTION, action)
                .addKeyValue(LogEvent.OUTCOME, LogEvent.FAILURE)
                .addKeyValue(LogEvent.REASON, reason.name())
                .log("Administrative identity change refused");
        audit.recordAdministrativeChangeRefused(actorId, subjectId, reason);
        return new UnsafeIdentityChangeException(message);
    }

    private static IdentitySummary summarize(ScimUser user, boolean admin) {
        return new IdentitySummary(
                user.id(),
                user.profile().userName(),
                admin,
                user.profile().active(),
                user.login().isLocked(),
                user.login().hasPassword(),
                user.createdAt());
    }
}
