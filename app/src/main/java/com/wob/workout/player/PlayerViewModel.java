package com.wob.workout.player;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.wob.workout.AppContainer;
import com.wob.workout.data.LiveDataExecutor;
import com.wob.workout.data.WorkoutExercise;
import com.wob.workout.data.entity.Workout;

import java.util.List;

/**
 * Adapts a {@link WorkoutSessionEngine} to the player screen: loads the workout once, starts
 * the engine on real time and the database history, forwards the screen's taps and exposes
 * what the engine reports as LiveData. Survives configuration changes, so a rotation never
 * starts a second workout session (see wayfinder ticket #4).
 *
 * <p>{@link #shouldPlayStartSound} / {@link #shouldPlayBeep} / {@link #shouldPlaySuccessSound}
 * live here rather than in the Fragment: LiveData redelivers its last value to a
 * freshly-registered observer (e.g. after rotation recreates the Fragment), so a dedup guard
 * only works if it's kept on the object that survives the rotation. They also answer false
 * while the {@link SoundSetting} is off, which keeps every player sound behind one switch.
 */
public class PlayerViewModel extends ViewModel implements WorkoutSessionEngine.Listener {

    private final SoundSetting soundSetting;
    private final WorkoutSessionEngine engine;

    private final MutableLiveData<PlayerExerciseState> exerciseState = new MutableLiveData<>();
    private final MutableLiveData<PlayerTick> tick = new MutableLiveData<>();
    private final MutableLiveData<Boolean> soundOn = new MutableLiveData<>();

    private boolean cleared;
    private PlayerExerciseState lastStartSoundState;
    private PlayerTick lastConsumedTick;
    private PlayerExerciseState lastSuccessSoundState;

    public PlayerViewModel(AppContainer container, long workoutId) {
        soundSetting = container.soundSetting;
        soundOn.setValue(soundSetting.isOn());
        engine = new WorkoutSessionEngine(
                new AndroidSessionTime(),
                new DatabaseSessionHistory(
                        container.workoutHistoryRepository, container.ioExecutor),
                this);
        load(container, workoutId);
    }

    public LiveData<PlayerExerciseState> getExerciseState() {
        return exerciseState;
    }

    public LiveData<PlayerTick> getTick() {
        return tick;
    }

    public LiveData<Boolean> isSoundOn() {
        return soundOn;
    }

    /** Turns all player sounds off, or back on; remembered for every later workout. */
    public void toggleSound() {
        boolean on = !soundSetting.isOn();
        soundSetting.setOn(on);
        soundOn.setValue(on);
    }

    /** Returns true at most once per state that has the start sound, even across a Fragment
     *  recreation that re-observes and receives LiveData's redelivered value. */
    public boolean shouldPlayStartSound(PlayerExerciseState state) {
        if (!state.startSound || state == lastStartSoundState) return false;
        lastStartSoundState = state;
        return soundSetting.isOn();
    }

    /** Same idea as {@link #shouldPlayStartSound} for the per-second beep. */
    public boolean shouldPlayBeep(PlayerTick tick) {
        if (tick == lastConsumedTick) return false;
        lastConsumedTick = tick;
        return tick.shouldBeep && soundSetting.isOn();
    }

    /** Same idea as {@link #shouldPlayStartSound}, for the sound of a completed workout. */
    public boolean shouldPlaySuccessSound(PlayerExerciseState state) {
        if (!state.successSound || state == lastSuccessSoundState) return false;
        lastSuccessSoundState = state;
        return soundSetting.isOn();
    }

    public void togglePause() {
        engine.togglePause();
    }

    public void next() {
        engine.next();
    }

    public void previous() {
        engine.previous();
    }

    @Override
    public void onExerciseState(PlayerExerciseState state) {
        exerciseState.setValue(state);
    }

    @Override
    public void onTick(PlayerTick tick) {
        this.tick.setValue(tick);
    }

    @Override
    protected void onCleared() {
        cleared = true;
        engine.close();
        super.onCleared();
    }

    private void load(AppContainer container, long workoutId) {
        LiveDataExecutor.runOnExecutorOnce(container.ioExecutor, () -> {
            Workout workout = container.workoutRepository.getById(workoutId);
            return new Loaded(
                    container.workoutRepository.getWorkoutExercises(workoutId),
                    workout != null ? workout.name : null,
                    (workout != null ? workout : new Workout()).transitionSeconds);
        }, () -> !cleared, loaded -> {
            if (loaded == null) return;
            engine.start(workoutId, loaded.workoutName, loaded.exercises,
                    loaded.transitionSeconds);
        });
    }

    private static class Loaded {
        final List<WorkoutExercise> exercises;
        final String workoutName;
        final int transitionSeconds;

        Loaded(List<WorkoutExercise> exercises, String workoutName, int transitionSeconds) {
            this.exercises = exercises;
            this.workoutName = workoutName;
            this.transitionSeconds = transitionSeconds;
        }
    }
}
