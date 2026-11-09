package com.wob.workout;

import android.content.Context;

import com.wob.workout.data.repository.ExerciseRepository;
import com.wob.workout.data.repository.WorkoutExerciseRepository;
import com.wob.workout.data.repository.WorkoutHistoryRepository;
import com.wob.workout.data.repository.WorkoutRepository;
import com.wob.workout.data.repository.WorkoutSummaryRepository;
import com.wob.workout.media.ExerciseThumbnails;
import com.wob.workout.player.PlayerRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * One instance, created once in {@link WorkoutApplication#onCreate()} and held for the
 * process lifetime. Replaces scattered {@code new XRepository(context)} call sites.
 */
public class AppContainer {

    public final Context appContext;

    public final WorkoutRepository workoutRepository;
    public final ExerciseRepository exerciseRepository;
    public final WorkoutExerciseRepository workoutExerciseRepository;
    public final WorkoutSummaryRepository workoutSummaryRepository;
    public final WorkoutHistoryRepository workoutHistoryRepository;
    public final PlayerRepository playerRepository;

    /** Single background thread for all Room access — see wayfinder ticket #2.
     *  <b>Must stay single-threaded:</b> {@code PlayerHistoryTracker} relies on it to run
     *  its fire-and-forget history writes in call order (issue #21). */
    public final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    /** Decodes pictures for display, apart from {@link #ioExecutor} so that a slow decode
     *  never holds up Room access. */
    public final ExecutorService mediaExecutor = Executors.newSingleThreadExecutor();

    public final ExerciseThumbnails thumbnails;

    public AppContainer(Context appContext) {
        this.appContext = appContext;

        thumbnails = new ExerciseThumbnails(appContext, mediaExecutor);

        workoutRepository = new WorkoutRepository(appContext);
        exerciseRepository = new ExerciseRepository(appContext);
        workoutExerciseRepository = new WorkoutExerciseRepository(appContext);
        workoutSummaryRepository = new WorkoutSummaryRepository(appContext);
        workoutHistoryRepository = new WorkoutHistoryRepository(appContext);
        playerRepository = new PlayerRepository(exerciseRepository, workoutExerciseRepository);
    }
}
