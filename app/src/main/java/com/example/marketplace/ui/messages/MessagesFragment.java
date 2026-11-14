package com.example.marketplace.ui.messages;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import com.example.marketplace.data.model.messages.Conversation;
import com.example.marketplace.databinding.FragmentMessagesBinding;
import com.example.marketplace.utils.Formatters;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.example.marketplace.utils.SessionManager;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/**
 * The Messages tab: every conversation, newest first, with search and pull-to-refresh.
 * Tapping one opens the chat.
 */
public class MessagesFragment extends Fragment {

    private FragmentMessagesBinding binding;
    private MessagesViewModel viewModel;
    private ConversationsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMessagesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MessagesViewModel.class);

        adapter = new ConversationsAdapter(SessionManager.getInstance().getRole(), this::openChat);
        binding.rvConversations.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvConversations.setAdapter(adapter);

        setUpHeader();
        setUpSearch();
        binding.swipeRefresh.setColorSchemeColors(
                MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorPrimary));
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
        binding.states.btnRetry.setOnClickListener(v -> viewModel.refresh());

        viewModel.getConversations().observe(getViewLifecycleOwner(), this::render);
        viewModel.getTotalUnread().observe(getViewLifecycleOwner(), unread ->
                binding.dotNotifications.setVisibility(unread != null && unread > 0 ? View.VISIBLE : View.GONE));
    }

    // ---------------------------------------------------------------- set-up

    private void setUpHeader() {
        binding.btnBack.setOnClickListener(v -> {
            if (!NavHostFragment.findNavController(this).popBackStack()) {
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });
        binding.btnNotifications.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_global_notifications));

        binding.tvAvatar.setText(Formatters.initials(requireContext(), SessionManager.getInstance().getFullName()));
        binding.tvAvatar.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(requireContext(), v);
            menu.getMenu().add(R.string.logout);
            menu.setOnMenuItemClickListener(item -> {
                SessionManager.getInstance().clear();
                NavHostFragment.findNavController(this).navigate(R.id.action_global_loginFragment);
                return true;
            });
            menu.show();
        });
    }

    private void setUpSearch() {
        // After a rotation the box is empty again: show the search the view model still has.
        if (TextUtils.isEmpty(binding.etSearch.getText()) && !viewModel.getQuery().isEmpty()) {
            binding.etSearch.setText(viewModel.getQuery());
        }
        // The list is already on the phone, so it can be filtered on every key.
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setQuery(s.toString());
            }
        });
    }

    // ---------------------------------------------------------------- list

    private void render(Resource<List<Conversation>> resource) {
        switch (resource.status) {
            case LOADING:
                hideStates();
                if (resource.data == null) {
                    adapter.submitList(null);
                    binding.states.progress.setVisibility(View.VISIBLE);
                } else {
                    adapter.submitList(resource.data);
                }
                break;
            case SUCCESS:
                binding.swipeRefresh.setRefreshing(false);
                hideStates();
                List<Conversation> items = resource.data;
                adapter.submitList(items);
                if (items == null || items.isEmpty()) {
                    binding.states.tvEmpty.setText(viewModel.getQuery().isEmpty()
                            ? R.string.messages_empty : R.string.messages_search_empty);
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

    private void hideStates() {
        binding.states.progress.setVisibility(View.GONE);
        binding.states.emptyGroup.setVisibility(View.GONE);
        binding.states.errorGroup.setVisibility(View.GONE);
    }

    private void openChat(@NonNull Conversation conversation) {
        String id = conversation.getId();
        if (id == null) {
            return;
        }
        String role = SessionManager.getInstance().getRole();
        Bundle args = new Bundle();
        args.putString(ChatFragment.ARG_CONVERSATION_ID, id);
        args.putString(ChatFragment.ARG_PARTICIPANT_NAME, conversation.getParticipantName(role));
        args.putString(ChatFragment.ARG_PARTICIPANT_SUBTITLE, conversation.getParticipantSubtitle(role));
        args.putString(ChatFragment.ARG_BATCH_NUMBER, conversation.getBatchTitle());
        NavHostFragment.findNavController(this).navigate(R.id.action_messages_to_chat, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
