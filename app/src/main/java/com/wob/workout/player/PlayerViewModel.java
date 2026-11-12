package com.wob.workout.player;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.wob.workout.AppContainer;
import com.wob.workout.data.LiveDataExecutor;
import com.wob.workout.data.entity.Workout;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Owns the player's state machine (READY/ACTIVE/TRANSITION/FINISHED), the single
 * {@link PlayerTimer}, and the {@link PlayerHistoryTracker}. Survives configuration
 * changes, which fixes a duplicate-session bug the old Fragment-owned version had on
 * rotation (see wayfinder ticket #4). Data loading (exercises + workout name) happens
 * once in the constructor; the state machine only starts once that load completes.
 *
 * <p>{@link #shouldPlayStartSound} / {@link #shouldPlayBeep} live here rather than as
 * Fragment-side fields: LiveData redelivers its last value to a freshly-registered
 * observer (e.g. after rotation recreates the Fragment), so a dedup guard only works if
 * it's kept on the object that survives the rotation.
 */
public class PlayerViewModel extends ViewModel implements PlayerTimer.Listener {

    /** The countdown before the first exercise. */
    private static final long COUNTDOWN_MILLIS = 3000;

    private final PlayerHistoryTracker historyTracker;
    private final PlayerTimer timer = new PlayerTimer(this);

    private final MutableLiveData<PlayerExerciseState> exerciseState = new MutableLiveData<>();
    private final MutableLiveData<PlayerTick> tick = new MutableLiveData<>();

    private List<PlayerExercise> exercises;
    private int index;
    private long lastBeepSecond = -1;
    private PlayerExerciseState.Phase currentPhase = PlayerExerciseState.Phase.READY;
    private boolean cleared;

    /** The exercise state whose start sound is still to be played, if any. */
    private PlayerExerciseState startSoundState;
    private PlayerTick lastConsumedTick;
    private boolean successSoundPlayed;

    private long transitionMillis;
    private PlayerProgress progress;
    /** Positions whose time ran out at least once, as opposed to being stepped away from. */
    private final Set<Integer> donePositions = new HashSet<>();

    public PlayerViewModel(AppContainer container, long workoutId) {
        historyTracker = new PlayerHistoryTracker(
                container.workoutHistoryRepository, container.ioExecutor);
        loadExercises(container, workoutId);
    }

    public LiveData<PlayerExerciseState> getExerciseState() {
        return exerciseState;
    }

    public LiveData<PlayerTick> getTick() {
        return tick;
    }

    /** The start sound belongs to an exercise that a countdown led into, not to one the user
     *  stepped to. Returns true at most once per such {@link PlayerExerciseState}, even across a
     *  Fragment recreation that re-observes and receives LiveData's redelivered value. */
    public boolean shouldPlayStartSound(PlayerExerciseState state) {
        if (state != startSoundState) return false;
        startSoundState = null;
        return true;
    }

    /** Same idea as {@link #shouldPlayStartSound} for the per-second beep. */
    public boolean shouldPlayBeep(PlayerTick tick) {
        if (tick == lastConsumedTick) return false;
        lastConsumedTick = tick;
        return tick.shouldBeep;
    }

    /** Same idea as {@link #shouldPlayStartSound}, for the sound of a completed workout. */
    public boolean shouldPlaySuccessSound(PlayerExerciseState state) {
        if (state.phase != PlayerExerciseState.Phase.FINISHED || successSoundPlayed) return false;
        successSoundPlayed = true;
        return true;
    }

    public void togglePause() {
        if (!isActive()) return;

        if (timer.isRunning()) {
            timer.pause();
            historyTracker.onExercisePause();
        } else {
            historyTracker.onExerciseResume();
            timer.resume();
        }
        tick.setValue(newTick(timer.getMillisLeft(), timer.isRunning(), false));
    }

    public void next() {
        if (!isActive()) return;
        if (index >= exercises.size() - 1) return;
        historyTracker.closeOutCurrentExercise(index, exercises.get(index));
        index++;
        beginExercise(false);
    }

    public void previous() {
        if (!isActive()) return;
        if (index <= 0) return;
        historyTracker.closeOutCurrentExercise(index, exercises.get(index));
        index--;
        beginExercise(false);
    }

    private boolean isActive() {
        return currentPhase == PlayerExerciseState.Phase.ACTIVE;
    }

    @Override
    public void onTick(long millisUntilFinished) {
        tick.setValue(newTick(millisUntilFinished, true, computeShouldBeep(millisUntilFinished)));
    }

    private PlayerTick newTick(long millisLeft, boolean running, boolean shouldBeep) {
        return new PlayerTick(
                millisLeft,
                running,
                shouldBeep,
                currentPhase,
                progress.remainingMillis(index, currentPhase, millisLeft),
                progress.totalMillis());
    }

    @Override
    public void onFinish() {
        switch (currentPhase) {
            case READY:
            case TRANSITION:
                beginExercise(true);
                break;
            case ACTIVE:
                donePositions.add(index);
                historyTracker.closeOutCurrentExercise(index, exercises.get(index));
                if (index >= exercises.size() - 1) {
                    // The exercise's own last seconds already counted down to the end.
                    finishWorkout();
                } else {
                    index++;
                    startTransition();
                }
                break;
            case FINISHED:
                break;
        }
    }

    @Override
    protected void onCleared() {
        cleared = true;
        timer.cancel();
        if (currentPhase != PlayerExerciseState.Phase.FINISHED
                && exercises != null && !exercises.isEmpty() && index < exercises.size()) {
            historyTracker.closeOutCurrentExercise(index, exercises.get(index));
        }
        historyTracker.finish(false);
        super.onCleared();
    }

    private void loadExercises(AppContainer container, long workoutId) {
        LiveDataExecutor.runOnExecutorOnce(container.ioExecutor, () -> {
            List<PlayerExercise> loadedExercises =
                    container.playerRepository.getExercises(workoutId);
            Workout workout = container.workoutRepository.getById(workoutId);
            return new LoadResult(
                    loadedExercises,
                    workout != null ? workout.name : null,
                    (workout != null ? workout : new Workout()).transitionSeconds);
        }, () -> !cleared, result -> {
            if (result == null) return;
            exercises = result.exercises;
            transitionMillis = result.transitionSeconds * 1000L;
            progress = new PlayerProgress(plannedSeconds(exercises));
            historyTracker.startSession(workoutId, result.workoutName);
            startInitialTransition();
        });
    }

    private static int[] plannedSeconds(List<PlayerExercise> exercises) {
        int[] seconds = new int[exercises.size()];
        for (int i = 0; i < seconds.length; i++) {
            seconds[i] = exercises.get(i).durationSeconds;
        }
        return seconds;
    }

    private void startInitialTransition() {
        index = 0;
        enterPhase(PlayerExerciseState.Phase.READY, exercises.get(index), COUNTDOWN_MILLIS);
    }

    private void startTransition() {
        enterPhase(PlayerExerciseState.Phase.TRANSITION, exercises.get(index), transitionMillis);
    }

    private void beginExercise(boolean afterCountdown) {
        historyTracker.startExercise();
        PlayerExercise current = exercises.get(index);
        PlayerExerciseState state = new PlayerExerciseState(
                current, index, exercises.size(), PlayerExerciseState.Phase.ACTIVE);
        // Set before the state goes out: observers are told inside setValue().
        startSoundState = afterCountdown ? state : null;
        enterPhase(state, current.durationSeconds * 1000L);
    }

    private void enterPhase(PlayerExerciseState.Phase phase, PlayerExercise exercise, long millis) {
        enterPhase(new PlayerExerciseState(exercise, index, exercises.size(), phase), millis);
    }

    private void enterPhase(PlayerExerciseState state, long millis) {
        currentPhase = state.phase;
        lastBeepSecond = -1;
        exerciseState.setValue(state);
        timer.start(millis);
    }

    private void finishWorkout() {
        currentPhase = PlayerExerciseState.Phase.FINISHED;
        historyTracker.finish(true);
        PlayerWorkoutSummary summary = new PlayerWorkoutSummary(
                historyTracker.getActiveSeconds(), donePositions.size(), exercises.size());
        exerciseState.setValue(
                new PlayerExerciseState(null, index, exercises.size(), currentPhase, summary));
    }

    /**
     * Beeps count down into the first exercise and out of every exercise. The pause between two
     * exercises stays quiet: its end is marked by the start sound alone.
     */
    private boolean countsDownAudibly() {
        return currentPhase != PlayerExerciseState.Phase.TRANSITION;
    }

    /** One beep for each of the last three seconds, however often a second is reported. */
    private boolean computeShouldBeep(long millisUntilFinished) {
        if (!countsDownAudibly()) return false;

        long second = (millisUntilFinished + 999) / 1000;
        if (second <= 3 && second > 0 && second != lastBeepSecond) {
            lastBeepSecond = second;
            return true;
        }
        return false;
    }

    private static class LoadResult {
        final List<PlayerExercise> exercises;
        final String workoutName;
        final int transitionSeconds;

        LoadResult(List<PlayerExercise> exercises, String workoutName, int transitionSeconds) {
            this.exercises = exercises;
            this.workoutName = workoutName;
            this.transitionSeconds = transitionSeconds;
        }
    }
}
