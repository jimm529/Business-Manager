package com.example.marketplace.ui.messages;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.marketplace.R;
import com.example.marketplace.data.model.bid.NegotiationDetail;
import com.example.marketplace.data.model.messages.ChatMessage;
import com.example.marketplace.databinding.FragmentChatBinding;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.example.marketplace.utils.SessionManager;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Locale;

/**
 * One conversation: the messages, an input bar to reply, and a tappable bid pill that opens
 * the Counter Price screen for that deal. Pull down to reload.
 */
public class ChatFragment extends Fragment {

    public static final String ARG_CONVERSATION_ID = "conversationId";
    public static final String ARG_PARTICIPANT_NAME = "participantName";
    public static final String ARG_PARTICIPANT_SUBTITLE = "participantSubtitle";
    public static final String ARG_BATCH_NUMBER = "batchNumber";

    private FragmentChatBinding binding;
    private ChatViewModel viewModel;
    private ChatAdapter adapter;
    private String conversationId;
    private int shownCount;
    private boolean resumedBefore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        conversationId = args == null ? null : args.getString(ARG_CONVERSATION_ID);
        if (conversationId == null) {
            // Nothing to show without a conversation: go back.
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        viewModel.init(conversationId);

        setUpHeader(args);

        adapter = new ChatAdapter(SessionManager.getInstance().getRole(), this::openBid);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(adapter);

        binding.swipeRefresh.setColorSchemeColors(
                MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorPrimary));
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
        binding.states.btnRetry.setOnClickListener(v -> viewModel.refresh());

        binding.btnAttach.setOnClickListener(v -> comingSoon());
        binding.btnSend.setOnClickListener(v -> viewModel.send(String.valueOf(binding.etMessage.getText())));

        viewModel.getMessages().observe(getViewLifecycleOwner(), this::render);
        viewModel.isSending().observe(getViewLifecycleOwner(), sending ->
                binding.btnSend.setEnabled(!Boolean.TRUE.equals(sending)));
        viewModel.getSendResults().observe(getViewLifecycleOwner(), this::handleSendResult);
    }

    private void setUpHeader(@Nullable Bundle args) {
        String name = args == null ? null : args.getString(ARG_PARTICIPANT_NAME);
        String subtitle = args == null ? null : args.getString(ARG_PARTICIPANT_SUBTITLE);
        if (TextUtils.isEmpty(subtitle) && args != null) {
            subtitle = args.getString(ARG_BATCH_NUMBER);
        }
        binding.tvName.setText(TextUtils.isEmpty(name) ? getString(R.string.messages_unknown_contact) : name);
        binding.tvRole.setVisibility(TextUtils.isEmpty(subtitle) ? View.GONE : View.VISIBLE);
        binding.tvRole.setText(subtitle == null ? null : subtitle.toUpperCase(Locale.getDefault()));

        binding.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        binding.btnCall.setOnClickListener(v -> comingSoon());
        binding.btnMenu.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(requireContext(), v);
            menu.getMenu().add(R.string.chat_menu_refresh);
            menu.setOnMenuItemClickListener(item -> {
                viewModel.refresh();
                return true;
            });
            menu.show();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Coming back from the Counter Price screen: the deal (and its messages) may have changed.
        if (resumedBefore && viewModel != null) {
            viewModel.refresh();
        }
        resumedBefore = true;
    }

    // ---------------------------------------------------------------- messages

    private void render(Resource<List<ChatMessage>> resource) {
        switch (resource.status) {
            case LOADING:
                hideStates();
                if (resource.data == null) {
                    adapter.submitList(null);
                    binding.states.progress.setVisibility(View.VISIBLE);
                } else {
                    submit(resource.data);
                }
                break;
            case SUCCESS:
                binding.swipeRefresh.setRefreshing(false);
                hideStates();
                List<ChatMessage> items = resource.data;
                submit(items);
                if (items == null || items.isEmpty()) {
                    binding.states.tvEmpty.setText(R.string.chat_empty);
                    binding.states.emptyGroup.setVisibility(View.VISIBLE);
                }
                break;
            case ERROR:
                binding.swipeRefresh.setRefreshing(false);
                hideStates();
                adapter.submitList(null);
                binding.states.tvError.setText(ResourceMessages.errorText(requireContext(), resource));
                binding.states.errorGroup.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    /** Shows the list and jumps to the newest message when new ones arrived. */
    private void submit(@Nullable List<ChatMessage> items) {
        int count = items == null ? 0 : items.size();
        boolean grew = count > shownCount;
        shownCount = count;
        adapter.submitList(items, () -> {
            if (grew && binding != null && count > 0) {
                binding.rvMessages.scrollToPosition(count - 1);
            }
        });
    }

    private void hideStates() {
        binding.states.progress.setVisibility(View.GONE);
        binding.states.emptyGroup.setVisibility(View.GONE);
        binding.states.errorGroup.setVisibility(View.GONE);
    }

    private void handleSendResult(@Nullable Event<ActionResult> event) {
        ActionResult result = event == null ? null : event.getContentIfNotHandled();
        if (result == null) {
            return;
        }
        if (result.isError()) {
            // Keep the typed text so nothing is lost.
            Snackbar.make(binding.getRoot(), result.text(requireContext()), Snackbar.LENGTH_LONG).show();
        } else {
            binding.etMessage.setText("");
        }
    }

    private void openBid(@NonNull ChatMessage message) {
        NegotiationDetail bid = message.getBid();
        if (bid == null || bid.getBidId() == null) {
            return;
        }
        Bundle args = new Bundle();
        args.putString(CounterPriceFragment.ARG_BID_ID, bid.getBidId());
        args.putString(CounterPriceFragment.ARG_CONVERSATION_ID, conversationId);
        args.putString(CounterPriceFragment.ARG_BATCH_NUMBER, bid.getBatchNumber());
        NavHostFragment.findNavController(this).navigate(R.id.action_chat_to_counterPrice, args);
    }

    private void comingSoon() {
        Snackbar.make(binding.getRoot(), R.string.coming_soon, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
