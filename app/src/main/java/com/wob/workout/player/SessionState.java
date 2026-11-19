package com.wob.workout.player;

import com.wob.workout.data.WorkoutExercise;

/**
 * What is going on in a workout session at one moment, as {@link WorkoutSessionEngine}
 * reports it: not how it looks, which {@link PlayerScreen} decides.
 */
public final class SessionState {

    /** FINISHED follows the last exercise directly: its own last seconds are the countdown. */
    public enum Phase {
        /** The countdown before the first exercise. */
        READY,
        ACTIVE,
        /** The transition between two exercises. */
        TRANSITION,
        /** A rest between two exercises, in place of the transition. */
        REST,
        FINISHED
    }

    public final Phase phase;
    /** Counts the phases entered: a new value whenever the screen moves to another phase or
     *  exercise, the same while only the time moves. */
    public final int scene;
    /** Where the exercise shown or previewed stands in the workout, from 0. */
    public final int position;
    public final int total;
    /** The exercise under way, or the one a countdown, transition or rest leads to; null
     *  once finished. Never a rest. */
    public final WorkoutExercise workoutExercise;
    /** What is left of the countdown, transition or exercise. */
    public final long millisLeft;
    public final boolean running;
    public final boolean canStepBack;
    public final boolean canStepForward;
    public final boolean canPause;
    /** What is left of the exercise time of the whole workout. */
    public final long workoutRemainingMillis;
    public final long workoutTotalMillis;
    /** Only once finished. */
    public final PlayerWorkoutSummary summary;

    SessionState(Phase phase, int scene, int position, int total,
                 WorkoutExercise workoutExercise, long millisLeft, boolean running,
                 boolean canStepBack, boolean canStepForward, boolean canPause,
                 long workoutRemainingMillis, long workoutTotalMillis,
                 PlayerWorkoutSummary summary) {
        this.phase = phase;
        this.scene = scene;
        this.position = position;
        this.total = total;
        this.workoutExercise = workoutExercise;
        this.millisLeft = millisLeft;
        this.running = running;
        this.canStepBack = canStepBack;
        this.canStepForward = canStepForward;
        this.canPause = canPause;
        this.workoutRemainingMillis = workoutRemainingMillis;
        this.workoutTotalMillis = workoutTotalMillis;
        this.summary = summary;
    }

    /** @return how far through the workout's exercise time this is, from 0 to 1000 */
    public int workoutPermilleDone() {
        if (workoutTotalMillis <= 0) return 1000;
        return (int) (1000 * (workoutTotalMillis - workoutRemainingMillis) / workoutTotalMillis);
    }
}
