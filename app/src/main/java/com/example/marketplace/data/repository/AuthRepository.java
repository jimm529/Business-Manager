package com.example.marketplace.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.marketplace.R;
import com.example.marketplace.data.api.ApiClient;
import com.example.marketplace.data.api.ApiService;
import com.example.marketplace.data.model.auth.AuthResponse;
import com.example.marketplace.data.model.auth.LoginRequest;
import com.example.marketplace.data.model.auth.RegisterRequest;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.SessionManager;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Runs login/register, saves the token on success and reports the result as LiveData. */
public class AuthRepository {

    /** Shape of the backend's error answers: { "message": "..." }. */
    private static class ErrorBody {
        @SerializedName("message")
        String message;
    }

    private static AuthRepository instance;

    private final ApiService api;
    private final SessionManager sessionManager;
    private final Gson gson = new Gson();

    private AuthRepository(ApiService api, SessionManager sessionManager) {
        this.api = api;
        this.sessionManager = sessionManager;
    }

    public static synchronized AuthRepository getInstance() {
        if (instance == null) {
            instance = new AuthRepository(ApiClient.getApiService(), SessionManager.getInstance());
        }
        return instance;
    }

    public LiveData<Resource<AuthResponse>> login(LoginRequest request) {
        return execute(api.login(request));
    }

    public LiveData<Resource<AuthResponse>> register(RegisterRequest request) {
        return execute(api.register(request));
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
                result.postValue(Resource.error(t instanceof IOException
                        ? R.string.error_network
                        : R.string.error_generic));
            }
        });
        return result;
    }

    private Resource<AuthResponse> handle(Response<AuthResponse> response) {
        if (!response.isSuccessful()) {
            return Resource.error(readErrorMessage(response), R.string.error_generic);
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

    private String readErrorMessage(Response<?> response) {
        try (ResponseBody errorBody = response.errorBody()) {
            if (errorBody != null) {
                ErrorBody parsed = gson.fromJson(errorBody.string(), ErrorBody.class);
                if (parsed != null && parsed.message != null && !parsed.message.trim().isEmpty()) {
                    return parsed.message;
                }
            }
        } catch (Exception ignored) {
            // Not JSON or unreadable: the caller falls back to a generic message.
        }
        return null;
    }
}
