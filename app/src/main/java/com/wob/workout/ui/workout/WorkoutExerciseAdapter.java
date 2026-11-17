package com.wob.workout.ui.workout;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wob.workout.data.WorkoutExercise;
import com.wob.workout.databinding.ItemWorkoutExerciseBinding;
import com.wob.workout.media.ExerciseThumbnails;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the workout draft's exercises as it hands them over; the draft owns their order.
 * A drag step that the draft has taken over reaches the list as a move, so the drag goes
 * on; any other change redraws the list.
 */
public class WorkoutExerciseAdapter
        extends RecyclerView.Adapter<WorkoutExerciseAdapter.ViewHolder> {

    public interface Listener {
        void onItemClicked(int position);
    }

    private final List<WorkoutExercise> shown = new ArrayList<>();
    private final Listener listener;
    private final ExerciseThumbnails thumbnails;

    public WorkoutExerciseAdapter(Listener listener, ExerciseThumbnails thumbnails) {
        this.listener = listener;
        this.thumbnails = thumbnails;
    }

    public void setData(List<WorkoutExercise> data) {
        int[] move = singleMove(shown, data);
        shown.clear();
        shown.addAll(data);
        if (move != null) {
            notifyItemMoved(move[0], move[1]);
        } else {
            notifyDataSetChanged();
        }
    }

    /** Where one workout exercise went if {@code after} is {@code before} with exactly that
     *  one moved, as a drag step leaves it; null for any other change. */
    static int[] singleMove(List<WorkoutExercise> before, List<WorkoutExercise> after) {
        if (before.size() != after.size()) return null;
        int first = 0;
        while (first < before.size() && before.get(first) == after.get(first)) first++;
        if (first == before.size()) return null;
        int last = before.size() - 1;
        while (before.get(last) == after.get(last)) last--;
        // Immutable values: the same object is the same, unchanged workout exercise.
        if (before.get(first) == after.get(last) && shifted(before, after, first, last, 1)) {
            return new int[] {first, last};
        }
        if (before.get(last) == after.get(first) && shifted(before, after, first, last, -1)) {
            return new int[] {last, first};
        }
        return null;
    }

    /** Whether the ones between {@code first} and {@code last} each moved one place:
     *  towards the start for {@code step} 1 (the one at {@code first} went down to
     *  {@code last}), towards the end for -1 (the one at {@code last} went up). */
    private static boolean shifted(List<WorkoutExercise> before, List<WorkoutExercise> after,
                                   int first, int last, int step) {
        for (int i = first; i <= last; i++) {
            int from = i + step;
            if (step == 1 && i == last) continue;
            if (step == -1 && i == first) continue;
            if (after.get(i) != before.get(from)) return false;
        }
        return true;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemWorkoutExerciseBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkoutExercise workoutExercise = shown.get(position);

        holder.binding.txtTitle.setText(workoutExercise.exercise.title);
        thumbnails.show(holder.binding.imgThumbnail, workoutExercise.exercise);
        holder.binding.txtDuration.setText(workoutExercise.durationSeconds + "s");
        holder.itemView.setOnClickListener(v -> {
            int clicked = holder.getBindingAdapterPosition();
            if (clicked != RecyclerView.NO_POSITION) listener.onItemClicked(clicked);
        });
    }

    @Override
    public int getItemCount() {
        return shown.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final ItemWorkoutExerciseBinding binding;

        ViewHolder(ItemWorkoutExerciseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
