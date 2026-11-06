package com.wob.workout.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.wob.workout.R;
import com.wob.workout.data.entity.Workout;

/**
 * The starter set (see GLOSSARY.md): the exercises and the one workout a fresh installation
 * starts with. Wired in as a Room {@code onCreate} callback, which fires only when the database
 * file is first created - so existing installs are untouched, and anything the user later
 * deletes stays deleted.
 *
 * <p>Content comes from {@code res/values/starter.xml}; the debug variant overrides it with a
 * small demo version. Rows are written with plain SQL because the DAOs can't be used from inside
 * {@code onCreate}.
 */
public final class StarterSet {

    private StarterSet() {
    }

    public static RoomDatabase.Callback callback(Context context) {
        Resources resources = context.getApplicationContext().getResources();
        return new RoomDatabase.Callback() {
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                insert(db, resources);
            }
        };
    }

    @VisibleForTesting
    static void insert(SupportSQLiteDatabase db, Resources resources) {
        String[] titles = resources.getStringArray(R.array.starter_exercise_titles);
        String[] descriptions = resources.getStringArray(R.array.starter_exercise_descriptions);
        if (titles.length != descriptions.length) {
            throw new IllegalStateException("starter set has " + titles.length + " titles but "
                    + descriptions.length + " descriptions");
        }
        int seconds = resources.getInteger(R.integer.starter_exercise_seconds);

        Workout defaults = new Workout();
        ContentValues workout = new ContentValues();
        workout.put("name", resources.getString(R.string.starter_workout_name));
        workout.put("transitionSeconds", defaults.transitionSeconds);
        workout.put("countdownEnabled", defaults.countdownEnabled);
        long workoutId = db.insert("Workout", SQLiteDatabase.CONFLICT_NONE, workout);

        for (int position = 0; position < titles.length; position++) {
            ContentValues exercise = new ContentValues();
            exercise.put("title", titles[position]);
            exercise.put("description", descriptions[position]);
            long exerciseId = db.insert("Exercise", SQLiteDatabase.CONFLICT_NONE, exercise);

            ContentValues entry = new ContentValues();
            entry.put("workoutId", workoutId);
            entry.put("exerciseId", exerciseId);
            entry.put("position", position);
            entry.put("durationSeconds", seconds);
            db.insert("WorkoutExercise", SQLiteDatabase.CONFLICT_NONE, entry);
        }
    }
}
