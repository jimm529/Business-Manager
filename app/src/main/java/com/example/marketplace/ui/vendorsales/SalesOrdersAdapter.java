package com.example.marketplace.ui.vendorsales;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.format.DateUtils;
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
import com.example.marketplace.data.model.vendor.SalesOrder;
import com.example.marketplace.databinding.ItemSalesOrderBinding;
import com.example.marketplace.utils.Formatters;
import com.google.android.material.color.MaterialColors;

import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/** Draws the sales cards: order id and status, material, buyer, value, date, metrics, and document buttons. */
public class SalesOrdersAdapter extends ListAdapter<SalesOrder, SalesOrdersAdapter.ViewHolder> {

    /** What happens when a document button is tapped. */
    public interface Listener {
        void onViewDocument(@NonNull SalesOrder order);

        void onDownloadDocument(@NonNull SalesOrder order);
    }

    private static final int STATUS_ICON_DP = 16;

    private static final DiffUtil.ItemCallback<SalesOrder> DIFF = new DiffUtil.ItemCallback<SalesOrder>() {
        @Override
        public boolean areItemsTheSame(@NonNull SalesOrder oldItem, @NonNull SalesOrder newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull SalesOrder oldItem, @NonNull SalesOrder newItem) {
            return oldItem.sameContentAs(newItem);
        }
    };

    private final Listener listener;
    private boolean busy;

    public SalesOrdersAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    /** While a document address is being fetched, its buttons are switched off. */
    public void setBusy(boolean busy) {
        if (this.busy != busy) {
            this.busy = busy;
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemSalesOrderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener, busy);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSalesOrderBinding binding;

        ViewHolder(ItemSalesOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(SalesOrder order, Listener listener, boolean busy) {
            Context context = itemView.getContext();
            String noValue = context.getString(R.string.no_value);

            binding.tvOrderId.setText(order.getOrderId() == null ? noValue : order.getOrderId());
            binding.tvMaterial.setText(order.getMaterial() == null ? noValue : order.getMaterial());
            // A cancelled sale is greyed out.
            binding.tvMaterial.setTextColor(MaterialColors.getColor(binding.getRoot(), order.isCancelled()
                    ? com.google.android.material.R.attr.colorOnSurfaceVariant
                    : com.google.android.material.R.attr.colorOnSurface));

            binding.tvBuyer.setText(order.getBuyer() == null || order.getBuyer().trim().isEmpty()
                    ? noValue : order.getBuyer());
            String value = Formatters.currency(order.getValue(), order.getCurrency(), 2);
            binding.tvValue.setText(value == null ? noValue : value);
            binding.tvDate.setText(Formatters.shortDate(context, order.getDate()));

            String quantity = Formatters.number(order.getQuantity());
            String purity = Formatters.number(order.getPurity());
            if (quantity == null) {
                binding.tvMetrics.setText(noValue);
            } else if (purity == null) {
                binding.tvMetrics.setText(context.getString(R.string.sales_metrics_quantity_only, quantity));
            } else {
                binding.tvMetrics.setText(context.getString(R.string.sales_metrics_value, quantity, purity));
            }

            bindStatus(context, order);

            // "Placed 2 hours ago", worked out from the order's date. Cancelled sales show no line.
            Date placed = Formatters.parseIso(order.getDate());
            if (placed == null || order.isCancelled()) {
                binding.tvFooter.setText("");
            } else {
                CharSequence ago = DateUtils.getRelativeTimeSpanString(placed.getTime(), System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS);
                binding.tvFooter.setText(context.getString(R.string.sales_placed, ago));
            }

            int docs = order.hasDocument() ? View.VISIBLE : View.GONE;
            binding.btnView.setVisibility(docs);
            binding.btnDownload.setVisibility(docs);
            binding.btnView.setEnabled(!busy);
            binding.btnDownload.setEnabled(!busy);
            binding.btnView.setOnClickListener(v -> listener.onViewDocument(order));
            binding.btnDownload.setOnClickListener(v -> listener.onDownloadDocument(order));
        }

        private void bindStatus(Context context, SalesOrder order) {
            int background;
            int textColor;
            int icon = 0;
            CharSequence label;
            if (order.isCompleted()) {
                background = R.drawable.bg_badge_market_ready;
                textColor = R.color.marketBadgeReadyText;
                icon = R.drawable.ic_dot;
                label = context.getString(R.string.sales_status_completed);
            } else if (order.isInTransit()) {
                background = R.drawable.bg_badge_pending;
                textColor = R.color.badgePendingText;
                icon = R.drawable.ic_local_shipping;
                label = context.getString(R.string.sales_status_in_transit);
            } else if (order.isCancelled()) {
                background = R.drawable.bg_badge_urgent;
                textColor = R.color.badgeUrgentText;
                icon = R.drawable.ic_cancel;
                label = context.getString(R.string.sales_status_cancelled);
            } else {
                // Dispatched, Awaiting Docs and any other state are shown as the server names them.
                background = R.drawable.bg_badge_market_extraction;
                textColor = R.color.marketBadgeExtractionText;
                label = order.getStatus() == null ? context.getString(R.string.no_value)
                        : order.getStatus().trim().toUpperCase(Locale.getDefault());
            }
            binding.tvStatus.setText(label);
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
    }
}
