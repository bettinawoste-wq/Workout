package com.wob.workout.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.wob.workout.AppContainer;
import com.wob.workout.R;
import com.wob.workout.WorkoutApplication;
import com.wob.workout.WorkoutViewModelFactory;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.databinding.FragmentHomeBinding;
import com.wob.workout.player.PlayerFragment;
import com.wob.workout.ui.exercise.ExerciseListFragment;
import com.wob.workout.ui.history.HistoryListFragment;
import com.wob.workout.ui.workout.WorkoutAdapter;
import com.wob.workout.ui.workout.WorkoutEditFragment;
import com.wob.workout.ui.workout.WorkoutListFragment;

public class HomeFragment extends Fragment implements WorkoutAdapter.Listener {

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

        AppContainer appContainer =
                ((WorkoutApplication) requireActivity().getApplication()).getAppContainer();
        HomeViewModel viewModel =
                new ViewModelProvider(this, new WorkoutViewModelFactory(appContainer))
                        .get(HomeViewModel.class);

        ViewCompat.setAccessibilityHeading(binding.txtWorkoutsHeading, true);
        WorkoutAdapter adapter = new WorkoutAdapter(this, appContainer.thumbnails);
        binding.recyclerRecentWorkouts.setLayoutManager(
                new LinearLayoutManager(requireContext()));
        binding.recyclerRecentWorkouts.setAdapter(adapter);
        EmptyState.setUp(binding.emptyState, R.drawable.ic_play, R.string.noch_keine_workouts,
                R.string.workout_anlegen, v -> open(WorkoutEditFragment.create()));
        viewModel.getRecentWorkouts().observe(getViewLifecycleOwner(), workouts -> {
            adapter.setData(workouts);
            binding.emptyState.getRoot()
                    .setVisibility(workouts.isEmpty() ? View.VISIBLE : View.GONE);
            // "Alle" would only lead to the same empty list. Invisible rather than gone, so
            // the heading row keeps its height.
            binding.btnAllWorkouts.setVisibility(
                    workouts.isEmpty() ? View.INVISIBLE : View.VISIBLE);
        });

        binding.btnAllWorkouts.setOnClickListener(v -> open(new WorkoutListFragment()));
        binding.btnExercises.setOnClickListener(v -> open(new ExerciseListFragment()));
        binding.btnHistory.setOnClickListener(v -> open(new HistoryListFragment()));
        return binding.getRoot();
    }

    @Override
    public void onWorkoutClicked(Workout workout) {
        open(WorkoutEditFragment.edit(workout.id));
    }

    @Override
    public void onWorkoutStartClicked(Workout workout) {
        open(PlayerFragment.create(workout.id, workout.name));
    }

    private void open(Fragment screen) {
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main, screen)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
    }
}
