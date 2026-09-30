package com.example.backend.auth.application;

import com.example.backend.audit.domain.AuditTrail;
import com.example.backend.auth.domain.ScheduledJob;
import com.example.backend.auth.domain.ScheduledJobLock;
import com.example.backend.observability.LogEvent;
import com.example.backend.scim.domain.PasswordChangeGracePolicy;
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
 * The grace-period job: deactivates every User that has left a required password change unmade for
 * longer than the configured window.
 *
 * <p>Shaped exactly as {@link InactivityDeactivationService} is, and for the same reasons: its own
 * lock ({@link ScheduledJob#PASSWORD_CHANGE_GRACE_DEACTIVATION}), so a second run of THIS job skips
 * and neither dormancy job waits on it; each candidate re-read under its resource lock and decided
 * again, so a User that completed the change in between is left alone; one transaction per run, so
 * a deactivation the trail cannot record does not happen; and sessions ended after the commit.
 *
 * <p>The Bootstrap Admin is never processed: the candidate query excludes reserved Users, and the
 * exemption is checked again on the locked read. Deactivating the recovery identity is the outcome
 * the flag exists to avoid.
 */
@Service
public class PasswordChangeGraceService {

    /** {@code event.action} on every record this job emits, its schedule included. */
    public static final String ACTION = "identity.password_change_grace_deactivation";

    private static final Logger log = LoggerFactory.getLogger(PasswordChangeGraceService.class);

    private final ScimUserRepository users;
    private final ScimUserSessions sessions;
    private final ScheduledJobLock lock;
    private final PasswordChangeGracePolicy policy;
    private final AuditTrail audit;
    private final Clock clock;

    public PasswordChangeGraceService(
            ScimUserRepository users,
            ScimUserSessions sessions,
            ScheduledJobLock lock,
            PasswordChangeGracePolicy policy,
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
     * One run: deactivates every active, unreserved User whose required change is overdue, or skips
     * when another run of this job holds its lock.
     */
    @Transactional
    public DormancyRun deactivateOverdueUsers() {
        if (!lock.tryAcquire(ScheduledJob.PASSWORD_CHANGE_GRACE_DEACTIVATION)) {
            logRun(true, 0);
            return DormancyRun.skippedRun();
        }
        Instant now = clock.instant();
        Instant cutoff = policy.cutoff(now);
        List<UUID> deactivated = new ArrayList<>();
        for (UUID candidate : users.findPasswordChangeOverdueActiveUserIds(cutoff)) {
            Optional<ScimUser> locked = users.findByIdForUpdate(candidate);
            if (locked.isEmpty() || !isStillDue(locked.get(), cutoff)) {
                continue;
            }
            users.updateActive(candidate, false, now);
            audit.recordPasswordChangeGraceDeactivation(candidate);
            sessions.revokeAfterCommit(
                    null, candidate, EnumSet.of(ScimUserSessions.Cause.DEACTIVATED));
            deactivated.add(candidate);
        }
        logRun(false, deactivated.size());
        return new DormancyRun(false, deactivated);
    }

    /** Still active, still overdue, and not the Bootstrap Admin — decided on the locked read. */
    private static boolean isStillDue(ScimUser user, Instant cutoff) {
        return user.profile().active()
                && !user.isExemptFromPasswordChangeGrace()
                && user.login().isPasswordChangeOverdueAt(cutoff);
    }

    /** Reports the run, a skipped or empty one included. Names no identity. */
    private void logRun(boolean skipped, int processed) {
        log.atInfo()
                .addKeyValue(LogEvent.ACTION, ACTION)
                .addKeyValue(LogEvent.OUTCOME, LogEvent.SUCCESS)
                .addKeyValue(LogEvent.DORMANCY_WINDOW, policy.window().toString())
                .addKeyValue(LogEvent.DORMANCY_SKIPPED, skipped)
                .addKeyValue(LogEvent.DORMANCY_PROCESSED, processed)
                .log("Change-grace deactivation run complete");
    }
}
