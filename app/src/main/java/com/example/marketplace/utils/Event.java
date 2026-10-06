package com.example.marketplace.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** A one-time message for the screen (e.g. "order sent"): after it was handled once it is not delivered again, so rotation does not repeat it. */
public class Event<T> {

    private final T content;
    private boolean handled;

    public Event(@NonNull T content) {
        this.content = content;
    }

    /** The content the first time, null afterwards. */
    @Nullable
    public T getContentIfNotHandled() {
        if (handled) {
            return null;
        }
        handled = true;
        return content;
    }
}
