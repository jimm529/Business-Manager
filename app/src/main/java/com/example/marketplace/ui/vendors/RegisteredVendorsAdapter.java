package com.example.marketplace.ui.vendors;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marketplace.R;
import com.example.marketplace.data.model.vendor.RegisteredVendor;
import com.example.marketplace.databinding.ItemRegisteredVendorCardBinding;
import com.example.marketplace.utils.Formatters;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

/** Draws the vendor cards: name and status, location and last audit, material, volume, compliance and a Details link. */
public class RegisteredVendorsAdapter extends ListAdapter<RegisteredVendor, RegisteredVendorsAdapter.ViewHolder> {

    /** What happens when "Details" is tapped. */
    public interface Listener {
        void onDetailsClick(@NonNull RegisteredVendor vendor);
    }

    private final Listener listener;

    public RegisteredVendorsAdapter(@NonNull Listener listener) {
        super(new DiffUtil.ItemCallback<RegisteredVendor>() {
            @Override
            public boolean areItemsTheSame(@NonNull RegisteredVendor oldItem, @NonNull RegisteredVendor newItem) {
                return Objects.equals(oldItem.getId(), newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull RegisteredVendor oldItem, @NonNull RegisteredVendor newItem) {
                return oldItem.sameContentAs(newItem);
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemRegisteredVendorCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemRegisteredVendorCardBinding binding;

        ViewHolder(ItemRegisteredVendorCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull RegisteredVendor vendor, @NonNull Listener listener) {
            Context context = itemView.getContext();
            String dash = context.getString(R.string.no_value);

            binding.tvName.setText(orDash(vendor.getName(), dash));
            bindStatus(context, vendor.getStatus());

            String location = orDash(vendor.getLocation(), dash);
            binding.tvSubtitle.setText(context.getString(R.string.vendors_location_audit,
                    location, auditDate(context, vendor.getLastAudit())));

            binding.tvMaterial.setText(orDash(vendor.getPrimaryMaterial(), dash));
            binding.tvVolume.setText(volume(vendor.getVolumeTons(), dash));
            bindCompliance(context, vendor);

            binding.btnDetails.setOnClickListener(v -> listener.onDetailsClick(vendor));
        }

        private void bindStatus(Context context, @Nullable String status) {
            int label;
            int background;
            int textColor;
            if (RegisteredVendor.STATUS_ACTIVE.equals(status)) {
                label = R.string.vendors_status_active;
                background = R.drawable.bg_badge_turn;
                textColor = R.color.badgeTurnText;
            } else if (RegisteredVendor.STATUS_PENDING.equals(status)) {
                label = R.string.vendors_status_pending;
                background = R.drawable.bg_badge_market_extraction;
                textColor = R.color.marketBadgeExtractionText;
            } else {
                label = R.string.vendors_status_inactive;
                background = R.drawable.bg_badge_market_neutral;
                textColor = R.color.marketBadgeNeutralText;
            }
            binding.tvStatus.setText(label);
            binding.tvStatus.setBackgroundResource(background);
            binding.tvStatus.setTextColor(ContextCompat.getColor(context, textColor));
        }

        /** A coloured dot (green / yellow / red) in front of the certificate name. */
        private void bindCompliance(Context context, RegisteredVendor vendor) {
            String state = vendor.getComplianceStatus();
            @ColorRes int dotColor;
            int fallbackLabel;
            if (RegisteredVendor.COMPLIANCE_EXPIRED.equals(state)) {
                dotColor = R.color.colorError;
                fallbackLabel = R.string.vendors_compliance_expired;
            } else if (RegisteredVendor.COMPLIANCE_PENDING.equals(state)) {
                dotColor = R.color.statusProcessing;
                fallbackLabel = R.string.vendors_compliance_pending;
            } else {
                dotColor = R.color.statusOnTheWay;
                fallbackLabel = R.string.vendors_compliance_compliant;
            }
            String label = vendor.getComplianceLabel();
            binding.tvCompliance.setText(label != null ? label : context.getString(fallbackLabel));
            Drawable dot = ContextCompat.getDrawable(context, R.drawable.bg_status_dot);
            if (dot != null) {
                dot = dot.mutate();
                dot.setTint(ContextCompat.getColor(context, dotColor));
            }
            binding.tvCompliance.setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null);
        }

        @NonNull
        private static String orDash(@Nullable String value, @NonNull String dash) {
            return value == null ? dash : value;
        }

        /** 1240.5 -> "1,240.50". */
        @NonNull
        private static String volume(@Nullable Double tons, @NonNull String dash) {
            if (tons == null) {
                return dash;
            }
            NumberFormat format = NumberFormat.getNumberInstance(Locale.getDefault());
            format.setMinimumFractionDigits(2);
            format.setMaximumFractionDigits(2);
            return format.format(tons);
        }

        /** "2023-11-15" (the audit day), or a dash when unknown. */
        @NonNull
        private static String auditDate(Context context, @Nullable String iso) {
            Date date = Formatters.parseIso(iso);
            if (date == null) {
                return context.getString(R.string.no_value);
            }
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC")); // parseIso reads server time as UTC
            return format.format(date);
        }
    }
}
