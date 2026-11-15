package com.wob.workout.player;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** The player's sound switch (issue #71), on a preferences file of its own. */
@RunWith(AndroidJUnit4.class)
public class SoundSettingTest {

    private SharedPreferences preferences;

    @Before
    public void setUp() {
        preferences = ApplicationProvider.<Context>getApplicationContext()
                .getSharedPreferences("sound-setting-test", Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
    }

    @After
    public void tearDown() {
        preferences.edit().clear().commit();
    }

    @Test
    public void soundsAreOnByDefault() {
        assertTrue(new SoundSetting(preferences).isOn());
    }

    @Test
    public void switchingSoundsOffIsRememberedAcrossRestarts() {
        new SoundSetting(preferences).setOn(false);

        // A new instance over the same file stands in for the app starting again.
        assertFalse(new SoundSetting(preferences).isOn());
    }

    @Test
    public void switchingSoundsBackOnIsRememberedToo() {
        new SoundSetting(preferences).setOn(false);
        new SoundSetting(preferences).setOn(true);

        assertTrue(new SoundSetting(preferences).isOn());
    }
}
