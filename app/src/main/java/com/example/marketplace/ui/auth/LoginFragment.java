package com.example.marketplace.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.marketplace.BuildConfig;
import com.example.marketplace.R;
import com.example.marketplace.data.api.ApiClient;
import com.example.marketplace.data.model.auth.AuthResponse;
import com.example.marketplace.databinding.DialogServerUrlBinding;
import com.example.marketplace.databinding.FragmentLoginBinding;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.example.marketplace.utils.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

/** Login screen: pick a portal, enter email and password, call POST auth/login. */
public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthViewModel viewModel;
    @Nullable
    private AlertDialog serverUrlDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        PortalSelector.bind(binding.portalSelector, viewModel, getViewLifecycleOwner());

        clearErrorWhenTyping(binding.layoutEmail);
        clearErrorWhenTyping(binding.layoutPassword);

        binding.btnLogin.setOnClickListener(v -> submit());
        binding.etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });

        binding.linkRegister.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_login_to_register));

        // These need backend features that do not exist yet (see API_NOTES.md).
        View.OnClickListener notAvailable = v -> showMessage(getString(R.string.feature_unavailable));
        binding.btnSupport.setOnClickListener(notAvailable);
        binding.btnForgot.setOnClickListener(notAvailable);
        binding.btnGoogle.setOnClickListener(notAvailable);
        binding.btnSso.setOnClickListener(notAvailable);

        // Developer tool: long-press SUPPORT or the logo to change the backend address (debug builds only).
        if (BuildConfig.DEBUG) {
            View.OnLongClickListener openServerDialog = v -> {
                showServerUrlDialog();
                return true;
            };
            binding.btnSupport.setOnLongClickListener(openServerDialog);
            binding.imgLogo.setOnLongClickListener(openServerDialog);
        }

        viewModel.getFormErrors().observe(getViewLifecycleOwner(), errors -> {
            binding.layoutEmail.setError(errors.email == null ? null : getString(errors.email));
            binding.layoutPassword.setError(errors.password == null ? null : getString(errors.password));
        });
        viewModel.getAuthResult().observe(getViewLifecycleOwner(), this::render);
    }

    private void submit() {
        viewModel.login(
                binding.etEmail.getText() == null ? null : binding.etEmail.getText().toString(),
                binding.etPassword.getText() == null ? null : binding.etPassword.getText().toString());
    }

    private void render(Resource<AuthResponse> result) {
        setLoading(result.status == Resource.Status.LOADING);
        switch (result.status) {
            case SUCCESS:
                // The token is saved; open Home and drop this screen so Back does not return here.
                viewModel.consumeAuthResult();
                NavHostFragment.findNavController(this).navigate(R.id.action_global_homeFragment);
                break;
            case ERROR:
                showMessage(ResourceMessages.errorText(requireContext(), result));
                viewModel.consumeAuthResult();
                break;
            default:
                break;
        }
    }

    private void setLoading(boolean loading) {
        binding.btnLogin.setEnabled(!loading);
        binding.btnLogin.setText(loading ? "" : getString(R.string.access_portal));
        binding.progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.portalSelector.cardVendor.setEnabled(!loading);
        binding.portalSelector.cardOperator.setEnabled(!loading);
    }

    /** Lets a developer view and change the backend address, e.g. localhost (adb reverse) or a Wi-Fi IP. */
    private void showServerUrlDialog() {
        if (serverUrlDialog != null && serverUrlDialog.isShowing()) {
            return;
        }
        SessionManager session = SessionManager.getInstance();
        DialogServerUrlBinding dialogBinding = DialogServerUrlBinding.inflate(getLayoutInflater());
        dialogBinding.etServerUrl.setText(session.getBaseUrl());

        serverUrlDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.server_url_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.server_url_save, null)
                .setNeutralButton(R.string.server_url_reset, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        serverUrlDialog.setOnShowListener(d -> {
            // Set the listeners here so an invalid address keeps the dialog open.
            serverUrlDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                CharSequence typed = dialogBinding.etServerUrl.getText();
                String url = typed == null ? "" : typed.toString().trim();
                if (ApiClient.toApiBaseUrl(url) == null || url.isEmpty()) {
                    dialogBinding.layoutServerUrl.setError(getString(R.string.server_url_invalid));
                    return;
                }
                applyServerUrl(session, url);
                serverUrlDialog.dismiss();
            });
            serverUrlDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                applyServerUrl(session, null);
                serverUrlDialog.dismiss();
            });
        });
        serverUrlDialog.show();
    }

    private void applyServerUrl(SessionManager session, @Nullable String url) {
        session.setBaseUrl(url);
        ApiClient.reset();
        showMessage(getString(R.string.server_url_saved, session.getBaseUrl()));
    }

    private void showMessage(String message) {
        if (binding != null) {
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    private static void clearErrorWhenTyping(TextInputLayout layout) {
        if (layout.getEditText() == null) {
            return;
        }
        layout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (serverUrlDialog != null) {
            serverUrlDialog.dismiss();
            serverUrlDialog = null;
        }
        binding = null;
    }
}
