package com.example.marketplace.ui.history;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marketplace.R;
import com.example.marketplace.data.model.dashboard.PurchaseHistoryResponse;
import com.example.marketplace.databinding.ItemPurchaseHistoryBinding;
import com.example.marketplace.utils.Formatters;

import java.util.Objects;

/** Draws one card per past purchase. Rows have no id, so two rows are "the same" when every field matches. */
public class PurchaseHistoryAdapter
        extends ListAdapter<PurchaseHistoryResponse.Row, PurchaseHistoryAdapter.ViewHolder> {

    private static final DiffUtil.ItemCallback<PurchaseHistoryResponse.Row> DIFF =
            new DiffUtil.ItemCallback<PurchaseHistoryResponse.Row>() {
                @Override
                public boolean areItemsTheSame(@NonNull PurchaseHistoryResponse.Row oldItem,
                                               @NonNull PurchaseHistoryResponse.Row newItem) {
                    return areContentsTheSame(oldItem, newItem);
                }

                @Override
                public boolean areContentsTheSame(@NonNull PurchaseHistoryResponse.Row oldItem,
                                                  @NonNull PurchaseHistoryResponse.Row newItem) {
                    return Objects.equals(oldItem.getDate(), newItem.getDate())
                            && Objects.equals(oldItem.getStatus(), newItem.getStatus())
                            && Objects.equals(oldItem.getQuantityMT(), newItem.getQuantityMT())
                            && Objects.equals(oldItem.getValueINR(), newItem.getValueINR());
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

            // The backend sends the day as YYYY-MM-DD; shortDate reads a full date-time, so pad it.
            String day = row.getDate();
            binding.tvDate.setText(day == null || day.length() < 10 ? noValue
                    : Formatters.shortDate(context, day.substring(0, 10) + "T00:00:00"));
            binding.tvStatus.setText(orElse(row.getStatus(), noValue));

            String quantity = Formatters.number(row.getQuantityMT());
            binding.tvQuantity.setText(quantity == null ? noValue
                    : context.getString(R.string.history_quantity_mt, quantity));
            binding.tvValue.setText(orElse(Formatters.currency(row.getValueINR(), null, 0), noValue));
        }

        private static String orElse(String value, String fallback) {
            return (value == null || value.trim().isEmpty()) ? fallback : value;
        }
    }
}
