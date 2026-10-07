package com.example.marketplace.ui.messages;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;

/** The outcome of something the person did (send, accept, counter, upload), ready to show as a message. */
public final class ActionResult {

    @Nullable
    private final Resource<?> error;
    @Nullable
    private final String serverMessage;
    @StringRes
    private final int successRes;

    private ActionResult(@Nullable Resource<?> error, @Nullable String serverMessage, @StringRes int successRes) {
        this.error = error;
        this.serverMessage = serverMessage;
        this.successRes = successRes;
    }

    public static ActionResult success(@Nullable String serverMessage, @StringRes int fallbackRes) {
        return new ActionResult(null, serverMessage, fallbackRes);
    }

    public static ActionResult failure(@NonNull Resource<?> error) {
        return new ActionResult(error, null, 0);
    }

    public boolean isError() {
        return error != null;
    }

    /** The server's message when it sent one, otherwise the app's own text. */
    @NonNull
    public String text(@NonNull Context context) {
        if (error != null) {
            return ResourceMessages.errorText(context, error);
        }
        if (serverMessage != null && !serverMessage.trim().isEmpty()) {
            return serverMessage;
        }
        return context.getString(successRes);
    }
}
