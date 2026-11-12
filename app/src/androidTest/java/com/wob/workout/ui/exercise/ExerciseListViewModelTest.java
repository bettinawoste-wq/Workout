package com.wob.workout.ui.exercise;

import static com.wob.workout.LiveDataTestUtil.getOrAwaitValue;
import static com.wob.workout.LiveDataTestUtil.observeValues;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.wob.workout.TestAppContainer;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.repository.ExerciseRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Covers {@link ExerciseListViewModel}'s reactive design (issue #6): {@code getExercises()}
 * is backed by Room's {@code getAllLive()}, so any insert/update/delete of the catalog
 * reaches an already-attached observer on its own - the removed {@code onResume()} reload
 * has no replacement. Each reactive test therefore keeps one observer attached across the
 * mutation ({@code observeValues}) instead of taking two independent one-shot reads,
 * which would pass even if nothing re-notified the existing observer.
 */
@RunWith(AndroidJUnit4.class)
public class ExerciseListViewModelTest {

    private static final long TIMEOUT_SECONDS = 2;

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private TestAppContainer testContainer;
    private ExerciseRepository repository;
    private ExerciseListViewModel viewModel;

    @Before
    public void setUp() {
        testContainer = new TestAppContainer();
        repository = testContainer.getAppContainer().exerciseRepository;
        viewModel = new ExerciseListViewModel(testContainer.getAppContainer());
    }

    @After
    public void tearDown() {
        testContainer.close();
    }

    @Test
    public void exercisesIsEmptyForEmptyCatalog() throws InterruptedException {
        List<Exercise> exercises = getOrAwaitValue(viewModel.getExercises());

        assertTrue(exercises.isEmpty());
    }

    @Test
    public void exercisesListsCatalogNewestFirst() throws InterruptedException {
        testContainer.insertExercise("Squat");
        testContainer.insertExercise("Lunge");

        List<Exercise> exercises = getOrAwaitValue(viewModel.getExercises());

        assertEquals(2, exercises.size());
        assertEquals("Lunge", exercises.get(0).title);
        assertEquals("Squat", exercises.get(1).title);
    }

    @Test
    public void getExercisesReturnsTheSameLiveDataEachTime() {
        // The Fragment observes once per view; a fresh LiveData per call would re-query
        // and drop the observer's connection to later changes.
        assertSame(viewModel.getExercises(), viewModel.getExercises());
    }

    @Test
    public void exercisesUpdatesOnInsertWithoutReload() throws InterruptedException {
        BlockingQueue<List<Exercise>> values = observeValues(viewModel.getExercises());
        assertTrue(poll(values).isEmpty());

        testContainer.insertExercise("Push-up");

        List<Exercise> afterInsert = poll(values);
        assertEquals(1, afterInsert.size());
        assertEquals("Push-up", afterInsert.get(0).title);
    }

    @Test
    public void exercisesUpdatesOnUpdateWithoutReload() throws InterruptedException {
        long id = testContainer.insertExercise("Push-up");
        BlockingQueue<List<Exercise>> values = observeValues(viewModel.getExercises());
        assertEquals("Push-up", poll(values).get(0).title);

        Exercise exercise = repository.getById(id);
        exercise.title = "Diamond Push-up";
        assertTrue(repository.update(exercise));

        List<Exercise> afterUpdate = poll(values);
        assertEquals(1, afterUpdate.size());
        assertEquals("Diamond Push-up", afterUpdate.get(0).title);
    }

    @Test
    public void exercisesUpdatesOnDeleteWithoutReload() throws InterruptedException {
        testContainer.insertExercise("Squat");
        long lungeId = testContainer.insertExercise("Lunge");
        BlockingQueue<List<Exercise>> values = observeValues(viewModel.getExercises());
        assertEquals(2, poll(values).size());

        repository.delete(repository.getById(lungeId));

        List<Exercise> afterDelete = poll(values);
        assertEquals(1, afterDelete.size());
        assertEquals("Squat", afterDelete.get(0).title);
    }

    /** Next emission of the still-attached observer; fails instead of returning null on
     *  timeout, so a missing re-notification reads as such rather than as an NPE. */
    private static List<Exercise> poll(BlockingQueue<List<Exercise>> values)
            throws InterruptedException {
        List<Exercise> value = values.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertNotNull("LiveData did not emit within " + TIMEOUT_SECONDS + "s", value);
        return value;
    }
}
