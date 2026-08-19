package com.wob.workout.data.database;

import android.content.Context;
import androidx.room.Room;

public class DatabaseProvider {
    private static AppDatabase INSTANCE;

    public static synchronized AppDatabase get(Context context) {

        if (INSTANCE == null) {

            INSTANCE =
                    Room.databaseBuilder(
                                    context,
                                    AppDatabase.class,
                                    "workout.db")
                            .fallbackToDestructiveMigration()
                            // TODO später entfernen
                            .allowMainThreadQueries()
                            .build();
        }

        return INSTANCE;
    }
}
