package com.example.marketplace.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;

import com.example.marketplace.R;
import com.example.marketplace.databinding.FragmentComingSoonBinding;

/** Placeholder for the Orders, Messages and Profile tabs until those screens are built. */
public class ComingSoonFragment extends Fragment {

    private FragmentComingSoonBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentComingSoonBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // The tab's name comes from the label set on its destination in nav_graph.xml.
        NavDestination destination = NavHostFragment.findNavController(this).getCurrentDestination();
        CharSequence label = destination == null ? null : destination.getLabel();
        binding.tvComingSoon.setText(label == null
                ? getString(R.string.coming_soon)
                : getString(R.string.coming_soon_screen, label));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
