package com.wob.workout.data.repository;

import android.content.Context;

import com.wob.workout.data.database.DatabaseProvider;
import com.wob.workout.data.entity.WorkoutExercise;

import java.util.List;

public class WorkoutExerciseRepository {

    private final Context context;

    public WorkoutExerciseRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public long save(WorkoutExercise workoutExercise) {
        return DatabaseProvider.get(context)
                .workoutExerciseDao()
                .insert(workoutExercise);
    }

    public void update(WorkoutExercise workoutExercise) {
        DatabaseProvider.get(context)
                .workoutExerciseDao()
                .update(workoutExercise);
    }

    public void delete(WorkoutExercise workoutExercise) {
        DatabaseProvider.get(context)
                .workoutExerciseDao()
                .delete(workoutExercise);
    }

    public List<WorkoutExercise> getByWorkout(long workoutId) {
        return DatabaseProvider.get(context)
                .workoutExerciseDao()
                .getByWorkout(workoutId);
    }

    public WorkoutExercise getById(long id) {
        return DatabaseProvider.get(context)
                .workoutExerciseDao()
                .getById(id);
    }

    public int getTotalDuration(long workoutId) {
        int total = 0;
        for (WorkoutExercise e : getByWorkout(workoutId))
            total += e.durationSeconds;
        return total;
    }

    public void deleteByWorkout(long workoutId) {
        DatabaseProvider.get(context)
                .workoutExerciseDao()
                .deleteByWorkout(workoutId);
    }
}