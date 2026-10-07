package com.example.marketplace.data.model.bid;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** The summary the server sends after an offer is accepted. The texts arrive already formatted. */
public class TransactionConfirmation {

    @SerializedName("transactionId")
    private String transactionId;
    @SerializedName("itemDetail")
    private String itemDetail;
    @SerializedName("quantity")
    private String quantity;
    @SerializedName("agreedRate")
    private String agreedRate;
    @SerializedName("totalContractValue")
    private String totalContractValue;
    @SerializedName("status")
    private String status;

    @Nullable
    public String getTransactionId() {
        return transactionId;
    }

    @Nullable
    public String getItemDetail() {
        return itemDetail;
    }

    @Nullable
    public String getQuantity() {
        return quantity;
    }

    @Nullable
    public String getAgreedRate() {
        return agreedRate;
    }

    @Nullable
    public String getTotalContractValue() {
        return totalContractValue;
    }

    @Nullable
    public String getStatus() {
        return status;
    }
}
