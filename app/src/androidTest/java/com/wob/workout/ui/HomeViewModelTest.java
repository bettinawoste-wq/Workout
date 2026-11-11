package com.wob.workout.ui;

import static com.wob.workout.LiveDataTestUtil.getOrAwaitValue;
import static org.junit.Assert.assertEquals;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.wob.workout.TestAppContainer;
import com.wob.workout.ui.workout.WorkoutListItem;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The home screen's Workouts section: the recent workouts, the first three of the
 *  workout list's order. */
@RunWith(AndroidJUnit4.class)
public class HomeViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private TestAppContainer testContainer;
    private HomeViewModel viewModel;

    @Before
    public void setUp() {
        testContainer = new TestAppContainer();
        viewModel = new HomeViewModel(testContainer.getAppContainer());
    }

    @After
    public void tearDown() {
        testContainer.close();
    }

    @Test
    public void aSingleNeverRunWorkoutIsShown() throws InterruptedException {
        testContainer.insertWorkout("Ganzkörper");

        assertEquals(Arrays.asList("Ganzkörper"),
                names(getOrAwaitValue(viewModel.getRecentWorkouts())));
    }

    @Test
    public void showsTheThreeMostRecentlyRunIncludingAborted() throws InterruptedException {
        long a = testContainer.insertWorkout("A");
        long b = testContainer.insertWorkout("B");
        long c = testContainer.insertWorkout("C");
        long d = testContainer.insertWorkout("D");
        testContainer.insertSession(a, 4_000, false);
        testContainer.insertSession(b, 3_000, true);
        testContainer.insertSession(c, 1_000, true);
        testContainer.insertSession(d, 2_000, false);

        assertEquals(Arrays.asList("A", "B", "D"),
                names(getOrAwaitValue(viewModel.getRecentWorkouts())));
    }

    @Test
    public void fillsUpWithTheNewestCreatedWhenFewerThanThreeWereRun()
            throws InterruptedException {
        long ran = testContainer.insertWorkout("Ran");
        testContainer.insertWorkout("Older");
        testContainer.insertWorkout("Newer");
        testContainer.insertWorkout("Newest");
        testContainer.insertSession(ran, 1_000, true);

        assertEquals(Arrays.asList("Ran", "Newest", "Newer"),
                names(getOrAwaitValue(viewModel.getRecentWorkouts())));
    }

    private static List<String> names(List<WorkoutListItem> items) {
        List<String> names = new ArrayList<>();
        for (WorkoutListItem item : items) names.add(item.workout.name);
        return names;
    }
}
