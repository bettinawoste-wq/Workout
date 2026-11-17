package com.wob.workout.data.repository;

import android.content.Context;

import com.wob.workout.data.database.DatabaseProvider;
import com.wob.workout.data.entity.WorkoutExerciseRow;

import java.util.List;

public class WorkoutExerciseRepository {

    private final Context context;

    public WorkoutExerciseRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public long save(WorkoutExerciseRow row) {
        return DatabaseProvider.get(context)
                .workoutExerciseDao()
                .insert(row);
    }

    public List<WorkoutExerciseRow> getByWorkout(long workoutId) {
        return DatabaseProvider.get(context)
                .workoutExerciseDao()
                .getByWorkout(workoutId);
    }

    public WorkoutExerciseRow getById(long id) {
        return DatabaseProvider.get(context)
                .workoutExerciseDao()
                .getById(id);
    }

    public int getTotalDuration(long workoutId) {
        int total = 0;
        for (WorkoutExerciseRow e : getByWorkout(workoutId))
            total += e.durationSeconds;
        return total;
    }

    public void deleteByWorkout(long workoutId) {
        DatabaseProvider.get(context)
                .workoutExerciseDao()
                .deleteByWorkout(workoutId);
    }
}