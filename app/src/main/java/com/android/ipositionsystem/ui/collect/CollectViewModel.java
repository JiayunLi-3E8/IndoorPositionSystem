package com.android.ipositionsystem.ui.collect;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CollectViewModel extends ViewModel {
    private final MutableLiveData<String> dataDisplay;
    private final MutableLiveData<Integer> scanProgress_max;
    private final MutableLiveData<Integer> scanProgress;

    public CollectViewModel() {
        dataDisplay = new MutableLiveData<>();
        scanProgress_max = new MutableLiveData<>();
        scanProgress = new MutableLiveData<>();
    }

    public MutableLiveData<String> getDataDisplay() {
        return dataDisplay;
    }

    public MutableLiveData<Integer> getScanProgress_max() {
        return scanProgress_max;
    }

    public MutableLiveData<Integer> getScanProgress() {
        return scanProgress;
    }
}