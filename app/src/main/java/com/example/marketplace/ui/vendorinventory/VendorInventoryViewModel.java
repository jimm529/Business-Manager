package com.example.marketplace.ui.vendorinventory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.data.model.messages.Conversation;
import com.example.marketplace.data.model.messages.ConversationResponse;
import com.example.marketplace.data.model.vendor.VendorLot;
import com.example.marketplace.data.model.vendor.VendorLotsResponse;
import com.example.marketplace.data.model.vendor.VendorProfileResponse;
import com.example.marketplace.data.repository.VendorInventoryRepository;
import com.example.marketplace.ui.messages.ActionResult;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Holds one vendor's Inventory page: the vendor's profile, their lots (searchable), whether the
 * lots are shown as cards or as a list, and starting a conversation for a custom quote.
 */
public class VendorInventoryViewModel extends ViewModel {

    private final VendorInventoryRepository repository = VendorInventoryRepository.getInstance();
    private final ResourceLoader<VendorProfileResponse> profile = new ResourceLoader<>();
    private final ResourceLoader<VendorLotsResponse> allLots = new ResourceLoader<>();
    private final MediatorLiveData<Resource<List<VendorLot>>> lots = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> compact = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> openingQuote = new MutableLiveData<>(false);
    private final MutableLiveData<Event<String>> quoteConversation = new MutableLiveData<>();
    private final MutableLiveData<Event<ActionResult>> quoteErrors = new MutableLiveData<>();

    @Nullable
    private String vendorId;
    private boolean started;
    private String query = "";

    public VendorInventoryViewModel() {
        lots.addSource(allLots.asLiveData(), state -> publish());
    }

    /** Starts loading. Calling it again (e.g. after rotation) does nothing. */
    public void init(@NonNull String id) {
        if (started) {
            return;
        }
        started = true;
        vendorId = id;
        refresh();
    }

    public LiveData<Resource<VendorProfileResponse>> getProfile() { return profile.asLiveData(); }

    /** This vendor's lots that match the search. */
    public LiveData<Resource<List<VendorLot>>> getLots() { return lots; }

    /** True when the lots are shown as a compact list instead of big cards. */
    public LiveData<Boolean> isCompact() { return compact; }

    public LiveData<Boolean> isOpeningQuote() { return openingQuote; }

    /** The conversation to open after "Request Custom Quote". */
    public LiveData<Event<String>> getQuoteConversation() { return quoteConversation; }

    public LiveData<Event<ActionResult>> getQuoteErrors() { return quoteErrors; }

    @NonNull
    public String getQuery() { return query; }

    public void setCompact(boolean value) { compact.setValue(value); }

    public void setQuery(@Nullable String text) {
        String clean = text == null ? "" : text.trim();
        if (!clean.equals(query)) {
            query = clean;
            publish();
        }
    }

    public void refresh() {
        if (vendorId == null) {
            return;
        }
        profile.load(repository.getProfile(vendorId));
        allLots.load(repository.getLots());
    }

    /** Finds (or starts) the conversation with this vendor so a custom quote can be asked for in chat. */
    public void requestQuote() {
        if (vendorId == null || Boolean.TRUE.equals(openingQuote.getValue())) {
            return;
        }
        openingQuote.setValue(true);
        LiveData<Resource<ConversationResponse>> call = repository.openConversation(vendorId);
        call.observeForever(new Observer<Resource<ConversationResponse>>() {
            @Override
            public void onChanged(Resource<ConversationResponse> result) {
                if (result.status == Resource.Status.LOADING) {
                    return;
                }
                call.removeObserver(this);
                openingQuote.setValue(false);
                Conversation conversation = result.data == null ? null : result.data.getConversation();
                if (result.status == Resource.Status.SUCCESS && conversation != null && conversation.getId() != null) {
                    quoteConversation.setValue(new Event<>(conversation.getId()));
                } else {
                    quoteErrors.setValue(new Event<>(ActionResult.failure(
                            result.status == Resource.Status.SUCCESS ? Resource.error(com.example.marketplace.R.string.error_generic) : result)));
                }
            }
        });
    }

    private void publish() {
        Resource<VendorLotsResponse> state = allLots.asLiveData().getValue();
        if (state == null || vendorId == null) {
            return;
        }
        if (state.data == null) {
            lots.setValue(state.withData(null));
            return;
        }
        List<VendorLot> mine = state.data.getLotsOf(vendorId);
        String needle = query.toLowerCase(Locale.getDefault());
        List<VendorLot> shown = new ArrayList<>();
        for (VendorLot lot : mine) {
            if (needle.isEmpty() || lot.matches(needle)) {
                shown.add(lot);
            }
        }
        lots.setValue(state.withData(shown));
    }
}
