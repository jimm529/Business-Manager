package com.example.marketplace.ui.market;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marketplace.R;
import com.example.marketplace.data.model.market.MarketProduct;
import com.example.marketplace.databinding.ItemMarketCardBinding;
import com.example.marketplace.utils.Formatters;

import java.util.Locale;
import java.util.Objects;

/** Draws the marketplace lot cards: status badge, bookmark, title, place, quantity, purity, bid and PLACE ORDER. */
public class MarketProductsAdapter extends ListAdapter<MarketProduct, MarketProductsAdapter.ViewHolder> {

    /** What happens when the bookmark or PLACE ORDER is tapped on a card. */
    public interface Listener {
        void onBookmarkClick(@NonNull MarketProduct product);

        void onPlaceOrderClick(@NonNull MarketProduct product);
    }

    private static final DiffUtil.ItemCallback<MarketProduct> DIFF = new DiffUtil.ItemCallback<MarketProduct>() {
        @Override
        public boolean areItemsTheSame(@NonNull MarketProduct oldItem, @NonNull MarketProduct newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull MarketProduct oldItem, @NonNull MarketProduct newItem) {
            return oldItem.sameContentAs(newItem);
        }
    };

    private final Listener listener;

    public MarketProductsAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemMarketCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false),
                listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemMarketCardBinding binding;
        private final Listener listener;

        ViewHolder(ItemMarketCardBinding binding, Listener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(MarketProduct product) {
            Context context = itemView.getContext();
            String noValue = context.getString(R.string.no_value);

            bindStatus(context, product.getStatus());

            String title = product.getTitle();
            binding.tvTitle.setText(title == null || title.trim().isEmpty()
                    ? context.getString(R.string.market_untitled_lot) : title);

            String location = product.getLocation();
            binding.tvLocation.setText(location == null || location.trim().isEmpty() ? noValue : location);

            String quantity = Formatters.number(product.getQuantity());
            String unit = product.getUnit() == null ? "" : product.getUnit();
            binding.tvQuantity.setText(quantity == null
                    ? noValue : context.getString(R.string.order_quantity, quantity, unit).trim());

            String purity = Formatters.number(product.getPurityGrade());
            binding.tvPurity.setText(purity == null ? noValue : context.getString(R.string.market_purity_value, purity));

            String price = Formatters.currency(product.getCurrentBidPrice(), product.getCurrency(), 2);
            binding.tvBid.setText(price == null ? noValue : context.getString(R.string.price_per_kg, price));

            binding.btnBookmark.setImageResource(product.isBookmarked()
                    ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
            binding.btnBookmark.setSelected(product.isBookmarked());
            binding.btnBookmark.setOnClickListener(v -> listener.onBookmarkClick(product));

            // Sold or reserved lots cannot be ordered any more.
            boolean orderable = !isClosed(product.getStatus());
            binding.btnPlaceOrder.setEnabled(orderable);
            binding.btnPlaceOrder.setOnClickListener(v -> listener.onPlaceOrderClick(product));
        }

        private void bindStatus(Context context, String status) {
            if (status == null || status.trim().isEmpty()) {
                binding.tvStatus.setText(R.string.market_status_unknown);
                binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_market_neutral);
                binding.tvStatus.setTextColor(context.getColor(R.color.marketBadgeNeutralText));
                return;
            }
            String clean = status.trim().toLowerCase(Locale.ROOT);
            int background;
            int text;
            if (clean.equals("in extraction")) {
                background = R.drawable.bg_badge_market_extraction;
                text = R.color.marketBadgeExtractionText;
            } else if (clean.equals("ready for pickup") || clean.equals("available") || clean.equals("published")) {
                background = R.drawable.bg_badge_market_ready;
                text = R.color.marketBadgeReadyText;
            } else {
                background = R.drawable.bg_badge_market_neutral;
                text = R.color.marketBadgeNeutralText;
            }
            binding.tvStatus.setText(status.trim().toUpperCase(Locale.getDefault()));
            binding.tvStatus.setBackgroundResource(background);
            binding.tvStatus.setTextColor(context.getColor(text));
        }

        private static boolean isClosed(String status) {
            if (status == null) {
                return false;
            }
            String clean = status.trim().toLowerCase(Locale.ROOT);
            return clean.equals("sold") || clean.equals("reserved");
        }
    }
}
