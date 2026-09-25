package com.example.backend.auth.infrastructure.transaction;

import com.example.backend.auth.application.AfterCommit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Outbound adapter for {@link AfterCommit}, backed by Spring's transaction
 * synchronization registry.
 *
 * <p>Registers the work as a synchronization on the transaction bound to the
 * calling thread, so it runs after that transaction commits and not at all if it
 * rolls back.
 *
 * <p>With no transaction bound, the work runs immediately. That is the honest
 * reading of the contract rather than a fallback that hides a mistake: without a
 * transaction there is no commit to wait for and nothing that could roll the
 * caller's write back, so "after the commit" and "now" are the same instant.
 */
@Component
public class AfterCommitAdapter implements AfterCommit {

    @Override
    public void run(Runnable work) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            work.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        work.run();
                    }
                });
    }
}
