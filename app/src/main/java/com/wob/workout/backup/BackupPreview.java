package com.wob.workout.backup;

import com.wob.workout.ui.ExerciseCount;

/** What a backup file holds, shown before anything is changed; or why it is refused. */
public final class BackupPreview {

    /** Null if the file can be read back. */
    public final BackupRefused.Reason refusal;
    public final int exercises;
    public final int workouts;
    public final int workoutSessions;

    private BackupPreview(BackupRefused.Reason refusal, int exercises, int workouts,
                          int workoutSessions) {
        this.refusal = refusal;
        this.exercises = exercises;
        this.workouts = workouts;
        this.workoutSessions = workoutSessions;
    }

    static BackupPreview readable(int exercises, int workouts, int workoutSessions) {
        return new BackupPreview(null, exercises, workouts, workoutSessions);
    }

    public static BackupPreview refused(BackupRefused.Reason reason) {
        return new BackupPreview(reason, 0, 0, 0);
    }

    /** For example "12 Übungen, 3 Workouts, 40 Einträge im Verlauf". */
    public String text() {
        return ExerciseCount.text(exercises)
                + ", " + workouts + (workouts == 1 ? " Workout" : " Workouts")
                + ", " + workoutSessions
                + (workoutSessions == 1 ? " Eintrag" : " Einträge") + " im Verlauf";
    }
}
