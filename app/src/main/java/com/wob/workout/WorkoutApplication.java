package com.wob.workout;

import android.app.Application;

public class WorkoutApplication extends Application {

    private AppContainer appContainer;

    @Override
    public void onCreate() {
        super.onCreate();
        appContainer = new AppContainer(this);
        // Before the first screen is created, so it never flashes in the other design.
        appContainer.designSetting.get().apply();
    }

    public AppContainer getAppContainer() {
        return appContainer;
    }
}
