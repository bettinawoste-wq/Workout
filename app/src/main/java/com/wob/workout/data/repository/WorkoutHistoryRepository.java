package com.wob.workout.data.repository;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.wob.workout.data.database.AppDatabase;
import com.wob.workout.data.database.DatabaseProvider;
import com.wob.workout.data.entity.ExerciseSession;
import com.wob.workout.data.entity.WorkoutSession;
import com.wob.workout.ui.history.WorkoutSessionSummary;

import java.util.ArrayList;
import java.util.List;

public class WorkoutHistoryRepository {

    private final AppDatabase database;

    public WorkoutHistoryRepository(Context context) {
        this(DatabaseProvider.get(context.getApplicationContext()));
    }

    /** Talks to a given database directly, bypassing the DatabaseProvider singleton.
     *  Production code must go through {@link #WorkoutHistoryRepository(Context)} so every
     *  repository shares the same database instance and invalidation tracker. */
    @VisibleForTesting
    public WorkoutHistoryRepository(AppDatabase database) {
        this.database = database;
    }

    public long startSession(long workoutId, String workoutName) {
        WorkoutSession session = new WorkoutSession();
        session.workoutId = workoutId;
        session.workoutName = workoutName;
        session.startedAt = System.currentTimeMillis();
        session.completed = false;

        return database.workoutSessionDao().insert(session);
    }

    public long recordExercise(
            long sessionId,
            long exerciseId,
            String exerciseTitle,
            int position,
            int plannedDurationSeconds,
            int actualDurationSeconds,
            long startedAt,
            long finishedAt) {

        ExerciseSession exerciseSession = new ExerciseSession();
        exerciseSession.sessionId = sessionId;
        exerciseSession.exerciseId = exerciseId;
        exerciseSession.exerciseTitle = exerciseTitle;
        exerciseSession.position = position;
        exerciseSession.plannedDurationSeconds = plannedDurationSeconds;
        exerciseSession.actualDurationSeconds = actualDurationSeconds;
        exerciseSession.startedAt = startedAt;
        exerciseSession.finishedAt = finishedAt;

        return database.exerciseSessionDao().insert(exerciseSession);
    }

    public void finishSession(long sessionId, boolean completed) {
        WorkoutSession session = database.workoutSessionDao().getById(sessionId);

        if (session == null) return;

        session.finishedAt = System.currentTimeMillis();
        session.completed = completed;
        session.activeDurationSeconds =
                database.exerciseSessionDao().getTotalActiveSeconds(sessionId);

        database.workoutSessionDao().update(session);
    }

    public List<WorkoutSessionSummary> getHistory() {
        List<WorkoutSessionSummary> result = new ArrayList<>();

        for (WorkoutSession session : database.workoutSessionDao().getAllOrderByStartedAtDesc()) {

            int exerciseCount =
                    database.exerciseSessionDao().countBySession(session.id);

            // Sessions ohne aufgezeichnete Uebung und ohne Abschluss sind nur ein
            // sofortiger Rueckzug aus dem Player, keine echten Verlaufseintraege.
            if (exerciseCount == 0 && !session.completed) continue;

            result.add(new WorkoutSessionSummary(session, exerciseCount));
        }

        return result;
    }

    public WorkoutSession getSession(long sessionId) {
        return database.workoutSessionDao().getById(sessionId);
    }

    public List<ExerciseSession> getExercises(long sessionId) {
        return database.exerciseSessionDao().getBySession(sessionId);
    }

    public void delete(WorkoutSession session) {
        database.workoutSessionDao().delete(session);
    }
}
