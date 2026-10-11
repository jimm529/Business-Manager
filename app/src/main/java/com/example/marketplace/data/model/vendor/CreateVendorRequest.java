package com.example.marketplace.data.model.vendor;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

/**
 * Body of POST vendors. Besides the five fields on the vendor card, the server insists on a licence
 * number, a lead manager and an e-mail address.
 */
public class CreateVendorRequest {

    @SerializedName("companyName")
    private final String companyName;
    @SerializedName("licenseNumber")
    private final String licenseNumber;
    @SerializedName("hqLocation")
    private final String hqLocation;
    @SerializedName("materialType")
    private final String materialType;
    @SerializedName("monthlyCapacity")
    private final double monthlyCapacity;
    @SerializedName("complianceLabel")
    private final String complianceLabel;
    @SerializedName("complianceCertifications")
    private final List<String> complianceCertifications;
    @SerializedName("complianceStatus")
    private final String complianceStatus;
    @SerializedName("leadManagerName")
    private final String leadManagerName;
    @SerializedName("secureEmail")
    private final String secureEmail;

    public CreateVendorRequest(@NonNull String companyName, @NonNull String licenseNumber,
                               @NonNull String hqLocation, @NonNull String materialType,
                               double monthlyCapacity, @NonNull String complianceLabel,
                               @NonNull String complianceStatus, @NonNull String leadManagerName,
                               @NonNull String secureEmail) {
        this.companyName = companyName;
        this.licenseNumber = licenseNumber;
        this.hqLocation = hqLocation;
        this.materialType = materialType;
        this.monthlyCapacity = monthlyCapacity;
        this.complianceLabel = complianceLabel;
        this.complianceCertifications = complianceLabel.isEmpty()
                ? Collections.<String>emptyList() : Collections.singletonList(complianceLabel);
        this.complianceStatus = complianceStatus;
        this.leadManagerName = leadManagerName;
        this.secureEmail = secureEmail;
    }
}
