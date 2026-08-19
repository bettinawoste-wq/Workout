package com.wob.workout.data.database;

import androidx.room.*;

import com.wob.workout.data.converter.MediaTypeConverter;
import com.wob.workout.data.dao.ExerciseDao;
import com.wob.workout.data.dao.WorkoutDao;
import com.wob.workout.data.dao.WorkoutExerciseDao;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.data.entity.WorkoutExercise;

@Database(
        entities = {
                Exercise.class,
                Workout.class,
                WorkoutExercise.class
        },
        version = 2,
        exportSchema = false
)
@TypeConverters(MediaTypeConverter.class)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ExerciseDao exerciseDao();
    public abstract WorkoutDao workoutDao();
    public abstract WorkoutExerciseDao workoutExerciseDao();
}
