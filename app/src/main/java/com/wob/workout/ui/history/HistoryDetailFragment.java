package com.wob.workout.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.wob.workout.data.entity.WorkoutSession;
import com.wob.workout.data.repository.WorkoutHistoryRepository;
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

        long sessionId = getArguments() != null
                ? getArguments().getLong(ARG_SESSION_ID, -1)
                : -1;

        WorkoutHistoryRepository repository = new WorkoutHistoryRepository(requireContext());
        WorkoutSession session = repository.getSession(sessionId);

        HistoryExerciseAdapter adapter = new HistoryExerciseAdapter();
        binding.recyclerExercises.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerExercises.setAdapter(adapter);

        if (session != null) {
            binding.txtName.setText(
                    session.workoutName != null ? session.workoutName : "Workout");
            binding.txtInfo.setText(
                    dateFormat.format(new Date(session.startedAt))
                            + " • " + (session.completed ? "Abgeschlossen" : "Abgebrochen"));

            adapter.setData(repository.getExercises(sessionId));
        }

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
