package com.example.backend.auth.application;

import com.example.backend.audit.domain.AuditTrail;
import com.example.backend.auth.domain.ScheduledJob;
import com.example.backend.auth.domain.ScheduledJobLock;
import com.example.backend.observability.LogEvent;
import com.example.backend.observability.LogEvent.Category;
import com.example.backend.observability.LogEvent.Operation;
import com.example.backend.observability.LogEvent.Type;
import com.example.backend.scim.domain.DormancyPolicy;
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
 * The inactivity job: deactivates every User that has gone longer than the configured window
 * without authenticating.
 *
 * <p>For each dormant User it does what an Admin's deactivation does, with the scheduled job as
 * the actor: {@code active=false} through the narrow port write, which advances the SCIM version;
 * an {@code INACTIVITY_DEACTIVATION} event; and the User's sessions ended after the commit (ADR
 * 0002), recorded as their own event.
 *
 * <h2>What stops a User being processed twice</h2>
 *
 * <ul>
 *   <li>The job's lock ({@link ScheduledJobLock}) — a second run of this job skips while one is in
 *       progress, on any instance. It is this job's lock only, so the dormant-authority job runs
 *       alongside it.
 *   <li>The User's own resource lock and a second look. Candidates are re-read under
 *       {@link ScimUserRepository#findByIdForUpdate} and decided again, so a User that logged in,
 *       was reactivated or was already deactivated since the candidate query is left alone — the
 *       same serialization point a conditional SCIM write uses.
 * </ul>
 *
 * <h2>The connector does not get the last word</h2>
 *
 * <p>A connector re-asserting {@code active=true} on an active User changes nothing — the SCIM
 * write computes no difference and writes nothing — so it does not reset the window, and the next
 * run deactivates a dormant User regardless. Only a stored transition from inactive to active
 * resets it (see {@link ScimUserRepository#updateActive}); a reactivated User still dormant by a
 * later run is deactivated again, which is the intended outcome rather than a loop.
 *
 * <p>The Bootstrap Admin is never processed: the candidate query excludes reserved Users, and the
 * exemption is checked again on the locked read.
 *
 * <p>One transaction per run. A failure on one User — an audit append that cannot commit, most of
 * all — rolls the whole run back and revokes nothing; the next run repeats it. That is the
 * fail-closed half of ADR 0004, and it keeps "deactivated but not recorded" impossible.
 */
@Service
public class InactivityDeactivationService {

    /** The operation every record this job emits is classified as, its schedule included. */
    public static final Operation OPERATION = Operation.INACTIVITY_DEACTIVATION;

    private static final Logger log = LoggerFactory.getLogger(InactivityDeactivationService.class);

    private final ScimUserRepository users;
    private final ScimUserSessions sessions;
    private final ScheduledJobLock lock;
    private final DormancyPolicy policy;
    private final AuditTrail audit;
    private final Clock clock;

    public InactivityDeactivationService(
            ScimUserRepository users,
            ScimUserSessions sessions,
            ScheduledJobLock lock,
            DormancyPolicy policy,
            AuditTrail audit,
            Clock clock) {
        this.users = users;
        this.sessions = sessions;
        this.lock = lock;
        this.policy = policy;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * One run: deactivates every dormant, active, unreserved User, or skips when another run of
     * this job holds its lock.
     */
    @Transactional
    public DormancyRun deactivateDormantUsers() {
        if (!lock.tryAcquire(ScheduledJob.INACTIVITY_DEACTIVATION)) {
            logRun(true, 0);
            return DormancyRun.skippedRun();
        }
        Instant now = clock.instant();
        Instant cutoff = policy.deactivationCutoff(now);
        List<UUID> deactivated = new ArrayList<>();
        for (UUID candidate : users.findDormantActiveUserIds(cutoff)) {
            Optional<ScimUser> locked = users.findByIdForUpdate(candidate);
            if (locked.isEmpty() || !isStillDue(locked.get(), cutoff)) {
                continue;
            }
            users.updateActive(candidate, false, now);
            audit.recordInactivityDeactivation(candidate);
            sessions.revokeAfterCommit(
                    null, candidate, EnumSet.of(ScimUserSessions.Cause.DEACTIVATED));
            deactivated.add(candidate);
        }
        logRun(false, deactivated.size());
        return new DormancyRun(false, deactivated);
    }

    /**
     * The decision, taken again on the locked read: still active, still dormant, and not the
     * Bootstrap Admin.
     */
    private static boolean isStillDue(ScimUser user, Instant cutoff) {
        return user.profile().active() && !user.isExemptFromDormancy() && user.isDormantAt(cutoff);
    }

    /**
     * Reports the run — including a skipped one and one that changed nothing, because "nobody was
     * dormant" and "the job never ran" must be told apart, as the retention job's log does. Names
     * no identity: the stable ids are the audit trail's to carry.
     */
    private void logRun(boolean skipped, int processed) {
        LogEvent.classify(log.atInfo(), OPERATION, Category.BATCH, Type.JOB_END)
                .addKeyValue(LogEvent.OUTCOME, LogEvent.SUCCESS)
                .addKeyValue(LogEvent.DORMANCY_WINDOW, policy.deactivationWindow().toString())
                .addKeyValue(LogEvent.DORMANCY_SKIPPED, skipped)
                .addKeyValue(LogEvent.DORMANCY_PROCESSED, processed)
                .log("Inactivity deactivation run complete");
    }
}
