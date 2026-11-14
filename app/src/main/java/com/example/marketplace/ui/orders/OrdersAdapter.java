package com.example.marketplace.ui.orders;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marketplace.R;
import com.example.marketplace.data.model.order.Order;
import com.example.marketplace.databinding.ItemOrderBinding;
import com.example.marketplace.utils.Formatters;

import java.util.Locale;
import java.util.Objects;

/**
 * Draws one card per order: number and status, commodity, placed / expected dates, quantity and value.
 * Any missing value shows as a dash.
 */
public class OrdersAdapter extends ListAdapter<Order, OrdersAdapter.ViewHolder> {

    private static final int STATUS_ICON_DP = 14;

    private static final DiffUtil.ItemCallback<Order> DIFF = new DiffUtil.ItemCallback<Order>() {
        @Override
        public boolean areItemsTheSame(@NonNull Order oldItem, @NonNull Order newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Order oldItem, @NonNull Order newItem) {
            return Objects.equals(oldItem.getOrderId(), newItem.getOrderId())
                    && Objects.equals(oldItem.getStatus(), newItem.getStatus())
                    && Objects.equals(oldItem.getValue(), newItem.getValue())
                    && Objects.equals(oldItem.getQuantity(), newItem.getQuantity())
                    && Objects.equals(oldItem.getCommodity(), newItem.getCommodity())
                    && Objects.equals(oldItem.getExpectedDate(), newItem.getExpectedDate())
                    && Objects.equals(oldItem.getCreatedAt(), newItem.getCreatedAt());
        }
    };

    public OrdersAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemOrderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemOrderBinding binding;

        ViewHolder(ItemOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Order order) {
            Context context = itemView.getContext();
            String noValue = context.getString(R.string.no_value);

            binding.tvOrderId.setText(orElse(order.getOrderId(), noValue));
            binding.tvCommodity.setText(orElse(order.getCommodity(), noValue));

            String quantity = Formatters.number(order.getQuantity());
            String unit = order.getUnit() == null ? "" : order.getUnit();
            binding.tvQuantity.setText(quantity == null ? noValue
                    : context.getString(R.string.order_quantity, quantity, unit).trim());
            binding.tvPlaced.setText(Formatters.shortDate(context, order.getCreatedAt()));
            binding.tvExpected.setText(Formatters.shortDate(context, order.getExpectedDate()));
            binding.tvValue.setText(orElse(Formatters.currency(order.getValue(), order.getCurrency(), 0), noValue));

            bindStatus(context, order.getStatus());
        }

        /** Green for delivered, blue for on the way, red for cancelled, amber for everything else. */
        private void bindStatus(Context context, String status) {
            int background;
            int textColor;
            int icon = 0;
            if (OrdersViewModel.Filter.DELIVERED.matches(status)) {
                background = R.drawable.bg_badge_market_ready;
                textColor = R.color.marketBadgeReadyText;
                icon = R.drawable.ic_check_circle;
            } else if (OrdersViewModel.Filter.IN_PROGRESS.matches(status)) {
                background = R.drawable.bg_badge_pending;
                textColor = R.color.badgePendingText;
                icon = R.drawable.ic_local_shipping;
            } else if (OrdersViewModel.Filter.CANCELLED.matches(status)) {
                background = R.drawable.bg_badge_urgent;
                textColor = R.color.badgeUrgentText;
                icon = R.drawable.ic_cancel;
            } else {
                background = R.drawable.bg_badge_market_extraction;
                textColor = R.color.marketBadgeExtractionText;
            }
            binding.tvStatus.setText(status == null || status.trim().isEmpty()
                    ? context.getString(R.string.no_value) : status.trim().toUpperCase(Locale.getDefault()));
            binding.tvStatus.setBackgroundResource(background);
            int color = context.getColor(textColor);
            binding.tvStatus.setTextColor(color);
            binding.tvStatus.setCompoundDrawablesRelative(
                    icon == 0 ? null : tinted(context, icon, color), null, null, null);
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
