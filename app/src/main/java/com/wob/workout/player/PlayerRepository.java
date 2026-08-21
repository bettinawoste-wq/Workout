package com.wob.workout.player;

import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.WorkoutExercise;
import com.wob.workout.data.repository.ExerciseRepository;
import com.wob.workout.data.repository.WorkoutExerciseRepository;

import java.util.ArrayList;
import java.util.List;

public class PlayerRepository {

    private final ExerciseRepository exerciseRepository;
    private final WorkoutExerciseRepository workoutExerciseRepository;

    public PlayerRepository(
            ExerciseRepository exerciseRepository,
            WorkoutExerciseRepository workoutExerciseRepository) {
        this.exerciseRepository = exerciseRepository;
        this.workoutExerciseRepository = workoutExerciseRepository;
    }

    public List<PlayerExercise> getExercises(long workoutId) {

        List<PlayerExercise> result = new ArrayList<>();

        for (WorkoutExercise workoutExercise :
                workoutExerciseRepository.getByWorkout(workoutId)) {

            Exercise exercise =
                    exerciseRepository.getById(workoutExercise.exerciseId);

            if (exercise != null) {
                result.add(new PlayerExercise(
                        exercise,
                        workoutExercise.durationSeconds));
            }
        }

        return result;
    }
}