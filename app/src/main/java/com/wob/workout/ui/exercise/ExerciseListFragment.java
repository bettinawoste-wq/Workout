package com.wob.workout.ui.exercise;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.repository.ExerciseRepository;
import com.wob.workout.R;
import com.wob.workout.databinding.FragmentExerciseListBinding;

import java.util.List;

public class ExerciseListFragment extends Fragment {
    private FragmentExerciseListBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding =
                FragmentExerciseListBinding.inflate(
                        inflater,
                        container,
                        false);
        binding.fabAddExercise.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.main,
                            new ExerciseEditFragment())
                    .addToBackStack(null)
                    .commit();
        });
        loadExercises();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadExercises();
    }
    private void loadExercises() {
        ExerciseRepository repository =
                new ExerciseRepository(
                        requireContext());
        List<Exercise> exercises =
                repository.getAll();
        ExerciseAdapter adapter =
                new ExerciseAdapter( this::openExercise);
        adapter.setData(exercises);
        binding.recyclerExercises.setLayoutManager(
                new LinearLayoutManager(
                        requireContext()));
        binding.recyclerExercises.setAdapter(
                adapter);
        binding.txtEmpty.setVisibility(
                exercises.isEmpty()
                        ? View.VISIBLE
                        : View.GONE);
    }

    private void openExercise(Exercise exercise) {
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.main,
                        ExerciseEditFragment.edit(
                                exercise.id))
                .addToBackStack(null)
                .commit();
    }
}