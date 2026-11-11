package com.wob.workout.ui.workout;

import static com.wob.workout.LiveDataTestUtil.getOrAwaitValue;
import static com.wob.workout.LiveDataTestUtil.observeValues;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.wob.workout.TestAppContainer;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.MediaType;
import com.wob.workout.data.repository.WorkoutSummaryRepository;

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
 * Exercises WorkoutSummaryRepository directly rather than constructing
 * WorkoutListViewModel: the ViewModel is a one-line passthrough (getWorkouts() just
 * returns repository.observeAll()), so the repository's LiveData is exactly what the
 * ViewModel exposes. Database setup and seeding come from {@link TestAppContainer}.
 */
@RunWith(AndroidJUnit4.class)
public class WorkoutListViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private TestAppContainer helper;
    private WorkoutSummaryRepository repository;

    @Before
    public void setUp() {
        helper = new TestAppContainer();
        repository = helper.getAppContainer().workoutSummaryRepository;
    }

    @After
    public void tearDown() {
        helper.close();
    }

    @Test
    public void workoutsReflectsSavedWorkoutAndItsExercises() throws InterruptedException {
        long workoutId = helper.insertWorkout("Leg Day");
        long exerciseId = helper.insertExercise("Squat");
        helper.insertWorkoutExercise(workoutId, exerciseId, 0, 45);

        List<WorkoutListItem> items = getOrAwaitValue(repository.observeAll());

        assertEquals(1, items.size());
        assertEquals("Leg Day", items.get(0).workout.name);
        assertEquals(1, items.get(0).exerciseCount);
        assertEquals(45, items.get(0).totalDurationSeconds);
    }

    @Test
    public void exercisesFollowTheWorkoutOrderWithOrWithoutMedia()
            throws InterruptedException {
        long workoutId = helper.insertWorkout("Leg Day");
        // Inserted in the opposite order to their positions.
        helper.insertWorkoutExercise(workoutId, insertPictured("Second"), 2, 30);
        helper.insertWorkoutExercise(workoutId, helper.insertExercise("No picture"), 1, 30);
        helper.insertWorkoutExercise(workoutId, insertPictured("First"), 0, 30);

        WorkoutListItem item = getOrAwaitValue(repository.observeAll()).get(0);

        assertEquals(3, item.exerciseCount);
        assertEquals(3, item.exercises.size());
        assertEquals("First", item.exercises.get(0).title);
        assertEquals("No picture", item.exercises.get(1).title);
        assertEquals("Second", item.exercises.get(2).title);
    }

    @Test
    public void workoutsUpdatesWhenAnExerciseIsAdded() throws InterruptedException {
        long workoutId = helper.insertWorkout("Push Day");
        long exerciseId = helper.insertExercise("Push-up");

        // One still-active observer across the mutation below - this is what actually
        // proves Room's invalidation tracking replaces the deleted onResume()/reload,
        // as opposed to two independent one-shot reads that would pass even if nothing
        // re-notified an existing observer.
        BlockingQueue<List<WorkoutListItem>> values = observeValues(repository.observeAll());

        List<WorkoutListItem> beforeAdding = values.poll(2, TimeUnit.SECONDS);
        assertEquals(0, beforeAdding.get(0).exerciseCount);

        helper.insertWorkoutExercise(workoutId, exerciseId, 0, 30);

        List<WorkoutListItem> afterAdding = values.poll(2, TimeUnit.SECONDS);
        assertEquals(1, afterAdding.get(0).exerciseCount);
        assertEquals(30, afterAdding.get(0).totalDurationSeconds);
    }

    @Test
    public void listStartsWithTheMostRecentlyRunThenTheNewestCreated()
            throws InterruptedException {
        long oldest = helper.insertWorkout("Oldest, never run");
        long ranLongAgo = helper.insertWorkout("Ran long ago");
        long ranLately = helper.insertWorkout("Ran lately, aborted");
        long newest = helper.insertWorkout("Newest, never run");
        helper.insertSession(ranLongAgo, 1_000, true);
        helper.insertSession(ranLately, 5_000, false);

        List<WorkoutListItem> items = getOrAwaitValue(repository.observeAll());

        assertEquals(
                Arrays.asList("Ran lately, aborted", "Ran long ago",
                        "Newest, never run", "Oldest, never run"),
                names(items));
    }

    @Test
    public void aWorkoutRanksByItsLatestSession() throws InterruptedException {
        long first = helper.insertWorkout("First");
        long second = helper.insertWorkout("Second");
        helper.insertSession(first, 1_000, true);
        helper.insertSession(second, 2_000, true);
        helper.insertSession(first, 3_000, true);

        List<WorkoutListItem> items = getOrAwaitValue(repository.observeAll());

        assertEquals(Arrays.asList("First", "Second"), names(items));
    }

    @Test
    public void listReordersWhenASessionIsRecordedAndDropsADeletedWorkout()
            throws InterruptedException {
        long older = helper.insertWorkout("Older");
        helper.insertWorkout("Newer");
        BlockingQueue<List<WorkoutListItem>> values = observeValues(repository.observeAll());
        assertEquals(Arrays.asList("Newer", "Older"),
                names(values.poll(2, TimeUnit.SECONDS)));

        long session = helper.insertSession(older, 1_000, false);
        assertEquals(Arrays.asList("Older", "Newer"),
                names(values.poll(2, TimeUnit.SECONDS)));

        helper.getAppContainer().workoutRepository.delete(
                helper.getAppContainer().workoutRepository.getById(older));
        assertEquals(Arrays.asList("Newer"), names(values.poll(2, TimeUnit.SECONDS)));
        assertNotNull(helper.getDatabase().workoutSessionDao().getById(session));
    }

    private static List<String> names(List<WorkoutListItem> items) {
        List<String> names = new ArrayList<>();
        for (WorkoutListItem item : items) names.add(item.workout.name);
        return names;
    }

    private long insertPictured(String title) {
        Exercise exercise = new Exercise();
        exercise.title = title;
        exercise.mediaFile = title + ".jpg";
        exercise.mediaType = MediaType.IMAGE;
        return helper.getAppContainer().exerciseRepository.save(exercise);
    }
}
