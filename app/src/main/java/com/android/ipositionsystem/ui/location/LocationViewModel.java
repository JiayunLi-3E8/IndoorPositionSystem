package com.android.ipositionsystem.ui.location;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LocationViewModel extends ViewModel {
    private final MutableLiveData<Integer> kValue;
    private final MutableLiveData<Integer> maxK;

    public LocationViewModel() {
        kValue = new MutableLiveData<>();
        maxK = new MutableLiveData<>();
    }

    public MutableLiveData<Integer> getKValue() {
        return kValue;
    }

    public MutableLiveData<Integer> getMaxK() {
        return maxK;
    }
}