package com.example.marketplace.utils;

import androidx.annotation.Nullable;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/** Turns numbers from the backend into text for the screen. Money uses the Indian format by default. */
public final class Formatters {

    private static final Locale INDIA = new Locale("en", "IN");
    private static final String DEFAULT_CURRENCY = "INR";

    private Formatters() { }

    /** e.g. 1250000 -> "₹12,50,000". Returns null when there is no value. */
    @Nullable
    public static String currency(@Nullable Double value, @Nullable String currencyCode, int maxFractionDigits) {
        if (value == null) {
            return null;
        }
        NumberFormat format = NumberFormat.getCurrencyInstance(INDIA);
        try {
            format.setCurrency(Currency.getInstance(
                    currencyCode == null || currencyCode.trim().isEmpty() ? DEFAULT_CURRENCY : currencyCode.trim()));
        } catch (IllegalArgumentException unknownCode) {
            format.setCurrency(Currency.getInstance(DEFAULT_CURRENCY));
        }
        format.setMaximumFractionDigits(maxFractionDigits);
        format.setMinimumFractionDigits(0);
        return format.format(value);
    }

    /** e.g. 12.5 -> "12.5", 12.0 -> "12". Returns null when there is no value. */
    @Nullable
    public static String number(@Nullable Double value) {
        if (value == null) {
            return null;
        }
        NumberFormat format = NumberFormat.getNumberInstance(INDIA);
        format.setMaximumFractionDigits(2);
        return format.format(value);
    }
}
