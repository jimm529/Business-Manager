package com.example.marketplace.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.data.model.dashboard.VendorOverviewResponse;
import com.example.marketplace.data.repository.DashboardRepository;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceLoader;

/** Holds the vendor dashboard so it survives rotation, and reloads it on pull-to-refresh. */
public class VendorHomeViewModel extends ViewModel {

    private final DashboardRepository repository = DashboardRepository.getInstance();
    private final ResourceLoader<VendorOverviewResponse> overview = new ResourceLoader<>();

    public VendorHomeViewModel() {
        refresh();
    }

    public LiveData<Resource<VendorOverviewResponse>> getOverview() {
        return overview.asLiveData();
    }

    public void refresh() {
        overview.load(repository.getVendorOverview());
    }
}
