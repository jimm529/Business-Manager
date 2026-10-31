package com.example.marketplace.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.marketplace.data.api.ApiClient;
import com.example.marketplace.data.model.messages.ConversationResponse;
import com.example.marketplace.data.model.vendor.VendorLotsResponse;
import com.example.marketplace.data.model.vendor.VendorProfileResponse;
import com.example.marketplace.utils.Resource;

/** What the buyer's "vendor inventory" page asks the server: the vendor's profile, the lots, and a conversation. */
public class VendorInventoryRepository {

    private static VendorInventoryRepository instance;

    private VendorInventoryRepository() { }

    public static synchronized VendorInventoryRepository getInstance() {
        if (instance == null) {
            instance = new VendorInventoryRepository();
        }
        return instance;
    }

    /** GET vendors/{id}. */
    public LiveData<Resource<VendorProfileResponse>> getProfile(@NonNull String vendorId) {
        return ApiResults.enqueue(ApiClient.getApiService().getVendorProfile(vendorId));
    }

    /** GET lots (all of them; the vendor's are picked out afterwards). */
    public LiveData<Resource<VendorLotsResponse>> getLots() {
        return ApiResults.enqueue(ApiClient.getApiService().getAllLots());
    }

    /** POST conversations/vendor/{vendorId}: finds the existing conversation or starts one. */
    public LiveData<Resource<ConversationResponse>> openConversation(@NonNull String vendorId) {
        return ApiResults.enqueue(ApiClient.getApiService().openVendorConversation(vendorId));
    }
}
