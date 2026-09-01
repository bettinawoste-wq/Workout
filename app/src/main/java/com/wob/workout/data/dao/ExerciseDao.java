package com.wob.workout.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

import com.wob.workout.data.entity.Exercise;


@Dao
public interface ExerciseDao {
    @Insert
    long insert(Exercise exercise);

    /** @return the number of rows updated (0 if the exercise's id no longer exists). */
    @Update
    int update(Exercise exercise);

    @Delete
    void delete(Exercise exercise);

    @Query("SELECT * FROM Exercise ORDER BY id DESC")
    List<Exercise> getAll();

    /** Same query as {@link #getAll()}, reactive — for a screen observing the catalog live.
     *  getAll() stays for the one-shot callers (e.g. WorkoutEditFragment's exercise picker). */
    @Query("SELECT * FROM Exercise ORDER BY id DESC")
    LiveData<List<Exercise>> getAllLive();

    @Query("SELECT * FROM Exercise WHERE id = :id LIMIT 1")
    Exercise getById(long id);
}
