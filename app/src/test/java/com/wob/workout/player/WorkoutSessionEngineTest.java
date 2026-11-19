package com.wob.workout.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.wob.workout.data.WorkoutExercise;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.player.SessionState.Phase;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The rules of a workout session, on fake time: two exercises of 60 and 30 seconds, a
 * 3-second countdown before the first and a 3-second transition between them.
 */
public class WorkoutSessionEngineTest {

    private static final long WORKOUT_ID = 7;

    private final FakeSessionTime time = new FakeSessionTime();
    private RecordedHistory history = new RecordedHistory();
    private final List<SessionState> states = new ArrayList<>();
    private final List<PlayerSound> sounds = new ArrayList<>();
    /** When each beep sounded, on {@link #time}'s clock. */
    private final List<Long> beepTimes = new ArrayList<>();
    private int nothingToPlay;
    private final WorkoutSessionEngine.Listener listener = new WorkoutSessionEngine.Listener() {
        @Override
        public void onState(SessionState state) {
            states.add(state);
        }

        @Override
        public void onSound(PlayerSound sound) {
            sounds.add(sound);
            if (sound == PlayerSound.BEEP) beepTimes.add(time.now());
        }

        @Override
        public void onNothingToPlay() {
            nothingToPlay++;
        }
    };
    private WorkoutSessionEngine engine;

    @Before
    public void setUp() {
        engine = new WorkoutSessionEngine(time, history, listener);
        engine.start(WORKOUT_ID, "Push Day",
                Arrays.asList(workoutExercise(1, "Push-up", 60), workoutExercise(2, "Squat", 30)),
                3);
    }

    // Phases.

    @Test
    public void startsWithTheCountdownToTheFirstExercise() {
        SessionState state = state();

        assertEquals(Phase.READY, state.phase);
        assertEquals(0, state.position);
        assertEquals(2, state.total);
        assertEquals("Push-up", state.workoutExercise.exercise.title);
        assertEquals(3000, state().millisLeft);
        assertTrue(state().running);
    }

    @Test
    public void theCountdownLeadsIntoTheFirstExercise() {
        time.advance(3000);

        assertEquals(Phase.ACTIVE, state().phase);
        assertEquals(0, state().position);
        assertEquals(60_000, state().millisLeft);
    }

    @Test
    public void anExerciseThatIsNotTheLastLeadsIntoTheTransition() {
        time.advance(3000 + 60_000);

        assertEquals(Phase.TRANSITION, state().phase);
        assertEquals(1, state().position);
        assertEquals("Squat", state().workoutExercise.exercise.title);
        assertEquals(3000, state().millisLeft);
    }

    @Test
    public void theLastExerciseFinishesWithoutAnotherCountdown() {
        time.advance(3000 + 60_000 + 3000 + 30_000);

        assertEquals(Phase.FINISHED, state().phase);
        assertNull(state().workoutExercise);
        assertFalse(time.isRunning());
    }

    @Test
    public void theTransitionLastsAsLongAsTheWorkoutSays() {
        startOther(9, 60, 30);
        time.advance(3000 + 60_000);

        assertEquals(Phase.TRANSITION, state().phase);
        assertEquals(9000, state().millisLeft);
        time.advance(8999);
        assertEquals(Phase.TRANSITION, state().phase);
        time.advance(1);
        assertEquals(Phase.ACTIVE, state().phase);
    }

    @Test
    public void anEmptyWorkoutDoesNotStart() {
        states.clear();
        WorkoutSessionEngine empty = new WorkoutSessionEngine(time, history, listener);

        empty.start(WORKOUT_ID, "Nothing", new ArrayList<>(), 3);
        empty.close();

        assertTrue(states.isEmpty());
        assertEquals(1, history.sessionsStarted);
        assertEquals(1, nothingToPlay);
    }

    // Stepping.

    @Test
    public void nextAndPreviousStepBetweenExercisesWithoutACountdown() {
        time.advance(3000);

        engine.next();
        assertEquals(Phase.ACTIVE, state().phase);
        assertEquals(1, state().position);
        assertEquals(30_000, state().millisLeft);

        engine.previous();
        assertEquals(Phase.ACTIVE, state().phase);
        assertEquals(0, state().position);
        assertEquals(60_000, state().millisLeft);
    }

