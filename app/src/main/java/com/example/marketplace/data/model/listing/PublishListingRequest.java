package com.example.marketplace.data.model.listing;

import com.google.gson.annotations.SerializedName;

/** Everything the server needs to publish a new lot. */
public class PublishListingRequest {

    @SerializedName("materialType")
    private final String materialType;
    @SerializedName("purityGrade")
    private final double purityGrade;
    @SerializedName("quantityMT")
    private final double quantityMT;
    @SerializedName("packagingType")
    private final String packagingType;
    @SerializedName("pickupLocation")
    private final String pickupLocation;
    @SerializedName("region")
    private final String region;
    /** yyyy-MM-dd */
    @SerializedName("availabilityDate")
    private final String availabilityDate;
    @SerializedName("shippingTerms")
    private final String shippingTerms;
    @SerializedName("askingPricePerKg")
    private final double askingPricePerKg;
    @SerializedName("certificationAccepted")
    private final boolean certificationAccepted;

    public PublishListingRequest(String materialType, double purityGrade, double quantityMT, String packagingType,
                                 String pickupLocation, String region, String availabilityDate,
                                 String shippingTerms, double askingPricePerKg, boolean certificationAccepted) {
        this.materialType = materialType;
        this.purityGrade = purityGrade;
        this.quantityMT = quantityMT;
        this.packagingType = packagingType;
        this.pickupLocation = pickupLocation;
        this.region = region;
        this.availabilityDate = availabilityDate;
        this.shippingTerms = shippingTerms;
        this.askingPricePerKg = askingPricePerKg;
        this.certificationAccepted = certificationAccepted;
    }
}
