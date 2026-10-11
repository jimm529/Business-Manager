package com.example.marketplace.ui.orders;

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
import com.example.marketplace.data.model.order.Order;
import com.example.marketplace.databinding.FragmentOrdersBinding;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.google.android.material.color.MaterialColors;

import java.util.List;

/**
 * Orders & Tracking: every order from the server, with status chips (All, Pending, In Progress,
 * Delivered, Cancelled), pull-to-refresh and loading / empty / error messages.
 */
public class OrdersFragment extends Fragment {

    private FragmentOrdersBinding binding;
    private OrdersViewModel viewModel;
    private OrdersAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(OrdersViewModel.class);

        adapter = new OrdersAdapter();
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvOrders.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        binding.btnRefresh.setOnClickListener(v -> {
            binding.swipeRefresh.setRefreshing(true);
            viewModel.refresh();
        });
        binding.swipeRefresh.setColorSchemeColors(
                MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorPrimary));
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
        binding.states.btnRetry.setOnClickListener(v -> viewModel.refresh());

        setUpFilters();
        viewModel.getOrders().observe(getViewLifecycleOwner(), this::render);
    }

    private void setUpFilters() {
        // Show the filter the view model remembers (matters after rotation), then listen for changes.
        binding.chipFilters.check(chipFor(viewModel.getFilter()));
        binding.chipFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                viewModel.setFilter(filterFor(checkedIds.get(0)));
            }
        });
    }

    private static int chipFor(@NonNull OrdersViewModel.Filter filter) {
        switch (filter) {
            case PENDING: return R.id.chipPending;
            case IN_PROGRESS: return R.id.chipInProgress;
            case DELIVERED: return R.id.chipDelivered;
            case CANCELLED: return R.id.chipCancelled;
            default: return R.id.chipAll;
        }
    }

    @NonNull
    private static OrdersViewModel.Filter filterFor(int chipId) {
        if (chipId == R.id.chipPending) return OrdersViewModel.Filter.PENDING;
        if (chipId == R.id.chipInProgress) return OrdersViewModel.Filter.IN_PROGRESS;
        if (chipId == R.id.chipDelivered) return OrdersViewModel.Filter.DELIVERED;
        if (chipId == R.id.chipCancelled) return OrdersViewModel.Filter.CANCELLED;
        return OrdersViewModel.Filter.ALL;
    }

    private void render(Resource<List<Order>> resource) {
        switch (resource.status) {
            case LOADING:
                hideStates();
                if (resource.data == null) {
                    adapter.submitList(null);
                    binding.states.progress.setVisibility(View.VISIBLE);
                } else {
                    adapter.submitList(resource.data);
                }
                break;
            case SUCCESS:
                binding.swipeRefresh.setRefreshing(false);
                hideStates();
                List<Order> items = resource.data;
                adapter.submitList(items);
                if (items == null || items.isEmpty()) {
                    boolean filtered = viewModel.getFilter() != OrdersViewModel.Filter.ALL;
                    binding.states.tvEmpty.setText(filtered ? R.string.orders_empty_filtered : R.string.orders_empty);
                    binding.states.emptyGroup.setVisibility(View.VISIBLE);
                }
                break;
            case ERROR:
                binding.swipeRefresh.setRefreshing(false);
                hideStates();
                adapter.submitList(null);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