    @Test
    public void steppingPastEitherEndDoesNothing() {
        time.advance(3000);
        int before = state().scene;

        engine.previous();
        engine.next();
        engine.next();

        assertEquals(before + 1, state().scene);
        assertEquals(1, state().position);
    }

    @Test
    public void noSteppingDuringTheCountdownOrTheTransition() {
        engine.next();
        assertEquals(Phase.READY, state().phase);

        time.advance(3000 + 60_000);
        engine.previous();
        engine.next();
        assertEquals(Phase.TRANSITION, state().phase);
    }

    // Pausing.

    @Test
    public void pausingAnExerciseHoldsItsTimeUntilResumed() {
        time.advance(3000 + 10_000);

        engine.togglePause();
        assertFalse(state().running);
        assertEquals(50_000, state().millisLeft);
        int statesWhilePaused = states.size();
        time.advance(120_000);
        assertEquals(statesWhilePaused, states.size());
        assertEquals(Phase.ACTIVE, state().phase);

        engine.togglePause();
        assertTrue(state().running);
        assertEquals(50_000, state().millisLeft);
        time.advance(50_000);
        assertEquals(Phase.TRANSITION, state().phase);
    }

    @Test
    public void pausingTheTransitionHoldsItsCountdown() {
        time.advance(3000 + 60_000 + 1000);

        engine.togglePause();
        time.advance(10_000);
        assertEquals(Phase.TRANSITION, state().phase);
        assertEquals(2000, state().millisLeft);

        engine.togglePause();
        time.advance(2000);
        assertEquals(Phase.ACTIVE, state().phase);
    }

    @Test
    public void aFinishedWorkoutCannotPause() {
        time.advance(3000 + 60_000 + 3000 + 30_000);
        int before = states.size();

        engine.togglePause();

        assertEquals(before, states.size());
        assertEquals(Phase.FINISHED, state().phase);
    }

    // Sounds.

    @Test
    public void theCountdownBeepsOnceForEachOfItsThreeSeconds() {
        time.advance(2999);

        assertEquals(3, beeps());
    }

    @Test
    public void theTransitionBeepsOnceForEachOfItsThreeSeconds() {
        time.advance(3000 + 59_999);
        int beforeTransition = beeps();

        time.advance(1 + 2999);

        assertEquals(3, beeps() - beforeTransition);
    }

    @Test
    public void aTransitionOfZeroGoesStraightToTheNextExercise() {
        startOther(0, 10, 10);
        time.advance(3000 + 10_000);

        assertEquals(Phase.ACTIVE, state().phase);
        assertEquals(1, state().position);
        assertEquals(10_000, state().millisLeft);
        assertEquals(2, count(PlayerSound.START));
        for (SessionState state : states) assertTrue(state.phase != Phase.TRANSITION);
    }

    @Test
    public void theLastThreeSecondsBeforeEveryExerciseBeepWhateverTheTransition() {
        for (int transitionSeconds : new int[] {0, 1, 2, 3, 5}) {
            assertEquals("transition " + transitionSeconds,
                    Arrays.asList(-3000L, -2000L, -1000L),
                    beepsBeforeTheSecondExercise(transitionSeconds));
        }
    }

    @Test
    public void theCountdownBeforeTheFirstExerciseStaysAsItWas() {
        startOther(0, 10, 10);
        long start = time.now();

        time.advance(3000);

        assertEquals(Arrays.asList(start, start + 1000, start + 2000), beepTimes);
    }

    @Test
    public void anExerciseShorterThanTheBeepsBeepsForWhatItHas() {
        startOther(0, 10, 2, 10);
        long secondStarts = time.now() + 3000 + 10_000;
        time.advance(3000 + 9_999);
        int before = beepTimes.size();

        time.advance(1 + 2000);

        assertEquals(Arrays.asList(secondStarts, secondStarts + 1000),
                beepTimes.subList(before, beepTimes.size()));
    }

    @Test
    public void theLastExercisesOwnLastSecondsStayQuiet() {
        time.advance(3000 + 60_000 + 3000);
        int before = beeps();

        time.advance(30_000);

        assertEquals(before, beeps());
    }

    @Test
    public void theStartSoundBelongsToAnExerciseACountdownLedInto() {
        time.advance(3000);
        assertEquals(1, count(PlayerSound.START));

        engine.next();
        engine.previous();
        assertEquals(1, count(PlayerSound.START));

        time.advance(60_000);
        assertEquals(1, count(PlayerSound.START)); // the transition
        time.advance(3000);
        assertEquals(2, count(PlayerSound.START));
    }

