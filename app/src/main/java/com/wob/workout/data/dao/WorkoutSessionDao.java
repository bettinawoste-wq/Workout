package com.wob.workout.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.wob.workout.data.entity.WorkoutSession;
import com.wob.workout.data.relation.WorkoutSessionWithExercises;
import java.util.List;

@Dao
public interface WorkoutSessionDao {

    @Insert
    long insert(WorkoutSession session);

    @Update
    void update(WorkoutSession session);

    @Delete
    void delete(WorkoutSession session);

    /** Batched with each session's exercises and reactive — for the history screen
     *  observing the list live, replacing the old one-count-query-per-session shape.
     *  See wayfinder ticket #8. */
    @Transaction
    @Query("SELECT * FROM WorkoutSession ORDER BY startedAt DESC")
    LiveData<List<WorkoutSessionWithExercises>> getAllWithExercisesOrderByStartedAtDesc();

    @Query("SELECT * FROM WorkoutSession WHERE id = :id LIMIT 1")
    WorkoutSession getById(long id);
}
