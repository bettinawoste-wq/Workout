package com.wob.workout.data.database;

import android.content.Context;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.room.Room;

public class DatabaseProvider {
    private static AppDatabase INSTANCE;

    /** What INSTANCE was before the first {@link #setInstanceForTest(AppDatabase)} call
     *  (typically the on-disk database {@code WorkoutApplication}'s container already
     *  opened), restored by {@link #resetForTest()}. Only meaningful while
     *  {@code overriddenForTest} is true. */
    private static AppDatabase instanceBeforeTest;
    private static boolean overriddenForTest;

    public static synchronized AppDatabase get(Context context) {

        if (INSTANCE == null) {

            INSTANCE =
                    Room.databaseBuilder(
                                    context,
                                    AppDatabase.class,
                                    "workout.db")
                            .addMigrations(AppDatabase.ALL_MIGRATIONS)
                            .addCallback(StarterSet.callback(context))
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
        if (!overriddenForTest) {
            instanceBeforeTest = INSTANCE;
            overriddenForTest = true;
        }
        INSTANCE = database;
    }

    /** Undoes {@link #setInstanceForTest(AppDatabase)}: restores whatever INSTANCE was
     *  before the override (null if nothing had been built yet), rather than clearing it -
     *  otherwise the next {@link #get(Context)} would open a second Room instance of the
     *  on-disk file alongside the one the app-wide container already holds, with separate
     *  invalidation trackers. A no-op if no override is active. Does not close the database
     *  being discarded - the caller that built it owns closing it, typically right before
     *  calling this. */
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.TESTS)
    public static synchronized void resetForTest() {
        if (overriddenForTest) {
            INSTANCE = instanceBeforeTest;
            instanceBeforeTest = null;
            overriddenForTest = false;
        }
    }
}
