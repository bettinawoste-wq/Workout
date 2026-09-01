package com.wob.workout.ui.exercise;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.wob.workout.AppContainer;
import com.wob.workout.WorkoutApplication;
import com.wob.workout.WorkoutViewModelFactory;
import com.wob.workout.databinding.FragmentExerciseEditBinding;

public class ExerciseEditFragment extends Fragment {

    private static final String ARG_EXERCISE_ID = "exercise_id";

    private FragmentExerciseEditBinding binding;
    private ExerciseEditViewModel viewModel;

    private final ActivityResultLauncher<PickVisualMediaRequest> mediaPicker =
            registerForActivityResult(
                    new ActivityResultContracts.PickVisualMedia(),
                    uri -> {
                        if (uri != null) viewModel.onMediaSelected(uri);
                    });

    public static ExerciseEditFragment edit(long exerciseId) {
        ExerciseEditFragment fragment = new ExerciseEditFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_EXERCISE_ID, exerciseId);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentExerciseEditBinding.inflate(inflater, container, false);

        long exerciseId = getArguments() != null
                ? getArguments().getLong(ARG_EXERCISE_ID, -1)
                : -1;

        AppContainer appContainer =
                ((WorkoutApplication) requireActivity().getApplication()).getAppContainer();
        viewModel = new ViewModelProvider(this, new WorkoutViewModelFactory(appContainer, exerciseId))
                .get(ExerciseEditViewModel.class);

        binding.btnDelete.setVisibility(viewModel.isNew() ? View.GONE : View.VISIBLE);

        // Same reasoning as WorkoutEditFragment: only seed on first arrival — a rotation has
        // already restored whatever the user typed via Android's own view-state saving.
        viewModel.getExercise().observe(getViewLifecycleOwner(), exercise -> {
            if (exercise == null) return;
            if (TextUtils.isEmpty(binding.edtTitle.getText())) {
                binding.edtTitle.setText(exercise.title);
            }
            if (TextUtils.isEmpty(binding.edtDescription.getText())) {
                binding.edtDescription.setText(exercise.description);
            }
        });

        viewModel.getPreview().observe(getViewLifecycleOwner(), this::showPreview);

        viewModel.getMediaError().observe(getViewLifecycleOwner(), event -> {
            String message = event != null ? event.getContentIfNotHandled() : null;
            if (message != null) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
            }
        });

        binding.btnSave.setOnClickListener(v -> save());
        binding.btnDelete.setOnClickListener(v -> confirmDelete());
        binding.btnSelectMedia.setOnClickListener(v ->
                mediaPicker.launch(
                        new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE)
                                .build()));

        return binding.getRoot();
    }

    private void showPreview(ExerciseEditViewModel.Preview preview) {
        if (preview == null) return;

        if (preview.bitmap != null) {
            binding.imgMediaPreview.setImageBitmap(preview.bitmap);
            binding.imgMediaPreview.setVisibility(View.VISIBLE);
            binding.txtMedia.setVisibility(View.GONE);
        } else {
            binding.imgMediaPreview.setVisibility(View.GONE);
            binding.txtMedia.setVisibility(View.VISIBLE);
            binding.txtMedia.setText(preview.text);
        }
    }

    private void save() {
        String title = binding.edtTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Snackbar.make(binding.getRoot(), "Titel erforderlich", Snackbar.LENGTH_SHORT).show();
            return;
        }

        String description = binding.edtDescription.getText().toString().trim();

        // Disable immediately so a second tap before the async write returns can't insert a
        // duplicate exercise.
        binding.btnSave.setEnabled(false);

        viewModel.save(title, description).observe(getViewLifecycleOwner(), saved -> {
            if (saved != null && saved) {
                requireActivity().getSupportFragmentManager().popBackStack();
            } else if (binding != null) {
                binding.btnSave.setEnabled(true);
                Snackbar.make(binding.getRoot(), "Speichern fehlgeschlagen", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDelete() {
        if (viewModel.isNew()) return;

        String title = binding.edtTitle.getText().toString();
        if (title.isEmpty() && viewModel.getExercise().getValue() != null) {
            title = viewModel.getExercise().getValue().title;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Übung löschen?")
                .setMessage("\"" + title + "\" wird endgültig gelöscht.")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Löschen", (dialog, which) ->
                        viewModel.delete().observe(getViewLifecycleOwner(), deleted -> {
                            if (deleted != null && deleted) {
                                requireActivity().getSupportFragmentManager().popBackStack();
                            }
                        }))
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
