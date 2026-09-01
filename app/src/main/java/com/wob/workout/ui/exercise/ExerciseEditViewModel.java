package com.wob.workout.ui.exercise;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.wob.workout.AppContainer;
import com.wob.workout.data.Event;
import com.wob.workout.data.LiveDataExecutor;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.MediaType;
import com.wob.workout.media.MediaStorageManager;
import com.wob.workout.media.MediaThumbnailLoader;

import java.io.File;

public class ExerciseEditViewModel extends ViewModel {

    private final AppContainer container;
    private final long exerciseId;

    private final MutableLiveData<Exercise> exercise = new MutableLiveData<>();
    private final MutableLiveData<Preview> preview = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> mediaError = new MutableLiveData<>();

    private String selectedMediaFile;
    private MediaType selectedMediaType;

    public ExerciseEditViewModel(AppContainer container, long exerciseId) {
        this.container = container;
        this.exerciseId = exerciseId;

        if (isNew()) {
            preview.setValue(computePreview(container.appContext, null, null));
            return;
        }

        LiveDataExecutor.runOnExecutorOnce(container.ioExecutor, () -> {
            Exercise loaded = container.exerciseRepository.getById(exerciseId);
            if (loaded == null) return null;
            return new LoadResult(loaded, computePreview(container.appContext, loaded.mediaFile, loaded.mediaType));
        }, result -> {
            if (result == null) return;
            exercise.setValue(result.exercise);
            selectedMediaFile = result.exercise.mediaFile;
            selectedMediaType = result.exercise.mediaType;
            preview.setValue(result.preview);
        });
    }

    public boolean isNew() {
        return exerciseId <= 0;
    }

    public LiveData<Exercise> getExercise() {
        return exercise;
    }

    public LiveData<Preview> getPreview() {
        return preview;
    }

    public LiveData<Event<String>> getMediaError() {
        return mediaError;
    }

    public LiveData<Boolean> save(String title, String description) {
        Exercise target = new Exercise();
        target.id = isNew() ? 0 : exerciseId;
        target.title = title;
        target.description = description;
        target.mediaFile = selectedMediaFile;
        target.mediaType = selectedMediaType;
        boolean isInsert = target.id == 0;

        return LiveDataExecutor.runOnExecutor(container.ioExecutor, () -> {
            if (isInsert) {
                container.exerciseRepository.save(target);
                return true;
            }
            return container.exerciseRepository.update(target);
        });
    }

    public LiveData<Boolean> delete() {
        return LiveDataExecutor.runOnExecutor(container.ioExecutor, () -> {
            Exercise target = new Exercise();
            target.id = exerciseId;
            container.exerciseRepository.delete(target);
            return true;
        });
    }

    public void onMediaSelected(Uri uri) {
        LiveDataExecutor.runOnExecutorOnce(container.ioExecutor, () -> {
            try {
                String mimeType = container.appContext.getContentResolver().getType(uri);

                MediaType mediaType;
                if (mimeType != null && mimeType.startsWith("image/")) {
                    mediaType = MediaType.IMAGE;
                } else if (mimeType != null && mimeType.startsWith("video/")) {
                    mediaType = MediaType.VIDEO;
                } else {
                    return MediaSelectionResult.error("Dateityp nicht unterstützt");
                }

                String mediaFile = MediaStorageManager.copyToExerciseFolder(container.appContext, uri);
                Preview loadedPreview = computePreview(container.appContext, mediaFile, mediaType);
                return MediaSelectionResult.success(mediaFile, mediaType, loadedPreview);
            } catch (Exception e) {
                return MediaSelectionResult.error("Medium konnte nicht gespeichert werden");
            }
        }, result -> {
            if (result.error != null) {
                mediaError.setValue(new Event<>(result.error));
                return;
            }
            // Only ever written here, on the main thread (this callback), even though it's
            // computed on the background executor above — the same discipline as
            // WorkoutEditViewModel's changeDuration()/reorder(): a field another observer
            // might read shouldn't be mutated from the background thread.
            selectedMediaFile = result.mediaFile;
            selectedMediaType = result.mediaType;
            preview.setValue(result.preview);
        });
    }

    private static Preview computePreview(Context appContext, String mediaFile, MediaType mediaType) {
        if (mediaFile == null || mediaType == null) {
            return new Preview(null, "Kein Medium ausgewählt");
        }

        File file = new File(MediaStorageManager.getFullPath(appContext, mediaFile));
        Bitmap bitmap = mediaType == MediaType.VIDEO
                ? MediaThumbnailLoader.loadVideoFrame(file)
                : MediaThumbnailLoader.loadRotatedImage(file);

        if (bitmap == null) {
            return new Preview(null, mediaFile);
        }
        return new Preview(bitmap, null);
    }

    /** Either a bitmap to show, or placeholder text to show instead — never both. */
    public static class Preview {
        public final Bitmap bitmap;
        public final String text;

        Preview(Bitmap bitmap, String text) {
            this.bitmap = bitmap;
            this.text = text;
        }
    }

    private static class LoadResult {
        final Exercise exercise;
        final Preview preview;

        LoadResult(Exercise exercise, Preview preview) {
            this.exercise = exercise;
            this.preview = preview;
        }
    }

    private static class MediaSelectionResult {
        final String mediaFile;
        final MediaType mediaType;
        final Preview preview;
        final String error;

        private MediaSelectionResult(String mediaFile, MediaType mediaType, Preview preview, String error) {
            this.mediaFile = mediaFile;
            this.mediaType = mediaType;
            this.preview = preview;
            this.error = error;
        }

        static MediaSelectionResult success(String mediaFile, MediaType mediaType, Preview preview) {
            return new MediaSelectionResult(mediaFile, mediaType, preview, null);
        }

        static MediaSelectionResult error(String message) {
            return new MediaSelectionResult(null, null, null, message);
        }
    }
}
