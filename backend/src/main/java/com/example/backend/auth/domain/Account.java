package com.example.backend.auth.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A login account as understood by authentication, independent of persistence.
 *
 * <p>Besides its credentials an account carries two unrelated kinds of state.
 * Its recent login history — how many consecutive failures have been recorded,
 * and, once the policy's limit is reached, the instant it was locked — is
 * behaviour here rather than in the caller, so "what counts as locked" has one
 * answer that a unit test can reach. Its administrative profile — whether it is
 * enabled, when it was created — is plain data that only account administration
 * reads.
 *
 * <p>The two are deliberately separate. A lockout is imposed by the failure run
 * and lifted only by an administrator's Unlock; {@code enabled} is a standing
 * decision an administrator takes directly. Collapsing them would make one of
 * those two behaviours unexpressible.
 *
 * <p>{@code lockedAt} records <em>when</em> the lock was imposed and nothing
 * about when it ends, because it does not end on its own: there is no duration,
 * no expiry and no clock in the decision. That is what makes {@link #isLocked()}
 * a question about the row alone — a lock cannot be "in the past", so no caller
 * has to agree with the server about the time to agree about the state.
 *
 * <p>{@code id} is the account's stable, non-reassignable identity: assigned once
 * at creation and carried unchanged through every transition below — no
 * {@code with...} method takes or produces a different one. {@code username} is
 * a mutable display and login attribute only; anything this application owns
 * that must keep pointing at the same account after a rename (session indexing,
 * the counter feature) is keyed by {@code id}, never by {@code username}.
 *
 * <p>{@code passwordHash} is the reason no caller outside this slice receives an
 * {@code Account}: the administrative listing is served as
 * {@link com.example.backend.auth.application.AccountSummary}, which has no
 * field to leak it into. It is nullable: a credentialless account — one an
 * administrator has created or reset without setting a password — carries no
 * hash at all, and is refused at login like a wrong-password attempt rather
 * than being unable to exist.
 *
 * <p>{@code createdAt} is nullable for one reason only: a row written before the
 * column existed has no value for it, and inventing one would be worse than
 * reporting that none is recorded. Startup seeding backfills what it can (see
 * {@link com.example.backend.auth.application.AccountService#seedDefaults}).
 */
public record Account(
        UUID id,
        String username,
        String passwordHash,
        AccountRole role,
        int failedLoginAttempts,
        Instant lockedAt,
        boolean enabled,
        Instant createdAt) {

    /**
     * A newly registered account: a freshly generated stable id, nothing failed
     * yet, nothing locked, and no creation timestamp recorded. Kept because most
     * callers — every test of the lockout rule among them — have no interest in
     * the profile fields, and spelling out eight arguments there would bury what
     * each case is actually about.
     */
    public Account(String username, String passwordHash, AccountRole role) {
        this(UUID.randomUUID(), username, passwordHash, role, 0, null, true, null);
    }

    /**
     * An account with a login history and no creation timestamp recorded — the
     * shape the lockout rule reasons about.
     */
    public Account(
            String username,
            String passwordHash,
            AccountRole role,
            int failedLoginAttempts,
            Instant lockedAt) {
        this(
                UUID.randomUUID(),
                username,
                passwordHash,
                role,
                failedLoginAttempts,
                lockedAt,
                true,
                null);
    }

    /**
     * An account with every profile field spelled out and a freshly generated
     * stable id. Kept for tests that need to control {@code enabled} or
     * {@code createdAt} directly without wiring up an existing account and
     * calling a {@code with...} transition on it.
     */
    public Account(
            String username,
            String passwordHash,
            AccountRole role,
            int failedLoginAttempts,
            Instant lockedAt,
            boolean enabled,
            Instant createdAt) {
        this(
                UUID.randomUUID(),
                username,
                passwordHash,
                role,
                failedLoginAttempts,
                lockedAt,
                enabled,
                createdAt);
    }

    /**
     * Whether the account is closed to logins. A lock stands until an
     * administrator lifts it, so the recorded instant being present <em>is</em>
     * the state: nothing has to be compared to a clock, and no passage of time
     * changes the answer.
     */
    public boolean isLocked() {
        return lockedAt != null;
    }

    /**
     * The account as it stands after one rejected login attempt.
     *
     * <p>A locked account is returned unchanged: the lock is already in force, so
     * attempts made against it neither count nor deepen it. Otherwise the failure
     * is counted, and reaching the policy's limit locks the account as of
     * {@code now} — permanently, in the sense that no later call here and no
     * elapsed time lifts it. Only {@link #withLockoutCleared()} does.
     */
    public Account withFailureRecorded(LockoutPolicy policy, Instant now) {
        if (isLocked()) {
            return this;
        }
        int attempts = failedLoginAttempts + 1;
        return new Account(
                id,
                username,
                passwordHash,
                role,
                attempts,
                attempts >= policy.maxAttempts() ? now : null,
                enabled,
                createdAt);
    }

    /**
     * The account as it stands after one rejected login attempt that must never
     * lock it: the failure run lengthens and no lock is imposed, whatever the
     * policy's limit says.
     *
     * <p>This is the Bootstrap Admin's path, and the reason it is a transition of
     * its own rather than a flag on {@link #withFailureRecorded}: with no
     * automatic lift, a locked recovery identity is an unrecoverable deployment,
     * so "counted but never locked" is a distinct rule and is named as one. The
     * run is still counted because the failures are still evidence — the audit
     * trail records each of them either way.
     */
    public Account withFailureCounted() {
        return new Account(
                id,
                username,
                passwordHash,
                role,
                failedLoginAttempts + 1,
                lockedAt,
                enabled,
                createdAt);
    }

    /**
     * The account as it stands after a login it accepted: the failure run is
     * over, so the count returns to zero. An account with nothing to clear is
     * returned as-is, so a caller can use the identity of the result to avoid a
     * pointless write.
     *
     * <p>A locked account never reaches here — it is refused before its password
     * is compared — so this clears a run rather than a lock in practice; it
     * clears both for the same reason {@link #withLockoutCleared()} does, namely
     * that "no failures recorded, no lock standing" is one state.
     */
    public Account withSuccessfulLogin() {
        return withFailureRunCleared();
    }

    /**
     * The account with a creation timestamp filled in where it had none. Startup
     * seeding's backfill for a row that predates that column; anything already
     * recorded is left alone, credentials and login history included.
     */
    public Account withCreatedAtBackfilled(Instant fallbackCreatedAt) {
        if (createdAt != null) {
            return this;
        }
        return new Account(
                id,
                username,
                passwordHash,
                role,
                failedLoginAttempts,
                lockedAt,
                enabled,
                fallbackCreatedAt);
    }

    /**
     * The account with its administrative standing changed, and nothing else.
     *
     * <p>Notably not the lockout: enabling an account does not unlock it, and
     * disabling one does not clear its failure run. The two are separate
     * capabilities because they answer different questions — whether an account
     * is permitted at all, and whether it is being penalised for failed logins —
     * and an administrator restoring access after a suspension is not thereby
     * deciding that a run of failed logins did not happen.
     *
     * <p>Returned unchanged when it already stands this way, so a caller can use
     * the identity of the result to avoid a pointless write.
     */
    public Account withEnabled(boolean shouldBeEnabled) {
        if (enabled == shouldBeEnabled) {
            return this;
        }
        return new Account(
                id,
                username,
                passwordHash,
                role,
                failedLoginAttempts,
                lockedAt,
                shouldBeEnabled,
                createdAt);
    }

    /**
     * The account with its lockout lifted: the recorded instant is discarded and
     * the failure run ends with it, exactly as an accepted login would leave it.
     *
     * <p>This is the <em>only</em> way a lockout ends. Left alone a lock stands
     * indefinitely, so an administrator's Unlock is not an early release from a
     * penalty that would have expired — it is the whole mechanism. It says
     * nothing about whether the account is enabled.
     */
    public Account withLockoutCleared() {
        return withFailureRunCleared();
    }

    /**
     * The single implementation of "no failures recorded, no lockout standing".
     * Two callers reach it for unrelated reasons — an accepted login and an
     * administrator lifting a lockout — and naming it after neither is what keeps
     * the other from reading as a side effect of the first.
     */
    private Account withFailureRunCleared() {
        if (failedLoginAttempts == 0 && lockedAt == null) {
            return this;
        }
        return new Account(id, username, passwordHash, role, 0, null, enabled, createdAt);
    }
}
