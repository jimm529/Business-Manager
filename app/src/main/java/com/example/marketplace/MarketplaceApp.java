package com.example.marketplace;

import android.app.Application;

import com.example.marketplace.utils.SessionManager;

/** Runs once when the app process starts: prepares secure session storage. */
public class MarketplaceApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SessionManager.init(this);
    }
}
