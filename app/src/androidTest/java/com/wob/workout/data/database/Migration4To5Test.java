package com.wob.workout.data.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.MediaType;
import com.wob.workout.media.Framing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

/**
 * Builds a version 4 database by hand (the schema is not exported), migrates it and lets Room
 * validate the result against the current entities.
 */
@RunWith(AndroidJUnit4.class)
public class Migration4To5Test {

    private static final String DB_NAME = "migration-4-5-test.db";
    private static final float DELTA = 0.001f;

    private Context context;
    private AppDatabase database;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(DB_NAME);
        createVersion4Database();
        database = Room.databaseBuilder(context, AppDatabase.class, DB_NAME)
                .addMigrations(AppDatabase.MIGRATION_4_5)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void tearDown() {
        database.close();
        context.deleteDatabase(DB_NAME);
    }

    @Test
    public void existingExerciseKeepsItsDataAndHasNoFraming() {
        Exercise exercise = database.exerciseDao().getAll().get(0);

        assertEquals("Squat", exercise.title);
        assertEquals("squat.jpg", exercise.mediaFile);
        assertEquals(MediaType.IMAGE, exercise.mediaType);
        assertNull(exercise.framing());
    }

    @Test
    public void framingCanBeStoredAfterTheMigration() {
        Exercise exercise = database.exerciseDao().getAll().get(0);
        exercise.storeFraming(new Framing(2f, 0.1f, -0.2f));
        database.exerciseDao().update(exercise);

        Framing stored = database.exerciseDao().getAll().get(0).framing();

        assertNotNull(stored);
        assertEquals(2f, stored.zoom, DELTA);
        assertEquals(0.1f, stored.offsetX, DELTA);
        assertEquals(-0.2f, stored.offsetY, DELTA);
    }

    private void createVersion4Database() {
        File file = context.getDatabasePath(DB_NAME);
        file.getParentFile().mkdirs();

        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, null)) {
            db.execSQL("CREATE TABLE `Exercise` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`title` TEXT, `description` TEXT, `mediaFile` TEXT, `mediaType` TEXT)");
            db.execSQL("CREATE TABLE `Workout` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, "
                    + "`transitionSeconds` INTEGER NOT NULL, "
                    + "`countdownEnabled` INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE `WorkoutExercise` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`workoutId` INTEGER NOT NULL, `exerciseId` INTEGER NOT NULL, "
                    + "`position` INTEGER NOT NULL, `durationSeconds` INTEGER NOT NULL, "
                    + "FOREIGN KEY(`workoutId`) REFERENCES `Workout`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE, "
                    + "FOREIGN KEY(`exerciseId`) REFERENCES `Exercise`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX `index_WorkoutExercise_workoutId` "
                    + "ON `WorkoutExercise` (`workoutId`)");
            db.execSQL("CREATE INDEX `index_WorkoutExercise_exerciseId` "
                    + "ON `WorkoutExercise` (`exerciseId`)");
            db.execSQL("CREATE TABLE `WorkoutSession` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`workoutId` INTEGER NOT NULL, `workoutName` TEXT, "
                    + "`startedAt` INTEGER NOT NULL, `finishedAt` INTEGER NOT NULL, "
                    + "`activeDurationSeconds` INTEGER NOT NULL, `completed` INTEGER NOT NULL)");
            db.execSQL("CREATE INDEX `index_WorkoutSession_workoutId` "
                    + "ON `WorkoutSession` (`workoutId`)");
            db.execSQL("CREATE TABLE `ExerciseSession` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`sessionId` INTEGER NOT NULL, `exerciseId` INTEGER NOT NULL, "
                    + "`exerciseTitle` TEXT, `position` INTEGER NOT NULL, "
                    + "`plannedDurationSeconds` INTEGER NOT NULL, "
                    + "`actualDurationSeconds` INTEGER NOT NULL, "
                    + "`startedAt` INTEGER NOT NULL, `finishedAt` INTEGER NOT NULL, "
                    + "FOREIGN KEY(`sessionId`) REFERENCES `WorkoutSession`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX `index_ExerciseSession_sessionId` "
                    + "ON `ExerciseSession` (`sessionId`)");

            db.execSQL("INSERT INTO `Exercise` (`title`, `mediaFile`, `mediaType`) "
                    + "VALUES ('Squat', 'squat.jpg', 'IMAGE')");
            db.setVersion(4);
        }
    }
}
