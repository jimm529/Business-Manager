package com.example.marketplace.data.model.auth;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

/** Body of POST auth/google: the ID token Google gave the phone, and which portal the person chose. */
public class GoogleLoginRequest {

    @SerializedName("credential")
    private final String credential;
    @SerializedName("portal")
    private final String portal;

    public GoogleLoginRequest(@NonNull String credential, @NonNull String portal) {
        this.credential = credential;
        this.portal = portal;
    }
}
