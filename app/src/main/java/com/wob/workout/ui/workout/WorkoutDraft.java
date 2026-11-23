package com.wob.workout.ui.workout;

import com.wob.workout.data.WorkoutExercise;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.player.DefaultTransitionSetting;
import com.wob.workout.ui.ExerciseCount;
import com.wob.workout.ui.MinutesSeconds;
import com.wob.workout.ui.WholeNumber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * What the workout editor shows and changes before "Speichern" writes it: the name, the
 * transition choice (its own length or the default transition) and the workout exercises
 * and rests in their order, with their durations. Knows whether it differs from
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

    /** The shortest an exercise or rest may last. */
    public static final int MIN_DURATION_SECONDS = 1;

    /** What an added rest lasts until its duration is changed. */
    public static final int NEW_REST_SECONDS = 30;

    private final List<WorkoutExercise> workoutExercises;
    private String name;
    /** The workout's own transition length, or null to follow the default transition. */
    private Integer transitionSeconds;
    /** What was loaded, to compare against: the trimmed name, the transition choice and
     *  {@link #rows()}. */
    private final String loadedName;
    private final Integer loadedTransitionSeconds;
    private final List<List<Long>> loadedRows;

    /** @param name              the stored name, empty for a new workout
     *  @param transitionSeconds the workout's own transition length, null if it follows
     *                           the default transition */
    public WorkoutDraft(String name, Integer transitionSeconds,
                        List<WorkoutExercise> workoutExercises) {
        this.name = name;
        this.transitionSeconds = transitionSeconds;
        this.workoutExercises = new ArrayList<>(workoutExercises);
        this.loadedName = name.trim();
        this.loadedTransitionSeconds = transitionSeconds;
        this.loadedRows = rows();
    }

    /** The workout exercises in their current order, as they would be saved. */
    public List<WorkoutExercise> workoutExercises() {
        return Collections.unmodifiableList(workoutExercises);
    }

    public void changeName(String name) {
        this.name = name;
    }

    /** The workout's own transition length, or null while it follows the default
     *  transition. */
    public Integer transitionSeconds() {
        return transitionSeconds;
    }

    /** What {@link #changeTransition(String)} makes of a typed transition length. */
    public enum TransitionAnswer {
        ACCEPTED,
        /** Not a whole number of seconds from 0 to 30; nothing changed. */
        NOT_VALID_SECONDS
    }

    /** Takes the workout's own transition length as typed, e.g. into the transition dialog.
     *  @return whether it was taken over */
    public TransitionAnswer changeTransition(String typed) {
        Integer seconds = WholeNumber.within(typed, DefaultTransitionSetting.MIN_SECONDS,
                DefaultTransitionSetting.MAX_SECONDS);
        if (seconds == null) return TransitionAnswer.NOT_VALID_SECONDS;
        transitionSeconds = seconds;
        return TransitionAnswer.ACCEPTED;
    }

    /** Lets the workout follow the default transition again ("Standard"). */
    public void followDefaultTransition() {
        transitionSeconds = null;
    }

    /** E.g. "Standard (3 s)" while following the default transition, otherwise "12 s".
     *  @param defaultSeconds the default transition's current length */
    public String transitionText(int defaultSeconds) {
        return transitionSeconds == null
                ? "Standard (" + defaultSeconds + " s)"
                : transitionSeconds + " s";
    }

    /** Appends the exercises after the existing ones, in the given order (the order they
     *  were ticked), each with {@link #NEW_EXERCISE_SECONDS}. */
    public void add(List<Exercise> exercises) {
        for (Exercise exercise : exercises) {
            workoutExercises.add(WorkoutExercise.added(exercise, NEW_EXERCISE_SECONDS));
        }
    }

    /** Appends a rest after the existing places, with {@link #NEW_REST_SECONDS}. */
    public void addRest() {
        workoutExercises.add(WorkoutExercise.addedRest(NEW_REST_SECONDS));
    }

    /** Whether the place at {@code position} is a rest that won't run: at the start, at the
     *  end or right after another rest. The editor shows it greyed out and keeps it. */
    public boolean isSkippedRest(int position) {
        return isAt(position) && WorkoutExercise.isSkippedRest(workoutExercises, position);
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

    /** What {@link #changeDuration(int, String)} makes of a typed duration. */
    public enum DurationAnswer {
        ACCEPTED,
        /** Not a whole number of seconds above zero; nothing changed. */
        NOT_VALID_SECONDS
    }

    /** Takes a duration as typed, e.g. into the duration dialog.
     *  @return whether it was taken over */
    public DurationAnswer changeDuration(int position, String typed) {
        Integer seconds = WholeNumber.within(typed, MIN_DURATION_SECONDS, Integer.MAX_VALUE);
        if (seconds == null) return DurationAnswer.NOT_VALID_SECONDS;
        changeDuration(position, seconds);
        return DurationAnswer.ACCEPTED;
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
            Long exerciseId = workoutExercise.exerciseId();
            // A rest has no exercise to look up.
            if (exerciseId == null || !lookedUp.contains(exerciseId)) {
                updated.add(workoutExercise);
            } else if (found.containsKey(exerciseId)) {
                updated.add(workoutExercise.withExercise(found.get(exerciseId)));
            }
        }
        workoutExercises.clear();
        workoutExercises.addAll(updated);
        loadedRows.removeIf(row -> row.get(1) != null && lookedUp.contains(row.get(1))
                && !found.containsKey(row.get(1)));
    }

    /** The trimmed name, or {@link #DEFAULT_NAME} for an empty one, so a workout never
     *  shows up nameless. */
    public String nameToSave() {
        String trimmed = name.trim();
        return trimmed.isEmpty() ? DEFAULT_NAME : trimmed;
    }

    /** Whether there is anything to play: at least one exercise, rests not counting. */
    public boolean canStart() {
        return exerciseCount() > 0;
    }

    public boolean hasUnsavedChanges() {
        return !name.trim().equals(loadedName)
                || !Objects.equals(transitionSeconds, loadedTransitionSeconds)
                || !rows().equals(loadedRows);
    }

    /** The exercises' durations added up, without the rests and transitions, so it matches
     *  the remaining time the player starts from. */
    public int totalSeconds() {
        int total = 0;
        for (WorkoutExercise workoutExercise : workoutExercises) {
            if (!workoutExercise.isRest()) total += workoutExercise.durationSeconds;
        }
        return total;
    }

    /** E.g. "10 Übungen · 10:00"; rests count neither as exercises nor towards the time. */
    public String summaryText() {
        return ExerciseCount.text(exerciseCount()) + " · " + MinutesSeconds.text(totalSeconds());
    }

    /** The exercises in the workout, rests not counting. */
    private int exerciseCount() {
        int exercises = 0;
        for (WorkoutExercise workoutExercise : workoutExercises) {
            if (!workoutExercise.isRest()) exercises++;
        }
        return exercises;
    }

    private boolean isAt(int position) {
        return position >= 0 && position < workoutExercises.size();
    }

    /** What saving would write of the workout exercises, as comparable values: per workout
     *  exercise in order its row id (0 while unsaved), exercise (null for a rest) and
     *  duration. */
    private List<List<Long>> rows() {
        List<List<Long>> rows = new ArrayList<>();
        for (WorkoutExercise workoutExercise : workoutExercises) {
            rows.add(Arrays.asList(workoutExercise.rowId, workoutExercise.exerciseId(),
                    (long) workoutExercise.durationSeconds));
        }
        return rows;
    }
}