    @Test
    public void onlyTheCompletedWorkoutHasTheSuccessSound() {
        time.advance(3000 + 60_000 + 3000 + 29_999);
        assertEquals(0, count(PlayerSound.SUCCESS));

        time.advance(1);
        assertEquals(1, count(PlayerSound.SUCCESS));
        assertEquals(PlayerSound.SUCCESS, sounds.get(sounds.size() - 1));

        engine.close();
        assertEquals(1, count(PlayerSound.SUCCESS));
    }

    @Test
    public void anAbortedWorkoutHasNoSuccessSound() {
        time.advance(3000 + 10_000);

        engine.close();

        assertEquals(0, count(PlayerSound.SUCCESS));
    }

    @Test
    public void steppingIsOnlyOfferedWhereItWouldDoSomething() {
        assertFalse(state().canStepBack);
        assertFalse(state().canStepForward);
        assertTrue(state().canPause);

        time.advance(3000);
        assertFalse(state().canStepBack);
        assertTrue(state().canStepForward);

        engine.next();
        assertTrue(state().canStepBack);
        assertFalse(state().canStepForward);

        time.advance(30_000);
        assertFalse(state().canStepBack);
        assertFalse(state().canStepForward);
        assertFalse(state().canPause);
    }

    @Test
    public void theSceneChangesWithThePhaseButNotWithTheTime() {
        int countdown = state().scene;
        time.advance(2000);
        assertEquals(countdown, state().scene);

        time.advance(1000);
        int exercise = state().scene;
        assertTrue(exercise != countdown);
        engine.togglePause();
        assertEquals(exercise, state().scene);
    }

    // Remaining time of the whole workout.

    @Test
    public void theCountdownDoesNotCountTowardsTheWorkoutsTime() {
        assertEquals(90_000, state().workoutTotalMillis);
        assertEquals(90_000, state().workoutRemainingMillis);
        time.advance(2000);
        assertEquals(90_000, state().workoutRemainingMillis);
    }

    @Test
    public void duringAnExerciseItsRestAndEverythingAfterRemains() {
        time.advance(3000 + 20_000);

        assertEquals(40_000 + 30_000, state().workoutRemainingMillis);
    }

    @Test
    public void theTransitionDoesNotCountTowardsTheWorkoutsTime() {
        time.advance(3000 + 60_000);
        assertEquals(30_000, state().workoutRemainingMillis);

        time.advance(2000);
        assertEquals(30_000, state().workoutRemainingMillis);
    }

    @Test
    public void steppingAheadJumpsTheWorkoutsTime() {
        time.advance(3000 + 10_000);

        engine.next();

        assertEquals(30_000, state().workoutRemainingMillis);
    }

    @Test
    public void onTheLastExerciseOnlyItsRestRemains() {
        time.advance(3000 + 60_000 + 3000 + 29_000);
        assertEquals(1000, state().workoutRemainingMillis);
    }

    // History.

    @Test
    public void aCompletedWorkoutIsRecordedWithEachExercise() {
        time.advance(3000 + 60_000 + 3000 + 30_000);

        assertEquals(1, history.sessionsStarted);
        assertEquals(WORKOUT_ID, history.workoutId);
        assertEquals("Push Day", history.workoutName);
        assertEquals(Arrays.asList("0 Push-up 60/60", "1 Squat 30/30"), history.exercises);
        assertEquals(Boolean.TRUE, history.completed);
        assertEquals(1, history.finishes);
    }

    @Test
    public void anExerciseSessionStartsWithTheExerciseNotWithTheCountdown() {
        long countdownStart = time.now();
        time.advance(3000 + 60_000);

        assertEquals(countdownStart + 3000, history.firstStartedAt);
        assertEquals(countdownStart + 63_000, history.firstFinishedAt);
    }

    @Test
    public void pausedTimeIsLeftOutOfTheRecordedExercise() {
        time.advance(3000 + 10_000);
        engine.togglePause();
        time.advance(25_000);
        engine.togglePause();
        time.advance(5000);

        engine.next();

        assertEquals(Arrays.asList("0 Push-up 15/60"), history.exercises);
    }

