package com.wob.workout.data;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wraps a blocking call (a synchronous Repository/DAO method) so a ViewModel can expose
 * it as LiveData without touching the main thread. See wayfinder ticket #2.
 */
public class LiveDataExecutor {

    private static final String TAG = "LiveDataExecutor";

    public static <T> LiveData<T> runOnExecutor(ExecutorService executor, Supplier<T> work) {
        MutableLiveData<T> result = new MutableLiveData<>();
        executor.execute(() -> {
            try {
                result.postValue(work.get());
            } catch (RuntimeException e) {
                // Without this, a failing DAO/repository call would silently strand the
                // LiveData with no value and no observer callback ever firing.
                Log.e(TAG, "Background work failed, LiveData will not receive a value", e);
            }
        });
        return result;
    }

    /** For a ViewModel that needs to load once (e.g. in its constructor), populate its
     *  initial state, and then stop observing — the one-shot-load pattern several screens'
     *  ViewModels need alongside {@link #runOnExecutor}. */
    public static <T> void runOnExecutorOnce(
            ExecutorService executor, Supplier<T> work, Consumer<T> onResult) {
        LiveData<T> result = runOnExecutor(executor, work);
        result.observeForever(new Observer<T>() {
            @Override
            public void onChanged(T value) {
                result.removeObserver(this);
                onResult.accept(value);
            }
        });
    }
}
