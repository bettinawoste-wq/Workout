package com.wob.workout.ui.workout;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.databinding.ItemWorkoutStripPictureBinding;
import com.wob.workout.media.ExerciseThumbnails;
import java.util.ArrayList;
import java.util.List;

/**
 * The picture strip of one workout card: every exercise in workout order, each as its picture
 * with the title below. A tap on any of them counts as a tap on the card.
 */
class PictureStripAdapter extends RecyclerView.Adapter<PictureStripAdapter.ViewHolder> {

    private final ExerciseThumbnails thumbnails;
    private final List<Exercise> exercises = new ArrayList<>();
    private Runnable onTap = () -> { };

    PictureStripAdapter(ExerciseThumbnails thumbnails) {
        this.thumbnails = thumbnails;
    }

    void setData(List<Exercise> data, Runnable onTap) {
        exercises.clear();
        exercises.addAll(data);
        this.onTap = onTap;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewHolder holder = new ViewHolder(ItemWorkoutStripPictureBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false));
        // Holders travel between cards through the shared pool, so the tap asks the adapter
        // it is currently bound to.
        holder.itemView.setOnClickListener(v -> {
            RecyclerView.Adapter<?> adapter = holder.getBindingAdapter();
            if (adapter instanceof PictureStripAdapter) {
                ((PictureStripAdapter) adapter).onTap.run();
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exercise exercise = exercises.get(position);
        holder.binding.txtTitle.setText(exercise.title);
        thumbnails.show(holder.binding.imgPicture, exercise);
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final ItemWorkoutStripPictureBinding binding;

        ViewHolder(ItemWorkoutStripPictureBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
