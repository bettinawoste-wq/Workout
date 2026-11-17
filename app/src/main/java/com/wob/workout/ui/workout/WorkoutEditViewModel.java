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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * The workout editor works on a {@link WorkoutDraft}: every change stays in it until
 * {@link #save()} writes it, all at once; leaving without saving throws it away, and a new
 * workout left that way is not created at all.
 *
 * <p>The workout is loaded once, with a one-shot load rather than a reactive Room query:
 * while the editor is open the draft, not the database, is what it shows. The Fragment
 * still calls {@link #refreshExercises()} on every view creation, since this Fragment
 * instance can survive on the back stack while the user edits or deletes an
 * {@code Exercise} from the catalog on a different screen (see wayfinder ticket #5); the
 * draft takes those over without losing its own changes.
 *
 * <p>The draft lives on the main thread; only loads and the save itself run on the io
 * executor.
 */
public class WorkoutEditViewModel extends ViewModel {

    private final AppContainer container;
    /** Starts at -1 for a new workout and is set once, on the io executor, when saving
     *  creates the workout; read from both threads, hence volatile. */
    private volatile long workoutId;

    /** Null while an existing workout is still loading. */
    private WorkoutDraft draft;

    private final MutableLiveData<Workout> workout = new MutableLiveData<>();
    private final MutableLiveData<List<WorkoutExerciseAdapter.ExerciseItem>> exercises =
            new MutableLiveData<>();
    private final MutableLiveData<String> summary = new MutableLiveData<>();
    private final MutableLiveData<Boolean> unsavedChanges = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> ready = new MutableLiveData<>(false);

    /** A new workout has no stored name to show; an existing one shows it only once. */
    private boolean nameShown;

    public WorkoutEditViewModel(AppContainer container, long workoutId) {
        this.container = container;
        this.workoutId = workoutId;
        this.nameShown = isNew();

        if (!isNew()) {
            LiveDataExecutor.runOnExecutorOnce(container.ioExecutor, this::load, loaded -> {
                if (loaded == null) return;
                // Before the workout goes out: its observer fills the name field, which
                // reaches changeName() and needs the draft to compare against.
                draft = new WorkoutDraft(loaded.workout.name, loaded.items);
                publish();
                workout.setValue(loaded.workout);
                ready.setValue(true);
            });
        } else {
            // Nothing to load. The transition is what a new workout gets by default.
            draft = new WorkoutDraft("", new ArrayList<>());
            publish();
            ready.setValue(true);
        }
    }

    public boolean isNew() {
        return workoutId <= 0;
    }

    /** True the first time only, for an existing workout: whether the screen should fill
     *  its name field from {@link #getWorkout()}. Afterwards the field itself is the truth
     *  (Android restores it on rotation and on return from the back stack). */
    public boolean takeNameToShow() {
        boolean show = !nameShown;
        nameShown = true;
        return show;
    }

    /** The workout as loaded; stays empty for a new one. */
    public LiveData<Workout> getWorkout() {
        return workout;
    }

    /** The draft's workout exercises, in order. */
    public LiveData<List<WorkoutExerciseAdapter.ExerciseItem>> getExercises() {
        return exercises;
    }

    /** The draft's exercise count and run time, e.g. "10 Übungen · 10:27". */
    public LiveData<String> getSummary() {
        return summary;
    }

    /** Whether the draft is ready to edit and save: at once for a new workout, once loaded
     *  for an existing one. */
    public LiveData<Boolean> getReady() {
        return ready;
    }

    /** Whether leaving now would throw changes away. */
    public LiveData<Boolean> getUnsavedChanges() {
        return unsavedChanges;
    }

    public boolean hasUnsavedChanges() {
        return draft != null && draft.hasUnsavedChanges();
    }

    public void changeName(String name) {
        if (draft == null) return;
        draft.changeName(name);
        publish();
    }

    /** Appends the exercises after the existing ones, in the given order (the order they
     *  were ticked), each with {@link WorkoutDraft#NEW_EXERCISE_SECONDS}. */
    public void addExercises(List<Exercise> toAdd) {
        if (draft == null || toAdd.isEmpty()) return;
        draft.add(toAdd);
        publish();
    }

    public void changeDuration(WorkoutExerciseAdapter.ExerciseItem item, int durationSeconds) {
        if (draft == null) return;
        draft.changeDuration(item, durationSeconds);
        publish();
    }

    /** @return where it was, for {@link #restoreExercise}; -1 if nothing was removed */
    public int removeExercise(WorkoutExerciseAdapter.ExerciseItem item) {
        if (draft == null) return -1;
        int position = draft.remove(item);
        publish();
        return position;
    }

    /** Undoes {@link #removeExercise}: same place, same duration. */
    public void restoreExercise(WorkoutExerciseAdapter.ExerciseItem item, int position) {
        if (draft == null || position < 0) return;
        draft.restore(item, position);
        publish();
    }

    /** @param newOrder the adapter's items as a drag left them */
    public void reorder(List<WorkoutExerciseAdapter.ExerciseItem> newOrder) {
        if (draft == null) return;
        draft.reorder(new ArrayList<>(newOrder));
        publish();
    }

    /** Safe to call every time the Fragment's view is (re)created, not just once — this is
     *  how a change made on another screen (e.g. deleting the Exercise this workout uses)
     *  while this Fragment sat on the back stack gets picked up on return. */
    public void refreshExercises() {
        if (draft == null || draft.items().isEmpty()) return;
        // Exercises added while this runs are not in here, so the draft keeps them.
        Set<Long> exerciseIds = new HashSet<>();
        for (WorkoutExerciseAdapter.ExerciseItem item : draft.items()) {
            exerciseIds.add(item.workoutExercise.exerciseId);
        }
        LiveDataExecutor.runOnExecutorOnce(container.ioExecutor, () -> {
            Map<Long, Exercise> found = new HashMap<>();
            for (long exerciseId : exerciseIds) {
                Exercise exercise = container.exerciseRepository.getById(exerciseId);
                if (exercise != null) found.put(exerciseId, exercise);
            }
            return found;
        }, found -> {
            if (found == null) return;
            draft.updateExercises(exerciseIds, found);
            publish();
        });
    }

    /** Writes the draft: the name (an empty one as {@link WorkoutDraft#DEFAULT_NAME}) and
     *  the workout exercises; creates a new workout.
     *  @return true once saved, null if not saved: still loading, or saving failed. */
    public LiveData<Boolean> save() {
        return saveDraftThen(name -> true);
    }

    /** Saves the draft first, so the copy matches what the editor shows, then copies the
     *  workout as "<name> (Kopie)" with the same exercises and durations.
     *  @return the copy's id, or null if still loading or saving or copying failed. */
    public LiveData<Long> duplicate() {
        return saveDraftThen(name ->
                container.workoutRepository.duplicate(workoutId, name + " (Kopie)"));
    }

    public LiveData<Boolean> delete() {
        return LiveDataExecutor.runOnExecutor(container.ioExecutor, () -> {
            Workout target = new Workout();
            target.id = workoutId;
            container.workoutRepository.delete(target);
            return true;
        });
    }

    /** For the exercise picker sheet, which needs the full exercise catalog (not just
     *  this workout's), loaded once per dialog open rather than kept live. */
    public LiveData<List<Exercise>> loadAllExercises() {
        return LiveDataExecutor.runOnExecutor(
                container.ioExecutor, container.exerciseRepository::getAll);
    }

    /** Writes the draft on the io executor, then runs {@code then} there with the saved
     *  name. Delivers null without writing while the draft is still loading. */
    private <T> LiveData<T> saveDraftThen(Function<String, T> then) {
        if (draft == null) return new MutableLiveData<>(null);
        String name = draft.nameToSave();
        List<WorkoutExercise> toSave = draft.workoutExercisesToSave();
        return LiveDataExecutor.runOnExecutor(container.ioExecutor, () -> {
            workoutId = container.workoutRepository.saveWithExercises(workoutId, name, toSave);
            return then.apply(name);
        });
    }

    private void publish() {
        exercises.setValue(new ArrayList<>(draft.items()));
        summary.setValue(draft.summaryText());
        unsavedChanges.setValue(draft.hasUnsavedChanges());
    }

    /** Runs on the io executor. */
    private Loaded load() {
        Workout loadedWorkout = container.workoutRepository.getById(workoutId);
        if (loadedWorkout == null) return null;
        List<WorkoutExerciseAdapter.ExerciseItem> items = new ArrayList<>();
        for (WorkoutExercise workoutExercise
                : container.workoutExerciseRepository.getByWorkout(workoutId)) {
            Exercise exercise = container.exerciseRepository.getById(workoutExercise.exerciseId);
            if (exercise != null) {
                items.add(new WorkoutExerciseAdapter.ExerciseItem(exercise, workoutExercise));
            }
        }
        return new Loaded(loadedWorkout, items);
    }

    private static class Loaded {
        final Workout workout;
        final List<WorkoutExerciseAdapter.ExerciseItem> items;

        Loaded(Workout workout, List<WorkoutExerciseAdapter.ExerciseItem> items) {
            this.workout = workout;
            this.items = items;
        }
    }
}
