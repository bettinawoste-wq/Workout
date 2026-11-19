package com.wob.workout.data.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.room.Room;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.wob.workout.data.entity.WeeklyGoalChange;
import com.wob.workout.data.entity.WorkoutSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

/**
 * Creates a version 8 database from its exported schema, migrates it with
 * {@link AppDatabase#MIGRATION_8_9}, and lets Room validate the result against the version 9
 * schema before the DAOs read it.
 */
@RunWith(AndroidJUnit4.class)
public class Migration8To9Test {

    private static final String DB_NAME = "migration-8-9-test.db";

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), AppDatabase.class);

    private AppDatabase database;

    @Before
    public void setUp() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(DB_NAME, 8)) {
            db.execSQL("INSERT INTO `Workout` (`id`, `name`, `countdownEnabled`) "
                    + "VALUES (1, 'Leg Day', 1)");
            db.execSQL("INSERT INTO `WorkoutSession` (`id`, `workoutId`, `workoutName`, "
                    + "`startedAt`, `finishedAt`, `activeDurationSeconds`, `completed`, "
                    + "`restSeconds`) VALUES (1, 1, 'Leg Day', 1000, 2000, 75, 1, 30)");
        }
        helper.runMigrationsAndValidate(DB_NAME, 9, true, AppDatabase.MIGRATION_8_9).close();

        database = Room.databaseBuilder(
                        ApplicationProvider.getApplicationContext(), AppDatabase.class, DB_NAME)
                .addMigrations(AppDatabase.MIGRATION_8_9)
                .allowMainThreadQueries()
                .build();
        helper.closeWhenFinished(database);
    }

    @Test
    public void thereIsNoWeeklyGoalAfterTheUpdate() {
        assertTrue(database.weeklyGoalDao().getAll().isEmpty());
    }

    @Test
    public void aWeeklyGoalChangeCanBeRecorded() {
        database.weeklyGoalDao().insert(new WeeklyGoalChange(20_408, 3));

        List<WeeklyGoalChange> changes = database.weeklyGoalDao().getAll();
        assertEquals(1, changes.size());
        assertEquals(20_408, changes.get(0).weekStartDay);
        assertEquals(Integer.valueOf(3), changes.get(0).trainingDays);
    }

    @Test
    public void theHistoryIsUntouched() {
        WorkoutSession session = database.workoutSessionDao().getById(1);

        assertEquals(75, session.activeDurationSeconds);
        assertTrue(session.completed);
        assertEquals(Integer.valueOf(30), session.restSeconds);
    }
}
