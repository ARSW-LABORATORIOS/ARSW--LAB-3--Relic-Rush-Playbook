package edu.eci.arsw.relicrush.concurrency;

import edu.eci.arsw.relicrush.model.ForgeStation;

/**
 * Deterministic lock ordering (by station id) prevents the circular wait
 * that caused the deadlock in the starter. Fine-grained locking is
 * preserved: forge operations on disjoint stations can still run in
 * parallel, nothing is serialized behind a single global lock.
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
        ForgeStation lo = first.id() < second.id() ? first : second;
        ForgeStation hi = first.id() < second.id() ? second : first;

        String who = Thread.currentThread().getName();
        notify(who, lo.name(), StationActivityListener.EventType.WAITING);
        synchronized (lo) {
            notify(who, lo.name(), StationActivityListener.EventType.ACQUIRED);
            notify(who, hi.name(), StationActivityListener.EventType.WAITING);
            synchronized (hi) {
                notify(who, hi.name(), StationActivityListener.EventType.ACQUIRED);
                try {
                    action.run();
                } finally {
                    notify(who, hi.name(), StationActivityListener.EventType.RELEASED);
                }
            }
            notify(who, lo.name(), StationActivityListener.EventType.RELEASED);
        }
    }

    private static void notify(String who, String stationName, StationActivityListener.EventType type) {
        StationActivityListener current = listener;
        if (current != null) {
            current.onStationEvent(who, stationName, type);
        }
    }
}
