package com.wob.workout.player;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.wob.workout.data.database.AppDatabase;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.ExerciseSession;
import com.wob.workout.data.repository.WorkoutHistoryRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * PlayerHistoryTracker is the one part of the player migration worth a real unit test —
 * pure state plus one repository dependency, no Android framework classes involved. See
 * wayfinder ticket #4.
 */
@RunWith(AndroidJUnit4.class)
public class PlayerHistoryTrackerTest {

    private AppDatabase database;
    private PlayerHistoryTracker tracker;
    private long sessionExerciseId;

    @Before
    public void createDatabase() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();

        WorkoutHistoryRepository historyRepository = new WorkoutHistoryRepository(database);
        tracker = new PlayerHistoryTracker(historyRepository);

        Exercise exercise = new Exercise();
        exercise.title = "Push-up";
        sessionExerciseId = database.exerciseDao().insert(exercise);

        // A fresh in-memory database, so this is the first row and its id is deterministically 1.
        tracker.startSession(1, "Push Day");
    }

    @After
    public void closeDatabase() {
        database.close();
    }

    @Test
    public void pausedTimeIsExcludedFromRecordedActiveSeconds() throws InterruptedException {
        Exercise exercise = new Exercise();
        exercise.id = sessionExerciseId;
        exercise.title = "Push-up";
        PlayerExercise current = new PlayerExercise(exercise, 60);

        tracker.startExercise();
        Thread.sleep(1100);
        tracker.onExercisePause();
        Thread.sleep(1000);
        tracker.onExerciseResume();
        Thread.sleep(1100);
        tracker.closeOutCurrentExercise(0, current);

        List<ExerciseSession> recorded = database.exerciseSessionDao().getBySession(1);
        assertEquals(1, recorded.size());

        // ~2.2s active across two segments, ~1s paused in between excluded. A generous
        // range absorbs scheduling jitter while still proving the pause wasn't counted
        // (if it were, this would be >= 3).
        assertTrue(recorded.get(0).actualDurationSeconds >= 1);
        assertTrue(recorded.get(0).actualDurationSeconds <= 2);
    }

    @Test
    public void finishIsIdempotentAndOnlyRecordsOnce() {
        tracker.finish(true);
        tracker.finish(false);

        assertTrue(database.workoutSessionDao().getById(1).completed);
    }
}
