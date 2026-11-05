package com.wob.workout;

import static com.wob.workout.LiveDataTestUtil.getOrAwaitValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.wob.workout.data.database.AppDatabase;
import com.wob.workout.data.database.DatabaseProvider;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.WorkoutExercise;
import com.wob.workout.ui.workout.WorkoutListItem;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Smoke test for {@link MigrationTestHelper} itself (issue #11): proves the container it
 * builds shares one isolated in-memory database across all repositories, that close()
 * fully tears down and hands DatabaseProvider back its previous instance, and that only
 * one helper can be live at a time.
 */
@RunWith(AndroidJUnit4.class)
public class MigrationTestHelperTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private MigrationTestHelper helper;

    @After
    public void tearDown() {
        if (helper != null) {
            helper.close();
        }
    }

    @Test
    public void repositoriesOnTheSameContainerShareOneDatabase() throws InterruptedException {
        helper = new MigrationTestHelper();
        AppContainer container = helper.getAppContainer();

        long workoutId = helper.insertWorkout("Leg Day");
        long exerciseId = helper.insertExercise("Squat");
        helper.insertWorkoutExercise(workoutId, exerciseId, 0, 45);

        // Written through the per-call repositories (workout/exercise/workoutExercise)...
        Exercise exercise = container.exerciseRepository.getById(exerciseId);
        List<WorkoutExercise> workoutExercises =
                container.workoutExerciseRepository.getByWorkout(workoutId);
        assertEquals("Squat", exercise.title);
        assertEquals(1, workoutExercises.size());
        assertEquals("Leg Day", container.workoutRepository.getById(workoutId).name);

        // ...and read back through WorkoutSummaryRepository, which captures its database
        // once at construction - the case that breaks if the helper wires things in the
        // wrong order.
        List<WorkoutListItem> summaries =
                getOrAwaitValue(container.workoutSummaryRepository.observeAll());
        assertEquals(1, summaries.size());
        assertEquals(45, summaries.get(0).totalDurationSeconds);
    }

    @Test
    public void closeTearsDownAndRestoresTheProvidersPreviousInstance() throws Exception {
        Object before = readProviderInstance();
        helper = new MigrationTestHelper();
        assertSame(helper.getDatabase(), readProviderInstance());

        helper.close();

        assertFalse(helper.getDatabase().isOpen());
        assertTrue(helper.getAppContainer().ioExecutor.isShutdown());
        assertSame(before, readProviderInstance());
        helper.close(); // idempotent: a second call (e.g. from @After) must not throw
    }

    @Test
    public void closedHelperLeavesNothingBehindForTheNextHelper() throws Exception {
        Object before = readProviderInstance();
        MigrationTestHelper first = new MigrationTestHelper();
        AppDatabase firstDatabase = first.getDatabase();
        try {
            first.insertWorkout("Only in first");
        } finally {
            first.close();
        }

        // The leak that matters is to code that does *not* use the helper.
        assertNotSame(firstDatabase, readProviderInstance());
        assertSame(before, readProviderInstance());

        helper = new MigrationTestHelper();
        assertTrue(helper.getAppContainer().workoutRepository.getAll().isEmpty());
    }

    @Test
    public void secondHelperWhileOneIsOpenIsRejected() throws Exception {
        helper = new MigrationTestHelper();

        assertThrows(IllegalStateException.class, MigrationTestHelper::new);
        // The rejected constructor must not have touched the live helper's wiring.
        assertSame(helper.getDatabase(), readProviderInstance());
    }

    private static AppDatabase readProviderInstance() throws Exception {
        Field field = DatabaseProvider.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        return (AppDatabase) field.get(null);
    }
}
