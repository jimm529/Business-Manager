package com.example.marketplace.ui.messages;

import android.content.ContentResolver;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.R;
import com.example.marketplace.data.api.UriRequestBody;
import com.example.marketplace.data.model.bid.BidsResponse;
import com.example.marketplace.data.model.bid.CounterOfferRequest;
import com.example.marketplace.data.model.bid.NegotiationDetail;
import com.example.marketplace.data.model.bid.NegotiationDocument;
import com.example.marketplace.data.model.bid.TransactionConfirmation;
import com.example.marketplace.data.model.messages.ConversationResponse;
import com.example.marketplace.data.repository.BidsRepository;
import com.example.marketplace.data.repository.MessagesRepository;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import okhttp3.MultipartBody;

/**
 * Holds one deal for the Counter Price screen and its counter-offer sheet: the deal, the documents
 * shared in the thread, and accept / counter / upload. Survives rotation.
 */
public class NegotiationViewModel extends ViewModel {

    private final BidsRepository bids = BidsRepository.getInstance();
    private final MessagesRepository messagesRepository = MessagesRepository.getInstance();

    private final ResourceLoader<BidsResponse> bidLoader = new ResourceLoader<>();
    private final ResourceLoader<ConversationResponse> conversationLoader = new ResourceLoader<>();
    private final MediatorLiveData<Resource<NegotiationDetail>> bid = new MediatorLiveData<>();
    private final MediatorLiveData<Resource<List<NegotiationDocument>>> documents = new MediatorLiveData<>();

    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    /** Messages for the Counter Price screen (accept failed, document shared...). */
    private final MutableLiveData<Event<ActionResult>> actionResults = new MutableLiveData<>();
    /** The answer to a counter offer, for the sheet that sent it (stays open on an error). */
    private final MutableLiveData<Event<ActionResult>> counterOutcome = new MutableLiveData<>();
    private final MutableLiveData<Event<TransactionConfirmation>> accepted = new MutableLiveData<>();

    @Nullable
    private String bidId;
    @Nullable
    private String conversationId;
    private boolean started;

    public NegotiationViewModel() {
        bid.addSource(bidLoader.asLiveData(), this::publishBid);
        documents.addSource(conversationLoader.asLiveData(), this::publishDocuments);
    }

    /** Starts loading. Calling it again (e.g. after rotation) does nothing. */
    public void init(@NonNull String bidId, @Nullable String conversationId) {
        if (started) {
            return;
        }
        started = true;
        this.bidId = bidId;
        this.conversationId = conversationId;
        refresh();
    }

    public LiveData<Resource<NegotiationDetail>> getBid() {
        return bid;
    }

    public LiveData<Resource<List<NegotiationDocument>>> getDocuments() {
        return documents;
    }

    /** True while accept, counter or upload is running. */
    public LiveData<Boolean> isBusy() {
        return busy;
    }

    public LiveData<Event<ActionResult>> getActionResults() {
        return actionResults;
    }

    public LiveData<Event<ActionResult>> getCounterOutcome() {
        return counterOutcome;
    }

    public LiveData<Event<TransactionConfirmation>> getAccepted() {
        return accepted;
    }

    @Nullable
    public NegotiationDetail currentBid() {
        Resource<NegotiationDetail> state = bid.getValue();
        return state == null ? null : state.data;
    }

    /** The conversation the deal belongs to (needed to upload a document). */
    @Nullable
    public String getConversationId() {
        return conversationId;
    }

    public void refresh() {
        bidLoader.load(bids.getBids());
        refreshDocuments();
    }

    public void refreshDocuments() {
        if (conversationId == null) {
            documents.setValue(Resource.success(new ArrayList<>()));
        } else {
            conversationLoader.load(messagesRepository.getConversation(conversationId));
        }
    }

    // ---------------------------------------------------------------- actions

