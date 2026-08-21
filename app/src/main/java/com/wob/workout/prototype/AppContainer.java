package com.wob.workout.prototype;

// PROTOTYPE — throwaway code answering wayfinder ticket:
// "Design the AppContainer, Application wiring & background-execution pattern"
// https://github.com/bettinawoste-wq/Workout/issues/2
// Not wired into the real app. Not to be merged into master.

import android.content.Context;

import com.wob.workout.data.repository.ExerciseRepository;
import com.wob.workout.data.repository.WorkoutExerciseRepository;
import com.wob.workout.data.repository.WorkoutHistoryRepository;
import com.wob.workout.data.repository.WorkoutRepository;
import com.wob.workout.data.repository.WorkoutSummaryRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * One instance, created once in {@link WorkoutApplication#onCreate()}, held for the
 * process lifetime. Replaces scattered {@code new XRepository(context)} call sites.
 */
public class AppContainer {

    public final WorkoutRepository workoutRepository;
    public final ExerciseRepository exerciseRepository;
    public final WorkoutExerciseRepository workoutExerciseRepository;
    public final WorkoutSummaryRepository workoutSummaryRepository;
    public final WorkoutHistoryRepository workoutHistoryRepository;

    /** Single background thread for all Room access. One thread is enough: Room's own
     *  connection pool is the real bottleneck, and a single thread makes ordering of
     *  writes trivially predictable — no need for a full thread pool here. */
    public final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    public AppContainer(Context appContext) {
        workoutRepository = new WorkoutRepository(appContext);
        exerciseRepository = new ExerciseRepository(appContext);
        workoutExerciseRepository = new WorkoutExerciseRepository(appContext);
        workoutSummaryRepository = new WorkoutSummaryRepository(appContext);
        workoutHistoryRepository = new WorkoutHistoryRepository(appContext);
    }
}
