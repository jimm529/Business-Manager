package com.example.marketplace.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.marketplace.R;
import com.example.marketplace.data.model.notifications.NotificationItem;
import com.example.marketplace.databinding.FragmentNotificationsBinding;
import com.example.marketplace.ui.messages.ActionResult;
import com.example.marketplace.ui.messages.ChatFragment;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.example.marketplace.utils.RoleAccess;
import com.example.marketplace.utils.SessionManager;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/**
 * The notification list behind every bell: new messages, deals waiting for you and order updates.
 * Tapping one marks it read and opens the screen it is about. Pull down to reload.
 */
public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private NotificationsViewModel viewModel;
    private NotificationsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);

        adapter = new NotificationsAdapter(this::open);
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNotifications.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        binding.btnMarkAll.setOnClickListener(v -> viewModel.markAllRead());
        binding.swipeRefresh.setColorSchemeColors(
                MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorPrimary));
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
        binding.states.btnRetry.setOnClickListener(v -> viewModel.refresh());

        viewModel.getNotifications().observe(getViewLifecycleOwner(), this::render);
        viewModel.getErrors().observe(getViewLifecycleOwner(), this::showError);
    }

    private void render(Resource<List<NotificationItem>> resource) {
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
                List<NotificationItem> items = resource.data;
                adapter.submitList(items);
                binding.btnMarkAll.setEnabled(items != null && NotificationsViewModel.unreadCount(items) > 0);
                if (items == null || items.isEmpty()) {
                    binding.states.tvEmpty.setText(R.string.notifications_empty);
                    binding.states.emptyGroup.setVisibility(View.VISIBLE);
                }
                break;
            case ERROR:
                binding.swipeRefresh.setRefreshing(false);
                hideStates();
                adapter.submitList(null);
                binding.btnMarkAll.setEnabled(false);
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

    // ---------------------------------------------------------------- opening one

    /** Marks the notification read, then goes to the screen it is about. */
    private void open(@NonNull NotificationItem item) {
        viewModel.markRead(item);
        NavController navController = NavHostFragment.findNavController(this);
        boolean vendor = RoleAccess.isVendor(SessionManager.getInstance().getRole());
        String type = item.getType();

        if (NotificationItem.TYPE_MESSAGE.equals(type)) {
            String conversationId = item.getConversationId();
            if (conversationId != null) {
                Bundle args = new Bundle();
                args.putString(ChatFragment.ARG_CONVERSATION_ID, conversationId);
                navController.navigate(R.id.action_notifications_to_chat, args);
            } else {
                openTab(navController, R.id.messagesFragment);
            }
        } else if (NotificationItem.TYPE_DEAL.equals(type)) {
            openTab(navController, vendor ? R.id.vendorRequestsFragment : R.id.messagesFragment);
        } else {
            // Order and shipping updates: the buyer's Orders, or the vendor's Sales History.
            navController.navigate(vendor ? R.id.vendorSalesFragment : R.id.ordersFragment);
        }
    }

    /** Opens a bottom-bar screen on top of Home, the same way the tabs do. */
    private void openTab(NavController navController, int destinationId) {
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.homeFragment, false)
                .build();
        navController.navigate(destinationId, null, options);
    }

    private void showError(@Nullable Event<ActionResult> event) {
        ActionResult result = event == null ? null : event.getContentIfNotHandled();
        if (result != null) {
            Snackbar.make(binding.getRoot(), result.text(requireContext()), Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
