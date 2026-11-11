package com.wob.workout.data.database;

import androidx.room.Room;
import androidx.room.testing.MigrationTestHelper;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

/**
 * Migrates the oldest database any release has shipped (version 4, see ADR 0004) to the
 * current version with exactly the migrations {@link DatabaseProvider} registers. Fails when
 * a version bump comes without its migration, or with one that is missing from
 * {@link AppDatabase#ALL_MIGRATIONS}: Room then finds no path, or the result does not
 * match the current schema.
 */
@RunWith(AndroidJUnit4.class)
public class AllMigrationsTest {

    private static final String DB_NAME = "all-migrations-test.db";
    private static final int OLDEST_RELEASED_VERSION = 4;

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), AppDatabase.class);

    @Test
    public void oldestReleasedVersionMigratesToTheCurrentSchema() throws IOException {
        helper.createDatabase(DB_NAME, OLDEST_RELEASED_VERSION).close();

        AppDatabase database = Room.databaseBuilder(
                        ApplicationProvider.getApplicationContext(), AppDatabase.class, DB_NAME)
                .addMigrations(AppDatabase.ALL_MIGRATIONS)
                .build();
        helper.closeWhenFinished(database);

        // Opening runs the migrations; Room then checks the result against the current
        // entities and throws if they do not match.
        database.getOpenHelper().getWritableDatabase();
    }
}
