package com.example.marketplace.ui.notifications;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marketplace.R;
import com.example.marketplace.data.model.notifications.NotificationItem;
import com.example.marketplace.databinding.ItemNotificationBinding;
import com.example.marketplace.utils.Formatters;
import com.google.android.material.color.MaterialColors;

import java.util.Date;
import java.util.Objects;

/** Draws the notifications: an icon for the kind, title, text and how long ago. Unread ones are tinted green. */
public class NotificationsAdapter extends ListAdapter<NotificationItem, NotificationsAdapter.ViewHolder> {

    /** What happens when a notification is tapped. */
    public interface Listener {
        void onNotificationClick(@NonNull NotificationItem item);
    }

    private static final DiffUtil.ItemCallback<NotificationItem> DIFF = new DiffUtil.ItemCallback<NotificationItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull NotificationItem oldItem, @NonNull NotificationItem newItem) {
            return Objects.equals(oldItem.getKey(), newItem.getKey());
        }

        @Override
        public boolean areContentsTheSame(@NonNull NotificationItem oldItem, @NonNull NotificationItem newItem) {
            return oldItem.sameContentAs(newItem);
        }
    };

    private final Listener listener;

    public NotificationsAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemNotificationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    @DrawableRes
    private static int iconFor(NotificationItem item) {
        String type = item.getType();
        if (NotificationItem.TYPE_MESSAGE.equals(type)) {
            return R.drawable.ic_nav_messages;
        }
        if (NotificationItem.TYPE_DEAL.equals(type)) {
            return R.drawable.ic_swap;
        }
        if (NotificationItem.TYPE_LOGISTICS.equals(type)) {
            return R.drawable.ic_local_shipping;
        }
        return R.drawable.ic_orders;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemNotificationBinding binding;

        ViewHolder(ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(NotificationItem item, Listener listener) {
            Context context = itemView.getContext();
            binding.tvTitle.setText(item.getTitle() == null ? context.getString(R.string.no_value) : item.getTitle());
            binding.tvBody.setText(item.getBody() == null ? "" : item.getBody());
            binding.tvBody.setVisibility(item.getBody() == null || item.getBody().isEmpty() ? View.GONE : View.VISIBLE);
            binding.ivIcon.setImageResource(iconFor(item));

            Date when = Formatters.parseIso(item.getCreatedAt());
            binding.tvTime.setText(when == null ? "" : DateUtils.getRelativeTimeSpanString(when.getTime(),
                    System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));

            boolean unread = !item.isRead();
            binding.viewUnread.setVisibility(unread ? View.VISIBLE : View.INVISIBLE);
            binding.root.setBackgroundColor(MaterialColors.getColor(binding.root, unread
                    ? com.google.android.material.R.attr.colorSecondaryContainer
                    : com.google.android.material.R.attr.colorSurface));
            binding.tvTitle.setTypeface(null, unread ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            binding.root.setOnClickListener(v -> listener.onNotificationClick(item));
        }
    }
}
