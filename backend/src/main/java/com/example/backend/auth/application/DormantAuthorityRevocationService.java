package com.example.backend.auth.application;

import com.example.backend.audit.domain.AuditTrail;
import com.example.backend.scheduling.domain.ScheduledJob;
import com.example.backend.scheduling.domain.ScheduledJobLock;
import com.example.backend.observability.LogEvent;
import com.example.backend.observability.LogEvent.Category;
import com.example.backend.observability.LogEvent.Operation;
import com.example.backend.observability.LogEvent.Type;
import com.example.backend.scim.domain.DormancyPolicy;
import com.example.backend.scim.domain.ReservedResourceName;
import com.example.backend.scim.domain.ScimGroup;
import com.example.backend.scim.domain.ScimGroupRepository;
import com.example.backend.scim.domain.ScimUser;
import com.example.backend.scim.domain.ScimUserRepository;
import com.example.backend.scim.domain.ScimUserSessions;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The dormant-authority job: removes the direct Admin-group membership of every User that has gone
 * longer than the configured window without authenticating.
 *
 * <p>Authority only. The User keeps baseline access — that follows from being active, and whether
 * it is active is the inactivity job's business — and keeps every ordinary Group membership,
 * because ordinary Groups confer no authority in this model. The removal is the narrow
 * {@link ScimGroupRepository#removeMember}, which advances the Admin group's and the User's
 * versions exactly as a connector-driven removal does; the User's sessions end after the commit,
 * so a session issued with the Superuser Role's Permissions does not outlive the authority.
 *
 * <h2>Serialization</h2>
 *
 * <p>This job's own lock ({@link ScheduledJob#DORMANT_AUTHORITY_REVOCATION}) keeps two runs of it
 * apart and never blocks the inactivity job, which holds a different one. Inside a run the Admin
 * group's resource row is locked before any membership moves, so a connector's conditional write
 * to the Group serializes with the job instead of racing it; each User is then re-read under its
 * own lock and decided again.
 *
 * <p>The job takes priority over the directory: a connector may re-add the membership, and while
 * the User stays dormant the next run removes it again. The Bootstrap Admin is never processed —
 * excluded from the candidate query and checked again on the locked read — so its membership, the
 * deployment's recovery authority, is not something this job can touch.
 */
@Service
public class DormantAuthorityRevocationService {

    /** The operation every record this job emits is classified as, its schedule included. */
    public static final Operation OPERATION = Operation.DORMANT_AUTHORITY_REVOCATION;

    private static final Logger log =
            LoggerFactory.getLogger(DormantAuthorityRevocationService.class);

    private final ScimUserRepository users;
    private final ScimGroupRepository groups;
    private final ScimUserSessions sessions;
    private final ScheduledJobLock lock;
    private final DormancyPolicy policy;
    private final AuditTrail audit;
    private final Clock clock;

    public DormantAuthorityRevocationService(
            ScimUserRepository users,
            ScimGroupRepository groups,
            ScimUserSessions sessions,
            ScheduledJobLock lock,
            DormancyPolicy policy,
            AuditTrail audit,
            Clock clock) {
        this.users = users;
        this.groups = groups;
        this.sessions = sessions;
        this.lock = lock;
        this.policy = policy;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * One run: removes the Admin-group membership of every dormant, unreserved member, or skips
     * when another run of this job holds its lock.
     */
    @Transactional
    public DormancyRun revokeDormantAuthority() {
        if (!lock.tryAcquire(ScheduledJob.DORMANT_AUTHORITY_REVOCATION)) {
            return DormancyRun.skippedRun();
        }
        Instant now = clock.instant();
        Instant cutoff = policy.authorityRevocationCutoff(now);
        List<UUID> candidates =
                groups.findDormantMemberIds(ReservedResourceName.ADMIN_GROUP, cutoff);
        List<UUID> revoked = new ArrayList<>();
        Optional<UUID> adminGroup = candidates.isEmpty() ? Optional.empty() : lockAdminGroup();
        if (adminGroup.isPresent()) {
            UUID groupId = adminGroup.get();
            for (UUID candidate : candidates) {
                Optional<ScimUser> locked = users.findByIdForUpdate(candidate);
                if (locked.isEmpty() || !isStillDue(locked.get(), cutoff)
                        || !groups.removeMember(groupId, candidate, now)) {
                    continue;
                }
                audit.recordDormantAuthorityRevocation(candidate);
                sessions.revokeAfterCommit(
                        null, candidate, EnumSet.of(ScimUserSessions.Cause.ROLE_REVOKED));
                revoked.add(candidate);
            }
        }
        logRun(revoked.size());
        return new DormancyRun(false, revoked);
    }

    /**
     * The Admin group's id, read under its resource lock — the same serialization point a
     * connector's conditional write to it takes. Empty before seeding has run, when there is no
     * authority to revoke.
     */
    private Optional<UUID> lockAdminGroup() {
        return groups.findByReservedName(ReservedResourceName.ADMIN_GROUP)
                .flatMap(group -> groups.findByIdForUpdate(group.id()))
                .map(ScimGroup::id);
    }

    /** The decision, taken again on the locked read: still dormant, and not the Bootstrap Admin. */
    private static boolean isStillDue(ScimUser user, Instant cutoff) {
        return !user.isExemptFromDormancy() && user.isDormantAt(cutoff);
    }

    /**
     * Reports what a run that did the work did, an empty one included; names no identity. A
     * skipped run is logged by {@code ScheduledJobMetrics.instrumentLocked}, as the run's
     * {@code lock-held} end.
     */
    private void logRun(int processed) {
        LogEvent.classify(log.atInfo(), OPERATION, Category.BATCH, Type.INFO)
                .addKeyValue(LogEvent.DORMANCY_WINDOW, policy.authorityRevocationWindow().toString())
                .addKeyValue(LogEvent.DORMANCY_PROCESSED, processed)
                .log("Dormant authority revocation run complete");
    }
}
