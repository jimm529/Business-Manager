package com.example.marketplace.data.model.market;

import com.google.gson.annotations.SerializedName;

/** Answer of POST lots/{id}/bookmark: whether the lot is now saved. */
public class BookmarkResponse {

    @SerializedName("bookmarked")
    private Boolean bookmarked;

    /** Null when the server did not say. */
    public Boolean getBookmarked() { return bookmarked; }
}
