package com.example.marketplace.data.api;

import android.util.Log;

import com.example.marketplace.BuildConfig;
import com.example.marketplace.utils.SessionManager;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Builds the one shared Retrofit/OkHttp setup. */
public final class ApiClient {

    /** Filter Logcat by this tag to see every request and response. */
    public static final String LOG_TAG = "MARKETPLACE_API";

    // Hides secrets inside logged JSON bodies: "password":"abc" becomes "password":"***".
    private static final Pattern SECRET_FIELDS = Pattern.compile(
            "(\"(?:password|confirmPassword|currentPassword|newPassword|token|pendingToken)\"\\s*:\\s*\")[^\"]*(\")");

    private static ApiService apiService;

    private ApiClient() { }

    public static synchronized ApiService getApiService() {
        if (apiService == null) {
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

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .client(builder.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }
}
