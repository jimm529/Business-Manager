package com.example.marketplace.ui.messages;

import android.graphics.Paint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.marketplace.R;
import com.example.marketplace.data.model.bid.NegotiationDetail;
import com.example.marketplace.databinding.BottomSheetCounterOfferBinding;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Formatters;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.math.BigDecimal;

/**
 * The "Submit Counter Offer" sheet. Typing a rate or quantity updates the new contract total
 * (rate x quantity x 1000 kg per metric ton) right away. It shares the Counter Price screen's
 * view model, so Counter sends the offer and closes the sheet when the server accepts it.
 */
public class SubmitCounterOfferBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "SubmitCounterOfferBottomSheet";
    private static final int MAX_NOTES = 500;

    private BottomSheetCounterOfferBinding binding;
    private NegotiationViewModel viewModel;
    @Nullable
    private NegotiationDetail bid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetCounterOfferBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireParentFragment()).get(NegotiationViewModel.class);
        bid = viewModel.currentBid();
        if (bid == null) {
            dismiss();
            return;
        }

        String noValue = getString(R.string.no_value);
        String batch = bid.getBatchNumber();
        binding.tvSubtitle.setText(batch == null ? noValue : getString(R.string.counter_sheet_subtitle, batch));
        String currentRate = Formatters.currency(bid.getCurrentRatePerKg(), bid.getCurrency(), 2);
        binding.tvCurrentRate.setText(getString(R.string.counter_rate_current, currentRate == null ? noValue : currentRate));

        String previous = Formatters.currency(bid.getTotalValue(), bid.getCurrency(), 2);
        binding.tvPreviousTotal.setText(previous == null ? noValue : previous);
        binding.tvPreviousTotal.setPaintFlags(binding.tvPreviousTotal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        // After a rotation the typed values are restored by the system; only fill an empty form.
        if (savedInstanceState == null) {
            binding.etRate.setText(plain(bid.getCurrentRatePerKg()));
            binding.etQuantity.setText(plain(bid.getQuantityMT()));
        }
        TextWatcher recalculate = new SimpleWatcher(this::updateTotal);
        binding.etRate.addTextChangedListener(recalculate);
        binding.etQuantity.addTextChangedListener(recalculate);
        updateTotal();

        binding.btnClose.setOnClickListener(v -> dismiss());
        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSubmit.setOnClickListener(v -> submit());

        viewModel.isBusy().observe(getViewLifecycleOwner(), busy -> {
            boolean running = Boolean.TRUE.equals(busy);
            binding.btnSubmit.setEnabled(!running);
            binding.btnCancel.setEnabled(!running);
        });
        viewModel.getCounterOutcome().observe(getViewLifecycleOwner(), this::handleOutcome);
    }

    private void updateTotal() {
        Double rate = parse(binding.etRate.getText());
        Double quantity = parse(binding.etQuantity.getText());
        if (rate == null || quantity == null || bid == null) {
            binding.tvNewTotal.setText(R.string.no_value);
            return;
        }
        String total = Formatters.currency(rate * quantity * 1000d, bid.getCurrency(), 2);
        binding.tvNewTotal.setText(total == null ? getString(R.string.no_value) : total);
    }

    private void submit() {
        binding.tvError.setVisibility(View.GONE);
        Double rate = parse(binding.etRate.getText());
        Double quantity = parse(binding.etQuantity.getText());
        String notes = binding.etNotes.getText() == null ? "" : binding.etNotes.getText().toString().trim();

        boolean valid = true;
        binding.tilRate.setError(rate == null || rate <= 0 ? getString(R.string.counter_error_rate) : null);
        binding.tilQuantity.setError(quantity == null || quantity <= 0 ? getString(R.string.counter_error_quantity) : null);
        binding.tilNotes.setError(notes.length() > MAX_NOTES ? getString(R.string.counter_error_notes) : null);
        if (rate == null || rate <= 0 || quantity == null || quantity <= 0 || notes.length() > MAX_NOTES) {
            valid = false;
        }
        if (valid) {
            viewModel.counter(rate, quantity, notes);
        }
    }

    private void handleOutcome(@Nullable Event<ActionResult> event) {
        ActionResult result = event == null ? null : event.getContentIfNotHandled();
        if (result == null) {
            return;
        }
        if (result.isError()) {
            // Stay open so the person can fix the numbers and try again.
            binding.tvError.setText(result.text(requireContext()));
            binding.tvError.setVisibility(View.VISIBLE);
        } else {
            dismiss();
        }
    }

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

    /** 1.92 -> "1.92", 40.0 -> "40": no grouping, no trailing zeros, so it can be edited. */
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
