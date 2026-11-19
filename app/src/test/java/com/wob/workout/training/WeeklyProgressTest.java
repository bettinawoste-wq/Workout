package com.wob.workout.training;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.wob.workout.data.entity.WeeklyGoalChange;
import com.wob.workout.data.entity.WorkoutSession;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/** The weekly goal's progress and streak, from the history and the goal's changes. */
public class WeeklyProgressTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    /** A Wednesday; its week runs from Monday 16 to Sunday 22 November 2026. */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 18, 18, 30);

    private final List<WorkoutSession> sessions = new ArrayList<>();
    private final List<WeeklyGoalChange> goalChanges = new ArrayList<>();

    @Test
    public void withoutAGoalTheLineIsNotShown() {
        completed(2026, 11, 16);

        WeeklyProgress progress = progress();

        assertFalse(progress.isShown());
        assertNull(progress.getGoal());
        assertEquals(0, progress.getStreakWeeks());
    }

    @Test
    public void countsDaysWithACompletedWorkoutSessionOnceEach() {
        goalFrom(2026, 11, 16, 3);
        completed(2026, 11, 16);
        completed(2026, 11, 18);
        completedAt(LocalDateTime.of(2026, 11, 18, 7, 0));
        aborted(2026, 11, 17);

        WeeklyProgress progress = progress();

        assertTrue(progress.isShown());
        assertEquals(Integer.valueOf(3), progress.getGoal());
        assertEquals(2, progress.getTrainingDaysThisWeek());
    }

    @Test
    public void aWeekRunsFromMondayToSundayInLocalTime() {
        goalFrom(2026, 11, 9, 3);
        // Sunday night before the week, and Monday just after midnight local time.
        completedAt(LocalDateTime.of(2026, 11, 15, 23, 50));
        completedAt(LocalDateTime.of(2026, 11, 16, 0, 10));
        // Sunday at the end of this week, from a later look at the week.
        completedAt(LocalDateTime.of(2026, 11, 22, 21, 0));

        assertEquals(2, progressAt(LocalDateTime.of(2026, 11, 22, 22, 0))
                .getTrainingDaysThisWeek());
        assertEquals(0, progressAt(LocalDateTime.of(2026, 11, 23, 8, 0))
                .getTrainingDaysThisWeek());
    }

    @Test
    public void theStreakCountsPastWeeksInARowThatReachedTheGoal() {
        goalFrom(2026, 10, 26, 2);
        trainedTwiceInWeekOf(2026, 10, 26);
        // The week of 2 November missed the goal with one day.
        completed(2026, 11, 3);
        trainedTwiceInWeekOf(2026, 11, 9);

        assertEquals(1, progress().getStreakWeeks());
    }

    @Test
    public void theRunningWeekCountsOnceReachedAndDoesNotBreakTheStreakBefore() {
        goalFrom(2026, 11, 2, 2);
        trainedTwiceInWeekOf(2026, 11, 2);
        trainedTwiceInWeekOf(2026, 11, 9);
        completed(2026, 11, 16);

        assertEquals(2, progress().getStreakWeeks());

        completed(2026, 11, 17);

        assertEquals(3, progress().getStreakWeeks());
    }

    @Test
    public void eachPastWeekIsMeasuredAgainstTheGoalThatAppliedThen() {
        goalFrom(2026, 11, 2, 2);
        trainedTwiceInWeekOf(2026, 11, 2);
        trainedTwiceInWeekOf(2026, 11, 9);
        // Raised in the current week: it applies from now on, not to the weeks before.
        goalFrom(2026, 11, 16, 4);

        WeeklyProgress progress = progress();

        assertEquals(Integer.valueOf(4), progress.getGoal());
        assertEquals(2, progress.getStreakWeeks());
    }

    @Test
    public void aRaisedGoalBreaksTheStreakWhereALaterWeekMissedIt() {
        goalFrom(2026, 11, 2, 2);
        trainedTwiceInWeekOf(2026, 11, 2);
        goalFrom(2026, 11, 9, 3);
        trainedTwiceInWeekOf(2026, 11, 9);

        assertEquals(0, progress().getStreakWeeks());
    }

    @Test
    public void removingTheGoalHidesTheLine() {
        goalFrom(2026, 11, 2, 2);
        trainedTwiceInWeekOf(2026, 11, 2);
        goalFrom(2026, 11, 16, null);

        WeeklyProgress progress = progress();

        assertFalse(progress.isShown());
        assertNull(progress.getGoal());
        assertEquals(0, progress.getStreakWeeks());
    }

    @Test
    public void aWeekWithoutAGoalBreaksTheStreakSoAGoalSetAgainStartsAfresh() {
        goalFrom(2026, 10, 26, 2);
        trainedTwiceInWeekOf(2026, 10, 26);
        goalFrom(2026, 11, 2, null);
        trainedTwiceInWeekOf(2026, 11, 2);
        goalFrom(2026, 11, 9, 2);
        trainedTwiceInWeekOf(2026, 11, 9);

        assertEquals(1, progress().getStreakWeeks());
    }

    @Test
    public void aGoalSetThisWeekStartsTheStreakAtNothingUntilReached() {
        trainedTwiceInWeekOf(2026, 11, 9);
        goalFrom(2026, 11, 16, 1);

        assertEquals(0, progress().getStreakWeeks());

        completed(2026, 11, 18);

        assertEquals(1, progress().getStreakWeeks());
    }

    @Test
    public void aReminderSpeaksUpOnADayWithoutACompletedWorkoutSession() {
        completed(2026, 11, 17);
        aborted(2026, 11, 18);

        assertFalse(progress().isReminderSilentToday());
    }

    @Test
    public void aReminderStaysSilentOnADayWithACompletedWorkoutSession() {
        completedAt(LocalDateTime.of(2026, 11, 18, 7, 0));

        assertTrue(progress().isReminderSilentToday());
    }

    @Test
    public void aReminderStaysSilentOnceTheWeeksGoalIsReached() {
        goalFrom(2026, 11, 16, 2);
        completed(2026, 11, 16);

        assertFalse(progress().isReminderSilentToday());

        completed(2026, 11, 17);

        assertTrue(progress().isReminderSilentToday());
    }

    private void trainedTwiceInWeekOf(int year, int month, int monday) {
        completed(year, month, monday);
        completed(year, month, monday + 2);
    }

    private WeeklyProgress progress() {
        return progressAt(NOW);
    }

    private WeeklyProgress progressAt(LocalDateTime now) {
        return WeeklyProgress.of(sessions, goalChanges,
                Clock.fixed(millis(now), TimeZone.getTimeZone(BERLIN)));
    }

    /** Sets the goal in the week of the given day, as the settings screen would then. */
    private void goalFrom(int year, int month, int day, Integer trainingDays) {
        long dayMillis = millis(LocalDate.of(year, month, day).atTime(12, 0));
        long weekStart = WeeklyProgress.weekStartDay(
                Clock.fixed(dayMillis, TimeZone.getTimeZone(BERLIN)));
        goalChanges.removeIf(change -> change.weekStartDay == weekStart);
        goalChanges.add(new WeeklyGoalChange(weekStart, trainingDays));
    }

    private void completed(int year, int month, int day) {
        completedAt(LocalDate.of(year, month, day).atTime(17, 0));
    }

    private void completedAt(LocalDateTime startedAt) {
        sessions.add(session(startedAt, true));
    }

    private void aborted(int year, int month, int day) {
        sessions.add(session(LocalDate.of(year, month, day).atTime(17, 0), false));
    }

    private static WorkoutSession session(LocalDateTime startedAt, boolean completed) {
        WorkoutSession session = new WorkoutSession();
        session.startedAt = millis(startedAt);
        session.finishedAt = session.startedAt + 20 * 60_000;
        session.completed = completed;
        return session;
    }

    private static long millis(LocalDateTime time) {
        return time.atZone(BERLIN).toInstant().toEpochMilli();
    }
}
