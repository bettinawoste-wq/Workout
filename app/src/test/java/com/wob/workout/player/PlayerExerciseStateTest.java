package com.wob.workout.player;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Which way the player can still step from a given exercise. The Fragment uses this to disable
 * the transport buttons that would do nothing (the ViewModel ignores them at either end).
 */
public class PlayerExerciseStateTest {

    private static PlayerExerciseState active(int index, int total) {
        return new PlayerExerciseState(null, index, total, PlayerExerciseState.Phase.ACTIVE);
    }

    @Test
    public void firstOfSeveral_canOnlyGoForward() {
        PlayerExerciseState state = active(0, 10);

        assertFalse(state.hasPrevious());
        assertTrue(state.hasNext());
    }

    @Test
    public void middleExercise_canGoBothWays() {
        PlayerExerciseState state = active(4, 10);

        assertTrue(state.hasPrevious());
        assertTrue(state.hasNext());
    }

    @Test
    public void lastOfSeveral_canOnlyGoBack() {
        PlayerExerciseState state = active(9, 10);

        assertTrue(state.hasPrevious());
        assertFalse(state.hasNext());
    }

    @Test
    public void singleExerciseWorkout_canGoNowhere() {
        PlayerExerciseState state = active(0, 1);

        assertFalse(state.hasPrevious());
        assertFalse(state.hasNext());
    }
}
