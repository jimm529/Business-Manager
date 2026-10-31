package com.example.marketplace;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.marketplace.databinding.ActivityMainBinding;
import com.example.marketplace.utils.RoleAccess;
import com.example.marketplace.utils.SessionManager;
import com.google.android.material.snackbar.Snackbar;

/** The single activity. It hosts the navigation graph and the bottom tab bar; screens are fragments. */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Keep content clear of the status bar, navigation bar and keyboard.
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // The navigation host is ready once the first frame is being prepared.
        final boolean freshStart = savedInstanceState == null;
        binding.getRoot().post(() -> setUpNavigation(freshStart));

        // When the server rejects the saved token (401), go back to Login.
        SessionManager.getInstance().getSessionExpired().observe(this, expired -> {
            if (Boolean.TRUE.equals(expired)) {
                NavController navController = getNavController();
                if (navController != null) {
                    navController.navigate(R.id.action_global_loginFragment);
                }
                Snackbar.make(binding.getRoot(), R.string.error_session_expired, Snackbar.LENGTH_LONG).show();
                SessionManager.getInstance().consumeSessionExpired();
            }
        });
    }

    private void setUpNavigation(boolean freshStart) {
        NavController navController = getNavController();
        if (navController == null) {
            return;
        }

        // Already signed in when the app opens: go straight to Home (Back will not return to Login).
        if (freshStart && SessionManager.getInstance().isLoggedIn()) {
            NavDestination current = navController.getCurrentDestination();
            if (current != null && current.getId() == R.id.loginFragment) {
                navController.navigate(R.id.action_global_homeFragment);
            }
        }

        final View[] tabViews = {binding.bottomNav.tabHome, binding.bottomNav.tabMarket,
                binding.bottomNav.tabMessages, binding.bottomNav.tabMore};

        // Each tab opens the screen of the signed-in role, on top of Home, so Back always
        // returns to Home and then leaves the app. The role is read at tap time, so a
        // different person signing in later gets their own tabs without restarting the app.
        for (int i = 0; i < tabViews.length; i++) {
            final int slot = i;
            tabViews[i].setOnClickListener(v -> openTopLevel(navController, tabIdsFor()[slot]));
        }
        View.OnClickListener openAction = v -> openTopLevel(navController, tabsForRole().action);
        binding.bottomNav.fabAction.setOnClickListener(openAction);
        binding.bottomNav.tabAction.setOnClickListener(openAction);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            String role = SessionManager.getInstance().getRole();

            // The two modules have different screens: send the person home if this one is not theirs.
            if (SessionManager.getInstance().isLoggedIn() && !RoleAccess.canOpen(role, destination.getId())) {
                binding.getRoot().post(() -> {
                    controller.navigate(R.id.action_global_homeFragment);
                    Snackbar.make(binding.getRoot(), R.string.no_access, Snackbar.LENGTH_LONG).show();
                });
                return;
            }

            applyTabs(role);
            boolean isTab = isTab(destination.getId());
            binding.bottomNav.getRoot().setVisibility(isTab ? View.VISIBLE : View.GONE);
            int[] ids = tabIdsFor();
            for (int i = 0; i < tabViews.length; i++) {
                tabViews[i].setSelected(ids[i] == destination.getId());
            }
            boolean onActionScreen = destination.getId() == tabsForRole().action
                    || destination.getId() == R.id.vendorInventoryFragment;
            binding.bottomNav.fabAction.setSelected(onActionScreen);
            binding.bottomNav.tabAction.setSelected(onActionScreen);
        });
    }

    private void openTopLevel(NavController navController, int destinationId) {
        NavDestination current = navController.getCurrentDestination();
        if (current != null && current.getId() == destinationId) {
            return;
        }
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.homeFragment, false)
                .build();
        navController.navigate(destinationId, null, options);
    }

    private RoleAccess.TabSet tabsForRole() {
        return RoleAccess.tabsFor(SessionManager.getInstance().getRole());
    }

    /** Home, second tab, Messages, More: the screen each tab opens for the current role. */
    private int[] tabIdsFor() {
        return new int[]{R.id.homeFragment, tabsForRole().second, R.id.messagesFragment, R.id.moreFragment};
    }

    /** Gives the second tab and the middle button the icon and name of the current role. */
    private void applyTabs(@Nullable String role) {
        RoleAccess.TabSet tabs = RoleAccess.tabsFor(role);
        binding.bottomNav.ivTabSecond.setImageResource(tabs.secondIcon);
        binding.bottomNav.tvTabSecond.setText(tabs.secondLabel);
        binding.bottomNav.tvTabAction.setText(tabs.actionLabel);
    }

    private boolean isTab(int destinationId) {
        RoleAccess.TabSet tabs = tabsForRole();
        return destinationId == R.id.homeFragment
                || destinationId == tabs.second
                || destinationId == R.id.messagesFragment
                || destinationId == R.id.moreFragment
                || destinationId == tabs.action
                // A vendor's Inventory is opened from the Action (+) screen and keeps the bar.
                || destinationId == R.id.vendorInventoryFragment;
    }

    @Nullable
    private NavController getNavController() {
        Fragment host = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        return host instanceof NavHostFragment ? ((NavHostFragment) host).getNavController() : null;
    }
}
