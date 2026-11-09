package com.wob.workout.player;

import android.net.Uri;
import android.view.View;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.wob.workout.R;
import com.wob.workout.data.LiveDataExecutor;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.databinding.FragmentPlayerBinding;
import com.wob.workout.media.Framing;
import com.wob.workout.media.MediaStorageManager;
import com.wob.workout.media.MediaThumbnailLoader;

import java.io.File;
import java.util.concurrent.ExecutorService;

public class MediaController {

    private final PlayerFragment fragment;
    private final FragmentPlayerBinding binding;
    private final ExecutorService decodeExecutor;
    private ExoPlayer exoPlayer;

    /** The photo on screen or being decoded, so that a phase change within one exercise
     *  doesn't decode it again. */
    private File shownImage;
    /** Identifies the newest request, so a slow decode can't overwrite a later picture. */
    private int latestRequest;

    public MediaController(PlayerFragment fragment,
                           FragmentPlayerBinding binding,
                           ExecutorService decodeExecutor) {
        this.fragment = fragment;
        this.binding = binding;
        this.decodeExecutor = decodeExecutor;
    }

    public void show(Exercise exercise) {
        release();
        binding.mediaFrame.setVisibility(View.VISIBLE);

        File file = existingMediaFile(exercise);
        if (file == null) {
            showPlaceholder();
            return;
        }

        String name = exercise.mediaFile.toLowerCase();
        if (name.endsWith(".mp4")
                || name.endsWith(".mov")
                || name.endsWith(".mkv")) {
            showVideo(file);
        } else {
            showImage(file, exercise.framing());
        }
    }

    /** Removes the picture frame altogether, for when there is no current exercise. */
    public void hide() {
        release();
        latestRequest++;
        shownImage = null;
        binding.mediaFrame.setVisibility(View.GONE);
    }

    private File existingMediaFile(Exercise exercise) {
        if (exercise.mediaFile == null || exercise.mediaFile.isEmpty()) return null;

        File file = new File(
                MediaStorageManager.getFullPath(
                        fragment.requireContext(),
                        exercise.mediaFile));

        return file.exists() ? file : null;
    }

    private void showPlaceholder() {
        latestRequest++;
        shownImage = null;
        setPlaceholderImage();
    }

    private void setPlaceholderImage() {
        binding.videoPlayer.setVisibility(View.GONE);
        binding.imgMedia.setVisibility(View.VISIBLE);
        binding.imgMedia.setFraming(null);
        binding.imgMedia.setImageResource(R.drawable.exercise_placeholder);
    }

    private void showImage(File file, Framing framing) {
        binding.videoPlayer.setVisibility(View.GONE);
        binding.imgMedia.setVisibility(View.VISIBLE);

        if (file.equals(shownImage)) return;
        shownImage = file;

        // The previous picture stays up until this one is decoded, so the frame doesn't flash.
        int request = ++latestRequest;
        LiveDataExecutor.runOnExecutorOnce(
                decodeExecutor,
                () -> MediaThumbnailLoader.loadRotatedImage(file),
                () -> request == latestRequest,
                bitmap -> {
                    if (bitmap == null) {
                        setPlaceholderImage();
                    } else {
                        binding.imgMedia.setFraming(framing);
                        binding.imgMedia.setImageBitmap(bitmap);
                    }
                });
    }

    private void showVideo(File file) {
        latestRequest++;
        shownImage = null;

        binding.imgMedia.setVisibility(View.GONE);
        binding.videoPlayer.setVisibility(View.VISIBLE);

        exoPlayer = new ExoPlayer.Builder(
                fragment.requireContext()).build();

        binding.videoPlayer.setPlayer(exoPlayer);

        Uri uri = Uri.fromFile(file);

        exoPlayer.setMediaItem(MediaItem.fromUri(uri));
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        exoPlayer.prepare();
        exoPlayer.play();
    }

    public void release() {
        if (exoPlayer == null) return;
        exoPlayer.release();
        exoPlayer = null;
    }
}
