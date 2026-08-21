package com.wob.workout.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.wob.workout.data.repository.WorkoutHistoryRepository;
import com.wob.workout.databinding.FragmentHistoryListBinding;

import java.util.List;

public class HistoryListFragment extends Fragment {

    private FragmentHistoryListBinding binding;
    private HistoryAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHistoryListBinding.inflate(inflater, container, false);

        adapter = new HistoryAdapter();

        binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerHistory.setAdapter(adapter);

        loadHistory();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistory();
    }

    private void loadHistory() {

        List<WorkoutSessionSummary> items =
                new WorkoutHistoryRepository(requireContext())
                        .getHistory();

        adapter.setData(items);

        binding.txtEmpty.setVisibility(
                items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
