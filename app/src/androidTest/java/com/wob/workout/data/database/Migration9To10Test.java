package com.wob.workout.data.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.room.Room;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.wob.workout.data.entity.Reminder;
import com.wob.workout.data.entity.WeeklyGoalChange;
import com.wob.workout.data.entity.WorkoutSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

/**
 * Creates a version 9 database from its exported schema, migrates it with
 * {@link AppDatabase#MIGRATION_9_10}, and lets Room validate the result against the version
 * 10 schema before the DAOs read it.
 */
@RunWith(AndroidJUnit4.class)
public class Migration9To10Test {

    private static final String DB_NAME = "migration-9-10-test.db";

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), AppDatabase.class);

    private AppDatabase database;

    @Before
    public void setUp() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(DB_NAME, 9)) {
            db.execSQL("INSERT INTO `Workout` (`id`, `name`, `countdownEnabled`) "
                    + "VALUES (1, 'Leg Day', 1)");
            db.execSQL("INSERT INTO `WorkoutSession` (`id`, `workoutId`, `workoutName`, "
                    + "`startedAt`, `finishedAt`, `activeDurationSeconds`, `completed`, "
                    + "`restSeconds`) VALUES (1, 1, 'Leg Day', 1000, 2000, 75, 1, 30)");
            db.execSQL("INSERT INTO `WeeklyGoalChange` (`weekStartDay`, `trainingDays`) "
                    + "VALUES (20408, 3)");
        }
        helper.runMigrationsAndValidate(DB_NAME, 10, true, AppDatabase.MIGRATION_9_10).close();

        database = Room.databaseBuilder(
                        ApplicationProvider.getApplicationContext(), AppDatabase.class, DB_NAME)
                .addMigrations(AppDatabase.MIGRATION_9_10)
                .allowMainThreadQueries()
                .build();
        helper.closeWhenFinished(database);
    }

    @Test
    public void thereIsNoReminderAfterTheUpdate() {
        assertTrue(database.reminderDao().getAll().isEmpty());
    }

    @Test
    public void aReminderCanBeStored() {
        database.reminderDao().insert(new Reminder(18 * 60, Reminder.MONDAY, true));

        List<Reminder> reminders = database.reminderDao().getAll();
        assertEquals(1, reminders.size());
        assertEquals(18 * 60, reminders.get(0).minuteOfDay);
        assertEquals(Reminder.MONDAY, reminders.get(0).weekdays);
        assertTrue(reminders.get(0).enabled);
    }

    @Test
    public void theHistoryAndTheWeeklyGoalAreUntouched() {
        WorkoutSession session = database.workoutSessionDao().getById(1);
        assertEquals(75, session.activeDurationSeconds);
        assertEquals(Integer.valueOf(30), session.restSeconds);

        List<WeeklyGoalChange> changes = database.weeklyGoalDao().getAll();
        assertEquals(1, changes.size());
        assertEquals(Integer.valueOf(3), changes.get(0).trainingDays);
    }
}
