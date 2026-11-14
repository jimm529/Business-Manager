package com.example.marketplace.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.example.marketplace.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Draws the page buttons of a long list: "‹ 1 2 3 … 41 ›". The current page is green, the arrows dim at
 * the ends, and tapping a number or an arrow reports the page to open.
 */
public final class PageButtons {

    /** With this many pages or fewer, every page number is shown. */
    private static final int SHOW_ALL_PAGES_UP_TO = 5;

    private PageButtons() { }

    /** Fills the row with buttons for this page position. Anything already in the row is removed. */
    public static void build(@NonNull LinearLayout row, int current, int total, @NonNull IntConsumer onPage) {
        Context context = row.getContext();
        row.removeAllViews();
        row.addView(arrow(context, R.drawable.ic_arrow_back, R.string.cd_previous_page,
                current > 1 ? current - 1 : -1, onPage));

        for (int page : pagesToShow(current, total)) {
            if (page == 0) {
                TextView dots = new TextView(context);
                dots.setText(R.string.sales_ellipsis);
                dots.setTextColor(MaterialColors.getColor(row, com.google.android.material.R.attr.colorOnSurfaceVariant));
                dots.setPadding(dp(context, 10), 0, dp(context, 10), 0);
                dots.setGravity(Gravity.CENTER);
                row.addView(dots, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(context, 44)));
            } else {
                row.addView(number(context, row, page, page == current, onPage));
            }
        }

        row.addView(arrow(context, R.drawable.ic_chevron_right, R.string.cd_next_page,
                current < total ? current + 1 : -1, onPage));
    }

    /** Page numbers to show; 0 stands for "...". */
    static List<Integer> pagesToShow(int current, int total) {
        List<Integer> pages = new ArrayList<>();
        if (total <= SHOW_ALL_PAGES_UP_TO) {
            for (int i = 1; i <= total; i++) {
                pages.add(i);
            }
        } else if (current <= 3) {
            pages.add(1);
            pages.add(2);
            pages.add(3);
            pages.add(0);
            pages.add(total);
        } else if (current >= total - 2) {
            pages.add(1);
            pages.add(0);
            pages.add(total - 2);
            pages.add(total - 1);
            pages.add(total);
        } else {
            pages.add(1);
            pages.add(0);
            pages.add(current - 1);
            pages.add(current);
            pages.add(current + 1);
            pages.add(0);
            pages.add(total);
        }
        return pages;
    }

    private static MaterialButton number(Context context, LinearLayout row, int page, boolean selected,
                                         IntConsumer onPage) {
        MaterialButton button = new MaterialButton(context, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(String.valueOf(page));
        button.setContentDescription(context.getString(R.string.cd_page, page));
        style(context, button);
        if (selected) {
            button.setBackgroundTintList(ColorStateList.valueOf(MaterialColors.getColor(row,
                    com.google.android.material.R.attr.colorSecondary)));
            button.setTextColor(MaterialColors.getColor(row, com.google.android.material.R.attr.colorOnSecondary));
            button.setStrokeWidth(0);
        } else {
            button.setTextColor(MaterialColors.getColor(row, com.google.android.material.R.attr.colorOnSurface));
            button.setOnClickListener(v -> onPage.accept(page));
        }
        return button;
    }

    /** Previous / next. A target of -1 means there is nothing to go to, so the button is dimmed. */
    private static MaterialButton arrow(Context context, @DrawableRes int icon, int description, int targetPage,
                                        IntConsumer onPage) {
        MaterialButton button = new MaterialButton(context, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setIconResource(icon);
        button.setIconPadding(0);
        button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        button.setContentDescription(context.getString(description));
        style(context, button);
        button.setPadding(dp(context, 11), 0, dp(context, 11), 0);
        button.setEnabled(targetPage > 0);
        button.setOnClickListener(v -> onPage.accept(targetPage));
        return button;
    }

    private static void style(Context context, MaterialButton button) {
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setMinWidth(dp(context, 46));
        button.setMinimumWidth(dp(context, 46));
        button.setPadding(0, 0, 0, 0);
        button.setCornerRadius(dp(context, 8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(context, 44));
        params.setMarginStart(dp(context, 3));
        params.setMarginEnd(dp(context, 3));
        button.setLayoutParams(params);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
