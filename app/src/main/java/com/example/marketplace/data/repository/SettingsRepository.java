package com.example.marketplace.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.marketplace.data.api.ApiClient;
import com.example.marketplace.data.model.settings.ChangePasswordRequest;
import com.example.marketplace.data.model.settings.MessageResponse;
import com.example.marketplace.data.model.settings.OperatorSettingsResponse;
import com.example.marketplace.data.model.settings.TwoFactorSetupResponse;
import com.example.marketplace.data.model.settings.TwoFactorVerifyRequest;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.RoleAccess;
import com.example.marketplace.utils.SessionManager;
import com.google.gson.JsonObject;

/** The Operator Settings calls: load, save, change password and set up two-factor sign-in. */
public class SettingsRepository {

    private static SettingsRepository instance;

    private SettingsRepository() { }

    public static synchronized SettingsRepository getInstance() {
        if (instance == null) {
            instance = new SettingsRepository();
        }
        return instance;
    }

    /** GET settings. */
    public LiveData<Resource<OperatorSettingsResponse>> getSettings() {
        return ApiResults.enqueue(ApiClient.getApiService().getSettings());
    }

    /** PUT settings. */
    public LiveData<Resource<OperatorSettingsResponse>> updateSettings(@NonNull JsonObject body) {
        return ApiResults.enqueue(ApiClient.getApiService().updateSettings(body));
    }

    /** PUT settings/password. */
    public LiveData<Resource<MessageResponse>> changePassword(@NonNull ChangePasswordRequest request) {
        return ApiResults.enqueue(isVendor()
                ? ApiClient.getApiService().changeAccountPassword(request)
                : ApiClient.getApiService().changePassword(request));
    }

    /** POST settings/2fa/setup. */
    public LiveData<Resource<TwoFactorSetupResponse>> setUpTwoFactor() {
        return ApiResults.enqueue(isVendor()
                ? ApiClient.getApiService().setUpAccountTwoFactor()
                : ApiClient.getApiService().setUpTwoFactor());
    }

    /** POST settings/2fa/verify. */
    public LiveData<Resource<MessageResponse>> verifyTwoFactor(@NonNull String code) {
        TwoFactorVerifyRequest request = new TwoFactorVerifyRequest(code);
        return ApiResults.enqueue(isVendor()
                ? ApiClient.getApiService().verifyAccountTwoFactor(request)
                : ApiClient.getApiService().verifyTwoFactor(request));
    }

    /** Vendors may not use the operator's settings routes, so password and 2FA go through the account routes. */
    private static boolean isVendor() {
        return RoleAccess.isVendor(SessionManager.getInstance().getRole());
    }
}
