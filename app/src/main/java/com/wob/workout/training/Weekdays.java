package com.wob.workout.training;

/**
 * Some days of the week, as a reminder is set for: a value, numbering the days 0 for Monday
 * through 6 for Sunday. Stored as one bit per day, Monday's lowest ({@link #bits()}); the
 * bit arithmetic lives here only.
 */
public final class Weekdays {

    public static final int MONDAY = 0;
    public static final int TUESDAY = 1;
    public static final int WEDNESDAY = 2;
    public static final int THURSDAY = 3;
    public static final int FRIDAY = 4;
    public static final int SATURDAY = 5;
    public static final int SUNDAY = 6;
    /** How many days a week has. */
    public static final int COUNT = 7;

    private static final int EVERY_DAY_BITS = (1 << COUNT) - 1;

    public static final Weekdays NONE = new Weekdays(0);
    public static final Weekdays MONDAY_TO_FRIDAY =
            of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY);

    private final int bits;

    private Weekdays(int bits) {
        this.bits = bits;
    }

    /** The given days, each 0 for Monday through 6 for Sunday. */
    public static Weekdays of(int... days) {
        Weekdays weekdays = NONE;
        for (int day : days) weekdays = weekdays.with(day, true);
        return weekdays;
    }

    /** The days as stored by {@link #bits()}. */
    public static Weekdays fromBits(int bits) {
        return new Weekdays(bits & EVERY_DAY_BITS);
    }

    /** One bit per day, Monday's lowest: how they are stored. */
    public int bits() {
        return bits;
    }

    public boolean contains(int day) {
        return (bits & bitOf(day)) != 0;
    }

    /** These days with {@code day} switched on or off. */
    public Weekdays with(int day, boolean on) {
        return new Weekdays(on ? bits | bitOf(day) : bits & ~bitOf(day));
    }

    public boolean isEmpty() {
        return bits == 0;
    }

    public boolean isEveryDay() {
        return bits == EVERY_DAY_BITS;
    }

    private static int bitOf(int day) {
        if (day < 0 || day >= COUNT) throw new IllegalArgumentException("day " + day);
        return 1 << day;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Weekdays && ((Weekdays) other).bits == bits;
    }

    @Override
    public int hashCode() {
        return bits;
    }

    @Override
    public String toString() {
        return "Weekdays(" + Integer.toBinaryString(bits) + ")";
    }
}
