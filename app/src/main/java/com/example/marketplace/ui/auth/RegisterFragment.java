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

import com.example.marketplace.R;
import com.example.marketplace.data.model.auth.AuthResponse;
import com.example.marketplace.data.model.auth.User;
import com.example.marketplace.databinding.FragmentRegisterBinding;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

/** Sign-up screen: pick a portal, fill the form, call POST auth/register. */
public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private AuthViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        PortalSelector.bind(binding.portalSelector, viewModel, getViewLifecycleOwner());

        clearErrorWhenTyping(binding.layoutFullName);
        clearErrorWhenTyping(binding.layoutJobTitle);
        clearErrorWhenTyping(binding.layoutEmail);
        clearErrorWhenTyping(binding.layoutPassword);
        clearErrorWhenTyping(binding.layoutConfirmPassword);

        binding.btnRegister.setOnClickListener(v -> submit());
        binding.etConfirmPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });

        binding.linkSignIn.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_register_to_login));

        viewModel.getFormErrors().observe(getViewLifecycleOwner(), errors -> {
            binding.layoutFullName.setError(errors.fullName == null ? null : getString(errors.fullName));
            binding.layoutJobTitle.setError(errors.jobTitle == null ? null : getString(errors.jobTitle));
            binding.layoutEmail.setError(errors.email == null ? null : getString(errors.email));
            binding.layoutPassword.setError(errors.password == null ? null : getString(errors.password));
            binding.layoutConfirmPassword.setError(
                    errors.confirmPassword == null ? null : getString(errors.confirmPassword));
        });
        viewModel.getAuthResult().observe(getViewLifecycleOwner(), this::render);
    }

    private void submit() {
        viewModel.register(
                text(binding.etFullName),
                text(binding.etJobTitle),
                text(binding.etEmail),
                text(binding.etPassword),
                text(binding.etConfirmPassword));
    }

    @Nullable
    private static String text(com.google.android.material.textfield.TextInputEditText editText) {
        return editText.getText() == null ? null : editText.getText().toString();
    }

    private void render(Resource<AuthResponse> result) {
        setLoading(result.status == Resource.Status.LOADING);
        switch (result.status) {
            case SUCCESS:
                User user = result.data == null ? null : result.data.getUser();
                String name = user == null ? null : user.getFullName();
                showMessage(name == null || name.isEmpty()
                        ? getString(R.string.account_created)
                        : getString(R.string.account_created_for, name));
                viewModel.consumeAuthResult();
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
        binding.btnRegister.setEnabled(!loading);
        binding.btnRegister.setText(loading ? "" : getString(R.string.create_account));
        binding.progressRegister.setVisibility(loading ? View.VISIBLE : View.GONE);
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
        binding = null;
    }
}
