package com.example.marketplace.ui.messages;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marketplace.R;
import com.example.marketplace.data.model.messages.Conversation;
import com.example.marketplace.databinding.ItemConversationBinding;
import com.example.marketplace.utils.Formatters;
import com.google.android.material.color.MaterialColors;

import java.util.Objects;

/** Draws the conversation list. Threads with unread messages get a green tint and a dark green bar on the left. */
public class ConversationsAdapter extends ListAdapter<Conversation, ConversationsAdapter.ViewHolder> {

    /** What happens when a thread is tapped. */
    public interface Listener {
        void onConversationClick(@NonNull Conversation conversation);
    }

    private final Listener listener;
    @Nullable
    private final String myRole;

    public ConversationsAdapter(@Nullable String myRole, @NonNull Listener listener) {
        super(new DiffUtil.ItemCallback<Conversation>() {
            @Override
            public boolean areItemsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
                return Objects.equals(oldItem.getId(), newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
                return oldItem.sameContentAs(newItem, myRole);
            }
        });
        this.myRole = myRole;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemConversationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), myRole, listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemConversationBinding binding;

        ViewHolder(ItemConversationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Conversation conversation, @Nullable String role, Listener listener) {
            Context context = itemView.getContext();

            String name = conversation.getParticipantName(role);
            binding.tvName.setText(name == null ? context.getString(R.string.messages_unknown_contact) : name);
            binding.tvTime.setText(Formatters.listTimestamp(context, conversation.getTimestamp()));

            String batch = conversation.getBatchTitle();
            binding.tvBatch.setVisibility(batch == null ? View.GONE : View.VISIBLE);
            binding.tvBatch.setText(batch);

            String preview = conversation.getLastMessage();
            binding.tvPreview.setText(preview == null ? context.getString(R.string.messages_no_preview) : preview);

            int unread = conversation.getUnreadCount(role);
            boolean highlighted = unread > 0;
            binding.tvUnread.setVisibility(highlighted ? View.VISIBLE : View.GONE);
            binding.tvUnread.setText(String.valueOf(Math.min(unread, 99)));
            binding.viewIndicator.setVisibility(highlighted ? View.VISIBLE : View.INVISIBLE);
            binding.root.setBackgroundColor(MaterialColors.getColor(binding.root, highlighted
                    ? com.google.android.material.R.attr.colorSecondaryContainer
                    : com.google.android.material.R.attr.colorSurface));

            binding.root.setOnClickListener(v -> listener.onConversationClick(conversation));
        }
    }
}
