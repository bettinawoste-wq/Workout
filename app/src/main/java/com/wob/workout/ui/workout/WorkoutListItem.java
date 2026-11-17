package com.wob.workout.ui.workout;

import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.ui.ExerciseCount;
import com.wob.workout.ui.MinutesSeconds;

import java.util.List;

public class WorkoutListItem {

    public final Workout workout;
    public final int exerciseCount;
    public final int totalDurationSeconds;
    /** The workout's exercises in workout order; one used twice appears twice. */
    public final List<Exercise> exercises;

    public WorkoutListItem(
            Workout workout,
            int exerciseCount,
            int totalDurationSeconds,
            List<Exercise> exercises) {

        this.workout = workout;
        this.exerciseCount = exerciseCount;
        this.totalDurationSeconds = totalDurationSeconds;
        this.exercises = exercises;
    }

    /** A workout without exercises has nothing to play. Asks the same list the player
     *  plays, so a row whose exercise is missing does not count. */
    public boolean canStart() {
        return !exercises.isEmpty();
    }

    /** E.g. "3 Übungen • 2:30"; for an empty workout, how to make it startable. */
    public String infoText() {
        if (!canStart()) return "Noch keine Übungen – füge eine hinzu, um zu starten";
        return ExerciseCount.text(exerciseCount)
                + " • " + MinutesSeconds.text(totalDurationSeconds);
    }

    /** Why the start button is disabled, for TalkBack; null while it is enabled. */
    public String startButtonDescription() {
        return canStart() ? null : "Start nicht möglich: noch keine Übungen";
    }
}
