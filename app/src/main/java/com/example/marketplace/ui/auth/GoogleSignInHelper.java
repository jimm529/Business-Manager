package com.example.marketplace.ui.auth;

import android.content.Context;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.example.marketplace.BuildConfig;
import com.example.marketplace.R;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

/**
 * Asks Google for an ID token ("Sign in with Google"). The phone shows Google's own account picker;
 * this class only hands back the token (or a reason it did not work). The server checks the token.
 */
public final class GoogleSignInHelper {

    /** What happened when asking Google. */
    public interface Callback {
        /** Google gave an ID token: send it to the server. */
        void onToken(@NonNull String idToken);

        /** The person closed the Google window: nothing to show. */
        void onCancelled();

        /** It did not work; this is the text to show. */
        void onFailure(@StringRes int message);
    }

    private GoogleSignInHelper() { }

    /** True when this build knows the Google Web client ID (set as marketplaceGoogleWebClientId). */
    public static boolean isConfigured() {
        return !BuildConfig.GOOGLE_WEB_CLIENT_ID.trim().isEmpty();
    }

    /** Opens Google's account picker. The callback runs on the main thread. */
    public static void start(@NonNull Context activityContext, @NonNull Callback callback) {
        if (!isConfigured()) {
            callback.onFailure(R.string.google_not_configured);
            return;
        }
        CredentialManager manager = CredentialManager.create(activityContext);
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(new GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build())
                .build();
        manager.getCredentialAsync(activityContext, request, new CancellationSignal(),
                ContextCompat.getMainExecutor(activityContext),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        String token = tokenOf(result.getCredential());
                        if (token == null) {
                            callback.onFailure(R.string.google_failed);
                        } else {
                            callback.onToken(token);
                        }
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException error) {
                        if (error instanceof GetCredentialCancellationException) {
                            callback.onCancelled();
                        } else if (error instanceof NoCredentialException) {
                            callback.onFailure(R.string.google_no_account);
                        } else {
                            callback.onFailure(R.string.google_failed);
                        }
                    }
                });
    }

    @Nullable
    private static String tokenOf(@NonNull Credential credential) {
        if (credential instanceof CustomCredential
                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
            try {
                return GoogleIdTokenCredential.createFrom(credential.getData()).getIdToken();
            } catch (RuntimeException unreadable) {
                // The token could not be read out of Google's answer.
                return null;
            }
        }
        return null;
    }
}
