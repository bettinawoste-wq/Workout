package com.wob.workout.ui.history;

import com.wob.workout.data.entity.WorkoutSession;

public class WorkoutSessionSummary {

    public final WorkoutSession session;
    public final int exerciseCount;

    public WorkoutSessionSummary(WorkoutSession session, int exerciseCount) {
        this.session = session;
        this.exerciseCount = exerciseCount;
    }
}
