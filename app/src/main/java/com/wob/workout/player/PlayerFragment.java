package com.wob.workout.player;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.wob.workout.AppContainer;
import com.wob.workout.WorkoutApplication;
import com.wob.workout.WorkoutViewModelFactory;
import com.wob.workout.databinding.FragmentPlayerBinding;

public class PlayerFragment extends Fragment {

    private static final String ARG_WORKOUT_ID = "workout_id";

    private FragmentPlayerBinding binding;
    private PlayerViewModel viewModel;
    private AudioManager audioManager;
    private MediaController mediaController;
    private PlayerExerciseState currentState;

    public static PlayerFragment create(long workoutId) {
        PlayerFragment fragment = new PlayerFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_WORKOUT_ID, workoutId);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPlayerBinding.inflate(inflater, container, false);

        requireActivity().getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        long workoutId = getArguments() != null
                ? getArguments().getLong(ARG_WORKOUT_ID, -1)
                : -1;

        AppContainer appContainer =
                ((WorkoutApplication) requireActivity().getApplication()).getAppContainer();
        viewModel = new ViewModelProvider(this, new WorkoutViewModelFactory(appContainer, workoutId))
                .get(PlayerViewModel.class);

        audioManager = new AudioManager(requireContext());
        mediaController = new MediaController(this, binding);

        binding.btnPause.setOnClickListener(v -> viewModel.togglePause());
        binding.btnNext.setOnClickListener(v -> viewModel.next());
        binding.btnPrevious.setOnClickListener(v -> viewModel.previous());
        binding.btnCancel.setOnClickListener(v -> confirmCancelWorkout());

        viewModel.getExerciseState().observe(getViewLifecycleOwner(), this::showExerciseState);
        viewModel.getTick().observe(getViewLifecycleOwner(), this::showTick);

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

    private void showExerciseState(PlayerExerciseState state) {
        currentState = state;

        binding.txtProgress.setText(
                Math.min(state.index + 1, state.total) + " / " + state.total);

        switch (state.phase) {
            case READY:
                binding.txtState.setText("BEREIT");
                showExerciseInfo(state.exercise);
                break;
            case ACTIVE:
                binding.txtState.setText("AKTUELLE ÜBUNG");
                showExerciseInfo(state.exercise);
                binding.btnNext.setEnabled(true);
                binding.btnPrevious.setEnabled(true);
                binding.btnPause.setEnabled(true);
                binding.btnPause.setText("Pause");
                audioManager.playStart();
                break;
            case TRANSITION:
                if (state.exercise != null) {
                    binding.txtState.setText("NÄCHSTE ÜBUNG");
                    showExerciseInfo(state.exercise);
                    binding.btnNext.setEnabled(false);
                    binding.btnPrevious.setEnabled(false);
                    binding.btnPause.setEnabled(false);
                } else {
                    binding.txtState.setText("FERTIG");
                }
                break;
            case FINISHED:
                showFinished(state.total);
                break;
        }
    }

    private void showExerciseInfo(PlayerExercise exercise) {
        if (exercise == null) return;
        binding.txtTitle.setText(exercise.exercise.title);
        binding.txtDescription.setText(exercise.exercise.description);
        mediaController.show(exercise.exercise);
    }

    private void showFinished(int total) {
        binding.txtTitle.setText(total + " Übungen. Gut gemacht 💪");
        binding.txtDescription.setText("Workout abgeschlossen");
        binding.txtTimer.setText("00:00");

        binding.btnPause.setEnabled(false);
        binding.btnNext.setEnabled(false);
        binding.btnPrevious.setEnabled(false);

        binding.imgMedia.setVisibility(View.GONE);
        binding.videoPlayer.setVisibility(View.GONE);
    }

    private void showTick(PlayerTick tick) {
        if (tick.shouldBeep) audioManager.playBeep();

        if (currentState == null) return;

        if (currentState.phase == PlayerExerciseState.Phase.TRANSITION) {
            String label = currentState.exercise != null ? "Start in " : "Ende in ";
            binding.txtTimer.setText(label + ((tick.remainingMillis + 999) / 1000));
        } else if (currentState.phase == PlayerExerciseState.Phase.ACTIVE) {
            binding.txtTimer.setText(formatTime(tick.remainingMillis));
            binding.btnPause.setText(tick.running ? "Pause" : "Weiter");
        }
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void onDestroyView() {
        requireActivity().getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        if (audioManager != null) audioManager.release();
        super.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();
        mediaController.release();
    }
}
