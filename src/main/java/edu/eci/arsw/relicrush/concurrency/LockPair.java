package edu.eci.arsw.relicrush.concurrency;

import edu.eci.arsw.relicrush.model.ForgeStation;

/**
 * Starter implementation intentionally contains a deadlock risk.
 *
 * Students: do NOT replace this with one global lock. Preserve concurrency
 * between forge operations that use disjoint stations.
 */
public final class LockPair {

    private LockPair() {
    }

    public static void withBoth(ForgeStation first, ForgeStation second, Runnable action) {
        ForgeStation lo = first.id() < second.id() ? first : second;
        ForgeStation hi = first.id() < second.id() ? second : first;

        synchronized (lo) {
            synchronized (hi) {
                action.run();
            }
        }
    }
}
