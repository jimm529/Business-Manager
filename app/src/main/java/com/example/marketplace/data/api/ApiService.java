package com.example.marketplace.data.api;

import com.example.marketplace.data.model.auth.AuthResponse;
import com.example.marketplace.data.model.auth.LoginRequest;
import com.example.marketplace.data.model.auth.MeResponse;
import com.example.marketplace.data.model.auth.RegisterRequest;
import com.example.marketplace.data.model.common.HealthResponse;
import com.example.marketplace.data.model.dashboard.ActiveDealsResponse;
import com.example.marketplace.data.model.dashboard.OrderApprovalsResponse;
import com.example.marketplace.data.model.dashboard.ProductsResponse;
import com.example.marketplace.data.model.dashboard.PurchaseHistoryResponse;
import com.example.marketplace.data.model.dashboard.VendorOverviewResponse;
import com.example.marketplace.data.model.dashboard.VendorsResponse;
import com.example.marketplace.data.model.order.OrdersResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * All backend calls. Paths are relative to the base URL, which already ends in "/api/",
 * so they must NOT start with "api/".
 */
public interface ApiService {

    /** Is the server up, and did it reach its database? Public, needs no login. */
    @GET("health")
    Call<HealthResponse> getHealth();

    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    /** The signed-in person's profile. */
    @GET("auth/me")
    Call<MeResponse> getMe();

    /** Vendor dashboard numbers. Vendors only. */
    @GET("vendor-portal/overview")
    Call<VendorOverviewResponse> getVendorOverview();

    /** Requests waiting for the vendor's decision. Vendors only. */
    @GET("vendor-portal/order-approvals")
    Call<OrderApprovalsResponse> getVendorApprovals();

    /** Orders of the signed-in user, newest first. */
    @GET("orders")
    Call<OrdersResponse> getOrders();

    /** Marketplace listings. A null parameter is left out of the request. */
    @GET("lots")
    Call<ProductsResponse> getLots(@Query("search") String search, @Query("materials") String materials);

    /** Buyer only: spending totals plus the newest orders. Dates are YYYY-MM-DD. */
    @GET("purchase-history")
    Call<PurchaseHistoryResponse> getPurchaseHistory(@Query("limit") int limit, @Query("startDate") String startDate);

    /** Buyer only: negotiations that are still open, newest activity first. */
    @GET("bids/active-deals")
    Call<ActiveDealsResponse> getActiveDeals();

    /** Buyer only: the suppliers this buyer works with. */
    @GET("vendors")
    Call<VendorsResponse> getVendors();
}
