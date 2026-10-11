package com.example.marketplace.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.marketplace.data.api.ApiClient;
import com.example.marketplace.data.model.settings.MessageResponse;
import com.example.marketplace.data.model.vendor.CreateVendorRequest;
import com.example.marketplace.data.model.vendor.RegisteredVendorsResponse;
import com.example.marketplace.utils.Resource;

/** The operator's registered vendors: list them (with search and status filter) and register a new one. */
public class VendorRepository {

    private static VendorRepository instance;

    private VendorRepository() { }

    public static synchronized VendorRepository getInstance() {
        if (instance == null) {
            instance = new VendorRepository();
        }
        return instance;
    }

    /** GET vendors. A null or empty search / status is left out of the request. */
    public LiveData<Resource<RegisteredVendorsResponse>> getVendors(@Nullable String search, @Nullable String status) {
        return ApiResults.enqueue(ApiClient.getApiService().getRegisteredVendors(emptyToNull(search), emptyToNull(status)));
    }

    /** POST vendors. */
    public LiveData<Resource<MessageResponse>> createVendor(@NonNull CreateVendorRequest request) {
        return ApiResults.enqueue(ApiClient.getApiService().createVendor(request));
    }

    @Nullable
    private static String emptyToNull(@Nullable String text) {
        return text == null || text.trim().isEmpty() ? null : text.trim();
    }
}
