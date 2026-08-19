package com.wob.workout.media;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class MediaStorageManager {

    public static File getExerciseFolder(Context context) {
        File dir = new File(context.getFilesDir(), "exercises");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static String copyToExerciseFolder(Context context, Uri sourceUri) throws Exception {
        String extension = getExtension(context, sourceUri);
        String fileName = UUID.randomUUID() + extension;
        File targetFile = new File(getExerciseFolder(context), fileName);

        try ( InputStream input = context.getContentResolver().openInputStream(sourceUri);
              FileOutputStream output = new FileOutputStream(targetFile)
        ) {
            byte[] buffer = new byte[8192];
            int length;
            while (true) {
                assert input != null;
                if (!((length = input.read(buffer)) > 0)) break;
                output.write(buffer, 0, length);
            }
        }
        return fileName;
    }

    private static String getExtension(Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType == null) {
            return "";
        }
        String extension =
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        if (extension == null) {
            return "";
        }
        return "." + extension;
    }

    public static String getFullPath(Context context, String fileName) {
        return new File(getExerciseFolder(context), fileName).getAbsolutePath();
    }
}