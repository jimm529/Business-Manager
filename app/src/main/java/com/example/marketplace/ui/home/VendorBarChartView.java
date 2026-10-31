package com.example.marketplace.ui.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.marketplace.R;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

/**
 * The vendor's revenue chart: one soft green bar per month with its name under it.
 * The current month's name is bold. Colours come from the theme and colors.xml.
 */
public class VendorBarChartView extends View {

    /** One month: the text under the bar, its revenue, and whether it is the current month. */
    public static final class Bar {
        final String label;
        final double value;
        final boolean current;

        public Bar(@NonNull String label, double value, boolean current) {
            this.label = label;
            this.value = value;
            this.current = current;
        }
    }

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint currentLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final List<Bar> bars = new ArrayList<>();

    public VendorBarChartView(Context context) {
        this(context, null);
    }

    public VendorBarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        barPaint.setColor(context.getColor(R.color.vendorChartBar));
        float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics());
        labelPaint.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(textSize);
        labelPaint.setTypeface(Typeface.DEFAULT_BOLD);
        currentLabelPaint.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface));
        currentLabelPaint.setTextAlign(Paint.Align.CENTER);
        currentLabelPaint.setTextSize(textSize);
        currentLabelPaint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    /** Shows these months, oldest first. A null or empty list clears the chart. */
    public void setBars(@Nullable List<Bar> months) {
        bars.clear();
        if (months != null) {
            bars.addAll(months);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int count = bars.size();
        if (count == 0) {
            return;
        }
        float slot = (getWidth() - getPaddingLeft() - getPaddingRight()) / (float) count;
        float barWidth = Math.min(slot * 0.62f, dp(40));
        Paint.FontMetrics metrics = labelPaint.getFontMetrics();
        float labelBaseline = getHeight() - getPaddingBottom() - metrics.descent;
        float chartBottom = labelBaseline + metrics.ascent - dp(12);
        float chartHeight = Math.max(0, chartBottom - getPaddingTop());

        double max = 0;
        for (Bar bar : bars) {
            max = Math.max(max, bar.value);
        }
        for (int i = 0; i < count; i++) {
            Bar bar = bars.get(i);
            float centerX = getPaddingLeft() + slot * i + slot / 2f;
            if (max > 0 && bar.value > 0) {
                float height = Math.max(dp(6), (float) (bar.value / max) * chartHeight);
                rect.set(centerX - barWidth / 2f, chartBottom - height, centerX + barWidth / 2f, chartBottom);
                canvas.drawRoundRect(rect, dp(3), dp(3), barPaint);
            }
            canvas.drawText(bar.label, centerX, labelBaseline, bar.current ? currentLabelPaint : labelPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desired = (int) dp(220) + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                resolveSize(desired, heightMeasureSpec));
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
