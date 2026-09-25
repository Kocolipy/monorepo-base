package com.example.backend.auth.infrastructure.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

/**
 * The adapter against Spring's real synchronization registry, driven without a
 * database: the registry is thread-bound, so a test can open synchronization,
 * hand work over, and trigger the commit or the rollback itself.
 *
 * <p>Worth testing directly because the whole value of the port is an ordering,
 * and an adapter that ran the work inline would satisfy every caller's compile
 * and silently restore the bug it exists to prevent.
 */
class AfterCommitAdapterTests {

    private final AfterCommitAdapter adapter = new AfterCommitAdapter();

    private final List<String> done = new ArrayList<>();

    @AfterEach
    void clearAnyTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void holdsTheWorkUntilTheTransactionCommits() {
        TransactionSynchronizationManager.initSynchronization();

        adapter.run(() -> done.add("revoked"));

        assertThat(done).isEmpty();

        TransactionSynchronizationUtils.triggerAfterCommit();

        assertThat(done).containsExactly("revoked");
    }

    /** A rollback is the case the ordering exists for: the work must never run. */
    @Test
    void neverRunsTheWorkWhenTheTransactionDoesNotCommit() {
        TransactionSynchronizationManager.initSynchronization();

        adapter.run(() -> done.add("revoked"));
        TransactionSynchronizationUtils.triggerAfterCompletion(
                TransactionSynchronization.STATUS_ROLLED_BACK);

        assertThat(done).isEmpty();
    }

    /**
     * With no transaction bound there is no commit to wait for and no rollback that
     * could contradict the work, so deferring it would only mean never running it.
     */
    @Test
    void runsTheWorkImmediatelyWithNoTransaction() {
        adapter.run(() -> done.add("revoked"));

        assertThat(done).containsExactly("revoked");
    }

    @Test
    void keepsTheOrderWorkWasHandedOverIn() {
        TransactionSynchronizationManager.initSynchronization();

        adapter.run(() -> done.add("first"));
        adapter.run(() -> done.add("second"));
        TransactionSynchronizationUtils.triggerAfterCommit();

        assertThat(done).containsExactly("first", "second");
    }
}
