package com.example.marketplace.data.api;

import com.example.marketplace.data.model.auth.AuthResponse;
import com.example.marketplace.data.model.auth.LoginRequest;
import com.example.marketplace.data.model.auth.MeResponse;
import com.example.marketplace.data.model.auth.RegisterRequest;
import com.example.marketplace.data.model.bid.BidActionResponse;
import com.example.marketplace.data.model.bid.BidsResponse;
import com.example.marketplace.data.model.bid.CounterOfferRequest;
import com.example.marketplace.data.model.bid.DocumentsResponse;
import com.example.marketplace.data.model.common.HealthResponse;
import com.example.marketplace.data.model.listing.ListingOptionsResponse;
import com.example.marketplace.data.model.listing.ListingTemplatesResponse;
import com.example.marketplace.data.model.listing.LocationsResponse;
import com.example.marketplace.data.model.listing.PublishListingRequest;
import com.example.marketplace.data.model.listing.PublishListingResponse;
import com.example.marketplace.data.model.messages.ConversationResponse;
import com.example.marketplace.data.model.messages.ConversationsResponse;
import com.example.marketplace.data.model.messages.MessagesResponse;
import com.example.marketplace.data.model.messages.SendMessageRequest;
import com.example.marketplace.data.model.messages.SendMessageResponse;
import com.example.marketplace.data.model.dashboard.ActiveDealsResponse;
import com.example.marketplace.data.model.dashboard.OrderApprovalsResponse;
import com.example.marketplace.data.model.dashboard.ProductsResponse;
import com.example.marketplace.data.model.dashboard.PurchaseHistoryResponse;
import com.example.marketplace.data.model.dashboard.VendorOverviewResponse;
import com.example.marketplace.data.model.dashboard.VendorsResponse;
import com.example.marketplace.data.model.market.BookmarkResponse;
import com.example.marketplace.data.model.market.MarketplaceResponse;
import com.example.marketplace.data.model.market.OrderRequest;
import com.example.marketplace.data.model.market.OrderResponse;
import com.example.marketplace.data.model.notifications.NotificationsResponse;
import com.example.marketplace.data.model.order.OrdersResponse;
import com.example.marketplace.data.model.vendor.OrderDocumentResponse;
import com.example.marketplace.data.model.vendor.OrderRequestsResponse;
import com.example.marketplace.data.model.vendor.SalesHistoryResponse;
import com.example.marketplace.data.model.vendor.VendorLotsResponse;
import com.example.marketplace.data.model.vendor.VendorProfileResponse;
import com.example.marketplace.data.model.vendor.VendorSettingsResponse;
import com.example.marketplace.data.model.vendor.RequestActionResponse;
import com.example.marketplace.data.model.settings.ChangePasswordRequest;
import com.example.marketplace.data.model.settings.MessageResponse;
import com.example.marketplace.data.model.settings.OperatorSettingsResponse;
import com.example.marketplace.data.model.settings.TwoFactorSetupResponse;
import com.example.marketplace.data.model.settings.TwoFactorVerifyRequest;
import com.example.marketplace.data.model.vendor.CreateVendorRequest;
import com.example.marketplace.data.model.vendor.RegisteredVendorsResponse;
import com.google.gson.JsonObject;