    @Test
    public void anExerciseLeftWhilePausedLeavesOutThePauseSoFar() {
        time.advance(3000 + 10_000);
        engine.togglePause();
        time.advance(25_000);

        engine.next();

        assertEquals(Arrays.asList("0 Push-up 10/60"), history.exercises);
    }

    @Test
    public void aPausedCountdownIsNotPartOfAnyExercise() {
        engine.togglePause();
        time.advance(20_000);
        engine.togglePause();
        time.advance(3000 + 60_000);

        assertEquals(Arrays.asList("0 Push-up 60/60"), history.exercises);
    }

    @Test
    public void closingDuringAnExerciseRecordsItAndAbortsTheSession() {
        time.advance(3000 + 60_000 + 3000 + 12_000);

        engine.close();

        assertEquals(Arrays.asList("0 Push-up 60/60", "1 Squat 12/30"), history.exercises);
        assertEquals(Boolean.FALSE, history.completed);
        assertFalse(time.isRunning());
    }

    @Test
    public void closingDuringTheTransitionRecordsNoFurtherExercise() {
        time.advance(3000 + 60_000 + 1000);

        engine.close();

        assertEquals(Arrays.asList("0 Push-up 60/60"), history.exercises);
        assertEquals(Boolean.FALSE, history.completed);
    }

    @Test
    public void closingAFinishedWorkoutChangesNothing() {
        time.advance(3000 + 60_000 + 3000 + 30_000);

        engine.close();
        engine.close();

        assertEquals(2, history.exercises.size());
        assertEquals(Boolean.TRUE, history.completed);
        assertEquals(1, history.finishes);
    }

    @Test
    public void closingTwiceFinishesTheSessionOnce() {
        time.advance(3000 + 5000);

        engine.close();
        engine.close();

        assertEquals(1, history.finishes);
        assertEquals(1, history.exercises.size());
    }

    // Summary.

    @Test
    public void theSummaryAddsUpTheRecordedExercises() {
        time.advance(3000 + 60_000 + 3000 + 30_000);

        PlayerWorkoutSummary summary = state().summary;
        assertEquals(90, summary.activeSeconds);
        assertEquals(2, summary.doneExercises);
        assertEquals(0, summary.skippedExercises());
    }

    @Test
    public void anExerciseSteppedAwayFromCountsAsSkipped() {
        time.advance(3000);
        engine.next();
        time.advance(30_000);

        assertEquals(Phase.FINISHED, state().phase);
        assertEquals(1, state().summary.doneExercises);
        assertEquals(1, state().summary.skippedExercises());
    }

    @Test
    public void aSkippedExerciseCountsAsDoneOnceItsTimeRanOutOnReturn() {
        time.advance(3000);
        engine.next();
        engine.previous();
        time.advance(60_000 + 3000 + 30_000);

        assertEquals(2, state().summary.doneExercises);
        assertEquals(0, state().summary.skippedExercises());
    }

    // Rests.

    @Test
    public void aRestBetweenTwoExercisesRunsAsItsOwnPhaseInsteadOfTheTransition() {
        startWithRest();
        time.advance(3000 + 60_000);

        assertEquals(Phase.REST, state().phase);
        assertEquals(20_000, state().millisLeft);
        assertEquals("the next exercise is previewed", "Squat",
                state().workoutExercise.exercise.title);
        assertEquals(1, state().position);
        assertEquals(2, state().total);

        time.advance(20_000);
        assertEquals(Phase.ACTIVE, state().phase);
        assertEquals("Squat", state().workoutExercise.exercise.title);
        for (SessionState state : states) assertTrue(state.phase != Phase.TRANSITION);
    }

    @Test
    public void restsAtEitherEndOrRightAfterAnotherRestDoNotRun() {
        startPlaces(0, rest(40), workoutExercise(1, "Push-up", 10), rest(5), rest(50),
                workoutExercise(2, "Squat", 10), rest(60));

        assertEquals(Phase.READY, state().phase);
        assertEquals(2, state().total);
        time.advance(3000 + 10_000);
        assertEquals(Phase.REST, state().phase);
        assertEquals("only the first of the two rests runs", 5000, state().millisLeft);
        time.advance(5000);
        assertEquals(Phase.ACTIVE, state().phase);
        time.advance(10_000);
        assertEquals(Phase.FINISHED, state().phase);
    }

