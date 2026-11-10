package com.wob.workout.player;

import static org.junit.Assert.assertEquals;

import com.wob.workout.player.PlayerExerciseState.Phase;

import org.junit.Test;

public class PlayerProgressTest {

    /** 60s + 30s + 10s */
    private static final long TOTAL = 100_000;

    private final PlayerProgress progress = new PlayerProgress(new int[] {60, 30, 10});

    @Test
    public void totalCountsExerciseTimeOnly() {
        assertEquals(TOTAL, progress.totalMillis());
    }

    @Test
    public void theStartCountdownDoesNotCount() {
        assertEquals(TOTAL, progress.remainingMillis(0, Phase.READY, 1500));
    }

    @Test
    public void duringAnExerciseItsRestAndEverythingAfterRemains() {
        assertEquals(20_000 + 30_000 + 10_000, progress.remainingMillis(0, Phase.ACTIVE, 20_000));
    }

    @Test
    public void thePauseBetweenExercisesDoesNotCount() {
        long endOfFirst = progress.remainingMillis(0, Phase.ACTIVE, 0);

        assertEquals(endOfFirst, progress.remainingMillis(1, Phase.TRANSITION, 4000));
        assertEquals(endOfFirst, progress.remainingMillis(1, Phase.TRANSITION, 1000));
        assertEquals(30_000 + 10_000, endOfFirst);
    }

    @Test
    public void onTheLastExerciseOnlyItsRestRemains() {
        assertEquals(4000, progress.remainingMillis(2, Phase.ACTIVE, 4000));
    }

    @Test
    public void skippingAheadJumps() {
        assertEquals(50_000 + 30_000 + 10_000, progress.remainingMillis(0, Phase.ACTIVE, 50_000));
        assertEquals(30_000 + 10_000, progress.remainingMillis(1, Phase.ACTIVE, 30_000));
    }

    @Test
    public void nothingRemainsOnceTheLastExerciseIsOver() {
        assertEquals(0, progress.remainingMillis(2, Phase.FINISHED, 0));
    }
}
