package com.wob.workout.media;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;

import java.io.File;
import java.io.IOException;

public class MediaThumbnailLoader {

    /** Decoded pictures keep at least this many pixels on their longest edge, when the
     *  original has that many: enough for a full-width picture frame on current phones. */
    static final int MIN_DECODED_EDGE_PX = 1280;

    /** Blocks while decoding, so call it off the main thread. */
    public static Bitmap loadRotatedImage(File file) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);

        options.inSampleSize = sampleSizeFor(options.outWidth, options.outHeight);
        options.inJustDecodeBounds = false;

        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (bitmap == null) return null;

        int rotation = readExifRotationDegrees(file);
        if (rotation == 0) return bitmap;

        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);

        return Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /** The largest power of two that still leaves {@link #MIN_DECODED_EDGE_PX} pixels. */
    static int sampleSizeFor(int width, int height) {
        int longestEdge = Math.max(width, height);
        int sampleSize = 1;
        while (longestEdge / (sampleSize * 2) >= MIN_DECODED_EDGE_PX) {
            sampleSize *= 2;
        }
        return sampleSize;
    }

    public static Bitmap loadVideoFrame(File file) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(file.getAbsolutePath());
            return retriever.getFrameAtTime(0);
        } catch (Exception e) {
            return null;
        } finally {
            try {
                retriever.release();
            } catch (IOException ignored) {
            }
        }
    }

    private static int readExifRotationDegrees(File file) {
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (IOException e) {
            return 0;
        }
    }
}
