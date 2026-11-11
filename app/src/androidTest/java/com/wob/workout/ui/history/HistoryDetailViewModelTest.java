package com.wob.workout.ui.history;

import static com.wob.workout.LiveDataTestUtil.getOrAwaitValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.wob.workout.AppContainer;
import com.wob.workout.TestAppContainer;
import com.wob.workout.data.entity.ExerciseSession;
import com.wob.workout.data.entity.WorkoutSession;
import com.wob.workout.data.repository.WorkoutHistoryRepository;
import com.wob.workout.ui.history.HistoryDetailViewModel.HistoryDetail;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Covers {@link HistoryDetailViewModel}'s single one-shot load: the constructor reads
 * {@code getSession()} and {@code getExercises()} together on the container's executor and
 * emits them as one {@link HistoryDetail}. A missing session (bad id, or deleted from the
 * history list) emits nothing at all rather than a half-filled detail. See wayfinder
 * ticket #9 and the ViewModel's class Javadoc.
 */
@RunWith(AndroidJUnit4.class)
public class HistoryDetailViewModelTest {

    private static final long TIMEOUT_SECONDS = 2;

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private TestAppContainer helper;
    private AppContainer container;
    private WorkoutHistoryRepository history;

    @Before
    public void setUp() {
        helper = new TestAppContainer();
        container = helper.getAppContainer();
        history = container.workoutHistoryRepository;
    }

    @After
    public void tearDown() {
        helper.close();
    }

    @Test
    public void constructorLoadsSessionHeaderAndExerciseRowsTogether()
            throws InterruptedException {
        long sessionId = history.startSession(7, "Leg Day");
        // Recorded out of order to show the rows follow position, not insertion order.
        history.recordExercise(sessionId, 2, "Lunge", 1, 30, 25, 3_000, 4_000);
        history.recordExercise(sessionId, 1, "Squat", 0, 45, 40, 1_000, 2_000);
        history.finishSession(sessionId, true);

        HistoryDetailViewModel viewModel = new HistoryDetailViewModel(container, sessionId);
        HistoryDetail detail = getOrAwaitValue(viewModel.getDetail());

        WorkoutSession session = detail.session;
        assertEquals(sessionId, session.id);
        assertEquals(7, session.workoutId);
        assertEquals("Leg Day", session.workoutName);
        assertTrue(session.completed);

        assertTitles(detail.exercises, "Squat", "Lunge");
        ExerciseSession squat = detail.exercises.get(0);
        assertEquals(sessionId, squat.sessionId);
        assertEquals(1, squat.exerciseId);
        assertEquals(0, squat.position);
        assertEquals(45, squat.plannedDurationSeconds);
        assertEquals(40, squat.actualDurationSeconds);
        assertEquals(1_000, squat.startedAt);
        assertEquals(2_000, squat.finishedAt);
        ExerciseSession lunge = detail.exercises.get(1);
        assertEquals(30, lunge.plannedDurationSeconds);
        assertEquals(25, lunge.actualDurationSeconds);
    }

    @Test
    public void exerciseRowsAreScopedToTheRequestedSession() throws InterruptedException {
        long otherId = history.startSession(1, "Push Day");
        history.recordExercise(otherId, 3, "Push-up", 0, 30, 30, 0, 0);
        long sessionId = history.startSession(2, "Pull Day");
        history.recordExercise(sessionId, 4, "Row", 0, 40, 35, 0, 0);
        history.finishSession(sessionId, false);

        HistoryDetail detail =
                getOrAwaitValue(new HistoryDetailViewModel(container, sessionId).getDetail());

        assertEquals("Pull Day", detail.session.workoutName);
        assertTitles(detail.exercises, "Row");
    }

    @Test
    public void sessionWithoutRecordedExercisesLoadsWithAnEmptyList()
            throws InterruptedException {
        long sessionId = history.startSession(5, "Core");
        history.finishSession(sessionId, true);

        HistoryDetail detail =
                getOrAwaitValue(new HistoryDetailViewModel(container, sessionId).getDetail());

        assertEquals("Core", detail.session.workoutName);
        assertNotNull(detail.exercises);
        assertTrue(detail.exercises.isEmpty());
    }

    @Test
    public void unknownSessionIdEmitsNothing() throws Exception {
        HistoryDetailViewModel viewModel = new HistoryDetailViewModel(container, 404);

        awaitExecutorIdle();

        assertNull(viewModel.getDetail().getValue());
    }

    @Test
    public void deletedSessionEmitsNothing() throws Exception {
        long sessionId = history.startSession(7, "Leg Day");
        history.recordExercise(sessionId, 1, "Squat", 0, 45, 40, 0, 0);
        history.finishSession(sessionId, true);
        // Swiped away on the history list; a stale sessionId then reaches the detail screen.
        history.delete(history.getSession(sessionId));

        HistoryDetailViewModel viewModel = new HistoryDetailViewModel(container, sessionId);

        awaitExecutorIdle();

        assertNull(viewModel.getDetail().getValue());
    }

    /** ioExecutor is single-threaded and InstantTaskExecutorRule makes postValue()
     *  synchronous, so once this barrier task has run the constructor's load has run and
     *  its result has been handed to the ViewModel (on the executor, or already in the
     *  constructor if the load won the race against observeForever). */
    private void awaitExecutorIdle() throws Exception {
        container.ioExecutor.submit(() -> { }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static void assertTitles(List<ExerciseSession> exercises, String... expectedTitles) {
        List<String> titles = new ArrayList<>();
        for (ExerciseSession exercise : exercises) {
            titles.add(exercise.exerciseTitle);
        }
        assertEquals(Arrays.asList(expectedTitles), titles);
    }
}
