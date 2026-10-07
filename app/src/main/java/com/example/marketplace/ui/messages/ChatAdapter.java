package com.example.marketplace.ui.messages;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marketplace.R;
import com.example.marketplace.data.model.bid.NegotiationDetail;
import com.example.marketplace.data.model.messages.ChatMessage;
import com.example.marketplace.databinding.ItemChatBidCardBinding;
import com.example.marketplace.databinding.ItemChatIncomingBinding;
import com.example.marketplace.databinding.ItemChatOutgoingBinding;
import com.example.marketplace.utils.Formatters;

import java.util.Objects;

/**
 * Draws the chat: the other person's bubbles on the left, mine on the right, and a tappable
 * "NEW BID RECEIVED" pill for deals sent by the other side.
 */
public class ChatAdapter extends ListAdapter<ChatMessage, RecyclerView.ViewHolder> {

    /** What happens when a bid pill is tapped. */
    public interface Listener {
        void onBidClick(@NonNull ChatMessage message);
    }

    private static final int TYPE_INCOMING = 0;
    private static final int TYPE_OUTGOING = 1;
    private static final int TYPE_BID = 2;

    private static final DiffUtil.ItemCallback<ChatMessage> DIFF = new DiffUtil.ItemCallback<ChatMessage>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.sameContentAs(newItem);
        }
    };

    @Nullable
    private final String myRole;
    private final Listener listener;

    public ChatAdapter(@Nullable String myRole, @NonNull Listener listener) {
        super(DIFF);
        this.myRole = myRole;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = getItem(position);
        if (message.isBidCard(myRole)) {
            return TYPE_BID;
        }
        return message.isMine(myRole) ? TYPE_OUTGOING : TYPE_INCOMING;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_BID:
                return new BidHolder(ItemChatBidCardBinding.inflate(inflater, parent, false));
            case TYPE_OUTGOING:
                return new OutgoingHolder(ItemChatOutgoingBinding.inflate(inflater, parent, false));
            default:
                return new IncomingHolder(ItemChatIncomingBinding.inflate(inflater, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = getItem(position);
        if (holder instanceof BidHolder) {
            ((BidHolder) holder).bind(message, listener);
        } else if (holder instanceof OutgoingHolder) {
            ((OutgoingHolder) holder).bind(message);
        } else {
            ((IncomingHolder) holder).bind(message);
        }
    }

    private static String textOf(Context context, ChatMessage message) {
        String text = message.getMessageText();
        return text == null ? context.getString(R.string.no_value) : text;
    }

    static class IncomingHolder extends RecyclerView.ViewHolder {
        private final ItemChatIncomingBinding binding;

        IncomingHolder(ItemChatIncomingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatMessage message) {
            Context context = itemView.getContext();
            binding.tvMessage.setText(textOf(context, message));
            binding.tvTime.setText(Formatters.messageTimestamp(context, message.getTimestamp()));
        }
    }

    static class OutgoingHolder extends RecyclerView.ViewHolder {
        private final ItemChatOutgoingBinding binding;

        OutgoingHolder(ItemChatOutgoingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatMessage message) {
            Context context = itemView.getContext();
            binding.tvMessage.setText(textOf(context, message));
            binding.tvTime.setText(Formatters.messageTimestamp(context, message.getTimestamp()));
        }
    }

    static class BidHolder extends RecyclerView.ViewHolder {
        private final ItemChatBidCardBinding binding;

        BidHolder(ItemChatBidCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatMessage message, Listener listener) {
            Context context = itemView.getContext();
            NegotiationDetail bid = message.getBid();
            String amount = bid == null ? null : Formatters.currency(bid.getTotalValue(), bid.getCurrency(), 2);
            String shownAmount = amount == null ? context.getString(R.string.no_value) : amount;

            int title = R.string.chat_bid_new;
            if (bid != null && bid.statusIs(NegotiationDetail.STATUS_ACCEPTED)) {
                title = R.string.chat_bid_accepted;
            } else if (bid != null && bid.statusIs(NegotiationDetail.STATUS_DECLINED)) {
                title = R.string.chat_bid_declined;
            } else if (bid != null && bid.statusIs(NegotiationDetail.STATUS_COUNTERED)) {
                title = R.string.chat_bid_countered;
            }
            binding.tvBid.setText(context.getString(title, shownAmount));
            binding.tvCaption.setText(context.getString(R.string.chat_bid_caption,
                    Formatters.messageTimestamp(context, message.getTimestamp()),
                    context.getString(R.string.chat_bid_tap)));
            binding.pill.setOnClickListener(v -> listener.onBidClick(message));
        }
    }
}
