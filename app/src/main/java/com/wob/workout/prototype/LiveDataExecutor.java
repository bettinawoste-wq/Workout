package com.wob.workout.prototype;

// PROTOTYPE — throwaway, see AppContainer.java for context.

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * Wraps a blocking call (e.g. a synchronous Repository/DAO method) so a ViewModel can
 * expose it as LiveData without ever touching the main thread.
 *
 * Answers the "hard to reason about on paper" cases directly:
 * - Rotation: the LiveData lives on the ViewModel, which survives rotation, so the
 *   background work is NOT re-triggered — the new Fragment instance just re-observes
 *   the same in-flight (or already-completed) LiveData.
 * - Fragment destroyed while work is in flight: postValue() still succeeds (LiveData
 *   doesn't care whether anyone is observing), but with no active observer nothing
 *   happens on screen — no crash, no leak, since the ViewModel (not the Fragment) holds
 *   the reference to the LiveData and the work.
 * - Two rapid calls: with a single-thread executor (see AppContainer.ioExecutor) the
 *   second call's Runnable simply queues behind the first — no overlapping DB access.
 */
public class LiveDataExecutor {

    public static <T> LiveData<T> runOnExecutor(ExecutorService executor, Supplier<T> work) {
        MutableLiveData<T> result = new MutableLiveData<>();
        executor.execute(() -> {
            T value = work.get();
            result.postValue(value); // postValue() is safe to call from any thread
        });
        return result;
    }
}
