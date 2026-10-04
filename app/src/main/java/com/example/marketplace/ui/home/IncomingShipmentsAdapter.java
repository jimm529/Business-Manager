package com.example.marketplace.ui.home;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marketplace.R;
import com.example.marketplace.data.model.dashboard.ShipmentItem;
import com.example.marketplace.databinding.ItemIncomingShipmentBinding;
import com.example.marketplace.utils.Formatters;

import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/** Draws the "Incoming Shipments" rows with a green, orange or grey status dot. */
public class IncomingShipmentsAdapter extends ListAdapter<ShipmentItem, IncomingShipmentsAdapter.ViewHolder> {

    /** What happens when the eye icon is tapped. */
    public interface Listener {
        void onView(@NonNull ShipmentItem shipment);
    }

    private static final DiffUtil.ItemCallback<ShipmentItem> DIFF = new DiffUtil.ItemCallback<ShipmentItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull ShipmentItem oldItem, @NonNull ShipmentItem newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ShipmentItem oldItem, @NonNull ShipmentItem newItem) {
            return oldItem.sameContentAs(newItem);
        }
    };

    private final Listener listener;

    public IncomingShipmentsAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(
                ItemIncomingShipmentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemIncomingShipmentBinding binding;
        private final Listener listener;

        ViewHolder(ItemIncomingShipmentBinding binding, Listener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(ShipmentItem shipment) {
            Context context = itemView.getContext();
            String noValue = context.getString(R.string.no_value);

            String lot = shipment.getLotNumber();
            binding.tvLot.setText(lot == null || lot.trim().isEmpty() ? noValue : lot);

            int labelRes;
            int colorRes;
            switch (shipment.getStatus()) {
                case ON_THE_WAY:
                    labelRes = R.string.shipment_on_the_way;
                    colorRes = R.color.statusOnTheWay;
                    break;
                case PROCESSING:
                    labelRes = R.string.shipment_processing;
                    colorRes = R.color.statusProcessing;
                    break;
                default:
                    labelRes = R.string.shipment_booked;
                    colorRes = R.color.statusBooked;
                    break;
            }
            binding.tvStatus.setText(context.getString(R.string.shipment_status_line, context.getString(labelRes)));
            binding.tvStatus.setTextColor(context.getColor(colorRes));

            String material = shipment.getMaterialName();
            binding.tvMaterial.setText(material == null || material.trim().isEmpty() ? noValue : material);

            String date = shipment.getDate() == null ? noValue
                    : DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMM d, yyyy"),
                    new Date(shipment.getDate())).toString();
            String number = Formatters.number(shipment.getWeight());
            String weight = number == null ? noValue : context.getString(R.string.shipment_weight, number,
                    shipment.getUnit() == null ? "" : shipment.getUnit()).trim();
            binding.tvDateWeight.setText(context.getString(R.string.shipment_date_weight, date, weight));

            binding.btnView.setOnClickListener(v -> listener.onView(shipment));
        }
    }
}
