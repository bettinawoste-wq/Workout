package com.wob.workout.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.wob.workout.training.Weekdays;

/**
 * A reminder: a time of day on chosen weekdays, switched on or off on its own. Not part of
 * the backup.
 */
@Entity
public class Reminder {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Local time of day in minutes after midnight, 0 to 1439. */
    public int minuteOfDay;

    /** The weekdays as stored, {@link Weekdays#bits()}; read them through {@link #days()}. */
    public int weekdays;

    public boolean enabled;

    public Reminder() {
    }

    @Ignore
    public Reminder(int minuteOfDay, Weekdays days, boolean enabled) {
        this.minuteOfDay = minuteOfDay;
        this.weekdays = days.bits();
        this.enabled = enabled;
    }

    /** The weekdays it is set for. */
    public Weekdays days() {
        return Weekdays.fromBits(weekdays);
    }
}