import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.PUT;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

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

    /** Market tab listings. A null parameter is left out of the request. */
    @GET("lots")
    Call<MarketplaceResponse> getMarketLots(@Query("search") String search,
                                            @Query("materials") String materials,
                                            @Query("region") String region);

    /** Saves the lot for this buyer, or removes it when it was already saved. */
    @POST("lots/{id}/bookmark")
    Call<BookmarkResponse> toggleBookmark(@Path("id") String lotId);

    /** Places an order request on a lot (the seller answers it later). */
    @POST("lots/{id}/offers")
    Call<OrderResponse> placeOrder(@Path("id") String lotId, @Body OrderRequest request);

    /** The signed-in person's conversation threads, newest activity first. */
    @GET("conversations")
    Call<ConversationsResponse> getConversations();

    /** One thread, including the documents shared in it. */
    @GET("conversations/{id}")
    Call<ConversationResponse> getConversation(@Path("id") String conversationId);

    /** Message history of a thread, oldest first. */
    @GET("conversations/{id}/messages")
    Call<MessagesResponse> getMessages(@Path("id") String conversationId);

    @POST("conversations/{id}/messages")
    Call<SendMessageResponse> sendMessage(@Path("id") String conversationId, @Body SendMessageRequest request);

    /** Shares a PDF or image in the thread. The file goes in the part named "file". */
    @Multipart
    @POST("conversations/{id}/documents")
    Call<DocumentsResponse> uploadDocument(@Path("id") String conversationId, @Part MultipartBody.Part file);

    /** Buyer only: all deals of this buyer (there is no single-deal call on the server). */
    @GET("bids")
    Call<BidsResponse> getBids();

    /** Buyer only: accepts the seller's offer and creates the order. */
    @POST("bids/{id}/accept")
    Call<BidActionResponse> acceptBid(@Path("id") String bidId);

    /** Buyer only: answers the deal with a new rate and quantity. */
    @POST("bids/{id}/counter")
    Call<BidActionResponse> counterBid(@Path("id") String bidId, @Body CounterOfferRequest request);

    /** Operator only: every Operator Settings section plus the dropdown choices and account details. */
    @GET("settings")
    Call<OperatorSettingsResponse> getSettings();

    /** Operator only: saves the sections in the body (a JSON object shaped like the settings sections). */
    @PUT("settings")
    Call<OperatorSettingsResponse> updateSettings(@Body JsonObject body);

    /** Changes the signed-in person's password. */
    @PUT("settings/password")
    Call<MessageResponse> changePassword(@Body ChangePasswordRequest request);

    /** Starts 2FA set-up: returns the secret for the authenticator app. */
    @POST("settings/2fa/setup")
    Call<TwoFactorSetupResponse> setUpTwoFactor();

    /** Finishes 2FA set-up with the 6-digit code from the authenticator app. */
    @POST("settings/2fa/verify")
    Call<MessageResponse> verifyTwoFactor(@Body TwoFactorVerifyRequest request);

    /** Changes the signed-in person's password (the account route, open to vendors too). */
    @POST("auth/change-password")
    Call<MessageResponse> changeAccountPassword(@Body ChangePasswordRequest request);

    /** Starts 2FA set-up through the account route (open to vendors too). */
    @POST("auth/2fa/setup")
    Call<TwoFactorSetupResponse> setUpAccountTwoFactor();

    /** Finishes 2FA set-up through the account route (open to vendors too). */
    @POST("auth/2fa/verify")
    Call<MessageResponse> verifyAccountTwoFactor(@Body TwoFactorVerifyRequest request);

    /** Operator only: the registered vendors. A null parameter is left out of the request. */
    @GET("vendors")
    Call<RegisteredVendorsResponse> getRegisteredVendors(@Query("search") String search,
                                                         @Query("status") String status);

    /** Operator only: registers a new vendor. */
    @POST("vendors")
    Call<MessageResponse> createVendor(@Body CreateVendorRequest request);

    /** Vendor only: the order requests (bids and orders) on this vendor's lots. */
    @GET("vendor-portal/orders")
    Call<OrderRequestsResponse> getVendorOrderRequests(@Query("limit") int limit);

    /** Vendor only: accepts a request. */
    @POST("vendor-portal/orders/{id}/accept")
    Call<RequestActionResponse> acceptVendorRequest(@Path("id") String requestId);

    /** Vendor only: rejects a request. */
    @POST("vendor-portal/orders/{id}/reject")
    Call<RequestActionResponse> rejectVendorRequest(@Path("id") String requestId);

    /** Vendor only: the choices for the New Listing dropdowns. */
    @GET("vendor-portal/listings/options")
    Call<ListingOptionsResponse> getListingOptions();

    /** Vendor only: earlier listings to copy. A null search leaves it out. */
    @GET("vendor-portal/listings/templates")
    Call<ListingTemplatesResponse> getListingTemplates(@Query("search") String search);

    /** Vendor only: pickup places used before. */
    @GET("vendor-portal/listings/locations")
    Call<LocationsResponse> getListingLocations();

    /** Vendor only: publishes a new lot on the marketplace. */
    @POST("vendor-portal/listings/publish")
    Call<PublishListingResponse> publishListing(@Body PublishListingRequest request);

    /** Operator only: one registered vendor's profile. */
    @GET("vendors/{id}")
    Call<VendorProfileResponse> getVendorProfile(@Path("id") String vendorId);

    /** Every marketplace listing (the vendor's own are picked out on the phone). */
    @GET("lots")
    Call<VendorLotsResponse> getAllLots();

    /** Operator only: opens (or finds) the conversation with one of their vendors. */
    @POST("conversations/vendor/{vendorId}")
    Call<ConversationResponse> openVendorConversation(@Path("vendorId") String vendorId);

    /** Vendor only: one page of the vendor's orders. Filters go in the map; missing ones are left out. */
    @GET("vendor-portal/sales-history")
    Call<SalesHistoryResponse> getSalesHistory(@QueryMap Map<String, String> filters);

    /** Vendor only: the address of an order's document, for viewing or downloading. */
    @GET("vendor-portal/orders/{id}/document")
    Call<OrderDocumentResponse> getOrderDocument(@Path("id") String orderId, @Query("download") boolean download);

    /** Vendor only: the vendor's whole Settings screen. */
    @GET("vendor-portal/settings")
    Call<VendorSettingsResponse> getVendorSettings();

    /** Vendor only: saves entityName, taxId and headquartersAddress. */
    @PUT("vendor-portal/settings/profile")
    Call<MessageResponse> saveVendorProfile(@Body JsonObject body);

    /** Vendor only: saves yield, capacity, typical purity and the listing defaults. */
    @PUT("vendor-portal/settings/operational-preferences")
    Call<MessageResponse> saveVendorPreferences(@Body JsonObject body);

    /** Vendor only: saves the notification switches. */
    @PUT("vendor-portal/settings/notifications")
    Call<MessageResponse> saveVendorNotifications(@Body JsonObject body);

    /** Vendor only: saves the signed-in user's name and job title. */
    @PUT("vendor-portal/settings/account")
    Call<MessageResponse> saveVendorAccount(@Body JsonObject body);

    /** The signed-in person's newest notifications. */
    @GET("notifications")
    Call<NotificationsResponse> getNotifications();

    /** Marks one notification as read; the body holds its key. */
    @PATCH("notifications/read")
    Call<MessageResponse> markNotificationRead(@Body JsonObject body);

    /** Marks every current notification as read. */
    @PATCH("notifications/read-all")
    Call<MessageResponse> markAllNotificationsRead();
}
