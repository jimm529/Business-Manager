package com.example.marketplace;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.marketplace.databinding.ActivityMainBinding;
import com.example.marketplace.utils.SessionManager;
import com.google.android.material.snackbar.Snackbar;

/** The single activity. It only hosts the navigation graph; screens are fragments. */
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

    private NavController getNavController() {
        Fragment host = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        return host instanceof NavHostFragment ? ((NavHostFragment) host).getNavController() : null;
    }
}
