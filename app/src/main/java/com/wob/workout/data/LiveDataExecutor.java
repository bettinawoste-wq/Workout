package com.wob.workout.data;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.ExecutorService;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wraps a blocking call (a synchronous Repository/DAO method) so a ViewModel can expose
 * it as LiveData without touching the main thread. See wayfinder ticket #2.
 */
public class LiveDataExecutor {

    private static final String TAG = "LiveDataExecutor";

    /** Always delivers exactly one value — {@code null} if {@code work} throws — so a
     *  caller observing this LiveData is guaranteed a callback instead of hanging forever
     *  on a failed background call. */
    public static <T> LiveData<T> runOnExecutor(ExecutorService executor, Supplier<T> work) {
        MutableLiveData<T> result = new MutableLiveData<>();
        executor.execute(() -> {
            try {
                result.postValue(work.get());
            } catch (RuntimeException e) {
                Log.e(TAG, "Background work failed, delivering null", e);
                result.postValue(null);
            }
        });
        return result;
    }

    /** For a ViewModel that needs to load once (e.g. in its constructor), populate its
     *  initial state, and then stop observing — the one-shot-load pattern several screens'
     *  ViewModels need alongside {@link #runOnExecutor}. {@code onResult} receives
     *  {@code null} if {@code work} threw. */
    public static <T> void runOnExecutorOnce(
            ExecutorService executor, Supplier<T> work, Consumer<T> onResult) {
        runOnExecutorOnce(executor, work, () -> true, onResult);
    }

    /** Same as {@link #runOnExecutorOnce(ExecutorService, Supplier, Consumer)}, but skips
     *  {@code onResult} if {@code stillValid} returns false once the load completes — for a
     *  ViewModel that may already be cleared (and torn down its own write-side state) by
     *  the time a background load finishes. */
    public static <T> void runOnExecutorOnce(
            ExecutorService executor, Supplier<T> work, BooleanSupplier stillValid,
            Consumer<T> onResult) {
        LiveData<T> result = runOnExecutor(executor, work);
        result.observeForever(new Observer<T>() {
            @Override
            public void onChanged(T value) {
                result.removeObserver(this);
                if (stillValid.getAsBoolean()) {
                    onResult.accept(value);
                }
            }
        });
    }
}
