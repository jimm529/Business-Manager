package com.example.marketplace.ui.vendorinventory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.marketplace.R;
import com.example.marketplace.data.model.vendor.VendorLot;
import com.example.marketplace.databinding.ItemVendorLotCardBinding;
import com.example.marketplace.databinding.ItemVendorLotRowBinding;
import com.example.marketplace.utils.Formatters;

import java.util.Locale;
import java.util.Objects;

/** Draws a vendor's lots, either as big cards (like the design) or as compact rows. */
public class VendorLotsAdapter extends ListAdapter<VendorLot, RecyclerView.ViewHolder> {

    /** What happens when a lot is opened. */
    public interface Listener {
        void onLotClick(@NonNull VendorLot lot);
    }

    private static final int TYPE_CARD = 0;
    private static final int TYPE_ROW = 1;

    private static final DiffUtil.ItemCallback<VendorLot> DIFF = new DiffUtil.ItemCallback<VendorLot>() {
        @Override
        public boolean areItemsTheSame(@NonNull VendorLot oldItem, @NonNull VendorLot newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull VendorLot oldItem, @NonNull VendorLot newItem) {
            return oldItem.sameContentAs(newItem);
        }
    };

    private final Listener listener;
    private boolean compact;

    public VendorLotsAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    /** Switches between cards and rows. */
    public void setCompact(boolean compact) {
        if (this.compact != compact) {
            this.compact = compact;
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return compact ? TYPE_ROW : TYPE_CARD;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ROW) {
            return new RowHolder(ItemVendorLotRowBinding.inflate(inflater, parent, false));
        }
        return new CardHolder(ItemVendorLotCardBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        VendorLot lot = getItem(position);
        if (holder instanceof RowHolder) {
            ((RowHolder) holder).bind(lot, listener);
        } else {
            ((CardHolder) holder).bind(lot, listener);
        }
    }

    // ---------------------------------------------------------------- shared formatting

    private static String quantityText(Context context, VendorLot lot) {
        String quantity = Formatters.number(lot.getQuantity());
        if (quantity == null) {
            return context.getString(R.string.no_value);
        }
        String unit = lot.getUnit() == null || lot.getUnit().trim().isEmpty()
                ? context.getString(R.string.requests_unit_mt) : lot.getUnit();
        return context.getString(R.string.order_quantity, quantity, unit);
    }

    private static String purityText(Context context, VendorLot lot) {
        String purity = Formatters.number(lot.getPurity());
        return purity == null ? context.getString(R.string.no_value) : context.getString(R.string.vendor_percent, purity);
    }

    private static String orDash(Context context, String value) {
        return value == null || value.trim().isEmpty() ? context.getString(R.string.no_value) : value;
    }

    /** Green for available lots, amber for a held reserve, grey for sold. */
    private static void styleBadge(Context context, TextView badge, VendorLot lot) {
        int text;
        int background;
        int color;
        if (lot.isReserved()) {
            text = R.string.vinv_reserved;
            background = R.drawable.bg_badge_market_extraction;
            color = R.color.marketBadgeExtractionText;
        } else if (lot.isSold()) {
            text = R.string.vinv_sold;
            background = R.drawable.bg_badge_market_neutral;
            color = R.color.marketBadgeNeutralText;
        } else {
            text = R.string.vinv_available;
            background = R.drawable.bg_badge_market_ready;
            color = R.color.marketBadgeReadyText;
        }
        String status = lot.getStatus();
        // Other server states (e.g. "Ready for Pickup") are shown as they are, in capitals.
        boolean custom = status != null && !status.trim().isEmpty() && !lot.isReserved() && !lot.isSold()
                && !status.trim().equalsIgnoreCase("available") && !status.trim().equalsIgnoreCase("published");
        badge.setText(custom ? status.trim().toUpperCase(Locale.getDefault()) : context.getString(text));
        badge.setBackgroundResource(background);
        badge.setTextColor(context.getColor(color));
    }

    private static void loadPhoto(ImageView view, VendorLot lot) {
        String photo = lot.getFirstPhoto();
        if (photo == null) {
            Glide.with(view).clear(view);
            view.setImageDrawable(null);
        } else {
            Glide.with(view).load(photo).centerCrop().into(view);
        }
    }

    // ---------------------------------------------------------------- holders

    static class CardHolder extends RecyclerView.ViewHolder {
        private final ItemVendorLotCardBinding binding;

        CardHolder(ItemVendorLotCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(VendorLot lot, Listener listener) {
            Context context = itemView.getContext();
            binding.tvTitle.setText(lot.getTitle() == null || lot.getTitle().trim().isEmpty()
                    ? context.getString(R.string.vinv_untitled) : lot.getTitle());
            binding.tvQuantity.setText(quantityText(context, lot));
            binding.tvPurity.setText(purityText(context, lot));
            binding.tvLocation.setText(orDash(context, lot.getLocation()));
            binding.tvLotId.setText(lot.getLotId() == null ? "" : context.getString(R.string.vinv_lot, lot.getLotId()));
            styleBadge(context, binding.tvBadge, lot);
            loadPhoto(binding.imgPhoto, lot);
            binding.btnDetails.setOnClickListener(v -> listener.onLotClick(lot));
        }
    }

    static class RowHolder extends RecyclerView.ViewHolder {
        private final ItemVendorLotRowBinding binding;

        RowHolder(ItemVendorLotRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(VendorLot lot, Listener listener) {
            Context context = itemView.getContext();
            binding.tvTitle.setText(lot.getTitle() == null || lot.getTitle().trim().isEmpty()
                    ? context.getString(R.string.vinv_untitled) : lot.getTitle());
            binding.tvSummary.setText(context.getString(R.string.vinv_summary,
                    quantityText(context, lot), purityText(context, lot)));
            binding.tvLocation.setText(orDash(context, lot.getLocation()));
            styleBadge(context, binding.tvBadge, lot);
            loadPhoto(binding.imgPhoto, lot);
            binding.root.setOnClickListener(v -> listener.onLotClick(lot));
        }
    }
}
