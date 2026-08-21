package com.wob.workout.data.repository;

import android.content.Context;

import com.wob.workout.data.database.DatabaseProvider;
import com.wob.workout.data.entity.ExerciseSession;
import com.wob.workout.data.entity.WorkoutSession;
import com.wob.workout.ui.history.WorkoutSessionSummary;

import java.util.ArrayList;
import java.util.List;

public class WorkoutHistoryRepository {

    private final Context context;

    public WorkoutHistoryRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public long startSession(long workoutId, String workoutName) {
        WorkoutSession session = new WorkoutSession();
        session.workoutId = workoutId;
        session.workoutName = workoutName;
        session.startedAt = System.currentTimeMillis();
        session.completed = false;

        return DatabaseProvider.get(context)
                .workoutSessionDao()
                .insert(session);
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

        return DatabaseProvider.get(context)
                .exerciseSessionDao()
                .insert(exerciseSession);
    }

    public void finishSession(long sessionId, boolean completed) {
        WorkoutSession session =
                DatabaseProvider.get(context)
                        .workoutSessionDao()
                        .getById(sessionId);

        if (session == null) return;

        session.finishedAt = System.currentTimeMillis();
        session.completed = completed;
        session.activeDurationSeconds =
                DatabaseProvider.get(context)
                        .exerciseSessionDao()
                        .getTotalActiveSeconds(sessionId);

        DatabaseProvider.get(context)
                .workoutSessionDao()
                .update(session);
    }

    public List<WorkoutSessionSummary> getHistory() {
        List<WorkoutSessionSummary> result = new ArrayList<>();

        for (WorkoutSession session :
                DatabaseProvider.get(context).workoutSessionDao().getAllOrderByStartedAtDesc()) {

            int exerciseCount =
                    DatabaseProvider.get(context)
                            .exerciseSessionDao()
                            .countBySession(session.id);

            // Sessions ohne aufgezeichnete Uebung und ohne Abschluss sind nur ein
            // sofortiger Rueckzug aus dem Player, keine echten Verlaufseintraege.
            if (exerciseCount == 0 && !session.completed) continue;

            result.add(new WorkoutSessionSummary(session, exerciseCount));
        }

        return result;
    }

    public WorkoutSession getSession(long sessionId) {
        return DatabaseProvider.get(context)
                .workoutSessionDao()
                .getById(sessionId);
    }

    public List<ExerciseSession> getExercises(long sessionId) {
        return DatabaseProvider.get(context)
                .exerciseSessionDao()
                .getBySession(sessionId);
    }

    public void delete(WorkoutSession session) {
        DatabaseProvider.get(context)
                .workoutSessionDao()
                .delete(session);
    }
}
