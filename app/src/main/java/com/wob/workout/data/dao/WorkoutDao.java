package com.wob.workout.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.data.relation.WorkoutWithExercises;
import java.util.List;

@Dao
public interface WorkoutDao {

    @Insert
    long insert(Workout workout);

    @Update
    void update(Workout workout);

    /** Partial update touching only the name, so a caller that hasn't loaded the rest of
     *  the row can't accidentally overwrite it. Returns the number of rows updated (0 if
     *  {@code id} no longer exists). */
    @Query("UPDATE Workout SET name = :name WHERE id = :id")
    int updateName(long id, String name);

    @Delete
    void delete(Workout workout);

    @Query("SELECT * FROM Workout WHERE id = :id LIMIT 1")
    Workout getById(long id);

    @Query("SELECT * FROM Workout ORDER BY id DESC")
    List<Workout> getAll();

    @Transaction
    @Query("SELECT * FROM Workout ORDER BY id DESC")
    LiveData<List<WorkoutWithExercises>> getAllWithExercises();
}