package com.example.marketplace.ui.settings;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.PopupMenu;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marketplace.R;
import com.example.marketplace.data.model.settings.ApprovalWorkflows;
import com.example.marketplace.data.model.settings.LogisticsPartner;
import com.example.marketplace.data.model.settings.OperatorSettingsResponse;
import com.example.marketplace.data.model.settings.SecuritySettings;
import com.example.marketplace.data.model.settings.WhitelistedVendor;
import com.example.marketplace.databinding.FragmentOperatorSettingsBinding;
import com.example.marketplace.databinding.ItemSettingsPartnerBinding;
import com.example.marketplace.databinding.ItemSettingsVendorBinding;
import com.example.marketplace.utils.Formatters;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.example.marketplace.utils.SessionManager;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The Operator Settings tab: eight cards filled from GET settings, edited into a draft held by the
 * view model, and saved with one "Save All Settings". Pull down to reload (this drops unsaved edits).
 */
public class OperatorSettingsFragment extends Fragment {

    private FragmentOperatorSettingsBinding binding;
    private SettingsViewModel viewModel;
    private ApproversAdapter approversAdapter;
    private ItemTouchHelper approversTouchHelper;
    private OnBackPressedCallback backCallback;

    /** True while the boxes are being filled from the draft, so that filling is not mistaken for typing. */
    private boolean populating;
    private final List<OperatorSettingsResponse.Option> paymentOptions = new ArrayList<>();
    private final List<OperatorSettingsResponse.Option> currencyOptions = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOperatorSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        setUpHeader();
        setUpBackHandling();
        setUpApprovers();
        setUpInputs();
        setUpClicks();

