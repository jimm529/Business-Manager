package com.example.marketplace.data.model.settings;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

/** Body of PUT settings/password. The server names the last field "confirmPassword". */
public class ChangePasswordRequest {

    @SerializedName("currentPassword")
    private final String currentPassword;
    @SerializedName("newPassword")
    private final String newPassword;
    @SerializedName("confirmPassword")
    private final String confirmNewPassword;

    public ChangePasswordRequest(@NonNull String currentPassword, @NonNull String newPassword,
                                 @NonNull String confirmNewPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmNewPassword = confirmNewPassword;
    }
}
