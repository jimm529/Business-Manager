package com.example.marketplace.data.model.auth;

import com.google.gson.annotations.SerializedName;

/** The signed-in person, exactly as the backend sends it. Any field can be null. */
public class User {
    @SerializedName("id")
    private String id;
    @SerializedName("fullName")
    private String fullName;
    @SerializedName("jobTitle")
    private String jobTitle;
    @SerializedName("email")
    private String email;
    @SerializedName("role")
    private String role;
    @SerializedName("vendor")
    private String vendor;
    @SerializedName("twoFactorEnabled")
    private Boolean twoFactorEnabled;

    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getJobTitle() { return jobTitle; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getVendor() { return vendor; }
    public boolean isTwoFactorEnabled() { return Boolean.TRUE.equals(twoFactorEnabled); }
}
