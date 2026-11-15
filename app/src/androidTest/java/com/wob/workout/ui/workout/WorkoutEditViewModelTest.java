package com.wob.workout.ui.workout;

import static com.wob.workout.LiveDataTestUtil.getOrAwaitValue;
import static com.wob.workout.LiveDataTestUtil.observeValues;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.wob.workout.AppContainer;
import com.wob.workout.TestAppContainer;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.ui.workout.WorkoutExerciseAdapter.ExerciseItem;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Covers {@link WorkoutEditViewModel}'s draft: changes show at once but are written only by
 * {@code save()}; leaving without it keeps the stored workout as it was, and a new workout
 * isn't created. What is stored is checked the way the user would see it, by opening the
 * workout in a fresh editor.
 *
 * <p>{@code exercises} is a plain {@code MutableLiveData} that keeps its last value, so a
 * fresh {@code getOrAwaitValue} after a mutation could return the stale list immediately.
 * {@link #awaitEmissionAfter} therefore keeps one observer attached across the mutation
 * and waits for the emission that follows it.
 */
@RunWith(AndroidJUnit4.class)
public class WorkoutEditViewModelTest {

    private static final long TIMEOUT_SECONDS = 2;

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private TestAppContainer testContainer;
    private AppContainer container;

    @Before
    public void setUp() {
        testContainer = new TestAppContainer();
        container = testContainer.getAppContainer();
    }

    @After
    public void tearDown() {
        testContainer.close();
    }

    @Test
    public void constructorLoadsWorkoutAndExercisesWithoutExplicitRefresh()
            throws InterruptedException {
        long workoutId = testContainer.insertWorkout("Leg Day");
        long squatId = testContainer.insertExercise("Squat");
        long lungeId = testContainer.insertExercise("Lunge");
        // Inserted out of order to show the list follows position, not insertion order.
        testContainer.insertWorkoutExercise(workoutId, lungeId, 1, 30);
        testContainer.insertWorkoutExercise(workoutId, squatId, 0, 45);

        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, workoutId);

        assertFalse(viewModel.isNew());
        Workout workout = getOrAwaitValue(viewModel.getWorkout());
        assertEquals("Leg Day", workout.name);

        List<ExerciseItem> items = getOrAwaitValue(viewModel.getExercises());
        assertTitles(items, "Squat", "Lunge");
        assertEquals(45, items.get(0).workoutExercise.durationSeconds);
        assertEquals(30, items.get(1).workoutExercise.durationSeconds);
        assertFalse(getOrAwaitValue(viewModel.getUnsavedChanges()));
    }

    @Test
    public void nothingIsWrittenBeforeSave() throws Exception {
        long workoutId = testContainer.insertWorkout("Leg Day");
        long squatId = testContainer.insertExercise("Squat");
        long lungeId = testContainer.insertExercise("Lunge");
        long plankId = testContainer.insertExercise("Plank");
        testContainer.insertWorkoutExercise(workoutId, squatId, 0, 45);
        testContainer.insertWorkoutExercise(workoutId, lungeId, 1, 30);
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, workoutId);
        List<ExerciseItem> loaded = getOrAwaitValue(viewModel.getExercises());

        viewModel.changeName("Arm Day");
        viewModel.changeDuration(loaded.get(0), 90);
        viewModel.reorder(Arrays.asList(loaded.get(1), loaded.get(0)));
        viewModel.removeExercise(loaded.get(1));
        List<ExerciseItem> shown = awaitEmissionAfter(viewModel, () -> viewModel.addExercises(
                Arrays.asList(container.exerciseRepository.getById(plankId))));
        testContainer.drainIoExecutor();

        assertTitles(shown, "Squat", "Plank");
        assertTrue(viewModel.hasUnsavedChanges());
        assertEquals("Leg Day", nameInFreshEditor(workoutId));
        List<ExerciseItem> stored = exercisesInFreshEditor(workoutId);
        assertTitles(stored, "Squat", "Lunge");
        assertEquals(45, stored.get(0).workoutExercise.durationSeconds);
    }

    @Test
    public void saveWritesNameOrderAndDurations() throws Exception {
        long workoutId = testContainer.insertWorkout("Leg Day");
        long squatId = testContainer.insertExercise("Squat");
        long lungeId = testContainer.insertExercise("Lunge");
        long plankId = testContainer.insertExercise("Plank");
        testContainer.insertWorkoutExercise(workoutId, squatId, 0, 45);
        testContainer.insertWorkoutExercise(workoutId, lungeId, 1, 30);
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, workoutId);
        List<ExerciseItem> loaded = getOrAwaitValue(viewModel.getExercises());
        ExerciseItem squat = loaded.get(0);
        ExerciseItem lunge = loaded.get(1);

        viewModel.changeName("  Leg Day Heavy ");
        viewModel.changeDuration(squat, 90);
        List<ExerciseItem> added = awaitEmissionAfter(viewModel, () -> viewModel.addExercises(
                Arrays.asList(container.exerciseRepository.getById(plankId))));
        viewModel.reorder(Arrays.asList(lunge, squat, added.get(2)));
        assertTrue(getOrAwaitValue(viewModel.save()));

        assertEquals("Leg Day Heavy", nameInFreshEditor(workoutId));
        List<ExerciseItem> stored = exercisesInFreshEditor(workoutId);
        assertTitles(stored, "Lunge", "Squat", "Plank");
        for (int i = 0; i < stored.size(); i++) {
            assertEquals(i, stored.get(i).workoutExercise.position);
        }
        assertEquals(30, stored.get(0).workoutExercise.durationSeconds);
        assertEquals(90, stored.get(1).workoutExercise.durationSeconds);
        assertEquals(60, stored.get(2).workoutExercise.durationSeconds);
        // The kept rows keep their id.
        assertEquals(lunge.workoutExercise.id, stored.get(0).workoutExercise.id);
    }

    @Test
    public void nothingIsSavedWhileTheWorkoutIsStillLoading() throws Exception {
        long workoutId = testContainer.insertWorkout("Leg Day");
        // Holds the io executor, so the editor's load can't run yet.
        CountDownLatch release = new CountDownLatch(1);
        container.ioExecutor.execute(() -> {
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, workoutId);

        try {
            assertFalse(getOrAwaitValue(viewModel.getReady()));
            assertNull(getOrAwaitValue(viewModel.save()));
            assertNull(getOrAwaitValue(viewModel.duplicate()));
        } finally {
            release.countDown();
        }

        assertNotNull(getOrAwaitValue(viewModel.getWorkout()));
        assertTrue(getOrAwaitValue(viewModel.getReady()));
        assertEquals(1, container.workoutRepository.getAll().size());
    }

    @Test
    public void addingTheSameExerciseTwiceKeepsBothInOrder() throws Exception {
        long workoutId = testContainer.insertWorkout("Core");
        long plankId = testContainer.insertExercise("Plank");
        long crunchId = testContainer.insertExercise("Crunch");
        testContainer.insertWorkoutExercise(workoutId, plankId, 0, 30);
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, workoutId);
        Exercise plank = container.exerciseRepository.getById(plankId);
        Exercise crunch = container.exerciseRepository.getById(crunchId);

        awaitEmissionAfter(viewModel, () -> viewModel.addExercises(Arrays.asList(crunch)));
        awaitEmissionAfter(viewModel, () -> viewModel.addExercises(Arrays.asList(plank)));
        assertTrue(getOrAwaitValue(viewModel.save()));

        List<ExerciseItem> stored = exercisesInFreshEditor(workoutId);
        assertTitles(stored, "Plank", "Crunch", "Plank");
        for (int i = 0; i < stored.size(); i++) {
            assertEquals(i, stored.get(i).workoutExercise.position);
        }
    }

    @Test
    public void savingWithAnExerciseRemovedDeletesItsRow() throws Exception {
        long workoutId = testContainer.insertWorkout("Full Body");
        long squatId = testContainer.insertExercise("Squat");
        long rowId = testContainer.insertExercise("Row");
        testContainer.insertWorkoutExercise(workoutId, squatId, 0, 45);
        testContainer.insertWorkoutExercise(workoutId, rowId, 1, 40);
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, workoutId);
        ExerciseItem squat = getOrAwaitValue(viewModel.getExercises()).get(0);

        List<ExerciseItem> shown =
                awaitEmissionAfter(viewModel, () -> viewModel.removeExercise(squat));
        assertTrue(getOrAwaitValue(viewModel.save()));

        assertTitles(shown, "Row");
        assertNull(container.workoutExerciseRepository.getById(squat.workoutExercise.id));
        assertTitles(exercisesInFreshEditor(workoutId), "Row");
    }

    @Test
    public void unsavedChangesFollowTheDraft() throws Exception {
        long workoutId = testContainer.insertWorkout("Core");
        long plankId = testContainer.insertExercise("Plank");
        testContainer.insertWorkoutExercise(workoutId, plankId, 0, 30);
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, workoutId);
        ExerciseItem plank = getOrAwaitValue(viewModel.getExercises()).get(0);
        assertFalse(viewModel.hasUnsavedChanges());

        viewModel.changeDuration(plank, 90);
        assertTrue(getOrAwaitValue(viewModel.getUnsavedChanges()));

        viewModel.changeDuration(plank, 30);
        assertFalse(getOrAwaitValue(viewModel.getUnsavedChanges()));
    }

    @Test
    public void theSummaryAddsUpTheExerciseDurations() throws Exception {
        Workout stored = new Workout();
        stored.name = "Core";
        long workoutId = container.workoutRepository.save(stored);
        long plankId = testContainer.insertExercise("Plank");
        long crunchId = testContainer.insertExercise("Crunch");
        testContainer.insertWorkoutExercise(workoutId, plankId, 0, 45);
        testContainer.insertWorkoutExercise(workoutId, crunchId, 1, 30);
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, workoutId);
        getOrAwaitValue(viewModel.getExercises());

        // 45 + 30 seconds, no transitions.
        assertEquals("2 Übungen · 1:15", getOrAwaitValue(viewModel.getSummary()));

        awaitEmissionAfter(viewModel, () -> viewModel.addExercises(
                Arrays.asList(container.exerciseRepository.getById(plankId))));
        // Plus 60 seconds.
        assertEquals("3 Übungen · 2:15", getOrAwaitValue(viewModel.getSummary()));
    }

    @Test
    public void refreshExercisesPicksUpAnExerciseRenamedOnAnotherScreen()
            throws InterruptedException {
        long workoutId = testContainer.insertWorkout("Leg Day");
        long squatId = testContainer.insertExercise("Squat");
        testContainer.insertWorkoutExercise(workoutId, squatId, 0, 45);
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, workoutId);
        assertTitles(getOrAwaitValue(viewModel.getExercises()), "Squat");

        // Out of band: the catalog screen edits the exercise while this one is on the back stack.
        Exercise squat = container.exerciseRepository.getById(squatId);
        squat.title = "Back Squat";
        assertTrue(container.exerciseRepository.update(squat));

        List<ExerciseItem> items = awaitEmissionAfter(viewModel, viewModel::refreshExercises);

        assertTitles(items, "Back Squat");
    }

    @Test
    public void refreshExercisesDropsAnExerciseDeletedOnAnotherScreen()
            throws InterruptedException {
        long workoutId = testContainer.insertWorkout("Leg Day");
        long squatId = testContainer.insertExercise("Squat");
        long lungeId = testContainer.insertExercise("Lunge");
        testContainer.insertWorkoutExercise(workoutId, squatId, 0, 45);
        testContainer.insertWorkoutExercise(workoutId, lungeId, 1, 30);
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, workoutId);
        List<ExerciseItem> loaded = getOrAwaitValue(viewModel.getExercises());
        assertTitles(loaded, "Squat", "Lunge");
        // A change of the draft's own, which the refresh must keep.
        viewModel.changeDuration(loaded.get(1), 50);

        // Out of band: the catalog screen deletes an exercise this workout uses. The
        // WorkoutExercise FK cascades, so its stored row goes with it.
        container.exerciseRepository.delete(container.exerciseRepository.getById(squatId));

        List<ExerciseItem> items = awaitEmissionAfter(viewModel, viewModel::refreshExercises);

        assertTitles(items, "Lunge");
        assertEquals(50, items.get(0).workoutExercise.durationSeconds);
    }

    @Test
    public void newWorkoutStartsWithNoExercisesWithoutLoading() throws Exception {
        // isNew() is "workoutId <= 0", so both 0 and a negative id count as new.
        for (long newWorkoutId : new long[] {0, -1}) {
            WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, newWorkoutId);

            assertTrue(viewModel.isNew());
            viewModel.refreshExercises(); // Must be a no-op for a new workout, too.
            // ioExecutor is single-threaded: once this barrier task has run, any load the
            // constructor or refreshExercises() had queued would have delivered its value.
            container.ioExecutor.submit(() -> { }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertNull(viewModel.getWorkout().getValue());
            // Empty rather than unset, so the editor shows its empty state right away.
            assertEquals(new ArrayList<ExerciseItem>(), viewModel.getExercises().getValue());
            assertEquals("0 Übungen · 0:00", viewModel.getSummary().getValue());
            assertFalse(viewModel.hasUnsavedChanges());
        }
    }

    @Test
    public void aNewWorkoutLeftWithoutSavingIsNotCreated() throws Exception {
        long squatId = testContainer.insertExercise("Squat");
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, -1);

        viewModel.changeName("Leg Day");
        viewModel.addExercises(Arrays.asList(container.exerciseRepository.getById(squatId)));
        testContainer.drainIoExecutor();

        assertTrue(viewModel.hasUnsavedChanges());
        assertTrue(container.workoutRepository.getAll().isEmpty());
    }

    @Test
    public void savingANewWorkoutCreatesItWithItsExercises() throws Exception {
        long squatId = testContainer.insertExercise("Squat");
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, -1);

        viewModel.changeName("Leg Day");
        viewModel.addExercises(Arrays.asList(container.exerciseRepository.getById(squatId)));
        assertTrue(getOrAwaitValue(viewModel.save()));

        List<Workout> all = container.workoutRepository.getAll();
        assertEquals(1, all.size());
        assertEquals("Leg Day", nameInFreshEditor(all.get(0).id));
        List<ExerciseItem> items = exercisesInFreshEditor(all.get(0).id);
        assertTitles(items, "Squat");
        assertEquals(60, items.get(0).workoutExercise.durationSeconds);
    }

    @Test
    public void anEmptyNameIsSavedAsNeuesWorkout() throws Exception {
        long workoutId = testContainer.insertWorkout("Leg Day");
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, workoutId);
        getOrAwaitValue(viewModel.getWorkout());

        viewModel.changeName("   ");
        assertTrue(getOrAwaitValue(viewModel.save()));
        WorkoutEditViewModel fresh = new WorkoutEditViewModel(container, -1);
        assertTrue(getOrAwaitValue(fresh.save()));

        assertEquals("Neues Workout", nameInFreshEditor(workoutId));
        for (Workout workout : container.workoutRepository.getAll()) {
            assertEquals("Neues Workout", workout.name);
        }
    }

    @Test
    public void duplicateCopiesNameExercisesAndDurations() throws Exception {
        long workoutId = testContainer.insertWorkout("Leg Day");
        long squatId = testContainer.insertExercise("Squat");
        long lungeId = testContainer.insertExercise("Lunge");
        testContainer.insertWorkoutExercise(workoutId, lungeId, 1, 30);
        testContainer.insertWorkoutExercise(workoutId, squatId, 0, 45);
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, workoutId);
        getOrAwaitValue(viewModel.getExercises());

        long copyId = getOrAwaitValue(viewModel.duplicate());

        assertTrue(copyId != workoutId);
        assertEquals("Leg Day (Kopie)", nameInFreshEditor(copyId));
        List<ExerciseItem> items = exercisesInFreshEditor(copyId);
        assertTitles(items, "Squat", "Lunge");
        assertEquals(45, items.get(0).workoutExercise.durationSeconds);
        assertEquals(30, items.get(1).workoutExercise.durationSeconds);
    }

    @Test
    public void duplicateSavesTheDraftFirst() throws Exception {
        long workoutId = testContainer.insertWorkout("Leg Day");
        long squatId = testContainer.insertExercise("Squat");
        long lungeId = testContainer.insertExercise("Lunge");
        testContainer.insertWorkoutExercise(workoutId, squatId, 0, 45);
        testContainer.insertWorkoutExercise(workoutId, lungeId, 1, 30);
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, workoutId);
        List<ExerciseItem> loaded = getOrAwaitValue(viewModel.getExercises());

        viewModel.changeName("Arm Day");
        viewModel.changeDuration(loaded.get(0), 90);
        viewModel.removeExercise(loaded.get(1));
        long copyId = getOrAwaitValue(viewModel.duplicate());

        // The copy matches what was on screen, and the original was saved that way first.
        assertEquals("Arm Day (Kopie)", nameInFreshEditor(copyId));
        assertEquals("Arm Day", nameInFreshEditor(workoutId));
        for (long id : new long[] {copyId, workoutId}) {
            List<ExerciseItem> items = exercisesInFreshEditor(id);
            assertTitles(items, "Squat");
            assertEquals(90, items.get(0).workoutExercise.durationSeconds);
        }
    }

    @Test
    public void duplicatingANewWorkoutCreatesItFirst() throws Exception {
        long squatId = testContainer.insertExercise("Squat");
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, -1);
        viewModel.addExercises(Arrays.asList(container.exerciseRepository.getById(squatId)));

        long copyId = getOrAwaitValue(viewModel.duplicate());

        assertEquals(2, container.workoutRepository.getAll().size());
        assertEquals("Neues Workout (Kopie)", nameInFreshEditor(copyId));
        assertTitles(exercisesInFreshEditor(copyId), "Squat");
    }

    @Test
    public void editingADuplicateLeavesTheOriginalUnchanged() throws Exception {
        long workoutId = testContainer.insertWorkout("Leg Day");
        long squatId = testContainer.insertExercise("Squat");
        long lungeId = testContainer.insertExercise("Lunge");
        testContainer.insertWorkoutExercise(workoutId, squatId, 0, 45);
        testContainer.insertWorkoutExercise(workoutId, lungeId, 1, 30);
        WorkoutEditViewModel original = new WorkoutEditViewModel(container, workoutId);
        getOrAwaitValue(original.getExercises());
        long copyId = getOrAwaitValue(original.duplicate());

        WorkoutEditViewModel copy = new WorkoutEditViewModel(container, copyId);
        List<ExerciseItem> copied = getOrAwaitValue(copy.getExercises());
        copy.changeDuration(copied.get(0), 99);
        copy.removeExercise(copied.get(1));
        copy.changeName("Arm Day");
        assertTrue(getOrAwaitValue(copy.save()));

        assertEquals("Leg Day", nameInFreshEditor(workoutId));
        List<ExerciseItem> stored = exercisesInFreshEditor(workoutId);
        assertTitles(stored, "Squat", "Lunge");
        assertEquals(45, stored.get(0).workoutExercise.durationSeconds);
    }

    @Test
    public void theStoredNameIsShownOnlyOnceAndNeverForANewWorkout() {
        long workoutId = testContainer.insertWorkout("Leg Day");
        WorkoutEditViewModel existing = new WorkoutEditViewModel(container, workoutId);
        WorkoutEditViewModel fresh = new WorkoutEditViewModel(container, -1);

        // Afterwards the field keeps what the user left in it, even an empty name.
        assertTrue(existing.takeNameToShow());
        assertFalse(existing.takeNameToShow());
        assertFalse(fresh.takeNameToShow());
    }

    /** What the editor shows when the workout is opened again, e.g. after leaving it. */
    private String nameInFreshEditor(long workoutId) throws InterruptedException {
        return getOrAwaitValue(new WorkoutEditViewModel(container, workoutId).getWorkout()).name;
    }

    private List<ExerciseItem> exercisesInFreshEditor(long workoutId)
            throws InterruptedException {
        return getOrAwaitValue(new WorkoutEditViewModel(container, workoutId).getExercises());
    }

    /** Waits for the initial load, runs {@code mutation}, and returns the list it emits. */
    private static List<ExerciseItem> awaitEmissionAfter(
            WorkoutEditViewModel viewModel, Runnable mutation) throws InterruptedException {
        List<ExerciseItem> before = getOrAwaitValue(viewModel.getExercises());
        BlockingQueue<List<ExerciseItem>> values = observeValues(viewModel.getExercises());
        // A new observer is immediately handed the current value; drain it first.
        assertSame(before, values.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        mutation.run();

        List<ExerciseItem> after = values.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertNotNull("No emission after the mutation", after);
        return after;
    }

    private static void assertTitles(List<ExerciseItem> items, String... expectedTitles) {
        List<String> titles = new ArrayList<>();
        for (ExerciseItem item : items) {
            titles.add(item.exercise.title);
        }
        assertEquals(Arrays.asList(expectedTitles), titles);
    }
}
