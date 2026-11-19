package com.wob.workout.data.repository;

import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;

import com.wob.workout.data.dao.WeeklyGoalDao;
import com.wob.workout.data.database.AppDatabase;
import com.wob.workout.data.database.DatabaseProvider;
import com.wob.workout.data.entity.WeeklyGoalChange;
import com.wob.workout.training.Clock;
import com.wob.workout.training.WeeklyProgress;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The weekly goal with its history of changes: each change applies from the week it was
 * made in, so {@link WeeklyProgress} measures every past week against the goal of its time.
 */
public class WeeklyGoalRepository {

    private final WeeklyGoalDao dao;

    public WeeklyGoalRepository(Context context) {
        this(DatabaseProvider.get(context.getApplicationContext()));
    }

    /** Talks to a given database directly, bypassing the DatabaseProvider singleton.
     *  Production code must go through {@link #WeeklyGoalRepository(Context)} so every
     *  repository shares the same database instance and invalidation tracker. */
    @VisibleForTesting
    public WeeklyGoalRepository(AppDatabase database) {
        this.dao = database.weeklyGoalDao();
    }

    /** The goal in training days that applies now, or null without one. */
    public Integer getGoal(Clock clock) {
        return WeeklyProgress.of(Collections.emptyList(), dao.getAll(), clock).getGoal();
    }

    /** Sets the goal, or removes it with null, from the current week on. */
    public void setGoal(Integer trainingDays, Clock clock) {
        if (Objects.equals(trainingDays, getGoal(clock))) return;
        dao.insert(new WeeklyGoalChange(WeeklyProgress.weekStartDay(clock), trainingDays));
    }

    /** Every change, oldest first. */
    public List<WeeklyGoalChange> getChanges() {
        return dao.getAll();
    }

    public LiveData<List<WeeklyGoalChange>> observeChanges() {
        return dao.observeAll();
    }
}
