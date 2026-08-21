package com.wob.workout.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.wob.workout.data.database.AppDatabase;
import com.wob.workout.data.database.DatabaseProvider;
import com.wob.workout.data.dao.WorkoutDao;
import com.wob.workout.data.entity.WorkoutExercise;
import com.wob.workout.data.relation.WorkoutWithExercises;
import com.wob.workout.ui.workout.WorkoutListItem;

import java.util.ArrayList;
import java.util.List;

public class WorkoutSummaryRepository {

    private final WorkoutDao workoutDao;

    public WorkoutSummaryRepository(Context context) {
        this(DatabaseProvider.get(context.getApplicationContext()));
    }

    /** For tests: talk to a given database directly, bypassing the DatabaseProvider singleton. */
    public WorkoutSummaryRepository(AppDatabase database) {
        this.workoutDao = database.workoutDao();
    }

    public LiveData<List<WorkoutListItem>> observeAll() {
        return Transformations.map(
                workoutDao.getAllWithExercises(),
                WorkoutSummaryRepository::toListItems);
    }

    private static List<WorkoutListItem> toListItems(List<WorkoutWithExercises> rows) {
        List<WorkoutListItem> result = new ArrayList<>();

        for (WorkoutWithExercises row : rows) {
            int duration = 0;
            for (WorkoutExercise exercise : row.exercises) {
                duration += exercise.durationSeconds;
            }
            result.add(new WorkoutListItem(row.workout, row.exercises.size(), duration));
        }

        return result;
    }
}
