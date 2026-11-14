package com.example.marketplace.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.marketplace.R;
import com.example.marketplace.data.model.dashboard.PurchaseHistoryResponse;
import com.example.marketplace.databinding.FragmentPurchaseHistoryBinding;
import com.example.marketplace.ui.common.PageButtons;
import com.example.marketplace.utils.Formatters;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.MaterialColors;

import java.util.List;

/**
 * Purchase History: summary cards, status chips, one card per past purchase and page buttons, all from
 * the server, with pull-to-refresh and loading / empty / error messages. The server only allows buyer accounts;
 * a vendor sees its message.
 */
public class PurchaseHistoryFragment extends Fragment {

    private FragmentPurchaseHistoryBinding binding;
    private PurchaseHistoryViewModel viewModel;
    private PurchaseHistoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPurchaseHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PurchaseHistoryViewModel.class);

        adapter = new PurchaseHistoryAdapter();
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistory.setAdapter(adapter);

        setUpStatusChips();
        binding.btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        binding.btnRefresh.setOnClickListener(v -> {
            binding.swipeRefresh.setRefreshing(true);
            viewModel.refresh();
        });
        binding.swipeRefresh.setColorSchemeColors(
                MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorPrimary));
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
        binding.states.btnRetry.setOnClickListener(v -> viewModel.refresh());

        viewModel.getHistory().observe(getViewLifecycleOwner(), this::render);
    }

    // ---------------------------------------------------------------- status chips

    /** One chip per status ("All" first). Picking one asks the server for that status, from page 1. */
    private void setUpStatusChips() {
        binding.groupStatus.removeAllViews();
        for (PurchaseHistoryViewModel.StatusFilter filter : PurchaseHistoryViewModel.StatusFilter.values()) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_filter_chip, binding.groupStatus, false);
            chip.setId(View.generateViewId());
            chip.setText(filter.label);
            chip.setTag(filter);
            binding.groupStatus.addView(chip);
            if (filter == viewModel.getFilter()) {
                binding.groupStatus.check(chip.getId());
            }
            chip.setOnClickListener(v -> viewModel.setFilter(filter));
        }
    }

    // ---------------------------------------------------------------- content

    private void render(Resource<PurchaseHistoryResponse> resource) {
        switch (resource.status) {
            case LOADING:
                hideStates();
                if (resource.data == null) {
                    adapter.submitList(null);
                    binding.summary.setVisibility(View.GONE);
                    binding.pagination.setVisibility(View.GONE);
                    binding.tvListEmpty.setVisibility(View.GONE);
                    binding.states.progress.setVisibility(View.VISIBLE);
                } else {
                    showData(resource.data);
                }
                break;
            case SUCCESS:
                binding.swipeRefresh.setRefreshing(false);
                hideStates();
                if (resource.data != null) {
                    showData(resource.data);
                }
                break;
            case ERROR:
                binding.swipeRefresh.setRefreshing(false);
                hideStates();
                adapter.submitList(null);
                binding.summary.setVisibility(View.GONE);
                binding.pagination.setVisibility(View.GONE);
                binding.tvListEmpty.setVisibility(View.GONE);
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

    private void showData(@NonNull PurchaseHistoryResponse data) {
        List<PurchaseHistoryResponse.Row> rows = data.getOrders();
        adapter.submitList(rows);
        bindSummary(data.getMetrics());

        boolean empty = rows.isEmpty();
        binding.tvListEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.tvListEmpty.setText(viewModel.getFilter() == PurchaseHistoryViewModel.StatusFilter.ALL
                ? R.string.history_empty : R.string.history_empty_filtered);

        binding.pagination.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (!empty) {
            int total = data.getTotalOrders();
            int size = data.getPageSize();
            int page = data.getCurrentPage();
            int start = (page - 1) * size + 1;
            int end = Math.min(page * size, total);
            binding.tvShowing.setText(getString(R.string.sales_showing, start, end, total));
            PageButtons.build(binding.pager, page, data.getTotalPages(), viewModel::goToPage);
        }
    }

    private void bindSummary(@Nullable PurchaseHistoryResponse.Metrics metrics) {
        binding.summary.setVisibility(metrics == null ? View.GONE : View.VISIBLE);
        if (metrics == null) {
            return;
        }
        String noValue = getString(R.string.no_value);
        Integer orders = metrics.getTotalOrders();
        binding.tvTotalOrders.setText(orders == null ? noValue : String.valueOf(orders));
        Double spend = metrics.getTotalSpend();
        binding.tvTotalSpend.setText(spend == null ? noValue : Formatters.compactCurrency(requireContext(), spend));
        String materials = Formatters.number(metrics.getMaterialsProcuredMT());
        binding.tvMaterials.setText(materials == null ? noValue : getString(R.string.history_materials_mt, materials));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
