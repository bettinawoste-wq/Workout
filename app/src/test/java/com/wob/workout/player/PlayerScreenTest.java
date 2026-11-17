package com.wob.workout.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.wob.workout.data.WorkoutExercise;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.player.SessionState.Phase;

import org.junit.Test;

/** What the player screen shows for a moment of a workout session. */
public class PlayerScreenTest {

    private static final WorkoutExercise PUSH_UP = workoutExercise("Push-up", "Arme beugen");

    @Test
    public void theCountdownPreviewsTheFirstExercise() {
        PlayerScreen screen = PlayerScreen.of(state(Phase.READY, 0, 2500, true));

        assertEquals("Start in 3", screen.timerText);
        assertEquals("ERSTE ÜBUNG", screen.mediaLabel);
        assertEquals("Push-up", screen.mediaTitle);
        assertEquals("Push-up", screen.title);
        assertEquals("Arme beugen", screen.description);
        assertSame(PUSH_UP.exercise, screen.exercise);
        assertEquals("1 / 3", screen.progressText);
    }

    @Test
    public void theTransitionPreviewsTheNextExercise() {
        PlayerScreen screen = PlayerScreen.of(state(Phase.TRANSITION, 1, 1000, true));

        assertEquals("Start in 1", screen.timerText);
        assertEquals("NÄCHSTE ÜBUNG", screen.mediaLabel);
        assertEquals("2 / 3", screen.progressText);
    }

    @Test
    public void anExerciseShowsItsTimeLeftAndNoPreview() {
        PlayerScreen screen = PlayerScreen.of(state(Phase.ACTIVE, 0, 48_900, true));

        assertEquals("00:48", screen.timerText);
        assertNull(screen.mediaLabel);
        assertNull(screen.mediaTitle);
        assertEquals("Push-up", screen.title);
    }

    @Test
    public void longExercisesShowMinutes() {
        assertEquals("10:05",
                PlayerScreen.of(state(Phase.ACTIVE, 0, 605_000, true)).timerText);
    }

    @Test
    public void theStepButtonsFollowTheSession() {
        PlayerScreen screen = PlayerScreen.of(new SessionState(Phase.ACTIVE, 1, 1, 3, PUSH_UP,
                1000, true, true, false, true, 1000, 3000, null));

        assertTrue(screen.previousEnabled);
        assertFalse(screen.nextEnabled);
    }

    @Test
    public void theButtonShowsPauseWhileRunningAndPlayWhilePaused() {
        PlayerScreen running = PlayerScreen.of(state(Phase.ACTIVE, 0, 5000, true));
        PlayerScreen paused = PlayerScreen.of(state(Phase.ACTIVE, 0, 5000, false));

        assertTrue(running.pauseButtonShowsPause);
        assertEquals("Pause", running.pauseButtonLabel);
        assertFalse(paused.pauseButtonShowsPause);
        assertEquals("Weiter", paused.pauseButtonLabel);
        assertTrue(paused.timerTappable);
    }

    @Test
    public void theWorkoutsRemainingTimeAndProgress() {
        PlayerScreen screen = PlayerScreen.of(new SessionState(Phase.ACTIVE, 1, 0, 3, PUSH_UP,
                5000, true, false, true, true, 90_000, 120_000, null));

        assertEquals("Noch 01:30", screen.workoutRemainingText);
        assertEquals(250, screen.workoutPermilleDone);
        assertFalse(screen.finished);
    }

    @Test
    public void aFinishedWorkoutShowsItsSummary() {
        PlayerScreen screen = PlayerScreen.of(new SessionState(Phase.FINISHED, 5, 2, 3, null,
                0, false, false, false, false, 0, 120_000,
                new PlayerWorkoutSummary(95, 2, 3)));

        assertTrue(screen.finished);
        assertEquals("01:35", screen.timerText);
        assertEquals("Gut gemacht 💪", screen.title);
        assertEquals("2 von 3 erledigt, 1 übersprungen", screen.description);
        assertTrue(screen.descriptionCentered);
        assertNull(screen.exercise);
        assertNull(screen.mediaLabel);
        assertFalse(screen.timerTappable);
        assertFalse(screen.previousEnabled);
        assertFalse(screen.nextEnabled);
    }

    @Test
    public void exerciseDescriptionsReadFromTheStart() {
        assertFalse(PlayerScreen.of(state(Phase.ACTIVE, 0, 5000, true)).descriptionCentered);
    }

    @Test
    public void theSceneIsPassedOn() {
        assertEquals(4, PlayerScreen.of(new SessionState(Phase.ACTIVE, 4, 0, 3, PUSH_UP,
                5000, true, false, true, true, 5000, 5000, null)).scene);
    }

    private static SessionState state(Phase phase, int position, long millisLeft,
                                      boolean running) {
        return new SessionState(phase, 1, position, 3, PUSH_UP, millisLeft, running,
                false, false, true, 120_000, 120_000, null);
    }

    private static WorkoutExercise workoutExercise(String title, String description) {
        Exercise exercise = new Exercise();
        exercise.id = 1;
        exercise.title = title;
        exercise.description = description;
        return new WorkoutExercise(10, exercise, 60);
    }
}
