package com.example.marketplace.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marketplace.R;
import com.example.marketplace.data.model.dashboard.ActiveDeal;
import com.example.marketplace.databinding.ItemActiveDealBinding;
import com.example.marketplace.utils.Formatters;

import java.util.Objects;

/** Draws the "Active Deals" cards: green YOUR TURN with Counter / Decline, or lavender PENDING VENDOR. */
public class ActiveDealsAdapter extends ListAdapter<ActiveDeal, ActiveDealsAdapter.ViewHolder> {

    /** What happens when a button on a card is tapped. */
    public interface Listener {
        void onCounter(@NonNull ActiveDeal deal);

        void onDecline(@NonNull ActiveDeal deal);
    }

    private static final DiffUtil.ItemCallback<ActiveDeal> DIFF = new DiffUtil.ItemCallback<ActiveDeal>() {
        @Override
        public boolean areItemsTheSame(@NonNull ActiveDeal oldItem, @NonNull ActiveDeal newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ActiveDeal oldItem, @NonNull ActiveDeal newItem) {
            return oldItem.sameContentAs(newItem);
        }
    };

    private final Listener listener;

    public ActiveDealsAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemActiveDealBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false),
                listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemActiveDealBinding binding;
        private final Listener listener;

        ViewHolder(ItemActiveDealBinding binding, Listener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(ActiveDeal deal) {
            Context context = itemView.getContext();
            String noValue = context.getString(R.string.no_value);

            String lot = deal.getLotNumber();
            binding.tvLot.setText(lot == null || lot.trim().isEmpty()
                    ? noValue : context.getString(R.string.deal_lot, lot));

            String material = deal.getMaterialName();
            String quantity = Formatters.number(deal.getQuantity());
            String name = material == null || material.trim().isEmpty() ? noValue : material;
            binding.tvMaterial.setText(quantity == null
                    ? name : context.getString(R.string.deal_material_quantity, name, quantity));

            String price = Formatters.currency(deal.getPricePerUnit(), null, 0);
            binding.tvPrice.setText(price == null ? noValue : context.getString(R.string.price_per_mt, price));
            binding.tvOfferLabel.setText(deal.isOfferSentByYou() ? R.string.deal_counter_sent : R.string.deal_last_offer);

            boolean yourTurn = deal.getTurnStatus() == ActiveDeal.Turn.YOURS;
            binding.tvBadge.setText(yourTurn ? R.string.badge_your_turn : R.string.badge_pending_vendor);
            binding.tvBadge.setBackgroundResource(yourTurn ? R.drawable.bg_badge_turn : R.drawable.bg_badge_pending);
            binding.tvBadge.setTextColor(context.getColor(yourTurn ? R.color.badgeTurnText : R.color.badgePendingText));

            binding.actions.setVisibility(yourTurn ? View.VISIBLE : View.GONE);
            binding.btnCounter.setOnClickListener(v -> listener.onCounter(deal));
            binding.btnDecline.setOnClickListener(v -> listener.onDecline(deal));
        }
    }
}
