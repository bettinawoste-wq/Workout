package com.wob.workout.data.repository;

import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.wob.workout.data.database.AppDatabase;
import com.wob.workout.data.database.DatabaseProvider;
import com.wob.workout.data.dao.WorkoutDao;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.WorkoutExercise;
import com.wob.workout.data.relation.WorkoutWithExercises;
import com.wob.workout.ui.workout.WorkoutListItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkoutSummaryRepository {

    private final WorkoutDao workoutDao;

    public WorkoutSummaryRepository(Context context) {
        this(DatabaseProvider.get(context.getApplicationContext()));
    }

    /** Talks to a given database directly, bypassing the DatabaseProvider singleton.
     *  Production code must go through {@link #WorkoutSummaryRepository(Context)} so every
     *  repository shares the same database instance and invalidation tracker. */
    @VisibleForTesting
    public WorkoutSummaryRepository(AppDatabase database) {
        this.workoutDao = database.workoutDao();
    }

    /** How many recent workouts the home screen shows. */
    public static final int RECENT_COUNT = 3;

    /** All workouts, most recently run first, then the never-run ones newest created
     *  first (see {@link WorkoutDao#getAllWithExercises()}). */
    public LiveData<List<WorkoutListItem>> observeAll() {
        return Transformations.map(
                workoutDao.getAllWithExercises(),
                WorkoutSummaryRepository::toListItems);
    }

    /** The recent workouts: the first {@link #RECENT_COUNT} of {@link #observeAll()}. */
    public LiveData<List<WorkoutListItem>> observeRecent() {
        return Transformations.map(
                observeAll(),
                items -> new ArrayList<>(items.subList(0, Math.min(RECENT_COUNT, items.size()))));
    }

    private static List<WorkoutListItem> toListItems(List<WorkoutWithExercises> rows) {
        List<WorkoutListItem> result = new ArrayList<>();

        for (WorkoutWithExercises row : rows) {
            int duration = 0;
            for (WorkoutExercise exercise : row.exercises) {
                duration += exercise.durationSeconds;
            }
            result.add(new WorkoutListItem(
                    row.workout, row.exercises.size(), duration, exercisesInOrder(row)));
        }

        return result;
    }

    private static List<Exercise> exercisesInOrder(WorkoutWithExercises row) {
        Map<Long, Exercise> exercisesById = new HashMap<>();
        for (Exercise exercise : row.exerciseDetails) {
            exercisesById.put(exercise.id, exercise);
        }

        List<Exercise> inOrder = new ArrayList<>();
        for (WorkoutExercise entry : row.exercisesInOrder()) {
            Exercise exercise = exercisesById.get(entry.exerciseId);
            if (exercise != null) inOrder.add(exercise);
        }
        return inOrder;
    }
}
