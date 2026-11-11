package com.wob.workout.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.wob.workout.AppContainer;
import com.wob.workout.R;
import com.wob.workout.WorkoutApplication;
import com.wob.workout.WorkoutViewModelFactory;
import com.wob.workout.databinding.FragmentHistoryListBinding;
import com.wob.workout.ui.ScreenToolbar;

public class HistoryListFragment extends Fragment {

    private FragmentHistoryListBinding binding;
    private HistoryAdapter adapter;
    private HistoryListViewModel viewModel;

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
        ScreenToolbar.setUp(this, binding.toolbar);

        AppContainer appContainer =
                ((WorkoutApplication) requireActivity().getApplication()).getAppContainer();
        viewModel = new ViewModelProvider(this, new WorkoutViewModelFactory(appContainer))
                .get(HistoryListViewModel.class);

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

        viewModel.getHistory().observe(getViewLifecycleOwner(), items -> {
            adapter.setData(items);
            binding.txtEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        });

        return binding.getRoot();
    }

    private void confirmDelete(int position) {
        WorkoutSessionSummary item = adapter.getItem(position);
        long sessionId = item.session.id;

        // Re-resolve the row's position by id rather than reusing the captured `position` —
        // the list can be replaced wholesale by a Room re-emission (any write anywhere in the
        // app invalidates it, not just this dialog's own pending action) while the dialog is
        // still showing, which would make `position` point at the wrong row by the time the
        // user taps Cancel.
        Runnable undoSwipe = () -> {
            int current = adapter.indexOfSessionId(sessionId);
            if (current != -1) adapter.notifyItemChanged(current);
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Verlaufseintrag löschen?")
                .setMessage("Der Eintrag für \"" + displayName(item) + "\" wird endgültig gelöscht.")
                .setNegativeButton("Abbrechen", (dialog, which) -> undoSwipe.run())
                .setPositiveButton("Löschen", (dialog, which) -> viewModel.delete(item.session))
                .setOnCancelListener(dialog -> undoSwipe.run())
                .show();
    }

    private String displayName(WorkoutSessionSummary item) {
        return item.session.workoutName != null ? item.session.workoutName : "Workout";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
