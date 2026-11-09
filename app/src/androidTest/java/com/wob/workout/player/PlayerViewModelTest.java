package com.wob.workout.player;

import static com.wob.workout.LiveDataTestUtil.getOrAwaitValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.wob.workout.AppContainer;
import com.wob.workout.MigrationTestHelper;
import com.wob.workout.data.entity.Workout;
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
 * durations so the real timer never finishes an exercise on its own; the few-second
 * transition countdowns are only ever held for a few milliseconds before the next step.
 * That window is the one remaining real-time dependency: a main-thread stall of over 3s
 * between two steps would let the real timer advance a phase first (removing it would need
 * an injectable timer in production code, which this ticket deliberately avoids).
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
                runOnMain(viewModel::onCleared);
            }
        } finally {
            if (helper != null) {
                helper.close();
            }
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
    public void finishFromFinishingRecordsCompletedSession() throws Exception {
        finishCurrentPhase(); // READY -> ACTIVE (0)
        finishCurrentPhase(); // ACTIVE (0) -> TRANSITION (1)
        finishCurrentPhase(); // TRANSITION -> ACTIVE (1)
        finishCurrentPhase(); // ACTIVE (1) -> FINISHING

        PlayerExerciseState state = finishCurrentPhase();

        assertEquals(Phase.FINISHED, state.phase);
        // History writes are fire-and-forget on the IO executor (issue #21).
        helper.drainIoExecutor();
        List<WorkoutSessionSummary> history = getOrAwaitValue(
                helper.getAppContainer().workoutHistoryRepository.observeHistory());
        assertEquals(1, history.size());
        assertTrue(history.get(0).session.completed);
        assertEquals("Push Day", history.get(0).session.workoutName);
        assertEquals(2, history.get(0).exerciseCount);
    }

    @Test
    public void togglePauseWhileActivePausesAndResumesTimer() throws InterruptedException {
        finishCurrentPhase(); // READY -> ACTIVE (0)

        // Read in the same main-thread step as the toggle, so a real timer tick can't
        // replace the emission in between.
        PlayerTick paused = onMain(() -> {
            viewModel.togglePause();
            return viewModel.getTick().getValue();
        });
        assertFalse(paused.running);
        assertEquals(Phase.ACTIVE, paused.phase);
        assertTrue(paused.remainingMillis > 0);
        assertTrue(paused.remainingMillis <= LONG_DURATION_SECONDS * 1000L);

        // Longer than the real timer's 1s tick interval: a still-running countdown would
        // have emitted a new tick by now.
        Thread.sleep(1100);
        assertSame(paused, onMain(() -> viewModel.getTick().getValue()));

        PlayerTick resumed = onMain(() -> {
            viewModel.togglePause();
            return viewModel.getTick().getValue();
        });
        assertTrue(resumed.running);
        assertEquals(Phase.ACTIVE, resumed.phase);
        // Resumes from where it paused rather than restarting the exercise.
        assertEquals(paused.remainingMillis, resumed.remainingMillis);
    }

    @Test
    public void shouldPlayStartSoundReturnsTrueOncePerActiveState() {
        PlayerExerciseState ready = currentState();
        assertFalse(onMain(() -> viewModel.shouldPlayStartSound(ready)));

        PlayerExerciseState active = finishCurrentPhase();

        assertTrue(onMain(() -> viewModel.shouldPlayStartSound(active)));
        // Simulates LiveData redelivering the same value to a recreated Fragment's observer.
        assertFalse(onMain(() -> viewModel.shouldPlayStartSound(active)));
    }

    @Test
    public void shouldPlayBeepReturnsTrueOncePerBeepingTick() {
        finishCurrentPhase(); // READY -> ACTIVE (0)

        PlayerTick silent = tickAt(10_000);
        assertFalse(onMain(() -> viewModel.shouldPlayBeep(silent)));

        PlayerTick beeping = tickAt(2500);
        assertTrue(beeping.shouldBeep);
        assertTrue(onMain(() -> viewModel.shouldPlayBeep(beeping)));
        // Simulates LiveData redelivering the same value to a recreated Fragment's observer.
        assertFalse(onMain(() -> viewModel.shouldPlayBeep(beeping)));

        // The next second's beep is a new value, so it plays again.
        PlayerTick nextBeeping = tickAt(1500);
        assertTrue(onMain(() -> viewModel.shouldPlayBeep(nextBeeping)));
    }

    @Test
    public void shouldPlaySuccessSoundOnlyOnceAndOnlyWhenFinished() {
        PlayerExerciseState active = finishCurrentPhase(); // READY -> ACTIVE (0)
        assertFalse(onMain(() -> viewModel.shouldPlaySuccessSound(active)));
        finishCurrentPhase(); // ACTIVE (0) -> TRANSITION (1)
        finishCurrentPhase(); // TRANSITION -> ACTIVE (1)
        finishCurrentPhase(); // ACTIVE (1) -> FINISHING

        PlayerExerciseState finished = finishCurrentPhase();

        assertTrue(onMain(() -> viewModel.shouldPlaySuccessSound(finished)));
        // Simulates LiveData redelivering the same value to a recreated Fragment's observer.
        assertFalse(onMain(() -> viewModel.shouldPlaySuccessSound(finished)));
    }

    @Test
    public void summaryCountsAnExerciseSteppedAwayFromAsSkipped() {
        finishCurrentPhase(); // READY -> ACTIVE (0)
        runOnMain(viewModel::next); // skips exercise 0
        finishCurrentPhase(); // ACTIVE (1) -> FINISHING

        PlayerExerciseState finished = finishCurrentPhase();

        assertEquals(Phase.FINISHED, finished.phase);
        assertEquals(1, finished.summary.doneExercises);
        assertEquals(1, finished.summary.skippedExercises());
    }

    @Test
    public void summaryCountsASkippedExerciseAsDoneOnceItsTimeRanOutOnReturn() {
        finishCurrentPhase(); // READY -> ACTIVE (0)
        runOnMain(viewModel::next); // skips exercise 0
        runOnMain(viewModel::previous); // back to exercise 0
        finishCurrentPhase(); // ACTIVE (0) -> TRANSITION (1)
        finishCurrentPhase(); // TRANSITION -> ACTIVE (1)
        finishCurrentPhase(); // ACTIVE (1) -> FINISHING

        PlayerExerciseState finished = finishCurrentPhase();

        assertEquals(2, finished.summary.doneExercises);
        assertEquals(0, finished.summary.skippedExercises());
    }

    @Test
    public void ticksCarryTheRemainingTimeOfTheWholeWorkout() {
        finishCurrentPhase(); // READY -> ACTIVE (0)

        PlayerTick tick = tickAt(10_000);

        long transition = new Workout().transitionSeconds * 1000L;
        long secondExercise = LONG_DURATION_SECONDS * 1000L;
        assertEquals(10_000 + transition + secondExercise, tick.workoutRemainingMillis);
        assertEquals(2 * secondExercise + transition, tick.workoutTotalMillis);
    }

    @Test
    public void theWorkoutsOwnPauseLengthIsUsedBetweenExercises() throws InterruptedException {
        AppContainer container = helper.getAppContainer();
        Workout workout = new Workout();
        workout.name = "Long Rest";
        workout.transitionSeconds = 9;
        long workoutId = container.workoutRepository.save(workout);
        helper.insertWorkoutExercise(workoutId, helper.insertExercise("A"), 0, LONG_DURATION_SECONDS);
        helper.insertWorkoutExercise(workoutId, helper.insertExercise("B"), 1, LONG_DURATION_SECONDS);

        PlayerViewModel other = onMain(() -> new PlayerViewModel(container, workoutId));
        try {
            getOrAwaitValue(other.getExerciseState());

            PlayerTick tick = onMain(() -> {
                other.onFinish(); // READY -> ACTIVE (0)
                other.onFinish(); // ACTIVE (0) -> TRANSITION (1)
                other.onTick(9000);
                return other.getTick().getValue();
            });

            assertEquals(Phase.TRANSITION, tick.phase);
            assertEquals(LONG_DURATION_SECONDS * 2000L + 9000, tick.workoutTotalMillis);
        } finally {
            runOnMain(other::onCleared);
        }
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
        runOnMain(() -> result.set(action.get()));
        return result.get();
    }

    private static void runOnMain(Runnable action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }
}
