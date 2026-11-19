package com.wob.workout.backup;

/** What an import does with the exercises, workouts and history already on the phone. */
public enum ImportMode {
    /** "Ersetzen": they are deleted first, so the phone holds exactly the file's. */
    REPLACE,
    /** "Zusammenführen": they stay, and the file's are added alongside as copies. */
    ADD_ALONGSIDE
}