    public void accept() {
        NegotiationDetail current = currentBid();
        if (current == null || current.getBidId() == null || isRunning()) {
            return;
        }
        busy.setValue(true);
        runOnce(bids.accept(current.getBidId()), result -> {
            busy.setValue(false);
            if (result.status == Resource.Status.SUCCESS && result.data != null) {
                refresh();
                TransactionConfirmation transaction = result.data.getTransaction();
                if (transaction != null) {
                    accepted.setValue(new Event<>(transaction));
                } else {
                    actionResults.setValue(new Event<>(
                            ActionResult.success(result.data.getMessage(), R.string.negotiation_accepted)));
                }
            } else {
                // The other side may have changed the deal: show the latest and say why it failed.
                refresh();
                actionResults.setValue(new Event<>(ActionResult.failure(result)));
            }
        });
    }

    /** Sends the counter offer: new rate per kg, new quantity in metric tons, and notes. */
    public void counter(double rate, double quantityMT, @Nullable String notes) {
        NegotiationDetail current = currentBid();
        if (current == null || current.getBidId() == null || isRunning()) {
            return;
        }
        busy.setValue(true);
        CounterOfferRequest request = new CounterOfferRequest(rate, quantityMT, notes, current.getUpdatedAt());
        runOnce(bids.counter(current.getBidId(), request), result -> {
            busy.setValue(false);
            refresh();
            if (result.status == Resource.Status.SUCCESS) {
                String serverMessage = result.data == null ? null : result.data.getMessage();
                ActionResult success = ActionResult.success(serverMessage, R.string.negotiation_counter_sent);
                counterOutcome.setValue(new Event<>(success));
                actionResults.setValue(new Event<>(success));
            } else {
                counterOutcome.setValue(new Event<>(ActionResult.failure(result)));
            }
        });
    }

    /** Shares the picked file (PDF or image) in the thread. The file is read while it uploads. */
    public void uploadDocument(@NonNull ContentResolver resolver, @NonNull Uri uri, @NonNull String fileName,
                               @Nullable String mimeType) {
        if (conversationId == null || isRunning()) {
            return;
        }
        busy.setValue(true);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", fileName,
                new UriRequestBody(resolver, uri, mimeType));
        runOnce(messagesRepository.uploadDocument(conversationId, part), result -> {
            busy.setValue(false);
            if (result.status == Resource.Status.SUCCESS) {
                refreshDocuments();
                String serverMessage = result.data == null ? null : result.data.getMessage();
                actionResults.setValue(new Event<>(ActionResult.success(serverMessage, R.string.document_uploaded)));
            } else {
                actionResults.setValue(new Event<>(ActionResult.failure(result)));
            }
        });
    }

    // ---------------------------------------------------------------- internals

    private boolean isRunning() {
        return Boolean.TRUE.equals(busy.getValue());
    }

    private void publishBid(Resource<BidsResponse> state) {
        NegotiationDetail found = null;
        if (state.data != null) {
            for (NegotiationDetail candidate : state.data.getBids()) {
                if (Objects.equals(candidate.getBidId(), bidId)) {
                    found = candidate;
                    break;
                }
            }
        }
        if (state.status == Resource.Status.SUCCESS && found == null) {
            bid.setValue(Resource.error(R.string.negotiation_not_found));
        } else {
            bid.setValue(state.withData(found));
        }
    }

    private void publishDocuments(Resource<ConversationResponse> state) {
        List<NegotiationDocument> list = null;
        if (state.data != null && state.data.getConversation() != null) {
            list = state.data.getConversation().getDocuments();
        }
        documents.setValue(state.withData(list));
    }

    /** Waits for the one final answer of a call (ignoring "loading") and hands it over. */
    private static <T> void runOnce(LiveData<Resource<T>> call, Consumer<Resource<T>> onDone) {
        call.observeForever(new Observer<Resource<T>>() {
            @Override
            public void onChanged(Resource<T> result) {
                if (result.status == Resource.Status.LOADING) {
                    return;
                }
                call.removeObserver(this);
                onDone.accept(result);
            }
        });
    }
}
