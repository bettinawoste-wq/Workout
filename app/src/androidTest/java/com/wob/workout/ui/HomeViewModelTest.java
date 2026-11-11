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

    private TestAppContainer helper;
    private HomeViewModel viewModel;

    @Before
    public void setUp() {
        helper = new TestAppContainer();
        viewModel = new HomeViewModel(helper.getAppContainer());
    }

    @After
    public void tearDown() {
        helper.close();
    }

    @Test
    public void aSingleNeverRunWorkoutIsShown() throws InterruptedException {
        helper.insertWorkout("Ganzkörper");

        assertEquals(Arrays.asList("Ganzkörper"),
                names(getOrAwaitValue(viewModel.getRecentWorkouts())));
    }

    @Test
    public void showsTheThreeMostRecentlyRunIncludingAborted() throws InterruptedException {
        long a = helper.insertWorkout("A");
        long b = helper.insertWorkout("B");
        long c = helper.insertWorkout("C");
        long d = helper.insertWorkout("D");
        helper.insertSession(a, 4_000, false);
        helper.insertSession(b, 3_000, true);
        helper.insertSession(c, 1_000, true);
        helper.insertSession(d, 2_000, false);

        assertEquals(Arrays.asList("A", "B", "D"),
                names(getOrAwaitValue(viewModel.getRecentWorkouts())));
    }

    @Test
    public void fillsUpWithTheNewestCreatedWhenFewerThanThreeWereRun()
            throws InterruptedException {
        long ran = helper.insertWorkout("Ran");
        helper.insertWorkout("Older");
        helper.insertWorkout("Newer");
        helper.insertWorkout("Newest");
        helper.insertSession(ran, 1_000, true);

        assertEquals(Arrays.asList("Ran", "Newest", "Newer"),
                names(getOrAwaitValue(viewModel.getRecentWorkouts())));
    }

    private static List<String> names(List<WorkoutListItem> items) {
        List<String> names = new ArrayList<>();
        for (WorkoutListItem item : items) names.add(item.workout.name);
        return names;
    }
}
