package com.wob.workout.player;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.wob.workout.AppContainer;
import com.wob.workout.R;
import com.wob.workout.WorkoutApplication;
import com.wob.workout.WorkoutViewModelFactory;
import com.wob.workout.databinding.FragmentPlayerBinding;
import com.wob.workout.ui.ScreenToolbar;

public class PlayerFragment extends Fragment {

    private static final String ARG_WORKOUT_ID = "workout_id";
    private static final String ARG_WORKOUT_NAME = "workout_name";

    private FragmentPlayerBinding binding;
    private PlayerViewModel viewModel;
    private AudioManager audioManager;
    private MediaController mediaController;

    /** While a workout runs, leaving it (back arrow or system back) aborts it, so ask first,
     *  just like "Workout abbrechen". Disabled once it's finished: then back just leaves. */
    private final OnBackPressedCallback confirmAbortOnBack = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            confirmCancelWorkout();
        }
    };

    public static PlayerFragment create(long workoutId, String workoutName) {
        PlayerFragment fragment = new PlayerFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_WORKOUT_ID, workoutId);
        args.putString(ARG_WORKOUT_NAME, workoutName);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPlayerBinding.inflate(inflater, container, false);
        ScreenToolbar.setUp(this, binding.toolbar);
        String workoutName = getArguments() != null
                ? getArguments().getString(ARG_WORKOUT_NAME)
                : null;
        binding.toolbar.setTitle(workoutName != null ? workoutName : "Workout");
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), confirmAbortOnBack);

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
        mediaController = new MediaController(this, binding, appContainer.mediaExecutor);

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
        binding.txtProgress.setText((state.index + 1) + " / " + state.total);

        switch (state.phase) {
            case READY:
                binding.txtState.setText("BEREIT");
                showPreviewLabel("ERSTE ÜBUNG", state.exercise);
                showExerciseInfo(state.exercise);
                setTransportEnabled(false);
                break;
            case ACTIVE:
                binding.txtState.setText("AKTUELLE ÜBUNG");
                showPreviewLabel(null, null);
                showExerciseInfo(state.exercise);
                setTransportEnabled(true);
                // Stepping past either end does nothing, so don't offer it.
                binding.btnPrevious.setEnabled(state.hasPrevious());
                binding.btnNext.setEnabled(state.hasNext());
                showPauseButton(true);
                if (viewModel.shouldPlayStartSound(state)) {
                    audioManager.playStart();
                }
                break;
            case TRANSITION:
                binding.txtState.setText("NÄCHSTE ÜBUNG");
                showPreviewLabel("NÄCHSTE ÜBUNG", state.exercise);
                showExerciseInfo(state.exercise);
                setTransportEnabled(false);
                break;
            case FINISHED:
                showFinished(state.summary);
                if (viewModel.shouldPlaySuccessSound(state)) {
                    audioManager.playSuccess();
                }
                break;
        }
    }

    /** Darkens the picture and names it while it only previews the coming exercise. */
    private void showPreviewLabel(String label, PlayerExercise exercise) {
        int visibility = label != null ? View.VISIBLE : View.GONE;
        binding.mediaScrim.setVisibility(visibility);
        binding.mediaLabels.setVisibility(visibility);
        binding.txtMediaLabel.setText(label);
        binding.txtMediaTitle.setText(exercise != null ? exercise.exercise.title : null);
    }

    private void showPauseButton(boolean running) {
        binding.btnPause.setIconResource(running ? R.drawable.ic_pause : R.drawable.ic_play);
        binding.btnPause.setContentDescription(running ? "Pause" : "Weiter");
    }

    private void setTransportEnabled(boolean enabled) {
        binding.btnNext.setEnabled(enabled);
        binding.btnPrevious.setEnabled(enabled);
        binding.btnPause.setEnabled(enabled);
    }

    private void showExerciseInfo(PlayerExercise exercise) {
        if (exercise == null) return;
        binding.txtTitle.setText(exercise.exercise.title);
        binding.txtDescription.setText(exercise.exercise.description);
        mediaController.show(exercise.exercise);
    }

    private void showFinished(PlayerWorkoutSummary summary) {
        binding.txtState.setText("GESAMTZEIT");
        binding.txtTimer.setText(formatTime(summary.activeSeconds * 1000L));
        binding.txtTitle.setText("Gut gemacht 💪");
        binding.txtDescription.setText(summary.resultText());

        binding.txtProgress.setVisibility(View.GONE);
        binding.rowTransport.setVisibility(View.GONE);
        binding.progressWorkout.setVisibility(View.GONE);
        binding.txtWorkoutRemaining.setVisibility(View.GONE);

        // The session is already completed, so "Workout abbrechen" and its "saved as aborted"
        // dialog no longer apply: turn the button into a plain way out.
        confirmAbortOnBack.setEnabled(false);
        binding.btnCancel.setText("Fertig");
        binding.btnCancel.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        mediaController.hide();
    }

    private void showTick(PlayerTick tick) {
        if (viewModel.shouldPlayBeep(tick)) audioManager.playBeep();

        binding.progressWorkout.setProgressCompat(tick.workoutPermilleDone(), true);
        binding.txtWorkoutRemaining.setText("Noch " + formatTime(tick.workoutRemainingMillis));

        switch (tick.phase) {
            case READY:
            case TRANSITION:
                binding.txtTimer.setText("Start in " + ((tick.remainingMillis + 999) / 1000));
                break;
            case ACTIVE:
                binding.txtTimer.setText(formatTime(tick.remainingMillis));
                showPauseButton(tick.running);
                break;
            case FINISHED:
                break;
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
