package com.example.marketplace.data.model.messages;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.marketplace.data.model.bid.NegotiationDocument;
import com.example.marketplace.utils.Roles;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** One conversation thread between a buyer and a seller. */
public class Conversation {

    @SerializedName("_id")
    private String id;
    @SerializedName("vendor")
    private Person vendor;
    @SerializedName("operator")
    private Person operator;
    @SerializedName("lotTitle")
    private String lotTitle;
    @SerializedName("lastMessageText")
    private String lastMessageText;
    @SerializedName("lastMessageAt")
    private String lastMessageAt;
    @SerializedName("unreadCountOperator")
    private Integer unreadCountOperator;
    @SerializedName("unreadCountVendor")
    private Integer unreadCountVendor;
    @SerializedName("lastMessage")
    private LastMessage lastMessage;
    @SerializedName("documents")
    private List<NegotiationDocument> documents;

    /** Seller company or buyer user: only the fields the screens need. */
    public static class Person {
        @SerializedName("companyName")
        private String companyName;
        @SerializedName("vendorName")
        private String vendorName;
        @SerializedName("leadManagerName")
        private String leadManagerName;
        @SerializedName("location")
        private String location;
        @SerializedName("fullName")
        private String fullName;
        @SerializedName("jobTitle")
        private String jobTitle;
    }

    /** The newest message; only the deal it mentions is needed for the batch line. */
    public static class LastMessage {
        @SerializedName("bid")
        private BidSummary bid;
    }

    public static class BidSummary {
        @SerializedName("batchTitle")
        private String batchTitle;
    }

    @Nullable
    public String getId() {
        return id;
    }

    /** The other side of the thread: the seller for a buyer, the buyer for a seller. */
    @Nullable
    public String getParticipantName(@Nullable String myRole) {
        if (Roles.VENDOR.equalsIgnoreCase(myRole)) {
            return operator == null ? null : firstNonEmpty(operator.fullName);
        }
        return vendor == null ? null : firstNonEmpty(vendor.companyName, vendor.vendorName);
    }

    /** The contact's role or position under the name (e.g. the seller's lead manager), when known. */
    @Nullable
    public String getParticipantSubtitle(@Nullable String myRole) {
        if (Roles.VENDOR.equalsIgnoreCase(myRole)) {
            return operator == null ? null : firstNonEmpty(operator.jobTitle);
        }
        return vendor == null ? null : firstNonEmpty(vendor.leadManagerName, vendor.location);
    }

    /** The batch this thread is about, e.g. "Batch #8829 - 40 MT Lead". */
    @Nullable
    public String getBatchTitle() {
        String direct = firstNonEmpty(lotTitle);
        if (direct != null) {
            return direct;
        }
        return lastMessage == null || lastMessage.bid == null ? null : firstNonEmpty(lastMessage.bid.batchTitle);
    }

    @Nullable
    public String getLastMessage() {
        return firstNonEmpty(lastMessageText);
    }

    @Nullable
    public String getTimestamp() {
        return lastMessageAt;
    }

    public int getUnreadCount(@Nullable String myRole) {
        Integer count = Roles.VENDOR.equalsIgnoreCase(myRole) ? unreadCountVendor : unreadCountOperator;
        return count == null ? 0 : Math.max(0, count);
    }

    @NonNull
    public List<NegotiationDocument> getDocuments() {
        return documents == null ? new ArrayList<>() : documents;
    }

    public boolean sameContentAs(Conversation other, @Nullable String myRole) {
        return Objects.equals(id, other.id)
                && Objects.equals(getParticipantName(myRole), other.getParticipantName(myRole))
                && Objects.equals(getBatchTitle(), other.getBatchTitle())
                && Objects.equals(getLastMessage(), other.getLastMessage())
                && Objects.equals(lastMessageAt, other.lastMessageAt)
                && getUnreadCount(myRole) == other.getUnreadCount(myRole);
    }

    @Nullable
    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
}
