package com.wob.workout.player;

import static com.wob.workout.LiveDataTestUtil.getOrAwaitValue;
import static com.wob.workout.LiveDataTestUtil.observeValues;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.wob.workout.AppContainer;
import com.wob.workout.TestAppContainer;
import com.wob.workout.data.Event;
import com.wob.workout.data.entity.ExerciseSession;
import com.wob.workout.ui.history.WorkoutSessionSummary;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * The player's real wiring: {@link PlayerViewModel} loading from the database, running
 * {@link WorkoutSessionEngine} on {@link AndroidSessionTime} and writing through
 * {@link DatabaseSessionHistory}. The rules themselves are covered on the JVM with fake time
 * ({@code WorkoutSessionEngineTest}); this test runs one short workout in real time, with
 * the sound setting on (a fresh test container's default).
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
        BlockingQueue<PlayerScreen> screens = observeValues(viewModel.getScreen());
        BlockingQueue<Event<PlayerSound>> sounds = observeValues(viewModel.getSound());

        assertEquals("ERSTE ÜBUNG", next(screens).mediaLabel);
        PlayerScreen finished = next(screens);
        while (!finished.finished) finished = next(screens);
        assertEquals("Übung erledigt", finished.description);
        // Three beeps into the exercise, its start, the end of the workout.
        List<PlayerSound> played = new ArrayList<>();
        for (Event<PlayerSound> event : sounds) played.add(event.getContentIfNotHandled());
        assertEquals(Arrays.asList(PlayerSound.BEEP, PlayerSound.BEEP, PlayerSound.BEEP,
                PlayerSound.START, PlayerSound.SUCCESS), played);

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

    @Test
    public void aWorkoutWithoutExercisesReportsThatThereIsNothingToPlay() throws Exception {
        long workoutId = testContainer.insertWorkout("Empty");
        AppContainer container = testContainer.getAppContainer();

        viewModel = onMain(() -> new PlayerViewModel(container, workoutId));
        BlockingQueue<Event<Boolean>> nothingToPlay =
                observeValues(viewModel.getNothingToPlay());

        Event<Boolean> event = nothingToPlay.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertNotNull("No nothing-to-play event", event);
        assertTrue(event.getContentIfNotHandled());
        assertNull(onMain(() -> viewModel.getScreen().getValue()));
    }

    @Test
    public void whileTheSoundIsOffNoSoundReachesTheScreen() throws Exception {
        long workoutId = testContainer.insertWorkout("Quiet");
        testContainer.insertWorkoutExercise(
                workoutId, testContainer.insertExercise("Plank"), 0, 1);
        AppContainer container = testContainer.getAppContainer();
        assertTrue(container.soundSetting.isOn()); // on by default

        viewModel = onMain(() -> new PlayerViewModel(container, workoutId));
        runOnMain(viewModel::toggleSound);
        assertFalse(onMain(() -> viewModel.isSoundOn().getValue()));
        BlockingQueue<PlayerScreen> screens = observeValues(viewModel.getScreen());
        BlockingQueue<Event<PlayerSound>> sounds = observeValues(viewModel.getSound());

        PlayerScreen screen = next(screens);
        while (!screen.finished) screen = next(screens);

        assertTrue(sounds.isEmpty());
    }

    @Test
    public void theSoundSwitchedOffStaysOffForTheNextWorkout() {
        long workoutId = testContainer.insertWorkout("Quiet");
        AppContainer container = testContainer.getAppContainer();
        viewModel = onMain(() -> new PlayerViewModel(container, workoutId));
        runOnMain(viewModel::toggleSound);

        PlayerViewModel nextWorkout = onMain(() -> new PlayerViewModel(container, workoutId));
        try {
            assertFalse(onMain(() -> nextWorkout.isSoundOn().getValue()));
            runOnMain(nextWorkout::toggleSound);
            assertTrue(container.soundSetting.isOn());
        } finally {
            runOnMain(nextWorkout::onCleared);
        }
    }

    private static PlayerScreen next(BlockingQueue<PlayerScreen> screens)
            throws InterruptedException {
        PlayerScreen screen = screens.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertNotNull("No further screen", screen);
        return screen;
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
