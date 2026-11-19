package com.wob.workout.player;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

/**
 * The phone's vibrator for the player's sounds, one pattern each, so that they can be told
 * apart without looking: short for a countdown beep, longer when an exercise starts, and
 * three pulses, the last one long, when the workout is completed.
 */
public class PlayerVibrator {

    /** Off and on times in milliseconds, starting with a pause before the first pulse. */
    private static final long[] BEEP = {0, 80};
    private static final long[] START = {0, 400};
    private static final long[] SUCCESS = {0, 150, 120, 150, 120, 500};

    private final Vibrator vibrator;

    public PlayerVibrator(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = context.getSystemService(VibratorManager.class);
            vibrator = manager != null ? manager.getDefaultVibrator() : null;
        } else {
            vibrator = context.getSystemService(Vibrator.class);
        }
    }

    /** The waveform call before Android 8 is the only one there is, deprecated since. */
    @SuppressWarnings("deprecation")
    public void vibrate(PlayerSound sound) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] pattern = patternOf(sound);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    private static long[] patternOf(PlayerSound sound) {
        switch (sound) {
            case START:
                return START;
            case SUCCESS:
                return SUCCESS;
            case BEEP:
            default:
                return BEEP;
        }
    }
}
