package com.example.marketplace.ui.messages;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.marketplace.R;
import com.example.marketplace.databinding.FragmentOfferAcceptedBinding;
import com.google.android.material.snackbar.Snackbar;

/**
 * The confirmation after an offer is accepted. Everything it shows arrives from the server's
 * answer through the screen's arguments, so it has nothing of its own to load.
 */
public class OfferAcceptedFragment extends Fragment {

    public static final String ARG_TRANSACTION_ID = "transactionId";
    public static final String ARG_ITEM_DETAIL = "itemDetail";
    public static final String ARG_QUANTITY = "quantity";
    public static final String ARG_AGREED_RATE = "agreedRate";
    public static final String ARG_TOTAL_VALUE = "totalContractValue";
    public static final String ARG_STATUS = "status";
    public static final String ARG_BATCH_NUMBER = "batchNumber";

    private FragmentOfferAcceptedBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOfferAcceptedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments() == null ? new Bundle() : getArguments();

        String batch = args.getString(ARG_BATCH_NUMBER);
        binding.tvSubtitle.setText(TextUtils.isEmpty(batch)
                ? getString(R.string.offer_accepted_subtitle_generic)
                : getString(R.string.offer_accepted_subtitle, batch));

        String transactionId = args.getString(ARG_TRANSACTION_ID);
        binding.tvTransactionId.setText(TextUtils.isEmpty(transactionId)
                ? getString(R.string.no_value) : getString(R.string.offer_transaction_id, transactionId));
        String status = args.getString(ARG_STATUS);
        binding.tvStatus.setText(TextUtils.isEmpty(status) ? getString(R.string.offer_status_default) : status);

        binding.tvItem.setText(orDash(args.getString(ARG_ITEM_DETAIL)));
        binding.tvQuantity.setText(orDash(args.getString(ARG_QUANTITY)));
        binding.tvRate.setText(orDash(args.getString(ARG_AGREED_RATE)));
        binding.tvTotal.setText(orDash(args.getString(ARG_TOTAL_VALUE)));

        // Home becomes the only screen on the stack, like after signing in.
        binding.btnDashboard.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_global_homeFragment));
        binding.btnLogistics.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), R.string.coming_soon, Snackbar.LENGTH_SHORT).show());
    }

    private String orDash(@Nullable String value) {
        return TextUtils.isEmpty(value) ? getString(R.string.no_value) : value;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
