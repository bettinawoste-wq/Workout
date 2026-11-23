package com.wob.workout.ui.exercise;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.wob.workout.AppContainer;
import com.wob.workout.data.entity.Exercise;

import java.util.List;

/** The exercise catalog, kept live, and what the search above it shows of it. */
public class ExerciseListViewModel extends ViewModel {

    private final LiveData<List<Exercise>> exercises;
    private final MutableLiveData<String> query = new MutableLiveData<>("");
    private final MediatorLiveData<List<Exercise>> shownExercises = new MediatorLiveData<>();
    private final MediatorLiveData<Boolean> noMatch = new MediatorLiveData<>();

    public ExerciseListViewModel(AppContainer container) {
        exercises = container.exerciseRepository.observeAll();
        // Each follows the catalog and the search on its own, so either can be observed alone.
        for (MediatorLiveData<?> result : new MediatorLiveData<?>[] {shownExercises, noMatch}) {
            result.addSource(exercises, all -> showSearch());
            result.addSource(query, typed -> showSearch());
        }
    }

    /** The whole catalog, whatever the search; empty means there are no exercises yet. */
    public LiveData<List<Exercise>> getExercises() {
        return exercises;
    }

    /** The exercises the search finds, all of them for an empty search. */
    public LiveData<List<Exercise>> getShownExercises() {
        return shownExercises;
    }

    /** Whether to say "Keine Übung gefunden". */
    public LiveData<Boolean> getNoMatch() {
        return noMatch;
    }

    public void search(String typed) {
        query.setValue(typed);
    }

    private void showSearch() {
        List<Exercise> all = exercises.getValue();
        if (all == null) return;
        String typed = query.getValue() != null ? query.getValue() : "";
        noMatch.setValue(ExerciseSearch.noMatch(all, typed));
        shownExercises.setValue(ExerciseSearch.matching(all, typed));
    }
}
