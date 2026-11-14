package com.example.marketplace.ui.history;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marketplace.R;
import com.example.marketplace.data.model.dashboard.PurchaseHistoryResponse;
import com.example.marketplace.databinding.ItemPurchaseHistoryBinding;
import com.example.marketplace.utils.Formatters;

import java.util.Locale;
import java.util.Objects;

/** Draws one card per past purchase: order id and status, material, seller, date, quantity, purity and value. */
public class PurchaseHistoryAdapter
        extends ListAdapter<PurchaseHistoryResponse.Row, PurchaseHistoryAdapter.ViewHolder> {

    private static final int STATUS_ICON_DP = 14;

    private static final DiffUtil.ItemCallback<PurchaseHistoryResponse.Row> DIFF =
            new DiffUtil.ItemCallback<PurchaseHistoryResponse.Row>() {
                @Override
                public boolean areItemsTheSame(@NonNull PurchaseHistoryResponse.Row oldItem,
                                               @NonNull PurchaseHistoryResponse.Row newItem) {
                    return Objects.equals(oldItem.getOrderId(), newItem.getOrderId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull PurchaseHistoryResponse.Row oldItem,
                                                  @NonNull PurchaseHistoryResponse.Row newItem) {
                    return oldItem.sameContentAs(newItem);
                }
            };

    public PurchaseHistoryAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemPurchaseHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPurchaseHistoryBinding binding;

        ViewHolder(ItemPurchaseHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PurchaseHistoryResponse.Row row) {
            Context context = itemView.getContext();
            String noValue = context.getString(R.string.no_value);

            binding.tvOrderId.setText(orElse(row.getOrderId(), noValue));
            binding.tvMaterial.setText(orElse(row.getMaterial(), noValue));
            String vendor = row.getVendorName();
            binding.tvVendor.setVisibility(vendor == null || vendor.trim().isEmpty() ? View.GONE : View.VISIBLE);
            binding.tvVendor.setText(vendor);

            // The backend sends the day as YYYY-MM-DD; shortDate reads a full date-time, so pad it.
            String day = row.getDate();
            binding.tvDate.setText(day == null || day.length() < 10 ? noValue
                    : Formatters.shortDate(context, day.substring(0, 10) + "T00:00:00"));

            String quantity = Formatters.number(row.getQuantityMT());
            binding.tvQuantity.setText(quantity == null ? noValue
                    : context.getString(R.string.history_quantity_mt, quantity));
            String purity = Formatters.number(row.getPurity());
            binding.tvPurity.setText(purity == null ? noValue : context.getString(R.string.vendor_percent, purity));
            binding.tvValue.setText(orElse(Formatters.currency(row.getValueINR(), row.getCurrency(), 0), noValue));

            bindStatus(context, row);
        }

        /** Green for completed, blue for on the way, red for cancelled, amber for everything else. */
        private void bindStatus(Context context, PurchaseHistoryResponse.Row row) {
            int background;
            int textColor;
            int icon = 0;
            if (row.isCompleted()) {
                background = R.drawable.bg_badge_market_ready;
                textColor = R.color.marketBadgeReadyText;
                icon = R.drawable.ic_dot;
            } else if (row.isOnTheWay()) {
                background = R.drawable.bg_badge_pending;
                textColor = R.color.badgePendingText;
                icon = R.drawable.ic_local_shipping;
            } else if (row.isCancelled()) {
                background = R.drawable.bg_badge_urgent;
                textColor = R.color.badgeUrgentText;
                icon = R.drawable.ic_cancel;
            } else {
                background = R.drawable.bg_badge_market_extraction;
                textColor = R.color.marketBadgeExtractionText;
            }
            String status = row.getStatus();
            binding.tvStatus.setText(status == null || status.trim().isEmpty()
                    ? context.getString(R.string.no_value) : status.trim().toUpperCase(Locale.getDefault()));
            binding.tvStatus.setBackgroundResource(background);
            int color = context.getColor(textColor);
            binding.tvStatus.setTextColor(color);
            binding.tvStatus.setCompoundDrawablesRelative(icon == 0 ? null : tinted(context, icon, color), null, null, null);
        }

        private static Drawable tinted(Context context, @DrawableRes int icon, int color) {
            Drawable drawable = ContextCompat.getDrawable(context, icon).mutate();
            int size = Math.round(STATUS_ICON_DP * context.getResources().getDisplayMetrics().density);
            drawable.setBounds(0, 0, size, size);
            drawable.setTint(color);
            return drawable;
        }

        private static String orElse(String value, String fallback) {
            return (value == null || value.trim().isEmpty()) ? fallback : value;
        }
    }
}
