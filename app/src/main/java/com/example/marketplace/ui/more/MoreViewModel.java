package com.example.marketplace.ui.more;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.data.model.auth.MeResponse;
import com.example.marketplace.data.repository.AuthRepository;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceLoader;

/** Loads the signed-in person's profile for the card at the top of the More screen. */
public class MoreViewModel extends ViewModel {

    private final ResourceLoader<MeResponse> profile = new ResourceLoader<>();

    public MoreViewModel() {
        refresh();
    }

    public LiveData<Resource<MeResponse>> getProfile() {
        return profile.asLiveData();
    }

    public void refresh() {
        profile.load(AuthRepository.getInstance().getMe());
    }
}
