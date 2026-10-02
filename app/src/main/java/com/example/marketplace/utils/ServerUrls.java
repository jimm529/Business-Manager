package com.example.marketplace.utils;

import androidx.annotation.Nullable;

import okhttp3.HttpUrl;

/**
 * Cleans up server addresses typed by a person, so all of these mean the same thing:
 * "192.168.1.5:5000", "http://192.168.1.5:5000", "http://192.168.1.5:5000/" and
 * "http://192.168.1.5:5000/api".
 */
public final class ServerUrls {

    private static final String API_SEGMENT = "/api";

    private ServerUrls() { }

    /**
     * Returns the server root with a trailing slash, e.g. "http://192.168.1.5:5000/",
     * or null when the text is not a usable http/https address. A missing "http://" is added,
     * and a trailing "/api" is removed (the app adds it itself).
     */
    @Nullable
    public static String normalize(@Nullable String input) {
        if (input == null) {
            return null;
        }
        String text = input.trim();
        if (text.isEmpty()) {
            return null;
        }
        if (!text.contains("://")) {
            text = "http://" + text;
        }
        HttpUrl url = HttpUrl.parse(text);
        if (url == null) {
            return null;
        }

        String path = url.encodedPath();
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.endsWith(API_SEGMENT)) {
            path = path.substring(0, path.length() - API_SEGMENT.length());
        }

        // Scheme, host and port with a "/" path, without any query or fragment.
        String root = url.newBuilder().encodedPath("/").query(null).fragment(null).build().toString();
        return path.isEmpty() ? root : root + path.substring(1) + "/";
    }

    /** The address Retrofit needs: the server root plus "api/", e.g. "http://192.168.1.5:5000/api/". */
    @Nullable
    public static String toApiBaseUrl(@Nullable String serverUrl) {
        String root = normalize(serverUrl);
        return root == null ? null : root + "api/";
    }
}
