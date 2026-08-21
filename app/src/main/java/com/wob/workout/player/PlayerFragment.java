package com.wob.workout.player;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.wob.workout.R;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.data.repository.WorkoutHistoryRepository;
import com.wob.workout.data.repository.WorkoutRepository;
import com.wob.workout.databinding.FragmentPlayerBinding;

import java.util.List;

public class PlayerFragment extends Fragment {
    private int index = 0;
    private static final String ARG_WORKOUT_ID = "workout_id";
    private PlayerRepository repository;
    private List<PlayerExercise> exercises;
    private FragmentPlayerBinding binding;
    private android.os.CountDownTimer timer;
    private long timeLeft;
    private boolean running = false;
    private boolean inTransition = false;
    private android.os.CountDownTimer transitionTimer;
    private ExoPlayer exoPlayer;
    private long lastBeepSecond = -1;
    private AudioManager audioManager;
    private MediaController mediaController;
    // TODO  private WorkoutTimer timer;

    private WorkoutHistoryRepository historyRepository;
    private long sessionId = -1;
    private boolean sessionClosed = false;
    private boolean exerciseStarted = false;
    private long currentExerciseStartedAt;
    private long currentExercisePausedMillis;
    private long pauseStartedAt = 0;

    private final android.media.ToneGenerator toneGenerator =
            new android.media.ToneGenerator(
                    android.media.AudioManager.STREAM_MUSIC,
                    100);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        android.util.Log.d("MediaPlayer", "onCreateView");
        binding = FragmentPlayerBinding.inflate(inflater, container, false);

        requireActivity().getWindow().addFlags(
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        long workoutId = getArguments() != null
                ? getArguments().getLong(ARG_WORKOUT_ID, -1)
                : -1;

        repository = new PlayerRepository(requireContext());
        exercises = repository.getExercises(workoutId);

        historyRepository = new WorkoutHistoryRepository(requireContext());
        Workout workout = new WorkoutRepository(requireContext()).getById(workoutId);
        sessionId = historyRepository.startSession(
                workoutId,
                workout != null ? workout.name : null);

        audioManager = new AudioManager(requireContext());
        mediaController = new MediaController(this, binding);

        startInitialTransition();

        binding.btnPause.setOnClickListener(v -> toggle());
        binding.btnNext.setOnClickListener(v -> next());
        binding.btnPrevious.setOnClickListener(v -> previous());
        binding.btnCancel.setOnClickListener(v -> confirmCancelWorkout());

        return binding.getRoot();
    }

