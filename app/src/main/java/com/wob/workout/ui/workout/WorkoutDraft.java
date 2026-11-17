package com.wob.workout.ui.workout;

import com.wob.workout.data.WorkoutExercise;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.ui.ExerciseCount;
import com.wob.workout.ui.MinutesSeconds;

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
 * <p>It owns its workout exercises: they are immutable values, so a change replaces one,
 * and they are addressed by their position in the list. Main thread only.
 */
public class WorkoutDraft {

    /** Saved in place of an empty name. */
    public static final String DEFAULT_NAME = "Neues Workout";

    /** What an added exercise lasts until its duration is changed. */
    public static final int NEW_EXERCISE_SECONDS = 60;

    private final List<WorkoutExercise> workoutExercises;
    private String name;
    /** What was loaded, to compare against: the trimmed name and {@link #rows()}. */
    private final String loadedName;
    private final List<List<Long>> loadedRows;

    /** @param name the stored name, empty for a new workout */
    public WorkoutDraft(String name, List<WorkoutExercise> workoutExercises) {
        this.name = name;
        this.workoutExercises = new ArrayList<>(workoutExercises);
        this.loadedName = name.trim();
        this.loadedRows = rows();
    }

    /** The workout exercises in their current order, as they would be saved. */
    public List<WorkoutExercise> workoutExercises() {
        return Collections.unmodifiableList(workoutExercises);
    }

    public void changeName(String name) {
        this.name = name;
    }

    /** Appends the exercises after the existing ones, in the given order (the order they
     *  were ticked), each with {@link #NEW_EXERCISE_SECONDS}. */
    public void add(List<Exercise> exercises) {
        for (Exercise exercise : exercises) {
            workoutExercises.add(WorkoutExercise.added(exercise, NEW_EXERCISE_SECONDS));
        }
    }

    /** @return the removed one, for {@link #restore}; null if there is none at
     *          {@code position} */
    public WorkoutExercise remove(int position) {
        if (!isAt(position)) return null;
        return workoutExercises.remove(position);
    }

    /** Puts a removed workout exercise back where it was, or at the end if the draft has
     *  since become shorter. */
    public void restore(int position, WorkoutExercise workoutExercise) {
        workoutExercises.add(
                Math.min(Math.max(position, 0), workoutExercises.size()), workoutExercise);
    }

    public void changeDuration(int position, int durationSeconds) {
        if (!isAt(position)) return;
        workoutExercises.set(
                position, workoutExercises.get(position).withDuration(durationSeconds));
    }

    /** Takes the workout exercise at {@code from} out and puts it in at {@code to}; the
     *  ones in between shift by one, as in a drag. */
    public void move(int from, int to) {
        if (!isAt(from) || !isAt(to)) return;
        workoutExercises.add(to, workoutExercises.remove(from));
    }

    /** Takes over exercises changed in the catalog since loading, e.g. while the editor sat
     *  on the back stack: renamed ones show their new title, deleted ones disappear. Their
     *  stored workout exercises went with them, so that is not a change of this draft.
     *  Exercises added after the lookup started were not looked up and stay as they are.
     *  @param lookedUp the exercise ids the catalog was asked for
     *  @param found    those of them still in the catalog, by id */
    public void updateExercises(Collection<Long> lookedUp, Map<Long, Exercise> found) {
        List<WorkoutExercise> updated = new ArrayList<>();
        for (WorkoutExercise workoutExercise : workoutExercises) {
            long exerciseId = workoutExercise.exerciseId();
            if (!lookedUp.contains(exerciseId)) {
                updated.add(workoutExercise);
            } else if (found.containsKey(exerciseId)) {
                updated.add(workoutExercise.withExercise(found.get(exerciseId)));
            }
        }
        workoutExercises.clear();
        workoutExercises.addAll(updated);
        loadedRows.removeIf(row -> lookedUp.contains(row.get(1))
                && !found.containsKey(row.get(1)));
    }

    /** The trimmed name, or {@link #DEFAULT_NAME} for an empty one, so a workout never
     *  shows up nameless. */
    public String nameToSave() {
        String trimmed = name.trim();
        return trimmed.isEmpty() ? DEFAULT_NAME : trimmed;
    }

    public boolean hasUnsavedChanges() {
        return !name.trim().equals(loadedName) || !rows().equals(loadedRows);
    }

    /** The exercises' durations added up, without the transitions, so it matches the
     *  remaining time the player starts from. */
    public int totalSeconds() {
        int total = 0;
        for (WorkoutExercise workoutExercise : workoutExercises) {
            total += workoutExercise.durationSeconds;
        }
        return total;
    }

    /** E.g. "10 Übungen · 10:00". */
    public String summaryText() {
        return ExerciseCount.text(workoutExercises.size()) + " · "
                + MinutesSeconds.text(totalSeconds());
    }

    private boolean isAt(int position) {
        return position >= 0 && position < workoutExercises.size();
    }

    /** What saving would write of the workout exercises, as comparable values: per workout
     *  exercise in order its row id (0 while unsaved), exercise and duration. */
    private List<List<Long>> rows() {
        List<List<Long>> rows = new ArrayList<>();
        for (WorkoutExercise workoutExercise : workoutExercises) {
            rows.add(Arrays.asList(workoutExercise.rowId, workoutExercise.exerciseId(),
                    (long) workoutExercise.durationSeconds));
        }
        return rows;
    }
}
