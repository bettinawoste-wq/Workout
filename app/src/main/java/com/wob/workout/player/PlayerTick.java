package com.wob.workout.player;

/**
 * Changes every second — this is what updates the timer text and triggers the beep sound.
 * Nothing here touches media. Carries {@code phase} so the Fragment can format the timer
 * text (mm:ss vs. "Start in X" vs. "Ende in X") without needing to remember the last
 * {@link PlayerExerciseState} it saw — one less implicit ordering assumption between the
 * two LiveData streams. See wayfinder ticket #4.
 */
public class PlayerTick {

    public final long remainingMillis;
    public final boolean running;
    public final boolean shouldBeep;
    public final PlayerExerciseState.Phase phase;

    public PlayerTick(long remainingMillis, boolean running, boolean shouldBeep,
                       PlayerExerciseState.Phase phase) {
        this.remainingMillis = remainingMillis;
        this.running = running;
        this.shouldBeep = shouldBeep;
        this.phase = phase;
    }
}
