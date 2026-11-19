package com.wob.workout.ui.settings;

import static com.wob.workout.LiveDataTestUtil.getOrAwaitValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.wob.workout.AppContainer;
import com.wob.workout.TestAppContainer;
import com.wob.workout.data.entity.Reminder;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/** The settings screen's Appearance and Player sections (issue #102) and its Training
 *  section's weekly goal (issue #105) and reminders (issue #106). */
@RunWith(AndroidJUnit4.class)
public class SettingsViewModelTest {

    private static final int MONDAY = Reminder.MONDAY;
    private static final int FRIDAY = Reminder.MONDAY << 4;

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

    @Test
    public void thereIsNoWeeklyGoalOnAFreshInstall() throws InterruptedException {
        assertNull(getOrAwaitValue(new SettingsViewModel(container).getWeeklyGoal()));
    }

    @Test
    public void aWeeklyGoalIsShownKeptAndCanBeRemoved() throws Exception {
        SettingsViewModel viewModel = new SettingsViewModel(container);
        getOrAwaitValue(viewModel.getWeeklyGoal());

        viewModel.setWeeklyGoal(3);

        assertEquals(Integer.valueOf(3), getOrAwaitValue(viewModel.getWeeklyGoal()));
        testContainer.drainIoExecutor();
        assertEquals(Integer.valueOf(3),
                getOrAwaitValue(new SettingsViewModel(container).getWeeklyGoal()));

        viewModel.setWeeklyGoal(null);
        testContainer.drainIoExecutor();

        assertNull(getOrAwaitValue(new SettingsViewModel(container).getWeeklyGoal()));
    }

    @Test
    public void thereAreNoRemindersOnAFreshInstall() throws InterruptedException {
        assertTrue(getOrAwaitValue(new SettingsViewModel(container).getReminders()).isEmpty());
    }

    @Test
    public void anAddedReminderIsListedSwitchedOnAndScheduled() throws Exception {
        SettingsViewModel viewModel = new SettingsViewModel(container);

        viewModel.addReminder(18 * 60 + 30, MONDAY | FRIDAY);
        testContainer.drainIoExecutor();

        List<Reminder> reminders = getOrAwaitValue(viewModel.getReminders());
        assertEquals(1, reminders.size());
        assertEquals(18 * 60 + 30, reminders.get(0).minuteOfDay);
        assertEquals(MONDAY | FRIDAY, reminders.get(0).weekdays);
        assertTrue(reminders.get(0).enabled);
        assertEquals(1, testContainer.getReminderAlarms().getRenewedWith().size());
    }

    @Test
    public void aReminderCanBeChangedSwitchedOffAndDeleted() throws Exception {
        SettingsViewModel viewModel = new SettingsViewModel(container);
        viewModel.addReminder(18 * 60, MONDAY);
        viewModel.addReminder(7 * 60, FRIDAY);
        testContainer.drainIoExecutor();
        Reminder evening = getOrAwaitValue(viewModel.getReminders()).get(1);

        viewModel.changeReminder(evening, 19 * 60, MONDAY | FRIDAY);
        testContainer.drainIoExecutor();
        Reminder changed = getOrAwaitValue(viewModel.getReminders()).get(1);
        assertEquals(19 * 60, changed.minuteOfDay);
        assertEquals(MONDAY | FRIDAY, changed.weekdays);

        viewModel.setReminderOn(changed, false);
        testContainer.drainIoExecutor();
        assertFalse(getOrAwaitValue(viewModel.getReminders()).get(1).enabled);
        assertFalse(testContainer.getReminderAlarms().getRenewedWith().get(1).enabled);

        viewModel.deleteReminder(changed);
        testContainer.drainIoExecutor();
        List<Reminder> left = getOrAwaitValue(viewModel.getReminders());
        assertEquals(1, left.size());
        assertEquals(7 * 60, left.get(0).minuteOfDay);
        assertEquals(1, testContainer.getReminderAlarms().getRenewedWith().size());
    }

    @Test
    public void aReminderWithoutWeekdaysIsNotStored() throws Exception {
        SettingsViewModel viewModel = new SettingsViewModel(container);

        viewModel.addReminder(18 * 60, 0);
        testContainer.drainIoExecutor();

        assertTrue(getOrAwaitValue(viewModel.getReminders()).isEmpty());
    }
}
