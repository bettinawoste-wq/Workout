package com.wob.workout.player;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.wob.workout.data.database.AppDatabase;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.ExerciseSession;
import com.wob.workout.data.repository.WorkoutHistoryRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PlayerHistoryTracker is the one part of the player migration worth a real unit test —
 * pure state plus one repository dependency, no Android framework classes involved. See
 * wayfinder ticket #4.
 *
 * <p>Every tracker call is made on the main thread, against a database built
 * <em>without</em> {@code allowMainThreadQueries()} - exactly as in production since
 * issue #21 - so a repository write that slipped back onto the calling thread fails here
 * with Room's "Cannot access database on the main thread". Assertions read the database
 * from the test thread, after {@link #drainWrites()}.
 */
@RunWith(AndroidJUnit4.class)
public class PlayerHistoryTrackerTest {

    private AppDatabase database;
    private ExecutorService ioExecutor;
    private PlayerHistoryTracker tracker;
    private long sessionExerciseId;

    @Before
    public void createDatabase() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        ioExecutor = Executors.newSingleThreadExecutor();

        WorkoutHistoryRepository historyRepository = new WorkoutHistoryRepository(database);
        tracker = new PlayerHistoryTracker(historyRepository, ioExecutor);

        Exercise exercise = new Exercise();
        exercise.title = "Push-up";
        sessionExerciseId = database.exerciseDao().insert(exercise);

        // A fresh in-memory database, so this is the first row and its id is deterministically 1.
        onMain(() -> tracker.startSession(1, "Push Day"));
    }

    @After
    public void closeDatabase() throws Exception {
        drainWrites();
        ioExecutor.shutdownNow();
        database.close();
    }

    @Test
    public void pausedTimeIsExcludedFromRecordedActiveSeconds() throws Exception {
        Exercise exercise = new Exercise();
        exercise.id = sessionExerciseId;
        exercise.title = "Push-up";
        PlayerExercise current = new PlayerExercise(exercise, 60);

        onMain(tracker::startExercise);
        Thread.sleep(1100);
        onMain(tracker::onExercisePause);
        Thread.sleep(1000);
        onMain(tracker::onExerciseResume);
        Thread.sleep(1100);
        onMain(() -> tracker.closeOutCurrentExercise(0, current));
        drainWrites();

        List<ExerciseSession> recorded = database.exerciseSessionDao().getBySession(1);
        assertEquals(1, recorded.size());

        // ~2.2s active across two segments, ~1s paused in between excluded. A generous
        // range absorbs scheduling jitter while still proving the pause wasn't counted
        // (if it were, this would be >= 3).
        assertTrue(recorded.get(0).actualDurationSeconds >= 1);
        assertTrue(recorded.get(0).actualDurationSeconds <= 2);
    }

    @Test
    public void finishIsIdempotentAndOnlyRecordsOnce() throws Exception {
        onMain(() -> tracker.finish(true));
        onMain(() -> tracker.finish(false));
        drainWrites();

        assertTrue(database.workoutSessionDao().getById(1).completed);
    }

    private static void onMain(Runnable action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }

    /** The executor is single-threaded, so once this no-op task has run every write
     *  submitted before it has too. */
    private void drainWrites() throws Exception {
        ioExecutor.submit(() -> { }).get();
    }
}
