package com.example.marketplace.ui.more;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.marketplace.R;
import com.example.marketplace.data.model.auth.MeResponse;
import com.example.marketplace.data.model.auth.User;
import com.example.marketplace.databinding.FragmentMoreBinding;
import com.example.marketplace.utils.Formatters;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.RoleAccess;
import com.example.marketplace.utils.Roles;
import com.example.marketplace.utils.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

/**
 * The fifth bottom tab: the person's profile card, three menu rows (Orders, Purchase History,
 * Operator Settings) and a Log out row that asks first.
 */
public class MoreFragment extends Fragment {

    private FragmentMoreBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMoreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MoreViewModel viewModel = new ViewModelProvider(this).get(MoreViewModel.class);

        if (RoleAccess.isVendor(SessionManager.getInstance().getRole())) {
            // The vendor module has its own menu; these pages are built later.
            bindMenuRow(binding.rowOrders.getRoot(), R.drawable.ic_history,
                    R.string.more_vendor_sales_title, R.string.more_vendor_sales_subtitle, R.id.action_more_to_vendorSales);
            bindMenuRow(binding.rowHistory.getRoot(), R.drawable.ic_gear,
                    R.string.more_vendor_settings_title, R.string.more_vendor_settings_subtitle, R.id.action_more_to_vendorSettings);
            binding.rowSettings.getRoot().setVisibility(View.GONE);
        } else {
            bindMenuRow(binding.rowOrders.getRoot(), R.drawable.ic_orders,
                    R.string.more_orders_title, R.string.more_orders_subtitle, R.id.action_more_to_orders);
            bindMenuRow(binding.rowHistory.getRoot(), R.drawable.ic_history,
                    R.string.more_history_title, R.string.more_history_subtitle, R.id.action_more_to_purchaseHistory);
            bindMenuRow(binding.rowSettings.getRoot(), R.drawable.ic_gear,
                    R.string.more_settings_title, R.string.more_settings_subtitle, R.id.action_more_to_operatorSettings);
        }
        binding.rowLogout.setOnClickListener(v -> confirmLogout());

        showSavedProfile();
        viewModel.getProfile().observe(getViewLifecycleOwner(), this::onProfile);
    }

    /** Fills one menu row (icon, title, subtitle) and makes the whole row open its screen. */
    private void bindMenuRow(@NonNull View row, @DrawableRes int icon, @StringRes int title,
                             @StringRes int subtitle, @IdRes int actionId) {
        ((ImageView) row.findViewById(R.id.ivIcon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.tvTitle)).setText(title);
        ((TextView) row.findViewById(R.id.tvSubtitle)).setText(subtitle);
        row.setOnClickListener(v -> {
            if (actionId == 0) {
                Snackbar.make(binding.getRoot(), R.string.coming_soon, Snackbar.LENGTH_SHORT).show();
            } else {
                NavHostFragment.findNavController(this).navigate(actionId);
            }
        });
    }

    /** Name and role come from the saved login, so the card is never empty while the profile loads. */
    private void showSavedProfile() {
        SessionManager session = SessionManager.getInstance();
        String name = session.getFullName();
        boolean hasName = name != null && !name.trim().isEmpty();
        binding.tvAvatar.setText(Formatters.initials(requireContext(), name));
        binding.tvUserName.setText(hasName ? name : getString(R.string.no_value));
        binding.tvRoleBadge.setText(Roles.VENDOR.equalsIgnoreCase(session.getRole())
                ? R.string.role_vendor : R.string.role_operator);
    }

    /** The server's profile adds the job title / organisation line when it has one. */
    private void onProfile(@NonNull Resource<MeResponse> resource) {
        if (resource.status != Resource.Status.SUCCESS || resource.data == null) {
            return;
        }
        User user = resource.data.getUser();
        String subtitle = user == null ? null : user.getJobTitle();
        boolean hasSubtitle = subtitle != null && !subtitle.trim().isEmpty();
        binding.tvCompany.setText(hasSubtitle ? subtitle : null);
        binding.tvCompany.setVisibility(hasSubtitle ? View.VISIBLE : View.GONE);
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.logout_confirm_title)
                .setMessage(R.string.logout_confirm_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.logout, (dialog, which) -> {
                    SessionManager.getInstance().clear();
                    NavHostFragment.findNavController(this).navigate(R.id.action_global_loginFragment);
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
