package com.wob.workout.data.database;

import androidx.annotation.NonNull;
import androidx.room.*;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.wob.workout.data.converter.MediaTypeConverter;
import com.wob.workout.data.dao.ExerciseDao;
import com.wob.workout.data.dao.ExerciseSessionDao;
import com.wob.workout.data.dao.WorkoutDao;
import com.wob.workout.data.dao.WorkoutExerciseDao;
import com.wob.workout.data.dao.WorkoutSessionDao;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.ExerciseSession;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.data.entity.WorkoutExercise;
import com.wob.workout.data.entity.WorkoutSession;

@Database(
        entities = {
                Exercise.class,
                Workout.class,
                WorkoutExercise.class,
                WorkoutSession.class,
                ExerciseSession.class
        },
        version = 5,
        exportSchema = true
)
@TypeConverters(MediaTypeConverter.class)
public abstract class AppDatabase extends RoomDatabase { 
    public abstract ExerciseDao exerciseDao();
    public abstract WorkoutDao workoutDao();
    public abstract WorkoutExerciseDao workoutExerciseDao();
    public abstract WorkoutSessionDao workoutSessionDao();
    public abstract ExerciseSessionDao exerciseSessionDao();

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
                            "FOREIGN KEY(`workoutId`) REFERENCES `Workout`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_WorkoutSession_workoutId` ON `WorkoutSession` (`workoutId`)");

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
                            "FOREIGN KEY(`sessionId`) REFERENCES `WorkoutSession`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ExerciseSession_sessionId` ON `ExerciseSession` (`sessionId`)");
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
                            "(`id`, `workoutId`, `workoutName`, `startedAt`, `finishedAt`, `activeDurationSeconds`, `completed`) " +
                            "SELECT `id`, `workoutId`, `workoutName`, `startedAt`, `finishedAt`, `activeDurationSeconds`, `completed` " +
                            "FROM `WorkoutSession_old`");

            db.execSQL("DROP TABLE `WorkoutSession_old`");

            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_WorkoutSession_workoutId` ON `WorkoutSession` (`workoutId`)");
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
}
