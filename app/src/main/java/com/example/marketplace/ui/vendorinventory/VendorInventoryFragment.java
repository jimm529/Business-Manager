package com.example.marketplace.ui.vendorinventory;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.marketplace.R;
import com.example.marketplace.data.model.vendor.VendorLot;
import com.example.marketplace.data.model.vendor.VendorProfileResponse;
import com.example.marketplace.databinding.FragmentVendorInventoryBinding;
import com.example.marketplace.databinding.ItemCertificationTileBinding;
import com.example.marketplace.ui.messages.ActionResult;
import com.example.marketplace.ui.messages.ChatFragment;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Formatters;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.example.marketplace.utils.SessionManager;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * A registered vendor's Inventory, for the buyer: the vendor's profile and certifications, the
 * vendor's live lots (cards or rows, searchable), and a way to ask for a custom quote in chat.
 */
public class VendorInventoryFragment extends Fragment {

    public static final String ARG_VENDOR_ID = "vendorId";
    public static final String ARG_VENDOR_NAME = "vendorName";

    private static final double DAYS_PER_YEAR = 365.25;
    private static final long MS_PER_DAY = 24L * 60L * 60L * 1000L;

    private FragmentVendorInventoryBinding binding;
    private VendorInventoryViewModel viewModel;
    private VendorLotsAdapter adapter;
    @Nullable
    private String vendorName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVendorInventoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        String vendorId = args == null ? null : args.getString(ARG_VENDOR_ID);
        vendorName = args == null ? null : args.getString(ARG_VENDOR_NAME);
        if (vendorId == null) {
            Snackbar.make(binding.getRoot(), R.string.vinv_missing_vendor, Snackbar.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }
        viewModel = new ViewModelProvider(this).get(VendorInventoryViewModel.class);
        viewModel.init(vendorId);

        setUpHeader();
        setUpSearch();
        adapter = new VendorLotsAdapter(this::showDetails);
        binding.rvLots.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvLots.setAdapter(adapter);

        binding.btnCards.setOnClickListener(v -> viewModel.setCompact(false));
        binding.btnList.setOnClickListener(v -> viewModel.setCompact(true));
        binding.btnQuote.setOnClickListener(v -> viewModel.requestQuote());
        binding.tvLotsEmpty.setOnClickListener(v -> {
            if (binding.tvLotsEmpty.getTag() != null) {
                viewModel.refresh();
            }
        });
        binding.swipeRefresh.setColorSchemeColors(
                MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorPrimary));
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
        binding.states.btnRetry.setOnClickListener(v -> viewModel.refresh());

