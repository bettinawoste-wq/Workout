package com.wob.workout;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.wob.workout.data.database.AppDatabase;
import com.wob.workout.data.database.DatabaseProvider;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.data.entity.WorkoutExercise;

/**
 * Shared scaffold for ViewModel tests: a real {@link AppContainer} wired to a fresh,
 * isolated in-memory {@link AppDatabase}. Replaces the in-memory-database
 * {@code @Before}/{@code @After} boilerplate that {@code WorkoutListViewModelTest}
 * established inline, and additionally covers ViewModels that need a whole
 * {@code AppContainer} rather than one repository.
 *
 * <p>How it works: the in-memory database is installed into {@link DatabaseProvider} via
 * its test seam (issue #10) <em>before</em> the container is constructed, so every
 * repository - including the ones that call {@code DatabaseProvider.get(context)} on each
 * operation - talks to the same database, exactly as in production.
 *
 * <p>Usage:
 * <pre>
 * &#64;Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();
 * private MigrationTestHelper helper;
 *
 * &#64;Before public void setUp()    { helper = new MigrationTestHelper(); }
 * &#64;After  public void tearDown() { helper.close(); }
 *
 * // new SomeViewModel(helper.getAppContainer()), seed with helper.insertWorkout(...)
 * </pre>
 *
 * <p><b>Always call {@link #close()} from {@code @After}</b> (JUnit runs it even when the
 * test fails): {@code DatabaseProvider}'s singleton is shared by the whole
 * instrumentation process, so skipping it would leak this database into the next test.
 * Construct at most one helper at a time for the same reason. {@code InstantTaskExecutorRule}
 * stays in the test class, since it is a JUnit rule and this is a plain object (like
 * {@link LiveDataTestUtil}).
 */
public class MigrationTestHelper {

    /** Guards the one-helper-at-a-time rule: a second live helper would silently re-point
     *  the first one's per-call repositories at its own database. */
    private static boolean active;

    private final AppDatabase database;
    private final AppContainer appContainer;
    private boolean closed;

    /** @throws IllegalStateException if another helper hasn't been closed yet. */
    public MigrationTestHelper() {
        synchronized (MigrationTestHelper.class) {
            if (active) {
                throw new IllegalStateException(
                        "Another MigrationTestHelper is still open - close() it first.");
            }
            active = true;
        }
        Context context = ApplicationProvider.getApplicationContext();
        AppDatabase db = null;
        try {
            db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                    .allowMainThreadQueries()
                    .build();
            // Must precede the container: WorkoutSummaryRepository/WorkoutHistoryRepository
            // capture DatabaseProvider.get() once, at construction.
            DatabaseProvider.setInstanceForTest(db);
            appContainer = new AppContainer(context);
        } catch (RuntimeException | Error e) {
            // No object reaches the caller, so nobody could close() it - undo here.
            if (db != null) {
                db.close();
            }
            DatabaseProvider.resetForTest();
            release();
            throw e;
        }
        database = db;
    }

    private static synchronized void release() {
        active = false;
    }

    public AppContainer getAppContainer() {
        return appContainer;
    }

    /** For DAO-level seeding or assertions the repositories don't expose. */
    public AppDatabase getDatabase() {
        return database;
    }

    public long insertWorkout(String name) {
        Workout workout = new Workout();
        workout.name = name;
        return appContainer.workoutRepository.save(workout);
    }

    public long insertExercise(String title) {
        Exercise exercise = new Exercise();
        exercise.title = title;
        return appContainer.exerciseRepository.save(exercise);
    }

    public long insertWorkoutExercise(long workoutId, long exerciseId, int position,
                                      int durationSeconds) {
        WorkoutExercise workoutExercise = new WorkoutExercise();
        workoutExercise.workoutId = workoutId;
        workoutExercise.exerciseId = exerciseId;
        workoutExercise.position = position;
        workoutExercise.durationSeconds = durationSeconds;
        return appContainer.workoutExerciseRepository.save(workoutExercise);
    }

    /** Stops the container's executor, closes the database and restores
     *  {@code DatabaseProvider}'s previous instance. Safe to call more than once; the
     *  singleton is restored even if an earlier teardown step throws. */
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            appContainer.ioExecutor.shutdownNow();
            database.close();
        } finally {
            DatabaseProvider.resetForTest();
            release();
        }
    }
}
