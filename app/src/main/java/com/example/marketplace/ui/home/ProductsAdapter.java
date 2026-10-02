package com.example.marketplace.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.marketplace.R;
import com.example.marketplace.data.model.dashboard.Product;
import com.example.marketplace.databinding.ItemProductCardBinding;
import com.example.marketplace.utils.Formatters;

import java.util.Objects;

/** Draws the marketplace listing cards on the Operator home feed. */
public class ProductsAdapter extends ListAdapter<Product, ProductsAdapter.ViewHolder> {

    private static final DiffUtil.ItemCallback<Product> DIFF = new DiffUtil.ItemCallback<Product>() {
        @Override
        public boolean areItemsTheSame(@NonNull Product oldItem, @NonNull Product newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Product oldItem, @NonNull Product newItem) {
            return Objects.equals(oldItem.getTitle(), newItem.getTitle())
                    && Objects.equals(oldItem.getStatus(), newItem.getStatus())
                    && Objects.equals(oldItem.getQuantity(), newItem.getQuantity())
                    && Objects.equals(oldItem.getPricePerKg(), newItem.getPricePerKg())
                    && Objects.equals(oldItem.getLocation(), newItem.getLocation())
                    && Objects.equals(oldItem.getFirstPhoto(), newItem.getFirstPhoto());
        }
    };

    public ProductsAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemProductCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemProductCardBinding binding;

        ViewHolder(ItemProductCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Product product) {
            Context context = itemView.getContext();
            String noValue = context.getString(R.string.no_value);

            binding.tvTitle.setText(orElse(product.getTitle(), noValue));
            binding.tvStatus.setText(orElse(product.getStatus(), noValue));

            Product.Seller seller = product.getVendor();
            String sellerName = seller == null ? null : seller.getDisplayName();
            binding.tvVendor.setText(sellerName == null ? context.getString(R.string.unknown_vendor) : sellerName);

            // "12.5 MT · 99.5% purity" (purity is left out when the listing has none)
            String quantity = Formatters.number(product.getQuantity());
            String unit = product.getUnit() == null ? "" : product.getUnit();
            String volume = quantity == null ? noValue : context.getString(R.string.order_quantity, quantity, unit).trim();
            String purity = Formatters.number(product.getLeadPurity());
            binding.tvVolume.setText(purity == null
                    ? volume
                    : context.getString(R.string.listing_volume_purity, volume, purity));

            binding.tvLocation.setText(orElse(product.getLocation(), noValue));

            String price = Formatters.currency(product.getPricePerKg(), null, 2);
            binding.tvPrice.setText(price == null ? noValue : context.getString(R.string.price_per_kg, price));

            String photo = product.getFirstPhoto();
            if (photo == null) {
                Glide.with(binding.imgPhoto).clear(binding.imgPhoto);
                binding.imgPhoto.setImageDrawable(null);
            } else {
                Glide.with(binding.imgPhoto).load(photo).centerCrop().into(binding.imgPhoto);
            }
        }

        private static String orElse(String value, String fallback) {
            return (value == null || value.trim().isEmpty()) ? fallback : value;
        }
    }
}
