package com.wob.workout.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.wob.workout.AppContainer;
import com.wob.workout.WorkoutApplication;
import com.wob.workout.WorkoutViewModelFactory;
import com.wob.workout.ui.ScreenToolbar;
import com.wob.workout.databinding.FragmentHistoryDetailBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryDetailFragment extends Fragment {

    private static final String ARG_SESSION_ID = "session_id";

    private FragmentHistoryDetailBinding binding;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    public static HistoryDetailFragment forSession(long sessionId) {
        HistoryDetailFragment fragment = new HistoryDetailFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_SESSION_ID, sessionId);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHistoryDetailBinding.inflate(inflater, container, false);
        ScreenToolbar.setUp(this, binding.toolbar);

        long sessionId = getArguments() != null
                ? getArguments().getLong(ARG_SESSION_ID, -1)
                : -1;

        AppContainer appContainer =
                ((WorkoutApplication) requireActivity().getApplication()).getAppContainer();
        HistoryDetailViewModel viewModel =
                new ViewModelProvider(this, new WorkoutViewModelFactory(appContainer, sessionId))
                        .get(HistoryDetailViewModel.class);

        HistoryExerciseAdapter adapter = new HistoryExerciseAdapter();
        binding.recyclerExercises.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerExercises.setAdapter(adapter);

        viewModel.getDetail().observe(getViewLifecycleOwner(), detail -> {
            binding.toolbar.setTitle(
                    detail.session.workoutName != null ? detail.session.workoutName : "Workout");
            binding.txtInfo.setText(
                    dateFormat.format(new Date(detail.session.startedAt))
                            + " • " + (detail.session.completed ? "Abgeschlossen" : "Abgebrochen"));

            adapter.setData(detail.exercises);
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
