package com.wob.workout.player;

import com.wob.workout.data.WorkoutExercise;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The rules of one workout session, without Android: the phases (a countdown before the
 * first exercise, the exercises, a transition between two of them, finished), stepping and
 * pausing, when it beeps, how much of the whole workout is left, which positions are done,
 * and the timing of its exercise sessions. Runs on {@link SessionTime} and writes to
 * {@link SessionHistory}; what goes on is reported to its {@link Listener}.
 *
 * <p>Single-threaded: every call and every time callback on one thread, the main thread in
 * the app.
 */
public class WorkoutSessionEngine implements SessionTime.Listener {

    /** The countdown before the first exercise. */
    static final long COUNTDOWN_MILLIS = 3000;

    /** The last seconds of a countdown or transition beep, one beep each. */
    private static final int BEEP_SECONDS = 3;

    public interface Listener {
        /** A new phase or exercise. */
        void onExerciseState(PlayerExerciseState state);

        /** The time moved, or stopped or went on. */
        void onTick(PlayerTick tick);
    }

    private final SessionTime time;
    private final SessionHistory history;
    private final Listener listener;

    private List<WorkoutExercise> exercises;
    private long transitionMillis;
    private long totalMillis;
    private int index;
    private PlayerExerciseState.Phase phase;
    private long lastBeepSecond = -1;
    private boolean closed;

    /** Positions whose time ran out at least once, as opposed to being stepped away from. */
    private final Set<Integer> donePositions = new HashSet<>();

    // The exercise session under way, if any.
    private boolean exerciseStarted;
    private long exerciseStartedAt;
    private long exercisePausedMillis;
    private long pauseStartedAt = -1;
    private int activeSeconds;

    public WorkoutSessionEngine(SessionTime time, SessionHistory history, Listener listener) {
        this.time = time;
        this.history = history;
        this.listener = listener;
    }

    /** Starts the workout session with the countdown to the first exercise. A workout
     *  without exercises has nothing to run and is not started. */
    public void start(long workoutId, String workoutName, List<WorkoutExercise> exercises,
                      int transitionSeconds) {
        if (closed || this.exercises != null || exercises.isEmpty()) return;
        this.exercises = new ArrayList<>(exercises);
        this.transitionMillis = transitionSeconds * 1000L;
        this.totalMillis = exerciseMillisFrom(0);
        history.startSession(workoutId, workoutName);
        index = 0;
        enter(new PlayerExerciseState(
                current(), index, this.exercises.size(), PlayerExerciseState.Phase.READY),
                COUNTDOWN_MILLIS);
    }

    /** Pauses or resumes an exercise, and also the countdown into one. Only an exercise's
     *  own time counts in the history, so a paused countdown leaves it untouched. */
    public void togglePause() {
        if (!canPause()) return;
        if (time.isRunning()) {
            time.pause();
            if (isActive()) pauseStartedAt = time.now();
        } else {
            if (isActive()) flushPause(time.now());
            time.resume();
        }
        emitTick(time.millisLeft(), false);
    }

    public void next() {
        if (!isActive() || index >= exercises.size() - 1) return;
        closeOutExercise();
        index++;
        beginExercise(false);
    }

    public void previous() {
        if (!isActive() || index <= 0) return;
        closeOutExercise();
        index--;
        beginExercise(false);
    }

    /** Ends the session where it is: an unfinished one is recorded as aborted, with the
     *  exercise under way. Nothing happens afterwards. */
    public void close() {
        if (closed) return;
        closed = true;
        time.cancel();
        if (exercises == null || phase == PlayerExerciseState.Phase.FINISHED) return;
        closeOutExercise();
        history.finishSession(false);
    }

    @Override
    public void onTick(long millisLeft) {
        if (closed) return;
        emitTick(millisLeft, true);
    }

