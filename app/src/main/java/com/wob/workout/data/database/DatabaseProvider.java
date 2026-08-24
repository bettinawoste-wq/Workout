package com.wob.workout.data.database;

import android.content.Context;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.room.Room;

public class DatabaseProvider {
    private static AppDatabase INSTANCE;

    public static synchronized AppDatabase get(Context context) {

        if (INSTANCE == null) {

            INSTANCE =
                    Room.databaseBuilder(
                                    context,
                                    AppDatabase.class,
                                    "workout.db")
                            .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
                            // TODO später entfernen
                            .allowMainThreadQueries()
                            .build();
        }

        return INSTANCE;
    }

    /** Points every future {@link #get(Context)} call at {@code database} instead of the
     *  real on-disk singleton, so tests can wire an in-memory database through unmodified
     *  production code (e.g. {@code AppContainer}) - but only for an {@code AppContainer}
     *  (or repository) constructed <em>after</em> this call. {@code WorkoutSummaryRepository}
     *  and {@code WorkoutHistoryRepository} cache the {@link AppDatabase} reference they get
     *  at construction, so an already-existing instance (e.g. the app-wide one
     *  {@code WorkoutApplication} builds at process startup) keeps talking to whatever
     *  database it already had. Callers own the {@code database} they pass in: closing it,
     *  if needed, is the caller's responsibility, not this class's. Must be paired with
     *  {@link #resetForTest()} in teardown, since INSTANCE is otherwise shared for the
     *  whole process. */
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.TESTS)
    public static synchronized void setInstanceForTest(AppDatabase database) {
        INSTANCE = database;
    }

    /** Clears the singleton set by {@link #setInstanceForTest(AppDatabase)}, so the next
     *  {@link #get(Context)} call rebuilds normally. Does not close the database being
     *  discarded - the caller that built it (via {@link #setInstanceForTest(AppDatabase)})
     *  owns closing it, typically right before calling this. */
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.TESTS)
    public static synchronized void resetForTest() {
        INSTANCE = null;
    }
}
