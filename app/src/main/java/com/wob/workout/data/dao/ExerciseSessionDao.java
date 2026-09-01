package com.wob.workout.data.dao;

import androidx.room.*;
import com.wob.workout.data.entity.ExerciseSession;
import java.util.List;

@Dao
public interface ExerciseSessionDao {

    @Insert
    long insert(ExerciseSession exerciseSession);

    @Query("SELECT * FROM ExerciseSession WHERE sessionId = :sessionId ORDER BY position")
    List<ExerciseSession> getBySession(long sessionId);

    @Query("SELECT COALESCE(SUM(actualDurationSeconds), 0) FROM ExerciseSession WHERE sessionId = :sessionId")
    int getTotalActiveSeconds(long sessionId);
}
