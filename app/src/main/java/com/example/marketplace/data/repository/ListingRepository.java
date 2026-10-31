package com.example.marketplace.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.marketplace.data.api.ApiClient;
import com.example.marketplace.data.model.listing.ListingOptionsResponse;
import com.example.marketplace.data.model.listing.ListingTemplatesResponse;
import com.example.marketplace.data.model.listing.LocationsResponse;
import com.example.marketplace.data.model.listing.PublishListingRequest;
import com.example.marketplace.data.model.listing.PublishListingResponse;
import com.example.marketplace.utils.Resource;

/** The vendor's New Listing form: dropdown choices, earlier listings to copy, pickup places, and publishing. */
public class ListingRepository {

    private static ListingRepository instance;

    private ListingRepository() { }

    public static synchronized ListingRepository getInstance() {
        if (instance == null) {
            instance = new ListingRepository();
        }
        return instance;
    }

    /** GET vendor-portal/listings/options. */
    public LiveData<Resource<ListingOptionsResponse>> getOptions() {
        return ApiResults.enqueue(ApiClient.getApiService().getListingOptions());
    }

    /** GET vendor-portal/listings/templates. Empty text means "all recent". */
    public LiveData<Resource<ListingTemplatesResponse>> getTemplates(@Nullable String search) {
        String clean = search == null || search.trim().isEmpty() ? null : search.trim();
        return ApiResults.enqueue(ApiClient.getApiService().getListingTemplates(clean));
    }

    /** GET vendor-portal/listings/locations. */
    public LiveData<Resource<LocationsResponse>> getLocations() {
        return ApiResults.enqueue(ApiClient.getApiService().getListingLocations());
    }

    /** POST vendor-portal/listings/publish. */
    public LiveData<Resource<PublishListingResponse>> publish(@NonNull PublishListingRequest request) {
        return ApiResults.enqueue(ApiClient.getApiService().publishListing(request));
    }
}
