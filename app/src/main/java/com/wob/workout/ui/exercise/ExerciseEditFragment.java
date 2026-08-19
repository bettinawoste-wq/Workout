package com.wob.workout.ui.exercise;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.wob.workout.data.entity.MediaType;
import com.wob.workout.databinding.FragmentExerciseEditBinding;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.repository.ExerciseRepository;
import com.wob.workout.media.MediaStorageManager;

public class ExerciseEditFragment extends Fragment {

    private static final String ARG_EXERCISE_ID = "exercise_id";

    private FragmentExerciseEditBinding binding;
    private ExerciseRepository repository;private String selectedMediaFile;
    private MediaType selectedMediaType;
    private Exercise exercise;

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
        repository = new ExerciseRepository(requireContext());

        loadExercise();

        binding.btnSave.setOnClickListener(v -> save());
        binding.btnDelete.setOnClickListener(v -> delete());
        binding.btnSelectMedia.setOnClickListener(v ->
                mediaPicker.launch(
                        new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE)
                                .build()));

        return binding.getRoot();
    }

    private void loadExercise() {
        long exerciseId = getArguments() != null
                ? getArguments().getLong(ARG_EXERCISE_ID, -1)
                : -1;
        if (exerciseId == -1) {
            binding.btnDelete.setVisibility(View.GONE);
            return;
        }

        exercise = repository.getById(exerciseId);
        if (exercise == null) {
            return;
        }
        binding.edtTitle.setText(exercise.title);
        binding.edtDescription.setText(exercise.description);
        selectedMediaFile = exercise.mediaFile;
        selectedMediaType = exercise.mediaType;
        if (selectedMediaFile != null) {
            binding.txtMedia.setText(selectedMediaFile);
        }
    }

    private void save() {
        String title = binding.edtTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Snackbar.make(binding.getRoot(), "Titel erforderlich", Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (exercise == null) {
            exercise = new Exercise();
        }
        exercise.title = title;
        exercise.description = binding.edtDescription.getText().toString().trim();
        exercise.mediaFile = selectedMediaFile;
        exercise.mediaType = selectedMediaType;
        if (exercise.id == 0) {
            repository.save(exercise);
        } else {
            repository.update(exercise);
        }
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private final ActivityResultLauncher<PickVisualMediaRequest> mediaPicker =
            registerForActivityResult(
                    new ActivityResultContracts.PickVisualMedia(),
                    this::onMediaSelected);

    private void delete() {
        if (exercise == null) {
            return;
        }
        repository.delete(exercise);
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void onMediaSelected(Uri uri) {
        if (uri == null) return;

        try {
            String mimeType = requireContext().getContentResolver().getType(uri);

            selectedMediaFile = MediaStorageManager.copyToExerciseFolder(requireContext(), uri);

            if (mimeType != null && mimeType.startsWith("image/")) {
                selectedMediaType = MediaType.IMAGE;
            } else if (mimeType != null && mimeType.startsWith("video/")) {
                selectedMediaType = MediaType.VIDEO;
            } else {
                Snackbar.make(binding.getRoot(), "Dateityp nicht unterstützt", Snackbar.LENGTH_SHORT).show();
                return;
            }

            binding.txtMedia.setText(selectedMediaFile);

        } catch (Exception e) {
            Snackbar.make(binding.getRoot(), "Medium konnte nicht gespeichert werden", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}