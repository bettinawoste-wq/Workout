package com.wob.workout.data.repository;

import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;

import com.wob.workout.data.dao.ReminderDao;
import com.wob.workout.data.database.AppDatabase;
import com.wob.workout.data.database.DatabaseProvider;
import com.wob.workout.data.entity.Reminder;

import java.util.List;

/** The stored reminders, ordered by time of day. Every method but {@link #observeAll()}
 *  runs off the main thread. */
public class ReminderRepository {

    private final ReminderDao dao;

    public ReminderRepository(Context context) {
        this(DatabaseProvider.get(context.getApplicationContext()));
    }

    /** Talks to a given database directly, bypassing the DatabaseProvider singleton.
     *  Production code must go through {@link #ReminderRepository(Context)} so every
     *  repository shares the same database instance and invalidation tracker. */
    @VisibleForTesting
    public ReminderRepository(AppDatabase database) {
        this.dao = database.reminderDao();
    }

    public List<Reminder> getAll() {
        return dao.getAll();
    }

    public LiveData<List<Reminder>> observeAll() {
        return dao.observeAll();
    }

    public long insert(Reminder reminder) {
        return dao.insert(reminder);
    }

    public void update(Reminder reminder) {
        dao.update(reminder);
    }

    public void delete(Reminder reminder) {
        dao.delete(reminder);
    }
}
