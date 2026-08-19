package com.wob.workout.data.dao;

import androidx.room.*;
import com.wob.workout.data.entity.Workout;
import java.util.List;

@Dao
public interface WorkoutDao {

    @Insert
    long insert(Workout workout);

    @Update
    void update(Workout workout);

    @Delete
    void delete(Workout workout);

    @Query("SELECT * FROM Workout WHERE id = :id LIMIT 1")
    Workout getById(long id);

    @Query("SELECT * FROM Workout ORDER BY id DESC")
    List<Workout> getAll();
}