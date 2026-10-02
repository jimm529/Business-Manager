package com.example.marketplace.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/** Wraps the state of one network call: idle, loading, success (with data) or error (with a message). */
public class Resource<T> {

    public enum Status { IDLE, LOADING, SUCCESS, ERROR }

    @NonNull
    public final Status status;
    @Nullable
    public final T data;
    /** Message that came from the server (may be null). */
    @Nullable
    public final String message;
    /** String resource to show when there is no server message (0 = none). */
    @StringRes
    public final int messageRes;

    private Resource(@NonNull Status status, @Nullable T data, @Nullable String message, @StringRes int messageRes) {
        this.status = status;
        this.data = data;
        this.message = message;
        this.messageRes = messageRes;
    }

    public static <T> Resource<T> idle() {
        return new Resource<>(Status.IDLE, null, null, 0);
    }

    public static <T> Resource<T> loading() {
        return new Resource<>(Status.LOADING, null, null, 0);
    }

    public static <T> Resource<T> success(@Nullable T data) {
        return new Resource<>(Status.SUCCESS, data, null, 0);
    }

    public static <T> Resource<T> error(@Nullable String message, @StringRes int fallbackRes) {
        return new Resource<>(Status.ERROR, null, message, fallbackRes);
    }

    public static <T> Resource<T> error(@StringRes int messageRes) {
        return new Resource<>(Status.ERROR, null, null, messageRes);
    }
}
