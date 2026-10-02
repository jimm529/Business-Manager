package com.example.marketplace.ui.auth;

import androidx.lifecycle.LifecycleOwner;

import com.example.marketplace.databinding.ViewPortalSelectorBinding;
import com.example.marketplace.utils.Roles;

/** Wires the two portal cards (Vendor / Operator) to the ViewModel so exactly one is selected. */
final class PortalSelector {

    private PortalSelector() { }

    static void bind(ViewPortalSelectorBinding binding, AuthViewModel viewModel, LifecycleOwner owner) {
        binding.cardVendor.setOnClickListener(v -> viewModel.selectRole(Roles.VENDOR));
        binding.cardOperator.setOnClickListener(v -> viewModel.selectRole(Roles.OPERATOR));

        // setValue always notifies, so this also undoes the card's own tap-to-toggle.
        viewModel.getSelectedRole().observe(owner, role -> {
            binding.cardVendor.setChecked(Roles.VENDOR.equals(role));
            binding.cardOperator.setChecked(Roles.OPERATOR.equals(role));
        });
    }
}
