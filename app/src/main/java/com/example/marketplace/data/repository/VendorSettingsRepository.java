package com.example.marketplace.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.marketplace.data.api.ApiClient;
import com.example.marketplace.data.model.settings.MessageResponse;
import com.example.marketplace.data.model.vendor.VendorSettingsResponse;
import com.example.marketplace.utils.Resource;
import com.google.gson.JsonObject;

/** The vendor's Settings screen: read everything, and save each section on its own. */
public class VendorSettingsRepository {

    private static VendorSettingsRepository instance;

    private VendorSettingsRepository() { }

    public static synchronized VendorSettingsRepository getInstance() {
        if (instance == null) {
            instance = new VendorSettingsRepository();
        }
        return instance;
    }

    /** GET vendor-portal/settings. */
    public LiveData<Resource<VendorSettingsResponse>> getSettings() {
        return ApiResults.enqueue(ApiClient.getApiService().getVendorSettings());
    }

    /** PUT vendor-portal/settings/profile: entityName, taxId, headquartersAddress. */
    public LiveData<Resource<MessageResponse>> saveProfile(@NonNull JsonObject body) {
        return ApiResults.enqueue(ApiClient.getApiService().saveVendorProfile(body));
    }

    /** PUT vendor-portal/settings/operational-preferences. */
    public LiveData<Resource<MessageResponse>> savePreferences(@NonNull JsonObject body) {
        return ApiResults.enqueue(ApiClient.getApiService().saveVendorPreferences(body));
    }

    /** PUT vendor-portal/settings/notifications. */
    public LiveData<Resource<MessageResponse>> saveNotifications(@NonNull JsonObject body) {
        return ApiResults.enqueue(ApiClient.getApiService().saveVendorNotifications(body));
    }

    /** PUT vendor-portal/settings/account: fullName, jobTitle. */
    public LiveData<Resource<MessageResponse>> saveAccount(@NonNull JsonObject body) {
        return ApiResults.enqueue(ApiClient.getApiService().saveVendorAccount(body));
    }
}
