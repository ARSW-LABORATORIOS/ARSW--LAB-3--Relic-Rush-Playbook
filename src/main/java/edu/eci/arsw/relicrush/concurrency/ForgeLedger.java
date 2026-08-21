package edu.eci.arsw.relicrush.concurrency;

import edu.eci.arsw.relicrush.model.ForgeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Global match ledger.
 *
 * Synchronized so the counter increment and the list write happen as one
 * atomic operation, without locking the rest of the game.
 */
public final class ForgeLedger {
    private int totalCrafted = 0;
    private final List<ForgeEvent> events = new ArrayList<>();

    public synchronized void record(ForgeEvent event) {
        totalCrafted++;
        events.add(event);
    }

    public synchronized int totalCrafted() {
        return totalCrafted;
    }

    public synchronized int eventCount() {
        return events.size();
    }

    public synchronized List<ForgeEvent> snapshot() {
        return List.copyOf(events);
    }
}
