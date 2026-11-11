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

    /** All workouts, most recently run first (by the start of their latest workout session,
     *  completed or aborted), then the never-run ones newest created first. Room re-queries
     *  when WorkoutSession changes, so a new session reorders the list live. */
    @Transaction
    @Query("SELECT Workout.* FROM Workout"
            + " LEFT JOIN (SELECT workoutId, MAX(startedAt) AS lastStartedAt"
            + " FROM WorkoutSession GROUP BY workoutId) AS lastRun"
            + " ON lastRun.workoutId = Workout.id"
            + " ORDER BY lastRun.lastStartedAt IS NULL, lastRun.lastStartedAt DESC,"
            + " Workout.id DESC")
    LiveData<List<WorkoutWithExercises>> getAllWithExercises();
}