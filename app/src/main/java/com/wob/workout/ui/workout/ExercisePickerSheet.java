package com.wob.workout.ui.workout;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.wob.workout.R;
import com.wob.workout.WorkoutApplication;
import com.wob.workout.databinding.SheetExercisePickerBinding;

/**
 * Picks several exercises at once for the workout editor it is shown from, which must be
 * its parent fragment ({@link #show(WorkoutEditFragment)}): it shares the editor's
 * {@link WorkoutEditViewModel}. The ticks survive rotation.
 */
public class ExercisePickerSheet extends BottomSheetDialogFragment {

    private static final String TAG = "exercise_picker";
    private static final String STATE_PICKED_IDS = "picked_ids";

    private SheetExercisePickerBinding binding;
    private ExercisePickerAdapter adapter;

    static void show(WorkoutEditFragment editor) {
        if (editor.getChildFragmentManager().findFragmentByTag(TAG) != null) return;
        new ExercisePickerSheet().show(editor.getChildFragmentManager(), TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        // Fully open right away: the half-open state would hide the button.
        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        return dialog;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = SheetExercisePickerBinding.inflate(inflater, container, false);

        // Created in the editor's onCreate, which always runs before this, its child.
        WorkoutEditViewModel viewModel = new ViewModelProvider(requireParentFragment())
                .get(WorkoutEditViewModel.class);

        adapter = new ExercisePickerAdapter(
                ((WorkoutApplication) requireActivity().getApplication()).getAppContainer().thumbnails,
                this::showPickedCount);
        binding.listExercises.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listExercises.setAdapter(adapter);
        long[] restored = savedInstanceState != null
                ? savedInstanceState.getLongArray(STATE_PICKED_IDS)
                : null;
        adapter.setPickedIds(restored != null ? restored : new long[0]);

        viewModel.loadAllExercises().observe(getViewLifecycleOwner(), exercises -> {
            if (exercises != null) adapter.setData(exercises);
        });

        binding.btnAdd.setOnClickListener(v -> {
            // Once only: a quick second tap would otherwise land before the dismissal.
            binding.btnAdd.setOnClickListener(null);
            viewModel.addExercises(adapter.getPicked());
            dismiss();
        });

        return binding.getRoot();
    }

    private void showPickedCount(int count) {
        binding.btnAdd.setText(getString(R.string.anzahl_hinzufuegen, count));
        binding.btnAdd.setEnabled(count > 0);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (adapter != null) outState.putLongArray(STATE_PICKED_IDS, adapter.getPickedIds());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
