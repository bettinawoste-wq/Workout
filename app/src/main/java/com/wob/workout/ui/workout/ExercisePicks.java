package com.wob.workout.ui.workout;

import com.wob.workout.data.entity.Exercise;
import com.wob.workout.ui.exercise.ExerciseSearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * What the exercise picker holds: the whole catalog, the search over it, and the ticks in the
 * order they were made, which is the order they get added to the workout in. A tick stays
 * while a search hides its exercise.
 */
class ExercisePicks {

    private final List<Exercise> catalog = new ArrayList<>();
    private final LinkedHashSet<Long> pickedIds = new LinkedHashSet<>();
    private String query = "";

    void setCatalog(List<Exercise> exercises) {
        catalog.clear();
        catalog.addAll(exercises);
    }

    void search(String typed) {
        query = typed;
    }

    /** The exercises the search finds, all of them for an empty search. */
    List<Exercise> shown() {
        return ExerciseSearch.matching(catalog, query);
    }

    boolean noMatch() {
        return ExerciseSearch.noMatch(catalog, query);
    }

    /** Ticks an exercise, or unticks it if it was ticked. */
    void toggle(long exerciseId) {
        if (!pickedIds.remove(exerciseId)) pickedIds.add(exerciseId);
    }

    boolean isPicked(long exerciseId) {
        return pickedIds.contains(exerciseId);
    }

    int count() {
        return pickedIds.size();
    }

    long[] pickedIds() {
        return pickedIds.stream().mapToLong(Long::longValue).toArray();
    }

    /** Puts back the ticks kept over a rotation. */
    void restore(long[] ids) {
        pickedIds.clear();
        for (long id : ids) pickedIds.add(id);
    }

    /** The ticked exercises in tick order, whatever the search; ticks for exercises not in
     *  the catalog are skipped. */
    List<Exercise> picked() {
        Map<Long, Exercise> byId = new HashMap<>();
        for (Exercise exercise : catalog) byId.put(exercise.id, exercise);

        List<Exercise> picked = new ArrayList<>();
        for (long id : pickedIds) {
            Exercise exercise = byId.get(id);
            if (exercise != null) picked.add(exercise);
        }
        return picked;
    }
}
