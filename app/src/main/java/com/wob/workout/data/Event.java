package com.wob.workout.data;

/**
 * A LiveData payload for a genuinely one-shot occurrence (an error to show once) rather
 * than continuous state (a list, a phase) that's safe to redeliver to a freshly-registered
 * observer. Wraps a value with a "handled" flag so re-observing (rotation, a second
 * observer) doesn't redisplay an already-consumed event. See wayfinder ticket #7.
 */
public class Event<T> {

    private final T content;
    private boolean handled = false;

    public Event(T content) {
        this.content = content;
    }

    /** Returns the content and marks it handled, or null if it was already handled. */
    public T getContentIfNotHandled() {
        if (handled) return null;
        handled = true;
        return content;
    }
}
