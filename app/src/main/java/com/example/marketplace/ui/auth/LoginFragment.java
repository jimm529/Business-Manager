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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.marketplace.BuildConfig;
import com.example.marketplace.R;
import com.example.marketplace.data.model.auth.AuthResponse;
import com.example.marketplace.databinding.FragmentLoginBinding;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

/** Login screen: pick a portal, enter email and password, call POST auth/login. */
public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthViewModel viewModel;
    /** The "cannot reach server" message that stays on screen until the person acts on it. */
    @Nullable
    private Snackbar connectionSnackbar;

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

        setUpServerConnectionTool();

        viewModel.getFormErrors().observe(getViewLifecycleOwner(), errors -> {
            binding.layoutEmail.setError(errors.email == null ? null : getString(errors.email));
            binding.layoutPassword.setError(errors.password == null ? null : getString(errors.password));
        });
        viewModel.getAuthResult().observe(getViewLifecycleOwner(), this::render);
    }

    /**
     * Developer tool (debug builds only): a server button in the header, or a long-press on SUPPORT
     * or the logo, opens the Server Connection sheet where the backend address can be tested and changed.
     */
    private void setUpServerConnectionTool() {
        if (!BuildConfig.DEBUG) {
            return;
        }
        binding.btnServer.setVisibility(View.VISIBLE);
        binding.btnServer.setOnClickListener(v -> openServerSheet());
        View.OnLongClickListener open = v -> {
            openServerSheet();
            return true;
        };
        binding.btnSupport.setOnLongClickListener(open);
        binding.imgLogo.setOnLongClickListener(open);

        getParentFragmentManager().setFragmentResultListener(
                ServerConfigBottomSheet.RESULT_KEY, getViewLifecycleOwner(), (key, result) ->
                        showMessage(getString(R.string.server_url_saved,
                                result.getString(ServerConfigBottomSheet.RESULT_URL, ""))));
    }

    private void openServerSheet() {
        dismissConnectionSnackbar();
        if (getParentFragmentManager().findFragmentByTag(ServerConfigBottomSheet.TAG) == null) {
            new ServerConfigBottomSheet().show(getParentFragmentManager(), ServerConfigBottomSheet.TAG);
        }
    }

    private void submit() {
        dismissConnectionSnackbar();
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
                showError(result);
                viewModel.consumeAuthResult();
                break;
            default:
                break;
        }
    }

    /**
     * Shows the server's own message. When the server could not be reached at all (debug builds),
     * the message stays up with a "Change server IP" button, and tapping the message does the same.
     */
    private void showError(Resource<AuthResponse> result) {
        String message = ResourceMessages.errorText(requireContext(), result);
        if (BuildConfig.DEBUG && ResourceMessages.isConnectionProblem(result)) {
            connectionSnackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.change_server_ip, v -> openServerSheet())
                    .setTextMaxLines(4);
            connectionSnackbar.getView().setOnClickListener(v -> openServerSheet());
            connectionSnackbar.show();
        } else {
            showMessage(message);
        }
    }

    private void dismissConnectionSnackbar() {
        if (connectionSnackbar != null) {
            connectionSnackbar.dismiss();
            connectionSnackbar = null;
        }
    }

    private void setLoading(boolean loading) {
        binding.btnLogin.setEnabled(!loading);
        binding.btnLogin.setText(loading ? "" : getString(R.string.access_portal));
        binding.progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.portalSelector.cardVendor.setEnabled(!loading);
        binding.portalSelector.cardOperator.setEnabled(!loading);
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
        dismissConnectionSnackbar();
        binding = null;
    }
}
