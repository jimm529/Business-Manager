package com.example.marketplace.ui.vendorsettings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.marketplace.R;
import com.example.marketplace.data.model.listing.ListingOptionsResponse;
import com.example.marketplace.data.model.vendor.VendorSettingsResponse;
import com.example.marketplace.databinding.FragmentVendorSettingsBinding;
import com.example.marketplace.databinding.ItemCertificationRowBinding;
import com.example.marketplace.ui.common.AllChoicesAdapter;
import com.example.marketplace.ui.messages.ActionResult;
import com.example.marketplace.ui.settings.ChangePasswordDialogFragment;
import com.example.marketplace.ui.settings.SettingsViewModel;
import com.example.marketplace.ui.settings.TwoFactorDialogFragment;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Formatters;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * The vendor's Settings: company profile, certifications, operational preferences, notifications and
 * login and security. Each card has its own Save button; the notification switches save when flipped.
 */
public class VendorSettingsFragment extends Fragment {

    private static final int MAX_FACILITY_IMAGES = 3;

    private FragmentVendorSettingsBinding binding;
    private VendorSettingsViewModel viewModel;
    private SettingsViewModel securityViewModel;

    /** The server answer the boxes were last filled from, so a save does not wipe what is typed elsewhere. */
    @Nullable
    private VendorSettingsResponse filledFrom;
    private List<String> purityChoices = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVendorSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(VendorSettingsViewModel.class);
        // The password and 2FA dialogs look for this view model in their parent screen.
        securityViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        binding.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        binding.swipeRefresh.setColorSchemeColors(
                MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorPrimary));
        binding.swipeRefresh.setOnRefreshListener(() -> {
            filledFrom = null;
            viewModel.refresh();
        });
        binding.states.btnRetry.setOnClickListener(v -> {
            filledFrom = null;
            viewModel.refresh();
        });

        binding.btnAddCert.setOnClickListener(v -> comingSoon());
        binding.btnSaveProfile.setOnClickListener(v -> saveProfile());
        binding.btnSavePrefs.setOnClickListener(v -> savePreferences());
        binding.btnSaveAccount.setOnClickListener(v -> saveAccount());
        binding.btnPassword.setOnClickListener(v -> {
            if (getChildFragmentManager().findFragmentByTag(ChangePasswordDialogFragment.TAG) == null) {
                new ChangePasswordDialogFragment().show(getChildFragmentManager(), ChangePasswordDialogFragment.TAG);
            }
        });
        binding.btn2fa.setOnClickListener(v -> securityViewModel.startTwoFactorSetup());

        setUpSwitch(binding.swOrders, "newOrderRequests");
        setUpSwitch(binding.swSales, "successfulSales");
        setUpSwitch(binding.swLogistics, "logisticsUpdates");
        setUpSwitch(binding.swPrice, "priceAlerts");

        viewModel.getSettings().observe(getViewLifecycleOwner(), this::renderSettings);
        viewModel.getOptions().observe(getViewLifecycleOwner(), this::renderOptions);
        viewModel.isSaving().observe(getViewLifecycleOwner(), this::renderSaving);
        viewModel.getResults().observe(getViewLifecycleOwner(), this::showResult);
        viewModel.getNotificationReverts().observe(getViewLifecycleOwner(), this::revertSwitch);

        securityViewModel.getTwoFactorSetupState().observe(getViewLifecycleOwner(), this::onTwoFactorSetup);
        securityViewModel.getSuccessMessages().observe(getViewLifecycleOwner(), event -> {
            Integer message = event == null ? null : event.getContentIfNotHandled();
            if (message != null) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    // ---------------------------------------------------------------- loading

    private void renderSettings(Resource<VendorSettingsResponse> resource) {
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
                if (resource.data == null || resource.data.getSettings() == null) {
                    binding.content.setVisibility(View.GONE);
                    binding.states.tvEmpty.setText(R.string.error_generic);
                    binding.states.emptyGroup.setVisibility(View.VISIBLE);
                } else {
                    binding.content.setVisibility(View.VISIBLE);
                    // Fill the boxes once per server answer; a rotation keeps what was typed.
                    if (filledFrom != resource.data) {
                        filledFrom = resource.data;
                        fill(resource.data);
                    }
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

    private void renderOptions(Resource<ListingOptionsResponse> resource) {
        if (resource.status == Resource.Status.SUCCESS) {
            purityChoices = VendorSettingsViewModel.purityChoices(resource.data);
            showPurityChoices(String.valueOf(binding.actPurity.getText()));
        }
    }

    /** Fills every card from the server's answer. */
    private void fill(@NonNull VendorSettingsResponse response) {
        VendorSettingsResponse.Settings settings = response.getSettings();
        if (settings == null) {
            return;
        }
        binding.etEntity.setText(settings.getEntityName());
        binding.etTax.setText(settings.getTaxId());
        binding.etAddress.setText(settings.getHeadquartersAddress());
        showFacilityImages(settings.getFacilityImages());
        showCertifications(settings.getCertifications());

        VendorSettingsResponse.OperationalPreferences prefs = settings.getOperationalPreferences();
        binding.etYield.setText(prefs == null ? "" : plain(prefs.getMaterialYield()));
        binding.etCapacity.setText(prefs == null ? "" : plain(prefs.getPlantCapacity()));
        showPurityChoices(prefs == null || prefs.getTypicalLeadPurity() == null ? "" : prefs.getTypicalLeadPurity());
        List<String> defaults = settings.getListingDefaults();
        binding.cbFob.setChecked(defaults.contains(VendorSettingsResponse.DEFAULT_FOB));
        binding.cbDdp.setChecked(defaults.contains(VendorSettingsResponse.DEFAULT_DDP));
        binding.cbNet30.setChecked(defaults.contains(VendorSettingsResponse.DEFAULT_NET30));

        VendorSettingsResponse.NotificationPreferences notifications = settings.getNotificationPreferences();
        setSwitch(binding.swOrders, notifications != null && notifications.isNewOrderRequests());
        setSwitch(binding.swSales, notifications != null && notifications.isSuccessfulSales());
        setSwitch(binding.swLogistics, notifications != null && notifications.isLogisticsUpdates());
        setSwitch(binding.swPrice, notifications != null && notifications.isPriceAlerts());

        VendorSettingsResponse.User user = response.getUser();
        binding.etName.setText(user == null ? "" : user.getFullName());
        binding.etRole.setText(user == null ? "" : user.getJobTitle());
        binding.etEmail.setText(user == null ? "" : user.getEmail());
        securityViewModel.setTwoFactorEnabled(user != null && user.isTwoFactorEnabled());
    }

    private void showPurityChoices(@NonNull String current) {
        List<String> choices = new ArrayList<>(purityChoices);
        // The saved value is always selectable, even if no material uses that exact purity.
        if (!current.trim().isEmpty() && !choices.contains(current.trim())) {
            choices.add(0, current.trim());
        }
        binding.actPurity.setAdapter(new AllChoicesAdapter(requireContext(), choices));
        binding.actPurity.setText(current, false);
    }

    // ---------------------------------------------------------------- facility photos and certifications

    private void showFacilityImages(@NonNull List<String> images) {
        binding.facilityRow.removeAllViews();

        // The first tile adds a photo; uploading stores a file in the cloud, so it is switched off for now.
        LinearLayout upload = new LinearLayout(requireContext());
        upload.setOrientation(LinearLayout.VERTICAL);
        upload.setGravity(android.view.Gravity.CENTER);
        upload.setBackgroundResource(R.drawable.bg_dashed_box);
        upload.setClickable(true);
        upload.setOnClickListener(v -> comingSoon());
        ImageView camera = new ImageView(requireContext());
        camera.setImageResource(R.drawable.ic_camera);
        camera.setImageTintList(android.content.res.ColorStateList.valueOf(MaterialColors.getColor(
                binding.getRoot(), com.google.android.material.R.attr.colorOnSurfaceVariant)));
        camera.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        upload.addView(camera, new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView label = new TextView(requireContext());
        label.setText(R.string.vsettings_upload);
        label.setTextSize(10);
        label.setTextColor(MaterialColors.getColor(binding.getRoot(),
                com.google.android.material.R.attr.colorOnSurfaceVariant));
        label.setPadding(0, dp(4), 0, 0);
        upload.addView(label);
        binding.facilityRow.addView(upload, tileParams(true));

        for (int i = 0; i < Math.min(images.size(), MAX_FACILITY_IMAGES); i++) {
            ImageView image = new ImageView(requireContext());
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackgroundResource(R.drawable.bg_image_placeholder);
            image.setContentDescription(getString(R.string.cd_facility_photo));
            image.setClipToOutline(true);
            Glide.with(image).load(images.get(i)).centerCrop().into(image);
            binding.facilityRow.addView(image, tileParams(false));
        }
    }

    private LinearLayout.LayoutParams tileParams(boolean first) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(100), 1f);
        if (!first) {
            params.setMarginStart(dp(8));
        }
        return params;
    }

    private void showCertifications(@NonNull List<VendorSettingsResponse.Certification> certifications) {
        binding.certList.removeAllViews();
        binding.tvCertsEmpty.setVisibility(certifications.isEmpty() ? View.VISIBLE : View.GONE);
        SimpleDateFormat monthYear = new SimpleDateFormat("MM/yyyy", Locale.US);
        monthYear.setTimeZone(TimeZone.getTimeZone("UTC"));
        for (VendorSettingsResponse.Certification cert : certifications) {
            ItemCertificationRowBinding row = ItemCertificationRowBinding.inflate(getLayoutInflater(), binding.certList, false);
            row.tvName.setText(cert.getName() == null ? getString(R.string.no_value) : cert.getName());

            String status = cert.getStatus() == null ? "" : cert.getStatus().trim().toLowerCase(Locale.ROOT);
            int stripe;
            int stateLabel;
            int background;
            int text;
            if ("active".equals(status)) {
                stripe = MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorSecondary);
                stateLabel = R.string.vsettings_cert_active;
                background = R.drawable.bg_badge_market_ready;
                text = R.color.marketBadgeReadyText;
            } else if ("expired".equals(status)) {
                stripe = MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorError);
                stateLabel = R.string.vsettings_cert_expired;
                background = R.drawable.bg_badge_urgent;
                text = R.color.badgeUrgentText;
            } else {
                stripe = requireContext().getColor(R.color.statusProcessing);
                stateLabel = R.string.vsettings_cert_pending;
                background = R.drawable.bg_badge_market_extraction;
                text = R.color.marketBadgeExtractionText;
            }
            row.viewStripe.setBackgroundColor(stripe);
            row.tvState.setText(stateLabel);
            row.tvState.setBackgroundResource(background);
            row.tvState.setTextColor(requireContext().getColor(text));

            Date expiry = Formatters.parseIso(cert.getExpiryDate());
            if (expiry != null) {
                row.tvExpiry.setText(getString(R.string.vsettings_cert_exp, monthYear.format(expiry)));
            } else if (!"active".equals(status) && !"expired".equals(status)) {
                row.tvExpiry.setText(R.string.vsettings_cert_review);
            } else {
                row.tvExpiry.setVisibility(View.GONE);
            }
            binding.certList.addView(row.getRoot());
        }
    }

    // ---------------------------------------------------------------- saving

    private void saveProfile() {
        String entity = text(binding.etEntity);
        String tax = text(binding.etTax);
        String address = text(binding.etAddress);
        binding.tilEntity.setError(entity.isEmpty() ? getString(R.string.vsettings_error_entity) : null);
        binding.tilTax.setError(tax.isEmpty() ? getString(R.string.vsettings_error_tax) : null);
        binding.tilAddress.setError(address.isEmpty() ? getString(R.string.vsettings_error_address) : null);
        if (entity.isEmpty() || tax.isEmpty() || address.isEmpty()) {
            return;
        }
        viewModel.saveProfile(entity, tax, address);
    }

    private void savePreferences() {
        Double yield = parse(text(binding.etYield));
        Double capacity = parse(text(binding.etCapacity));
        boolean yieldBad = yield == null || yield < 0 || yield > 100;
        boolean capacityBad = capacity == null || capacity < 0;
        binding.tilYield.setError(yieldBad ? getString(R.string.vsettings_error_yield) : null);
        binding.tilCapacity.setError(capacityBad ? getString(R.string.vsettings_error_capacity) : null);
        if (yieldBad || capacityBad) {
            return;
        }
        List<String> defaults = new ArrayList<>();
        if (binding.cbFob.isChecked()) {
            defaults.add(VendorSettingsResponse.DEFAULT_FOB);
        }
        if (binding.cbDdp.isChecked()) {
            defaults.add(VendorSettingsResponse.DEFAULT_DDP);
        }
        if (binding.cbNet30.isChecked()) {
            defaults.add(VendorSettingsResponse.DEFAULT_NET30);
        }
        viewModel.savePreferences(yield, capacity, String.valueOf(binding.actPurity.getText()).trim(), defaults);
    }

    private void saveAccount() {
        String name = text(binding.etName);
        String role = text(binding.etRole);
        binding.tilName.setError(name.isEmpty() ? getString(R.string.vsettings_error_name) : null);
        binding.tilRole.setError(role.isEmpty() ? getString(R.string.vsettings_error_role) : null);
        if (name.isEmpty() || role.isEmpty()) {
            return;
        }
        viewModel.saveAccount(name, role);
    }

    private void setUpSwitch(@NonNull MaterialSwitch toggle, @NonNull String key) {
        toggle.setOnCheckedChangeListener((CompoundButton button, boolean checked) -> viewModel.saveNotification(key, checked));
    }

    /** Sets a switch to what the server says without sending that back as a change. */
    private void setSwitch(@NonNull MaterialSwitch toggle, boolean checked) {
        toggle.setOnCheckedChangeListener(null);
        toggle.setChecked(checked);
        String key = toggle == binding.swOrders ? "newOrderRequests"
                : toggle == binding.swSales ? "successfulSales"
                : toggle == binding.swLogistics ? "logisticsUpdates" : "priceAlerts";
        setUpSwitch(toggle, key);
    }

    private void revertSwitch(@Nullable Event<String> event) {
        String key = event == null ? null : event.getContentIfNotHandled();
        if (key == null) {
            return;
        }
        MaterialSwitch toggle = "newOrderRequests".equals(key) ? binding.swOrders
                : "successfulSales".equals(key) ? binding.swSales
                : "logisticsUpdates".equals(key) ? binding.swLogistics : binding.swPrice;
        setSwitch(toggle, !toggle.isChecked());
    }

    private void renderSaving(@Nullable Boolean saving) {
        boolean busy = Boolean.TRUE.equals(saving);
        binding.btnSaveProfile.setEnabled(!busy);
        binding.btnSavePrefs.setEnabled(!busy);
        binding.btnSaveAccount.setEnabled(!busy);
    }

    private void showResult(@Nullable Event<ActionResult> event) {
        ActionResult result = event == null ? null : event.getContentIfNotHandled();
        if (result != null) {
            Snackbar.make(binding.getRoot(), result.text(requireContext()), Snackbar.LENGTH_LONG).show();
        }
    }

    // ---------------------------------------------------------------- two-factor

    private void onTwoFactorSetup(@NonNull Resource<?> resource) {
        binding.btn2fa.setEnabled(resource.status != Resource.Status.LOADING);
        if (resource.status == Resource.Status.SUCCESS) {
            if (getChildFragmentManager().findFragmentByTag(TwoFactorDialogFragment.TAG) == null
                    && !getChildFragmentManager().isStateSaved()) {
                new TwoFactorDialogFragment().show(getChildFragmentManager(), TwoFactorDialogFragment.TAG);
            }
        } else if (resource.status == Resource.Status.ERROR) {
            Snackbar.make(binding.getRoot(), ResourceMessages.errorText(requireContext(), resource),
                    Snackbar.LENGTH_LONG).show();
            securityViewModel.clearTwoFactor();
        }
    }

    // ---------------------------------------------------------------- helpers

    private static String text(com.google.android.material.textfield.TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    /** Reads a typed number; a comma counts as the decimal point. Null when empty or not a number. */
    @Nullable
    private static Double parse(@NonNull String text) {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(text.replace(',', '.'));
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }

    /** 98.5 -> "98.5", 1250.0 -> "1250": no grouping, no trailing zeros, so it can be edited. */
    private static String plain(@Nullable Double value) {
        return value == null ? "" : BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
