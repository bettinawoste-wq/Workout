package com.wob.workout.player;

/**
 * Changes every second — this is what updates the timer text and triggers the beep sound.
 * Nothing here touches media. See wayfinder ticket #4.
 */
public class PlayerTick {

    public final long remainingMillis;
    public final boolean running;
    public final boolean shouldBeep;

    public PlayerTick(long remainingMillis, boolean running, boolean shouldBeep) {
        this.remainingMillis = remainingMillis;
        this.running = running;
        this.shouldBeep = shouldBeep;
    }
}
