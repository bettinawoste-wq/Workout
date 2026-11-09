package com.wob.workout.ui.exercise;

import static com.wob.workout.LiveDataTestUtil.getOrAwaitValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.net.Uri;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.wob.workout.AppContainer;
import com.wob.workout.MigrationTestHelper;
import com.wob.workout.data.Event;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.MediaType;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Covers {@link ExerciseEditViewModel}: the one-shot load, save (insert vs. update),
 * delete, and the one-shot {@code mediaError} {@link Event} (wayfinder ticket #7).
 *
 * <p>Media is only exercised where it is deterministic without real media files: a
 * {@code mediaFile} that doesn't exist on disk (the preview falls back to its name), and a
 * {@code Uri} no provider can type (the "unsupported type" error). The success path and
 * the copy-failure error of {@code onMediaSelected} both need a content {@code Uri} that
 * reports an image/video type, i.e. a real provider, and are left to the manual smoke test.
 */
@RunWith(AndroidJUnit4.class)
public class ExerciseEditViewModelTest {

    private static final long TIMEOUT_SECONDS = 2;
    private static final String NO_MEDIA_TEXT = "Kein Medium ausgewählt";
    private static final String UNSUPPORTED_TYPE_ERROR = "Dateityp nicht unterstützt";
    /** No provider is registered under this authority, so {@code getType()} returns null. */
    private static final Uri UNTYPEABLE_URI =
            Uri.parse("content://com.wob.workout.test.nonexistent/media/1");

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private MigrationTestHelper helper;
    private AppContainer container;

    @Before
    public void setUp() {
        helper = new MigrationTestHelper();
        container = helper.getAppContainer();
    }

    @After
    public void tearDown() {
        helper.close();
    }

    @Test
    public void newExerciseSkipsTheLoadAndShowsTheNoMediaPlaceholder() throws Exception {
        // isNew() is "exerciseId <= 0", so both 0 and a negative id count as new.
        for (long newExerciseId : new long[] {0, -1}) {
            ExerciseEditViewModel viewModel = new ExerciseEditViewModel(container, newExerciseId);

            assertTrue(viewModel.isNew());
            // Set synchronously in the constructor, not via the executor.
            assertPreviewText(NO_MEDIA_TEXT, viewModel.getPreview().getValue());
            awaitExecutorIdle();
            assertNull(viewModel.getExercise().getValue());
        }
    }

    @Test
    public void constructorLoadsExistingExercise() throws InterruptedException {
        long id = insertExercise("Squat", "Feet shoulder-width", null, null);

        ExerciseEditViewModel viewModel = new ExerciseEditViewModel(container, id);

        assertFalse(viewModel.isNew());
        Exercise exercise = getOrAwaitValue(viewModel.getExercise());
        assertEquals(id, exercise.id);
        assertEquals("Squat", exercise.title);
        assertEquals("Feet shoulder-width", exercise.description);
        assertPreviewText(NO_MEDIA_TEXT, getOrAwaitValue(viewModel.getPreview()));
    }

    @Test
    public void previewFallsBackToTheFileNameWhenTheMediaFileIsMissing()
            throws InterruptedException {
        // Neither file exists in the exercise folder, so neither decoder yields a bitmap.
        for (MediaType type : MediaType.values()) {
            String missingFile = "missing-" + type + ".bin";
            long id = insertExercise("Plank", null, missingFile, type);

            ExerciseEditViewModel viewModel = new ExerciseEditViewModel(container, id);
            getOrAwaitValue(viewModel.getExercise());

            assertPreviewText(missingFile, getOrAwaitValue(viewModel.getPreview()));
        }
    }

    @Test
    public void constructorLeavesStateEmptyWhenTheExerciseNoLongerExists() throws Exception {
        long id = insertExercise("Gone", null, null, null);
        container.exerciseRepository.delete(container.exerciseRepository.getById(id));

        ExerciseEditViewModel viewModel = new ExerciseEditViewModel(container, id);
        awaitExecutorIdle();

        assertNull(viewModel.getExercise().getValue());
        assertNull(viewModel.getPreview().getValue());
    }

    @Test
    public void saveInsertsANewExercise() throws InterruptedException {
        ExerciseEditViewModel viewModel = new ExerciseEditViewModel(container, 0);

        assertEquals(Boolean.TRUE, getOrAwaitValue(viewModel.save("Lunge", "Alternate legs")));

        List<Exercise> all = container.exerciseRepository.getAll();
        assertEquals(1, all.size());
        assertEquals("Lunge", all.get(0).title);
        assertEquals("Alternate legs", all.get(0).description);
        assertNull(all.get(0).mediaFile);
        assertNull(all.get(0).mediaType);
    }

    @Test
    public void saveUpdatesAnExistingExerciseAndKeepsItsMedia() throws InterruptedException {
        long id = insertExercise("Squat", "Old", "squat.jpg", MediaType.IMAGE);
        ExerciseEditViewModel viewModel = new ExerciseEditViewModel(container, id);
        // save() reads the media fields the load callback set, so wait for it first.
        getOrAwaitValue(viewModel.getExercise());

        assertEquals(Boolean.TRUE, getOrAwaitValue(viewModel.save("Back Squat", "New")));

        Exercise saved = container.exerciseRepository.getById(id);
        assertEquals("Back Squat", saved.title);
        assertEquals("New", saved.description);
        assertEquals("squat.jpg", saved.mediaFile);
        assertEquals(MediaType.IMAGE, saved.mediaType);
        assertEquals(1, container.exerciseRepository.getAll().size());
    }

    @Test
    public void saveReportsFalseWhenTheExerciseWasDeletedMeanwhile()
            throws InterruptedException {
        long id = insertExercise("Squat", null, null, null);
        ExerciseEditViewModel viewModel = new ExerciseEditViewModel(container, id);
        getOrAwaitValue(viewModel.getExercise());
        // Out of band: deleted elsewhere while this screen was open.
        container.exerciseRepository.delete(container.exerciseRepository.getById(id));

        assertEquals(Boolean.FALSE, getOrAwaitValue(viewModel.save("Back Squat", null)));

        // An update must not resurrect the row as an insert.
        assertTrue(container.exerciseRepository.getAll().isEmpty());
    }

    @Test
    public void deleteRemovesTheExercise() throws InterruptedException {
        long keptId = insertExercise("Kept", null, null, null);
        long id = insertExercise("Squat", null, null, null);
        ExerciseEditViewModel viewModel = new ExerciseEditViewModel(container, id);
        getOrAwaitValue(viewModel.getExercise());

        assertEquals(Boolean.TRUE, getOrAwaitValue(viewModel.delete()));

        assertNull(container.exerciseRepository.getById(id));
        assertNotNull(container.exerciseRepository.getById(keptId));
    }

    @Test
    public void unsupportedMediaIsReportedOnceAndNotRedelivered() throws InterruptedException {
        ExerciseEditViewModel viewModel = new ExerciseEditViewModel(container, 0);

        viewModel.onMediaSelected(UNTYPEABLE_URI);

        Event<String> event = getOrAwaitValue(viewModel.getMediaError());
        assertEquals(UNSUPPORTED_TYPE_ERROR, event.getContentIfNotHandled());
        assertNull(event.getContentIfNotHandled());

        // A re-registered observer (e.g. after rotation) gets the same, already-handled
        // event rather than the message again.
        Event<String> redelivered = getOrAwaitValue(viewModel.getMediaError());
        assertSame(event, redelivered);
        assertNull(redelivered.getContentIfNotHandled());
    }

    @Test
    public void eachFailedSelectionRaisesAFreshEvent() throws Exception {
        ExerciseEditViewModel viewModel = new ExerciseEditViewModel(container, 0);
        viewModel.onMediaSelected(UNTYPEABLE_URI);
        Event<String> first = getOrAwaitValue(viewModel.getMediaError());
        assertEquals(UNSUPPORTED_TYPE_ERROR, first.getContentIfNotHandled());

        // A file:// Uri has no content-provider type either.
        viewModel.onMediaSelected(Uri.parse("file:///nonexistent/clip.mp4"));
        awaitExecutorIdle();

        Event<String> second = viewModel.getMediaError().getValue();
        assertNotNull(second);
        assertNotSame(first, second);
        assertEquals(UNSUPPORTED_TYPE_ERROR, second.getContentIfNotHandled());
    }

    @Test
    public void failedSelectionKeepsThePreviouslySelectedMedia() throws Exception {
        long id = insertExercise("Squat", null, "squat.jpg", MediaType.IMAGE);
        ExerciseEditViewModel viewModel = new ExerciseEditViewModel(container, id);
        getOrAwaitValue(viewModel.getExercise());
        ExerciseEditViewModel.Preview previewBefore = getOrAwaitValue(viewModel.getPreview());

        viewModel.onMediaSelected(UNTYPEABLE_URI);
        assertNotNull(getOrAwaitValue(viewModel.getMediaError()));

        assertSame(previewBefore, viewModel.getPreview().getValue());
        assertEquals(Boolean.TRUE, getOrAwaitValue(viewModel.save("Squat", null)));
        Exercise saved = container.exerciseRepository.getById(id);
        assertEquals("squat.jpg", saved.mediaFile);
        assertEquals(MediaType.IMAGE, saved.mediaType);
    }

    @Test
    public void removingMediaClearsThePreviewAndIsPersistedOnSave() throws InterruptedException {
        long id = insertExercise("Squat", null, "squat.jpg", MediaType.IMAGE);
        ExerciseEditViewModel viewModel = new ExerciseEditViewModel(container, id);
        getOrAwaitValue(viewModel.getExercise());
        assertTrue(viewModel.hasMedia());

        viewModel.removeMedia();

        assertFalse(viewModel.hasMedia());
        assertPreviewText(NO_MEDIA_TEXT, viewModel.getPreview().getValue());
        assertEquals(Boolean.TRUE, getOrAwaitValue(viewModel.save("Squat", null)));
        Exercise saved = container.exerciseRepository.getById(id);
        assertNull(saved.mediaFile);
        assertNull(saved.mediaType);
    }

    private long insertExercise(String title, String description, String mediaFile,
                                MediaType mediaType) {
        Exercise exercise = new Exercise();
        exercise.title = title;
        exercise.description = description;
        exercise.mediaFile = mediaFile;
        exercise.mediaType = mediaType;
        return container.exerciseRepository.save(exercise);
    }

    /** ioExecutor is single-threaded: once this barrier task has run, every task queued
     *  before it - and, under InstantTaskExecutorRule, its LiveData callback - is done. */
    private void awaitExecutorIdle() throws Exception {
        container.ioExecutor.submit(() -> { }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static void assertPreviewText(String expectedText,
                                          ExerciseEditViewModel.Preview preview) {
        assertNotNull(preview);
        assertNull(preview.bitmap);
        assertEquals(expectedText, preview.text);
    }
}
