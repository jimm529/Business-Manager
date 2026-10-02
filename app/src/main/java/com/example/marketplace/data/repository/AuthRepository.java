package com.example.marketplace.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.marketplace.BuildConfig;
import com.example.marketplace.R;
import com.example.marketplace.data.api.ApiClient;
import com.example.marketplace.data.model.auth.AuthResponse;
import com.example.marketplace.data.model.auth.LoginRequest;
import com.example.marketplace.data.model.auth.RegisterRequest;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.SessionManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import okhttp3.HttpUrl;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Runs login/register, saves the token on success and reports the result as LiveData. */
public class AuthRepository {

    private static final String TAG = ApiClient.LOG_TAG;

    private static AuthRepository instance;

    private final SessionManager sessionManager;

    private AuthRepository(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public static synchronized AuthRepository getInstance() {
        if (instance == null) {
            instance = new AuthRepository(SessionManager.getInstance());
        }
        return instance;
    }

    public LiveData<Resource<AuthResponse>> login(LoginRequest request) {
        return execute(ApiClient.getApiService().login(request));
    }

    public LiveData<Resource<AuthResponse>> register(RegisterRequest request) {
        return execute(ApiClient.getApiService().register(request));
    }

    private LiveData<Resource<AuthResponse>> execute(Call<AuthResponse> call) {
        MutableLiveData<Resource<AuthResponse>> result = new MutableLiveData<>(Resource.loading());
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                result.postValue(handle(response));
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                Log.e(TAG, call.request().method() + " " + call.request().url() + " failed", t);
                result.postValue(describeFailure(t));
            }
        });
        return result;
    }

    /** The request never got an answer: work out why, so the message actually helps. */
    private Resource<AuthResponse> describeFailure(Throwable t) {
        if (t instanceof SocketTimeoutException) {
            return Resource.error(R.string.error_timeout);
        }
        if (t instanceof UnknownHostException || t instanceof ConnectException || t instanceof IOException) {
            // Debug builds show which address was tried; that is the first thing to check.
            if (BuildConfig.DEBUG) {
                String url = sessionManager.getBaseUrl();
                HttpUrl base = HttpUrl.parse(url);
                String address = base == null ? url : base.host() + ":" + base.port();
                return Resource.errorWithArgs(R.string.error_cannot_reach_server_debug, address);
            }
            return Resource.error(R.string.error_network);
        }
        return Resource.error(R.string.error_generic);
    }

    private Resource<AuthResponse> handle(Response<AuthResponse> response) {
        if (!response.isSuccessful()) {
            // 400 / 401 / 403 / 404 / 409 / 500: show exactly what the server said.
            String serverMessage = readErrorMessage(response);
            Log.w(TAG, "HTTP " + response.code() + " from server: " + serverMessage);
            return Resource.error(serverMessage, R.string.error_generic);
        }
        AuthResponse body = response.body();
        if (body == null) {
            return Resource.error(R.string.error_generic);
        }
        if (body.requiresTwoFactor()) {
            // The 2-step code screen is not built yet; no token was issued so nothing is saved.
            return Resource.error(R.string.error_two_factor_unsupported);
        }
        String token = body.getToken();
        if (token == null || token.isEmpty()) {
            return Resource.error(body.getMessage(), R.string.error_generic);
        }
        sessionManager.saveSession(token, body.getUser());
        return Resource.success(body);
    }

    /**
     * Reads the human message out of an error answer. The Express backend sends
     * {"message": "..."}; older handlers may use "msg" or "error" (text or an object with "message").
     * Returns null when the body is empty, not JSON (e.g. an HTML error page) or has no message.
     */
    @Nullable
    private String readErrorMessage(Response<?> response) {
        try (ResponseBody errorBody = response.errorBody()) {
            if (errorBody == null) {
                return null;
            }
            JsonElement root = JsonParser.parseString(errorBody.string());
            if (!root.isJsonObject()) {
                return null;
            }
            JsonObject object = root.getAsJsonObject();
            for (String key : new String[]{"message", "msg", "error"}) {
                String text = asText(object.get(key));
                if (text != null) {
                    return text;
                }
            }
        } catch (Exception ignored) {
            // Unreadable body: the caller falls back to a generic message.
        }
        return null;
    }

    @Nullable
    private String asText(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            String text = element.getAsString().trim();
            return text.isEmpty() ? null : text;
        }
        if (element.isJsonObject()) {
            return asText(element.getAsJsonObject().get("message"));
        }
        return null;
    }
}
