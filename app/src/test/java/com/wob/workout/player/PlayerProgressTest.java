package com.wob.workout.player;

import static org.junit.Assert.assertEquals;

import com.wob.workout.player.PlayerExerciseState.Phase;

import org.junit.Test;

public class PlayerProgressTest {

    private static final long TRANSITION = 3000;
    /** 60s + 3s + 30s + 3s + 10s */
    private static final long TOTAL = 106_000;

    private final PlayerProgress progress =
            new PlayerProgress(new int[] {60, 30, 10}, TRANSITION);

    @Test
    public void totalCountsExercisesAndTheTransitionsBetweenThem() {
        assertEquals(TOTAL, progress.totalMillis());
    }

    @Test
    public void theStartCountdownDoesNotCount() {
        assertEquals(TOTAL, progress.remainingMillis(0, Phase.READY, 1500));
    }

    @Test
    public void duringAnExerciseItsRestAndEverythingAfterRemains() {
        assertEquals(20_000 + 3000 + 30_000 + 3000 + 10_000,
                progress.remainingMillis(0, Phase.ACTIVE, 20_000));
    }

    @Test
    public void duringATransitionTheNextExerciseStillFullyRemains() {
        assertEquals(2000 + 30_000 + 3000 + 10_000,
                progress.remainingMillis(1, Phase.TRANSITION, 2000));
    }

    @Test
    public void onTheLastExerciseOnlyItsRestRemains() {
        assertEquals(4000, progress.remainingMillis(2, Phase.ACTIVE, 4000));
    }

    @Test
    public void skippingAheadJumps() {
        long before = progress.remainingMillis(0, Phase.ACTIVE, 50_000);
        long after = progress.remainingMillis(1, Phase.ACTIVE, 30_000);

        assertEquals(50_000 + 3000 + 30_000 + 3000 + 10_000, before);
        assertEquals(30_000 + 3000 + 10_000, after);
    }

    @Test
    public void nothingRemainsOnceTheLastExerciseIsOver() {
        assertEquals(0, progress.remainingMillis(2, Phase.FINISHING, 2000));
        assertEquals(0, progress.remainingMillis(2, Phase.FINISHED, 0));
    }

    @Test
    public void aSingleExerciseHasNoTransition() {
        assertEquals(45_000, new PlayerProgress(new int[] {45}, TRANSITION).totalMillis());
    }
}
