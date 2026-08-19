package com.wob.workout.data.dao;

import androidx.room.*;
import java.util.List;

import com.wob.workout.data.entity.Exercise;


@Dao
public interface ExerciseDao {
    @Insert
    long insert(Exercise exercise);

    @Update
    void update(Exercise exercise);

    @Delete
    void delete(Exercise exercise);

    @Query("SELECT * FROM Exercise ORDER BY id DESC")
    List<Exercise> getAll();

    @Query("SELECT * FROM Exercise WHERE id = :id LIMIT 1")
    Exercise getById(long id);
}
