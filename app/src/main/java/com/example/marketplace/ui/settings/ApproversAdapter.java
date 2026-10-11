package com.example.marketplace.ui.settings;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marketplace.R;
import com.example.marketplace.databinding.ItemSettingsApproverBinding;

import java.util.List;

/**
 * The approver rows of Approval Workflows. The adapter works directly on the draft's list, so a
 * drag or a remove changes the draft; the screen is told through the listener to refresh "unsaved".
 */
public class ApproversAdapter extends RecyclerView.Adapter<ApproversAdapter.ViewHolder> {

    public interface Listener {
        void onRemove(int position);

        void onMove(int from, int to);

        void onStartDrag(@NonNull RecyclerView.ViewHolder holder);
    }

    private final Listener listener;
    private List<SettingsDraft.Approver> items;

    public ApproversAdapter(@NonNull List<SettingsDraft.Approver> items, @NonNull Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    /** Shows another list (e.g. after Cancel Changes replaced the draft). */
    @SuppressLint("NotifyDataSetChanged")
    public void setItems(@NonNull List<SettingsDraft.Approver> newItems) {
        items = newItems;
        notifyDataSetChanged();
    }

    /** Called while the person drags: moves the row on screen and in the draft. */
    public void move(int from, int to) {
        listener.onMove(from, to);
        notifyItemMoved(from, to);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemSettingsApproverBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SettingsDraft.Approver approver = items.get(position);
        String name = approver.name == null || approver.name.trim().isEmpty()
                ? holder.itemView.getContext().getString(R.string.no_value) : approver.name;
        holder.binding.tvApprover.setText(approver.title == null || approver.title.trim().isEmpty()
                ? name
                : holder.itemView.getContext().getString(R.string.settings_approver_label, name, approver.title));
        holder.binding.ivDrag.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                listener.onStartDrag(holder);
            }
            return false;
        });
        holder.binding.btnRemove.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                listener.onRemove(adapterPosition);
                notifyItemRemoved(adapterPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemSettingsApproverBinding binding;

        ViewHolder(@NonNull ItemSettingsApproverBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
