package com.wob.workout.data.database;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.*;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.wob.workout.data.converter.MediaTypeConverter;
import com.wob.workout.data.dao.ExerciseDao;
import com.wob.workout.data.dao.ExerciseSessionDao;
import com.wob.workout.data.dao.ReminderDao;
import com.wob.workout.data.dao.WeeklyGoalDao;
import com.wob.workout.data.dao.WorkoutDao;
import com.wob.workout.data.dao.WorkoutExerciseDao;
import com.wob.workout.data.dao.WorkoutSessionDao;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.ExerciseSession;
import com.wob.workout.data.entity.Reminder;
import com.wob.workout.data.entity.WeeklyGoalChange;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.data.entity.WorkoutExerciseRow;
import com.wob.workout.data.entity.WorkoutSession;

@Database(
        entities = {
                Exercise.class,
                Workout.class,
                WorkoutExerciseRow.class,
                WorkoutSession.class,
                ExerciseSession.class,
                WeeklyGoalChange.class,
                Reminder.class
        },
        version = 10,
        exportSchema = true
)
@TypeConverters(MediaTypeConverter.class)
public abstract class AppDatabase extends RoomDatabase { 
    public abstract ExerciseDao exerciseDao();
    public abstract WorkoutDao workoutDao();
    public abstract WorkoutExerciseDao workoutExerciseDao();
    public abstract WorkoutSessionDao workoutSessionDao();
    public abstract ExerciseSessionDao exerciseSessionDao();
    public abstract WeeklyGoalDao weeklyGoalDao();
    public abstract ReminderDao reminderDao();

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `WorkoutSession` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`workoutId` INTEGER NOT NULL, " +
                            "`workoutName` TEXT, " +
                            "`startedAt` INTEGER NOT NULL, " +
                            "`finishedAt` INTEGER NOT NULL, " +
                            "`activeDurationSeconds` INTEGER NOT NULL, " +
                            "`completed` INTEGER NOT NULL, " +
                            "FOREIGN KEY(`workoutId`) REFERENCES `Workout`(`id`) " +
                            "ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_WorkoutSession_workoutId` " +
                            "ON `WorkoutSession` (`workoutId`)");

            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ExerciseSession` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`sessionId` INTEGER NOT NULL, " +
                            "`exerciseId` INTEGER NOT NULL, " +
                            "`exerciseTitle` TEXT, " +
                            "`position` INTEGER NOT NULL, " +
                            "`plannedDurationSeconds` INTEGER NOT NULL, " +
                            "`actualDurationSeconds` INTEGER NOT NULL, " +
                            "`startedAt` INTEGER NOT NULL, " +
                            "`finishedAt` INTEGER NOT NULL, " +
                            "FOREIGN KEY(`sessionId`) REFERENCES `WorkoutSession`(`id`) " +
                            "ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ExerciseSession_sessionId` " +
                            "ON `ExerciseSession` (`sessionId`)");
        }
    };

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // WorkoutSession soll unabhaengig vom Workout weiterbestehen, deshalb hier die
            // Fremdschluessel-Kaskade auf Workout entfernen (SQLite kann das nur per Neuanlage).
            db.execSQL("ALTER TABLE `WorkoutSession` RENAME TO `WorkoutSession_old`");

            db.execSQL(
                    "CREATE TABLE `WorkoutSession` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`workoutId` INTEGER NOT NULL, " +
                            "`workoutName` TEXT, " +
                            "`startedAt` INTEGER NOT NULL, " +
                            "`finishedAt` INTEGER NOT NULL, " +
                            "`activeDurationSeconds` INTEGER NOT NULL, " +
                            "`completed` INTEGER NOT NULL)");

            db.execSQL(
                    "INSERT INTO `WorkoutSession` " +
                            "(`id`, `workoutId`, `workoutName`, `startedAt`, `finishedAt`, " +
                            "`activeDurationSeconds`, `completed`) " +
                            "SELECT `id`, `workoutId`, `workoutName`, `startedAt`, `finishedAt`, " +
                            "`activeDurationSeconds`, `completed` " +
                            "FROM `WorkoutSession_old`");

            db.execSQL("DROP TABLE `WorkoutSession_old`");

            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_WorkoutSession_workoutId` " +
                            "ON `WorkoutSession` (`workoutId`)");
        }
    };

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Left NULL for existing exercises, which means the default framing (ADR 0003).
            db.execSQL("ALTER TABLE `Exercise` ADD COLUMN `framingZoom` REAL");
            db.execSQL("ALTER TABLE `Exercise` ADD COLUMN `framingOffsetX` REAL");
            db.execSQL("ALTER TABLE `Exercise` ADD COLUMN `framingOffsetY` REAL");
        }
    };

    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // The transition lasts 3 seconds in every workout (issue #67).
            db.execSQL("UPDATE `Workout` SET `transitionSeconds` = 3");
        }
    };

    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // A workout's transition becomes optional, and every existing workout follows
            // the default transition (issue #103). SQLite can only drop NOT NULL by building
            // the table anew; foreign keys are not enforced during a migration, so dropping
            // the old table leaves the workout exercises that refer to it alone.
            db.execSQL(
                    "CREATE TABLE `Workout_new` ("
                            + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                            + " `name` TEXT,"
                            + " `transitionSeconds` INTEGER,"
                            + " `countdownEnabled` INTEGER NOT NULL)");
            db.execSQL(
                    "INSERT INTO `Workout_new` (`id`, `name`, `transitionSeconds`,"
                            + " `countdownEnabled`)"
                            + " SELECT `id`, `name`, NULL, `countdownEnabled` FROM `Workout`");
            // Ids of deleted workouts stay used: the history still refers to them.
            long lastId = 0;
            try (Cursor cursor = db.query(
                    "SELECT `seq` FROM `sqlite_sequence` WHERE `name` = 'Workout'")) {
                if (cursor.moveToFirst()) lastId = cursor.getLong(0);
            }
            db.execSQL("DROP TABLE `Workout`");
            db.execSQL("ALTER TABLE `Workout_new` RENAME TO `Workout`");
            db.execSQL("DELETE FROM `sqlite_sequence` WHERE `name` = 'Workout'");
            db.execSQL("INSERT INTO `sqlite_sequence` (`name`, `seq`) VALUES ('Workout',"
                    + " MAX(" + lastId + ", (SELECT IFNULL(MAX(`id`), 0) FROM `Workout`)))");
        }
    };

    public static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // A place in a workout can be a rest, with no exercise (issue #104): the
            // exercise reference becomes optional, which SQLite can only do by building the
            // table anew. Existing places keep their exercise, id and order.
            db.execSQL(
                    "CREATE TABLE `WorkoutExercise_new` ("
                            + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                            + " `workoutId` INTEGER NOT NULL,"
                            + " `exerciseId` INTEGER,"
                            + " `position` INTEGER NOT NULL,"
                            + " `durationSeconds` INTEGER NOT NULL,"
                            + " FOREIGN KEY(`workoutId`) REFERENCES `Workout`(`id`)"
                            + " ON UPDATE NO ACTION ON DELETE CASCADE ,"
                            + " FOREIGN KEY(`exerciseId`) REFERENCES `Exercise`(`id`)"
                            + " ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL(
                    "INSERT INTO `WorkoutExercise_new` (`id`, `workoutId`, `exerciseId`,"
                            + " `position`, `durationSeconds`)"
                            + " SELECT `id`, `workoutId`, `exerciseId`, `position`,"
                            + " `durationSeconds` FROM `WorkoutExercise`");
            db.execSQL("DROP TABLE `WorkoutExercise`");
            db.execSQL("ALTER TABLE `WorkoutExercise_new` RENAME TO `WorkoutExercise`");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_WorkoutExercise_workoutId`"
                    + " ON `WorkoutExercise` (`workoutId`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_WorkoutExercise_exerciseId`"
                    + " ON `WorkoutExercise` (`exerciseId`)");
            // A workout session's rest time; none for those recorded before.
            db.execSQL("ALTER TABLE `WorkoutSession` ADD COLUMN `restSeconds` INTEGER");
        }
    };

    public static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // The weekly goal's changes, each from the week it was made in (issue #105).
            // None yet: there is no goal until one is set.
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `WeeklyGoalChange` ("
                            + "`weekStartDay` INTEGER NOT NULL,"
                            + " `trainingDays` INTEGER,"
                            + " PRIMARY KEY(`weekStartDay`))");
        }
    };

    public static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Reminders (issue #106). None yet: there are none until the user adds one.
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `Reminder` ("
                            + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                            + " `minuteOfDay` INTEGER NOT NULL,"
                            + " `weekdays` INTEGER NOT NULL,"
                            + " `enabled` INTEGER NOT NULL)");
        }
    };

    /** Every migration, in order. {@link DatabaseProvider} registers exactly these, and
     *  {@code AllMigrationsTest} checks they lead from version 4 to the current schema. */
    public static final Migration[] ALL_MIGRATIONS = {
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10
    };
}
