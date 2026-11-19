package com.wob.workout.backup;

/** What a backup file holds, shown before anything is changed; or why it is refused. The
 *  settings screen words the counts. */
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
}
