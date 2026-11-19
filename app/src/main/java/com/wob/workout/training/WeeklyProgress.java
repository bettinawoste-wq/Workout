package com.wob.workout.training;

import com.wob.workout.data.entity.WeeklyGoalChange;
import com.wob.workout.data.entity.WorkoutSession;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * The weekly goal's progress, worked out from the history and the goal's changes at one
 * moment: the current week's training days and goal, and the streak. Plain Java, so the
 * rules run in JVM tests; days are counted in the given time zone and weeks run Monday to
 * Sunday.
 */
public final class WeeklyProgress {

    private static final long MILLIS_PER_DAY = 24L * 60 * 60 * 1000;

    private final Set<Long> trainingDays;
    private final List<WeeklyGoalChange> goalChanges;
    private final long today;
    private final long currentWeek;

    private WeeklyProgress(Set<Long> trainingDays, List<WeeklyGoalChange> goalChanges,
            long today) {
        this.trainingDays = trainingDays;
        this.goalChanges = goalChanges;
        this.today = today;
        this.currentWeek = weekStartOf(today);
    }

    /**
     * @param sessions    the history's workout sessions; only completed ones count, on the
     *                    day they started
     * @param goalChanges every change of the weekly goal, in any order
     */
    public static WeeklyProgress of(List<WorkoutSession> sessions,
            List<WeeklyGoalChange> goalChanges, long nowMillis, TimeZone zone) {
        Set<Long> days = new HashSet<>();
        for (WorkoutSession session : sessions) {
            if (session.completed) days.add(day(session.startedAt, zone));
        }
        return new WeeklyProgress(days, goalChanges, day(nowMillis, zone));
    }

    /** The Monday of the week {@code millis} falls in, as days since 1970-01-01: the week a
     *  goal set at that moment applies from. */
    public static long weekStartDay(long millis, TimeZone zone) {
        return weekStartOf(day(millis, zone));
    }

    private static long weekStartOf(long day) {
        // 1970-01-01 was a Thursday, three days after a Monday.
        return day - Math.floorMod(day + 3, 7);
    }

    private static long day(long millis, TimeZone zone) {
        return Math.floorDiv(millis + zone.getOffset(millis), MILLIS_PER_DAY);
    }

    /** Whether the home screen shows the line: while a goal is set for the current week. */
    public boolean isShown() {
        return getGoal() != null;
    }

    /** The current week's goal in training days, or null without one. */
    public Integer getGoal() {
        return goalOf(currentWeek);
    }

    public int getTrainingDaysThisWeek() {
        return trainingDaysIn(currentWeek);
    }

    /** The weeks in a row, up to the current one, that reached the goal applying then. The
     *  current week counts once reached and doesn't break the streak while it is running; a
     *  week without a goal breaks it. */
    public int getStreakWeeks() {
        if (getGoal() == null) return 0;
        int streak = isReached(currentWeek) ? 1 : 0;
        for (long week = currentWeek - 7; isReached(week); week -= 7) {
            streak++;
        }
        return streak;
    }

    /** Whether a reminder stays silent today: once today has a completed workout session,
     *  or the current week has reached its goal. */
    public boolean isReminderSilentToday() {
        return trainingDays.contains(today) || isReached(currentWeek);
    }

    private boolean isReached(long weekStart) {
        Integer goal = goalOf(weekStart);
        return goal != null && trainingDaysIn(weekStart) >= goal;
    }

    private Integer goalOf(long weekStart) {
        WeeklyGoalChange applying = null;
        for (WeeklyGoalChange change : goalChanges) {
            if (change.weekStartDay <= weekStart
                    && (applying == null || change.weekStartDay > applying.weekStartDay)) {
                applying = change;
            }
        }
        return applying == null ? null : applying.trainingDays;
    }

    private int trainingDaysIn(long weekStart) {
        int count = 0;
        for (long day = weekStart; day < weekStart + 7; day++) {
            if (trainingDays.contains(day)) count++;
        }
        return count;
    }
}
