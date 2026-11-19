package com.wob.workout.player;

import com.wob.workout.data.WorkoutExercise;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The rules of one workout session, without Android: the phases (a countdown before the
 * first exercise, the exercises, a transition between two of them unless it lasts 0
 * seconds, finished), stepping and pausing, when it beeps (the last 3 seconds before every
 * exercise), how much of the whole workout is left, which positions are done, and the
 * timing of its exercise sessions. Runs on {@link SessionTime} and writes to
 * {@link SessionHistory}; what goes on is reported to its {@link Listener} as a
 * {@link SessionState}, and each sound once, when it is due.
 *
 * <p>Single-threaded: every call and every time callback on one thread, the main thread in
 * the app.
 */
public class WorkoutSessionEngine implements SessionTime.Listener {

    /** The countdown before the first exercise. */
    static final long COUNTDOWN_MILLIS = 3000;

    /** The last seconds before every exercise starts beep, one beep each. */
    private static final int BEEP_SECONDS = 3;

    public interface Listener {
        /** A new phase or exercise, or the time moved, stopped or went on. */
        void onState(SessionState state);

        void onSound(PlayerSound sound);

        /** The workout has no exercises, so nothing was started. */
        void onNothingToPlay();
    }

    private final SessionTime time;
    private final SessionHistory history;
    private final Listener listener;

    private List<WorkoutExercise> exercises;
    private long transitionMillis;
    private long totalMillis;
    private int index;
    private SessionState.Phase phase;
    private int scene;
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
     *  without exercises has nothing to run: it is not started, and the listener hears
     *  {@link Listener#onNothingToPlay()} instead. */
    public void start(long workoutId, String workoutName, List<WorkoutExercise> exercises,
                      int transitionSeconds) {
        if (closed || this.exercises != null) return;
        if (exercises.isEmpty()) {
            listener.onNothingToPlay();
            return;
        }
        this.exercises = new ArrayList<>(exercises);
        this.transitionMillis = transitionSeconds * 1000L;
        this.totalMillis = exerciseMillisFrom(0);
        history.startSession(workoutId, workoutName);
        index = 0;
        enter(SessionState.Phase.READY, COUNTDOWN_MILLIS);
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
        publish(time.millisLeft());
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
        if (exercises == null || phase == SessionState.Phase.FINISHED) return;
        closeOutExercise();
        history.finishSession(false);
    }

    @Override
    public void onTick(long millisLeft) {
        if (closed) return;
        publish(millisLeft);
        beepIfDue(millisLeft);
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
                    if (transitionMillis > 0) {
                        enter(SessionState.Phase.TRANSITION, transitionMillis);
                    } else {
                        beginExercise(true);
                    }
                }
                break;
            case FINISHED:
                break;
        }
    }

    private boolean canPause() {
        return exercises != null && !closed && phase != SessionState.Phase.FINISHED;
    }

    private boolean isActive() {
        return exercises != null && !closed && phase == SessionState.Phase.ACTIVE;
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
        enter(SessionState.Phase.ACTIVE, current().durationSeconds * 1000L);
        if (afterCountdown) listener.onSound(PlayerSound.START);
    }

    private void enter(SessionState.Phase phase, long millis) {
        this.phase = phase;
        scene++;
        // A transition goes on with the beeps the exercise before it began.
        if (phase != SessionState.Phase.TRANSITION) lastBeepSecond = -1;
        time.start(millis, this);
        publish(millis);
        beepIfDue(millis);
    }

    private void finish() {
        phase = SessionState.Phase.FINISHED;
        scene++;
        history.finishSession(true);
        listener.onState(new SessionState(phase, scene, index, exercises.size(), null, 0, false,
                false, false, false, 0, totalMillis,
                new PlayerWorkoutSummary(activeSeconds, donePositions.size(), exercises.size())));
        listener.onSound(PlayerSound.SUCCESS);
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

    /** Reports where the phase under way stands. */
    private void publish(long millisLeft) {
        boolean active = phase == SessionState.Phase.ACTIVE;
        listener.onState(new SessionState(phase, scene, index, exercises.size(), current(),
                millisLeft, time.isRunning(), active && index > 0,
                active && index < exercises.size() - 1, true,
                workoutRemainingMillis(millisLeft), totalMillis, null));
    }

    /** Beeps count down into every exercise, once for each of the last three seconds before
     *  it starts, however often a second is reported: in the countdown before the first one,
     *  and before each of the others in the transition, reaching back into the exercise
     *  before when the transition is shorter. The last exercise's own last seconds stay
     *  quiet. */
    private void beepIfDue(long millisLeft) {
        long untilNextExercise;
        switch (phase) {
            case READY:
            case TRANSITION:
                untilNextExercise = millisLeft;
                break;
            case ACTIVE:
                if (index >= exercises.size() - 1) return;
                untilNextExercise = millisLeft + transitionMillis;
                break;
            default:
                return;
        }
        long second = (untilNextExercise + 999) / 1000;
        if (second <= BEEP_SECONDS && second > 0 && second != lastBeepSecond) {
            lastBeepSecond = second;
            listener.onSound(PlayerSound.BEEP);
        }
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
