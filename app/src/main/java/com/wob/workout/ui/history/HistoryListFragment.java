package com.wob.workout.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.wob.workout.R;
import com.wob.workout.data.repository.WorkoutHistoryRepository;
import com.wob.workout.databinding.FragmentHistoryListBinding;

import java.util.List;

public class HistoryListFragment extends Fragment {

    private FragmentHistoryListBinding binding;
    private HistoryAdapter adapter;
    private WorkoutHistoryRepository repository;

    private final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
            new ItemTouchHelper.SimpleCallback(
                    0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView,
                                      @NonNull RecyclerView.ViewHolder viewHolder,
                                      @NonNull RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    confirmDelete(viewHolder.getBindingAdapterPosition());
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHistoryListBinding.inflate(inflater, container, false);
        repository = new WorkoutHistoryRepository(requireContext());

        adapter = new HistoryAdapter(item ->
                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main, HistoryDetailFragment.forSession(item.session.id))
                        .addToBackStack(null)
                        .commit());

        binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerHistory.setAdapter(adapter);
        itemTouchHelper.attachToRecyclerView(binding.recyclerHistory);

        loadHistory();

        return binding.getRoot();
    }

    private void confirmDelete(int position) {
        WorkoutSessionSummary item = adapter.getItem(position);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Verlaufseintrag löschen?")
                .setMessage("Der Eintrag für \"" + displayName(item) + "\" wird endgültig gelöscht.")
                .setNegativeButton("Abbrechen", (dialog, which) -> adapter.notifyItemChanged(position))
                .setPositiveButton("Löschen", (dialog, which) -> {
                    repository.delete(item.session);
                    adapter.removeItem(position);
                    binding.txtEmpty.setVisibility(
                            adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                })
                .setOnCancelListener(dialog -> adapter.notifyItemChanged(position))
                .show();
    }

    private String displayName(WorkoutSessionSummary item) {
        return item.session.workoutName != null ? item.session.workoutName : "Workout";
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistory();
    }

    private void loadHistory() {

        List<WorkoutSessionSummary> items = repository.getHistory();

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
