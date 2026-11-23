package com.wob.workout.ui.workout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.wob.workout.data.entity.Exercise;
import com.wob.workout.databinding.ItemExercisePickBinding;
import com.wob.workout.media.ExerciseThumbnails;

import java.util.ArrayList;
import java.util.List;

/** The exercises the search shows, with a checkbox per row; what is ticked, and in which
 *  order, is {@link ExercisePicks}'. */
class ExercisePickerAdapter extends RecyclerView.Adapter<ExercisePickerAdapter.ViewHolder> {

    interface PickedCountListener {
        void onPickedCountChanged(int count);
    }

    private final ExercisePicks picks = new ExercisePicks();
    /** What the search shows, in the list's order. */
    private List<Exercise> shown = new ArrayList<>();
    private final ExerciseThumbnails thumbnails;
    private final PickedCountListener listener;

    ExercisePickerAdapter(ExerciseThumbnails thumbnails, PickedCountListener listener) {
        this.thumbnails = thumbnails;
        this.listener = listener;
    }

    /** @return whether the search under way finds nothing in the new catalog */
    boolean setData(List<Exercise> data) {
        picks.setCatalog(data);
        showSearch();
        return picks.noMatch();
    }

    /** Shows the exercises whose title contains {@code typed}; ticks stay as they are.
     *  @return whether the search found nothing */
    boolean search(String typed) {
        picks.search(typed);
        showSearch();
        return picks.noMatch();
    }

    long[] getPickedIds() {
        return picks.pickedIds();
    }

    void setPickedIds(long[] ids) {
        picks.restore(ids);
        notifyDataSetChanged();
        listener.onPickedCountChanged(picks.count());
    }

    /** The ticked exercises in tick order, also those the search hides. */
    List<Exercise> getPicked() {
        return picks.picked();
    }

    private void showSearch() {
        shown = picks.shown();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemExercisePickBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exercise exercise = shown.get(position);
        holder.binding.txtTitle.setText(exercise.title);
        thumbnails.show(holder.binding.imgThumbnail, exercise);

        boolean picked = picks.isPicked(exercise.id);
        holder.binding.checkPicked.setChecked(picked);
        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;
            picks.toggle(exercise.id);
            notifyItemChanged(adapterPosition);
            listener.onPickedCountChanged(picks.count());
        });
        // The checkbox itself is hidden from TalkBack, so the row announces the tick.
        ViewCompat.setAccessibilityDelegate(holder.itemView, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(
                    @NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(CheckBox.class.getName());
                info.setCheckable(true);
                info.setChecked(picked
                        ? AccessibilityNodeInfoCompat.CHECKED_STATE_TRUE
                        : AccessibilityNodeInfoCompat.CHECKED_STATE_FALSE);
            }
        });
    }

    @Override
    public int getItemCount() {
        return shown.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemExercisePickBinding binding;

        ViewHolder(ItemExercisePickBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
