package com.wob.workout.data.dao;

import androidx.room.*;
import com.wob.workout.data.entity.WorkoutExercise;
import java.util.List;

@Dao
public interface WorkoutExerciseDao {

    @Insert
    long insert(WorkoutExercise workoutExercise);

    /** All or none, in one transaction. */
    @Insert
    void insertAll(List<WorkoutExercise> workoutExercises);

    @Update
    void update(WorkoutExercise workoutExercise);

    @Delete
    void delete(WorkoutExercise workoutExercise);

    @Query("SELECT * FROM WorkoutExercise WHERE workoutId = :workoutId ORDER BY position")
    List<WorkoutExercise> getByWorkout(long workoutId);

    @Query("SELECT * FROM WorkoutExercise WHERE id = :id LIMIT 1")
    WorkoutExercise getById(long id);

    @Query("DELETE FROM WorkoutExercise WHERE workoutId = :workoutId")
    void deleteByWorkout(long workoutId);
}