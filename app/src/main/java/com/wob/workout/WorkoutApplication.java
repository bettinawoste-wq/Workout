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
        // Sets the reminders' alarm again whenever the app starts, also when the system
        // dropped it (after an app update or a force stop).
        appContainer.ioExecutor.execute(() -> appContainer.reminderAlarms.renew(
                appContainer.reminderRepository.getAll()));
    }

    public AppContainer getAppContainer() {
        return appContainer;
    }
}
