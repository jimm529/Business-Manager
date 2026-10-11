package com.example.marketplace.ui.history;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.data.model.dashboard.PurchaseHistoryResponse;
import com.example.marketplace.data.repository.OrdersRepository;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceLoader;

/** Loads the purchase history once and keeps it through rotation. */
public class PurchaseHistoryViewModel extends ViewModel {

    private final ResourceLoader<PurchaseHistoryResponse> history = new ResourceLoader<>();

    public PurchaseHistoryViewModel() {
        refresh();
    }

    public LiveData<Resource<PurchaseHistoryResponse>> getHistory() {
        return history.asLiveData();
    }

    public void refresh() {
        history.load(OrdersRepository.getInstance().getPurchaseHistory());
    }
}
