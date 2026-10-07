package com.example.marketplace.data.model.bid;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of GET bids: the signed-in buyer's deals. */
public class BidsResponse {

    @SerializedName("data")
    private List<NegotiationDetail> data;

    @NonNull
    public List<NegotiationDetail> getBids() {
        return data == null ? new ArrayList<>() : data;
    }
}
