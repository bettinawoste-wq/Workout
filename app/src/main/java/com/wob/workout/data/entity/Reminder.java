package com.wob.workout.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * A reminder: a time of day on chosen weekdays, switched on or off on its own. Not part of
 * the backup.
 */
@Entity
public class Reminder {

    /** Monday's bit in {@link #weekdays}; Tuesday is the next bit up, through Sunday. */
    public static final int MONDAY = 1;

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Local time of day in minutes after midnight, 0 to 1439. */
    public int minuteOfDay;

    /** One bit per weekday, Monday's lowest ({@link #MONDAY}) through Sunday's. */
    public int weekdays;

    public boolean enabled;

    public Reminder() {
    }

    @Ignore
    public Reminder(int minuteOfDay, int weekdays, boolean enabled) {
        this.minuteOfDay = minuteOfDay;
        this.weekdays = weekdays;
        this.enabled = enabled;
    }

    /** Whether it is set for a weekday, 0 for Monday through 6 for Sunday. */
    public boolean isOn(int weekday) {
        return (weekdays & (MONDAY << weekday)) != 0;
    }
}
