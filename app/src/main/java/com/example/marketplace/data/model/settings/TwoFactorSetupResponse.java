package com.example.marketplace.data.model.settings;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** Answer of POST settings/2fa/setup: the secret to type into an authenticator app. */
public class TwoFactorSetupResponse {

    @SerializedName("manualSecret")
    private String manualSecret;

    @Nullable
    public String getManualSecret() {
        return manualSecret;
    }
}
