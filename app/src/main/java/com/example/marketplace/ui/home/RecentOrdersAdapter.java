package com.example.marketplace.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marketplace.R;
import com.example.marketplace.data.model.order.Order;
import com.example.marketplace.databinding.ItemRecentOrderBinding;
import com.example.marketplace.utils.Formatters;

import java.util.Objects;

/** Draws the rows of the vendor's "Recent orders" list. */
public class RecentOrdersAdapter extends ListAdapter<Order, RecentOrdersAdapter.ViewHolder> {

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
                    && Objects.equals(oldItem.getBuyerDisplayName(), newItem.getBuyerDisplayName());
        }
    };

    public RecentOrdersAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemRecentOrderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemRecentOrderBinding binding;

        ViewHolder(ItemRecentOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Order order) {
            Context context = itemView.getContext();
            String noValue = context.getString(R.string.no_value);

            binding.tvOrderId.setText(orElse(order.getOrderId(), noValue));
            binding.tvStatus.setText(orElse(order.getStatus(), noValue));
            binding.tvCommodity.setText(orElse(order.getCommodity(), noValue));

            String buyer = order.getBuyerDisplayName();
            binding.tvBuyer.setText(buyer == null ? context.getString(R.string.unknown_buyer) : buyer);

            String quantity = Formatters.number(order.getQuantity());
            String unit = order.getUnit() == null ? "" : order.getUnit();
            String value = Formatters.currency(order.getValue(), order.getCurrency(), 0);
            // Quantity and value share the commodity line / value label.
            if (quantity != null) {
                binding.tvCommodity.setText(context.getString(R.string.order_quantity, quantity, unit).trim()
                        + " · " + orElse(order.getCommodity(), noValue));
            }
            binding.tvValue.setText(orElse(value, noValue));
        }

        private static String orElse(String value, String fallback) {
            return (value == null || value.trim().isEmpty()) ? fallback : value;
        }
    }
}
