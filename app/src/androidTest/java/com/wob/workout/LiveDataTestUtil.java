package com.wob.workout;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.test.platform.app.InstrumentationRegistry;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/** Reads a LiveData value synchronously in a test. Registers the observer on the main
 *  thread (LiveData requires this), then waits for it off the main thread. */
public class LiveDataTestUtil {

    public static <T> T getOrAwaitValue(LiveData<T> liveData) throws InterruptedException {
        AtomicReference<T> data = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Observer<T> observer = new Observer<T>() {
            @Override
            public void onChanged(T value) {
                data.set(value);
                latch.countDown();
                liveData.removeObserver(this);
            }
        };

        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> liveData.observeForever(observer));

        if (!latch.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException(new TimeoutException("LiveData value was never set."));
        }

        return data.get();
    }
}
