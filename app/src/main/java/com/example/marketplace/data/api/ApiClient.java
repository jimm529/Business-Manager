package com.example.marketplace.data.api;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.marketplace.BuildConfig;
import com.example.marketplace.utils.ServerUrls;
import com.example.marketplace.utils.SessionManager;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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
            apiService = buildRetrofit(url, getClient()).create(ApiService.class);
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
     * A separate, short-lived client for one server address, used to test a connection before saving it.
     * It never sends the login token. Throws IllegalArgumentException when the address is not valid.
     */
    public static ApiService createService(@NonNull String serverUrl, int timeoutSeconds) {
        String apiUrl = ServerUrls.toApiBaseUrl(serverUrl);
        if (apiUrl == null) {
            throw new IllegalArgumentException("Not a valid server address: " + serverUrl);
        }
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS);
        addLogging(builder);
        return buildRetrofit(apiUrl, builder.build()).create(ApiService.class);
    }

    private static Retrofit buildRetrofit(String apiBaseUrl, OkHttpClient httpClient) {
        return new Retrofit.Builder()
                .baseUrl(apiBaseUrl)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    /** The saved address, or the build default when the saved one is not usable. */
    private static String resolveApiBaseUrl() {
        String url = ServerUrls.toApiBaseUrl(SessionManager.getInstance().getBaseUrl());
        if (url == null) {
            url = ServerUrls.toApiBaseUrl(BuildConfig.BASE_URL);
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
            addLogging(builder);
            client = builder.build();
        }
        return client;
    }

    /** Full request/response logging, debug builds only. */
    private static void addLogging(OkHttpClient.Builder builder) {
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(
                    message -> Log.d(LOG_TAG, SECRET_FIELDS.matcher(message).replaceAll("$1***$2")));
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            logging.redactHeader("Authorization");
            builder.addInterceptor(logging);
        }
    }
}
