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

    /** Saves a workout as the editor leaves it, in one transaction: its name and exactly
     *  these workout exercises, in place of the stored ones. Ones with id 0 are added; the
     *  others keep their id. Creates the workout if {@code id} is not positive.
     *  @return the workout's id. */
    public long saveWithExercises(long id, String name, List<WorkoutExercise> workoutExercises) {
        AppDatabase db = DatabaseProvider.get(context);
        return db.runInTransaction(() -> {
            long workoutId = id;
            if (workoutId > 0) {
                db.workoutDao().updateName(workoutId, name);
            } else {
                Workout workout = new Workout();
                workout.name = name;
                workoutId = db.workoutDao().insert(workout);
            }
            db.workoutExerciseDao().deleteByWorkout(workoutId);
            for (WorkoutExercise workoutExercise : workoutExercises) {
                workoutExercise.workoutId = workoutId;
            }
            db.workoutExerciseDao().insertAll(workoutExercises);
            return workoutId;
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