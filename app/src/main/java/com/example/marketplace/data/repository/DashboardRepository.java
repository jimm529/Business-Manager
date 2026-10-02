package com.example.marketplace.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.marketplace.R;
import com.example.marketplace.data.api.ApiClient;
import com.example.marketplace.data.model.auth.MeResponse;
import com.example.marketplace.data.model.auth.User;
import com.example.marketplace.data.model.dashboard.OrderApprovalsResponse;
import com.example.marketplace.data.model.dashboard.ProductsResponse;
import com.example.marketplace.data.model.dashboard.VendorOverviewResponse;
import com.example.marketplace.data.model.order.OrdersResponse;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.SessionManager;

/** Loads everything the Home screen shows: the profile, the vendor dashboard and the marketplace feed. */
public class DashboardRepository {

    private static DashboardRepository instance;

    private final SessionManager sessionManager;

    private DashboardRepository(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public static synchronized DashboardRepository getInstance() {
        if (instance == null) {
            instance = new DashboardRepository(SessionManager.getInstance());
        }
        return instance;
    }

    /** The signed-in person (GET auth/me). Also refreshes the saved name and role. */
    public LiveData<Resource<MeResponse>> getProfile() {
        return ApiResults.enqueue(ApiClient.getApiService().getMe(), this::keepProfile);
    }

    private Resource<MeResponse> keepProfile(@NonNull MeResponse body) {
        User user = body.getUser();
        if (user == null) {
            return Resource.error(R.string.error_generic);
        }
        sessionManager.updateUser(user);
        return Resource.success(body);
    }

    /** Vendor dashboard numbers (GET vendor-portal/overview). */
    public LiveData<Resource<VendorOverviewResponse>> getVendorOverview() {
        return ApiResults.enqueue(ApiClient.getApiService().getVendorOverview());
    }

    /** How many requests wait for the vendor's decision (GET vendor-portal/order-approvals). */
    public LiveData<Resource<OrderApprovalsResponse>> getVendorApprovals() {
        return ApiResults.enqueue(ApiClient.getApiService().getVendorApprovals());
    }

    /** The user's orders, newest first (GET orders). */
    public LiveData<Resource<OrdersResponse>> getOrders() {
        return ApiResults.enqueue(ApiClient.getApiService().getOrders());
    }

    /** Marketplace listings (GET lots), optionally filtered by search text and one material. */
    public LiveData<Resource<ProductsResponse>> getProducts(@Nullable String search, @Nullable String material) {
        return ApiResults.enqueue(ApiClient.getApiService().getLots(emptyToNull(search), emptyToNull(material)));
    }

    @Nullable
    private static String emptyToNull(@Nullable String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }
}
