package com.wob.workout.player;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.wob.workout.AppContainer;
import com.wob.workout.R;
import com.wob.workout.WorkoutApplication;
import com.wob.workout.WorkoutViewModelFactory;
import com.wob.workout.databinding.FragmentPlayerBinding;
import com.wob.workout.ui.Navigator;
import com.wob.workout.ui.ScreenToolbar;

/** Shows the {@link PlayerScreen} its view model hands over, plays the sounds it sends and
 *  vibrates for the ones it sends to vibrate. */
public class PlayerFragment extends Fragment {

    private static final String ARG_WORKOUT_ID = "workout_id";
    private static final String ARG_WORKOUT_NAME = "workout_name";

    private FragmentPlayerBinding binding;
    private PlayerViewModel viewModel;
    private AudioManager audioManager;
    private PlayerVibrator vibrator;
    private MediaController mediaController;
    /** The scene whose picture is on screen, so that a tick doesn't load it again. */
    private int shownScene = -1;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        binding = FragmentPlayerBinding.inflate(inflater, container, false);
        shownScene = -1;
        ScreenToolbar.setUp(this, binding.toolbar);
        String workoutName = getArguments() != null
                ? getArguments().getString(ARG_WORKOUT_NAME)
                : null;
        binding.toolbar.setTitle(workoutName != null ? workoutName : "Workout");
        binding.toolbar.inflateMenu(R.menu.menu_player);
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), confirmAbortOnBack);

        long workoutId = getArguments() != null
                ? getArguments().getLong(ARG_WORKOUT_ID, -1)
                : -1;

        AppContainer appContainer =
                ((WorkoutApplication) requireActivity().getApplication()).getAppContainer();
        viewModel = new ViewModelProvider(
                this, new WorkoutViewModelFactory(appContainer, workoutId))
                .get(PlayerViewModel.class);

        audioManager = new AudioManager(requireContext());
        vibrator = new PlayerVibrator(requireContext());
        mediaController = new MediaController(this, binding, appContainer.mediaExecutor);

        binding.btnPause.setOnClickListener(v -> viewModel.togglePause());
        // A larger target than the pause button, for a tap in the middle of an exercise.
        binding.txtTimer.setOnClickListener(v -> viewModel.togglePause());
        binding.btnNext.setOnClickListener(v -> viewModel.next());
        binding.btnPrevious.setOnClickListener(v -> viewModel.previous());
        binding.btnCancel.setOnClickListener(v -> confirmCancelWorkout());
        binding.btnDone.setOnClickListener(v -> Navigator.of(this).close());
        binding.btnAgain.setOnClickListener(v -> startAgain(workoutId, workoutName));
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() != R.id.action_sound) return false;
            viewModel.toggleSound();
            return true;
        });

        viewModel.isSoundOn().observe(getViewLifecycleOwner(), this::showSound);
        viewModel.getScreen().observe(getViewLifecycleOwner(), this::show);
        viewModel.getNothingToPlay().observe(getViewLifecycleOwner(), event -> {
            if (event.getContentIfNotHandled() == null) return;
            Toast.makeText(requireContext(), "Dieses Workout hat noch keine Übungen",
                    Toast.LENGTH_LONG).show();
            Navigator.of(this).close();
        });
        viewModel.getSound().observe(getViewLifecycleOwner(), event -> {
            PlayerSound due = event.getContentIfNotHandled();
            if (due != null) audioManager.play(due);
        });
        viewModel.getVibration().observe(getViewLifecycleOwner(), event -> {
            PlayerSound due = event.getContentIfNotHandled();
            if (due != null) vibrator.vibrate(due);
        });

        return binding.getRoot();
    }

    private void confirmCancelWorkout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Workout abbrechen?")
                .setMessage("Dein Fortschritt wird als abgebrochen im Verlauf gespeichert.")
                .setNegativeButton("Weiter", null)
                .setPositiveButton("Abbrechen", (dialog, which) ->
                        Navigator.of(this).close())
                .show();
    }

    /** Runs the same workout once more, as a new workout session, in place of this player,
     *  so back still leads to where the player was opened. */
    private void startAgain(long workoutId, String workoutName) {
        Navigator.of(this).replace(create(workoutId, workoutName));
    }

    private void show(PlayerScreen screen) {
        binding.txtProgress.setText(screen.progressText);
        binding.txtTimer.setText(screen.timerText);
        binding.txtTimer.setClickable(screen.timerTappable);

        int previewVisibility = screen.mediaLabel != null ? View.VISIBLE : View.GONE;
        binding.mediaScrim.setVisibility(previewVisibility);
        binding.mediaLabels.setVisibility(previewVisibility);
        binding.txtMediaLabel.setText(screen.mediaLabel);
        binding.txtMediaTitle.setText(screen.mediaTitle);

        binding.txtTitle.setText(screen.title);
        binding.txtDescription.setText(screen.description);
        binding.txtDescription.setGravity(
                screen.descriptionCentered ? Gravity.CENTER_HORIZONTAL : Gravity.START);

        binding.btnPrevious.setEnabled(screen.previousEnabled);
        binding.btnNext.setEnabled(screen.nextEnabled);
        binding.btnPause.setIconResource(
                screen.pauseButtonShowsPause ? R.drawable.ic_pause : R.drawable.ic_play);
        binding.btnPause.setContentDescription(screen.pauseButtonLabel);

        binding.progressWorkout.setProgressCompat(screen.workoutPermilleDone, true);
        binding.txtWorkoutRemaining.setText(screen.workoutRemainingText);

        int runningVisibility = screen.finished ? View.GONE : View.VISIBLE;
        binding.progressWorkout.setVisibility(runningVisibility);
        binding.rowWorkoutProgress.setVisibility(runningVisibility);
        binding.rowTransport.setVisibility(runningVisibility);
        binding.btnCancel.setVisibility(runningVisibility);
        binding.rowFinished.setVisibility(screen.finished ? View.VISIBLE : View.GONE);
        confirmAbortOnBack.setEnabled(!screen.finished);

        if (screen.scene != shownScene) {
            shownScene = screen.scene;
            if (screen.exercise != null) {
                mediaController.show(screen.exercise);
            } else {
                mediaController.hide();
            }
        }
    }

    /** The speaker shows the current state; its label says what a tap does. */
    private void showSound(boolean on) {
        MenuItem item = binding.toolbar.getMenu().findItem(R.id.action_sound);
        item.setIcon(on ? R.drawable.ic_volume_up : R.drawable.ic_volume_off);
        item.setTitle(on ? R.string.ton_ausschalten : R.string.ton_einschalten);
    }

    @Override
    public void onDestroyView() {
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
