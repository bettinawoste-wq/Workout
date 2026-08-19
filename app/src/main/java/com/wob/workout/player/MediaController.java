package com.wob.workout.player;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;

import androidx.core.content.FileProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;

import com.wob.workout.data.entity.Exercise;
import com.wob.workout.databinding.FragmentPlayerBinding;
import com.wob.workout.media.MediaStorageManager;

import java.io.File;

public class MediaController {

    private final PlayerFragment fragment;
    private final FragmentPlayerBinding binding;
    private ExoPlayer exoPlayer;

    public MediaController(PlayerFragment fragment,
                           FragmentPlayerBinding binding) {
        this.fragment = fragment;
        this.binding = binding;
    }

    public void show(Exercise exercise) {
        release();

        binding.imgMedia.setVisibility(View.GONE);
        binding.videoPlayer.setVisibility(View.GONE);

        if (exercise.mediaFile == null || exercise.mediaFile.isEmpty()) return;
        String file = exercise.mediaFile.toLowerCase();

        if (file.endsWith(".mp4")
                || file.endsWith(".mov")
                || file.endsWith(".mkv")) {
            showVideo(exercise.mediaFile);
        } else {
            showImage(exercise.mediaFile);
        }
    }

    private void showImage(String fileName) {
        File file = new File(
                MediaStorageManager.getFullPath(
                        fragment.requireContext(),
                        fileName));

        if (!file.exists()) return;

        binding.imgMedia.setVisibility(View.VISIBLE);
        binding.imgMedia.setImageBitmap(
                BitmapFactory.decodeFile(file.getAbsolutePath()));
    }

    private void showVideo(String fileName) {
        File file = new File(
                MediaStorageManager.getFullPath(
                        fragment.requireContext(),
                        fileName));

        if (!file.exists()) return;

        binding.videoPlayer.setVisibility(View.VISIBLE);

        exoPlayer = new ExoPlayer.Builder(
                fragment.requireContext()).build();

        binding.videoPlayer.setPlayer(exoPlayer);

        Uri uri = Uri.fromFile(file);

        exoPlayer.setMediaItem(MediaItem.fromUri(uri));
        exoPlayer.prepare();
        exoPlayer.play();
    }

    public void release() {
        if (exoPlayer == null) return;
        exoPlayer.release();
        exoPlayer = null;
    }
}