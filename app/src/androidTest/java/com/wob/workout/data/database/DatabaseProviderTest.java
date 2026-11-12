package com.wob.workout.data.database;

import static org.junit.Assert.assertSame;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;

/**
 * Covers the test-only seam added for issue #10 (TestAppContainer's foundation) -
 * every other DatabaseProvider.get(context) call site is exercised, untouched, by the
 * app's existing tests, so this only needs to prove the seam itself behaves.
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseProviderTest {

    private AppDatabase inMemory;

    @Before
    public void createDatabase() {
        Context context = ApplicationProvider.getApplicationContext();
        inMemory = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void closeDatabaseAndResetSingleton() {
        // Runs even if the test body's assertion throws, unlike a close() call at the end
        // of the test method - the in-memory connection must not outlive the test either
        // way, since DatabaseProvider.INSTANCE is shared for the whole instrumentation
        // process.
        inMemory.close();
        DatabaseProvider.resetForTest();
    }

    @Test
    public void setInstanceForTest_makesGetReturnItWithoutBuildingARealDatabase() {
        Context context = ApplicationProvider.getApplicationContext();

        DatabaseProvider.setInstanceForTest(inMemory);

        assertSame(inMemory, DatabaseProvider.get(context));
    }

    @Test
    public void resetForTest_restoresTheInstanceFromBeforeTheOverride() throws Exception {
        AppDatabase before = readInstanceField();
        DatabaseProvider.setInstanceForTest(inMemory);
        // Nested overrides still restore the original.
        DatabaseProvider.setInstanceForTest(inMemory);

        DatabaseProvider.resetForTest();

        assertSame(before, readInstanceField());
    }

    /** DatabaseProvider intentionally exposes no getter for INSTANCE beyond the seam issue #10
     *  scoped - reading the private field via reflection here avoids adding extra test-only
     *  API surface just to assert this one fact. */
    private AppDatabase readInstanceField() throws Exception {
        Field field = DatabaseProvider.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        return (AppDatabase) field.get(null);
    }
}
