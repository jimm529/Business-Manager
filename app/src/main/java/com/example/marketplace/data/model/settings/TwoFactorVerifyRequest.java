package com.example.marketplace.data.model.settings;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

/** Body of POST settings/2fa/verify: the 6-digit code from the authenticator app. */
public class TwoFactorVerifyRequest {

    @SerializedName("code")
    private final String code;

    public TwoFactorVerifyRequest(@NonNull String code) {
        this.code = code;
    }
}
