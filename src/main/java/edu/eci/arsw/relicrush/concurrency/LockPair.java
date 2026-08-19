package edu.eci.arsw.relicrush.concurrency;

import edu.eci.arsw.relicrush.model.ForgeStation;

/**
 * Starter implementation intentionally contains a deadlock risk.
 *
 * Students: do NOT replace this with one global lock. Preserve concurrency
 * between forge operations that use disjoint stations.
 */
public final class LockPair {

    private static volatile StationActivityListener listener;

    private LockPair() {
    }

    /**
     * Optional hook for the UI bonus: lets a listener show, in real time,
     * which adventurer holds or is waiting on each station. Does not change
     * the locking strategy below in any way.
     */
    public static void setListener(StationActivityListener newListener) {
        listener = newListener;
    }

    public static void withBoth(ForgeStation first, ForgeStation second, Runnable action) {
        // TODO LAB 3: This acquisition strategy can create circular wait.
        // Fix it using a deterministic ordering strategy (or justify another
        // deadlock-prevention approach) while preserving fine-grained locking.
        String who = Thread.currentThread().getName();
        notify(who, first.name(), StationActivityListener.EventType.WAITING);
        synchronized (first) {
            notify(who, first.name(), StationActivityListener.EventType.ACQUIRED);
            // This small delay makes the deadlock easier to reproduce in the starter.
            sleepQuietly(2);
            notify(who, second.name(), StationActivityListener.EventType.WAITING);
            synchronized (second) {
                notify(who, second.name(), StationActivityListener.EventType.ACQUIRED);
                try {
                    action.run();
                } finally {
                    notify(who, second.name(), StationActivityListener.EventType.RELEASED);
                }
            }
            notify(who, first.name(), StationActivityListener.EventType.RELEASED);
        }
    }

    private static void notify(String who, String stationName, StationActivityListener.EventType type) {
        StationActivityListener current = listener;
        if (current != null) {
            current.onStationEvent(who, stationName, type);
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
