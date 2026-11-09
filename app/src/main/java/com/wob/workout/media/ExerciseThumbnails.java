package com.wob.workout.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import com.wob.workout.R;
import com.wob.workout.data.LiveDataExecutor;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.MediaType;

import java.io.File;
import java.util.concurrent.ExecutorService;

/**
 * Fills the small pictures in lists: decoded in the background and kept in memory, so that
 * scrolling stays smooth. Call from the main thread.
 */
public class ExerciseThumbnails {

    /** Generous for a small frame, so that a zoomed-in framing stays sharp. */
    private static final int THUMBNAIL_EDGE_PX = 480;
    private static final int CACHE_BYTES = 16 * 1024 * 1024;

    private final Context context;
    private final ExecutorService executor;
    private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(CACHE_BYTES) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getAllocationByteCount();
        }
    };

    public ExerciseThumbnails(Context context, ExecutorService executor) {
        this.context = context;
        this.executor = executor;
    }

    public void show(FramedImageView view, Exercise exercise) {
        String mediaFile = exercise.mediaFile;
        // Rows get recycled while scrolling: the tag tells a finished decode whether its view
        // is still meant to show that picture.
        view.setTag(mediaFile);

        if (mediaFile == null || mediaFile.isEmpty()) {
            showPlaceholder(view);
            return;
        }

        MediaType type = exercise.mediaType;
        Framing framing = type == MediaType.IMAGE ? exercise.framing() : null;

        Bitmap cached = cache.get(mediaFile);
        if (cached != null) {
            showBitmap(view, cached, framing);
            return;
        }

        view.setImageDrawable(null);
        LiveDataExecutor.runOnExecutorOnce(
                executor,
                () -> decodeAndCache(mediaFile, type),
                () -> mediaFile.equals(view.getTag()),
                bitmap -> {
                    if (bitmap == null) {
                        showPlaceholder(view);
                    } else {
                        showBitmap(view, bitmap, framing);
                    }
                });
    }

    private static void showPlaceholder(FramedImageView view) {
        view.setFraming(null);
        view.setImageResource(R.drawable.exercise_placeholder);
    }

    private static void showBitmap(FramedImageView view, Bitmap bitmap, Framing framing) {
        view.setFraming(framing);
        view.setImageBitmap(bitmap);
    }

    private Bitmap decodeAndCache(String mediaFile, MediaType type) {
        File file = new File(MediaStorageManager.getFullPath(context, mediaFile));
        Bitmap bitmap = type == MediaType.VIDEO
                ? smallVideoFrame(file)
                : MediaThumbnailLoader.loadRotatedImage(file, THUMBNAIL_EDGE_PX);

        if (bitmap != null) cache.put(mediaFile, bitmap);
        return bitmap;
    }

    private static Bitmap smallVideoFrame(File file) {
        Bitmap frame = MediaThumbnailLoader.loadVideoFrame(file);
        if (frame == null) return null;

        float scale = (float) THUMBNAIL_EDGE_PX / Math.max(frame.getWidth(), frame.getHeight());
        if (scale >= 1) return frame;

        return Bitmap.createScaledBitmap(
                frame,
                Math.round(frame.getWidth() * scale),
                Math.round(frame.getHeight() * scale),
                true);
    }
}
