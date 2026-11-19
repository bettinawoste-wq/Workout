package com.wob.workout.player;

import com.wob.workout.data.WorkoutExercise;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The rules of one workout session, without Android: the phases (a countdown before the
 * first exercise, the exercises, between two of them a rest or else a transition unless it
 * lasts 0 seconds, finished), stepping and pausing, when it beeps (the last 3 seconds before
 * every exercise), how much of the whole workout is left, which positions are done, and the
 * timing of its exercise sessions and rests. Positions count exercises only. Runs on
 * {@link SessionTime} and writes to
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

    /** The workout's exercises only, without its rests. */
    private List<WorkoutExercise> exercises;
    /** Per exercise, how long the rest after it lasts; 0 where none runs. */
    private List<Long> restMillisAfter;
    private long transitionMillis;
    private long totalMillis;
    private int index;
    private SessionState.Phase phase;
    private int scene;
    private long lastBeepSecond = -1;
    private boolean closed;

    /** Positions whose time ran out at least once, as opposed to being stepped away from. */
    private final Set<Integer> donePositions = new HashSet<>();

    // The exercise session or rest under way, if any: their own time, pauses left out.
    private boolean timing;
    private long timingStartedAt;
    private long timingPausedMillis;
    private long pauseStartedAt = -1;
    private int activeSeconds;
    /** The rests' time so far, pauses left out. */
    private long restMillis;
    /** Whether a rest has run, if only for a moment: without one there is no rest time. */
    private boolean rested;

    public WorkoutSessionEngine(SessionTime time, SessionHistory history, Listener listener) {
        this.time = time;
        this.history = history;
        this.listener = listener;
    }

    /** Starts the workout session with the countdown to the first exercise. A workout
     *  without exercises (rests don't count) has nothing to run: it is not started, and the
     *  listener hears {@link Listener#onNothingToPlay()} instead.
     *  @param places the workout's exercises and rests, in order; rests that would be
     *               skipped don't run */
    public void start(long workoutId, String workoutName, List<WorkoutExercise> places,
                      int transitionSeconds) {
        if (closed || this.exercises != null) return;
        List<WorkoutExercise> onlyExercises = new ArrayList<>();
        List<Long> rests = new ArrayList<>();
        for (int i = 0; i < places.size(); i++) {
            WorkoutExercise place = places.get(i);
            if (!place.isRest()) {
                onlyExercises.add(place);
                rests.add(0L);
            } else if (!WorkoutExercise.isSkippedRest(places, i)) {
                // Runs after the exercise before it, the last one added.
                rests.set(rests.size() - 1, place.durationSeconds * 1000L);
            }
        }
        if (onlyExercises.isEmpty()) {
            listener.onNothingToPlay();
            return;
        }
        this.exercises = onlyExercises;
        this.restMillisAfter = rests;
        this.transitionMillis = transitionSeconds * 1000L;
        this.totalMillis = exerciseMillisFrom(0);
        history.startSession(workoutId, workoutName);
        index = 0;
        enter(SessionState.Phase.READY, COUNTDOWN_MILLIS);
    }

    /** Pauses or resumes an exercise or a rest, and also the countdown into one. Only an
     *  exercise's or rest's own time counts in the history, so a paused countdown leaves it
     *  untouched. */
    public void togglePause() {
        if (!canPause()) return;
        if (time.isRunning()) {
            time.pause();
            if (timing) pauseStartedAt = time.now();
        } else {
            if (timing) flushPause(time.now());
            time.resume();
        }
        publish(time.millisLeft());
    }

    /** Steps to the next exercise, passing over a rest; during a rest, ends it. */
    public void next() {
        if (isResting()) {
            closeOutRest();
            beginExercise(false);
            return;
        }
        if (!isActive() || index >= exercises.size() - 1) return;
        closeOutExercise();
        index++;
        beginExercise(false);
    }

    /** Steps to the exercise before, passing over a rest; during a rest, that is the
     *  exercise the rest followed. */
    public void previous() {
        if (isResting()) {
            closeOutRest();
            index--;
            beginExercise(false);
            return;
        }
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
        if (phase == SessionState.Phase.REST) {
            closeOutRest();
        } else {
            closeOutExercise();
        }
        history.finishSession(false, restSeconds());
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
            case REST:
                closeOutRest();
                beginExercise(true);
                break;
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
                    long restMillis = restMillisAfter.get(index);
                    index++;
                    if (restMillis > 0) {
                        // The rest takes the transition's place.
                        rested = true;
                        startTiming();
                        enter(SessionState.Phase.REST, restMillis);
                    } else if (transitionMillis > 0) {
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

    private boolean isResting() {
        return exercises != null && !closed && phase == SessionState.Phase.REST;
    }

    /** Adds the rest under way to the rest time. */
    private void closeOutRest() {
        if (!timing) return;
        timing = false;
        restMillis += timedMillis(time.now());
    }

    /** The rest time in seconds, 0 when every rest was ended at once; null when no rest
     *  ran. */
    private Integer restSeconds() {
        return rested ? (int) (restMillis / 1000) : null;
    }

    /** An exercise session or a rest starts now. */
    private void startTiming() {
        timing = true;
        timingStartedAt = time.now();
        timingPausedMillis = 0;
        pauseStartedAt = -1;
    }

    /** The time of the exercise session or rest under way, pauses left out. */
    private long timedMillis(long now) {
        return Math.max(0, now - timingStartedAt - flushPause(now));
    }

    private WorkoutExercise current() {
        return exercises.get(index);
    }

    /** @param afterCountdown whether a countdown led into it, rather than a step */
    private void beginExercise(boolean afterCountdown) {
        startTiming();
        enter(SessionState.Phase.ACTIVE, current().durationSeconds * 1000L);
        if (afterCountdown) listener.onSound(PlayerSound.START);
    }

    private void enter(SessionState.Phase phase, long millis) {
        this.phase = phase;
        scene++;
        // A transition or rest goes on with the beeps the exercise before it began.
        if (phase != SessionState.Phase.TRANSITION && phase != SessionState.Phase.REST) {
            lastBeepSecond = -1;
        }
        time.start(millis, this);
        publish(millis);
        beepIfDue(millis);
    }

    private void finish() {
        phase = SessionState.Phase.FINISHED;
        scene++;
        history.finishSession(true, restSeconds());
        listener.onState(new SessionState(phase, scene, index, exercises.size(), null, 0, false,
                false, false, false, 0, totalMillis,
                new PlayerWorkoutSummary(activeSeconds, donePositions.size(), exercises.size(),
                        restSeconds())));
        listener.onSound(PlayerSound.SUCCESS);
    }

    /** Records the exercise session under way, if there is one. */
    private void closeOutExercise() {
        if (!timing) return;
        timing = false;
        long now = time.now();
        int actualSeconds = (int) (timedMillis(now) / 1000);
        activeSeconds += actualSeconds;
        history.recordExercise(index, current(), actualSeconds, timingStartedAt, now);
    }

    /** Folds a pause still going on into the paused time and returns that. */
    private long flushPause(long now) {
        if (pauseStartedAt >= 0) {
            timingPausedMillis += now - pauseStartedAt;
            pauseStartedAt = -1;
        }
        return timingPausedMillis;
    }

    /** Reports where the phase under way stands. */
    private void publish(long millisLeft) {
        boolean active = phase == SessionState.Phase.ACTIVE;
        // A rest stands between two exercises, so both ways lead somewhere.
        boolean resting = phase == SessionState.Phase.REST;
        listener.onState(new SessionState(phase, scene, index, exercises.size(), current(),
                millisLeft, time.isRunning(), resting || active && index > 0,
                resting || active && index < exercises.size() - 1, true,
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
            case REST:
                untilNextExercise = millisLeft;
                break;
            case ACTIVE:
                if (index >= exercises.size() - 1) return;
                untilNextExercise = millisLeft + gapAfter(index);
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
            case REST:
                // index is already the exercise this transition or rest leads to.
                return exerciseMillisFrom(index);
            default:
                return 0;
        }
    }

    /** How long it is from the end of the exercise at {@code position} to the start of the
     *  next one: the rest after it, or else the transition. */
    private long gapAfter(int position) {
        long restMillis = restMillisAfter.get(position);
        return restMillis > 0 ? restMillis : transitionMillis;
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
