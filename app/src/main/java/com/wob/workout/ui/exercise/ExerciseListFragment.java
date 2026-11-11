package com.wob.workout.ui.exercise;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.wob.workout.AppContainer;
import com.wob.workout.R;
import com.wob.workout.WorkoutApplication;
import com.wob.workout.WorkoutViewModelFactory;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.databinding.FragmentExerciseListBinding;
import com.wob.workout.ui.ScreenToolbar;

public class ExerciseListFragment extends Fragment {
    private FragmentExerciseListBinding binding;
    private ExerciseAdapter adapter;

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
        ScreenToolbar.setUp(this, binding.toolbar);

        AppContainer appContainer =
                ((WorkoutApplication) requireActivity().getApplication()).getAppContainer();
        ExerciseListViewModel viewModel =
                new ViewModelProvider(this, new WorkoutViewModelFactory(appContainer))
                        .get(ExerciseListViewModel.class);

        adapter = new ExerciseAdapter(this::openExercise, appContainer.thumbnails);
        binding.recyclerExercises.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerExercises.setAdapter(adapter);

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

        viewModel.getExercises().observe(getViewLifecycleOwner(), exercises -> {
            adapter.setData(exercises);
            binding.txtEmpty.setVisibility(exercises.isEmpty() ? View.VISIBLE : View.GONE);
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
