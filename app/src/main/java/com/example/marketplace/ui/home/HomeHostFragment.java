package com.example.marketplace.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.marketplace.R;
import com.example.marketplace.utils.RoleAccess;
import com.example.marketplace.utils.SessionManager;

/**
 * The Home tab. It shows no content itself: it puts the vendor's Home or the operator's Home
 * inside, depending on who is signed in, so the two modules never share a dashboard.
 */
public class HomeHostFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_host, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // After a rotation the system restores the child by itself; add it only the first time.
        if (savedInstanceState == null) {
            Fragment home = RoleAccess.isVendor(SessionManager.getInstance().getRole())
                    ? new VendorHomeFragment() : new HomeFragment();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.homeContainer, home)
                    .commit();
        }
    }
}
