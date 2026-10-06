package com.example.marketplace.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.marketplace.data.api.ApiClient;
import com.example.marketplace.data.model.market.BookmarkResponse;
import com.example.marketplace.data.model.market.MarketplaceResponse;
import com.example.marketplace.data.model.market.OrderRequest;
import com.example.marketplace.data.model.market.OrderResponse;
import com.example.marketplace.utils.Resource;

/** Everything the Market tab asks the server: the listings, saving a lot and placing an order. */
public class MarketRepository {

    private static MarketRepository instance;

    private MarketRepository() { }

    public static synchronized MarketRepository getInstance() {
        if (instance == null) {
            instance = new MarketRepository();
        }
        return instance;
    }

    /** Listings (GET lots). Empty text and null mean "no filter". */
    public LiveData<Resource<MarketplaceResponse>> getLots(@Nullable String search, @Nullable String material,
                                                           @Nullable String region) {
        return ApiResults.enqueue(ApiClient.getApiService()
                .getMarketLots(clean(search), clean(material), clean(region)));
    }

    /** Toggles the saved state of one lot (POST lots/{id}/bookmark). */
    public LiveData<Resource<BookmarkResponse>> toggleBookmark(@NonNull String lotId) {
        return ApiResults.enqueue(ApiClient.getApiService().toggleBookmark(lotId));
    }

    /** Orders the lot at its listed price and quantity (POST lots/{id}/offers). */
    public LiveData<Resource<OrderResponse>> placeOrder(@NonNull String lotId) {
        return ApiResults.enqueue(ApiClient.getApiService().placeOrder(lotId, new OrderRequest()));
    }

    @Nullable
    private static String clean(@Nullable String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }
}
