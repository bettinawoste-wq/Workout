package com.wob.workout.backup;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.wob.workout.AppContainer;
import com.wob.workout.TestAppContainer;
import com.wob.workout.data.WorkoutExercise;
import com.wob.workout.data.database.AppDatabase;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.ExerciseSession;
import com.wob.workout.data.entity.MediaType;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.data.entity.WorkoutSession;
import com.wob.workout.media.Framing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The backup (issue #107): export, preview and both ways of reading it back, on the
 * in-memory database and a media folder of the test's own, never the app's.
 */
@RunWith(AndroidJUnit4.class)
public class BackupTest {

    private static final byte[] PHOTO = {1, 2, 3, 4, 5};

    private TestAppContainer testContainer;
    private AppContainer container;
    private AppDatabase db;
    private File mediaFolder;
    private Backup backup;

    @Before
    public void setUp() throws IOException {
        testContainer = new TestAppContainer();
        container = testContainer.getAppContainer();
        db = testContainer.getDatabase();
        Context context = ApplicationProvider.getApplicationContext();
        mediaFolder = new File(context.getCacheDir(), "backup-test-media");
        deleteFolder(mediaFolder);
        assertTrue(mediaFolder.mkdirs());
        backup = new Backup(context, mediaFolder);
    }

    @After
    public void tearDown() {
        testContainer.close();
        deleteFolder(mediaFolder);
    }

    @Test
    public void exportThenReplaceRestoresTheSameContentWithMedia() throws Exception {
        Seed seed = seed();
        byte[] file = export();
        container.soundSetting.setOn(false);
        long now = System.currentTimeMillis();
        container.weeklyGoalRepository.setGoal(3, now, TimeZone.getDefault());
        // What happens on the phone after the export goes away with the replace.
        long later = insertExercise("Später", "later.jpg", new byte[]{9});
        testContainer.insertSession(seed.workoutId, 3_000, true);

        backup.replaceWith(new ByteArrayInputStream(file));

        List<Exercise> exercises = exercisesInOrder();
        assertEquals(Arrays.asList("Liegestütz", "Kniebeuge"), titlesOf(exercises));
        Exercise pushUp = exercises.get(0);
        assertEquals("Arme beugen", pushUp.description);
        assertEquals(MediaType.IMAGE, pushUp.mediaType);
        assertEquals(1.5f, pushUp.framingZoom, 0f);
        assertEquals(0.1f, pushUp.framingOffsetX, 0f);
        assertEquals(-0.2f, pushUp.framingOffsetY, 0f);
        assertArrayEquals(PHOTO, Files.readAllBytes(new File(mediaFolder, pushUp.mediaFile)
                .toPath()));
        assertNull(exercises.get(1).mediaFile);
        assertFalse("the replaced exercise's photo is gone",
                new File(mediaFolder, "later.jpg").exists());
        assertNull(db.exerciseDao().getById(later));

        List<Workout> workouts = container.workoutRepository.getAll();
        assertEquals(1, workouts.size());
        Workout workout = workouts.get(0);
        assertEquals("Ganzkörper", workout.name);
        assertEquals(Integer.valueOf(5), workout.transitionSeconds);
        assertPlaces(container.workoutRepository.getWorkoutExercises(workout.id),
                exercises.get(0), exercises.get(1));

        List<WorkoutSession> sessions = sessionsInOrder();
        assertEquals(2, sessions.size());
        WorkoutSession run = sessions.get(0);
        assertEquals(workout.id, run.workoutId);
        assertEquals("Ganzkörper", run.workoutName);
        assertEquals(1_000, run.startedAt);
        assertTrue(run.completed);
        assertEquals(Integer.valueOf(30), run.restSeconds);
        assertEquals(Collections.singletonList("Liegestütz"),
                exerciseTitlesOf(db.exerciseSessionDao().getBySession(run.id)));
        WorkoutSession ofDeletedWorkout = sessions.get(1);
        assertEquals("Gelöscht", ofDeletedWorkout.workoutName);
        assertNull(ofDeletedWorkout.restSeconds);
        assertNotEquals("a deleted workout's history stays apart from every workout",
                workout.id, ofDeletedWorkout.workoutId);

        assertFalse("settings are not part of the backup", container.soundSetting.isOn());
        assertEquals("nor is the weekly goal", Integer.valueOf(3),
                container.weeklyGoalRepository.getGoal(now, TimeZone.getDefault()));
    }

    @Test
    public void exportThenAddAlongsideDoublesItWithReferencesIntact() throws Exception {
        Seed seed = seed();
        byte[] file = export();

        backup.addAlongside(new ByteArrayInputStream(file));

        List<Exercise> exercises = exercisesInOrder();
        assertEquals(Arrays.asList("Liegestütz", "Kniebeuge", "Liegestütz", "Kniebeuge"),
                titlesOf(exercises));
        Exercise original = exercises.get(0);
        Exercise copy = exercises.get(2);
        assertEquals(seed.pushUpId, original.id);
        assertEquals("pushup.jpg", original.mediaFile);
        assertNotEquals("the copy's photo has a name of its own",
                original.mediaFile, copy.mediaFile);
        assertArrayEquals(PHOTO, Files.readAllBytes(new File(mediaFolder, original.mediaFile)
                .toPath()));
        assertArrayEquals(PHOTO, Files.readAllBytes(new File(mediaFolder, copy.mediaFile)
                .toPath()));

        List<Workout> workouts = container.workoutRepository.getAll();
        assertEquals(2, workouts.size());
        Workout workoutCopy = workouts.get(0);
        assertNotEquals(seed.workoutId, workoutCopy.id);
        assertEquals("Ganzkörper", workoutCopy.name);
        assertPlaces(container.workoutRepository.getWorkoutExercises(seed.workoutId),
                exercises.get(0), exercises.get(1));
        assertPlaces(container.workoutRepository.getWorkoutExercises(workoutCopy.id),
                exercises.get(2), exercises.get(3));

        List<WorkoutSession> sessions = sessionsInOrder();
        assertEquals(4, sessions.size());
        assertEquals(seed.workoutId, sessions.get(0).workoutId);
        assertEquals(workoutCopy.id, sessions.get(2).workoutId);
        assertEquals(Collections.singletonList("Liegestütz"),
                exerciseTitlesOf(db.exerciseSessionDao().getBySession(sessions.get(2).id)));
        assertEquals("both copies of a deleted workout's history stay apart from every workout",
                3, Arrays.asList(seed.workoutId, workoutCopy.id, sessions.get(3).workoutId)
                        .stream().distinct().count());
    }

    @Test
    public void thePreviewCountsWhatTheFileHolds() throws Exception {
        seed();

        BackupPreview preview = backup.preview(new ByteArrayInputStream(export()));

        assertNull(preview.refusal);
        assertEquals(2, preview.exercises);
        assertEquals(1, preview.workouts);
        assertEquals(2, preview.workoutSessions);
        assertEquals("2 Übungen, 1 Workout, 2 Einträge im Verlauf", preview.text());
    }

    @Test
    public void aFileFromANewerVersionIsRefusedAndNothingChanges() throws Exception {
        seed();
        byte[] file = zip("{\"formatVersion\": " + (Backup.FORMAT_VERSION + 1)
                + ", \"exercises\": []}", "media/x.jpg");

        assertEquals(BackupRefused.Reason.NEWER_VERSION,
                backup.preview(new ByteArrayInputStream(file)).refusal);
        assertRefused(BackupRefused.Reason.NEWER_VERSION, file);
    }

    @Test
    public void anUnreadableFileIsRefusedAndNothingChanges() throws Exception {
        seed();
        byte[] notAZip = "Einkaufsliste: Milch, Brot".getBytes(StandardCharsets.UTF_8);

        assertEquals(BackupRefused.Reason.UNREADABLE,
                backup.preview(new ByteArrayInputStream(notAZip)).refusal);
        assertRefused(BackupRefused.Reason.UNREADABLE, notAZip);
    }

    @Test
    public void aFileThatFailsHalfwayLeavesNeitherDataNorMediaBehind() throws Exception {
        seed();
        // Readable up to the workout, whose place refers to an exercise the file lacks.
        byte[] file = zip("{\"formatVersion\": 1,"
                + " \"exercises\": [{\"id\": 1, \"title\": \"Neu\", \"mediaFile\": \"x.jpg\","
                + " \"mediaType\": \"IMAGE\"}],"
                + " \"workouts\": [{\"id\": 1, \"name\": \"Kaputt\"}],"
                + " \"workoutExercises\": [{\"workoutId\": 1, \"exerciseId\": 7,"
                + " \"position\": 0, \"durationSeconds\": 30}],"
                + " \"workoutSessions\": [], \"exerciseSessions\": []}", "media/x.jpg");

        assertRefused(BackupRefused.Reason.UNREADABLE, file);
    }

    /** Both ways of reading {@code file} back are refused, and the seed stays as it was. */
    private void assertRefused(BackupRefused.Reason reason, byte[] file) throws Exception {
        List<String> mediaBefore = mediaFiles();
        for (boolean replace : new boolean[]{true, false}) {
            try {
                if (replace) {
                    backup.replaceWith(new ByteArrayInputStream(file));
                } else {
                    backup.addAlongside(new ByteArrayInputStream(file));
                }
                fail("expected the file to be refused");
            } catch (BackupRefused refused) {
                assertEquals(reason, refused.reason);
            }
            assertEquals(Arrays.asList("Liegestütz", "Kniebeuge"),
                    titlesOf(exercisesInOrder()));
            assertEquals(1, container.workoutRepository.getAll().size());
            assertEquals(2, sessionsInOrder().size());
            assertEquals(mediaBefore, mediaFiles());
        }
    }

    private static final class Seed {
        long pushUpId;
        long workoutId;
    }

    /** Two exercises (one with a framed photo), a workout with a rest between them, one run
     *  of it and one run of a workout deleted since. */
    private Seed seed() throws IOException {
        Seed seed = new Seed();
        seed.pushUpId = insertExercise("Liegestütz", "pushup.jpg", PHOTO);
        Exercise pushUp = db.exerciseDao().getById(seed.pushUpId);
        pushUp.description = "Arme beugen";
        pushUp.storeFraming(new Framing(1.5f, 0.1f, -0.2f));
        db.exerciseDao().update(pushUp);
        long squat = testContainer.insertExercise("Kniebeuge");

        seed.workoutId = container.workoutRepository.saveWithExercises(0, "Ganzkörper", 5,
                Arrays.asList(
                        new WorkoutExercise(0, pushUp, 40),
                        WorkoutExercise.addedRest(30),
                        new WorkoutExercise(0, db.exerciseDao().getById(squat), 45)));

        long run = testContainer.insertSession(seed.workoutId, 1_000, true);
        WorkoutSession session = db.workoutSessionDao().getById(run);
        session.workoutName = "Ganzkörper";
        session.restSeconds = 30;
        db.workoutSessionDao().update(session);
        container.workoutHistoryRepository.recordExercise(run, seed.pushUpId, "Liegestütz", 0,
                40, 38, 1_000, 39_000);

        long deleted = testContainer.insertWorkout("Gelöscht");
        long gone = testContainer.insertSession(deleted, 2_000, false);
        WorkoutSession goneSession = db.workoutSessionDao().getById(gone);
        goneSession.workoutName = "Gelöscht";
        db.workoutSessionDao().update(goneSession);
        db.workoutDao().delete(db.workoutDao().getById(deleted));
        return seed;
    }

    private long insertExercise(String title, String mediaFile, byte[] content)
            throws IOException {
        try (FileOutputStream out = new FileOutputStream(new File(mediaFolder, mediaFile))) {
            out.write(content);
        }
        Exercise exercise = new Exercise();
        exercise.title = title;
        exercise.mediaFile = mediaFile;
        exercise.mediaType = MediaType.IMAGE;
        return container.exerciseRepository.save(exercise);
    }

    private byte[] export() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        backup.export(out);
        return out.toByteArray();
    }

    private static byte[] zip(String data, String mediaEntry) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("data.json"));
            zip.write(data.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(mediaEntry));
            zip.write(PHOTO);
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    /** A workout's places as the seed made them: exercise, 30-second rest, exercise. */
    private static void assertPlaces(List<WorkoutExercise> places, Exercise first,
                                     Exercise second) {
        assertEquals(3, places.size());
        assertEquals(first.id, places.get(0).exercise.id);
        assertEquals(40, places.get(0).durationSeconds);
        assertTrue(places.get(1).isRest());
        assertEquals(30, places.get(1).durationSeconds);
        assertEquals(second.id, places.get(2).exercise.id);
        assertEquals(45, places.get(2).durationSeconds);
    }

    private List<Exercise> exercisesInOrder() {
        List<Exercise> exercises = new ArrayList<>(container.exerciseRepository.getAll());
        exercises.sort((a, b) -> Long.compare(a.id, b.id));
        return exercises;
    }

    private List<WorkoutSession> sessionsInOrder() {
        List<WorkoutSession> sessions = new ArrayList<>(db.workoutSessionDao().getAll());
        sessions.sort((a, b) -> Long.compare(a.id, b.id));
        return sessions;
    }

    private List<String> mediaFiles() {
        String[] names = mediaFolder.list();
        List<String> sorted = new ArrayList<>(Arrays.asList(names != null ? names
                : new String[0]));
        Collections.sort(sorted);
        return sorted;
    }

    private static List<String> titlesOf(List<Exercise> exercises) {
        List<String> titles = new ArrayList<>();
        for (Exercise exercise : exercises) titles.add(exercise.title);
        return titles;
    }

    private static List<String> exerciseTitlesOf(List<ExerciseSession> exerciseSessions) {
        List<String> titles = new ArrayList<>();
        for (ExerciseSession exerciseSession : exerciseSessions) {
            titles.add(exerciseSession.exerciseTitle);
        }
        return titles;
    }

    private static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) file.delete();
        }
        folder.delete();
    }
}
