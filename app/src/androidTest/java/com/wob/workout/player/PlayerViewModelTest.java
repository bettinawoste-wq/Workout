package com.wob.workout.player;

import static com.wob.workout.LiveDataTestUtil.getOrAwaitValue;
import static com.wob.workout.LiveDataTestUtil.observeValues;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.wob.workout.AppContainer;
import com.wob.workout.TestAppContainer;
import com.wob.workout.data.entity.ExerciseSession;
import com.wob.workout.player.PlayerExerciseState.Phase;
import com.wob.workout.ui.history.WorkoutSessionSummary;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * The player's real wiring: {@link PlayerViewModel} loading from the database, running
 * {@link WorkoutSessionEngine} on {@link AndroidSessionTime} and writing through
 * {@link DatabaseSessionHistory}. The rules themselves are covered on the JVM with fake time
 * ({@code WorkoutSessionEngineTest}); this test runs one short workout in real time.
 *
 * <p>No {@code InstantTaskExecutorRule}: the real {@link android.os.CountDownTimer} needs the
 * main thread's Looper, so the ViewModel is created and cleared on the main thread, exactly
 * like in production.
 */
@RunWith(AndroidJUnit4.class)
public class PlayerViewModelTest {

    /** The countdown before the exercise, the exercise, and room for a slow device. */
    private static final long TIMEOUT_SECONDS = 15;

    private TestAppContainer testContainer;
    private PlayerViewModel viewModel;

    @Before
    public void setUp() {
        testContainer = new TestAppContainer();
    }

    @After
    public void tearDown() {
        try {
            if (viewModel != null) {
                runOnMain(viewModel::onCleared);
            }
        } finally {
            testContainer.close();
        }
    }

    @Test
    public void aShortWorkoutRunsToTheEndAndIsRecordedAsCompleted() throws Exception {
        long workoutId = testContainer.insertWorkout("Quick");
        long plankId = testContainer.insertExercise("Plank");
        testContainer.insertWorkoutExercise(workoutId, plankId, 0, 1);
        AppContainer container = testContainer.getAppContainer();

        viewModel = onMain(() -> new PlayerViewModel(container, workoutId));
        BlockingQueue<PlayerExerciseState> states = observeValues(viewModel.getExerciseState());

        assertEquals(Phase.READY, next(states).phase);
        assertEquals(Phase.ACTIVE, next(states).phase);
        PlayerExerciseState finished = next(states);
        assertEquals(Phase.FINISHED, finished.phase);
        assertEquals(1, finished.summary.doneExercises);

        // History writes are fire-and-forget on the IO executor (issue #21).
        testContainer.drainIoExecutor();
        List<WorkoutSessionSummary> history =
                getOrAwaitValue(container.workoutHistoryRepository.observeHistory());
        assertEquals(1, history.size());
        assertTrue(history.get(0).session.completed);
        assertEquals("Quick", history.get(0).session.workoutName);
        List<ExerciseSession> exercises =
                container.workoutHistoryRepository.getExercises(history.get(0).session.id);
        assertEquals(1, exercises.size());
        assertEquals("Plank", exercises.get(0).exerciseTitle);
        assertEquals(1, exercises.get(0).plannedDurationSeconds);
    }

    private static PlayerExerciseState next(BlockingQueue<PlayerExerciseState> states)
            throws InterruptedException {
        PlayerExerciseState state = states.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertNotNull("No further state", state);
        return state;
    }

    /** Runs {@code action} on the main thread and returns its result. */
    private static <T> T onMain(Supplier<T> action) {
        AtomicReference<T> result = new AtomicReference<>();
        runOnMain(() -> result.set(action.get()));
        return result.get();
    }

    private static void runOnMain(Runnable action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }
}
