package com.example.marketplace.data.api;

import androidx.annotation.NonNull;

import com.example.marketplace.utils.SessionManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Adds "Authorization: Bearer <token>" to every call when a token exists, and
 * sends the user back to Login when the server says the token is no longer valid (401).
 */
public class AuthInterceptor implements Interceptor {

    private final SessionManager sessionManager;

    public AuthInterceptor(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        String token = sessionManager.getToken();

        Request.Builder builder = original.newBuilder().header("Accept", "application/json");
        boolean sentToken = token != null && !token.isEmpty();
        if (sentToken) {
            builder.header("Authorization", "Bearer " + token);
        }

        Response response = chain.proceed(builder.build());

        // A 401 on login/register just means wrong credentials, so only react when we sent a token.
        if (response.code() == 401 && sentToken) {
            sessionManager.notifySessionExpired();
        }
        return response;
    }
}
