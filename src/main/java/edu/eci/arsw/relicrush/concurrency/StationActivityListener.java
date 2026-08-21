package edu.eci.arsw.relicrush.concurrency;

/**
 * Optional hook so the UI bonus can show, in real time, which adventurer
 * holds each station and who is waiting on one. LockPair calls this when it
 * exists; if nobody registered a listener, nothing changes for the console
 * game.
 */
public interface StationActivityListener {

    enum EventType { WAITING, ACQUIRED, RELEASED }

    void onStationEvent(String adventurerName, String stationName, EventType type);
}
