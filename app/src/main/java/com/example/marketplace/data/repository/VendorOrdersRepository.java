package com.example.marketplace.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.marketplace.data.api.ApiClient;
import com.example.marketplace.data.model.vendor.OrderRequestsResponse;
import com.example.marketplace.data.model.vendor.RequestActionResponse;
import com.example.marketplace.utils.Resource;

/** The vendor's Order Requests Ledger: read the requests, accept one, or reject one. */
public class VendorOrdersRepository {

    /** How many requests one load asks for. Search and filters then work on what was loaded. */
    private static final int LIMIT = 200;

    private static VendorOrdersRepository instance;

    private VendorOrdersRepository() { }

    public static synchronized VendorOrdersRepository getInstance() {
        if (instance == null) {
            instance = new VendorOrdersRepository();
        }
        return instance;
    }

    /** GET vendor-portal/orders. */
    public LiveData<Resource<OrderRequestsResponse>> getRequests() {
        return ApiResults.enqueue(ApiClient.getApiService().getVendorOrderRequests(LIMIT));
    }

    /** POST vendor-portal/orders/{id}/accept. */
    public LiveData<Resource<RequestActionResponse>> accept(@NonNull String requestId) {
        return ApiResults.enqueue(ApiClient.getApiService().acceptVendorRequest(requestId));
    }

    /** POST vendor-portal/orders/{id}/reject. */
    public LiveData<Resource<RequestActionResponse>> reject(@NonNull String requestId) {
        return ApiResults.enqueue(ApiClient.getApiService().rejectVendorRequest(requestId));
    }
}
