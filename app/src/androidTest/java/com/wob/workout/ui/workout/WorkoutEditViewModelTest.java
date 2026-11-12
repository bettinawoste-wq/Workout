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
import java.util.concurrent.TimeUnit;

/**
 * Covers {@link WorkoutEditViewModel}'s non-reactive "write, then reload" design: every
 * mutation re-runs the load and emits a fresh list, and {@code refreshExercises()} is the
 * only way a change made elsewhere (another screen editing the catalog while this
 * Fragment sat on the back stack) reaches it. See the ViewModel's class Javadoc.
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
    }

    @Test
    public void addExercisesAppendsThemInTickOrderWith60SecondsEach() throws InterruptedException {
        long workoutId = testContainer.insertWorkout("Push Day");
        long pushUpId = testContainer.insertExercise("Push-up");
        long dipId = testContainer.insertExercise("Dip");
        long plankId = testContainer.insertExercise("Plank");
        testContainer.insertWorkoutExercise(workoutId, pushUpId, 0, 30);
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, workoutId);
        // Ticked in this order, not in catalog order.
        List<Exercise> ticked = Arrays.asList(
                container.exerciseRepository.getById(plankId),
                container.exerciseRepository.getById(dipId));

        List<ExerciseItem> items =
                awaitEmissionAfter(viewModel, () -> viewModel.addExercises(ticked));

        assertTitles(items, "Push-up", "Plank", "Dip");
        assertEquals(1, items.get(1).workoutExercise.position);
        assertEquals(2, items.get(2).workoutExercise.position);
        assertEquals(30, items.get(0).workoutExercise.durationSeconds);
        assertEquals(60, items.get(1).workoutExercise.durationSeconds);
        assertEquals(60, items.get(2).workoutExercise.durationSeconds);
    }

    @Test
    public void addingTheSameExerciseTwiceKeepsBothInOrder() throws InterruptedException {
        long workoutId = testContainer.insertWorkout("Core");
        long plankId = testContainer.insertExercise("Plank");
        long crunchId = testContainer.insertExercise("Crunch");
        testContainer.insertWorkoutExercise(workoutId, plankId, 0, 30);
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, workoutId);
        Exercise plank = container.exerciseRepository.getById(plankId);
        Exercise crunch = container.exerciseRepository.getById(crunchId);

        awaitEmissionAfter(viewModel, () -> viewModel.addExercises(Arrays.asList(crunch)));
        List<ExerciseItem> items =
                awaitEmissionAfter(viewModel, () -> viewModel.addExercises(Arrays.asList(plank)));

        assertTitles(items, "Plank", "Crunch", "Plank");
        for (int i = 0; i < items.size(); i++) {
            assertEquals(i, items.get(i).workoutExercise.position);
        }
    }

    @Test
    public void changeDurationIsPersistedAndReloaded() throws InterruptedException {
        long workoutId = testContainer.insertWorkout("Core");
        long plankId = testContainer.insertExercise("Plank");
        testContainer.insertWorkoutExercise(workoutId, plankId, 0, 30);
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, workoutId);
        ExerciseItem plank = getOrAwaitValue(viewModel.getExercises()).get(0);

        List<ExerciseItem> items =
                awaitEmissionAfter(viewModel, () -> viewModel.changeDuration(plank, 90));

        assertEquals(1, items.size());
        assertEquals(90, items.get(0).workoutExercise.durationSeconds);
        assertEquals(90, container.workoutExerciseRepository
                .getById(plank.workoutExercise.id).durationSeconds);
    }

    @Test
    public void removeExerciseDropsItFromTheList() throws InterruptedException {
        long workoutId = testContainer.insertWorkout("Full Body");
        long squatId = testContainer.insertExercise("Squat");
        long rowId = testContainer.insertExercise("Row");
        testContainer.insertWorkoutExercise(workoutId, squatId, 0, 45);
        testContainer.insertWorkoutExercise(workoutId, rowId, 1, 40);
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, workoutId);
        ExerciseItem squat = getOrAwaitValue(viewModel.getExercises()).get(0);

        List<ExerciseItem> items =
                awaitEmissionAfter(viewModel, () -> viewModel.removeExercise(squat));

        assertTitles(items, "Row");
        assertNull(container.workoutExerciseRepository.getById(squat.workoutExercise.id));
    }

    @Test
    public void reorderPersistsTheNewPositions() throws InterruptedException {
        long workoutId = testContainer.insertWorkout("Circuit");
        long aId = testContainer.insertExercise("A");
        long bId = testContainer.insertExercise("B");
        long cId = testContainer.insertExercise("C");
        testContainer.insertWorkoutExercise(workoutId, aId, 0, 10);
        testContainer.insertWorkoutExercise(workoutId, bId, 1, 20);
        testContainer.insertWorkoutExercise(workoutId, cId, 2, 30);
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, workoutId);
        List<ExerciseItem> loaded = getOrAwaitValue(viewModel.getExercises());

        // What a drag of "C" to the top leaves in the adapter's list.
        List<ExerciseItem> newOrder = new ArrayList<>();
        newOrder.add(loaded.get(2));
        newOrder.add(loaded.get(0));
        newOrder.add(loaded.get(1));
        List<ExerciseItem> items = awaitEmissionAfter(viewModel, () -> viewModel.reorder(newOrder));

        assertTitles(items, "C", "A", "B");
        for (int i = 0; i < items.size(); i++) {
            assertEquals(i, items.get(i).workoutExercise.position);
        }
        // Durations travel with their exercise rather than staying in their old slot.
        assertEquals(30, items.get(0).workoutExercise.durationSeconds);
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
        assertTitles(getOrAwaitValue(viewModel.getExercises()), "Squat", "Lunge");

        // Out of band: the catalog screen deletes an exercise this workout uses. The
        // WorkoutExercise FK cascades, so this does not reach loadExerciseItems()'s
        // null-exercise guard - it mirrors what production actually does.
        container.exerciseRepository.delete(container.exerciseRepository.getById(squatId));

        List<ExerciseItem> items = awaitEmissionAfter(viewModel, viewModel::refreshExercises);

        assertTitles(items, "Lunge");
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
        }
    }

    @Test
    public void typingANameSavesANewWorkoutRightAway() throws Exception {
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, -1);

        viewModel.changeName("Leg Day");

        Workout created = getOrAwaitValue(viewModel.getWorkout());
        assertFalse(viewModel.isNew());
        assertEquals("Leg Day", nameInFreshEditor(created.id));

        viewModel.changeName("Leg Day Heavy");
        testContainer.drainIoExecutor();

        assertEquals("Leg Day Heavy", nameInFreshEditor(created.id));
        assertEquals(1, container.workoutRepository.getAll().size());
    }

    @Test
    public void clearingTheNameSavesNeuesWorkout() throws Exception {
        long workoutId = testContainer.insertWorkout("Leg Day");
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, workoutId);

        viewModel.changeName("   ");
        testContainer.drainIoExecutor();

        assertEquals("Neues Workout", nameInFreshEditor(workoutId));
    }

    @Test
    public void anEmptyNameDoesNotCreateANewWorkout() throws Exception {
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, -1);

        viewModel.changeName("");
        testContainer.drainIoExecutor();

        assertTrue(viewModel.isNew());
        assertTrue(container.workoutRepository.getAll().isEmpty());
    }

    @Test
    public void addingAnExerciseToANewWorkoutSavesItAsNeuesWorkout() throws Exception {
        long squatId = testContainer.insertExercise("Squat");
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, -1);

        viewModel.addExercises(Arrays.asList(container.exerciseRepository.getById(squatId)));

        Workout created = getOrAwaitValue(viewModel.getWorkout());
        assertEquals("Neues Workout", nameInFreshEditor(created.id));
        List<ExerciseItem> items = getOrAwaitValue(
                new WorkoutEditViewModel(container, created.id).getExercises());
        assertTitles(items, "Squat");
        assertEquals(60, items.get(0).workoutExercise.durationSeconds);
    }

    @Test
    public void duplicateCopiesNameExercisesAndDurations() throws Exception {
        long workoutId = testContainer.insertWorkout("Leg Day");
        long squatId = testContainer.insertExercise("Squat");
        long lungeId = testContainer.insertExercise("Lunge");
        testContainer.insertWorkoutExercise(workoutId, lungeId, 1, 30);
        testContainer.insertWorkoutExercise(workoutId, squatId, 0, 45);
        WorkoutEditViewModel viewModel = new WorkoutEditViewModel(container, workoutId);

        long copyId = getOrAwaitValue(viewModel.duplicate());

        assertTrue(copyId != workoutId);
        assertEquals("Leg Day (Kopie)", nameInFreshEditor(copyId));
        List<ExerciseItem> items =
                getOrAwaitValue(new WorkoutEditViewModel(container, copyId).getExercises());
        assertTitles(items, "Squat", "Lunge");
        assertEquals(45, items.get(0).workoutExercise.durationSeconds);
        assertEquals(30, items.get(1).workoutExercise.durationSeconds);
    }

    @Test
    public void editingADuplicateLeavesTheOriginalUnchanged() throws Exception {
        long workoutId = testContainer.insertWorkout("Leg Day");
        long squatId = testContainer.insertExercise("Squat");
        long lungeId = testContainer.insertExercise("Lunge");
        testContainer.insertWorkoutExercise(workoutId, squatId, 0, 45);
        testContainer.insertWorkoutExercise(workoutId, lungeId, 1, 30);
        long copyId = getOrAwaitValue(new WorkoutEditViewModel(container, workoutId).duplicate());

        WorkoutEditViewModel copy = new WorkoutEditViewModel(container, copyId);
        List<ExerciseItem> copied = getOrAwaitValue(copy.getExercises());
        awaitEmissionAfter(copy, () -> copy.changeDuration(copied.get(0), 99));
        awaitEmissionAfter(copy, () -> copy.removeExercise(copied.get(1)));
        copy.changeName("Arm Day");
        testContainer.drainIoExecutor();

        assertEquals("Leg Day", nameInFreshEditor(workoutId));
        List<ExerciseItem> original =
                getOrAwaitValue(new WorkoutEditViewModel(container, workoutId).getExercises());
        assertTitles(original, "Squat", "Lunge");
        assertEquals(45, original.get(0).workoutExercise.durationSeconds);
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
