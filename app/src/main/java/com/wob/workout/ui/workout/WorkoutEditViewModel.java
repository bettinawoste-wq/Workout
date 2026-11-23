package com.wob.workout.ui.workout;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.wob.workout.AppContainer;
import com.wob.workout.data.LiveDataExecutor;
import com.wob.workout.data.WorkoutExercise;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.ui.DeleteConfirmation;

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
    private final MutableLiveData<List<WorkoutExercise>> exercises = new MutableLiveData<>();
    private final MutableLiveData<String> summary = new MutableLiveData<>();
    private final MutableLiveData<String> transition = new MutableLiveData<>();
    private final MutableLiveData<Boolean> unsavedChanges = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> canStart = new MutableLiveData<>(false);
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
                draft = new WorkoutDraft(loaded.workout.name, loaded.workout.transitionSeconds,
                        loaded.workoutExercises);
                publish();
                workout.setValue(loaded.workout);
                ready.setValue(true);
            });
        } else {
            // Nothing to load. A new workout follows the default transition.
            draft = new WorkoutDraft("", null, new ArrayList<>());
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
    public LiveData<List<WorkoutExercise>> getExercises() {
        return exercises;
    }

    /** The draft's exercise count and run time, e.g. "10 Übungen · 10:27". */
    public LiveData<String> getSummary() {
        return summary;
    }

    /** The draft's transition choice, e.g. "Standard (3 s)" or "12 s". */
    public LiveData<String> getTransition() {
        return transition;
    }

    /** The workout's own transition length to start the dialog with: its own one, or the
     *  default transition's while it follows that. */
    public int transitionSecondsToEdit() {
        Integer own = draft != null ? draft.transitionSeconds() : null;
        return container.defaultTransitionSetting.resolve(own);
    }

    /** Takes the workout's own transition length as typed in the transition dialog; the
     *  dialog only shows the answer. Before the draft has loaded there is nothing to
     *  change. */
    public WorkoutDraft.TransitionAnswer changeTransition(String typed) {
        if (draft == null) return WorkoutDraft.TransitionAnswer.ACCEPTED;
        WorkoutDraft.TransitionAnswer answer = draft.changeTransition(typed);
        if (answer == WorkoutDraft.TransitionAnswer.ACCEPTED) publish();
        return answer;
    }

    /** Lets the workout follow the default transition again. */
    public void followDefaultTransition() {
        if (draft == null) return;
        draft.followDefaultTransition();
        publish();
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

    /** Appends a rest of {@link WorkoutDraft#NEW_REST_SECONDS}. */
    public void addRest() {
        if (draft == null) return;
        draft.addRest();
        publish();
    }

    /** Takes a duration as typed in the duration dialog; the dialog only shows the answer.
     *  Before the draft has loaded there is nothing to change. */
    public WorkoutDraft.DurationAnswer changeDuration(int position, String typed) {
        if (draft == null) return WorkoutDraft.DurationAnswer.ACCEPTED;
        WorkoutDraft.DurationAnswer answer = draft.changeDuration(position, typed);
        if (answer == WorkoutDraft.DurationAnswer.ACCEPTED) publish();
        return answer;
    }

    /** @return the removed one, for {@link #restoreExercise}; null if nothing was removed */
    public WorkoutExercise removeExercise(int position) {
        if (draft == null) return null;
        WorkoutExercise removed = draft.remove(position);
        publish();
        return removed;
    }

    /** Undoes {@link #removeExercise}: same place, same duration. */
    public void restoreExercise(int position, WorkoutExercise removed) {
        if (draft == null || removed == null) return;
        draft.restore(position, removed);
        publish();
    }

    /** One step of a drag: the workout exercise at {@code from} now stands at {@code to}. */
    public void moveExercise(int from, int to) {
        if (draft == null) return;
        draft.move(from, to);
        publish();
    }

    /** Safe to call every time the Fragment's view is (re)created, not just once — this is
     *  how a change made on another screen (e.g. deleting the Exercise this workout uses)
     *  while this Fragment sat on the back stack gets picked up on return. */
    public void refreshExercises() {
        if (draft == null || draft.workoutExercises().isEmpty()) return;
        // Exercises added while this runs are not in here, so the draft keeps them.
        Set<Long> exerciseIds = new HashSet<>();
        for (WorkoutExercise workoutExercise : draft.workoutExercises()) {
            if (!workoutExercise.isRest()) exerciseIds.add(workoutExercise.exerciseId());
        }
        if (exerciseIds.isEmpty()) return;
        LiveDataExecutor.runOnExecutorOnce(container.ioExecutor, () -> {
            Map<Long, Exercise> found = new HashMap<>();
            for (Exercise exercise : container.exerciseRepository.getByIds(exerciseIds)) {
                found.put(exercise.id, exercise);
            }
            return found;
        }, found -> {
            if (found == null) return;
            draft.updateExercises(exerciseIds, found);
            publish();
        });
    }

    /** Writes the draft: the name (an empty one as {@link WorkoutDraft#DEFAULT_NAME}), the
     *  transition choice and the workout exercises; creates a new workout.
     *  @return true once saved, null if not saved: still loading, or saving failed. */
    public LiveData<Boolean> save() {
        return saveDraftThen(name -> true);
    }

    /** Whether the workout as edited has anything to play, so the editor's start is on. */
    public LiveData<Boolean> getCanStart() {
        return canStart;
    }

    /** Saves the draft, without asking, so the player plays what the editor shows, and
     *  takes the saved workout as the new draft: back from the player, nothing is left
     *  unsaved.
     *  @return the saved workout's id, or null if still loading or saving failed. */
    public LiveData<Long> saveToStart() {
        MediatorLiveData<Long> started = new MediatorLiveData<>();
        started.addSource(saveDraftThen(name -> load()), loaded -> {
            if (loaded == null) {
                started.setValue(null);
                return;
            }
            draft = new WorkoutDraft(loaded.workout.name, loaded.workout.transitionSeconds,
                    loaded.workoutExercises);
            publish();
            started.setValue(loaded.workout.id);
        });
        return started;
    }

    /** The name the player shows for the workout just saved to start. */
    public String nameToStart() {
        return draft != null ? draft.nameToSave() : WorkoutDraft.DEFAULT_NAME;
    }

    /** Saves the draft first, so the copy matches what the editor shows, then copies the
     *  workout as "<name> (Kopie)" with the same transition choice, exercises and
     *  durations.
     *  @return the copy's id, or null if still loading or saving or copying failed. */
    public LiveData<Long> duplicate() {
        return saveDraftThen(name ->
                container.workoutRepository.duplicate(workoutId, name + " (Kopie)"));
    }

    /** What the delete confirmation calls the workout: the name field's text, or the stored
     *  name while the field is empty. */
    public String nameToConfirmDelete(String fieldText) {
        Workout loaded = workout.getValue();
        return DeleteConfirmation.name(fieldText, loaded != null ? loaded.name : null);
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
        Integer transitionSeconds = draft.transitionSeconds();
        List<WorkoutExercise> toSave = new ArrayList<>(draft.workoutExercises());
        return LiveDataExecutor.runOnExecutor(container.ioExecutor, () -> {
            workoutId = container.workoutRepository.saveWithExercises(
                    workoutId, name, transitionSeconds, toSave);
            return then.apply(name);
        });
    }

    private void publish() {
        exercises.setValue(new ArrayList<>(draft.workoutExercises()));
        summary.setValue(draft.summaryText());
        transition.setValue(
                draft.transitionText(container.defaultTransitionSetting.getSeconds()));
        unsavedChanges.setValue(draft.hasUnsavedChanges());
        canStart.setValue(draft.canStart());
    }

    /** Runs on the io executor. */
    private Loaded load() {
        Workout loadedWorkout = container.workoutRepository.getById(workoutId);
        if (loadedWorkout == null) return null;
        return new Loaded(
                loadedWorkout, container.workoutRepository.getWorkoutExercises(workoutId));
    }

    private static class Loaded {
        final Workout workout;
        final List<WorkoutExercise> workoutExercises;

        Loaded(Workout workout, List<WorkoutExercise> workoutExercises) {
            this.workout = workout;
            this.workoutExercises = workoutExercises;
        }
    }
}
