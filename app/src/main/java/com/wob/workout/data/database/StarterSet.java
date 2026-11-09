package com.wob.workout.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.wob.workout.R;
import com.wob.workout.data.entity.MediaType;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.media.MediaStorageManager;

import java.io.IOException;

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

    private static final String TAG = "StarterSet";

    public static RoomDatabase.Callback callback(Context context) {
        Context appContext = context.getApplicationContext();
        return new RoomDatabase.Callback() {
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                insert(db, appContext);
            }
        };
    }

    @VisibleForTesting
    static void insert(SupportSQLiteDatabase db, Context context) {
        Resources resources = context.getResources();
        String[] titles = resources.getStringArray(R.array.starter_exercise_titles);
        String[] descriptions = resources.getStringArray(R.array.starter_exercise_descriptions);
        int[] images = imageResources(resources);
        if (titles.length != descriptions.length || titles.length != images.length) {
            throw new IllegalStateException("starter set has " + titles.length + " titles, "
                    + descriptions.length + " descriptions and " + images.length + " images");
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
            String imageFile = copyImage(context, images[position]);
            if (imageFile != null) {
                exercise.put("mediaFile", imageFile);
                exercise.put("mediaType", MediaType.IMAGE.name());
            }
            long exerciseId = db.insert("Exercise", SQLiteDatabase.CONFLICT_NONE, exercise);

            ContentValues entry = new ContentValues();
            entry.put("workoutId", workoutId);
            entry.put("exerciseId", exerciseId);
            entry.put("position", position);
            entry.put("durationSeconds", seconds);
            db.insert("WorkoutExercise", SQLiteDatabase.CONFLICT_NONE, entry);
        }
    }

    /** @return the image resource per exercise, 0 where it has none */
    private static int[] imageResources(Resources resources) {
        TypedArray array = resources.obtainTypedArray(R.array.starter_exercise_images);
        try {
            int[] images = new int[array.length()];
            for (int i = 0; i < images.length; i++) {
                images[i] = array.getResourceId(i, 0);
            }
            return images;
        } finally {
            array.recycle();
        }
    }

    /**
     * Starter exercises become ordinary ones, so their picture lives where any other does.
     * Returns the stored file name, or null if there is no picture or the copy failed: a missing
     * picture must not stop the app from creating its database.
     */
    private static String copyImage(Context context, int imageResource) {
        if (imageResource == 0) return null;

        String fileName = context.getResources().getResourceEntryName(imageResource) + ".png";
        try {
            MediaStorageManager.copyResourceToExerciseFolder(context, imageResource, fileName);
            return fileName;
        } catch (IOException e) {
            Log.e(TAG, "Could not copy starter image " + fileName, e);
            return null;
        }
    }
}
