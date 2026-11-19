package com.wob.workout.ui.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.wob.workout.AppContainer;
import com.wob.workout.TestAppContainer;
import com.wob.workout.data.entity.WeeklyGoalChange;
import com.wob.workout.player.DefaultTransitionSetting;
import com.wob.workout.player.VibrationSetting;
import com.wob.workout.training.Clock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.TimeZone;

/** The app-wide choices of the settings screen (issue #102), on the test container's
 *  preferences file: each one's default on a fresh install, and that it persists; the
 *  default transition (issue #103) too. The weekly goal (issue #105) lives in the
 *  in-memory database, with its history of changes. */
@RunWith(AndroidJUnit4.class)
public class SettingsTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

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

    @Test
    public void thereIsNoWeeklyGoalOnAFreshInstall() {
        assertNull(container.weeklyGoalRepository.getGoal(at(2026, 11, 18)));
        assertTrue(container.weeklyGoalRepository.getChanges().isEmpty());
    }

    @Test
    public void settingChangingAndRemovingTheWeeklyGoalRecordsEachWithItsWeek() {
        // Wednesday 4, Thursday 12 and Monday 16 November 2026, in three weeks in a row.
        container.weeklyGoalRepository.setGoal(3, at(2026, 11, 4));
        container.weeklyGoalRepository.setGoal(4, at(2026, 11, 12));
        container.weeklyGoalRepository.setGoal(null, at(2026, 11, 16));

        List<WeeklyGoalChange> changes = container.weeklyGoalRepository.getChanges();
        assertEquals(3, changes.size());
        assertChange(changes.get(0), 2026, 11, 2, 3);
        assertChange(changes.get(1), 2026, 11, 9, 4);
        assertChange(changes.get(2), 2026, 11, 16, null);
        assertEquals(Integer.valueOf(4),
                container.weeklyGoalRepository.getGoal(at(2026, 11, 15)));
        assertNull(container.weeklyGoalRepository.getGoal(at(2026, 11, 18)));
    }

    @Test
    public void aSecondChangeInTheSameWeekReplacesTheFirst() {
        container.weeklyGoalRepository.setGoal(2, at(2026, 11, 2));
        container.weeklyGoalRepository.setGoal(3, at(2026, 11, 16));
        container.weeklyGoalRepository.setGoal(5, at(2026, 11, 22));

        List<WeeklyGoalChange> changes = container.weeklyGoalRepository.getChanges();
        assertEquals(2, changes.size());
        assertChange(changes.get(1), 2026, 11, 16, 5);
    }

    private static void assertChange(WeeklyGoalChange change, int year, int month,
            int monday, Integer trainingDays) {
        assertEquals(LocalDateTime.of(year, month, monday, 0, 0).toLocalDate().toEpochDay(),
                change.weekStartDay);
        assertEquals(trainingDays, change.trainingDays);
    }

    /** Noon of the given day in Berlin. */
    private static Clock at(int year, int month, int day) {
        return Clock.fixed(LocalDateTime.of(year, month, day, 12, 0).atZone(BERLIN).toInstant()
                .toEpochMilli(), TimeZone.getTimeZone(BERLIN));
    }
}
