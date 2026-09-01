package com.wob.workout.ui.exercise;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.wob.workout.AppContainer;
import com.wob.workout.data.entity.Exercise;

import java.util.List;

public class ExerciseListViewModel extends ViewModel {

    private final LiveData<List<Exercise>> exercises;

    public ExerciseListViewModel(AppContainer container) {
        exercises = container.exerciseRepository.observeAll();
    }

    public LiveData<List<Exercise>> getExercises() {
        return exercises;
    }
}
