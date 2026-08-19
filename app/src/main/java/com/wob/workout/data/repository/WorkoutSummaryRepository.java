package com.wob.workout.data.repository;

import android.content.Context;

import com.wob.workout.data.entity.Workout;
import com.wob.workout.data.entity.WorkoutExercise;
import com.wob.workout.ui.workout.WorkoutListItem;

import java.util.ArrayList;
import java.util.List;

public class WorkoutSummaryRepository {

    private final WorkoutRepository workoutRepository;
    private final WorkoutExerciseRepository workoutExerciseRepository;

    public WorkoutSummaryRepository(Context context) {
        workoutRepository = new WorkoutRepository(context);
        workoutExerciseRepository = new WorkoutExerciseRepository(context);
    }

    public List<WorkoutListItem> getAll() {
        List<WorkoutListItem> result = new ArrayList<>();
        for (Workout workout : workoutRepository.getAll()) {
            List<WorkoutExercise> exercises =
                    workoutExerciseRepository.getByWorkout(workout.id);

            int duration = 0;
            for (WorkoutExercise exercise : exercises)
                duration += exercise.durationSeconds;
            result.add(
                    new WorkoutListItem(
                            workout,
                            exercises.size(),
                            duration));
        }

        return result;
    }
}