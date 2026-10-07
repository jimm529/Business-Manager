package com.example.marketplace.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.marketplace.data.api.ApiClient;
import com.example.marketplace.data.model.bid.BidActionResponse;
import com.example.marketplace.data.model.bid.BidsResponse;
import com.example.marketplace.data.model.bid.CounterOfferRequest;
import com.example.marketplace.utils.Resource;

/** The buyer's deals: read them, accept the seller's offer, or counter it. */
public class BidsRepository {

    private static BidsRepository instance;

    private BidsRepository() { }

    public static synchronized BidsRepository getInstance() {
        if (instance == null) {
            instance = new BidsRepository();
        }
        return instance;
    }

    /** GET bids. The server has no "one deal" call, so a deal is picked out of this list. */
    public LiveData<Resource<BidsResponse>> getBids() {
        return ApiResults.enqueue(ApiClient.getApiService().getBids());
    }

    /** POST bids/{id}/accept. */
    public LiveData<Resource<BidActionResponse>> accept(@NonNull String bidId) {
        return ApiResults.enqueue(ApiClient.getApiService().acceptBid(bidId));
    }

    /** POST bids/{id}/counter. */
    public LiveData<Resource<BidActionResponse>> counter(@NonNull String bidId, @NonNull CounterOfferRequest request) {
        return ApiResults.enqueue(ApiClient.getApiService().counterBid(bidId, request));
    }
}
