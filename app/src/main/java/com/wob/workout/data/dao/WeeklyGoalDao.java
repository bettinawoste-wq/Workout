package com.wob.workout.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.wob.workout.data.entity.WeeklyGoalChange;

import java.util.List;

@Dao
public interface WeeklyGoalDao {

    /** Replaces a change already made in the same week. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WeeklyGoalChange change);

    @Query("SELECT * FROM WeeklyGoalChange ORDER BY weekStartDay")
    List<WeeklyGoalChange> getAll();

    @Query("SELECT * FROM WeeklyGoalChange ORDER BY weekStartDay")
    LiveData<List<WeeklyGoalChange>> observeAll();
}
