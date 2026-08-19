package com.wob.workout.data.repository;

import android.content.Context;

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

    public void update(Exercise exercise) {
        DatabaseProvider.get(context)
                .exerciseDao()
                .update(exercise);
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
}