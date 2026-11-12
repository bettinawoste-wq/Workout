package com.wob.workout.data.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.room.Room;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.MediaType;
import com.wob.workout.media.Framing;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

/**
 * Creates a version 4 database from its exported schema (generated from tag v1.1, see
 * ADR 0004), migrates it with {@link AppDatabase#MIGRATION_4_5}, and lets Room validate the
 * result against the version 5 schema before the DAOs read it.
 */
@RunWith(AndroidJUnit4.class)
public class Migration4To5Test {

    private static final String DB_NAME = "migration-4-5-test.db";
    private static final float DELTA = 0.001f;

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), AppDatabase.class);

    private AppDatabase database;

    @Before
    public void setUp() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(DB_NAME, 4)) {
            db.execSQL("INSERT INTO `Exercise` (`title`, `mediaFile`, `mediaType`) "
                    + "VALUES ('Squat', 'squat.jpg', 'IMAGE')");
        }
        helper.runMigrationsAndValidate(DB_NAME, 5, true, AppDatabase.MIGRATION_4_5).close();

        database = Room.databaseBuilder(
                        ApplicationProvider.getApplicationContext(), AppDatabase.class, DB_NAME)
                .addMigrations(AppDatabase.MIGRATION_4_5)
                .allowMainThreadQueries()
                .build();
        helper.closeWhenFinished(database);
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
}
