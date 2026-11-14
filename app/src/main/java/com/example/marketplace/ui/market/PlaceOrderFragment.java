package com.example.marketplace.ui.market;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.marketplace.R;
import com.example.marketplace.data.model.market.LotDetailResponse;
import com.example.marketplace.databinding.FragmentPlaceOrderBinding;
import com.example.marketplace.ui.messages.ActionResult;
import com.example.marketplace.ui.messages.ChatFragment;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Formatters;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.google.android.material.snackbar.Snackbar;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * The full Place Order page: the lot, the buyer's own quantity, price and notes, a live total, and a
 * confirmation once the seller's request is sent. The numbers are checked before anything is sent.
 */
public class PlaceOrderFragment extends Fragment {

    public static final String ARG_LOT_ID = "lotId";

    private static final int MAX_NOTES = 500;

    private FragmentPlaceOrderBinding binding;
    private PlaceOrderViewModel viewModel;

    @Nullable
    private LotDetailResponse.Lot lot;
    @Nullable
    private String sellerName;
    /** The boxes are filled with the lot's own quantity and price only the first time, not after a rotation. */
    private boolean allowPrefill;
    private boolean prefilled;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlaceOrderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        String lotId = args == null ? null : args.getString(ARG_LOT_ID);
        if (lotId == null) {
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }
        allowPrefill = savedInstanceState == null;
        viewModel = new ViewModelProvider(this).get(PlaceOrderViewModel.class);
        viewModel.init(lotId);

        binding.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        binding.btnCancel.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        binding.btnBackToMarket.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        binding.states.btnRetry.setOnClickListener(v -> viewModel.refresh());
        binding.btnPlace.setOnClickListener(v -> submit());
        binding.btnOpenChat.setOnClickListener(v -> openChat());

        Runnable recalculate = this::updateTotals;
        binding.etQuantity.addTextChangedListener(new SimpleWatcher(() -> {
            binding.tilQuantity.setError(null);
            recalculate.run();
        }));
        binding.etPrice.addTextChangedListener(new SimpleWatcher(() -> {
            binding.tilPrice.setError(null);
            recalculate.run();
        }));
        binding.etNotes.addTextChangedListener(new SimpleWatcher(() -> binding.tilNotes.setError(null)));

