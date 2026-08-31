package com.anshun.dms.common;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Small bridge for side effects that must happen only after a database transaction reaches a terminal state. */
public final class TransactionCallbacks {
    private TransactionCallbacks() { }

    public static void afterCommit(Runnable callback) {
        if (synchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { callback.run(); }
            });
        } else {
            callback.run();
        }
    }

    public static void afterRollback(Runnable callback) {
        if (!synchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) callback.run();
            }
        });
    }

    private static boolean synchronizationActive() {
        return TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive();
    }
}
