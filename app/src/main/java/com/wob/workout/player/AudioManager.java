package com.wob.workout.player;

import android.content.Context;
import android.media.SoundPool;

import com.wob.workout.R;

public class AudioManager {

    private final SoundPool soundPool;
    private final int beepSoundId;
    private final int startSoundId;
    private boolean loaded;

    public AudioManager(Context context) {

        soundPool = new SoundPool.Builder()
                .setMaxStreams(2)
                .build();

        beepSoundId = soundPool.load(context, R.raw.beep, 1);
        startSoundId = soundPool.load(context, R.raw.start, 1);

        soundPool.setOnLoadCompleteListener(
                (pool, sampleId, status) -> loaded = true);
    }

    public void playBeep() {
        if (!loaded) return;
        soundPool.play(beepSoundId, 1f, 1f, 1, 0, 1f);
    }

    public void playStart() {
        if (!loaded) return;
        soundPool.play(startSoundId, 1f, 1f, 1, 0, 1f);
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void release() {
        soundPool.release();
    }
}