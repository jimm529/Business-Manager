package com.example.marketplace.ui.messages;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.marketplace.R;
import com.example.marketplace.data.model.bid.NegotiationDetail;
import com.example.marketplace.data.model.bid.NegotiationDocument;
import com.example.marketplace.data.model.bid.TransactionConfirmation;
import com.example.marketplace.databinding.FragmentCounterPriceBinding;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Formatters;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.example.marketplace.utils.Roles;
import com.example.marketplace.utils.SessionManager;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/**
 * The active negotiation for one deal: its details, Accept / Counter / Upload buttons, and the
 * documents shared in the thread. Pull down to reload.
 */
public class CounterPriceFragment extends Fragment {

    public static final String ARG_BID_ID = "bidId";
    public static final String ARG_CONVERSATION_ID = "conversationId";
    public static final String ARG_BATCH_NUMBER = "batchNumber";

    private static final String[] UPLOAD_TYPES = {"application/pdf", "image/jpeg", "image/png"};

    private FragmentCounterPriceBinding binding;
    private NegotiationViewModel viewModel;
    private DocumentsAdapter documentsAdapter;

    private final ActivityResultLauncher<String[]> pickDocument =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onDocumentPicked);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCounterPriceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        String bidId = args == null ? null : args.getString(ARG_BID_ID);
        if (bidId == null) {
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }
        viewModel = new ViewModelProvider(this).get(NegotiationViewModel.class);
        viewModel.init(bidId, args.getString(ARG_CONVERSATION_ID));

        setUpHeader();
        documentsAdapter = new DocumentsAdapter(this::download);
        binding.rvDocuments.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvDocuments.setAdapter(documentsAdapter);

        binding.swipeRefresh.setColorSchemeColors(
                MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorPrimary));
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
        binding.states.btnRetry.setOnClickListener(v -> viewModel.refresh());
        binding.btnDocsRetry.setOnClickListener(v -> viewModel.refreshDocuments());

        binding.btnAccept.setOnClickListener(v -> confirmAccept());
        binding.btnCounter.setOnClickListener(v -> {
            if (viewModel.currentBid() != null
                    && getChildFragmentManager().findFragmentByTag(SubmitCounterOfferBottomSheet.TAG) == null) {
                new SubmitCounterOfferBottomSheet().show(getChildFragmentManager(), SubmitCounterOfferBottomSheet.TAG);
            }
        });
        binding.btnUpload.setOnClickListener(v -> pickDocument.launch(UPLOAD_TYPES));

        viewModel.getBid().observe(getViewLifecycleOwner(), this::renderBid);
        viewModel.getDocuments().observe(getViewLifecycleOwner(), this::renderDocuments);
        viewModel.isBusy().observe(getViewLifecycleOwner(), busy -> updateButtons());
        viewModel.getActionResults().observe(getViewLifecycleOwner(), this::handleAction);
        viewModel.getAccepted().observe(getViewLifecycleOwner(), this::handleAccepted);
    }

    private void setUpHeader() {
        binding.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        binding.btnCall.setOnClickListener(v -> comingSoon());
        binding.btnMenu.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(requireContext(), v);
            menu.getMenu().add(R.string.chat_menu_refresh);
            menu.setOnMenuItemClickListener(item -> {
                viewModel.refresh();
                return true;
            });
            menu.show();
        });
    }

    // ---------------------------------------------------------------- the deal

    private void renderBid(Resource<NegotiationDetail> resource) {
        switch (resource.status) {
            case LOADING:
                hideStates();
                if (resource.data == null) {
                    binding.content.setVisibility(View.GONE);
                    binding.states.progress.setVisibility(View.VISIBLE);
                } else {
                    bindBid(resource.data);
                }
                break;
            case SUCCESS:
                binding.swipeRefresh.setRefreshing(false);
                hideStates();
                if (resource.data == null) {
                    binding.content.setVisibility(View.GONE);
                    binding.states.tvEmpty.setText(R.string.negotiation_not_found);
                    binding.states.emptyGroup.setVisibility(View.VISIBLE);
                } else {
                    bindBid(resource.data);
                }
                break;
            case ERROR:
                binding.swipeRefresh.setRefreshing(false);
                hideStates();
                binding.content.setVisibility(View.GONE);
                binding.states.tvError.setText(ResourceMessages.errorText(requireContext(), resource));
                binding.states.errorGroup.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
        updateButtons();
    }

    private void bindBid(NegotiationDetail bid) {
        binding.content.setVisibility(View.VISIBLE);
        String noValue = getString(R.string.no_value);

        String batch = bid.getBatchNumber();
        binding.tvBatch.setText(batch == null ? noValue : getString(R.string.negotiation_batch, batch));
        String commodity = bid.getCommodity();
        binding.tvCommodity.setText(commodity == null ? noValue : commodity);
        String kg = Formatters.number(bid.getQuantityKg());
        binding.tvQuantity.setText(kg == null ? noValue : getString(R.string.negotiation_quantity_value, kg));
        String total = Formatters.currency(bid.getTotalValue(), bid.getCurrency(), 2);
        binding.tvTotal.setText(total == null ? noValue : total);

        int label;
        int background;
        int textColor;
        if (bid.statusIs(NegotiationDetail.STATUS_ACCEPTED)) {
            label = R.string.negotiation_status_accepted;
            background = R.drawable.bg_badge_market_ready;
            textColor = R.color.marketBadgeReadyText;
        } else if (bid.statusIs(NegotiationDetail.STATUS_DECLINED)) {
            label = R.string.negotiation_status_declined;
            background = R.drawable.bg_badge_market_neutral;
            textColor = R.color.marketBadgeNeutralText;
        } else if (!bid.isBuyerTurn()) {
            label = R.string.negotiation_status_waiting;
            background = R.drawable.bg_badge_pending;
            textColor = R.color.badgePendingText;
        } else {
            label = R.string.negotiation_status_pending;
            background = R.drawable.bg_badge_turn;
            textColor = R.color.badgeTurnText;
        }
        binding.tvStatus.setText(label);
        binding.tvStatus.setBackgroundResource(background);
        binding.tvStatus.setTextColor(requireContext().getColor(textColor));
    }

    /** Accept and Counter work only for a buyer, only on their turn, and not while something is running. */
    private void updateButtons() {
        if (binding == null) {
            return;
        }
        NegotiationDetail bid = viewModel.currentBid();
        boolean busy = Boolean.TRUE.equals(viewModel.isBusy().getValue());
        boolean buyer = !Roles.VENDOR.equalsIgnoreCase(SessionManager.getInstance().getRole());
        boolean canAct = bid != null && buyer && bid.isBuyerTurn() && !busy;
        binding.btnAccept.setEnabled(canAct);
        binding.btnCounter.setEnabled(canAct);
        binding.btnUpload.setEnabled(!busy && viewModel.getConversationId() != null);

        int note = 0;
        if (bid != null) {
            if (!buyer) {
                note = R.string.negotiation_buyers_only_note;
            } else if (bid.isClosed()) {
                note = R.string.negotiation_closed_note;
            } else if (!bid.isBuyerTurn()) {
                note = R.string.negotiation_waiting_note;
            }
        }
        binding.tvNote.setVisibility(note == 0 ? View.GONE : View.VISIBLE);
        if (note != 0) {
            binding.tvNote.setText(note);
        }
    }

    private void hideStates() {
        binding.states.progress.setVisibility(View.GONE);
        binding.states.emptyGroup.setVisibility(View.GONE);
        binding.states.errorGroup.setVisibility(View.GONE);
    }

    // ---------------------------------------------------------------- documents

    private void renderDocuments(Resource<List<NegotiationDocument>> resource) {
        binding.docsProgress.setVisibility(View.GONE);
        binding.tvDocsMessage.setVisibility(View.GONE);
        binding.btnDocsRetry.setVisibility(View.GONE);
        switch (resource.status) {
            case LOADING:
                documentsAdapter.submitList(resource.data);
                if (resource.data == null) {
                    binding.docsProgress.setVisibility(View.VISIBLE);
                }
                break;
            case SUCCESS:
                documentsAdapter.submitList(resource.data);
                if (resource.data == null || resource.data.isEmpty()) {
                    binding.tvDocsMessage.setText(R.string.negotiation_documents_empty);
                    binding.tvDocsMessage.setVisibility(View.VISIBLE);
                }
                break;
            case ERROR:
                documentsAdapter.submitList(null);
                binding.tvDocsMessage.setText(ResourceMessages.errorText(requireContext(), resource));
                binding.tvDocsMessage.setVisibility(View.VISIBLE);
                binding.btnDocsRetry.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void download(@NonNull NegotiationDocument document) {
        String url = document.getDownloadUrl();
        if (url == null) {
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException | SecurityException noApp) {
            Snackbar.make(binding.getRoot(), R.string.document_open_failed, Snackbar.LENGTH_LONG).show();
        }
    }

    /** The person picked a file (or backed out, then the address is null). */
    private void onDocumentPicked(@Nullable Uri uri) {
        if (uri == null || viewModel == null) {
            return;
        }
        String name = null;
        try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) {
                    name = cursor.getString(column);
                }
            }
        } catch (RuntimeException unreadable) {
            // Falls back to the last part of the address below.
        }
        if (name == null || name.trim().isEmpty()) {
            String last = uri.getLastPathSegment();
            name = last == null ? getString(R.string.document_untitled) : last;
        }
        String mime = requireContext().getContentResolver().getType(uri);
        viewModel.uploadDocument(requireContext().getContentResolver(), uri, name, mime);
    }

    // ---------------------------------------------------------------- actions

    private void confirmAccept() {
        NegotiationDetail bid = viewModel.currentBid();
        if (bid == null) {
            return;
        }
        String total = Formatters.currency(bid.getTotalValue(), bid.getCurrency(), 2);
        String commodity = bid.getCommodity() == null ? getString(R.string.negotiation_untitled) : bid.getCommodity();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.negotiation_accept_title)
                .setMessage(getString(R.string.negotiation_accept_message, commodity,
                        total == null ? getString(R.string.no_value) : total))
                .setNegativeButton(R.string.counter_cancel, null)
                .setPositiveButton(R.string.negotiation_accept_confirm, (dialog, which) -> viewModel.accept())
                .show();
    }

    private void handleAction(@Nullable Event<ActionResult> event) {
        ActionResult result = event == null ? null : event.getContentIfNotHandled();
        if (result != null) {
            Snackbar.make(binding.getRoot(), result.text(requireContext()), Snackbar.LENGTH_LONG).show();
        }
    }

    private void handleAccepted(@Nullable Event<TransactionConfirmation> event) {
        TransactionConfirmation transaction = event == null ? null : event.getContentIfNotHandled();
        if (transaction == null) {
            return;
        }
        NegotiationDetail bid = viewModel.currentBid();
        Bundle args = new Bundle();
        args.putString(OfferAcceptedFragment.ARG_TRANSACTION_ID, transaction.getTransactionId());
        args.putString(OfferAcceptedFragment.ARG_ITEM_DETAIL, transaction.getItemDetail());
        args.putString(OfferAcceptedFragment.ARG_QUANTITY, transaction.getQuantity());
        args.putString(OfferAcceptedFragment.ARG_AGREED_RATE, transaction.getAgreedRate());
        args.putString(OfferAcceptedFragment.ARG_TOTAL_VALUE, transaction.getTotalContractValue());
        args.putString(OfferAcceptedFragment.ARG_STATUS, transaction.getStatus());
        args.putString(OfferAcceptedFragment.ARG_BATCH_NUMBER, bid == null ? null : bid.getBatchNumber());
        NavHostFragment.findNavController(this).navigate(R.id.action_counterPrice_to_offerAccepted, args);
    }

    private void comingSoon() {
        Snackbar.make(binding.getRoot(), R.string.coming_soon, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
