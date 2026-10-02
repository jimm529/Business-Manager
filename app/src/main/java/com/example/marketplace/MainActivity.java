package com.example.marketplace;

import android.os.Bundle;

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

        // Tapping a tab opens it on top of Home, so Back always returns to Home and then leaves the app.
        binding.bottomNav.setOnItemSelectedListener(item -> {
            NavDestination current = navController.getCurrentDestination();
            if (current != null && current.getId() == item.getItemId()) {
                return true;
            }
            NavOptions options = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.homeFragment, false)
                    .build();
            navController.navigate(item.getItemId(), null, options);
            return true;
        });

        // The tab bar shows only on the four main tabs, with the current tab highlighted.
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            boolean isTab = isTab(destination.getId());
            binding.bottomNav.setVisibility(isTab ? android.view.View.VISIBLE : android.view.View.GONE);
            if (isTab && binding.bottomNav.getMenu().findItem(destination.getId()) != null) {
                binding.bottomNav.getMenu().findItem(destination.getId()).setChecked(true);
            }
        });
    }

    private static boolean isTab(int destinationId) {
        return destinationId == R.id.homeFragment
                || destinationId == R.id.ordersFragment
                || destinationId == R.id.messagesFragment
                || destinationId == R.id.profileFragment;
    }

    @Nullable
    private NavController getNavController() {
        Fragment host = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        return host instanceof NavHostFragment ? ((NavHostFragment) host).getNavController() : null;
    }
}
