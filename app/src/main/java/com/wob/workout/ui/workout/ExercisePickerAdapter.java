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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** The exercise catalog with a checkbox per row. Remembers the ticks in the order they
 *  were made, which is the order they get added to the workout in. */
class ExercisePickerAdapter extends RecyclerView.Adapter<ExercisePickerAdapter.ViewHolder> {

    interface PickedCountListener {
        void onPickedCountChanged(int count);
    }

    private final List<Exercise> exercises = new ArrayList<>();
    /** Exercise ids in tick order; unticking removes one, ticking again appends it. */
    private final LinkedHashSet<Long> pickedIds = new LinkedHashSet<>();
    private final ExerciseThumbnails thumbnails;
    private final PickedCountListener listener;

    ExercisePickerAdapter(ExerciseThumbnails thumbnails, PickedCountListener listener) {
        this.thumbnails = thumbnails;
        this.listener = listener;
    }

    void setData(List<Exercise> data) {
        exercises.clear();
        exercises.addAll(data);
        notifyDataSetChanged();
    }

    long[] getPickedIds() {
        return pickedIds.stream().mapToLong(Long::longValue).toArray();
    }

    void setPickedIds(long[] ids) {
        pickedIds.clear();
        for (long id : ids) pickedIds.add(id);
        notifyDataSetChanged();
        listener.onPickedCountChanged(pickedIds.size());
    }

    /** The ticked exercises in tick order; ticks for exercises not in the list are skipped. */
    List<Exercise> getPicked() {
        Map<Long, Exercise> byId = new HashMap<>();
        for (Exercise exercise : exercises) byId.put(exercise.id, exercise);

        List<Exercise> picked = new ArrayList<>();
        for (long id : pickedIds) {
            Exercise exercise = byId.get(id);
            if (exercise != null) picked.add(exercise);
        }
        return picked;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemExercisePickBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exercise exercise = exercises.get(position);
        holder.binding.txtTitle.setText(exercise.title);
        thumbnails.show(holder.binding.imgThumbnail, exercise);

        boolean picked = pickedIds.contains(exercise.id);
        holder.binding.checkPicked.setChecked(picked);
        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;
            if (!pickedIds.remove(exercise.id)) pickedIds.add(exercise.id);
            notifyItemChanged(adapterPosition);
            listener.onPickedCountChanged(pickedIds.size());
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
        return exercises.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemExercisePickBinding binding;

        ViewHolder(ItemExercisePickBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
