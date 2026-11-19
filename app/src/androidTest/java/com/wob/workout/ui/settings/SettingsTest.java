package com.wob.workout.ui.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.wob.workout.AppContainer;
import com.wob.workout.TestAppContainer;
import com.wob.workout.player.DefaultTransitionSetting;
import com.wob.workout.player.VibrationSetting;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** The app-wide choices of the settings screen (issue #102), on the test container's
 *  preferences file: each one's default on a fresh install, and that it persists; the
 *  default transition (issue #103) too. */
@RunWith(AndroidJUnit4.class)
public class SettingsTest {

    private TestAppContainer testContainer;
    private AppContainer container;

    @Before
    public void setUp() {
        testContainer = new TestAppContainer();
        container = testContainer.getAppContainer();
    }

    @After
    public void tearDown() {
        testContainer.close();
    }

    @Test
    public void aFreshInstallFollowsTheSystemDesign() {
        assertEquals(Design.SYSTEM, container.designSetting.get());
    }

    @Test
    public void eachDesignChoiceIsRememberedAcrossRestarts() {
        for (Design design : new Design[] {Design.DARK, Design.LIGHT, Design.SYSTEM}) {
            container.designSetting.set(design);

            // A new instance over the same file stands in for the app starting again.
            assertEquals(design, new DesignSetting(testContainer.getSettings()).get());
        }
    }

    @Test
    public void vibrationIsOffOnAFreshInstall() {
        assertFalse(container.vibrationSetting.isOn());
    }

    @Test
    public void switchingVibrationOnAndOffIsRememberedAcrossRestarts() {
        container.vibrationSetting.setOn(true);
        assertTrue(new VibrationSetting(testContainer.getSettings()).isOn());

        container.vibrationSetting.setOn(false);
        assertFalse(new VibrationSetting(testContainer.getSettings()).isOn());
    }

    @Test
    public void theDefaultTransitionLastsThreeSecondsOnAFreshInstall() {
        assertEquals(3, container.defaultTransitionSetting.getSeconds());
    }

    @Test
    public void theDefaultTransitionIsRememberedAcrossRestartsFromZeroToThirtySeconds() {
        for (int seconds : new int[] {0, 30, 12}) {
            container.defaultTransitionSetting.setSeconds(seconds);

            assertEquals(seconds,
                    new DefaultTransitionSetting(testContainer.getSettings()).getSeconds());
        }
    }

    @Test
    public void changingTheDefaultChangesEveryWorkoutWithoutItsOwnTransition() {
        container.defaultTransitionSetting.setSeconds(8);
        assertEquals(8, container.defaultTransitionSetting.resolve(null));
        assertEquals(12, container.defaultTransitionSetting.resolve(12));
        assertEquals(0, container.defaultTransitionSetting.resolve(0));

        container.defaultTransitionSetting.setSeconds(0);
        assertEquals(0, container.defaultTransitionSetting.resolve(null));
        assertEquals(12, container.defaultTransitionSetting.resolve(12));
    }
}
