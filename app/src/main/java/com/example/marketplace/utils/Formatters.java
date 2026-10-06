package com.example.marketplace.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.marketplace.R;

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

    /** Short money for big numbers, Indian style: 42800000 -> "₹4.28 Cr", 250000 -> "₹2.5 L", 900 -> "₹900". */
    @NonNull
    public static String compactCurrency(@NonNull Context context, double value) {
        double abs = Math.abs(value);
        NumberFormat plain = NumberFormat.getNumberInstance(INDIA);
        plain.setMaximumFractionDigits(2);
        if (abs >= 10_000_000d) {
            return "₹" + context.getString(R.string.amount_crore, plain.format(value / 10_000_000d));
        }
        if (abs >= 100_000d) {
            return "₹" + context.getString(R.string.amount_lakh, plain.format(value / 100_000d));
        }
        String full = currency(value, null, 0);
        return full == null ? "" : full;
    }

    /** "Jane Doe" becomes "JD", "Jane" becomes "J"; a dash when there is no name. */
    @NonNull
    public static String initials(@NonNull Context context, @Nullable String name) {
        if (name == null || name.trim().isEmpty()) {
            return context.getString(R.string.no_value);
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder letters = new StringBuilder();
        for (int i = 0; i < parts.length && letters.length() < 2; i++) {
            letters.append(parts[i].substring(0, parts[i].offsetByCodePoints(0, 1)));
        }
        return letters.toString().toUpperCase(Locale.getDefault());
    }
}
