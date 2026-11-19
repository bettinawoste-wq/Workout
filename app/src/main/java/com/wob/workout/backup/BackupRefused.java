package com.wob.workout.backup;

/** A file the backup won't read back; nothing was changed. */
public class BackupRefused extends Exception {

    public enum Reason {
        /** Written by a newer version of the app, in a format this one doesn't know. */
        NEWER_VERSION,
        /** Not a backup, or a damaged one. */
        UNREADABLE
    }

    public final Reason reason;

    BackupRefused(Reason reason, Throwable cause) {
        super(reason.name(), cause);
        this.reason = reason;
    }
}
