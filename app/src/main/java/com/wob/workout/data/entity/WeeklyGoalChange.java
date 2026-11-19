package com.wob.workout.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * One change of the weekly goal, effective from the week it was made in until the next
 * change. One per week: a later change in the same week replaces it.
 */
@Entity
public class WeeklyGoalChange {

    /** The Monday of the week it applies from, as days since 1970-01-01. */
    @PrimaryKey
    public long weekStartDay;

    /** Training days to reach per week; null when the goal was removed. */
    public Integer trainingDays;

    public WeeklyGoalChange() {
    }

    @Ignore
    public WeeklyGoalChange(long weekStartDay, Integer trainingDays) {
        this.weekStartDay = weekStartDay;
        this.trainingDays = trainingDays;
    }
}
