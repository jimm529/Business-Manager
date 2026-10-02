package com.example.marketplace.ui.auth;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ImageViewCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.marketplace.BuildConfig;
import com.example.marketplace.R;
import com.example.marketplace.data.api.ApiClient;
import com.example.marketplace.data.model.common.HealthResponse;
import com.example.marketplace.databinding.BottomSheetServerConfigBinding;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.example.marketplace.utils.ServerUrls;
import com.example.marketplace.utils.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.color.MaterialColors;

/**
 * Developer tool (debug builds): choose how this phone reaches the backend, test it with
 * GET health, and save it. Opens from the Login screen.
 */
public class ServerConfigBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "ServerConfigBottomSheet";
    /** The Login screen listens for this result to show "Server set to ...". */
    public static final String RESULT_KEY = "server_config_result";
    public static final String RESULT_URL = "url";

    private BottomSheetServerConfigBinding binding;
    private ServerConfigViewModel viewModel;
    /** True while the code (not the user) changes the text or the chips, so the two do not loop. */
    private boolean syncing;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetServerConfigBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ServerConfigViewModel.class);

        binding.chipUsb.setText(getString(R.string.server_chip_usb, getString(R.string.server_preset_usb_url)));
        binding.chipEmulator.setText(getString(R.string.server_chip_emulator, getString(R.string.server_preset_emulator_url)));
        binding.chipLan.setText(getString(R.string.server_chip_lan, getString(R.string.server_preset_lan_example)));

        // First time: show the address the app uses now. After rotation Android restores the text.
        if (savedInstanceState == null) {
            setUrlText(SessionManager.getInstance().getBaseUrl());
        }
        syncChipsWithText();

        binding.chipPresets.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (syncing || checkedIds.isEmpty()) {
                return;
            }
            onPresetChosen(checkedIds.get(0));
        });

        binding.etServerUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (syncing) {
                    return;
                }
                // The old test result no longer describes this address.
                binding.layoutServerUrl.setError(null);
                viewModel.clearResult();
                syncChipsWithText();
            }
        });

        binding.btnTest.setOnClickListener(v -> {
            String url = readValidUrl();
            if (url != null) {
                viewModel.test(url);
            }
        });
        binding.btnSave.setOnClickListener(v -> save());
        binding.btnUseDefault.setOnClickListener(v -> {
            setUrlText(BuildConfig.BASE_URL);
            viewModel.clearResult();
            syncChipsWithText();
        });

        viewModel.getHealth().observe(getViewLifecycleOwner(), this::renderTestResult);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Open fully, so the keyboard does not hide the buttons.
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetBehavior<?> behavior = ((BottomSheetDialog) getDialog()).getBehavior();
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
    }

    // ------------------------------------------------------------- presets

    private void onPresetChosen(int chipId) {
        if (chipId == R.id.chipUsb) {
            setUrlText(getString(R.string.server_preset_usb_url));
        } else if (chipId == R.id.chipEmulator) {
            setUrlText(getString(R.string.server_preset_emulator_url));
        } else if (chipId == R.id.chipLan) {
            // Start the Wi-Fi address for the person to finish typing.
            setUrlText(getString(R.string.server_preset_lan_prefix));
            binding.etServerUrl.requestFocus();
        }
        binding.layoutServerUrl.setError(null);
        viewModel.clearResult();
    }

    /** Ticks the chip that matches the typed address; anything else counts as a custom LAN address. */
    private void syncChipsWithText() {
        String typed = ServerUrls.normalize(textOf());
        int chipId = R.id.chipLan;
        if (typed != null) {
            if (typed.equals(ServerUrls.normalize(getString(R.string.server_preset_usb_url)))) {
                chipId = R.id.chipUsb;
            } else if (typed.equals(ServerUrls.normalize(getString(R.string.server_preset_emulator_url)))) {
                chipId = R.id.chipEmulator;
            }
        }
        syncing = true;
        binding.chipPresets.check(chipId);
        syncing = false;
    }

    // ---------------------------------------------------------- test / save

    private void renderTestResult(Resource<HealthResponse> result) {
        boolean testing = result.status == Resource.Status.LOADING;
        binding.btnTest.setEnabled(!testing);
        binding.statusRow.setVisibility(result.status == Resource.Status.IDLE ? View.GONE : View.VISIBLE);
        binding.progressTest.setVisibility(testing ? View.VISIBLE : View.GONE);
        binding.imgStatus.setVisibility(testing ? View.GONE : View.VISIBLE);

        switch (result.status) {
            case LOADING:
                binding.tvStatus.setText(R.string.server_testing);
                break;
            case SUCCESS:
                HealthResponse health = result.data;
                String database = health == null || health.getDatabase() == null
                        ? getString(R.string.no_value)
                        : health.getDatabase();
                if (health != null && health.isDatabaseConnected()) {
                    showStatus(R.drawable.ic_check_circle, com.google.android.material.R.attr.colorPrimary,
                            getString(R.string.server_test_ok, database));
                } else {
                    // The server answered, but it cannot reach MongoDB, so logging in will not work yet.
                    showStatus(R.drawable.ic_error, com.google.android.material.R.attr.colorError,
                            getString(R.string.server_test_db_down, database));
                }
                break;
            case ERROR:
                showStatus(R.drawable.ic_error, com.google.android.material.R.attr.colorError,
                        ResourceMessages.errorText(requireContext(), result));
                break;
            default:
                break;
        }
    }

    private void showStatus(int iconRes, int tintAttr, String text) {
        binding.imgStatus.setImageResource(iconRes);
        ImageViewCompat.setImageTintList(binding.imgStatus,
                ColorStateList.valueOf(MaterialColors.getColor(binding.getRoot(), tintAttr)));
        binding.tvStatus.setText(text);
    }

    private void save() {
        String url = readValidUrl();
        if (url == null) {
            return;
        }
        SessionManager.getInstance().setBaseUrl(url);
        ApiClient.reset();

        Bundle result = new Bundle();
        result.putString(RESULT_URL, url);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        dismiss();
    }

    /** The typed address cleaned up (also written back into the field), or null with an error shown. */
    @Nullable
    private String readValidUrl() {
        String url = ServerUrls.normalize(textOf());
        if (url == null) {
            binding.layoutServerUrl.setError(getString(R.string.server_url_invalid));
            return null;
        }
        if (!url.equals(textOf())) {
            setUrlText(url);
        }
        return url;
    }

    private String textOf() {
        return binding.etServerUrl.getText() == null ? "" : binding.etServerUrl.getText().toString().trim();
    }

    private void setUrlText(String text) {
        syncing = true;
        binding.etServerUrl.setText(text);
        binding.etServerUrl.setSelection(text.length());
        syncing = false;
        syncChipsWithText();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
