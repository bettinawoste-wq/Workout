package com.wob.workout.ui.workout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.wob.workout.data.WorkoutExercise;
import com.wob.workout.data.entity.Exercise;

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
        List<WorkoutExercise> stored = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            stored.add(stored(i + 1, "Übung " + i, 60));
        }

        WorkoutDraft draft = new WorkoutDraft("Ganzkörper", stored);

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

        draft.changeDuration(0, 90);
        assertTrue(draft.hasUnsavedChanges());

        draft.changeDuration(0, 45);
        assertFalse(draft.hasUnsavedChanges());
    }

    @Test
    public void changingADurationReplacesTheValueInsteadOfChangingIt() {
        WorkoutDraft draft = legDay();
        WorkoutExercise before = draft.workoutExercises().get(0);

        draft.changeDuration(0, 90);

        WorkoutExercise after = draft.workoutExercises().get(0);
        assertNotSame(before, after);
        assertEquals(45, before.durationSeconds);
        assertEquals(90, after.durationSeconds);
        assertEquals(before.rowId, after.rowId);
        assertSame(before.exercise, after.exercise);
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
        assertEquals(60, draft.workoutExercises().get(2).durationSeconds);
        assertEquals(60, draft.workoutExercises().get(3).durationSeconds);
        assertEquals(WorkoutExercise.UNSAVED, draft.workoutExercises().get(3).rowId);
        assertTrue(draft.hasUnsavedChanges());
    }

    @Test
    public void removingAnExerciseIsAnUnsavedChange() {
        WorkoutDraft draft = legDay();

        WorkoutExercise removed = draft.remove(0);

        assertEquals("Squat", removed.exercise.title);
        assertTitles(draft, "Lunge");
        assertTrue(draft.hasUnsavedChanges());
    }

    @Test
    public void removingAtAPositionThatIsGoneRemovesNothing() {
        WorkoutDraft draft = legDay();

        assertNull(draft.remove(2));
        assertNull(draft.remove(-1));
        assertTitles(draft, "Squat", "Lunge");
    }

    @Test
    public void restoringPutsTheRemovedOneBackWhereItWas() {
        WorkoutDraft draft = legDay();
        draft.add(Arrays.asList(exercise(3, "Plank")));
        WorkoutExercise lunge = draft.remove(1);

        draft.restore(1, lunge);

        assertTitles(draft, "Squat", "Lunge", "Plank");
    }

    @Test
    public void restoringIntoADraftThatGotShorterPutsItAtTheEnd() {
        WorkoutDraft draft = legDay();
        WorkoutExercise lunge = draft.remove(1);
        draft.remove(0);

        draft.restore(1, lunge);

        assertTitles(draft, "Lunge");
    }

    @Test
    public void movingIsAnUnsavedChangeUntilTheOldOrderIsBack() {
        WorkoutDraft draft = legDay();

        draft.move(0, 1);
        assertTitles(draft, "Lunge", "Squat");
        assertTrue(draft.hasUnsavedChanges());

        draft.move(1, 0);
        assertTitles(draft, "Squat", "Lunge");
        assertFalse(draft.hasUnsavedChanges());
    }

    @Test
    public void movingAcrossSeveralPlacesShiftsTheOnesInBetween() {
        WorkoutDraft draft = legDay();
        draft.add(Arrays.asList(exercise(3, "Plank"), exercise(4, "Bridge")));

        draft.move(3, 0);
        assertTitles(draft, "Bridge", "Squat", "Lunge", "Plank");

        draft.move(0, 2);
        assertTitles(draft, "Squat", "Lunge", "Bridge", "Plank");
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

        draft.remove(0);
        draft.remove(0);
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
        draft.changeDuration(0, 90);
        Map<Long, Exercise> catalog = new HashMap<>();
        catalog.put(1L, exercise(1, "Squat"));

        draft.updateExercises(Arrays.asList(1L, 2L), catalog);

        assertTitles(draft, "Squat");
        assertEquals(90, draft.workoutExercises().get(0).durationSeconds);
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
    public void theWorkoutExercisesCannotBeChangedFromOutside() {
        WorkoutDraft draft = legDay();

        try {
            draft.workoutExercises().remove(0);
        } catch (UnsupportedOperationException expected) {
            // As intended.
        }

        assertTitles(draft, "Squat", "Lunge");
    }

    private static void assertTitles(WorkoutDraft draft, String... expectedTitles) {
        List<String> titles = new ArrayList<>();
        for (WorkoutExercise workoutExercise : draft.workoutExercises()) {
            titles.add(workoutExercise.exercise.title);
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
                Arrays.asList(stored(1, "Squat", 45), stored(2, "Lunge", 30)));
    }

    private static long nextRowId = 100;

    /** A stored workout exercise, as the editor loads it. */
    private static WorkoutExercise stored(long exerciseId, String title, int seconds) {
        return new WorkoutExercise(nextRowId++, exercise(exerciseId, title), seconds);
    }
}
