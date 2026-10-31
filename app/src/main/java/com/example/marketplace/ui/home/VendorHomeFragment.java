package com.example.marketplace.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.marketplace.R;
import com.example.marketplace.data.model.dashboard.VendorOverviewResponse;
import com.example.marketplace.databinding.FragmentVendorHomeBinding;
import com.example.marketplace.databinding.ItemVendorInventoryBinding;
import com.example.marketplace.databinding.ItemVendorNegotiationBinding;
import com.example.marketplace.databinding.ItemVendorPipelineBinding;
import com.example.marketplace.ui.messages.ChatFragment;
import com.example.marketplace.utils.Formatters;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.example.marketplace.utils.SessionManager;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The vendor's Home: stat cards, the revenue chart, open negotiations, the logistics pipeline and
 * the inventory table, all from GET vendor-portal/overview. Pull down to reload.
 */
public class VendorHomeFragment extends Fragment {

    /** A purity at or above this (percent) is shown as good; below it as low. */
    private static final double GOOD_PURITY = 80.0;
    /** The dashboard shows only the newest few negotiations; View All opens the full list. */
    private static final int MAX_NEGOTIATIONS = 5;

    private FragmentVendorHomeBinding binding;
    private VendorHomeViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVendorHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(VendorHomeViewModel.class);

