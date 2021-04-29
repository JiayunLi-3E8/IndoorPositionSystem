package com.android.ipositionsystem.ui.location;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LocationViewModel extends ViewModel {
    private final MutableLiveData<String> kValue;

    public LocationViewModel() {
        kValue = new MutableLiveData<>();
    }

    public MutableLiveData<String> getKValue() {
        return kValue;
    }
}