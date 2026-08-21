package com.wob.workout;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.test.platform.app.InstrumentationRegistry;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/** Reads LiveData values synchronously in a test. Registers the observer on the main
 *  thread (LiveData requires this), then waits for values off the main thread. */
public class LiveDataTestUtil {

    /** For a one-shot read: observes, waits for exactly one value, then detaches. */
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

    /** For asserting a single, still-active observer sees more than one emission (e.g.
     *  the value before and after a mutation) — poll() the returned queue once per
     *  expected emission, in order. The observer is never detached; only use this with a
     *  short-lived LiveData (e.g. backed by a test's in-memory database). */
    public static <T> BlockingQueue<T> observeValues(LiveData<T> liveData) {
        BlockingQueue<T> values = new LinkedBlockingQueue<>();

        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> liveData.observeForever(values::add));

        return values;
    }
}