    @Test
    public void nextDuringARestEndsItAndStartsTheNextExercise() {
        startWithRest();
        time.advance(3000 + 60_000 + 5000);
        assertTrue(state().canStepForward);

        engine.next();

        assertEquals(Phase.ACTIVE, state().phase);
        assertEquals("Squat", state().workoutExercise.exercise.title);
        assertEquals(30_000, state().millisLeft);
    }

    @Test
    public void previousDuringARestGoesToTheExerciseBeforeIt() {
        startWithRest();
        time.advance(3000 + 60_000 + 5000);
        assertTrue(state().canStepBack);

        engine.previous();

        assertEquals(Phase.ACTIVE, state().phase);
        assertEquals("Push-up", state().workoutExercise.exercise.title);
        assertEquals(60_000, state().millisLeft);
    }

    @Test
    public void steppingBetweenExercisesPassesOverRests() {
        startWithRest();
        time.advance(3000);

        engine.next();
        assertEquals(Phase.ACTIVE, state().phase);
        assertEquals("Squat", state().workoutExercise.exercise.title);

        engine.previous();
        assertEquals(Phase.ACTIVE, state().phase);
        assertEquals("Push-up", state().workoutExercise.exercise.title);
    }

    @Test
    public void pausingARestHoldsItUntilResumed() {
        startWithRest();
        time.advance(3000 + 60_000 + 5000);

        engine.togglePause();
        assertFalse(state().running);
        time.advance(60_000);
        assertEquals(Phase.REST, state().phase);
        assertEquals(15_000, state().millisLeft);

        engine.togglePause();
        assertTrue(state().running);
        time.advance(15_000);
        assertEquals(Phase.ACTIVE, state().phase);
    }

    @Test
    public void theLastThreeSecondsOfARestBeepAndNoTransitionFollowsIt() {
        startWithRest();
        long squatStarts = time.now() + 3000 + 60_000 + 20_000;
        time.advance(3000 + 60_000);
        int before = beepTimes.size();

        time.advance(20_000);

        assertEquals(Arrays.asList(squatStarts - 3000, squatStarts - 2000, squatStarts - 1000),
                beepTimes.subList(before, beepTimes.size()));
        assertEquals(Phase.ACTIVE, state().phase);
    }

    @Test
    public void aRestShorterThanTheBeepsReachesBackIntoTheExerciseBeforeIt() {
        startPlaces(5, workoutExercise(1, "Push-up", 10), rest(1),
                workoutExercise(2, "Squat", 10));
        long squatStarts = time.now() + 3000 + 10_000 + 1000;
        time.advance(3000);
        int afterCountdown = beepTimes.size();

        time.advance(10_000 + 1000);

        assertEquals(Phase.ACTIVE, state().phase);
        assertEquals(Arrays.asList(squatStarts - 3000, squatStarts - 2000, squatStarts - 1000),
                beepTimes.subList(afterCountdown, beepTimes.size()));
    }

    @Test
    public void theRemainingTimeCountsExercisesOnly() {
        startWithRest();
        assertEquals(90_000, state().workoutTotalMillis);

        time.advance(3000 + 60_000 + 5000);

        assertEquals(30_000, state().workoutRemainingMillis);
    }

    @Test
    public void theActualRestTimeLeavesOutPausesAndIsReportedOnItsOwn() {
        startWithRest();
        time.advance(3000 + 60_000 + 5000);
        engine.togglePause();
        time.advance(40_000);
        engine.togglePause();
        time.advance(15_000 + 30_000);

        assertEquals(Phase.FINISHED, state().phase);
        assertEquals(20, state().summary.restSeconds);
        assertEquals(90, state().summary.activeSeconds);
        assertEquals(Integer.valueOf(20), history.restSeconds);
        assertEquals("rests are not exercise sessions",
                Arrays.asList("0 Push-up 60/60", "1 Squat 30/30"), history.exercises);
    }

    @Test
    public void aRestEndedEarlyCountsOnlyAsLongAsItRanAndSkipsNoExercise() {
        startWithRest();
        time.advance(3000 + 60_000 + 7000);
        engine.next();
        time.advance(30_000);

        assertEquals(7, state().summary.restSeconds);
        assertEquals(0, state().summary.skippedExercises());
        assertEquals(2, state().summary.totalExercises);
    }

    @Test
    public void theTransitionIsNoRestTime() {
        time.advance(3000 + 60_000 + 3000 + 30_000);

        assertEquals(0, state().summary.restSeconds);
        assertEquals(Integer.valueOf(0), history.restSeconds);
    }

