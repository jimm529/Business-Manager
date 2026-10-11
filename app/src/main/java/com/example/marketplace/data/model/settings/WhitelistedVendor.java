package com.example.marketplace.data.model.settings;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** Card 7: a vendor the operator trusts. The server does not send any yet, so the list is normally empty. */
public class WhitelistedVendor {

    @SerializedName("name")
    private String name;
    @SerializedName("complianceScore")
    private Double complianceScore;
    @SerializedName("statusBadge")
    private String statusBadge;

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public Double getComplianceScore() {
        return complianceScore;
    }

    @Nullable
    public String getStatusBadge() {
        return statusBadge;
    }
}
