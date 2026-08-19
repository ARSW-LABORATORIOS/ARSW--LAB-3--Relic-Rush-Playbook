package edu.eci.arsw.relicrush.concurrency;

/**
 * Pause/resume switch for the UI bonus. Same pattern as the Warehouse Lab's
 * SimulationControl: paused adventurers block on wait() instead of spinning,
 * and resume() wakes everyone with notifyAll().
 */
public final class SimulationControl {
    private boolean paused;

    public synchronized void pause() {
        paused = true;
    }

    public synchronized void resume() {
        paused = false;
        notifyAll();
    }

    public synchronized void awaitIfPaused() throws InterruptedException {
        while (paused) {
            wait();
        }
    }

    public synchronized boolean isPaused() {
        return paused;
    }
}