    @Test
    public void anAbortedWorkoutReportsTheRestTimeSoFar() {
        startWithRest();
        time.advance(3000 + 60_000 + 12_000);

        engine.close();

        assertEquals(Boolean.FALSE, history.completed);
        assertEquals(Integer.valueOf(12), history.restSeconds);
    }

    @Test
    public void aWorkoutOfRestsOnlyIsEmpty() {
        startPlaces(3, rest(30), rest(30));

        assertTrue(states.isEmpty());
        assertEquals(1, nothingToPlay);
    }

    private void startWithRest() {
        startPlaces(3, workoutExercise(1, "Push-up", 60), rest(20),
                workoutExercise(2, "Squat", 30));
    }

    private void startPlaces(int transitionSeconds, WorkoutExercise... places) {
        engine.close();
        states.clear();
        sounds.clear();
        beepTimes.clear();
        history = new RecordedHistory();
        engine = new WorkoutSessionEngine(time, history, listener);
        engine.start(WORKOUT_ID, "With rests", Arrays.asList(places), transitionSeconds);
    }

    private static long nextRestRowId = 500;

    private static WorkoutExercise rest(int seconds) {
        return new WorkoutExercise(nextRestRowId++, null, seconds);
    }

    private void startOther(int transitionSeconds, int... seconds) {
        engine.close();
        states.clear();
        sounds.clear();
        beepTimes.clear();
        List<WorkoutExercise> workoutExercises = new ArrayList<>();
        for (int i = 0; i < seconds.length; i++) {
            workoutExercises.add(workoutExercise(10 + i, "Übung " + i, seconds[i]));
        }
        engine = new WorkoutSessionEngine(time, new RecordedHistory(), listener);
        engine.start(WORKOUT_ID, "Other", workoutExercises, transitionSeconds);
    }

    /** Runs a workout of two 10-second exercises with this transition up to the second
     *  exercise's start and gives the beeps after the countdown, relative to that start. */
    private List<Long> beepsBeforeTheSecondExercise(int transitionSeconds) {
        startOther(transitionSeconds, 10, 10);
        long secondStarts = time.now() + 3000 + 10_000 + transitionSeconds * 1000L;
        time.advance(3000);
        int afterCountdown = beepTimes.size();

        time.advance(10_000 + transitionSeconds * 1000L);

        assertEquals(Phase.ACTIVE, state().phase);
        assertEquals(1, state().position);
        List<Long> relative = new ArrayList<>();
        for (long beepTime : beepTimes.subList(afterCountdown, beepTimes.size())) {
            relative.add(beepTime - secondStarts);
        }
        return relative;
    }

    private SessionState state() {
        return states.get(states.size() - 1);
    }

    private int beeps() {
        return count(PlayerSound.BEEP);
    }

    private int count(PlayerSound sound) {
        int count = 0;
        for (PlayerSound played : sounds) {
            if (played == sound) count++;
        }
        return count;
    }

    private static WorkoutExercise workoutExercise(long exerciseId, String title, int seconds) {
        Exercise exercise = new Exercise();
        exercise.id = exerciseId;
        exercise.title = title;
        return new WorkoutExercise(100 + exerciseId, exercise, seconds);
    }

    /** {@link SessionHistory} in memory. */
    private static class RecordedHistory implements SessionHistory {
        int sessionsStarted;
        long workoutId;
        String workoutName;
        /** "position title actual/planned" per exercise session. */
        final List<String> exercises = new ArrayList<>();
        long firstStartedAt;
        long firstFinishedAt;
        Boolean completed;
        Integer restSeconds;
        int finishes;

        @Override
        public void startSession(long workoutId, String workoutName) {
            sessionsStarted++;
            this.workoutId = workoutId;
            this.workoutName = workoutName;
        }

        @Override
        public void recordExercise(int position, WorkoutExercise workoutExercise,
                                   int actualSeconds, long startedAt, long finishedAt) {
            if (exercises.isEmpty()) {
                firstStartedAt = startedAt;
                firstFinishedAt = finishedAt;
            }
            exercises.add(position + " " + workoutExercise.exercise.title + " "
                    + actualSeconds + "/" + workoutExercise.durationSeconds);
        }

        @Override
        public void finishSession(boolean completed, int restSeconds) {
            finishes++;
            this.completed = completed;
            this.restSeconds = restSeconds;
        }
    }
}
