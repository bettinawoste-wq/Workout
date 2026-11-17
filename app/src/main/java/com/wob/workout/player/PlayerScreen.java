package com.wob.workout.player;

import com.wob.workout.data.entity.Exercise;

import java.util.Locale;

/**
 * What the player screen shows for one {@link SessionState}: its texts and switches. The
 * screen only puts them in place.
 */
public final class PlayerScreen {

    /** Changes when the screen moves to another phase or exercise; see
     *  {@link SessionState#scene}. The picture is shown again only then. */
    public final int scene;
    /** Whose picture, title and description are shown; null once finished. */
    public final Exercise exercise;
    /** E.g. "1 / 10". */
    public final String progressText;
    /** E.g. "Start in 3", "00:48", or the active time once finished. */
    public final String timerText;
    /** Whether tapping the timer pauses or resumes. */
    public final boolean timerTappable;
    /** Over the picture while it only previews the coming exercise; null otherwise. */
    public final String mediaLabel;
    public final String mediaTitle;
    public final String title;
    public final String description;
    public final boolean descriptionCentered;
    public final boolean previousEnabled;
    public final boolean nextEnabled;
    /** The pause icon while the time runs, the play icon while it stands. */
    public final boolean pauseButtonShowsPause;
    public final String pauseButtonLabel;
    /** E.g. "Noch 09:12". */
    public final String workoutRemainingText;
    public final int workoutPermilleDone;
    /** The summary instead of the transport: no workout progress, no step or pause buttons,
     *  no "Workout abbrechen"; "Fertig" and "Nochmal" instead, and back just leaves. */
    public final boolean finished;

    private PlayerScreen(int scene, Exercise exercise, String progressText, String timerText,
                         boolean timerTappable, String mediaLabel, String mediaTitle,
                         String title, String description, boolean descriptionCentered,
                         boolean previousEnabled, boolean nextEnabled,
                         boolean pauseButtonShowsPause, String workoutRemainingText,
                         int workoutPermilleDone, boolean finished) {
        this.scene = scene;
        this.exercise = exercise;
        this.progressText = progressText;
        this.timerText = timerText;
        this.timerTappable = timerTappable;
        this.mediaLabel = mediaLabel;
        this.mediaTitle = mediaTitle;
        this.title = title;
        this.description = description;
        this.descriptionCentered = descriptionCentered;
        this.previousEnabled = previousEnabled;
        this.nextEnabled = nextEnabled;
        this.pauseButtonShowsPause = pauseButtonShowsPause;
        this.pauseButtonLabel = pauseButtonShowsPause ? "Pause" : "Weiter";
        this.workoutRemainingText = workoutRemainingText;
        this.workoutPermilleDone = workoutPermilleDone;
        this.finished = finished;
    }

    public static PlayerScreen of(SessionState state) {
        String progress = (state.position + 1) + " / " + state.total;
        String remaining = "Noch " + minutesSeconds(state.workoutRemainingMillis);

        if (state.phase == SessionState.Phase.FINISHED) {
            return new PlayerScreen(state.scene, null, progress,
                    minutesSeconds(state.summary.activeSeconds * 1000L), false, null, null,
                    "Gut gemacht 💪", state.summary.resultText(), true, false, false,
                    false, remaining, state.workoutPermilleDone(), true);
        }

        Exercise exercise = state.workoutExercise.exercise;
        boolean active = state.phase == SessionState.Phase.ACTIVE;
        String label = state.phase == SessionState.Phase.READY ? "ERSTE ÜBUNG"
                : state.phase == SessionState.Phase.TRANSITION ? "NÄCHSTE ÜBUNG"
                : null;
        String timer = active
                ? minutesSeconds(state.millisLeft)
                : "Start in " + ((state.millisLeft + 999) / 1000);
        return new PlayerScreen(state.scene, exercise, progress, timer, state.canPause,
                label, label != null ? exercise.title : null, exercise.title,
                exercise.description, false, state.canStepBack, state.canStepForward,
                state.running, remaining, state.workoutPermilleDone(), false);
    }

    /** E.g. "01:35"; seconds are cut, not rounded. */
    private static String minutesSeconds(long millis) {
        long totalSeconds = millis / 1000;
        return String.format(Locale.ROOT, "%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}
