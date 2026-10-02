package com.example.marketplace.ui.auth;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/** Which form fields failed validation. A null value means that field is fine. */
public class FormErrors {
    @Nullable @StringRes public Integer fullName;
    @Nullable @StringRes public Integer jobTitle;
    @Nullable @StringRes public Integer email;
    @Nullable @StringRes public Integer password;
    @Nullable @StringRes public Integer confirmPassword;

    public boolean hasErrors() {
        return fullName != null || jobTitle != null || email != null
                || password != null || confirmPassword != null;
    }
}
