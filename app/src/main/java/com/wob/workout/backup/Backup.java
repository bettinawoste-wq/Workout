package com.wob.workout.backup;

import android.content.Context;

import com.wob.workout.data.database.AppDatabase;
import com.wob.workout.data.database.DatabaseProvider;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.ExerciseSession;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.data.entity.WorkoutExerciseRow;
import com.wob.workout.data.entity.WorkoutSession;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * The backup: one ZIP file holding a data document ({@code data.json}, with a format
 * version) and the exercises' photos and videos ({@code media/<name>}). Settings,
 * reminders and the weekly goal are not part of it.
 *
 * <p>Reading a file back first gives a {@link #preview}; nothing changes until
 * {@link #replaceWith} or {@link #addAlongside}. Both insert everything with new identities
 * (references inside the file remapped) and the media under new names, in one database
 * transaction; media copied for an import that fails are removed again. A file of a newer
 * format version, or one that can't be read, is refused before anything changes.
 *
 * <p>Blocking: call it off the main thread.
 */
public class Backup {

    /** The format version this app writes; it reads this one and every older one. */
    public static final int FORMAT_VERSION = 1;

    private static final String DATA_ENTRY = "data.json";
    private static final String MEDIA_PREFIX = "media/";

    private final Context context;
    private final File mediaFolder;

    /** @param mediaFolder where exercises' photos and videos are stored by name */
    public Backup(Context context, File mediaFolder) {
        this.context = context.getApplicationContext();
        this.mediaFolder = mediaFolder;
    }

    /** Writes everything that belongs in a backup to {@code out}, as one ZIP file. */
    public void export(OutputStream out) throws IOException {
        AppDatabase db = DatabaseProvider.get(context);
        BackupDocument document = db.runInTransaction(() -> BackupDocument.of(db));
        String json;
        try {
            json = document.toJson(FORMAT_VERSION);
        } catch (JSONException e) {
            throw new IOException(e);
        }
        ZipOutputStream zip = new ZipOutputStream(out);
        zip.putNextEntry(new ZipEntry(DATA_ENTRY));
        zip.write(json.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
        Set<String> mediaFiles = new LinkedHashSet<>();
        for (Exercise exercise : document.exercises) {
            if (exercise.mediaFile != null) mediaFiles.add(exercise.mediaFile);
        }
        for (String name : mediaFiles) {
            File file = new File(mediaFolder, name);
            if (!file.isFile()) continue;
            zip.putNextEntry(new ZipEntry(MEDIA_PREFIX + name));
            try (InputStream in = new FileInputStream(file)) {
                copy(in, zip);
            }
            zip.closeEntry();
        }
        zip.finish();
    }

    /** What the file holds, or why it would be refused. Changes nothing. */
    public BackupPreview preview(InputStream in) {
        try {
            BackupDocument document = read(in, null);
            return BackupPreview.readable(document.exercises.size(), document.workouts.size(),
                    document.workoutSessions.size());
        } catch (BackupRefused refused) {
            return BackupPreview.refused(refused.reason);
        } catch (IOException e) {
            return BackupPreview.refused(BackupRefused.Reason.UNREADABLE);
        }
    }

    /** Leaves the phone with exactly the file's exercises, workouts and history.
     *  @throws BackupRefused if the file can't be read back; nothing changed
     *  @throws IOException if its media couldn't be stored; nothing changed */
    public void replaceWith(InputStream in) throws BackupRefused, IOException {
        importFrom(in, ImportMode.REPLACE);
    }

    /** Adds the file's exercises, workouts and history alongside what is there, as copies.
     *  @throws BackupRefused if the file can't be read back; nothing changed
     *  @throws IOException if its media couldn't be stored; nothing changed */
    public void addAlongside(InputStream in) throws BackupRefused, IOException {
        importFrom(in, ImportMode.ADD_ALONGSIDE);
    }

    /** {@link #replaceWith} or {@link #addAlongside}, as {@code mode} says.
     *  @throws BackupRefused if the file can't be read back; nothing changed
     *  @throws IOException if its media couldn't be stored; nothing changed */
    public void importFrom(InputStream in, ImportMode mode) throws BackupRefused, IOException {
        // The replaced exercises' media go once the import holds.
        for (String name : insertAll(in, mode)) {
            //noinspection ResultOfMethodCallIgnored
            new File(mediaFolder, name).delete();
        }
    }

    /** @return the media files of what was replaced, to remove once the import holds */
    private List<String> insertAll(InputStream in, ImportMode mode)
            throws BackupRefused, IOException {
        Map<String, String> newNames = new HashMap<>();
        List<File> copied = new ArrayList<>();
        boolean done = false;
        try {
            BackupDocument document = read(in, (name, content) -> {
                String newName = UUID.randomUUID() + extensionOf(name);
                File target = new File(mediaFolder, newName);
                copied.add(target);
                try (OutputStream out = new FileOutputStream(target)) {
                    copy(content, out);
                }
                newNames.put(name, newName);
            });
            AppDatabase db = DatabaseProvider.get(context);
            List<String> replacedMedia;
            try {
                replacedMedia = db.runInTransaction(() -> {
                    List<String> previous = new ArrayList<>();
                    if (mode == ImportMode.REPLACE) {
                        for (Exercise exercise : db.exerciseDao().getAll()) {
                            if (exercise.mediaFile != null) previous.add(exercise.mediaFile);
                        }
                        db.workoutSessionDao().deleteAll();
                        db.workoutDao().deleteAll();
                        db.exerciseDao().deleteAll();
                    }
                    insert(db, document, newNames);
                    return previous;
                });
            } catch (BrokenReference e) {
                throw new BackupRefused(BackupRefused.Reason.UNREADABLE, e);
            }
            done = true;
            return replacedMedia;
        } finally {
            if (!done) {
                for (File file : copied) {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            }
        }
    }

    /** Inserts the document's rows with new ids, its references remapped to them. */
    private static void insert(AppDatabase db, BackupDocument document,
                               Map<String, String> newNames) {
        Map<Long, Long> exerciseIds = new HashMap<>();
        for (Exercise exercise : document.exercises) {
            long oldId = exercise.id;
            exercise.id = 0;
            exercise.mediaFile = exercise.mediaFile != null
                    ? newNames.get(exercise.mediaFile) : null;
            if (exercise.mediaFile == null) exercise.mediaType = null;
            exerciseIds.put(oldId, db.exerciseDao().insert(exercise));
        }

        Map<Long, Long> workoutIds = new HashMap<>();
        for (Workout workout : document.workouts) {
            long oldId = workout.id;
            workout.id = 0;
            workoutIds.put(oldId, db.workoutDao().insert(workout));
        }

        for (WorkoutExerciseRow place : document.workoutExercises) {
            place.id = 0;
            place.workoutId = mapped(workoutIds, place.workoutId);
            if (place.exerciseId != null) {
                place.exerciseId = mapped(exerciseIds, place.exerciseId);
            }
            db.workoutExerciseDao().insert(place);
        }

        Map<Long, Long> sessionIds = new HashMap<>();
        for (WorkoutSession session : document.workoutSessions) {
            long oldId = session.id;
            session.id = 0;
            Long workoutId = workoutIds.get(session.workoutId);
            if (workoutId == null) {
                // Recorded against a workout deleted since: an id no workout will ever
                // have, so this history stays apart from every workout.
                workoutId = reserveWorkoutId(db);
                workoutIds.put(session.workoutId, workoutId);
            }
            session.workoutId = workoutId;
            sessionIds.put(oldId, db.workoutSessionDao().insert(session));
        }

        for (ExerciseSession exerciseSession : document.exerciseSessions) {
            exerciseSession.id = 0;
            exerciseSession.sessionId = mapped(sessionIds, exerciseSession.sessionId);
            // Only kept as a record; one of an exercise deleted since refers to none.
            Long exerciseId = exerciseIds.get(exerciseSession.exerciseId);
            exerciseSession.exerciseId = exerciseId != null ? exerciseId : 0;
            db.exerciseSessionDao().insert(exerciseSession);
        }
    }

    /** A workout id that is never handed out again: workouts' ids are never reused. */
    private static long reserveWorkoutId(AppDatabase db) {
        Workout placeholder = new Workout();
        placeholder.id = db.workoutDao().insert(placeholder);
        db.workoutDao().delete(placeholder);
        return placeholder.id;
    }

    private static long mapped(Map<Long, Long> ids, long oldId) {
        Long newId = ids.get(oldId);
        if (newId == null) throw new BrokenReference("Nothing in the backup has id " + oldId);
        return newId;
    }

    /** A row of the document refers to one the document doesn't hold. */
    private static final class BrokenReference extends RuntimeException {
        BrokenReference(String message) {
            super(message);
        }
    }

    private interface MediaSink {
        void accept(String name, InputStream content) throws IOException;
    }

    /** Reads the document, handing every media file to {@code media} (skipped if null). */
    private static BackupDocument read(InputStream in, MediaSink media)
            throws BackupRefused, IOException {
        JSONObject json = null;
        try {
            ZipInputStream zip = new ZipInputStream(in);
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (DATA_ENTRY.equals(name)) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    copy(zip, bytes);
                    json = new JSONObject(new String(bytes.toByteArray(),
                            StandardCharsets.UTF_8));
                    if (BackupDocument.formatVersionOf(json) > FORMAT_VERSION) {
                        throw new BackupRefused(BackupRefused.Reason.NEWER_VERSION, null);
                    }
                } else if (media != null && name.startsWith(MEDIA_PREFIX)
                        && !entry.isDirectory()) {
                    media.accept(name.substring(MEDIA_PREFIX.length()), zip);
                }
            }
            if (json == null) {
                throw new BackupRefused(BackupRefused.Reason.UNREADABLE, null);
            }
            return BackupDocument.fromJson(json);
        } catch (JSONException | ZipException | IllegalArgumentException e) {
            throw new BackupRefused(BackupRefused.Reason.UNREADABLE, e);
        }
    }

    /** The name's extension, dot included, if it is a plain one; otherwise none. */
    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0) return "";
        String extension = name.substring(dot);
        return extension.matches("\\.[A-Za-z0-9]{1,8}") ? extension : "";
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int length;
        while ((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
        }
    }
}
