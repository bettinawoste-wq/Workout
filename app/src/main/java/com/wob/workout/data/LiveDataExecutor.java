package com.wob.workout.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * Wraps a blocking call (a synchronous Repository/DAO method) so a ViewModel can expose
 * it as LiveData without touching the main thread. See wayfinder ticket #2.
 */
public class LiveDataExecutor {

    public static <T> LiveData<T> runOnExecutor(ExecutorService executor, Supplier<T> work) {
        MutableLiveData<T> result = new MutableLiveData<>();
        executor.execute(() -> {
            T value = work.get();
            result.postValue(value);
        });
        return result;
    }
}
