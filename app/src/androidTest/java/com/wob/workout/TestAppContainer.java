package com.wob.workout;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.wob.workout.data.database.AppDatabase;
import com.wob.workout.data.database.DatabaseProvider;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.data.entity.WorkoutExercise;
import com.wob.workout.data.entity.WorkoutSession;

import java.util.concurrent.TimeUnit;

/**
 * Shared scaffold for ViewModel tests: a real {@link AppContainer} wired to a fresh,
 * isolated in-memory {@link AppDatabase}. Replaces the in-memory-database
 * {@code @Before}/{@code @After} boilerplate that {@code WorkoutListViewModelTest}
 * established inline, and additionally covers ViewModels that need a whole
 * {@code AppContainer} rather than one repository.
 *
 * <p>Called {@code MigrationTestHelper} in issues #10 and #11 (after the MVVM migration);
 * renamed so it is not confused with Room's {@code androidx.room.testing.MigrationTestHelper},
 * which the migration tests use (ADR 0004).
 *
 * <p>How it works: the in-memory database is installed into {@link DatabaseProvider} via
 * its test seam (issue #10) <em>before</em> the container is constructed, so every
 * repository - including the ones that call {@code DatabaseProvider.get(context)} on each
 * operation - talks to the same database, exactly as in production.
 *
 * <p>Usage:
 * <pre>
 * &#64;Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();
 * private TestAppContainer testContainer;
 *
 * &#64;Before public void setUp()    { testContainer = new TestAppContainer(); }
 * &#64;After  public void tearDown() { testContainer.close(); }
 *
 * // new SomeViewModel(testContainer.getAppContainer()), seed with testContainer.insertWorkout(...)
 * </pre>
 *
 * <p><b>Always call {@link #close()} from {@code @After}</b> (JUnit runs it even when the
 * test fails): {@code DatabaseProvider}'s singleton is shared by the whole
 * instrumentation process, so skipping it would leak this database into the next test.
 * Construct at most one test container at a time for the same reason.
 * {@code InstantTaskExecutorRule} stays in the test class, since it is a JUnit rule and this
 * is a plain object (like
 * {@link LiveDataTestUtil}).
 */
public class TestAppContainer {

    /** Guards the one-at-a-time rule: a second live test container would silently re-point
     *  the first one's per-call repositories at its own database. */
    private static boolean active;

    private final AppDatabase database;
    private final AppContainer appContainer;
    private boolean closed;

    /** @throws IllegalStateException if another test container hasn't been closed yet. */
    public TestAppContainer() {
        synchronized (TestAppContainer.class) {
            if (active) {
                throw new IllegalStateException(
                        "Another TestAppContainer is still open - close() it first.");
            }
            active = true;
        }
        Context context = ApplicationProvider.getApplicationContext();
        AppDatabase db = null;
        try {
            // No allowMainThreadQueries(), same as production since issue #21: a ViewModel
            // that touches the database on the main thread fails its test instead of
            // crashing only on the device. Test bodies run on the instrumentation thread,
            // so seeding and assertions are unaffected.
            db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
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

    /** Blocks until every task already submitted to the container's {@code ioExecutor} has
     *  run - for asserting on fire-and-forget writes (e.g. {@code PlayerHistoryTracker}).
     *  Works because that executor is single-threaded. */
    public void drainIoExecutor() throws Exception {
        appContainer.ioExecutor.submit(() -> { }).get();
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

    /** Records a workout session that started at {@code startedAt}, as the player would. */
    public long insertSession(long workoutId, long startedAt, boolean completed) {
        WorkoutSession session = new WorkoutSession();
        session.workoutId = workoutId;
        session.workoutName = "Session of " + workoutId;
        session.startedAt = startedAt;
        session.finishedAt = startedAt + 60_000;
        session.completed = completed;
        return database.workoutSessionDao().insert(session);
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
            // Let already-queued writes (e.g. PlayerHistoryTracker's fire-and-forget ones)
            // finish before the database closes. shutdownNow() would interrupt a running
            // Room call, which rethrows the InterruptedException on the executor thread and
            // kills the instrumentation process.
            appContainer.ioExecutor.shutdown();
            try {
                if (!appContainer.ioExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    appContainer.ioExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                appContainer.ioExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            database.close();
        } finally {
            DatabaseProvider.resetForTest();
            release();
        }
    }
}
