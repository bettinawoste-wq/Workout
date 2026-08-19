package com.wob.workout.ui.workout;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.wob.workout.R;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.data.repository.*;
import com.wob.workout.databinding.FragmentWorkoutListBinding;
import com.wob.workout.player.PlayerFragment;

import java.util.List;

public class WorkoutListFragment extends Fragment implements WorkoutAdapter.Listener {

    private FragmentWorkoutListBinding binding;
    private WorkoutAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentWorkoutListBinding.inflate(inflater, container, false);

        adapter = new WorkoutAdapter(this);

        binding.recyclerWorkouts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerWorkouts.setAdapter(adapter);

        binding.btnAdd.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main, new WorkoutEditFragment())
                        .addToBackStack(null)
                        .commit());

        loadWorkouts();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadWorkouts();
    }

    private void loadWorkouts() {

        List<WorkoutListItem> items =
                new WorkoutSummaryRepository(requireContext())
                        .getAll();

        adapter.setData(items);

        binding.txtEmpty.setVisibility(
                items.isEmpty() ? View.VISIBLE : View.GONE);
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
                .replace(R.id.main, PlayerFragment.create(workout.id))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}