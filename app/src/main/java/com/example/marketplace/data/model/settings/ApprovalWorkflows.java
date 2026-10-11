package com.example.marketplace.data.model.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Card 2: orders above this amount need a manager's sign-off, from the approvers in this order. */
public class ApprovalWorkflows {

    @SerializedName("managerSignoffThreshold")
    private Double managerSignoffThreshold;
    @SerializedName("approvers")
    private List<Approver> approvers;

    @Nullable
    public Double getManagerSignoffThreshold() {
        return managerSignoffThreshold;
    }

    /** Never null. */
    @NonNull
    public List<Approver> getApprovers() {
        return approvers == null ? new ArrayList<>() : approvers;
    }

    /** One entry of the approver list; "user" is null when that account no longer exists. */
    public static class Approver {
        @SerializedName("_id")
        private String id;
        @SerializedName("orderIndex")
        private int orderIndex;
        @SerializedName("user")
        private OrgUser user;

        @Nullable
        public String getId() {
            return id;
        }

        public int getOrderIndex() {
            return orderIndex;
        }

        @Nullable
        public OrgUser getUser() {
            return user;
        }
    }

    /** A person of the operator's organization who can be picked as approver. */
    public static class OrgUser {
        @SerializedName("_id")
        private String id;
        @SerializedName("fullName")
        private String fullName;
        @SerializedName("jobTitle")
        private String jobTitle;
        @SerializedName("email")
        private String email;

        @Nullable
        public String getId() {
            return id;
        }

        @Nullable
        public String getFullName() {
            return fullName;
        }

        @Nullable
        public String getJobTitle() {
            return jobTitle;
        }

        @Nullable
        public String getEmail() {
            return email;
        }
    }
}
