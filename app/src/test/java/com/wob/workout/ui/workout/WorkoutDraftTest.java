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

        WorkoutDraft draft = new WorkoutDraft("Ganzkörper", null, stored);

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
    public void aDurationAsTypedIsAcceptedWhenItIsAWholePositiveNumberOfSeconds() {
        WorkoutDraft draft = legDay();

        assertEquals(WorkoutDraft.DurationAnswer.ACCEPTED, draft.changeDuration(0, " 90 "));

        assertEquals(90, draft.workoutExercises().get(0).durationSeconds);
    }

    @Test
    public void aDurationThatIsNotAValidNumberOfSecondsIsRejectedAndChangesNothing() {
        WorkoutDraft draft = legDay();

        for (String typed : new String[] {"", "  ", "0", "-5", "1.5", "abc", "99999999999"}) {
            assertEquals(typed, WorkoutDraft.DurationAnswer.NOT_VALID_SECONDS,
                    draft.changeDuration(0, typed));
        }

        assertEquals(45, draft.workoutExercises().get(0).durationSeconds);
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
        WorkoutDraft draft = new WorkoutDraft("", null, new ArrayList<>());
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

    // Transition.

    @Test
    public void aWorkoutWithoutItsOwnTransitionShowsTheDefault() {
        WorkoutDraft draft = legDay();

        assertNull(draft.transitionSeconds());
        assertEquals("Standard (3 s)", draft.transitionText(3));
        assertEquals("Standard (0 s)", draft.transitionText(0));
    }

    @Test
    public void aWorkoutsOwnTransitionShowsItsLength() {
        WorkoutDraft draft = new WorkoutDraft("Stretching", 12, new ArrayList<>());

        assertEquals(Integer.valueOf(12), draft.transitionSeconds());
        assertEquals("12 s", draft.transitionText(3));
    }

    @Test
    public void aTransitionAsTypedIsAcceptedFromZeroToThirtySeconds() {
        WorkoutDraft draft = legDay();

        assertEquals(WorkoutDraft.TransitionAnswer.ACCEPTED, draft.changeTransition(" 0 "));
        assertEquals(Integer.valueOf(0), draft.transitionSeconds());

        assertEquals(WorkoutDraft.TransitionAnswer.ACCEPTED, draft.changeTransition("30"));
        assertEquals(Integer.valueOf(30), draft.transitionSeconds());
    }

    @Test
    public void aTransitionOutsideZeroToThirtySecondsIsRejectedAndChangesNothing() {
        WorkoutDraft draft = new WorkoutDraft("Stretching", 12, new ArrayList<>());

        for (String typed : new String[] {"31", "-1", "", "2.5", "drei"}) {
            assertEquals(typed, WorkoutDraft.TransitionAnswer.NOT_VALID_SECONDS,
                    draft.changeTransition(typed));
        }

        assertEquals(Integer.valueOf(12), draft.transitionSeconds());
        assertFalse(draft.hasUnsavedChanges());
    }

    @Test
    public void aWorkoutCanGoBackToTheDefaultTransition() {
        WorkoutDraft draft = new WorkoutDraft("Stretching", 12, new ArrayList<>());

        draft.followDefaultTransition();

        assertNull(draft.transitionSeconds());
        assertEquals("Standard (5 s)", draft.transitionText(5));
    }

    @Test
    public void theTransitionChoiceIsAnUnsavedChangeUntilItIsChangedBack() {
        WorkoutDraft draft = legDay();

        draft.changeTransition("3");
        assertTrue("own 3 s is not the default, even at the same length",
                draft.hasUnsavedChanges());

        draft.followDefaultTransition();
        assertFalse(draft.hasUnsavedChanges());

        WorkoutDraft own = new WorkoutDraft("Stretching", 12, new ArrayList<>());
        own.followDefaultTransition();
        assertTrue(own.hasUnsavedChanges());
        own.changeTransition("12");
        assertFalse(own.hasUnsavedChanges());
    }

    // Rests.

    @Test
    public void anAddedRestGoesAtTheEndWith30Seconds() {
        WorkoutDraft draft = legDay();

        draft.addRest();

        assertTitles(draft, "Squat", "Lunge", "Pause");
        WorkoutExercise rest = draft.workoutExercises().get(2);
        assertTrue(rest.isRest());
        assertEquals(30, rest.durationSeconds);
        assertEquals(WorkoutExercise.UNSAVED, rest.rowId);
        assertTrue(draft.hasUnsavedChanges());
    }

    @Test
    public void aRestIsMovedRetimedRemovedAndRestoredLikeAnExercise() {
        WorkoutDraft draft = legDay();
        draft.addRest();

        draft.move(2, 1);
        assertTitles(draft, "Squat", "Pause", "Lunge");

        assertEquals(WorkoutDraft.DurationAnswer.ACCEPTED, draft.changeDuration(1, "45"));
        assertEquals(45, draft.workoutExercises().get(1).durationSeconds);
        assertTrue(draft.workoutExercises().get(1).isRest());

        WorkoutExercise removed = draft.remove(1);
        assertTitles(draft, "Squat", "Lunge");
        draft.restore(1, removed);
        assertTitles(draft, "Squat", "Pause", "Lunge");
        assertEquals(45, draft.workoutExercises().get(1).durationSeconds);
    }

    @Test
    public void aWorkoutWithAnExerciseCanStart() {
        assertTrue(legDay().canStart());
    }

    @Test
    public void anEmptyWorkoutOrOneOfRestsOnlyCannotStart() {
        assertFalse(new WorkoutDraft("", null, new ArrayList<>()).canStart());
        assertFalse(new WorkoutDraft("Breather", null,
                Arrays.asList(storedRest(30), storedRest(20))).canStart());
    }

    @Test
    public void aStoredRestIsUnchangedUntilItsDurationOrPlaceChanges() {
        WorkoutDraft draft = new WorkoutDraft("Leg Day", null, Arrays.asList(
                stored(1, "Squat", 45), storedRest(30), stored(2, "Lunge", 30)));
        assertFalse(draft.hasUnsavedChanges());

        draft.changeDuration(1, 60);
        assertTrue(draft.hasUnsavedChanges());
        draft.changeDuration(1, 30);
        assertFalse(draft.hasUnsavedChanges());
    }

    @Test
    public void restsAtTheStartTheEndOrRightAfterAnotherRestAreSkipped() {
        WorkoutDraft draft = new WorkoutDraft("Leg Day", null, Arrays.asList(
                storedRest(30), stored(1, "Squat", 45), storedRest(30), storedRest(30),
                stored(2, "Lunge", 30), storedRest(30), storedRest(30)));

        List<Boolean> skipped = new ArrayList<>();
        for (int i = 0; i < draft.workoutExercises().size(); i++) {
            skipped.add(draft.isSkippedRest(i));
        }

        assertEquals(Arrays.asList(true, false, false, true, false, true, true), skipped);
        assertEquals("nothing is removed", 7, draft.workoutExercises().size());
    }

    @Test
    public void theSummaryCountsExercisesAndTheirTimeOnly() {
        WorkoutDraft draft = legDay();

        draft.addRest();
        draft.move(2, 1);

        assertEquals(45 + 30, draft.totalSeconds());
        assertEquals("2 Übungen · 1:15", draft.summaryText());
    }

    @Test
    public void aDraftOfRestsOnlyHasNoExercises() {
        WorkoutDraft draft = new WorkoutDraft("", null, new ArrayList<>());
        draft.addRest();
        draft.addRest();

        assertEquals("0 Übungen · 0:00", draft.summaryText());
    }

    @Test
    public void restsStayWhenTheCatalogIsUpdated() {
        WorkoutDraft draft = new WorkoutDraft("Leg Day", null, Arrays.asList(
                stored(1, "Squat", 45), storedRest(20), stored(2, "Lunge", 30)));
        Map<Long, Exercise> catalog = new HashMap<>();
        catalog.put(1L, exercise(1, "Squat"));

        draft.updateExercises(Arrays.asList(1L, 2L), catalog);

        assertTitles(draft, "Squat", "Pause");
        assertFalse(draft.hasUnsavedChanges());
    }

    private static void assertTitles(WorkoutDraft draft, String... expectedTitles) {
        List<String> titles = new ArrayList<>();
        for (WorkoutExercise workoutExercise : draft.workoutExercises()) {
            titles.add(workoutExercise.isRest() ? "Pause" : workoutExercise.exercise.title);
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
        return new WorkoutDraft("Leg Day", null,
                Arrays.asList(stored(1, "Squat", 45), stored(2, "Lunge", 30)));
    }

    private static long nextRowId = 100;

    /** A stored workout exercise, as the editor loads it. */
    private static WorkoutExercise stored(long exerciseId, String title, int seconds) {
        return new WorkoutExercise(nextRowId++, exercise(exerciseId, title), seconds);
    }

    /** A stored rest, as the editor loads it. */
    private static WorkoutExercise storedRest(int seconds) {
        return new WorkoutExercise(nextRowId++, null, seconds);
    }
}
