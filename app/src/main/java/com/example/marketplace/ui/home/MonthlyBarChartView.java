package com.example.marketplace.ui.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.marketplace.data.model.dashboard.MonthlyPurchaseData;
import com.google.android.material.color.MaterialColors;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A small bar chart: one bar per month with the short month name (JAN, FEB, ...) under it.
 * Colours come from the theme. Without data it draws only the month names.
 */
public class MonthlyBarChartView extends View {

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bar = new RectF();
    private final List<MonthlyPurchaseData> data = new ArrayList<>();

    private final float barRadius;
    private final float labelGap;

    public MonthlyBarChartView(Context context) {
        this(context, null);
    }

    public MonthlyBarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        barPaint.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary));
        labelPaint.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
        barRadius = dp(4);
        labelGap = dp(12);
    }

    /** Shows these months, oldest first. A null or empty list clears the chart. */
    public void setData(@Nullable List<MonthlyPurchaseData> months) {
        data.clear();
        if (months != null) {
            data.addAll(months);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int count = data.size();
        if (count == 0) {
            return;
        }
        float slot = (getWidth() - getPaddingLeft() - getPaddingRight()) / (float) count;
        float barWidth = Math.min(slot * 0.5f, dp(28));
        Paint.FontMetrics metrics = labelPaint.getFontMetrics();
        float labelBaseline = getHeight() - getPaddingBottom() - metrics.descent;
        float chartBottom = labelBaseline + metrics.ascent - labelGap;
        float chartTop = getPaddingTop();
        float chartHeight = Math.max(0, chartBottom - chartTop);

        double max = 0;
        for (MonthlyPurchaseData month : data) {
            max = Math.max(max, month.getAmount());
        }

        String[] names = DateFormatSymbols.getInstance(Locale.getDefault()).getShortMonths();
        for (int i = 0; i < count; i++) {
            MonthlyPurchaseData month = data.get(i);
            float centerX = getPaddingLeft() + slot * i + slot / 2f;

            if (max > 0 && month.getAmount() > 0) {
                // Every month with a purchase gets at least a visible stub.
                float height = Math.max(dp(4), (float) (month.getAmount() / max) * chartHeight);
                bar.set(centerX - barWidth / 2f, chartBottom - height, centerX + barWidth / 2f, chartBottom);
                canvas.drawRoundRect(bar, barRadius, barRadius, barPaint);
            }

            String name = month.getMonth() >= 0 && month.getMonth() < names.length
                    ? names[month.getMonth()].toUpperCase(Locale.getDefault()) : "";
            canvas.drawText(name, centerX, labelBaseline, labelPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desired = (int) dp(160) + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(
                resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                resolveSize(desired, heightMeasureSpec));
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