        viewModel.getProfile().observe(getViewLifecycleOwner(), this::renderProfile);
        viewModel.getLots().observe(getViewLifecycleOwner(), this::renderLots);
        viewModel.isCompact().observe(getViewLifecycleOwner(), this::renderMode);
        viewModel.isOpeningQuote().observe(getViewLifecycleOwner(), opening ->
                binding.btnQuote.setEnabled(!Boolean.TRUE.equals(opening)));
        viewModel.getQuoteConversation().observe(getViewLifecycleOwner(), this::openChat);
        viewModel.getQuoteErrors().observe(getViewLifecycleOwner(), this::showError);
    }

    private void setUpHeader() {
        binding.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        binding.btnNotifications.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_global_notifications));
        binding.tvAvatar.setText(Formatters.initials(requireContext(), SessionManager.getInstance().getFullName()));
    }

    private void setUpSearch() {
        // After a rotation the box is empty again: show the search the view model still has.
        if (TextUtils.isEmpty(binding.etSearch.getText()) && !viewModel.getQuery().isEmpty()) {
            binding.etSearch.setText(viewModel.getQuery());
        }
        // The lots are already on the phone, so searching can react to every key.
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setQuery(s.toString());
            }
        });
    }

    // ---------------------------------------------------------------- vendor card

    private void renderProfile(Resource<VendorProfileResponse> resource) {
        switch (resource.status) {
            case LOADING:
                hideStates();
                if (resource.data == null) {
                    binding.content.setVisibility(View.GONE);
                    binding.states.progress.setVisibility(View.VISIBLE);
                }
                break;
            case SUCCESS:
                binding.swipeRefresh.setRefreshing(false);
                hideStates();
                if (resource.data == null || resource.data.getData() == null) {
                    binding.content.setVisibility(View.GONE);
                    binding.states.tvEmpty.setText(R.string.vinv_missing_vendor);
                    binding.states.emptyGroup.setVisibility(View.VISIBLE);
                } else {
                    binding.content.setVisibility(View.VISIBLE);
                    bindProfile(resource.data.getData());
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
    }

    private void hideStates() {
        binding.states.progress.setVisibility(View.GONE);
        binding.states.emptyGroup.setVisibility(View.GONE);
        binding.states.errorGroup.setVisibility(View.GONE);
    }

    private void bindProfile(@NonNull VendorProfileResponse.Data vendor) {
        String noValue = getString(R.string.no_value);
        vendorName = vendor.getName() == null ? vendorName : vendor.getName();
        binding.tvVendorName.setText(vendorName == null ? noValue : vendorName);

        // Status pill
        int label;
        int background;
        int color;
        if (vendor.isActive()) {
            label = R.string.vinv_status_active;
            background = R.drawable.bg_badge_market_ready;
            color = R.color.marketBadgeReadyText;
        } else if ("pending".equalsIgnoreCase(vendor.getStatus())) {
            label = R.string.vinv_status_pending;
            background = R.drawable.bg_badge_market_extraction;
            color = R.color.marketBadgeExtractionText;
        } else {
            label = R.string.vinv_status_inactive;
            background = R.drawable.bg_badge_market_neutral;
            color = R.color.marketBadgeNeutralText;
        }
        binding.tvVendorStatus.setText(label);
        binding.tvVendorStatus.setBackgroundResource(background);
        binding.tvVendorStatus.setTextColor(requireContext().getColor(color));

        // A short line worked out from the vendor's real fields (the server has no free-text description).
        String material = vendor.getMaterial();
        String location = vendor.getLocation();
        if (material != null && location != null) {
            binding.tvDescription.setText(getString(R.string.vinv_description, material, location));
        } else if (material != null) {
            binding.tvDescription.setText(getString(R.string.vinv_description_material_only, material));
        } else if (location != null) {
            binding.tvDescription.setText(getString(R.string.vinv_description_location_only, location));
        } else {
            binding.tvDescription.setText(null);
        }
        binding.tvDescription.setVisibility(binding.tvDescription.length() == 0 ? View.GONE : View.VISIBLE);

        Double rating = vendor.getRating();
        binding.tvRating.setText(rating == null ? noValue : String.format(Locale.getDefault(), "%.2f", rating));
        binding.tvAge.setText(ageText(vendor.getCreatedAt()));

        List<VendorProfileResponse.CertificationTile> certs = vendor.getCertificationTiles();
        String compliance = vendor.getComplianceLabel();
        if ((compliance == null || compliance.trim().isEmpty()) && !certs.isEmpty()) {
            compliance = certs.get(0).name;
        }
        binding.tvCompliance.setText(compliance == null || compliance.trim().isEmpty() ? noValue : compliance);
        bindCertifications(certs);
    }

    /** Whole years since the vendor joined, or "< 1 Year". A dash when the date is unknown. */
    private String ageText(@Nullable String createdAt) {
        Date joined = Formatters.parseIso(createdAt);
        if (joined == null) {
            return getString(R.string.no_value);
        }
        long days = Math.max(0, (System.currentTimeMillis() - joined.getTime()) / MS_PER_DAY);
        int years = (int) (days / DAYS_PER_YEAR);
        return years < 1 ? getString(R.string.vinv_less_than_year)
                : getResources().getQuantityString(R.plurals.vinv_years, years, years);
    }

    private void bindCertifications(@NonNull List<VendorProfileResponse.CertificationTile> certs) {
        binding.certRow.removeAllViews();
        binding.certScroll.setVisibility(certs.isEmpty() ? View.GONE : View.VISIBLE);
        for (VendorProfileResponse.CertificationTile cert : certs) {
            ItemCertificationTileBinding tile = ItemCertificationTileBinding.inflate(getLayoutInflater(), binding.certRow, false);
            tile.tvName.setText(cert.name);
            boolean active = "active".equalsIgnoreCase(cert.status);
            int state = active ? R.string.vinv_cert_active
                    : "expired".equalsIgnoreCase(cert.status) ? R.string.vinv_cert_expired : R.string.vinv_cert_pending;
            tile.tvState.setText(state);
            tile.ivIcon.setColorFilter(MaterialColors.getColor(binding.getRoot(), active
                    ? com.google.android.material.R.attr.colorSecondary
                    : com.google.android.material.R.attr.colorOnSurfaceVariant));
            binding.certRow.addView(tile.getRoot());
        }
    }

    // ---------------------------------------------------------------- lots

    private void renderMode(@Nullable Boolean compact) {
        boolean rows = Boolean.TRUE.equals(compact);
        adapter.setCompact(rows);
        binding.btnCards.setColorFilter(MaterialColors.getColor(binding.getRoot(), rows
                ? com.google.android.material.R.attr.colorOnSurfaceVariant
                : com.google.android.material.R.attr.colorPrimary));
        binding.btnList.setColorFilter(MaterialColors.getColor(binding.getRoot(), rows
                ? com.google.android.material.R.attr.colorPrimary
                : com.google.android.material.R.attr.colorOnSurfaceVariant));
    }

    private void renderLots(Resource<List<VendorLot>> resource) {
        binding.tvLotsEmpty.setTag(null);
        binding.tvLotsEmpty.setVisibility(View.GONE);
        switch (resource.status) {
            case LOADING:
                adapter.submitList(resource.data);
                if (resource.data == null) {
                    binding.tvLotsEmpty.setText(R.string.vinv_loading);
                    binding.tvLotsEmpty.setVisibility(View.VISIBLE);
                }
                break;
            case SUCCESS:
                adapter.submitList(resource.data);
                if (resource.data == null || resource.data.isEmpty()) {
                    binding.tvLotsEmpty.setText(viewModel.getQuery().isEmpty()
                            ? R.string.vinv_empty : R.string.vinv_search_empty);
                    binding.tvLotsEmpty.setVisibility(View.VISIBLE);
                }
                break;
            case ERROR:
                adapter.submitList(null);
                binding.tvLotsEmpty.setText(getString(R.string.error_tap_to_retry,
                        ResourceMessages.errorText(requireContext(), resource)));
                binding.tvLotsEmpty.setTag(Boolean.TRUE);
                binding.tvLotsEmpty.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void showDetails(@NonNull VendorLot lot) {
        String noValue = getString(R.string.no_value);
        String quantity = Formatters.number(lot.getQuantity());
        String purity = Formatters.number(lot.getPurity());
        String price = Formatters.currency(lot.getPricePerKg(), lot.getCurrency(), 2);
        String message = getString(R.string.vinv_details_message,
                lot.getLotId() == null ? noValue : lot.getLotId(),
                lot.getMaterial() == null ? noValue : lot.getMaterial(),
                quantity == null ? noValue : getString(R.string.order_quantity, quantity,
                        lot.getUnit() == null ? getString(R.string.requests_unit_mt) : lot.getUnit()),
                purity == null ? noValue : getString(R.string.vendor_percent, purity),
                price == null ? noValue : getString(R.string.price_per_kg, price),
                lot.getLocation() == null ? noValue : lot.getLocation(),
                lot.getStatus() == null ? noValue : lot.getStatus());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(lot.getTitle() == null ? getString(R.string.vinv_details_title) : lot.getTitle())
                .setMessage(message)
                .setPositiveButton(R.string.cd_close, null)
                .show();
    }

    // ---------------------------------------------------------------- custom quote

    private void openChat(@Nullable Event<String> event) {
        String conversationId = event == null ? null : event.getContentIfNotHandled();
        if (conversationId == null) {
            return;
        }
        Bundle args = new Bundle();
        args.putString(ChatFragment.ARG_CONVERSATION_ID, conversationId);
        args.putString(ChatFragment.ARG_PARTICIPANT_NAME, vendorName);
        NavHostFragment.findNavController(this).navigate(R.id.action_vendorInventory_to_chat, args);
    }

    private void showError(@Nullable Event<ActionResult> event) {
        ActionResult result = event == null ? null : event.getContentIfNotHandled();
        if (result != null) {
            Snackbar.make(binding.getRoot(), result.text(requireContext()), Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
