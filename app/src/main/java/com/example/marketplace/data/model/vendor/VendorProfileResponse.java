package com.example.marketplace.data.model.vendor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of GET vendors/{id}: the supplier's profile plus how this buyer's orders with them went. */
public class VendorProfileResponse {

    @SerializedName("data")
    private Data data;

    @Nullable
    public Data getData() { return data; }

    public static class Data {
        @SerializedName("_id")
        private String id;
        @SerializedName("companyName")
        private String companyName;
        @SerializedName("vendorName")
        private String vendorName;
        @SerializedName("location")
        private String location;
        @SerializedName("hqLocation")
        private String hqLocation;
        @SerializedName("primaryMaterial")
        private String primaryMaterial;
        @SerializedName("materialType")
        private String materialType;
        @SerializedName("status")
        private String status;
        @SerializedName("complianceStatus")
        private String complianceStatus;
        @SerializedName("complianceLabel")
        private String complianceLabel;
        @SerializedName("complianceCertifications")
        private List<String> complianceCertifications;
        @SerializedName("certifications")
        private List<Certification> certifications;
        @SerializedName("createdAt")
        private String createdAt;
        @SerializedName("performanceMetrics")
        private Performance performanceMetrics;

        @Nullable public String getId() { return id; }

        @Nullable
        public String getName() { return firstFilled(companyName, vendorName); }

        @Nullable
        public String getLocation() { return firstFilled(hqLocation, location); }

        @Nullable
        public String getMaterial() { return firstFilled(primaryMaterial, materialType); }

        public boolean isActive() { return "active".equalsIgnoreCase(status); }

        @Nullable public String getStatus() { return status; }

        @Nullable public String getCreatedAt() { return createdAt; }

        @Nullable public String getComplianceLabel() { return complianceLabel; }

        /** The buyer's own rating of this supplier (from their orders), or null when there are none yet. */
        @Nullable
        public Double getRating() { return performanceMetrics == null ? null : performanceMetrics.sellerRating; }

        /**
         * The certification names with their state ("active", "pending", "expired").
         * The detailed ledger wins; otherwise the plain list of names gets the vendor's overall state.
         */
        @NonNull
        public List<CertificationTile> getCertificationTiles() {
            List<CertificationTile> tiles = new ArrayList<>();
            if (certifications != null) {
                for (Certification certification : certifications) {
                    if (certification != null && certification.name != null && !certification.name.trim().isEmpty()) {
                        tiles.add(new CertificationTile(certification.name.trim(), certification.status));
                    }
                }
            }
            if (tiles.isEmpty() && complianceCertifications != null) {
                String state = "compliant".equalsIgnoreCase(complianceStatus) ? "active" : complianceStatus;
                for (String name : complianceCertifications) {
                    if (name != null && !name.trim().isEmpty()) {
                        tiles.add(new CertificationTile(name.trim(), state));
                    }
                }
            }
            return tiles;
        }

        @Nullable
        private static String firstFilled(@Nullable String first, @Nullable String second) {
            if (first != null && !first.trim().isEmpty()) {
                return first.trim();
            }
            return second == null || second.trim().isEmpty() ? null : second.trim();
        }
    }

    private static class Certification {
        @SerializedName("name")
        String name;
        @SerializedName("status")
        String status;
    }

    private static class Performance {
        @SerializedName("sellerRating")
        Double sellerRating;
    }

    /** One certification to show: its name and state. */
    public static final class CertificationTile {
        public final String name;
        @Nullable
        public final String status;

        CertificationTile(String name, @Nullable String status) {
            this.name = name;
            this.status = status;
        }
    }
}
