package com.wob.workout.training;

import java.util.TimeZone;

/**
 * The current time and the time zone that local days and times of day are counted in, read
 * together wherever the weekly goal or the reminders need "now". {@link #SYSTEM} in the app
 * ({@code AppContainer.clock}); tests use {@link #fixed}.
 */
public interface Clock {

    /** The phone's clock and time zone, read anew on every call, so a changed time zone
     *  counts at once. */
    Clock SYSTEM = new Clock() {
        @Override
        public long nowMillis() {
            return System.currentTimeMillis();
        }

        @Override
        public TimeZone zone() {
            return TimeZone.getDefault();
        }
    };

    long nowMillis();

    TimeZone zone();

    /** A clock that always reads the given moment in the given time zone. */
    static Clock fixed(long nowMillis, TimeZone zone) {
        return new Clock() {
            @Override
            public long nowMillis() {
                return nowMillis;
            }

            @Override
            public TimeZone zone() {
                return zone;
            }
        };
    }
}
