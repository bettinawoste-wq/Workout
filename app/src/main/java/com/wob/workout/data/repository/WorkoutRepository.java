package com.wob.workout.data.repository;

import android.content.Context;

import com.wob.workout.data.database.AppDatabase;
import com.wob.workout.data.database.DatabaseProvider;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.data.entity.WorkoutExercise;

import java.util.List;

public class WorkoutRepository {

    private final Context context;

    public WorkoutRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public long save(Workout workout) {
        return DatabaseProvider.get(context)
                .workoutDao()
                .insert(workout);
    }

    public void update(Workout workout) {
        DatabaseProvider.get(context)
                .workoutDao()
                .update(workout);
    }

    /** @return true if a row was actually updated (false if {@code id} no longer exists). */
    public boolean updateName(long id, String name) {
        return DatabaseProvider.get(context)
                .workoutDao()
                .updateName(id, name) > 0;
    }

    public void delete(Workout workout) {
        DatabaseProvider.get(context)
                .workoutDao()
                .delete(workout);
    }

    /** Copies the workout and its workout exercises (positions and durations) under
     *  {@code name}, in one transaction. Its history stays with the original.
     *  @return the copy's id. */
    public long duplicate(long id, String name) {
        AppDatabase db = DatabaseProvider.get(context);
        return db.runInTransaction(() -> {
            Workout copy = db.workoutDao().getById(id);
            copy.id = 0;
            copy.name = name;
            long copyId = db.workoutDao().insert(copy);
            for (WorkoutExercise workoutExercise : db.workoutExerciseDao().getByWorkout(id)) {
                workoutExercise.id = 0;
                workoutExercise.workoutId = copyId;
                db.workoutExerciseDao().insert(workoutExercise);
            }
            return copyId;
        });
    }

    public Workout getById(long id) {
        return DatabaseProvider.get(context)
                .workoutDao()
                .getById(id);
    }

    public List<Workout> getAll() {
        return DatabaseProvider.get(context)
                .workoutDao()
                .getAll();
    }
}