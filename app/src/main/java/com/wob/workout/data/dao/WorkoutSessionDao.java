package com.wob.workout.data.dao;

import androidx.room.*;
import com.wob.workout.data.entity.WorkoutSession;
import java.util.List;

@Dao
public interface WorkoutSessionDao {

    @Insert
    long insert(WorkoutSession session);

    @Update
    void update(WorkoutSession session);

    @Query("SELECT * FROM WorkoutSession ORDER BY startedAt DESC")
    List<WorkoutSession> getAllOrderByStartedAtDesc();

    @Query("SELECT * FROM WorkoutSession WHERE id = :id LIMIT 1")
    WorkoutSession getById(long id);
}
