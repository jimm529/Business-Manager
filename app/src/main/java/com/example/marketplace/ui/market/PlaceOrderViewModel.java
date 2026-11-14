package com.example.marketplace.ui.market;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.R;
import com.example.marketplace.data.model.market.LotDetailResponse;
import com.example.marketplace.data.model.market.OrderRequest;
import com.example.marketplace.data.model.market.OrderResponse;
import com.example.marketplace.data.repository.MarketRepository;
import com.example.marketplace.ui.messages.ActionResult;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceLoader;

/**
 * Holds the Place Order page: the lot (loaded fresh from the server), the order being sent, and what
 * the server answered. Survives rotation, so a sent order is never sent twice.
 */
public class PlaceOrderViewModel extends ViewModel {

    /** The server accepted the order: its message and the conversation where the seller will answer. */
    public static final class PlacedOrder {
        @Nullable
        public final String message;
        @Nullable
        public final String conversationId;

        PlacedOrder(@Nullable String message, @Nullable String conversationId) {
            this.message = message;
            this.conversationId = conversationId;
        }
    }

    private final MarketRepository repository = MarketRepository.getInstance();
    private final ResourceLoader<LotDetailResponse> lot = new ResourceLoader<>();
    private final MutableLiveData<Boolean> placing = new MutableLiveData<>(false);
    private final MutableLiveData<PlacedOrder> placed = new MutableLiveData<>();
    private final MutableLiveData<Event<ActionResult>> errors = new MutableLiveData<>();

    @Nullable
    private String lotId;
    private boolean started;

    /** Starts loading the lot. Calling it again (e.g. after rotation) does nothing. */
    public void init(@NonNull String id) {
        if (started) {
            return;
        }
        started = true;
        lotId = id;
        refresh();
    }

    public LiveData<Resource<LotDetailResponse>> getLot() { return lot.asLiveData(); }

    public LiveData<Boolean> isPlacing() { return placing; }

    /** Null until the order was accepted by the server; then the page shows its confirmation. */
    public LiveData<PlacedOrder> getPlaced() { return placed; }

    public LiveData<Event<ActionResult>> getErrors() { return errors; }

    public void refresh() {
        if (lotId != null) {
            lot.load(repository.getLotDetail(lotId));
        }
    }

    /** Sends the order with the buyer's own quantity (metric tons), price per kg and notes. */
    public void placeOrder(double quantityMT, double ratePerKg, @Nullable String notes) {
        if (lotId == null || Boolean.TRUE.equals(placing.getValue()) || placed.getValue() != null) {
            return;
        }
        placing.setValue(true);
        LiveData<Resource<OrderResponse>> call =
                repository.placeOrder(lotId, new OrderRequest(ratePerKg, quantityMT, notes));
        call.observeForever(new Observer<Resource<OrderResponse>>() {
            @Override
            public void onChanged(Resource<OrderResponse> result) {
                if (result.status == Resource.Status.LOADING) {
                    return;
                }
                call.removeObserver(this);
                placing.setValue(false);
                if (result.status == Resource.Status.SUCCESS) {
                    OrderResponse body = result.data;
                    placed.setValue(new PlacedOrder(body == null ? null : body.getMessage(),
                            body == null ? null : body.getConversationId()));
                } else {
                    errors.setValue(new Event<>(ActionResult.failure(result)));
                }
            }
        });
    }

    /** Text for a generic "could not" case, so the page can show something when the server said nothing. */
    @NonNull
    public static ActionResult genericError() {
        return ActionResult.failure(Resource.error(R.string.error_generic));
    }
}
