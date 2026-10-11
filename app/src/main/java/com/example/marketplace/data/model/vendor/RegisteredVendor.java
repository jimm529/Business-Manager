package com.example.marketplace.data.model.vendor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

/**
 * One supplier record from GET vendors. The server keeps two names for several fields
 * (companyName/vendorName, hqLocation/location, ...), so each getter picks whichever one is filled.
 */
public class RegisteredVendor {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_INACTIVE = "inactive";

    public static final String COMPLIANCE_COMPLIANT = "compliant";
    public static final String COMPLIANCE_PENDING = "pending";
    public static final String COMPLIANCE_EXPIRED = "expired";

    @SerializedName("_id")
    private String id;
    @SerializedName("companyName")
    private String companyName;
    @SerializedName("vendorName")
    private String vendorName;
    @SerializedName("hqLocation")
    private String hqLocation;
    @SerializedName("location")
    private String location;
    @SerializedName("materialType")
    private String materialType;
    @SerializedName("primaryMaterial")
    private String primaryMaterial;
    @SerializedName("volumeTons")
    private Double volumeTons;
    @SerializedName("monthlyCapacity")
    private Double monthlyCapacity;
    @SerializedName("complianceStatus")
    private String complianceStatus;
    @SerializedName("complianceLabel")
    private String complianceLabel;
    @SerializedName("lastAudit")
    private String lastAudit;
    @SerializedName("status")
    private String status;

    @Nullable
    public String getId() {
        return id;
    }

    @Nullable
    public String getName() {
        return firstFilled(companyName, vendorName);
    }

    @Nullable
    public String getLocation() {
        return firstFilled(hqLocation, location);
    }

    @Nullable
    public String getPrimaryMaterial() {
        return firstFilled(primaryMaterial, materialType);
    }

    /** Volume in metric tons, or null when the server has none. */
    @Nullable
    public Double getVolumeTons() {
        return volumeTons != null ? volumeTons : monthlyCapacity;
    }

    /** "compliant", "pending" or "expired" (lower case); null when unknown. */
    @Nullable
    public String getComplianceStatus() {
        return complianceStatus == null ? null : complianceStatus.trim().toLowerCase();
    }

    /** The certificate name to show in the compliance badge, e.g. "ISO 14001". */
    @Nullable
    public String getComplianceLabel() {
        return firstFilled(complianceLabel);
    }

    /** The audit time as the server sent it (ISO text). */
    @Nullable
    public String getLastAudit() {
        return lastAudit;
    }

    /** "active", "pending" or "inactive" (lower case); null when unknown. */
    @Nullable
    public String getStatus() {
        return status == null ? null : status.trim().toLowerCase();
    }

    /** True when everything the card shows is the same, so the list does not redraw needlessly. */
    public boolean sameContentAs(@NonNull RegisteredVendor other) {
        return Objects.equals(getName(), other.getName())
                && Objects.equals(getLocation(), other.getLocation())
                && Objects.equals(getPrimaryMaterial(), other.getPrimaryMaterial())
                && Objects.equals(getVolumeTons(), other.getVolumeTons())
                && Objects.equals(getComplianceStatus(), other.getComplianceStatus())
                && Objects.equals(getComplianceLabel(), other.getComplianceLabel())
                && Objects.equals(lastAudit, other.lastAudit)
                && Objects.equals(getStatus(), other.getStatus());
    }

    @Nullable
    private static String firstFilled(@Nullable String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
}
