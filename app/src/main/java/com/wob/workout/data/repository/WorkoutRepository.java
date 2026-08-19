package com.wob.workout.data.repository;

import android.content.Context;

import com.wob.workout.data.database.DatabaseProvider;
import com.wob.workout.data.entity.Workout;

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