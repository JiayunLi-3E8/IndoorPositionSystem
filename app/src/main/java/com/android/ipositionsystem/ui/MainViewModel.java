package com.android.ipositionsystem.ui;

import android.hardware.SensorManager;

import androidx.lifecycle.ViewModel;

import com.android.ipositionsystem.IpsDbHelper;

public class MainViewModel extends ViewModel {
    private SensorManager sensorManager;
    private IpsDbHelper openHelper;

    public SensorManager getSensorManager() {
        return sensorManager;
    }

    public void setSensorManager(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
    }

    public IpsDbHelper getOpenHelper() {
        return openHelper;
    }

    public void setOpenHelper(IpsDbHelper openHelper) {
        this.openHelper = openHelper;
    }
}
