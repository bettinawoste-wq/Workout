package com.wob.workout.data.dao;

import androidx.room.*;
import com.wob.workout.data.entity.WorkoutExerciseRow;
import java.util.List;

@Dao
public interface WorkoutExerciseDao {

    @Insert
    long insert(WorkoutExerciseRow row);

    /** All or none, in one transaction. */
    @Insert
    void insertAll(List<WorkoutExerciseRow> rows);

    @Query("SELECT * FROM WorkoutExercise WHERE workoutId = :workoutId ORDER BY position")
    List<WorkoutExerciseRow> getByWorkout(long workoutId);

    @Query("SELECT * FROM WorkoutExercise WHERE id = :id LIMIT 1")
    WorkoutExerciseRow getById(long id);

    @Query("DELETE FROM WorkoutExercise WHERE workoutId = :workoutId")
    void deleteByWorkout(long workoutId);
}