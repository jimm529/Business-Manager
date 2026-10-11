package com.example.marketplace.ui.vendors;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.marketplace.R;
import com.example.marketplace.data.model.vendor.RegisteredVendor;
import com.example.marketplace.databinding.FragmentRegisteredVendorsBinding;
import com.example.marketplace.utils.Formatters;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.example.marketplace.utils.SessionManager;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/**
 * The screen behind the round Action (+) button: every registered vendor as a card, with search,
 * a status filter, "+ Add" to register one and pull-to-refresh. Loading, empty and error messages share one overlay.
 */
public class RegisteredVendorsFragment extends Fragment {

    /** Filter choices in the order they appear: null means "all". */
    private static final String[] FILTER_VALUES = {
            null,
            RegisteredVendor.STATUS_ACTIVE,
            RegisteredVendor.STATUS_PENDING,
            RegisteredVendor.STATUS_INACTIVE};

    private FragmentRegisteredVendorsBinding binding;
    private RegisteredVendorsViewModel viewModel;
    private RegisteredVendorsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisteredVendorsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(RegisteredVendorsViewModel.class);

        adapter = new RegisteredVendorsAdapter(vendor -> showComingSoon());
        binding.rvVendors.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvVendors.setAdapter(adapter);

        setUpHeader();
        setUpSearch();
        binding.btnFilter.setOnClickListener(v -> showFilterDialog());
        binding.btnRefresh.setOnClickListener(v -> {
            binding.swipeRefresh.setRefreshing(true);
            viewModel.refresh();
        });
        binding.btnAdd.setOnClickListener(v -> {
            if (getChildFragmentManager().findFragmentByTag(AddVendorDialogFragment.TAG) == null) {
                new AddVendorDialogFragment().show(getChildFragmentManager(), AddVendorDialogFragment.TAG);
            }
        });
        binding.swipeRefresh.setColorSchemeColors(
                MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorPrimary));
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
        binding.states.btnRetry.setOnClickListener(v -> viewModel.refresh());

        viewModel.getVendors().observe(getViewLifecycleOwner(), this::render);
        viewModel.getSuccessMessages().observe(getViewLifecycleOwner(), event -> {
            Integer message = event.getContentIfNotHandled();
            if (message != null) {
                Snackbar snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG);
                snackbar.setBackgroundTint(MaterialColors.getColor(binding.getRoot(),
                        com.google.android.material.R.attr.colorPrimary));
                snackbar.setTextColor(MaterialColors.getColor(binding.getRoot(),
                        com.google.android.material.R.attr.colorOnPrimary));
                snackbar.show();
            }
        });
    }

    // ---------------------------------------------------------------- set-up

    private void setUpHeader() {
        binding.btnBack.setOnClickListener(v -> {
            if (!NavHostFragment.findNavController(this).popBackStack()) {
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });
        binding.btnNotifications.setOnClickListener(v -> showComingSoon());

        binding.tvAvatar.setText(Formatters.initials(requireContext(), SessionManager.getInstance().getFullName()));
        binding.tvAvatar.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(requireContext(), v);
            menu.getMenu().add(R.string.logout);
            menu.setOnMenuItemClickListener(item -> {
                SessionManager.getInstance().clear();
                NavHostFragment.findNavController(this).navigate(R.id.action_global_loginFragment);
                return true;
            });
            menu.show();
        });
    }

    private void setUpSearch() {
        // After a rotation the box is empty again: show the search the view model still has.
        if (TextUtils.isEmpty(binding.etSearch.getText()) && !viewModel.getQuery().isEmpty()) {
            binding.etSearch.setText(viewModel.getQuery());
        }
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

    private void showFilterDialog() {
        String[] labels = {
                getString(R.string.vendors_filter_all),
                getString(R.string.vendors_status_active),
                getString(R.string.vendors_status_pending),
                getString(R.string.vendors_status_inactive)};
        int checked = 0;
        for (int i = 0; i < FILTER_VALUES.length; i++) {
            if (TextUtils.equals(FILTER_VALUES[i], viewModel.getStatusFilter())) {
                checked = i;
            }
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.vendors_filter_title)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    viewModel.setStatusFilter(FILTER_VALUES[which]);
                    dialog.dismiss();
                })
                .show();
    }

    // ---------------------------------------------------------------- list

    private void render(Resource<List<RegisteredVendor>> resource) {
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
                List<RegisteredVendor> items = resource.data;
                adapter.submitList(items);
                if (items == null || items.isEmpty()) {
                    boolean filtered = !viewModel.getQuery().isEmpty() || viewModel.getStatusFilter() != null;
                    binding.states.tvEmpty.setText(filtered ? R.string.vendors_empty_filtered : R.string.vendors_empty);
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

    private void showComingSoon() {
        Snackbar.make(binding.getRoot(), R.string.coming_soon, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
