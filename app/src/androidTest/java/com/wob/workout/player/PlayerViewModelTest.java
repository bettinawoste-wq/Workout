package com.wob.workout.player;

import static com.wob.workout.LiveDataTestUtil.getOrAwaitValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.wob.workout.AppContainer;
import com.wob.workout.MigrationTestHelper;
import com.wob.workout.player.PlayerExerciseState.Phase;
import com.wob.workout.ui.history.WorkoutSessionSummary;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Covers {@link PlayerViewModel}'s state machine (the logic wayfinder ticket #4 moved out of
 * the Fragment) against a real {@link AppContainer} on an in-memory database.
 *
 * <p>The state machine is driven by calling the ViewModel's public
 * {@link PlayerTimer.Listener} methods ({@code onTick}/{@code onFinish}) directly instead of
 * waiting on the real countdown, so the tests are fast. Exercises are seeded with long
 * durations so the real timer never finishes an exercise on its own; the fixed 3-second
 * transition countdowns are only ever held for a few milliseconds before the next step.
 *
 * <p><b>Why there is no {@code InstantTaskExecutorRule} here</b>, unlike the other
 * ViewModel tests: {@link PlayerTimer} creates a real {@link android.os.CountDownTimer},
 * whose {@link android.os.Handler} needs a {@link android.os.Looper} on the creating
 * thread. With the rule, the async load's {@code postValue} would deliver synchronously on
 * the IO executor thread (no Looper) and crash when the first phase starts the timer.
 * Instead every ViewModel interaction runs on the main thread via {@link #onMain}, exactly
 * like in production, which also serialises the test's calls with the real timer's
 * callbacks.
 */
@RunWith(AndroidJUnit4.class)
public class PlayerViewModelTest {

    /** Long enough that the real exercise countdown can never finish during a test. */
    private static final int LONG_DURATION_SECONDS = 600;

    private MigrationTestHelper helper;
    private PlayerViewModel viewModel;

    @Before
    public void setUp() throws InterruptedException {
        helper = new MigrationTestHelper();
        long workoutId = helper.insertWorkout("Push Day");
        long pushUpId = helper.insertExercise("Push-up");
        long squatId = helper.insertExercise("Squat");
        helper.insertWorkoutExercise(workoutId, pushUpId, 0, LONG_DURATION_SECONDS);
        helper.insertWorkoutExercise(workoutId, squatId, 1, LONG_DURATION_SECONDS);

        AppContainer container = helper.getAppContainer();
        // The constructor observes the load's LiveData, which must happen on the main thread.
        viewModel = onMain(() -> new PlayerViewModel(container, workoutId));
        // Waits for the async load; asserted in initialLoadEntersReadyForFirstExercise.
        getOrAwaitValue(viewModel.getExerciseState());
    }

    @After
    public void tearDown() {
        try {
            if (viewModel != null) {
                // Cancels the real timer so it can't fire into the next test.
                onMain(() -> {
                    viewModel.onCleared();
                    return null;
                });
            }
        } finally {
            helper.close();
        }
    }

    @Test
    public void initialLoadEntersReadyForFirstExercise() {
        PlayerExerciseState state = currentState();

        assertEquals(Phase.READY, state.phase);
        assertEquals(0, state.index);
        assertEquals(2, state.total);
        assertEquals("Push-up", state.exercise.exercise.title);
    }

    @Test
    public void finishFromReadyBeginsFirstExercise() {
        PlayerExerciseState state = finishCurrentPhase();

        assertEquals(Phase.ACTIVE, state.phase);
        assertEquals(0, state.index);
        assertEquals("Push-up", state.exercise.exercise.title);
    }

    @Test
    public void finishFromActiveOnNonLastExerciseTransitionsToNextExercise() {
        finishCurrentPhase(); // READY -> ACTIVE (0)

        PlayerExerciseState state = finishCurrentPhase();

        assertEquals(Phase.TRANSITION, state.phase);
        assertEquals(1, state.index);
        assertEquals("Squat", state.exercise.exercise.title);
    }

    @Test
    public void finishFromActiveOnLastExerciseStartsFinishing() {
        finishCurrentPhase(); // READY -> ACTIVE (0)
        finishCurrentPhase(); // ACTIVE (0) -> TRANSITION (1)
        PlayerExerciseState lastActive = finishCurrentPhase(); // TRANSITION -> ACTIVE (1)
        assertEquals(Phase.ACTIVE, lastActive.phase);
        assertEquals(1, lastActive.index);

        PlayerExerciseState state = finishCurrentPhase();

        assertEquals(Phase.FINISHING, state.phase);
        assertNull(state.exercise);
    }

    @Test
    public void finishFromFinishingRecordsCompletedSession() throws InterruptedException {
        finishCurrentPhase(); // READY -> ACTIVE (0)
        finishCurrentPhase(); // ACTIVE (0) -> TRANSITION (1)
        finishCurrentPhase(); // TRANSITION -> ACTIVE (1)
        finishCurrentPhase(); // ACTIVE (1) -> FINISHING

        PlayerExerciseState state = finishCurrentPhase();

        assertEquals(Phase.FINISHED, state.phase);
        List<WorkoutSessionSummary> history = getOrAwaitValue(
                helper.getAppContainer().workoutHistoryRepository.observeHistory());
        assertEquals(1, history.size());
        assertTrue(history.get(0).session.completed);
        assertEquals("Push Day", history.get(0).session.workoutName);
        assertEquals(2, history.get(0).exerciseCount);
    }

    @Test
    public void togglePauseWhileActivePausesAndResumesTimer() {
        finishCurrentPhase(); // READY -> ACTIVE (0)

        // Read in the same main-thread step as the toggle, so a real timer tick can't
        // replace the emission in between.
        PlayerTick paused = onMain(() -> {
            viewModel.togglePause();
            return viewModel.getTick().getValue();
        });
        assertFalse(paused.running);
        assertEquals(Phase.ACTIVE, paused.phase);

        PlayerTick resumed = onMain(() -> {
            viewModel.togglePause();
            return viewModel.getTick().getValue();
        });
        assertTrue(resumed.running);
        assertEquals(Phase.ACTIVE, resumed.phase);
    }

    @Test
    public void shouldPlayStartSoundReturnsTrueOncePerActiveState() {
        PlayerExerciseState ready = currentState();
        assertFalse(viewModel.shouldPlayStartSound(ready));

        PlayerExerciseState active = finishCurrentPhase();

        assertTrue(viewModel.shouldPlayStartSound(active));
        // Simulates LiveData redelivering the same value to a recreated Fragment's observer.
        assertFalse(viewModel.shouldPlayStartSound(active));
    }

    @Test
    public void shouldPlayBeepReturnsTrueOncePerBeepingTick() {
        finishCurrentPhase(); // READY -> ACTIVE (0)

        PlayerTick beeping = tickAt(2500);
        assertTrue(beeping.shouldBeep);

        assertTrue(viewModel.shouldPlayBeep(beeping));
        // Simulates LiveData redelivering the same value to a recreated Fragment's observer.
        assertFalse(viewModel.shouldPlayBeep(beeping));
    }

    /** Calls {@code onFinish()} as the timer would and returns the resulting state. */
    private PlayerExerciseState finishCurrentPhase() {
        return onMain(() -> {
            viewModel.onFinish();
            return viewModel.getExerciseState().getValue();
        });
    }

    /** Calls {@code onTick()} as the timer would and returns the resulting tick. */
    private PlayerTick tickAt(long millisUntilFinished) {
        return onMain(() -> {
            viewModel.onTick(millisUntilFinished);
            return viewModel.getTick().getValue();
        });
    }

    private PlayerExerciseState currentState() {
        return onMain(() -> viewModel.getExerciseState().getValue());
    }

    /** Runs {@code action} on the main thread and returns its result. */
    private static <T> T onMain(Supplier<T> action) {
        AtomicReference<T> result = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> result.set(action.get()));
        return result.get();
    }
}
