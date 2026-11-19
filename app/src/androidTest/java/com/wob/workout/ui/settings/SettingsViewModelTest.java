package com.wob.workout.ui.settings;

import static com.wob.workout.LiveDataTestUtil.getOrAwaitValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.wob.workout.AppContainer;
import com.wob.workout.TestAppContainer;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** The settings screen's Appearance and Player sections (issue #102). */
@RunWith(AndroidJUnit4.class)
public class SettingsViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

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
    public void showsTheDefaultsOfAFreshInstall() throws InterruptedException {
        SettingsViewModel viewModel = new SettingsViewModel(container);

        assertEquals(Design.SYSTEM, getOrAwaitValue(viewModel.getDesign()));
        assertTrue(getOrAwaitValue(viewModel.isSoundOn()));
        assertFalse(getOrAwaitValue(viewModel.isVibrationOn()));
    }

    @Test
    public void aChosenDesignIsShownAndKept() throws InterruptedException {
        SettingsViewModel viewModel = new SettingsViewModel(container);

        viewModel.setDesign(Design.DARK);

        assertEquals(Design.DARK, getOrAwaitValue(viewModel.getDesign()));
        assertEquals(Design.DARK,
                getOrAwaitValue(new SettingsViewModel(container).getDesign()));
    }

    @Test
    public void theSoundSwitchIsTheSameAsThePlayers() throws InterruptedException {
        SettingsViewModel viewModel = new SettingsViewModel(container);

        viewModel.setSoundOn(false);

        assertFalse(getOrAwaitValue(viewModel.isSoundOn()));
        assertFalse(container.soundSetting.isOn());

        // Switched back on in the player's toolbar, then the settings screen opens again.
        container.soundSetting.setOn(true);
        assertTrue(getOrAwaitValue(new SettingsViewModel(container).isSoundOn()));
    }

    @Test
    public void theVibrationSwitchIsKept() throws InterruptedException {
        SettingsViewModel viewModel = new SettingsViewModel(container);

        viewModel.setVibrationOn(true);

        assertTrue(getOrAwaitValue(viewModel.isVibrationOn()));
        assertTrue(container.vibrationSetting.isOn());
    }
}
