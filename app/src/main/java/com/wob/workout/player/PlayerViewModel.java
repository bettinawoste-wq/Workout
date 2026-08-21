package com.wob.workout.player;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.wob.workout.AppContainer;
import com.wob.workout.data.LiveDataExecutor;
import com.wob.workout.data.entity.Workout;

import java.util.List;

/**
 * Owns the player's state machine (README/ACTIVE/TRANSITION/FINISHED), the single
 * {@link PlayerTimer}, and the {@link PlayerHistoryTracker}. Survives configuration
 * changes, which fixes a duplicate-session bug the old Fragment-owned version had on
 * rotation (see wayfinder ticket #4). Data loading (exercises + workout name) happens
 * once in the constructor via {@link LiveDataExecutor}; the state machine only starts
 * once that load completes.
 */
public class PlayerViewModel extends ViewModel implements PlayerTimer.Listener {

    private static final long TRANSITION_MILLIS = 3000;

    private final PlayerHistoryTracker historyTracker;
    private final PlayerTimer timer = new PlayerTimer(this);

    private final MutableLiveData<PlayerExerciseState> exerciseState = new MutableLiveData<>();
    private final MutableLiveData<PlayerTick> tick = new MutableLiveData<>();

    private List<PlayerExercise> exercises;
    private int index;
    private long lastBeepSecond = -1;
    private PlayerExerciseState.Phase currentPhase = PlayerExerciseState.Phase.READY;
    private boolean finishTransition;

    public PlayerViewModel(AppContainer container, long workoutId) {
        historyTracker = new PlayerHistoryTracker(container.workoutHistoryRepository);
        loadExercises(container, workoutId);
    }

    public LiveData<PlayerExerciseState> getExerciseState() {
        return exerciseState;
    }

    public LiveData<PlayerTick> getTick() {
        return tick;
    }

    public void togglePause() {
        if (timer.isRunning()) {
            timer.pause();
            historyTracker.onExercisePause();
        } else {
            historyTracker.onExerciseResume();
            timer.resume();
        }
        tick.setValue(new PlayerTick(timer.getMillisLeft(), timer.isRunning(), false));
    }

    public void next() {
        if (currentPhase != PlayerExerciseState.Phase.ACTIVE) return;
        if (index >= exercises.size() - 1) return;
        historyTracker.closeOutCurrentExercise(index, exercises.get(index));
        index++;
        beginExercise();
    }

    public void previous() {
        if (currentPhase != PlayerExerciseState.Phase.ACTIVE) return;
        if (index <= 0) return;
        historyTracker.closeOutCurrentExercise(index, exercises.get(index));
        index--;
        beginExercise();
    }

    @Override
    public void onTick(long millisUntilFinished) {
        tick.setValue(new PlayerTick(millisUntilFinished, true, computeShouldBeep(millisUntilFinished)));
    }

    @Override
    public void onFinish() {
        switch (currentPhase) {
            case READY:
                beginExercise();
                break;
            case ACTIVE:
                historyTracker.closeOutCurrentExercise(index, exercises.get(index));
                if (index >= exercises.size() - 1) {
                    startFinishTransition();
                } else {
                    index++;
                    startTransition();
                }
                break;
            case TRANSITION:
                if (finishTransition) {
                    finishWorkout();
                } else {
                    beginExercise();
                }
                break;
            case FINISHED:
                break;
        }
    }

    @Override
    protected void onCleared() {
        timer.cancel();
        if (currentPhase != PlayerExerciseState.Phase.FINISHED
                && exercises != null && !exercises.isEmpty() && index < exercises.size()) {
            historyTracker.closeOutCurrentExercise(index, exercises.get(index));
        }
        historyTracker.finish(false);
        super.onCleared();
    }

    private void loadExercises(AppContainer container, long workoutId) {
        LiveData<LoadResult> loaded = LiveDataExecutor.runOnExecutor(container.ioExecutor, () -> {
            List<PlayerExercise> loadedExercises = container.playerRepository.getExercises(workoutId);
            Workout workout = container.workoutRepository.getById(workoutId);
            return new LoadResult(loadedExercises, workout != null ? workout.name : null);
        });

        loaded.observeForever(new Observer<LoadResult>() {
            @Override
            public void onChanged(LoadResult result) {
                loaded.removeObserver(this);
                exercises = result.exercises;
                historyTracker.startSession(workoutId, result.workoutName);
                startInitialTransition();
            }
        });
    }

    private void startInitialTransition() {
        index = 0;
        lastBeepSecond = -1;
        currentPhase = PlayerExerciseState.Phase.READY;
        finishTransition = false;
        exerciseState.setValue(
                new PlayerExerciseState(exercises.get(index), index, exercises.size(), currentPhase));
        timer.start(TRANSITION_MILLIS);
    }

    private void startTransition() {
        currentPhase = PlayerExerciseState.Phase.TRANSITION;
        finishTransition = false;
        lastBeepSecond = -1;
        exerciseState.setValue(
                new PlayerExerciseState(exercises.get(index), index, exercises.size(), currentPhase));
        timer.start(TRANSITION_MILLIS);
    }

    private void startFinishTransition() {
        currentPhase = PlayerExerciseState.Phase.TRANSITION;
        finishTransition = true;
        lastBeepSecond = -1;
        exerciseState.setValue(
                new PlayerExerciseState(null, exercises.size(), exercises.size(), currentPhase));
        timer.start(TRANSITION_MILLIS);
    }

    private void beginExercise() {
        currentPhase = PlayerExerciseState.Phase.ACTIVE;
        finishTransition = false;
        lastBeepSecond = -1;
        PlayerExercise current = exercises.get(index);
        historyTracker.startExercise();
        exerciseState.setValue(new PlayerExerciseState(current, index, exercises.size(), currentPhase));
        timer.start(current.durationSeconds * 1000L);
    }

    private void finishWorkout() {
        currentPhase = PlayerExerciseState.Phase.FINISHED;
        historyTracker.finish(true);
        exerciseState.setValue(
                new PlayerExerciseState(null, exercises.size(), exercises.size(), currentPhase));
    }

    /** Same guard {@code lastBeepSecond} already implemented in the pre-migration Fragment,
     *  now applied uniformly to both the exercise countdown and the transition countdown
     *  (the old code only beeped near the end of an exercise after a pause/resume cycle —
     *  a latent inconsistency this migration fixes as a side effect of consolidating the
     *  timer logic into one place). */
    private boolean computeShouldBeep(long millisUntilFinished) {
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

        LoadResult(List<PlayerExercise> exercises, String workoutName) {
            this.exercises = exercises;
            this.workoutName = workoutName;
        }
    }
}
