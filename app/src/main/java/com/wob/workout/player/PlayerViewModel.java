package com.wob.workout.player;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.wob.workout.AppContainer;
import com.wob.workout.data.Event;
import com.wob.workout.data.LiveDataExecutor;
import com.wob.workout.data.WorkoutExercise;
import com.wob.workout.data.entity.Workout;

import java.util.List;

/**
 * Adapts a {@link WorkoutSessionEngine} to the player screen: loads the workout once, starts
 * the engine on real time and the database history, forwards the screen's taps, and hands
 * over what to show as a {@link PlayerScreen} and each due sound as a one-shot
 * {@link Event}, once to play (while sound is on) and once to vibrate (while vibration is
 * on). Survives configuration changes, so a rotation never starts a second workout
 * session (see wayfinder ticket #4) and never plays a sound again: a recreated screen gets
 * the last event back, already handled.
 */
public class PlayerViewModel extends ViewModel implements WorkoutSessionEngine.Listener {

    private final SoundSetting soundSetting;
    private final VibrationSetting vibrationSetting;
    private final WorkoutSessionEngine engine;

    private final MutableLiveData<PlayerScreen> screen = new MutableLiveData<>();
    private final MutableLiveData<Event<PlayerSound>> sound = new MutableLiveData<>();
    private final MutableLiveData<Event<PlayerSound>> vibration = new MutableLiveData<>();
    private final MutableLiveData<Boolean> soundOn = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> nothingToPlay = new MutableLiveData<>();

    private boolean cleared;

    public PlayerViewModel(AppContainer container, long workoutId) {
        soundSetting = container.soundSetting;
        vibrationSetting = container.vibrationSetting;
        soundOn.setValue(soundSetting.isOn());
        engine = new WorkoutSessionEngine(
                new AndroidSessionTime(),
                new DatabaseSessionHistory(
                        container.workoutHistoryRepository, container.ioExecutor),
                this);
        load(container, workoutId);
    }

    /** Empty until the workout has loaded. */
    public LiveData<PlayerScreen> getScreen() {
        return screen;
    }

    /** The sounds to play, only while the sound setting is on. */
    public LiveData<Event<PlayerSound>> getSound() {
        return sound;
    }

    /** The sounds to vibrate for, one pattern each, only while the vibration setting is on;
     *  independent of the sound setting. */
    public LiveData<Event<PlayerSound>> getVibration() {
        return vibration;
    }

    /** Once, when the workout turned out to have no exercises: there is no screen to show. */
    public LiveData<Event<Boolean>> getNothingToPlay() {
        return nothingToPlay;
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
    public void onState(SessionState state) {
        screen.setValue(PlayerScreen.of(state));
    }

    @Override
    public void onSound(PlayerSound due) {
        if (soundSetting.isOn()) sound.setValue(new Event<>(due));
        if (vibrationSetting.isOn()) vibration.setValue(new Event<>(due));
    }

    @Override
    public void onNothingToPlay() {
        nothingToPlay.setValue(new Event<>(true));
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
