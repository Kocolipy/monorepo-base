package com.example.backend.auth.application;

import com.example.backend.audit.domain.AuditPasswordChangeRefusal;
import com.example.backend.audit.domain.AuditTrail;
import com.example.backend.observability.LogEvent;
import com.example.backend.scim.domain.PasswordPolicy;
import com.example.backend.scim.domain.ScimLoginState;
import com.example.backend.scim.domain.ScimPasswordHistoryRepository;
import com.example.backend.scim.domain.ScimUser;
import com.example.backend.scim.domain.ScimUserRepository;
import com.example.backend.scim.domain.ScimUserSessions;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The authenticated self-service password change: the one thing a User with a required change may
 * do besides logging out, and available to every other User too.
 *
 * <p>In order:
 *
 * <ol>
 *   <li>An inactive or locked User is refused before anything is compared — the lockout blocks this
 *       path exactly as it blocks Login, and only an Admin's Unlock lifts it.
 *   <li>The current password must verify. A wrong one is counted toward the same failure run as a
 *       rejected Login ({@link LoginAttemptService#recordPasswordChangeFailure}), so at the
 *       threshold the User locks and every session it holds ends.
 *   <li>The new password must satisfy {@link PasswordPolicy} and must not match the current
 *       password or one of the retained previous ones. A refusal names the rule, never a value.
 *   <li>Accepted: the new password is hashed, the flag is cleared, the version advances, the
 *       history records the new hash, a {@code PASSWORD_CHANGE} event is committed with the write,
 *       and every session of the User — the one that submitted this included — ends after the
 *       commit.
 * </ol>
 *
 * <h2>Why a refusal commits</h2>
 *
 * <p>The refusals must NOT roll back: a wrong current password's count has to commit for the
 * lockout to mean anything. So the use case is one transaction that the two refusal exceptions do
 * not roll back, and the User is read under its resource lock first, so a concurrent change or
 * lockout is decided against the state it left rather than a stale read.
 */
@Service
public class PasswordChangeService {

    private static final Logger log = LoggerFactory.getLogger(PasswordChangeService.class);

    private static final String ACTION = "identity.password_change";

    private final ScimUserRepository users;
    private final ScimPasswordHistoryRepository passwordHistory;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService attempts;
    private final ScimUserSessions sessions;
    private final AuditTrail audit;
    private final Clock clock;

    public PasswordChangeService(
            ScimUserRepository users,
            ScimPasswordHistoryRepository passwordHistory,
            PasswordEncoder passwordEncoder,
            LoginAttemptService attempts,
            ScimUserSessions sessions,
            AuditTrail audit,
            Clock clock) {
        this.users = users;
        this.passwordHistory = passwordHistory;
        this.passwordEncoder = passwordEncoder;
        this.attempts = attempts;
        this.sessions = sessions;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * Replaces the User's password with {@code newPassword}, given its current one.
     *
     * @param userId the caller's stable id, taken from its session — never from the request
     * @throws CurrentPasswordRejectedException when the User is gone, inactive or locked, or the
     *                                          current password did not verify
     * @throws PasswordPolicyViolationException when the new password breaks a rule
     */
    @Transactional(noRollbackFor = {
            CurrentPasswordRejectedException.class, PasswordPolicyViolationException.class})
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        ScimUser user = users.findByIdForUpdate(userId)
                .orElseThrow(CurrentPasswordRejectedException::new);
        refuseStanding(user);
        if (!user.login().hasPassword()
                || !passwordEncoder.matches(currentPassword, user.login().passwordHash())) {
            attempts.recordPasswordChangeFailure(user.id());
            refused(AuditPasswordChangeRefusal.BAD_CURRENT_PASSWORD);
            throw new CurrentPasswordRejectedException();
        }
        PasswordPolicy.violation(newPassword, user.profile().userName())
                .ifPresent(rule -> refusePolicy(user, rule));
        if (isReused(user, newPassword)) {
            refusePolicy(user, PasswordPolicy.Rule.REUSED);
        }

        Instant now = clock.instant();
        String passwordHash = passwordEncoder.encode(newPassword);
        // The current password verified, which ends a failure run as an accepted login does.
        ScimLoginState cleared = user.login().withFailureRunCleared();
        if (cleared != user.login()) {
            users.updateLoginState(userId, cleared);
        }
        users.completePasswordChange(userId, passwordHash, now)
                .orElseThrow(CurrentPasswordRejectedException::new);
        passwordHistory.record(userId, passwordHash, now);
        audit.recordPasswordChanged(userId);
        sessions.revokeAfterCommit(
                null, userId, EnumSet.of(ScimUserSessions.Cause.PASSWORD_CHANGED));
        log.atInfo()
                .addKeyValue(LogEvent.ACTION, ACTION)
                .addKeyValue(LogEvent.OUTCOME, LogEvent.SUCCESS)
                .log("Self-service change completed");
    }

    /** Refuses an inactive or locked User before anything is compared. */
    private void refuseStanding(ScimUser user) {
        if (!user.profile().active()) {
            audit.recordPasswordChangeRefused(user.id(), AuditPasswordChangeRefusal.ACCOUNT_DISABLED);
            refused(AuditPasswordChangeRefusal.ACCOUNT_DISABLED);
            throw new CurrentPasswordRejectedException();
        }
        if (user.login().isLocked()) {
            audit.recordPasswordChangeRefused(user.id(), AuditPasswordChangeRefusal.ACCOUNT_LOCKED);
            refused(AuditPasswordChangeRefusal.ACCOUNT_LOCKED);
            throw new CurrentPasswordRejectedException();
        }
    }

    /**
     * Whether the candidate matches the current credential or a remembered one — matched through
     * the encoder, one hash at a time, because a salted hash can only be compared that way.
     */
    private boolean isReused(ScimUser user, String candidate) {
        List<String> remembered = new ArrayList<>(passwordHistory.findRecentHashes(user.id()));
        remembered.add(user.login().passwordHash());
        return remembered.stream().anyMatch(hash -> passwordEncoder.matches(candidate, hash));
    }

    private void refusePolicy(ScimUser user, PasswordPolicy.Rule rule) {
        AuditPasswordChangeRefusal reason = switch (rule) {
            case TOO_SHORT -> AuditPasswordChangeRefusal.TOO_SHORT;
            case TOO_LONG -> AuditPasswordChangeRefusal.TOO_LONG;
            case CONTAINS_USER_NAME -> AuditPasswordChangeRefusal.CONTAINS_USER_NAME;
            case REUSED -> AuditPasswordChangeRefusal.REUSED;
        };
        audit.recordPasswordChangeRefused(user.id(), reason);
        refused(reason);
        throw new PasswordPolicyViolationException(rule);
    }

    /** Logs a refusal by its closed-set reason; no identity and no value is written. */
    private static void refused(AuditPasswordChangeRefusal reason) {
        log.atWarn()
                .addKeyValue(LogEvent.ACTION, ACTION)
                .addKeyValue(LogEvent.OUTCOME, LogEvent.FAILURE)
                .addKeyValue(LogEvent.REASON, reason.name())
                .log("Self-service change refused");
    }
}
