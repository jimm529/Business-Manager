package com.example.marketplace.data.api;

import android.util.Log;

import androidx.annotation.Nullable;

import com.example.marketplace.BuildConfig;
import com.example.marketplace.utils.SessionManager;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Builds the shared Retrofit/OkHttp setup. The server address comes from SessionManager,
 * so when it is changed the next call to getApiService() automatically rebuilds Retrofit.
 */
public final class ApiClient {

    /** Filter Logcat by this tag to see every request and response. */
    public static final String LOG_TAG = "MARKETPLACE_API";

    // Hides secrets inside logged JSON bodies: "password":"abc" becomes "password":"***".
    private static final Pattern SECRET_FIELDS = Pattern.compile(
            "(\"(?:password|confirmPassword|currentPassword|newPassword|token|pendingToken)\"\\s*:\\s*\")[^\"]*(\")");

    private static OkHttpClient client;
    private static ApiService apiService;
    private static String builtForUrl;

    private ApiClient() { }

    public static synchronized ApiService getApiService() {
        String url = resolveApiBaseUrl();
        if (apiService == null || !url.equals(builtForUrl)) {
            apiService = new Retrofit.Builder()
                    .baseUrl(url)
                    .client(getClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService.class);
            builtForUrl = url;
        }
        return apiService;
    }

    /** Forces the next getApiService() call to build a fresh Retrofit instance. */
    public static synchronized void reset() {
        apiService = null;
        builtForUrl = null;
    }

    /**
     * Turns a server address into the Retrofit base URL: "http://localhost:5000/" becomes
     * "http://localhost:5000/api/". An address that already ends in "/api" is kept as is.
     * Returns null when the text is not a valid http/https address.
     */
    @Nullable
    public static String toApiBaseUrl(@Nullable String serverUrl) {
        if (serverUrl == null) {
            return null;
        }
        String trimmed = serverUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (!trimmed.endsWith("/api")) {
            trimmed = trimmed + "/api";
        }
        HttpUrl parsed = HttpUrl.parse(trimmed + "/");
        return parsed == null ? null : parsed.toString();
    }

    /** The saved address, or the build default when the saved one is not usable. */
    private static String resolveApiBaseUrl() {
        String url = toApiBaseUrl(SessionManager.getInstance().getBaseUrl());
        if (url == null) {
            url = toApiBaseUrl(BuildConfig.BASE_URL);
        }
        if (url == null) {
            throw new IllegalStateException("BuildConfig.BASE_URL is not a valid address: " + BuildConfig.BASE_URL);
        }
        return url;
    }

    private static OkHttpClient getClient() {
        if (client == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new AuthInterceptor(SessionManager.getInstance()));

            // Full request/response logging, debug builds only.
            if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor(
                        message -> Log.d(LOG_TAG, SECRET_FIELDS.matcher(message).replaceAll("$1***$2")));
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);
                logging.redactHeader("Authorization");
                builder.addInterceptor(logging);
            }
            client = builder.build();
        }
        return client;
    }
}
