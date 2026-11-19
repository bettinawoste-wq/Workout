package com.wob.workout.data.repository;

import android.content.Context;

import com.wob.workout.data.WorkoutExercise;
import com.wob.workout.data.database.AppDatabase;
import com.wob.workout.data.database.DatabaseProvider;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.WorkoutExerciseRow;
import com.wob.workout.data.relation.WorkoutWithExercises;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /** Copies the workout (its transition choice too) and its workout exercises (positions
     *  and durations) under
     *  {@code name}, in one transaction. Its history stays with the original.
     *  @return the copy's id. */
    public long duplicate(long id, String name) {
        AppDatabase db = DatabaseProvider.get(context);
        return db.runInTransaction(() -> {
            Workout copy = db.workoutDao().getById(id);
            copy.id = 0;
            copy.name = name;
            long copyId = db.workoutDao().insert(copy);
            for (WorkoutExerciseRow workoutExercise : db.workoutExerciseDao().getByWorkout(id)) {
                workoutExercise.id = 0;
                workoutExercise.workoutId = copyId;
                db.workoutExerciseDao().insert(workoutExercise);
            }
            return copyId;
        });
    }

    /** Saves a workout as the editor leaves it, in one transaction: its name, its own
     *  transition length (null to follow the default transition) and exactly these workout
     *  exercises, in this order, in place of the stored ones. Unsaved ones are
     *  added; the others keep their row id. Creates the workout if {@code id} is not
     *  positive.
     *  @return the workout's id. */
    public long saveWithExercises(long id, String name, Integer transitionSeconds,
                                  List<WorkoutExercise> workoutExercises) {
        AppDatabase db = DatabaseProvider.get(context);
        return db.runInTransaction(() -> {
            long workoutId = id;
            if (workoutId > 0) {
                db.workoutDao().updateNameAndTransition(workoutId, name, transitionSeconds);
            } else {
                Workout workout = new Workout();
                workout.name = name;
                workout.transitionSeconds = transitionSeconds;
                workoutId = db.workoutDao().insert(workout);
            }
            db.workoutExerciseDao().deleteByWorkout(workoutId);
            List<WorkoutExerciseRow> rows = new ArrayList<>();
            for (WorkoutExercise workoutExercise : workoutExercises) {
                WorkoutExerciseRow row = new WorkoutExerciseRow();
                row.id = workoutExercise.rowId;
                row.workoutId = workoutId;
                row.exerciseId = workoutExercise.exerciseId();
                row.durationSeconds = workoutExercise.durationSeconds;
                row.position = rows.size();
                rows.add(row);
            }
            db.workoutExerciseDao().insertAll(rows);
            return workoutId;
        });
    }

    /** The workout's exercises in their order, empty if the workout is gone. The editor and
     *  the player both load them here. */
    public List<WorkoutExercise> getWorkoutExercises(long workoutId) {
        WorkoutWithExercises workout =
                DatabaseProvider.get(context).workoutDao().getWithExercises(workoutId);
        return workout != null ? workoutExercisesOf(workout) : new ArrayList<>();
    }

    /** The one place that turns stored rows into workout exercises: in their position's
     *  order, leaving out any whose exercise is missing. */
    static List<WorkoutExercise> workoutExercisesOf(WorkoutWithExercises workout) {
        Map<Long, Exercise> exercisesById = new HashMap<>();
        for (Exercise exercise : workout.exerciseDetails) {
            exercisesById.put(exercise.id, exercise);
        }
        List<WorkoutExercise> inOrder = new ArrayList<>();
        for (WorkoutExerciseRow row : workout.exercisesInOrder()) {
            Exercise exercise = exercisesById.get(row.exerciseId);
            if (exercise != null) {
                inOrder.add(new WorkoutExercise(row.id, exercise, row.durationSeconds));
            }
        }
        return inOrder;
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