    private void confirmCancelWorkout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Workout abbrechen?")
                .setMessage("Dein Fortschritt wird als abgebrochen im Verlauf gespeichert.")
                .setNegativeButton("Weiter", null)
                .setPositiveButton("Abbrechen", (dialog, which) ->
                        requireActivity().getSupportFragmentManager().popBackStack())
                .show();
    }

    @Override
    public void onDestroyView() {
        requireActivity().getWindow().clearFlags(
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (timer != null) timer.cancel();
        if (transitionTimer != null) transitionTimer.cancel();

        if (sessionId >= 0 && !sessionClosed) {
            closeOutCurrentExercise();
            historyRepository.finishSession(sessionId, false);
            sessionClosed = true;
        }

        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        toneGenerator.release();
        if (audioManager != null)
            audioManager.release();
        super.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();
        mediaController.release();
    }

    private void showExercise() {
        android.util.Log.d("MediaPlayer", "showExercise index=" + index);
        binding.txtState.setText("AKTUELLE ÜBUNG");
        if (exercises == null || exercises.isEmpty()) return;

        if (timer != null) timer.cancel();
        PlayerExercise current = exercises.get(index);
        updateProgress();

        binding.btnNext.setEnabled(true);
        binding.btnPrevious.setEnabled(true);
        binding.btnPause.setEnabled(true);
        binding.txtTitle.setText(current.exercise.title);
        binding.txtDescription.setText(current.exercise.description);

        currentExerciseStartedAt = System.currentTimeMillis();
        currentExercisePausedMillis = 0;
        pauseStartedAt = 0;
        exerciseStarted = true;

        startTimer(current.durationSeconds);
        mediaController.show(current.exercise);
    }

    private void closeOutCurrentExercise() {
        if (!exerciseStarted || exercises == null || exercises.isEmpty()) return;

        long now = System.currentTimeMillis();
        long pausedMillis = currentExercisePausedMillis;
        if (pauseStartedAt != 0) {
            pausedMillis += now - pauseStartedAt;
            pauseStartedAt = 0;
        }

        long activeMillis = Math.max(0, now - currentExerciseStartedAt - pausedMillis);
        PlayerExercise current = exercises.get(index);

        historyRepository.recordExercise(
                sessionId,
                current.exercise.id,
                current.exercise.title,
                index,
                current.durationSeconds,
                (int) (activeMillis / 1000),
                currentExerciseStartedAt,
                now);

        exerciseStarted = false;
    }

    private void startTimer(int seconds) {
        timeLeft = seconds * 1000L;
        running = true;
        timer = new android.os.CountDownTimer(timeLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = millisUntilFinished;
                binding.txtTimer.setText(
                        formatTime(millisUntilFinished));
            }
            @Override
            public void onFinish() {
                binding.txtTimer.setText("0");
                nextAuto();
            }
        }.start();
    }

    private void startInitialTransition() {
        android.util.Log.d("MediaPlayer", "startInitialTransition");
        index = 0;
        lastBeepSecond = -1;
        PlayerExercise next = exercises.get(index);

        binding.txtState.setText("BEREIT");
        binding.txtTitle.setText(next.exercise.title);
        binding.txtDescription.setText(next.exercise.description);

        mediaController.show(next.exercise);

        transitionTimer = new android.os.CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                playCountdownBeep(millisUntilFinished);
                binding.txtTimer.setText(
                        "Start in " + ((millisUntilFinished + 999) / 1000));
            }
            @Override
            public void onFinish() {
                playStartSound();
                binding.txtState.setText("AKTIV");
                showExercise();
            }
        }.start();
    }

    private void startTransition() {
        lastBeepSecond = -1;
        binding.txtState.setText("BEREIT MACHEN");
        binding.txtState.setText("NÄCHSTE ÜBUNG");
        if (timer != null) timer.cancel();
        inTransition = true;
        PlayerExercise next = exercises.get(index);

        binding.txtTitle.setText(next.exercise.title);
        binding.txtDescription.setText(next.exercise.description);
        mediaController.show(next.exercise);
        binding.btnNext.setEnabled(false);
        binding.btnPrevious.setEnabled(false);
        binding.btnPause.setEnabled(false);

        transitionTimer = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                playCountdownBeep(millisUntilFinished);
                binding.txtTimer.setText(
                        "Start in " + (millisUntilFinished / 1000));
            }
            @Override
            public void onFinish() {
                playStartSound();
                inTransition = false;
                showExercise();
            }
        }.start();
    }

    private void startFinishTransition() {
        lastBeepSecond = -1;
        transitionTimer = new CountDownTimer(3000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                playCountdownBeep(millisUntilFinished);
                binding.txtState.setText("FERTIG");
                binding.txtTimer.setText(
                        "Ende in " + ((millisUntilFinished + 999) / 1000));
            }

            @Override
            public void onFinish() {
                finishWorkout();
            }
        }.start();
    }

    @OptIn(markerClass = UnstableApi.class)
    private void loadVideo(String fileName) {
        String fullPath =
                com.wob.workout.media.MediaStorageManager.getFullPath(
                        requireContext(),
                        fileName);

        java.io.File file = new java.io.File(fullPath);

        android.util.Log.d("MediaPlayer", "video path=" + fullPath);

        if (!file.exists()) {
            android.util.Log.e("MediaPlayer", "VIDEO NOT FOUND: " + fullPath);
            return;
        }

        binding.imgMedia.setVisibility(View.GONE);
        binding.videoPlayer.setVisibility(View.VISIBLE);

        exoPlayer = new ExoPlayer.Builder(requireContext()).build();

        binding.videoPlayer.setPlayer(exoPlayer);
        binding.videoPlayer.setUseController(false);
        binding.videoPlayer.setControllerAutoShow(false);

        android.net.Uri uri =
                android.net.Uri.fromFile(file);

        exoPlayer.setMediaItem(MediaItem.fromUri(uri));
        exoPlayer.setRepeatMode(androidx.media3.common.Player.REPEAT_MODE_ONE);

        exoPlayer.prepare();
        exoPlayer.play();
    }

    private void updateProgress() {
        binding.txtProgress.setText(
                (index + 1) + " / " + exercises.size());
    }

    private void playCountdownBeep(long millisUntilFinished) {

        long second = (millisUntilFinished + 999) / 1000;

        if (second <= 3 && second > 0 && second != lastBeepSecond) {

            lastBeepSecond = second;
            audioManager.playBeep();
        }
    }

    private void playStartSound() {
        audioManager.playStart();
    }

    private void finishWorkout() {

        if (timer != null) timer.cancel();

        closeOutCurrentExercise();
        historyRepository.finishSession(sessionId, true);
        sessionClosed = true;

        binding.txtState.setText("FERTIG");
        binding.txtTitle.setText(exercises.size() + " Übungen. Gut gemacht 💪");
        binding.txtDescription.setText("Workout abgeschlossen");
        binding.txtProgress.setText(
                exercises.size() + " / " + exercises.size());

        binding.txtTimer.setText("00:00");

        binding.btnPause.setEnabled(false);
        binding.btnNext.setEnabled(false);
        binding.btnPrevious.setEnabled(false);

        binding.imgMedia.setVisibility(View.GONE);
        binding.videoPlayer.setVisibility(View.GONE);
    }

    private void nextAuto() {
        closeOutCurrentExercise();
        if (index >= exercises.size() - 1) {
            startFinishTransition();
            return;
        }
        index++;
        startTransition();
    }

    public static PlayerFragment create(long workoutId) {
        PlayerFragment fragment = new PlayerFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_WORKOUT_ID, workoutId);

        fragment.setArguments(args);

        return fragment;
    }

    private void toggle() {
        if (timer == null) return;
        if (running) {
            timer.cancel();
            running = false;
            pauseStartedAt = System.currentTimeMillis();
            binding.btnPause.setText("Weiter");
        } else {

            if (pauseStartedAt != 0) {
                currentExercisePausedMillis += System.currentTimeMillis() - pauseStartedAt;
                pauseStartedAt = 0;
            }

            PlayerExercise current = exercises.get(index);

            timer = new android.os.CountDownTimer(timeLeft, 1000) {

                @Override
                public void onTick(long millisUntilFinished) {
                    timeLeft = millisUntilFinished;
                    playCountdownBeep(millisUntilFinished);
                    binding.txtTimer.setText(
                            formatTime(millisUntilFinished));
                }

                @Override
                public void onFinish() {
                    binding.txtTimer.setText("0");
                    nextAuto();
                }
            }.start();

            running = true;
            binding.btnPause.setText("Pause");
        }
    }

    private void next() {
        if (exercises == null || index >= exercises.size() - 1) return;
        closeOutCurrentExercise();
        index++;
        showExercise();
        playStartSound();
    }

    private void previous() {
        if (exercises == null || index <= 0) return;
        closeOutCurrentExercise();
        index--;
        showExercise();
        playStartSound();
    }

    private String formatTime(long millis) {

        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }
}