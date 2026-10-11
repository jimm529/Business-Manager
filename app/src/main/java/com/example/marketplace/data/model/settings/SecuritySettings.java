package com.example.marketplace.data.model.settings;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Card 5. The server sends the 2FA amount threshold here; whether 2FA is on comes from the account
 * details, and the ERP key fields are only filled when the server provides them (today it does not).
 */
public class SecuritySettings {

    @SerializedName("twoFactorThreshold")
    private Double twoFactorThreshold;
    private transient boolean twoFactorEnabled;
    @SerializedName("erpApiKeyMasked")
    private String erpApiKeyMasked;
    @SerializedName("erpIntegrationName")
    private String erpIntegrationName;

    @Nullable
    public Double getTwoFactorThreshold() {
        return twoFactorThreshold;
    }

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }

    @Nullable
    public String getErpApiKeyMasked() {
        return erpApiKeyMasked;
    }

    @Nullable
    public String getErpIntegrationName() {
        return erpIntegrationName;
    }
}
