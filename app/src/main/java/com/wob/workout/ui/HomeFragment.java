package com.wob.workout.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.wob.workout.R;
import com.wob.workout.databinding.FragmentHomeBinding;
import com.wob.workout.ui.exercise.ExerciseListFragment;
import com.wob.workout.ui.workout.WorkoutListFragment;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        binding =
                FragmentHomeBinding.inflate(
                        inflater,
                        container,
                        false);

        binding.btnExercises.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.main,
                            new ExerciseListFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.btnWorkouts.setOnClickListener(v ->
                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.main,
                                new WorkoutListFragment())
                        .addToBackStack(null)
                        .commit());
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
    }
}