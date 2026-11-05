package com.wob.workout.ui.history;

import static com.wob.workout.LiveDataTestUtil.getOrAwaitValue;
import static com.wob.workout.LiveDataTestUtil.observeValues;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.wob.workout.MigrationTestHelper;
import com.wob.workout.data.dao.WorkoutSessionDao;
import com.wob.workout.data.entity.WorkoutSession;
import com.wob.workout.data.repository.WorkoutHistoryRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Covers {@link HistoryListViewModel}'s reactive design (wayfinder ticket #8): the list is
 * a Room {@code @Relation} query ({@code WorkoutSessionWithExercises}) mapped to
 * {@link WorkoutSessionSummary} rows, with the "instant abort" filter folded into that
 * mapping, and no {@code refresh()} - writes reach a still-active observer through Room's
 * invalidation tracking alone.
 *
 * <p>Sessions are seeded through the real {@link WorkoutHistoryRepository} the Player uses
 * ({@code startSession}/{@code recordExercise}/{@code finishSession}).
 */
@RunWith(AndroidJUnit4.class)
public class HistoryListViewModelTest {

    private static final long TIMEOUT_SECONDS = 2;

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private MigrationTestHelper helper;
    private WorkoutHistoryRepository history;
    private HistoryListViewModel viewModel;

    @Before
    public void setUp() {
        helper = new MigrationTestHelper();
        history = helper.getAppContainer().workoutHistoryRepository;
        viewModel = new HistoryListViewModel(helper.getAppContainer());
    }

    @After
    public void tearDown() {
        helper.close();
    }

    @Test
    public void historyMapsEachSessionToItsSummaryNewestFirst() throws InterruptedException {
        long legDayId = history.startSession(1, "Leg Day");
        recordExercise(legDayId, 0, 45);
        recordExercise(legDayId, 1, 30);
        history.finishSession(legDayId, true);
        // Aborted mid-workout but with an exercise recorded: a real history entry.
        long pushDayId = history.startSession(2, "Push Day");
        recordExercise(pushDayId, 0, 20);
        history.finishSession(pushDayId, false);
        // startSession stamps System.currentTimeMillis(), which can tie within one test.
        setStartedAt(legDayId, 1_000L);
        setStartedAt(pushDayId, 2_000L);

        List<WorkoutSessionSummary> rows = getOrAwaitValue(viewModel.getHistory());

        assertEquals(2, rows.size());
        WorkoutSessionSummary pushDay = rows.get(0);
        assertEquals(pushDayId, pushDay.session.id);
        assertEquals("Push Day", pushDay.session.workoutName);
        assertEquals(1, pushDay.exerciseCount);
        assertFalse(pushDay.session.completed);
        assertEquals(20, pushDay.session.activeDurationSeconds);

        WorkoutSessionSummary legDay = rows.get(1);
        assertEquals(legDayId, legDay.session.id);
        assertEquals("Leg Day", legDay.session.workoutName);
        assertEquals(2, legDay.exerciseCount);
        assertTrue(legDay.session.completed);
        assertEquals(75, legDay.session.activeDurationSeconds);
    }

    @Test
    public void historyHidesInstantAbortsButKeepsCompletedEmptySessions()
            throws InterruptedException {
        // Left the Player before the first exercise was recorded: an instant abort.
        long abortedId = history.startSession(1, "Aborted");
        history.finishSession(abortedId, false);
        // Never finished at all (e.g. app killed) and nothing recorded: also filtered.
        history.startSession(1, "Unfinished");
        // Completed, even with no exercise rows (an empty workout): shown.
        long emptyCompletedId = history.startSession(2, "Empty Workout");
        history.finishSession(emptyCompletedId, true);

        List<WorkoutSessionSummary> rows = getOrAwaitValue(viewModel.getHistory());

        assertEquals(1, rows.size());
        assertEquals(emptyCompletedId, rows.get(0).session.id);
        assertEquals(0, rows.get(0).exerciseCount);
    }

    @Test
    public void filterIsReappliedWhenAnExerciseIsRecordedForAnObservedSession()
            throws InterruptedException {
        long sessionId = history.startSession(1, "Leg Day");

        // One still-active observer across the write: the filter lives inside the reactive
        // mapping, so a session must appear as soon as it stops being an instant abort.
        BlockingQueue<List<WorkoutSessionSummary>> values = observeValues(viewModel.getHistory());
        assertTrue(poll(values).isEmpty());

        recordExercise(sessionId, 0, 45);

        List<WorkoutSessionSummary> rows = poll(values);
        assertEquals(1, rows.size());
        assertEquals(sessionId, rows.get(0).session.id);
        assertEquals(1, rows.get(0).exerciseCount);
    }

    @Test
    public void deleteDropsTheRowFromTheStillObservedList() throws InterruptedException {
        long keptId = history.startSession(1, "Kept");
        recordExercise(keptId, 0, 30);
        history.finishSession(keptId, true);
        long deletedId = history.startSession(2, "Deleted");
        recordExercise(deletedId, 0, 40);
        history.finishSession(deletedId, true);

        // One still-active observer across the delete: proves Room's invalidation, not a
        // refresh() call, is what removes the row (there is no refresh() to call).
        BlockingQueue<List<WorkoutSessionSummary>> values = observeValues(viewModel.getHistory());
        List<WorkoutSessionSummary> before = poll(values);
        assertEquals(2, before.size());
        WorkoutSession toDelete = findSession(before, deletedId);

        viewModel.delete(toDelete);

        List<WorkoutSessionSummary> after = poll(values);
        assertEquals(1, after.size());
        assertEquals(keptId, after.get(0).session.id);
        // The ExerciseSession FK cascades, so the deleted session leaves nothing behind.
        assertTrue(history.getExercises(deletedId).isEmpty());
    }

    /** The exerciseId is arbitrary: ExerciseSession is a snapshot (title copied in) with no
     *  foreign key to Exercise, so no catalog row is needed. */
    private void recordExercise(long sessionId, int position, int actualDurationSeconds) {
        history.recordExercise(sessionId, 100 + position, "Exercise " + position, position,
                actualDurationSeconds, actualDurationSeconds, 0L, 0L);
    }

    private void setStartedAt(long sessionId, long startedAt) {
        WorkoutSessionDao dao = helper.getDatabase().workoutSessionDao();
        WorkoutSession session = dao.getById(sessionId);
        session.startedAt = startedAt;
        dao.update(session);
    }

    private static WorkoutSession findSession(List<WorkoutSessionSummary> rows, long sessionId) {
        for (WorkoutSessionSummary row : rows) {
            if (row.session.id == sessionId) {
                return row.session;
            }
        }
        throw new AssertionError("No row for session " + sessionId + " in " + rows.size() + " rows");
    }

    private static List<WorkoutSessionSummary> poll(
            BlockingQueue<List<WorkoutSessionSummary>> values) throws InterruptedException {
        List<WorkoutSessionSummary> value = values.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertNotNull("LiveData did not emit within " + TIMEOUT_SECONDS + "s", value);
        return value;
    }
}
