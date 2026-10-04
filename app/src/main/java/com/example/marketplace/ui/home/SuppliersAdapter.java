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
import com.example.marketplace.data.model.dashboard.SupplierSummary;
import com.example.marketplace.databinding.ItemSupplierGridBinding;

import java.util.Objects;

/** Draws the two-column "Regular Suppliers" tiles: logo above the company name. */
public class SuppliersAdapter extends ListAdapter<SupplierSummary, SuppliersAdapter.ViewHolder> {

    private static final DiffUtil.ItemCallback<SupplierSummary> DIFF = new DiffUtil.ItemCallback<SupplierSummary>() {
        @Override
        public boolean areItemsTheSame(@NonNull SupplierSummary oldItem, @NonNull SupplierSummary newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull SupplierSummary oldItem, @NonNull SupplierSummary newItem) {
            return oldItem.sameContentAs(newItem);
        }
    };

    public SuppliersAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemSupplierGridBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSupplierGridBinding binding;

        ViewHolder(ItemSupplierGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(SupplierSummary supplier) {
            Context context = itemView.getContext();
            String name = supplier.getName();
            binding.tvName.setText(name == null || name.trim().isEmpty() ? context.getString(R.string.unknown_vendor) : name);

            String image = supplier.getImageUrl();
            if (image == null) {
                Glide.with(binding.imgSupplier).clear(binding.imgSupplier);
                binding.imgSupplier.setImageResource(R.drawable.ic_vendor_factory);
            } else {
                Glide.with(binding.imgSupplier).load(image)
                        .placeholder(R.drawable.bg_image_placeholder)
                        .error(R.drawable.ic_vendor_factory)
                        .centerCrop().into(binding.imgSupplier);
            }
        }
    }
}
