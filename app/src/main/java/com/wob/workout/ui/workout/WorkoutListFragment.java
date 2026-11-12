package com.wob.workout.ui.workout;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.wob.workout.AppContainer;
import com.wob.workout.R;
import com.wob.workout.WorkoutApplication;
import com.wob.workout.WorkoutViewModelFactory;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.databinding.FragmentWorkoutListBinding;
import com.wob.workout.player.PlayerFragment;
import com.wob.workout.ui.EmptyState;
import com.wob.workout.ui.ScreenToolbar;

public class WorkoutListFragment extends Fragment implements WorkoutAdapter.Listener {

    private FragmentWorkoutListBinding binding;
    private WorkoutAdapter adapter;
    private WorkoutListViewModel viewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentWorkoutListBinding.inflate(inflater, container, false);
        ScreenToolbar.setUp(this, binding.toolbar);

        AppContainer appContainer =
                ((WorkoutApplication) requireActivity().getApplication()).getAppContainer();
        viewModel = new ViewModelProvider(this, new WorkoutViewModelFactory(appContainer))
                .get(WorkoutListViewModel.class);

        adapter = new WorkoutAdapter(this, appContainer.thumbnails);

        binding.recyclerWorkouts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerWorkouts.setAdapter(adapter);

        binding.btnAdd.setOnClickListener(v -> openNewWorkout());
        EmptyState.setUp(binding.emptyState, R.drawable.ic_play, R.string.noch_keine_workouts,
                R.string.workout_anlegen, v -> openNewWorkout());

        viewModel.getWorkouts().observe(getViewLifecycleOwner(), items -> {
            adapter.setData(items);
            EmptyState.show(binding.emptyState, items.isEmpty(), binding.btnAdd);
        });

        return binding.getRoot();
    }

    private void openNewWorkout() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main, WorkoutEditFragment.create())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onWorkoutClicked(Workout workout) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main, WorkoutEditFragment.edit(workout.id))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onWorkoutStartClicked(Workout workout) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main, PlayerFragment.create(workout.id, workout.name))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
