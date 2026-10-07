package com.example.marketplace.data.api;

import android.content.ContentResolver;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * Sends a file the person picked (a content:// address) without loading it all into memory first.
 * The file is read while the upload runs, on the network thread.
 */
public class UriRequestBody extends RequestBody {

    private final ContentResolver resolver;
    private final Uri uri;
    @Nullable
    private final MediaType mediaType;

    public UriRequestBody(@NonNull ContentResolver resolver, @NonNull Uri uri, @Nullable String mimeType) {
        this.resolver = resolver;
        this.uri = uri;
        this.mediaType = mimeType == null ? null : MediaType.parse(mimeType);
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return mediaType;
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        try (InputStream input = resolver.openInputStream(uri)) {
            if (input == null) {
                throw new IOException("Cannot open " + uri);
            }
            try (Source source = Okio.source(input)) {
                sink.writeAll(source);
            }
        }
    }
}
