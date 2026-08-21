package com.wob.workout.ui.history;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wob.workout.databinding.ItemHistoryBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<WorkoutSessionSummary> sessions = new ArrayList<>();
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    public void setData(List<WorkoutSessionSummary> data) {
        sessions.clear();
        sessions.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        WorkoutSessionSummary item = sessions.get(position);

        String name = item.session.workoutName != null
                ? item.session.workoutName
                : "Workout";

        holder.binding.txtName.setText(name);
        holder.binding.txtStatus.setText(
                item.session.completed ? "✓ Abgeschlossen" : "✗ Abgebrochen");
        holder.binding.txtInfo.setText(
                dateFormat.format(new Date(item.session.startedAt))
                        + " • " + item.exerciseCount + " Übungen"
                        + " • " + formatDuration(item.session.activeDurationSeconds));
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    private String formatDuration(int seconds) {
        return String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final ItemHistoryBinding binding;

        ViewHolder(ItemHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
