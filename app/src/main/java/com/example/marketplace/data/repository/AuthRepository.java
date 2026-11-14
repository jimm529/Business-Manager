package com.example.marketplace.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.marketplace.R;
import com.example.marketplace.data.api.ApiClient;
import com.example.marketplace.data.model.auth.AuthResponse;
import com.example.marketplace.data.model.auth.GoogleLoginRequest;
import com.example.marketplace.data.model.auth.LoginRequest;
import com.example.marketplace.data.model.auth.MeResponse;
import com.example.marketplace.data.model.auth.RegisterRequest;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.SessionManager;

/** Runs login/register, saves the token on success and reports the result as LiveData. */
public class AuthRepository {

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
        return ApiResults.enqueue(ApiClient.getApiService().login(request), this::saveSession);
    }

    /** Signs in with the ID token Google gave the phone (POST auth/google). */
    public LiveData<Resource<AuthResponse>> googleLogin(@NonNull String idToken, @NonNull String portal) {
        return ApiResults.enqueue(ApiClient.getApiService().googleLogin(new GoogleLoginRequest(idToken, portal)),
                this::saveSession);
    }

    public LiveData<Resource<AuthResponse>> register(RegisterRequest request) {
        return ApiResults.enqueue(ApiClient.getApiService().register(request), this::saveSession);
    }

    /** The signed-in person's profile (GET auth/me). */
    public LiveData<Resource<MeResponse>> getMe() {
        return ApiResults.enqueue(ApiClient.getApiService().getMe());
    }

    private Resource<AuthResponse> saveSession(@NonNull AuthResponse body) {
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
}
