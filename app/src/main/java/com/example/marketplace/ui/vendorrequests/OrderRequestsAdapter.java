package com.example.marketplace.ui.vendorrequests;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marketplace.R;
import com.example.marketplace.data.model.vendor.OrderRequestItem;
import com.example.marketplace.databinding.ItemOrderRequestBinding;
import com.example.marketplace.utils.Formatters;
import com.google.android.material.color.MaterialColors;

import java.util.Objects;

/** Draws the order requests. Only requests waiting for the vendor show Accept and Reject. */
public class OrderRequestsAdapter extends ListAdapter<OrderRequestItem, OrderRequestsAdapter.ViewHolder> {

    /** What happens when Accept or Reject is tapped on a request. */
    public interface Listener {
        void onAccept(@NonNull OrderRequestItem item);

        void onReject(@NonNull OrderRequestItem item);
    }

    private static final DiffUtil.ItemCallback<OrderRequestItem> DIFF = new DiffUtil.ItemCallback<OrderRequestItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull OrderRequestItem oldItem, @NonNull OrderRequestItem newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull OrderRequestItem oldItem, @NonNull OrderRequestItem newItem) {
            return oldItem.sameContentAs(newItem);
        }
    };

    private final Listener listener;
    private boolean busy;

    public OrderRequestsAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    /** While an accept or reject runs, the buttons are switched off so it cannot be sent twice. */
    public void setBusy(boolean busy) {
        if (this.busy != busy) {
            this.busy = busy;
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemOrderRequestBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener, busy);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemOrderRequestBinding binding;

        ViewHolder(ItemOrderRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(OrderRequestItem item, Listener listener, boolean busy) {
            Context context = itemView.getContext();
            String noValue = context.getString(R.string.no_value);

            String buyer = item.getBuyerName();
            boolean hasBuyer = buyer != null && !buyer.trim().isEmpty();
            binding.tvBuyer.setText(hasBuyer ? buyer : context.getString(R.string.requests_unknown_buyer));
            binding.tvAvatar.setText(Formatters.initials(context, buyer));
            // Two avatar shades so neighbouring buyers are easy to tell apart.
            boolean dark = hasBuyer && buyer.length() % 2 == 0;
            binding.tvAvatar.setBackgroundResource(dark
                    ? R.drawable.bg_request_avatar_dark : R.drawable.bg_request_avatar_slate);

            binding.tvOrderId.setText(item.getOrderId() == null ? noValue : item.getOrderId());
            binding.tvMaterial.setText(item.getMaterial() == null ? noValue : item.getMaterial());
            binding.tvQuantityRate.setText(quantityAndRate(context, item));
            bindStatus(context, item);

            binding.actions.setVisibility(item.isPending() ? View.VISIBLE : View.GONE);
            binding.btnAccept.setEnabled(!busy);
            binding.btnReject.setEnabled(!busy);
            binding.btnAccept.setOnClickListener(v -> listener.onAccept(item));
            binding.btnReject.setOnClickListener(v -> listener.onReject(item));
        }

        /** "240 MT / ₹1.92 / kg" with the amount bold and the rate in green. */
        private CharSequence quantityAndRate(Context context, OrderRequestItem item) {
            String noValue = context.getString(R.string.no_value);
            String quantity = Formatters.number(item.getQuantityMT());
            String rate = Formatters.currency(item.getOfferedRate(), "INR", 2);
            SpannableStringBuilder text = new SpannableStringBuilder();

            int start = text.length();
            text.append(quantity == null ? noValue : context.getString(R.string.order_quantity, quantity,
                    context.getString(R.string.requests_unit_mt)).trim());
            text.setSpan(new StyleSpan(Typeface.BOLD), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new ForegroundColorSpan(MaterialColors.getColor(binding.getRoot(),
                    com.google.android.material.R.attr.colorOnSurface)), start, text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            text.append(" / ");

            start = text.length();
            text.append(rate == null ? noValue : rate + context.getString(R.string.requests_unit_kg));
            text.setSpan(new StyleSpan(Typeface.BOLD), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new ForegroundColorSpan(MaterialColors.getColor(binding.getRoot(),
                    com.google.android.material.R.attr.colorSecondary)), start, text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return text;
        }

        private void bindStatus(Context context, OrderRequestItem item) {
            int label;
            int background;
            int textColor;
            if (item.isPending()) {
                label = R.string.requests_status_pending;
                background = R.drawable.bg_badge_market_extraction;
                textColor = R.color.marketBadgeExtractionText;
            } else if (item.isCounter()) {
                label = R.string.requests_status_counter;
                background = R.drawable.bg_badge_pending;
                textColor = R.color.badgePendingText;
            } else if (item.isAccepted()) {
                label = R.string.requests_status_accepted;
                background = R.drawable.bg_badge_market_ready;
                textColor = R.color.marketBadgeReadyText;
            } else {
                label = R.string.requests_status_rejected;
                background = R.drawable.bg_badge_urgent;
                textColor = R.color.badgeUrgentText;
            }
            binding.tvBadge.setText(label);
            binding.tvBadge.setBackgroundResource(background);
            binding.tvBadge.setTextColor(context.getColor(textColor));
        }
    }
}
