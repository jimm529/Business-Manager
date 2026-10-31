package com.example.marketplace.ui.listing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.R;
import com.example.marketplace.data.model.listing.ListingOptionsResponse;
import com.example.marketplace.data.model.listing.ListingTemplatesResponse;
import com.example.marketplace.data.model.listing.LocationsResponse;
import com.example.marketplace.data.model.listing.PublishListingRequest;
import com.example.marketplace.data.model.listing.PublishListingResponse;
import com.example.marketplace.data.repository.ListingRepository;
import com.example.marketplace.ui.messages.ActionResult;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceLoader;

/**
 * Holds the New Listing form's data so it survives rotation: the dropdown choices from the server,
 * earlier listings to copy, pickup suggestions, which choices are selected, and publishing.
 * Typed text lives in the form's own fields; only the selections (which need a stored value) live here.
 */
public class ListingFormViewModel extends ViewModel {

    private final ListingRepository repository = ListingRepository.getInstance();
    private final ResourceLoader<ListingOptionsResponse> options = new ResourceLoader<>();
    private final ResourceLoader<ListingTemplatesResponse> templates = new ResourceLoader<>();
    private final ResourceLoader<LocationsResponse> locations = new ResourceLoader<>();
    private final MutableLiveData<Boolean> publishing = new MutableLiveData<>(false);
    private final MutableLiveData<Event<ActionResult>> publishResults = new MutableLiveData<>();

    // The chosen dropdown values (what the server stores, not the label) and the date (UTC midnight).
    @Nullable private String materialValue;
    @Nullable private String packagingValue;
    @Nullable private String regionValue;
    @Nullable private String incotermValue;
    @Nullable private Long availabilityUtcMillis;
    private String templateSearch = "";

    public ListingFormViewModel() {
        loadOptions();
        loadLocations();
        loadTemplates();
    }

    public LiveData<Resource<ListingOptionsResponse>> getOptions() { return options.asLiveData(); }

    public LiveData<Resource<ListingTemplatesResponse>> getTemplates() { return templates.asLiveData(); }

    public LiveData<Resource<LocationsResponse>> getLocations() { return locations.asLiveData(); }

    public LiveData<Boolean> isPublishing() { return publishing; }

    /** One-time answer of Publish: success (the form is cleared) or the error to show. */
    public LiveData<Event<ActionResult>> getPublishResults() { return publishResults; }

    public void loadOptions() { options.load(repository.getOptions()); }

    public void loadLocations() { locations.load(repository.getLocations()); }

    public void loadTemplates() { templates.load(repository.getTemplates(templateSearch)); }

    /** Searches the earlier listings by text. Empty text shows the recent ones again. */
    public void searchTemplates(@Nullable String text) {
        String clean = text == null ? "" : text.trim();
        if (!clean.equals(templateSearch)) {
            templateSearch = clean;
            loadTemplates();
        }
    }

    // ---------------------------------------------------------------- selections

    @Nullable public String getMaterialValue() { return materialValue; }
    @Nullable public String getPackagingValue() { return packagingValue; }
    @Nullable public String getRegionValue() { return regionValue; }
    @Nullable public String getIncotermValue() { return incotermValue; }
    @Nullable public Long getAvailabilityUtcMillis() { return availabilityUtcMillis; }

    public void setMaterialValue(@Nullable String value) { materialValue = value; }
    public void setPackagingValue(@Nullable String value) { packagingValue = value; }
    public void setRegionValue(@Nullable String value) { regionValue = value; }
    public void setIncotermValue(@Nullable String value) { incotermValue = value; }
    public void setAvailabilityUtcMillis(@Nullable Long millis) { availabilityUtcMillis = millis; }

    /** Forgets every selection (after publishing or discarding). */
    public void clearSelections() {
        materialValue = null;
        packagingValue = null;
        regionValue = null;
        incotermValue = null;
        availabilityUtcMillis = null;
    }

    // ---------------------------------------------------------------- publish

    public void publish(@NonNull PublishListingRequest request) {
        if (Boolean.TRUE.equals(publishing.getValue())) {
            return;
        }
        publishing.setValue(true);
        LiveData<Resource<PublishListingResponse>> call = repository.publish(request);
        call.observeForever(new Observer<Resource<PublishListingResponse>>() {
            @Override
            public void onChanged(Resource<PublishListingResponse> result) {
                if (result.status == Resource.Status.LOADING) {
                    return;
                }
                call.removeObserver(this);
                publishing.setValue(false);
                if (result.status == Resource.Status.SUCCESS) {
                    String message = result.data == null ? null : result.data.getMessage();
                    publishResults.setValue(new Event<>(ActionResult.success(message, R.string.listing_published)));
                } else {
                    publishResults.setValue(new Event<>(ActionResult.failure(result)));
                }
            }
        });
    }
}
