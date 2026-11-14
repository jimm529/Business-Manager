package com.example.marketplace.data.model.messages;

import androidx.annotation.Nullable;

import com.example.marketplace.data.model.bid.NegotiationDetail;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

/** One message in a conversation. A message can carry the deal it is about (a "bid card"). */
public class ChatMessage {

    public static final String TYPE_BID_RECEIVED = "bid_received";
    public static final String TYPE_COUNTER_OFFER = "counter_offer";

    @SerializedName("_id")
    private String id;
    @SerializedName("conversation")
    private String conversation;
    @SerializedName("senderRole")
    private String senderRole;
    @SerializedName("senderName")
    private String senderName;
    @SerializedName("type")
    private String type;
    @SerializedName("content")
    private String content;
    @SerializedName("createdAt")
    private String createdAt;
    @SerializedName("bid")
    private NegotiationDetail bid;

    @Nullable
    public String getId() {
        return id;
    }

    /** The conversation this message belongs to (live messages carry it). */
    @Nullable
    public String getConversationId() {
        return conversation;
    }

    @Nullable
    public String getSenderName() {
        return senderName;
    }

    @Nullable
    public String getMessageText() {
        return content;
    }

    @Nullable
    public String getTimestamp() {
        return createdAt;
    }

    @Nullable
    public NegotiationDetail getBid() {
        return bid;
    }

    /** Sent by the signed-in person's side (buyer or seller)? */
    public boolean isMine(@Nullable String myRole) {
        return senderRole != null && senderRole.equalsIgnoreCase(myRole);
    }

    /** Shown as a tappable "NEW BID RECEIVED" card: a deal sent by the other side. */
    public boolean isBidCard(@Nullable String myRole) {
        if (bid == null || bid.getBidId() == null) {
            return false;
        }
        boolean bidType = TYPE_BID_RECEIVED.equals(type) || TYPE_COUNTER_OFFER.equals(type);
        return bidType && !isMine(myRole);
    }

    public boolean sameContentAs(ChatMessage other) {
        boolean sameBid = bid == null ? other.bid == null : other.bid != null && bid.sameContentAs(other.bid);
        return Objects.equals(id, other.id) && Objects.equals(content, other.content)
                && Objects.equals(createdAt, other.createdAt) && sameBid;
    }
}
