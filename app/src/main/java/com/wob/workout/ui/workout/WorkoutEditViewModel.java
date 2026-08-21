package com.wob.workout.ui.workout;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.wob.workout.AppContainer;
import com.wob.workout.data.LiveDataExecutor;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.data.entity.WorkoutExercise;

import java.util.ArrayList;
import java.util.List;

/**
 * An explicit exception to the reactive-LiveData pattern {@link WorkoutListViewModel}
 * established: every mutation to this screen's exercise list originates from this screen
 * itself, so there's no external-staleness problem a Room-native reactive query would be
 * solving — and a fresh reactive emission racing a drag gesture's own optimistic reorder
 * would be a real interaction wrinkle for no benefit. Instead, {@link #exercises} is a
 * plain one-shot-per-write LiveData: each mutating method performs its write, then re-runs
 * the same load composition to refresh it. See wayfinder ticket #5.
 */
public class WorkoutEditViewModel extends ViewModel {

    private final AppContainer container;
    private final long workoutId;

    private final MutableLiveData<Workout> workout = new MutableLiveData<>();
    private final MutableLiveData<List<WorkoutExerciseAdapter.ExerciseItem>> exercises =
            new MutableLiveData<>();

    public WorkoutEditViewModel(AppContainer container, long workoutId) {
        this.container = container;
        this.workoutId = workoutId;

        if (!isNew()) {
            LiveDataExecutor.runOnExecutorOnce(
                    container.ioExecutor, () -> container.workoutRepository.getById(workoutId),
                    workout::setValue);
            loadExercises();
        }
    }

    public boolean isNew() {
        return workoutId <= 0;
    }

    public LiveData<Workout> getWorkout() {
        return workout;
    }

    public LiveData<List<WorkoutExerciseAdapter.ExerciseItem>> getExercises() {
        return exercises;
    }

    public LiveData<Boolean> save(String name) {
        Workout target = workout.getValue() != null ? workout.getValue() : new Workout();
        target.id = isNew() ? 0 : workoutId;
        target.name = name;
        boolean isInsert = target.id == 0;

        return LiveDataExecutor.runOnExecutor(container.ioExecutor, () -> {
            if (isInsert) {
                container.workoutRepository.save(target);
            } else {
                container.workoutRepository.update(target);
            }
            return true;
        });
    }

    public LiveData<Boolean> delete() {
        Workout target = workout.getValue() != null ? workout.getValue() : new Workout();
        target.id = workoutId;

        return LiveDataExecutor.runOnExecutor(container.ioExecutor, () -> {
            container.workoutRepository.delete(target);
            return true;
        });
    }

    /** For the exercise-picker dialog, which needs the full exercise catalog (not just
     *  this workout's), loaded once per dialog open rather than kept live. */
    public LiveData<List<Exercise>> loadAllExercises() {
        return LiveDataExecutor.runOnExecutor(container.ioExecutor, container.exerciseRepository::getAll);
    }

    public void addExercise(Exercise exercise, int durationSeconds) {
        writeThenReload(() -> {
            WorkoutExercise workoutExercise = new WorkoutExercise();
            workoutExercise.workoutId = workoutId;
            workoutExercise.exerciseId = exercise.id;
            workoutExercise.position = container.workoutExerciseRepository.getByWorkout(workoutId).size();
            workoutExercise.durationSeconds = durationSeconds;
            container.workoutExerciseRepository.save(workoutExercise);
        });
    }

    public void changeDuration(WorkoutExerciseAdapter.ExerciseItem item, int durationSeconds) {
        writeThenReload(() -> {
            item.workoutExercise.durationSeconds = durationSeconds;
            container.workoutExerciseRepository.update(item.workoutExercise);
        });
    }

    public void removeExercise(WorkoutExerciseAdapter.ExerciseItem item) {
        writeThenReload(() -> container.workoutExerciseRepository.delete(item.workoutExercise));
    }

    public void reorder(List<WorkoutExerciseAdapter.ExerciseItem> newOrder) {
        writeThenReload(() -> {
            for (int i = 0; i < newOrder.size(); i++) {
                WorkoutExercise workoutExercise = newOrder.get(i).workoutExercise;
                if (workoutExercise.position != i) {
                    workoutExercise.position = i;
                    container.workoutExerciseRepository.update(workoutExercise);
                }
            }
        });
    }

    private void loadExercises() {
        LiveDataExecutor.runOnExecutorOnce(container.ioExecutor, this::loadExerciseItems, exercises::setValue);
    }

    private void writeThenReload(Runnable write) {
        LiveDataExecutor.runOnExecutorOnce(container.ioExecutor, () -> {
            write.run();
            return loadExerciseItems();
        }, exercises::setValue);
    }

    private List<WorkoutExerciseAdapter.ExerciseItem> loadExerciseItems() {
        List<WorkoutExerciseAdapter.ExerciseItem> items = new ArrayList<>();

        for (WorkoutExercise workoutExercise : container.workoutExerciseRepository.getByWorkout(workoutId)) {
            Exercise exercise = container.exerciseRepository.getById(workoutExercise.exerciseId);
            if (exercise != null) {
                items.add(new WorkoutExerciseAdapter.ExerciseItem(exercise, workoutExercise));
            }
        }

        return items;
    }
}
