package com.example.marketplace.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.marketplace.R;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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

    /** Reads a server time such as "2026-10-07T10:15:30.000Z" (UTC). Null when missing or unreadable. */
    @Nullable
    public static Date parseIso(@Nullable String iso) {
        if (iso == null || iso.length() < 19) {
            return null;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format.parse(iso.substring(0, 19));
        } catch (ParseException unreadable) {
            return null;
        }
    }

    /** Today: the time ("2:15 PM"). Yesterday: the word. Earlier: the day ("Oct 7"). A dash when unknown. */
    @NonNull
    public static String listTimestamp(@NonNull Context context, @Nullable String iso) {
        Date date = parseIso(iso);
        if (date == null) {
            return context.getString(R.string.no_value);
        }
        Calendar then = Calendar.getInstance();
        then.setTime(date);
        Calendar now = Calendar.getInstance();
        if (sameDay(now, then)) {
            return DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
        }
        now.add(Calendar.DAY_OF_YEAR, -1);
        if (sameDay(now, then)) {
            return context.getString(R.string.yesterday);
        }
        return new SimpleDateFormat("MMM d", Locale.getDefault()).format(date);
    }

    /** Chat bubble time: the time today, the day and time before that. A dash when unknown. */
    @NonNull
    public static String messageTimestamp(@NonNull Context context, @Nullable String iso) {
        Date date = parseIso(iso);
        if (date == null) {
            return context.getString(R.string.no_value);
        }
        String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
        Calendar then = Calendar.getInstance();
        then.setTime(date);
        if (sameDay(Calendar.getInstance(), then)) {
            return time;
        }
        return new SimpleDateFormat("MMM d", Locale.getDefault()).format(date) + ", " + time;
    }

    /** "Oct 7, 2026", or a dash when unknown. */
    @NonNull
    public static String shortDate(@NonNull Context context, @Nullable String iso) {
        Date date = parseIso(iso);
        return date == null ? context.getString(R.string.no_value)
                : new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date);
    }

    /** 2400000 -> "2.3 MB". A dash when unknown. */
    @NonNull
    public static String fileSize(@NonNull Context context, @Nullable Long bytes) {
        if (bytes == null || bytes < 0) {
            return context.getString(R.string.no_value);
        }
        NumberFormat one = NumberFormat.getNumberInstance(Locale.getDefault());
        one.setMaximumFractionDigits(1);
        if (bytes >= 1024L * 1024L) {
            return context.getString(R.string.file_size_mb, one.format(bytes / (1024d * 1024d)));
        }
        if (bytes >= 1024L) {
            return context.getString(R.string.file_size_kb, one.format(bytes / 1024d));
        }
        return context.getString(R.string.file_size_bytes, one.format(bytes));
    }

    private static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
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
