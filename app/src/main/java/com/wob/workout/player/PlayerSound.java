package com.wob.workout.player;

/** The sounds of a workout session, each reported once, when it is due. */
public enum PlayerSound {
    /** One for each of the last three seconds of a countdown or transition. */
    BEEP,
    /** An exercise a countdown led into begins; not one the user stepped to. */
    START,
    /** The workout is completed. */
    SUCCESS
}
