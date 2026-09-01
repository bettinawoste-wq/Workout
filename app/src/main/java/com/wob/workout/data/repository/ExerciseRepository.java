package com.wob.workout.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.wob.workout.data.database.DatabaseProvider;
import com.wob.workout.data.entity.Exercise;

import java.util.List;

public class ExerciseRepository {

    private final Context context;

    public ExerciseRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public long save(Exercise exercise) {
        return DatabaseProvider.get(context)
                .exerciseDao()
                .insert(exercise);
    }

    /** @return true if a row was actually updated (false if the exercise's id no longer exists). */
    public boolean update(Exercise exercise) {
        return DatabaseProvider.get(context)
                .exerciseDao()
                .update(exercise) > 0;
    }

    public void delete(Exercise exercise) {
        DatabaseProvider.get(context)
                .exerciseDao()
                .delete(exercise);
    }

    public Exercise getById(long id) {
        return DatabaseProvider.get(context)
                .exerciseDao()
                .getById(id);
    }

    public List<Exercise> getAll() {
        return DatabaseProvider.get(context)
                .exerciseDao()
                .getAll();
    }

    public LiveData<List<Exercise>> observeAll() {
        return DatabaseProvider.get(context)
                .exerciseDao()
                .getAllLive();
    }
}