    @Override
    public void onFinish() {
        if (closed) return;
        switch (phase) {
            case READY:
            case TRANSITION:
                beginExercise(true);
                break;
            case ACTIVE:
                donePositions.add(index);
                closeOutExercise();
                if (index >= exercises.size() - 1) {
                    // No transition follows the last exercise: straight to the success sound.
                    finish();
                } else {
                    index++;
                    enter(new PlayerExerciseState(current(), index, exercises.size(),
                            PlayerExerciseState.Phase.TRANSITION), transitionMillis);
                }
                break;
            case FINISHED:
                break;
        }
    }

    private boolean canPause() {
        return exercises != null && !closed && phase != PlayerExerciseState.Phase.FINISHED;
    }

    private boolean isActive() {
        return exercises != null && !closed && phase == PlayerExerciseState.Phase.ACTIVE;
    }

    private WorkoutExercise current() {
        return exercises.get(index);
    }

    /** @param afterCountdown whether a countdown led into it, rather than a step */
    private void beginExercise(boolean afterCountdown) {
        exerciseStarted = true;
        exerciseStartedAt = time.now();
        exercisePausedMillis = 0;
        pauseStartedAt = -1;
        enter(new PlayerExerciseState(current(), index, exercises.size(),
                PlayerExerciseState.Phase.ACTIVE, afterCountdown),
                current().durationSeconds * 1000L);
    }

    private void enter(PlayerExerciseState state, long millis) {
        phase = state.phase;
        lastBeepSecond = -1;
        listener.onExerciseState(state);
        time.start(millis, this);
        emitTick(millis, true);
    }

    private void finish() {
        phase = PlayerExerciseState.Phase.FINISHED;
        history.finishSession(true);
        listener.onExerciseState(PlayerExerciseState.finished(index, exercises.size(),
                new PlayerWorkoutSummary(activeSeconds, donePositions.size(), exercises.size())));
    }

    /** Records the exercise session under way, if there is one. */
    private void closeOutExercise() {
        if (!exerciseStarted) return;
        exerciseStarted = false;
        long now = time.now();
        long activeMillis = Math.max(0, now - exerciseStartedAt - flushPause(now));
        int actualSeconds = (int) (activeMillis / 1000);
        activeSeconds += actualSeconds;
        history.recordExercise(index, current(), actualSeconds, exerciseStartedAt, now);
    }

    /** Folds a pause still going on into the paused time and returns that. */
    private long flushPause(long now) {
        if (pauseStartedAt >= 0) {
            exercisePausedMillis += now - pauseStartedAt;
            pauseStartedAt = -1;
        }
        return exercisePausedMillis;
    }

    /** @param counting whether this is the time going on, which may beep; a pause or resume
     *                  only shows where it stands */
    private void emitTick(long millisLeft, boolean counting) {
        boolean beep = counting && shouldBeep(millisLeft);
        listener.onTick(new PlayerTick(millisLeft, time.isRunning(), beep, phase,
                workoutRemainingMillis(millisLeft), totalMillis));
    }

    /** Beeps count down into every exercise: in the countdown before the first one and in
     *  the transition before each of the others, once for each of the last three seconds,
     *  however often a second is reported. An exercise's own last seconds stay quiet. */
    private boolean shouldBeep(long millisLeft) {
        if (phase != PlayerExerciseState.Phase.READY
                && phase != PlayerExerciseState.Phase.TRANSITION) {
            return false;
        }
        long second = (millisLeft + 999) / 1000;
        if (second <= BEEP_SECONDS && second > 0 && second != lastBeepSecond) {
            lastBeepSecond = second;
            return true;
        }
        return false;
    }

    /** Exercise time only: during the countdown and between exercises it stands still. */
    private long workoutRemainingMillis(long millisLeft) {
        switch (phase) {
            case READY:
                return totalMillis;
            case ACTIVE:
                return millisLeft + exerciseMillisFrom(index + 1);
            case TRANSITION:
                // index is already the exercise this transition leads to.
                return exerciseMillisFrom(index);
            default:
                return 0;
        }
    }

    /** The exercise time from the exercise at {@code from} to the end. */
    private long exerciseMillisFrom(int from) {
        long millis = 0;
        for (int i = from; i < exercises.size(); i++) {
            millis += exercises.get(i).durationSeconds * 1000L;
        }
        return millis;
    }
}
