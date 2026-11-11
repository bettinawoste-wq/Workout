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
 * the same load composition to refresh it. The Fragment also calls {@link #refreshExercises()}
 * on every view creation (not just the first), since this Fragment instance can survive on
 * the back stack while the user edits or deletes an {@code Exercise} from the catalog on a
 * different screen — a real external staleness source the original "self-contained" framing
 * missed. See wayfinder ticket #5.
 */
public class WorkoutEditViewModel extends ViewModel {

    /** Stored in place of an empty name, so a workout never shows up nameless. */
    public static final String DEFAULT_NAME = "Neues Workout";

    private final AppContainer container;
    /** Starts at -1 for a new workout and is set once, on the io executor, when the first
     *  change creates the workout; read from both threads, hence volatile. */
    private volatile long workoutId;

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
            refreshExercises();
        }
    }

    public boolean isNew() {
        return workoutId <= 0;
    }

    /** Saves the name right away; an empty one is saved as {@link #DEFAULT_NAME}. A new
     *  workout is created by its first non-empty name, not by an empty one. */
    public void changeName(String name) {
        String trimmed = name.trim();
        String toSave = trimmed.isEmpty() ? DEFAULT_NAME : trimmed;
        // Checked on the executor, behind any creation still queued there.
        container.ioExecutor.execute(() -> {
            if (trimmed.isEmpty() && isNew()) return;
            if (!createIfNew(toSave)) {
                container.workoutRepository.updateName(workoutId, toSave);
            }
        });
    }

    /** Runs on the io executor. Creates the workout if it doesn't exist yet, so the first
     *  change to a new workout saves it, and publishes it through {@link #getWorkout()}.
     *  @return true if this call created it. */
    private boolean createIfNew(String name) {
        if (!isNew()) return false;
        Workout created = new Workout();
        created.name = name;
        created.id = container.workoutRepository.save(created);
        workoutId = created.id;
        workout.postValue(created);
        return true;
    }

    public LiveData<Workout> getWorkout() {
        return workout;
    }

    public LiveData<List<WorkoutExerciseAdapter.ExerciseItem>> getExercises() {
        return exercises;
    }

    /** Safe to call every time the Fragment's view is (re)created, not just once — this is
     *  how a change made on another screen (e.g. deleting the Exercise this workout uses)
     *  while this Fragment sat on the back stack gets picked up on return. */
    public void refreshExercises() {
        if (isNew()) return;
        writeThenReload(() -> { });
    }

    public LiveData<Boolean> delete() {
        return LiveDataExecutor.runOnExecutor(container.ioExecutor, () -> {
            Workout target = new Workout();
            target.id = workoutId;
            container.workoutRepository.delete(target);
            return true;
        });
    }

    /** Copies this workout as "<name> (Kopie)" with the same exercises and durations.
     *  @return the copy's id, or null if copying failed. */
    public LiveData<Long> duplicate() {
        return LiveDataExecutor.runOnExecutor(container.ioExecutor, () -> {
            Workout source = container.workoutRepository.getById(workoutId);
            return container.workoutRepository.duplicate(workoutId, source.name + " (Kopie)");
        });
    }

    /** For the exercise-picker dialog, which needs the full exercise catalog (not just
     *  this workout's), loaded once per dialog open rather than kept live. */
    public LiveData<List<Exercise>> loadAllExercises() {
        return LiveDataExecutor.runOnExecutor(container.ioExecutor, container.exerciseRepository::getAll);
    }

    public void addExercise(Exercise exercise, int durationSeconds) {
        writeThenReload(() -> {
            createIfNew(DEFAULT_NAME);
            WorkoutExercise workoutExercise = new WorkoutExercise();
            workoutExercise.workoutId = workoutId;
            workoutExercise.exerciseId = exercise.id;
            workoutExercise.position = container.workoutExerciseRepository.getByWorkout(workoutId).size();
            workoutExercise.durationSeconds = durationSeconds;
            container.workoutExerciseRepository.save(workoutExercise);
        });
    }

    public void changeDuration(WorkoutExerciseAdapter.ExerciseItem item, int durationSeconds) {
        // Mutate on the caller's (main) thread — item.workoutExercise is the same object the
        // adapter's ViewHolder may be reading right now; only the DB write itself belongs
        // on the background executor.
        item.workoutExercise.durationSeconds = durationSeconds;
        writeThenReload(() -> container.workoutExerciseRepository.update(item.workoutExercise));
    }

    public void removeExercise(WorkoutExerciseAdapter.ExerciseItem item) {
        writeThenReload(() -> container.workoutExerciseRepository.delete(item.workoutExercise));
    }

    public void reorder(List<WorkoutExerciseAdapter.ExerciseItem> newOrder) {
        // Renumber synchronously here, on the caller's (main) thread, for the same reason as
        // changeDuration() above — these WorkoutExercise objects are adapter-owned, and
        // newOrder is the adapter's own live list, not a snapshot.
        List<WorkoutExercise> toUpdate = new ArrayList<>();
        for (int i = 0; i < newOrder.size(); i++) {
            WorkoutExercise workoutExercise = newOrder.get(i).workoutExercise;
            if (workoutExercise.position != i) {
                workoutExercise.position = i;
                toUpdate.add(workoutExercise);
            }
        }

        writeThenReload(() -> {
            for (WorkoutExercise workoutExercise : toUpdate) {
                container.workoutExerciseRepository.update(workoutExercise);
            }
        });
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
