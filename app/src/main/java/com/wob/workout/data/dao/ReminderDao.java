package com.wob.workout.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.wob.workout.data.entity.Reminder;

import java.util.List;

@Dao
public interface ReminderDao {

    @Insert
    long insert(Reminder reminder);

    @Update
    void update(Reminder reminder);

    @Delete
    void delete(Reminder reminder);

    @Query("SELECT * FROM Reminder ORDER BY minuteOfDay, id")
    List<Reminder> getAll();

    @Query("SELECT * FROM Reminder ORDER BY minuteOfDay, id")
    LiveData<List<Reminder>> observeAll();
}
