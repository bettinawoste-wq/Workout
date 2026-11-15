package com.wob.workout.ui.workout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.WorkoutExercise;
import com.wob.workout.ui.workout.WorkoutExerciseAdapter.ExerciseItem;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkoutDraftTest {

    @Test
    public void totalAddsUpTheExercisesWithoutTransitions() {
        // 10 exercises of one minute: 10:00, like the player's remaining time at the start.
        List<ExerciseItem> items = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            items.add(item(i + 1, "Übung " + i, 60));
        }

        WorkoutDraft draft = new WorkoutDraft("Ganzkörper", items);

        assertEquals(600, draft.totalSeconds());
        assertEquals("10 Übungen · 10:00", draft.summaryText());
    }

    @Test
    public void aDraftAsLoadedHasNoUnsavedChanges() {
        WorkoutDraft draft = legDay();

        assertFalse(draft.hasUnsavedChanges());
    }

    @Test
    public void changingADurationIsAnUnsavedChangeUntilItIsChangedBack() {
        WorkoutDraft draft = legDay();
        ExerciseItem squat = draft.items().get(0);

        draft.changeDuration(squat, 90);
        assertTrue(draft.hasUnsavedChanges());

        draft.changeDuration(squat, 45);
        assertFalse(draft.hasUnsavedChanges());
    }

    @Test
    public void renamingIsAnUnsavedChangeButSurroundingSpacesAreNot() {
        WorkoutDraft draft = legDay();

        draft.changeName(" Leg Day  ");
        assertFalse(draft.hasUnsavedChanges());

        draft.changeName("Arm Day");
        assertTrue(draft.hasUnsavedChanges());
    }

    @Test
    public void addedExercisesGoAtTheEndInTickOrderWith60SecondsEach() {
        WorkoutDraft draft = legDay();

        draft.add(Arrays.asList(exercise(3, "Plank"), exercise(1, "Squat")));

        assertTitles(draft, "Squat", "Lunge", "Plank", "Squat");
        assertEquals(60, draft.items().get(2).workoutExercise.durationSeconds);
        assertEquals(60, draft.items().get(3).workoutExercise.durationSeconds);
        assertTrue(draft.hasUnsavedChanges());
    }

    @Test
    public void removingAnExerciseIsAnUnsavedChange() {
        WorkoutDraft draft = legDay();

        draft.remove(draft.items().get(0));

        assertTitles(draft, "Lunge");
        assertTrue(draft.hasUnsavedChanges());
    }

    @Test
    public void reorderingIsAnUnsavedChangeUntilTheOldOrderIsBack() {
        WorkoutDraft draft = legDay();
        ExerciseItem squat = draft.items().get(0);
        ExerciseItem lunge = draft.items().get(1);

        draft.reorder(Arrays.asList(lunge, squat));
        assertTitles(draft, "Lunge", "Squat");
        assertTrue(draft.hasUnsavedChanges());

        draft.reorder(Arrays.asList(squat, lunge));
        assertFalse(draft.hasUnsavedChanges());
    }

    @Test
    public void aNewWorkoutHasUnsavedChangesOnlyOnceSomethingIsEntered() {
        WorkoutDraft draft = new WorkoutDraft("", new ArrayList<>());
        assertFalse(draft.hasUnsavedChanges());

        draft.changeName("   ");
        assertFalse(draft.hasUnsavedChanges());

        draft.changeName("Leg Day");
        assertTrue(draft.hasUnsavedChanges());
    }

    @Test
    public void theTotalFollowsTheDraft() {
        WorkoutDraft draft = legDay();
        assertEquals(45 + 30, draft.totalSeconds());

        draft.add(Arrays.asList(exercise(3, "Plank")));
        assertEquals(45 + 30 + 60, draft.totalSeconds());

        draft.remove(draft.items().get(0));
        draft.remove(draft.items().get(0));
        assertEquals("1 Übung · 1:00", draft.summaryText());
    }

    @Test
    public void exercisesChangedInTheCatalogReachTheDraftWithoutCountingAsItsChanges() {
        WorkoutDraft draft = legDay();
        Map<Long, Exercise> catalog = new HashMap<>();
        // Squat (1) was renamed on another screen, Lunge (2) deleted there.
        catalog.put(1L, exercise(1, "Back Squat"));

        draft.updateExercises(Arrays.asList(1L, 2L), catalog);

        assertTitles(draft, "Back Squat");
        assertFalse(draft.hasUnsavedChanges());
    }

    @Test
    public void anExerciseDeletedInTheCatalogLeavesTheDraftsOwnChanges() {
        WorkoutDraft draft = legDay();
        draft.changeDuration(draft.items().get(0), 90);
        Map<Long, Exercise> catalog = new HashMap<>();
        catalog.put(1L, exercise(1, "Squat"));

        draft.updateExercises(Arrays.asList(1L, 2L), catalog);

        assertTitles(draft, "Squat");
        assertTrue(draft.hasUnsavedChanges());
    }

    @Test
    public void anExerciseAddedWhileTheCatalogLoadsIsKept() {
        WorkoutDraft draft = legDay();
        // The refresh looked up Squat and Lunge; Plank was added while it ran.
        draft.add(Arrays.asList(exercise(3, "Plank")));
        Map<Long, Exercise> catalog = new HashMap<>();
        catalog.put(1L, exercise(1, "Squat"));
        catalog.put(2L, exercise(2, "Lunge"));

        draft.updateExercises(Arrays.asList(1L, 2L), catalog);

        assertTitles(draft, "Squat", "Lunge", "Plank");
        assertTrue(draft.hasUnsavedChanges());
    }

    @Test
    public void anEmptyNameIsSavedAsNeuesWorkout() {
        WorkoutDraft draft = legDay();

        draft.changeName("   ");
        assertEquals("Neues Workout", draft.nameToSave());

        draft.changeName("  Arm Day ");
        assertEquals("Arm Day", draft.nameToSave());
    }

    @Test
    public void savingWritesTheWorkoutExercisesNumberedInTheirCurrentOrder() {
        WorkoutDraft draft = legDay();
        ExerciseItem squat = draft.items().get(0);
        ExerciseItem lunge = draft.items().get(1);
        draft.reorder(Arrays.asList(lunge, squat));
        draft.add(Arrays.asList(exercise(3, "Plank")));

        List<WorkoutExercise> toSave = draft.workoutExercisesToSave();

        assertEquals(3, toSave.size());
        assertEquals(lunge.workoutExercise.id, toSave.get(0).id);
        assertEquals(squat.workoutExercise.id, toSave.get(1).id);
        assertEquals(0, toSave.get(2).id);
        assertEquals(3, toSave.get(2).exerciseId);
        for (int i = 0; i < toSave.size(); i++) {
            assertEquals(i, toSave.get(i).position);
        }
        // Copies, so writing them off the main thread can't race the adapter's own items.
        assertNotSame(lunge.workoutExercise, toSave.get(0));
    }

    private static void assertTitles(WorkoutDraft draft, String... expectedTitles) {
        List<String> titles = new ArrayList<>();
        for (ExerciseItem item : draft.items()) {
            titles.add(item.exercise.title);
        }
        assertEquals(Arrays.asList(expectedTitles), titles);
    }

    private static Exercise exercise(long id, String title) {
        Exercise exercise = new Exercise();
        exercise.id = id;
        exercise.title = title;
        return exercise;
    }

    private static WorkoutDraft legDay() {
        return new WorkoutDraft("Leg Day",
                Arrays.asList(item(1, "Squat", 45), item(2, "Lunge", 30)));
    }

    private static long nextId = 100;

    /** A stored workout exercise, as the editor loads it. */
    private static ExerciseItem item(long exerciseId, String title, int seconds) {
        Exercise exercise = new Exercise();
        exercise.id = exerciseId;
        exercise.title = title;
        WorkoutExercise workoutExercise = new WorkoutExercise();
        workoutExercise.id = nextId++;
        workoutExercise.exerciseId = exerciseId;
        workoutExercise.durationSeconds = seconds;
        return new ExerciseItem(exercise, workoutExercise);
    }
}