        binding.swipeRefresh.setColorSchemeColors(
                MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorPrimary));
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
        binding.states.btnRetry.setOnClickListener(v -> viewModel.refresh());

        binding.btnNewBatch.setOnClickListener(v -> openTopLevel(R.id.vendorListingsFragment));
        binding.btnViewInventory.setOnClickListener(v -> openTopLevel(R.id.vendorListingsFragment));
        binding.btnViewAllNegotiations.setOnClickListener(v -> openTopLevel(R.id.vendorRequestsFragment));
        binding.btnInventoryFilter.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), R.string.coming_soon, Snackbar.LENGTH_SHORT).show());

        viewModel.getOverview().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(Resource<VendorOverviewResponse> resource) {
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
                VendorOverviewResponse.Data data = resource.data == null ? null : resource.data.getData();
                if (data == null) {
                    binding.content.setVisibility(View.GONE);
                    binding.states.tvEmpty.setText(R.string.error_generic);
                    binding.states.emptyGroup.setVisibility(View.VISIBLE);
                } else {
                    binding.content.setVisibility(View.VISIBLE);
                    bind(data);
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

    // ---------------------------------------------------------------- content

    private void bind(@NonNull VendorOverviewResponse.Data data) {
        bindWelcome(data.getVendor());
        bindStats(data.getStats());
        bindChart(data.getMarketPerformance());
        bindNegotiations(data.getActiveNegotiations());
        bindPipeline(data.getLogisticsPipeline());
        bindInventory(data.getInventoryStatus());
    }

    private void bindWelcome(@Nullable VendorOverviewResponse.VendorInfo vendor) {
        String name = SessionManager.getInstance().getFullName();
        if ((name == null || name.trim().isEmpty()) && vendor != null) {
            name = vendor.getCompanyName();
        }
        binding.tvName.setText(name == null || name.trim().isEmpty() ? getString(R.string.no_value) : name);
        binding.tvAvatar.setText(Formatters.initials(requireContext(), name));
    }

    private void bindStats(@Nullable VendorOverviewResponse.Stats stats) {
        String noValue = getString(R.string.no_value);
        Double revenue = stats == null ? null : stats.getTotalRevenue();
        binding.tvRevenue.setText(revenue == null ? noValue : Formatters.compactCurrency(requireContext(), revenue));

        Double growth = stats == null ? null : stats.getRevenueGrowthPct();
        binding.tvGrowth.setVisibility(growth == null ? View.GONE : View.VISIBLE);
        if (growth != null) {
            binding.tvGrowth.setText(getString(R.string.vendor_growth, String.format(Locale.getDefault(), "%+.1f", growth)));
            binding.tvGrowth.setTextColor(MaterialColors.getColor(binding.getRoot(), growth >= 0
                    ? com.google.android.material.R.attr.colorSecondary
                    : com.google.android.material.R.attr.colorError));
        }

        Integer batches = stats == null ? null : stats.getActiveListings();
        binding.tvBatches.setText(batches == null ? noValue : String.valueOf(batches));
        String mt = Formatters.number(stats == null ? null : stats.getActiveVolumeMt());
        binding.tvBatchesMt.setText(mt == null ? "" : getString(R.string.vendor_batches_mt, mt));

        String efficiency = Formatters.number(stats == null ? null : stats.getExtractionEfficiency());
        binding.tvEfficiency.setText(efficiency == null ? noValue : getString(R.string.vendor_percent, efficiency));

        VendorOverviewResponse.ComplianceHealth compliance = stats == null ? null : stats.getComplianceHealth();
        String status = compliance == null ? null : compliance.getStatus();
        int label = R.string.vendor_compliance_expired;
        if ("compliant".equalsIgnoreCase(status)) {
            label = R.string.vendor_compliance_active;
        } else if ("pending".equalsIgnoreCase(status)) {
            label = R.string.vendor_compliance_pending;
        }
        binding.tvCompliance.setText(compliance == null ? getString(R.string.no_value) : getString(label));
        String audit = compliance == null ? null : compliance.getNextAudit();
        binding.tvNextAudit.setVisibility(audit == null || audit.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvNextAudit.setText(audit == null ? null : getString(R.string.vendor_next_audit, audit));
    }

    private void bindChart(@NonNull List<VendorOverviewResponse.ChartBar> months) {
        List<VendorBarChartView.Bar> bars = new ArrayList<>();
        boolean anySales = false;
        for (VendorOverviewResponse.ChartBar month : months) {
            String name = month.isCurrent() ? getString(R.string.vendor_chart_now)
                    : (month.getMonthName() == null ? "" : month.getMonthName());
            bars.add(new VendorBarChartView.Bar(name, month.getValue(), month.isCurrent()));
            anySales |= month.getValue() > 0;
        }
        binding.chart.setBars(bars);
        binding.chart.setVisibility(bars.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvChartEmpty.setVisibility(anySales ? View.GONE : View.VISIBLE);
    }

    private void bindNegotiations(@NonNull List<VendorOverviewResponse.Negotiation> deals) {
        binding.listNegotiations.removeAllViews();
        binding.tvNegotiationsEmpty.setVisibility(deals.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < Math.min(deals.size(), MAX_NEGOTIATIONS); i++) {
            VendorOverviewResponse.Negotiation deal = deals.get(i);
            ItemVendorNegotiationBinding row = ItemVendorNegotiationBinding.inflate(inflater, binding.listNegotiations, false);

            String batch = deal.getBatchId() == null ? null
                    : deal.getBatchId().trim().replaceFirst("^(?i)batch\\s+", "").replaceFirst("^#", "");
            row.tvBatch.setText(batch == null || batch.isEmpty()
                    ? getString(R.string.no_value) : getString(R.string.negotiation_batch, batch));
            row.tvTitle.setText(deal.getBatchTitle() == null ? getString(R.string.no_value) : deal.getBatchTitle());
            String price = deal.getPriceDisplay();
            row.tvPrice.setText(price == null ? getString(R.string.no_value) : price);

            boolean countered = deal.isCountered();
            row.tvBadge.setText(countered ? R.string.vendor_badge_counter : R.string.vendor_badge_pending_bid);
            row.tvBadge.setBackgroundResource(countered
                    ? R.drawable.bg_badge_market_ready : R.drawable.bg_badge_market_extraction);
            row.tvBadge.setTextColor(requireContext().getColor(countered
                    ? R.color.marketBadgeReadyText : R.color.marketBadgeExtractionText));

            row.root.setOnClickListener(v -> openChat(deal));
            binding.listNegotiations.addView(row.getRoot());
        }
    }

    private void bindPipeline(@NonNull List<VendorOverviewResponse.PipelineStage> stages) {
        binding.listPipeline.removeAllViews();
        binding.tvPipelineEmpty.setVisibility(stages.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < stages.size(); i++) {
            VendorOverviewResponse.PipelineStage stage = stages.get(i);
            ItemVendorPipelineBinding row = ItemVendorPipelineBinding.inflate(inflater, binding.listPipeline, false);
            boolean first = i == 0;
            row.viewDot.setBackgroundResource(first ? R.drawable.bg_dot_active : R.drawable.bg_dot_idle);
            row.viewLine.setVisibility(i == stages.size() - 1 ? View.INVISIBLE : View.VISIBLE);
            row.tvType.setText(stage.getType() == null ? "" : stage.getType());
            row.tvType.setTextColor(MaterialColors.getColor(binding.getRoot(), first
                    ? com.google.android.material.R.attr.colorPrimary
                    : com.google.android.material.R.attr.colorOnSurfaceVariant));
            row.tvTitle.setText(stage.getTitle() == null ? getString(R.string.no_value) : stage.getTitle());
            row.tvSubtitle.setText(stage.getSubtitle() == null ? "" : stage.getSubtitle());
            binding.listPipeline.addView(row.getRoot());
        }
    }

    private void bindInventory(@NonNull List<VendorOverviewResponse.InventoryItem> items) {
        binding.listInventory.removeAllViews();
        binding.tvInventoryEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = getLayoutInflater();
        String noValue = getString(R.string.no_value);
        for (VendorOverviewResponse.InventoryItem item : items) {
            ItemVendorInventoryBinding row = ItemVendorInventoryBinding.inflate(inflater, binding.listInventory, false);
            row.tvId.setText(item.getDisplayId() == null ? noValue : item.getDisplayId());
            row.tvMaterial.setText(item.getMaterial() == null ? noValue : item.getMaterial());
            String value = Formatters.currency(item.getValue(), null, 0);
            row.tvValue.setText(value == null ? noValue : value);

            double purity = item.getPurityValue();
            int background;
            int text;
            if (purity >= GOOD_PURITY) {
                background = R.drawable.bg_badge_market_ready;
                text = R.color.marketBadgeReadyText;
            } else if (purity > 0) {
                background = R.drawable.bg_badge_urgent;
                text = R.color.badgeUrgentText;
            } else {
                background = R.drawable.bg_badge_market_neutral;
                text = R.color.marketBadgeNeutralText;
            }
            row.tvPurity.setText(item.getPurityText() == null ? noValue : item.getPurityText());
            row.tvPurity.setBackgroundResource(background);
            row.tvPurity.setTextColor(requireContext().getColor(text));
            binding.listInventory.addView(row.getRoot());
        }
    }

    // ---------------------------------------------------------------- navigation

    private void openChat(@NonNull VendorOverviewResponse.Negotiation deal) {
        String conversationId = deal.getConversationId();
        if (conversationId == null) {
            Snackbar.make(binding.getRoot(), R.string.vendor_negotiation_chat_missing, Snackbar.LENGTH_SHORT).show();
            return;
        }
        Bundle args = new Bundle();
        args.putString(ChatFragment.ARG_CONVERSATION_ID, conversationId);
        args.putString(ChatFragment.ARG_PARTICIPANT_NAME, deal.getBatchTitle());
        NavHostFragment.findNavController(this).navigate(R.id.action_home_to_chat, args);
    }

    /** Opens a tab-level screen on top of Home, the same way the bottom bar does. */
    private void openTopLevel(int destinationId) {
        NavController navController = NavHostFragment.findNavController(this);
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.homeFragment, false)
                .build();
        navController.navigate(destinationId, null, options);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
