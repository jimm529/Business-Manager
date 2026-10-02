package com.example.marketplace.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.marketplace.R;

/** Turns the error inside a Resource into text for the user. The server's own message wins. */
public final class ResourceMessages {

    private ResourceMessages() { }

    /** True when the call failed because the server could not be reached (not because it said no). */
    public static boolean isConnectionProblem(@NonNull Resource<?> resource) {
        return resource.messageRes != 0
                && (resource.messageRes == R.string.error_network
                || resource.messageRes == R.string.error_timeout
                || resource.messageRes == R.string.error_cannot_reach_server_debug);
    }

    @NonNull
    public static String errorText(@NonNull Context context, @NonNull Resource<?> resource) {
        if (resource.message != null && !resource.message.trim().isEmpty()) {
            return resource.message;
        }
        if (resource.messageRes != 0) {
            return context.getString(resource.messageRes, resource.messageArgs);
        }
        return context.getString(R.string.error_generic);
    }
}
