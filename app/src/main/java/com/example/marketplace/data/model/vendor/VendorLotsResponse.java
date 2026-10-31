package com.example.marketplace.data.model.vendor;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of GET lots: every listing on the marketplace. */
public class VendorLotsResponse {

    @SerializedName("lots")
    private List<VendorLot> lots;

    /** The lots that belong to this vendor. */
    @NonNull
    public List<VendorLot> getLotsOf(@NonNull String vendorId) {
        List<VendorLot> result = new ArrayList<>();
        if (lots != null) {
            for (VendorLot lot : lots) {
                if (lot != null && vendorId.equals(lot.getVendorId())) {
                    result.add(lot);
                }
            }
        }
        return result;
    }
}
