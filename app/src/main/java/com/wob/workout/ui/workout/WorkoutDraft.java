package com.wob.workout.ui.workout;

import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.WorkoutExercise;
import com.wob.workout.ui.ExerciseCount;
import com.wob.workout.ui.MinutesSeconds;
import com.wob.workout.ui.workout.WorkoutExerciseAdapter.ExerciseItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * What the workout editor shows and changes before "Speichern" writes it: the name and the
 * workout exercises in their order, with their durations. Knows whether it differs from
 * what was loaded, so leaving can ask before throwing changes away.
 *
 * <p>Main thread only: its items are the same objects the editor's adapter shows.
 */
public class WorkoutDraft {

    /** Saved in place of an empty name. */
    public static final String DEFAULT_NAME = "Neues Workout";

    /** What an added exercise lasts until its duration is changed. */
    public static final int NEW_EXERCISE_SECONDS = 60;

    private final List<ExerciseItem> items;
    private String name;
    /** What was loaded, to compare against: the trimmed name and {@link #rows()}. */
    private final String loadedName;
    private final List<List<Long>> loadedRows;

    /** @param name the stored name, empty for a new workout */
    public WorkoutDraft(String name, List<ExerciseItem> items) {
        this.name = name;
        this.items = new ArrayList<>(items);
        this.loadedName = name.trim();
        this.loadedRows = rows();
    }

    /** The workout exercises in their current order; not to be changed by the caller. */
    public List<ExerciseItem> items() {
        return Collections.unmodifiableList(items);
    }

    public void changeName(String name) {
        this.name = name;
    }

    /** Appends the exercises after the existing ones, in the given order (the order they
     *  were ticked), each with {@link #NEW_EXERCISE_SECONDS}. */
    public void add(List<Exercise> exercises) {
        for (Exercise exercise : exercises) {
            WorkoutExercise workoutExercise = new WorkoutExercise();
            workoutExercise.exerciseId = exercise.id;
            workoutExercise.durationSeconds = NEW_EXERCISE_SECONDS;
            items.add(new ExerciseItem(exercise, workoutExercise));
        }
    }

    /** @return where it was, for {@link #restore}; -1 if it wasn't in the draft */
    public int remove(ExerciseItem item) {
        int position = items.indexOf(item);
        if (position >= 0) items.remove(position);
        return position;
    }

    /** Puts a removed workout exercise back where it was, or at the end if the draft has
     *  since become shorter. */
    public void restore(ExerciseItem item, int position) {
        items.add(Math.min(Math.max(position, 0), items.size()), item);
    }

    public void changeDuration(ExerciseItem item, int durationSeconds) {
        item.workoutExercise.durationSeconds = durationSeconds;
    }

    /** @param newOrder the same items, e.g. as a drag left them in the adapter */
    public void reorder(List<ExerciseItem> newOrder) {
        items.clear();
        items.addAll(newOrder);
    }

    /** Takes over exercises changed in the catalog since loading, e.g. while the editor sat
     *  on the back stack: renamed ones show their new title, deleted ones disappear. Their
     *  stored workout exercises went with them, so that is not a change of this draft.
     *  Exercises added after the lookup started were not looked up and stay as they are.
     *  @param lookedUp the exercise ids the catalog was asked for
     *  @param found    those of them still in the catalog, by id */
    public void updateExercises(Collection<Long> lookedUp, Map<Long, Exercise> found) {
        List<ExerciseItem> updated = new ArrayList<>();
        for (ExerciseItem item : items) {
            long exerciseId = item.workoutExercise.exerciseId;
            if (!lookedUp.contains(exerciseId)) {
                updated.add(item);
            } else if (found.containsKey(exerciseId)) {
                updated.add(new ExerciseItem(found.get(exerciseId), item.workoutExercise));
            }
        }
        items.clear();
        items.addAll(updated);
        loadedRows.removeIf(row -> lookedUp.contains(row.get(1))
                && !found.containsKey(row.get(1)));
    }

    /** The trimmed name, or {@link #DEFAULT_NAME} for an empty one, so a workout never
     *  shows up nameless. */
    public String nameToSave() {
        String trimmed = name.trim();
        return trimmed.isEmpty() ? DEFAULT_NAME : trimmed;
    }

    /** Copies of the workout exercises, numbered in their current order, for saving off
     *  the main thread; new ones still have id 0. */
    public List<WorkoutExercise> workoutExercisesToSave() {
        List<WorkoutExercise> toSave = new ArrayList<>();
        for (ExerciseItem item : items) {
            WorkoutExercise copy = new WorkoutExercise();
            copy.id = item.workoutExercise.id;
            copy.workoutId = item.workoutExercise.workoutId;
            copy.exerciseId = item.workoutExercise.exerciseId;
            copy.durationSeconds = item.workoutExercise.durationSeconds;
            copy.position = toSave.size();
            toSave.add(copy);
        }
        return toSave;
    }

    public boolean hasUnsavedChanges() {
        return !name.trim().equals(loadedName) || !rows().equals(loadedRows);
    }

    /** The exercises' durations added up, without the transitions, so it matches the
     *  remaining time the player starts from. */
    public int totalSeconds() {
        int total = 0;
        for (ExerciseItem item : items) {
            total += item.workoutExercise.durationSeconds;
        }
        return total;
    }

    /** E.g. "10 Übungen · 10:00". */
    public String summaryText() {
        return ExerciseCount.text(items.size()) + " · " + MinutesSeconds.text(totalSeconds());
    }

    /** What saving would write of the workout exercises, as comparable values: per workout
     *  exercise in order its stored id (0 while unsaved), exercise and duration. */
    private List<List<Long>> rows() {
        List<List<Long>> rows = new ArrayList<>();
        for (ExerciseItem item : items) {
            rows.add(Arrays.asList(item.workoutExercise.id, item.workoutExercise.exerciseId,
                    (long) item.workoutExercise.durationSeconds));
        }
        return rows;
    }
}