        binding.swipeRefresh.setColorSchemeColors(
                MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorPrimary));
        binding.swipeRefresh.setOnChildScrollUpCallback((parent, child) -> binding.scroll.canScrollVertically(-1));
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
        binding.states.btnRetry.setOnClickListener(v -> viewModel.refresh());

        viewModel.getSettings().observe(getViewLifecycleOwner(), this::render);
        viewModel.getDraftVersion().observe(getViewLifecycleOwner(), version -> populate());
        viewModel.isDirty().observe(getViewLifecycleOwner(), dirty -> updateActionBar());
        viewModel.isSaving().observe(getViewLifecycleOwner(), saving -> updateActionBar());
        viewModel.isTwoFactorEnabled().observe(getViewLifecycleOwner(), this::showTwoFactorState);
        viewModel.getSuccessMessages().observe(getViewLifecycleOwner(), event -> {
            Integer message = event.getContentIfNotHandled();
            if (message != null) {
                showSuccess(message);
            }
        });
        viewModel.getErrors().observe(getViewLifecycleOwner(), event -> {
            Resource<Void> error = event.getContentIfNotHandled();
            if (error != null) {
                anchored(Snackbar.make(binding.getRoot(), ResourceMessages.errorText(requireContext(), error),
                        Snackbar.LENGTH_LONG)).show();
            }
        });
        viewModel.getTwoFactorSetupState().observe(getViewLifecycleOwner(), this::onTwoFactorSetup);
    }

    // ---------------------------------------------------------------- set-up

    private void setUpHeader() {
        binding.btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        binding.btnNotifications.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_global_notifications));

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

    /** Back with unsaved edits asks before throwing them away. */
    private void setUpBackHandling() {
        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.settings_discard_title)
                        .setMessage(R.string.settings_discard_message)
                        .setNegativeButton(R.string.settings_keep_editing, null)
                        .setPositiveButton(R.string.settings_discard_confirm, (dialog, which) -> {
                            viewModel.discardChanges();
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        })
                        .show();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);
    }

    private void setUpApprovers() {
        approversAdapter = new ApproversAdapter(viewModel.getDraft().approvers, new ApproversAdapter.Listener() {
            @Override
            public void onRemove(int position) {
                viewModel.removeApprover(position);
                showApproversEmpty();
            }

            @Override
            public void onMove(int from, int to) {
                viewModel.moveApprover(from, to);
            }

            @Override
            public void onStartDrag(@NonNull RecyclerView.ViewHolder holder) {
                approversTouchHelper.startDrag(holder);
            }
        });
        binding.rvApprovers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvApprovers.setAdapter(approversAdapter);

        approversTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                approversAdapter.move(viewHolder.getBindingAdapterPosition(), target.getBindingAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Swiping is switched off (the flags are 0).
            }
        });
        approversTouchHelper.attachToRecyclerView(binding.rvApprovers);
    }

    private void setUpInputs() {
        watch(binding.etFacility, text -> viewModel.getDraft().facilityName = text);
        watch(binding.etLimit, text -> viewModel.getDraft().procurementLimit = text);
        watch(binding.etSignoff, text -> {
            viewModel.getDraft().signoffThreshold = text;
            showSignoffBadge(text);
        });
        watch(binding.etPurity, text -> viewModel.getDraft().minimumPurity = text);
        watch(binding.etVolume, text -> viewModel.getDraft().targetVolume = text);
        watch(binding.etBaseRate, text -> viewModel.getDraft().baseMaterialRate = text);
        watch(binding.etAdjustment, text -> viewModel.getDraft().marketAdjustment = text);

        binding.switchLowStock.setOnCheckedChangeListener((button, checked) -> {
            if (!populating) {
                viewModel.getDraft().lowStockAlertEnabled = checked;
                viewModel.onDraftEdited();
            }
        });
        binding.switchOverride.setOnCheckedChangeListener((button, checked) -> {
            if (!populating) {
                viewModel.getDraft().manualPriceOverride = checked;
                viewModel.onDraftEdited();
            }
        });

        binding.ddTerms.setOnItemClickListener((parent, v, position, id) -> {
            if (position < paymentOptions.size()) {
                String value = paymentOptions.get(position).getValue();
                viewModel.getDraft().paymentTerms = value == null ? "" : value;
                viewModel.onDraftEdited();
            }
        });
        binding.ddCurrency.setOnItemClickListener((parent, v, position, id) -> {
            if (position < currencyOptions.size()) {
                String value = currencyOptions.get(position).getValue();
                viewModel.getDraft().billingCurrency = value == null ? "" : value;
                binding.tilLimit.setPrefixText(TextUtils.isEmpty(value) ? null : value);
                viewModel.onDraftEdited();
            }
        });
    }

    private void setUpClicks() {
        binding.btnUpdateProfile.setOnClickListener(v -> saveIfChanged());
        binding.btnSaveAll.setOnClickListener(v -> viewModel.save());
        binding.btnCancelChanges.setOnClickListener(v -> viewModel.discardChanges());

        binding.btnAddApprover.setOnClickListener(v -> pickApprover());
        binding.btnConfigure2fa.setOnClickListener(v -> viewModel.startTwoFactorSetup());
        binding.btnCopyKey.setOnClickListener(v -> showComingSoon());
        binding.btnRotateKey.setOnClickListener(v -> showComingSoon());
        binding.btnManageList.setOnClickListener(v -> showComingSoon());
        binding.btnAddVendor.setOnClickListener(v -> showComingSoon());
        binding.btnSecurityQuestions.setOnClickListener(v -> showComingSoon());
        binding.btnChangePassword.setOnClickListener(v -> {
            if (getChildFragmentManager().findFragmentByTag(ChangePasswordDialogFragment.TAG) == null) {
                new ChangePasswordDialogFragment().show(getChildFragmentManager(), ChangePasswordDialogFragment.TAG);
            }
        });
    }

    // ---------------------------------------------------------------- rendering

    private void render(Resource<OperatorSettingsResponse> resource) {
        switch (resource.status) {
            case LOADING:
                hideStates();
                if (resource.data == null) {
                    showContent(false);
                    binding.states.progress.setVisibility(View.VISIBLE);
                }
                break;
            case SUCCESS:
                binding.swipeRefresh.setRefreshing(false);
                hideStates();
                if (resource.data == null || !resource.data.hasSettings()) {
                    showContent(false);
                    binding.states.tvEmpty.setText(R.string.settings_empty);
                    binding.states.emptyGroup.setVisibility(View.VISIBLE);
                } else {
                    showContent(true);
                    populate();
                }
                break;
            case ERROR:
                binding.swipeRefresh.setRefreshing(false);
                hideStates();
                showContent(false);
                binding.states.tvError.setText(ResourceMessages.errorText(requireContext(), resource));
                binding.states.errorGroup.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void showContent(boolean visible) {
        binding.scroll.setVisibility(visible ? View.VISIBLE : View.GONE);
        binding.actionBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void hideStates() {
        binding.states.progress.setVisibility(View.GONE);
        binding.states.emptyGroup.setVisibility(View.GONE);
        binding.states.errorGroup.setVisibility(View.GONE);
    }

    /** Fills every card from the draft and the server answer (used on load, Cancel Changes and rotation). */
    private void populate() {
        Resource<OperatorSettingsResponse> state = viewModel.getSettings().getValue();
        if (binding == null || state == null || state.data == null || !state.data.hasSettings()) {
            return;
        }
        OperatorSettingsResponse response = state.data;
        SettingsDraft draft = viewModel.getDraft();
        populating = true;
        try {
            // 1. Procurement profile
            setText(binding.etFacility, draft.facilityName);
            setText(binding.etLimit, draft.procurementLimit);
            paymentOptions.clear();
            paymentOptions.addAll(response.getOptions().getPaymentTerms());
            currencyOptions.clear();
            currencyOptions.addAll(response.getOptions().getBillingCurrencies());
            bindDropdown(binding.ddTerms, paymentOptions, draft.paymentTerms);
            bindDropdown(binding.ddCurrency, currencyOptions, draft.billingCurrency);
            binding.tilLimit.setPrefixText(TextUtils.isEmpty(draft.billingCurrency) ? null : draft.billingCurrency);

            // 2. Approval workflows
            setText(binding.etSignoff, draft.signoffThreshold);
            showSignoffBadge(draft.signoffThreshold);
            approversAdapter.setItems(draft.approvers);
            showApproversEmpty();

            // 3. Material preferences
            setText(binding.etPurity, draft.minimumPurity);
            setText(binding.etVolume, draft.targetVolume);
            binding.switchLowStock.setChecked(draft.lowStockAlertEnabled);

            // 4. Logistics partners
            showPartners(draft.partners);

            // 5. Security and access
            showErpKey(response.getSettings().getSecurityAccess());

            // 6. Price fixing
            setText(binding.etBaseRate, draft.baseMaterialRate);
            setText(binding.etAdjustment, draft.marketAdjustment);
            binding.switchOverride.setChecked(draft.manualPriceOverride);

            // 7. Vendor whitelisting
            showVendors(response.getSettings().getWhitelistedVendors());

            // 8. Login and account details
            OperatorSettingsResponse.AccountDetails account = response.getAccountDetails();
            binding.tvFullName.setText(orDash(account.getFullName()));
            binding.tvRole.setText(orDash(account.getRole()));
            binding.tvEmail.setText(orDash(account.getEmail()));
        } finally {
            populating = false;
        }
        updateActionBar();
    }

    private void bindDropdown(@NonNull com.google.android.material.textfield.MaterialAutoCompleteTextView view,
                              @NonNull List<OperatorSettingsResponse.Option> options, @NonNull String selectedValue) {
        List<String> labels = new ArrayList<>();
        String selectedLabel = selectedValue;
        for (OperatorSettingsResponse.Option option : options) {
            String label = option.getLabel() == null ? String.valueOf(option.getValue()) : option.getLabel();
            labels.add(label);
            if (selectedValue.equals(option.getValue())) {
                selectedLabel = label;
            }
        }
        view.setAdapter(new AllItemsAdapter(requireContext(), labels));
        view.setText(selectedLabel, false);
    }

    private void showSignoffBadge(@NonNull String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            binding.tvSignoffBadge.setVisibility(View.GONE);
            return;
        }
        String shown = trimmed;
        try {
            String formatted = Formatters.number(Double.parseDouble(trimmed));
            shown = formatted == null ? trimmed : formatted;
        } catch (NumberFormatException keepTyped) {
            // Half-typed number: show it as typed.
        }
        binding.tvSignoffBadge.setText(getString(R.string.settings_signoff_badge, shown));
        binding.tvSignoffBadge.setVisibility(View.VISIBLE);
    }

    private void showApproversEmpty() {
        boolean empty = viewModel.getDraft().approvers.isEmpty();
        binding.tvApproversEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void showPartners(@NonNull List<LogisticsPartner> partners) {
        binding.llPartners.removeAllViews();
        binding.tvPartnersEmpty.setVisibility(partners.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (LogisticsPartner partner : partners) {
            ItemSettingsPartnerBinding row = ItemSettingsPartnerBinding.inflate(inflater, binding.llPartners, false);
            bindPartner(row, partner);
            binding.llPartners.addView(row.getRoot());
        }
    }

    private void bindPartner(@NonNull ItemSettingsPartnerBinding row, @NonNull LogisticsPartner partner) {
        String name = orDash(partner.getName());
        row.tvPartnerName.setText(name);
        boolean connected = partner.isConnected();
        row.tvPartnerStatus.setText(connected ? R.string.settings_status_connected : R.string.settings_status_inactive);
        row.tvPartnerStatus.setBackgroundResource(connected
                ? R.drawable.bg_badge_market_ready : R.drawable.bg_badge_market_neutral);
        row.tvPartnerStatus.setTextColor(requireContext().getColor(connected
                ? R.color.marketBadgeReadyText : R.color.marketBadgeNeutralText));
        row.btnPartnerAction.setImageResource(connected ? R.drawable.ic_gear : R.drawable.ic_link);
        row.btnPartnerAction.setContentDescription(getString(
                connected ? R.string.cd_partner_disconnect : R.string.cd_partner_connect, name));
        row.btnPartnerAction.setOnClickListener(v -> {
            partner.setConnected(!partner.isConnected());
            viewModel.onDraftEdited();
            bindPartner(row, partner);
        });
    }

    private void showErpKey(@NonNull SecuritySettings security) {
        String key = security.getErpApiKeyMasked();
        boolean hasKey = !TextUtils.isEmpty(key);
        if (hasKey) {
            binding.tvErpKey.setText(key);
            binding.tvErpKey.setTextSize(15);
        } else {
            binding.tvErpKey.setText(R.string.settings_erp_none);
            binding.tvErpKey.setTextSize(13);
        }
        binding.btnCopyKey.setVisibility(hasKey ? View.VISIBLE : View.GONE);
        binding.btnRotateKey.setVisibility(hasKey ? View.VISIBLE : View.GONE);
        String integration = security.getErpIntegrationName();
        boolean hasIntegration = hasKey && !TextUtils.isEmpty(integration);
        binding.tvErpIntegration.setVisibility(hasIntegration ? View.VISIBLE : View.GONE);
        if (hasIntegration) {
            binding.tvErpIntegration.setText(getString(R.string.settings_erp_integration, integration));
        }
    }

    private void showVendors(@NonNull List<WhitelistedVendor> vendors) {
        binding.llVendors.removeAllViews();
        binding.tvVendorsEmpty.setVisibility(vendors.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (WhitelistedVendor vendor : vendors) {
            ItemSettingsVendorBinding row = ItemSettingsVendorBinding.inflate(inflater, binding.llVendors, false);
            row.tvVendorName.setText(orDash(vendor.getName()));
            String score = Formatters.number(vendor.getComplianceScore());
            row.tvVendorCompliance.setVisibility(score == null ? View.GONE : View.VISIBLE);
            if (score != null) {
                row.tvVendorCompliance.setText(getString(R.string.settings_compliance, score));
            }
            boolean hasBadge = !TextUtils.isEmpty(vendor.getStatusBadge());
            row.tvVendorBadge.setVisibility(hasBadge ? View.VISIBLE : View.GONE);
            if (hasBadge) {
                row.tvVendorBadge.setText(vendor.getStatusBadge().toUpperCase(java.util.Locale.getDefault()));
            }
            binding.llVendors.addView(row.getRoot());
        }
    }

    private void showTwoFactorState(@Nullable Boolean enabled) {
        boolean on = Boolean.TRUE.equals(enabled);
        binding.tvTwoFaBadge.setVisibility(on ? View.VISIBLE : View.GONE);
        binding.btnConfigure2fa.setVisibility(on ? View.GONE : View.VISIBLE);
    }

    /** Save and Cancel are only usable when something changed, and not while saving. */
    private void updateActionBar() {
        if (binding == null) {
            return;
        }
        boolean dirty = Boolean.TRUE.equals(viewModel.isDirty().getValue());
        boolean saving = Boolean.TRUE.equals(viewModel.isSaving().getValue());
        binding.btnSaveAll.setEnabled(dirty && !saving);
        binding.btnSaveAll.setText(saving ? "" : getString(R.string.settings_save_all));
        binding.progressSave.setVisibility(saving ? View.VISIBLE : View.GONE);
        binding.btnCancelChanges.setEnabled(dirty && !saving);
        backCallback.setEnabled(dirty);
    }

    // ---------------------------------------------------------------- actions

    private void saveIfChanged() {
        if (Boolean.TRUE.equals(viewModel.isDirty().getValue())) {
            viewModel.save();
        }
    }

    private void pickApprover() {
        List<ApprovalWorkflows.OrgUser> people = viewModel.getAvailableApprovers();
        if (people.isEmpty()) {
            anchored(Snackbar.make(binding.getRoot(), R.string.settings_no_more_approvers, Snackbar.LENGTH_SHORT)).show();
            return;
        }
        String[] names = new String[people.size()];
        for (int i = 0; i < names.length; i++) {
            ApprovalWorkflows.OrgUser person = people.get(i);
            String name = orDash(person.getFullName());
            names[i] = TextUtils.isEmpty(person.getJobTitle())
                    ? name : getString(R.string.settings_approver_label, name, person.getJobTitle());
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_pick_approver)
                .setItems(names, (dialog, which) -> {
                    viewModel.addApprover(people.get(which));
                    approversAdapter.setItems(viewModel.getDraft().approvers);
                    showApproversEmpty();
                })
                .show();
    }

    private void onTwoFactorSetup(@NonNull Resource<?> resource) {
        binding.btnConfigure2fa.setEnabled(resource.status != Resource.Status.LOADING);
        if (resource.status == Resource.Status.SUCCESS) {
            if (getChildFragmentManager().findFragmentByTag(TwoFactorDialogFragment.TAG) == null
                    && !getChildFragmentManager().isStateSaved()) {
                new TwoFactorDialogFragment().show(getChildFragmentManager(), TwoFactorDialogFragment.TAG);
            }
        } else if (resource.status == Resource.Status.ERROR) {
            anchored(Snackbar.make(binding.getRoot(), ResourceMessages.errorText(requireContext(), resource),
                    Snackbar.LENGTH_LONG)).show();
            viewModel.clearTwoFactor();
        }
    }

    private void showSuccess(int messageRes) {
        Snackbar snackbar = anchored(Snackbar.make(binding.getRoot(), messageRes, Snackbar.LENGTH_LONG));
        snackbar.setBackgroundTint(MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorPrimary));
        snackbar.setTextColor(MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorOnPrimary));
        snackbar.show();
    }

    private void showComingSoon() {
        anchored(Snackbar.make(binding.getRoot(), R.string.coming_soon, Snackbar.LENGTH_SHORT)).show();
    }

    /** Keeps a snackbar above the sticky Save bar instead of covering it. */
    @NonNull
    private Snackbar anchored(@NonNull Snackbar snackbar) {
        if (binding.actionBar.getVisibility() == View.VISIBLE) {
            snackbar.setAnchorView(binding.actionBar);
        }
        return snackbar;
    }

    // ---------------------------------------------------------------- small helpers

    /** Writes what the person types into the draft (but not when the boxes are being filled by the screen itself). */
    private void watch(@NonNull EditText box, @NonNull Consumer<String> setter) {
        box.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (!populating) {
                    setter.accept(s.toString());
                    viewModel.onDraftEdited();
                }
            }
        });
    }

    private static void setText(@NonNull EditText box, @NonNull String value) {
        if (!TextUtils.equals(box.getText(), value)) {
            box.setText(value);
        }
    }

    @NonNull
    private String orDash(@Nullable String value) {
        return TextUtils.isEmpty(value) ? getString(R.string.no_value) : value;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /** A dropdown list that always shows every choice, even after one has been picked. */
    private static class AllItemsAdapter extends ArrayAdapter<String> {
        private final List<String> all;

        AllItemsAdapter(@NonNull android.content.Context context, @NonNull List<String> items) {
            super(context, android.R.layout.simple_dropdown_item_1line, items);
            this.all = new ArrayList<>(items);
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    results.values = all;
                    results.count = all.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    notifyDataSetChanged();
                }
            };
        }
    }
}
