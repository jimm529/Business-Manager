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
    /** Values to fill into messageRes (for strings that contain %1$s and so on). */
    @NonNull
    public final Object[] messageArgs;

    private Resource(@NonNull Status status, @Nullable T data, @Nullable String message,
                     @StringRes int messageRes, @NonNull Object[] messageArgs) {
        this.status = status;
        this.data = data;
        this.message = message;
        this.messageRes = messageRes;
        this.messageArgs = messageArgs;
    }

    public static <T> Resource<T> idle() {
        return new Resource<>(Status.IDLE, null, null, 0, new Object[0]);
    }

    public static <T> Resource<T> loading() {
        return new Resource<>(Status.LOADING, null, null, 0, new Object[0]);
    }

    /** Loading again while the previous data is still on screen (pull-to-refresh). */
    public static <T> Resource<T> loading(@Nullable T previousData) {
        return new Resource<>(Status.LOADING, previousData, null, 0, new Object[0]);
    }

    public static <T> Resource<T> success(@Nullable T data) {
        return new Resource<>(Status.SUCCESS, data, null, 0, new Object[0]);
    }

    public static <T> Resource<T> error(@Nullable String message, @StringRes int fallbackRes) {
        return new Resource<>(Status.ERROR, null, message, fallbackRes, new Object[0]);
    }

    public static <T> Resource<T> error(@StringRes int messageRes) {
        return new Resource<>(Status.ERROR, null, null, messageRes, new Object[0]);
    }

    public static <T> Resource<T> errorWithArgs(@StringRes int messageRes, Object... args) {
        return new Resource<>(Status.ERROR, null, null, messageRes, args);
    }
}
