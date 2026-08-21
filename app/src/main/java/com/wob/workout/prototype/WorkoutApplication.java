package com.wob.workout.prototype;

// PROTOTYPE — throwaway, see AppContainer.java for context.

import android.app.Application;

/**
 * Would replace the missing {@code android:name} on the {@code <application>} tag
 * in AndroidManifest.xml once this pattern is adopted for real.
 */
public class WorkoutApplication extends Application {

    private AppContainer appContainer;

    @Override
    public void onCreate() {
        super.onCreate();
        appContainer = new AppContainer(this);
    }

    public AppContainer getAppContainer() {
        return appContainer;
    }
}