        viewModel.getLot().observe(getViewLifecycleOwner(), this::renderLot);
        viewModel.isPlacing().observe(getViewLifecycleOwner(), placing -> updateButtons());
        viewModel.getPlaced().observe(getViewLifecycleOwner(), this::renderPlaced);
        viewModel.getErrors().observe(getViewLifecycleOwner(), this::showError);
    }

    // ---------------------------------------------------------------- the lot

    private void renderLot(Resource<LotDetailResponse> resource) {
        if (viewModel.getPlaced().getValue() != null) {
            return;
        }
        switch (resource.status) {
            case LOADING:
                hideStates();
                if (resource.data == null) {
                    binding.content.setVisibility(View.GONE);
                    binding.states.progress.setVisibility(View.VISIBLE);
                }
                break;
            case SUCCESS:
                hideStates();
                if (resource.data == null || resource.data.getLot() == null) {
                    binding.content.setVisibility(View.GONE);
                    binding.states.tvEmpty.setText(R.string.po_lot_missing);
                    binding.states.emptyGroup.setVisibility(View.VISIBLE);
                } else {
                    binding.content.setVisibility(View.VISIBLE);
                    bindLot(resource.data);
                }
                break;
            case ERROR:
                hideStates();
                binding.content.setVisibility(View.GONE);
                binding.states.tvError.setText(ResourceMessages.errorText(requireContext(), resource));
                binding.states.errorGroup.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void hideStates() {
        binding.states.progress.setVisibility(View.GONE);
        binding.states.emptyGroup.setVisibility(View.GONE);
        binding.states.errorGroup.setVisibility(View.GONE);
    }

    private void bindLot(@NonNull LotDetailResponse response) {
        LotDetailResponse.Lot current = response.getLot();
        lot = current;
        String noValue = getString(R.string.no_value);

        String title = current.getTitle();
        binding.tvTitle.setText(title == null || title.trim().isEmpty() ? getString(R.string.market_untitled_lot) : title);
        String location = current.getLocation();
        binding.tvLocation.setText(location == null || location.trim().isEmpty() ? noValue : location);

        LotDetailResponse.Seller seller = response.getSeller();
        sellerName = seller == null ? null : seller.getName();
        if (sellerName == null || sellerName.trim().isEmpty()) {
            binding.tvSeller.setVisibility(View.GONE);
        } else {
            binding.tvSeller.setVisibility(View.VISIBLE);
            binding.tvSeller.setText(seller.isVerified()
                    ? getString(R.string.po_seller_verified, sellerName) : getString(R.string.po_seller, sellerName));
        }

        // Status badge: green while it can be ordered, grey once sold or reserved.
        boolean closed = current.isClosed();
        String status = current.getStatus();
        binding.tvStatus.setText(status == null || status.trim().isEmpty()
                ? getString(R.string.market_status_unknown) : status.trim().toUpperCase(Locale.getDefault()));
        binding.tvStatus.setBackgroundResource(closed
                ? R.drawable.bg_badge_market_neutral : R.drawable.bg_badge_market_ready);
        binding.tvStatus.setTextColor(requireContext().getColor(closed
                ? R.color.marketBadgeNeutralText : R.color.marketBadgeReadyText));
        binding.tvClosed.setVisibility(closed ? View.VISIBLE : View.GONE);

        String quantity = Formatters.number(current.getQuantity());
        String unit = current.getUnit() == null || current.getUnit().trim().isEmpty()
                ? getString(R.string.requests_unit_mt) : current.getUnit();
        binding.tvAvailable.setText(quantity == null ? noValue : getString(R.string.order_quantity, quantity, unit));
        binding.tvQuantityHint.setText(quantity == null ? "" : getString(R.string.po_quantity_hint, quantity));
        String purity = Formatters.number(current.getPurity());
        binding.tvPurity.setText(purity == null ? noValue : getString(R.string.vendor_percent, purity));
        String asking = Formatters.currency(current.getPricePerKg(), current.getCurrency(), 2);
        binding.tvAsking.setText(asking == null ? noValue : getString(R.string.price_per_kg, asking));
        binding.tvPriceHint.setText(asking == null ? "" : getString(R.string.po_price_hint, asking));

        // Start from the lot's own quantity and price; the buyer then changes what they want.
        if (allowPrefill && !prefilled) {
            prefilled = true;
            binding.etQuantity.setText(plain(current.getQuantity()));
            binding.etPrice.setText(plain(current.getPricePerKg()));
        }
        updateTotals();
        updateButtons();
    }

    // ---------------------------------------------------------------- totals and sending

    private void updateTotals() {
        if (binding == null) {
            return;
        }
        String noValue = getString(R.string.no_value);
        Double quantity = parse(binding.etQuantity.getText());
        Double rate = parse(binding.etPrice.getText());
        String currency = lot == null ? null : lot.getCurrency();
        if (quantity == null || rate == null) {
            binding.tvTotal.setText(noValue);
        } else {
            String total = Formatters.currency(quantity * 1000d * rate, currency, 2);
            binding.tvTotal.setText(total == null ? noValue : total);
        }
        Double askingRate = lot == null ? null : lot.getPricePerKg();
        if (quantity == null || askingRate == null) {
            binding.tvAskingTotal.setText(noValue);
        } else {
            String askingTotal = Formatters.currency(quantity * 1000d * askingRate, currency, 2);
            binding.tvAskingTotal.setText(askingTotal == null ? noValue : askingTotal);
        }
    }

    private void updateButtons() {
        if (binding == null) {
            return;
        }
        boolean placing = Boolean.TRUE.equals(viewModel.isPlacing().getValue());
        boolean canOrder = lot != null && !lot.isClosed();
        binding.btnPlace.setEnabled(canOrder && !placing);
        binding.btnCancel.setEnabled(!placing);
    }

    private void submit() {
        if (lot == null || lot.isClosed()) {
            return;
        }
        Double quantity = parse(binding.etQuantity.getText());
        Double rate = parse(binding.etPrice.getText());
        String notes = binding.etNotes.getText() == null ? "" : binding.etNotes.getText().toString().trim();
        Double available = lot.getQuantity();

        boolean quantityBad = quantity == null || quantity <= 0;
        boolean overAvailable = !quantityBad && available != null && quantity > available;
        boolean rateBad = rate == null || rate <= 0;
        boolean notesBad = notes.length() > MAX_NOTES;

        if (quantityBad) {
            binding.tilQuantity.setError(getString(R.string.po_error_quantity));
        } else if (overAvailable) {
            binding.tilQuantity.setError(getString(R.string.po_error_quantity_max, Formatters.number(available)));
        } else {
            binding.tilQuantity.setError(null);
        }
        binding.tilPrice.setError(rateBad ? getString(R.string.po_error_price) : null);
        binding.tilNotes.setError(notesBad ? getString(R.string.po_error_notes) : null);
        if (quantityBad || overAvailable || rateBad || notesBad) {
            return;
        }
        viewModel.placeOrder(quantity, rate, notes);
    }

    // ---------------------------------------------------------------- the confirmation

    private void renderPlaced(@Nullable PlaceOrderViewModel.PlacedOrder placed) {
        if (placed == null) {
            return;
        }
        hideStates();
        binding.content.setVisibility(View.GONE);
        binding.successGroup.setVisibility(View.VISIBLE);
        binding.tvSuccessMessage.setText(placed.message == null || placed.message.trim().isEmpty()
                ? getString(R.string.po_success_message) : placed.message);
        binding.btnOpenChat.setVisibility(placed.conversationId == null ? View.GONE : View.VISIBLE);
    }

    private void openChat() {
        PlaceOrderViewModel.PlacedOrder placed = viewModel.getPlaced().getValue();
        if (placed == null || placed.conversationId == null) {
            return;
        }
        Bundle args = new Bundle();
        args.putString(ChatFragment.ARG_CONVERSATION_ID, placed.conversationId);
        args.putString(ChatFragment.ARG_PARTICIPANT_NAME, sellerName);
        NavHostFragment.findNavController(this).navigate(R.id.action_placeOrder_to_chat, args);
    }

    private void showError(@Nullable Event<ActionResult> event) {
        ActionResult result = event == null ? null : event.getContentIfNotHandled();
        if (result != null) {
            Snackbar.make(binding.getRoot(), result.text(requireContext()), Snackbar.LENGTH_LONG).show();
        }
    }

    // ---------------------------------------------------------------- helpers

    /** Reads a typed number; a comma counts as the decimal point. Null when empty or not a number. */
    @Nullable
    private static Double parse(@Nullable CharSequence text) {
        if (text == null || text.toString().trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(text.toString().trim().replace(',', '.'));
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }

    /** 2.15 -> "2.15", 25.0 -> "25": no grouping, no trailing zeros, so it can be edited. */
    private static String plain(@Nullable Double value) {
        return value == null ? "" : BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /** A text watcher that only cares that the text changed. */
    private static class SimpleWatcher implements TextWatcher {
        private final Runnable onChange;

        SimpleWatcher(Runnable onChange) {
            this.onChange = onChange;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            onChange.run();
        }
    }
}
