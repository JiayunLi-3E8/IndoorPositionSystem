package com.android.ipositionsystem.ui.collect;

import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.android.ipositionsystem.IpsDbHelper;
import com.android.ipositionsystem.R;
import com.android.ipositionsystem.fmMapUtils.FileUtils;
import com.android.ipositionsystem.fmMapUtils.MapCoordResult;
import com.android.ipositionsystem.knn.PositionInfo;
import com.android.ipositionsystem.ui.MainViewModel;
import com.fengmap.android.FMErrorMsg;
import com.fengmap.android.data.OnFMDownloadProgressListener;
import com.fengmap.android.map.FMMap;
import com.fengmap.android.map.FMMapUpgradeInfo;
import com.fengmap.android.map.FMMapView;
import com.fengmap.android.map.FMPickMapCoordResult;
import com.fengmap.android.map.FMViewMode;
import com.fengmap.android.map.event.OnFMMapClickListener;
import com.fengmap.android.map.event.OnFMMapInitListener;
import com.fengmap.android.map.layer.FMImageLayer;
import com.fengmap.android.map.marker.FMImageMarker;

import org.jetbrains.annotations.NotNull;

public class CollectFragment extends Fragment {
    private CollectViewModel collectViewModel;

    private boolean collecting = false;
    private int collectMaxCount, collectedCount;
    private final PositionInfo collectInfo = new PositionInfo();

    private SensorManager sensorManager;
    private CollectSensorEventListener sensorEventListener;
    private IpsDbHelper openHelper;

    private EditText position_id;
    private EditText scanCount;

    private FMMap fmMap;
    FMImageMarker marker = null;

    public CollectFragment() {
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_collect, container, false);

        MainViewModel viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        sensorManager = viewModel.getSensorManager();
        sensorEventListener = new CollectSensorEventListener();
        openHelper = viewModel.getOpenHelper();

        collectViewModel = new ViewModelProvider(this).get(CollectViewModel.class);
        TextView dataDisplay = root.findViewById(R.id.dataDisplay);
        collectViewModel.getDataDisplay().observe(getViewLifecycleOwner(), dataDisplay::setText);
        ProgressBar scanProgress = root.findViewById(R.id.scanProgress);
        collectViewModel.getScanProgress_max().observe(getViewLifecycleOwner(), scanProgress::setMax);
        collectViewModel.getScanProgress().observe(getViewLifecycleOwner(), scanProgress::setProgress);

        Button scanStart_Add = root.findViewById(R.id.scanStart_Add);
        scanStart_Add.setOnClickListener(new OnScanStartAddClickListener());
        Button scanStart_Cover = root.findViewById(R.id.scanStart_Cover);
        scanStart_Cover.setOnClickListener(new OnScanStartCoverClickListener());
        Button scanStop = root.findViewById(R.id.scanStop);
        scanStop.setOnClickListener(new OnScanStopClickListener());

        position_id = root.findViewById(R.id.position_id);
        scanCount = root.findViewById(R.id.scanCount);

//        openMapByPath(root);
        openMapById(root);
        fmMap.setOnFMMapClickListener(new OnFmMapClickListener());

        return root;
    }

    private void openMapById(@NotNull View view) { //??????????????????
        FMMapView mapView = view.findViewById(R.id.mapView);
        fmMap = mapView.getFMMap();
        fmMap.setOnFMMapInitListener(new OnFMMapInitListener() { //?????????????????????
            @Override
            public void onMapInitSuccess(String path) { //?????????????????????
                fmMap.loadThemeById("1384497800603836418"); //????????????
                fmMap.setFMViewMode(FMViewMode.FMVIEW_MODE_3D); //????????????3D????????????
                fmMap.setZoomLevel(22, true); //??????????????????????????????
            }

            @Override
            public void onMapInitFailure(String path, int errorCode) { //?????????????????????
                Log.e("onMapInitFailure", path);
                Log.e("onMapInitFailure", FMErrorMsg.getErrorMsg(errorCode));
            }

            @Override
            public boolean onUpgrade(FMMapUpgradeInfo upgradeInfo) { //?????????????????????
                fmMap.upgrade(upgradeInfo, new OnFMDownloadProgressListener() {
                    @SuppressLint("DefaultLocale")
                    @Override
                    public void onProgress(long bytesWritten, long totalSize) {
                        TextView downloadProgress = requireActivity().findViewById(R.id.dataDisplay);
                        downloadProgress.setText(String.format("???????????????%.2fKB/%.2fKB", bytesWritten / 1024.0, totalSize / 1024.0));
                    }

                    @Override
                    public void onCompleted(String mapPath) {
                        TextView downloadProgress = requireActivity().findViewById(R.id.dataDisplay);
                        downloadProgress.setText("????????????");
                    }

                    @Override
                    public void onFailure(String mapPath, int errorCode) {
                        Log.e("MapUpgradeFailure", mapPath);
                        Log.e("MapUpgradeFailure", FMErrorMsg.getErrorMsg(errorCode));
                    }
                });
                return true;
            }
        });
        fmMap.openMapById("1384107483851476993", false); //??????????????????????????????
    }

    private void openMapByPath(@NotNull View view) {
        FMMapView mapView = view.findViewById(R.id.mapView);
        fmMap = mapView.getFMMap();
        fmMap.setOnFMMapInitListener(new OnFMMapInitListener() {
            @Override
            public void onMapInitSuccess(String path) {
                //??????????????????
                String s = FileUtils.getDefaultThemePath(requireContext());
                fmMap.loadThemeByPath(s);
                fmMap.setFMViewMode(FMViewMode.FMVIEW_MODE_3D); //????????????3D????????????
            }

            @Override
            public void onMapInitFailure(String path, int errorCode) {
                Log.e("onMapInitFailure", path);
                Log.e("onMapInitFailure", FMErrorMsg.getErrorMsg(errorCode));
            }

            @Override
            public boolean onUpgrade(FMMapUpgradeInfo upgradeInfo) {
                return false;
            }
        });
        //??????????????????
        String path = FileUtils.getDefaultMapPath(requireContext());
        fmMap.openMapByPath(path);
    }

    @Override
    public void onDestroyView() {
        if (fmMap != null) {
            fmMap.onDestroy(); //??????????????????
        }
        super.onDestroyView();
    }

    private class OnFmMapClickListener implements OnFMMapClickListener { //??????????????????
        @Override
        public void onMapClick(float x, float y) {
            FMPickMapCoordResult result = fmMap.pickMapCoord(x, y); //?????????????????????????????????
            if (result != null) {
                if (marker == null) {
                    FMImageLayer imageLayer = fmMap.getFMLayerProxy().getFMImageLayer(result.getGroupId()); //??????????????????
                    marker = new FMImageMarker(result.getMapCoord(), R.drawable.ic_marker_blue); //??????????????????
                    marker.setFMImageMarkerOffsetMode(FMImageMarker.FMImageMarkerOffsetMode.FMNODE_CUSTOM_HEIGHT); //????????????????????????????????????
                    marker.setCustomOffsetHeight(1); //?????????????????????
                    imageLayer.addMarker(marker); //?????????????????????
                } else {
                    marker.updatePosition(result.getMapCoord()); //??????????????????
                }
            }
        }
    }

    private class OnScanStartAddClickListener implements View.OnClickListener { //??????????????????
        @Override
        public void onClick(View view) {
            collectStart(false);
        }
    }

    private class OnScanStartCoverClickListener implements View.OnClickListener { //??????????????????
        @Override
        public void onClick(View view) {
            collectStart(true);
        }
    }

    @SuppressWarnings("deprecation")
    private void collectStart(boolean Cover) { //??????????????????Cover???true?????????false??????
        if (!collecting) {
            if (marker == null) { //??????????????????
                Toast.makeText(getActivity(), "?????????????????????", Toast.LENGTH_SHORT).show();
                return;
            }
            String id = position_id.getEditableText().toString();
            String count = scanCount.getEditableText().toString();
            if (id.equals("") || count.equals("")) {
                Toast.makeText(getActivity(), "?????????????????????", Toast.LENGTH_SHORT).show();
                return;
            }
            collectInfo.setPosition_id(Integer.parseInt(id)); //?????????id????????????
            if (Cover) { //?????????????????????????????????????????????
                openHelper.delPointsMagneticById(collectInfo.getPosition_id());
            }
            collectMaxCount = Integer.parseInt(count); //?????????????????????
            collectViewModel.getScanProgress_max().setValue(collectMaxCount); //????????????????????????
            collectedCount = -1; //???????????????
            collecting = true;
            Sensor orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION); //????????????????????????
            sensorManager.registerListener(sensorEventListener, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL);
            Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(getActivity(), "????????????...", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressWarnings("deprecation")
    private class CollectSensorEventListener implements SensorEventListener { //??????????????????
        @SuppressLint("DefaultLocale")
        @Override
        public void onSensorChanged(SensorEvent event) { //???????????????????????????????????????
            if (collectedCount < collectMaxCount) { //?????????????????????????????????
                if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) { //???????????????????????????????????????
                    collectInfo.setOrientation_x(event.values[SensorManager.DATA_X]);
                    collectInfo.setOrientation_y(event.values[SensorManager.DATA_Y]);
                    collectInfo.setOrientation_z(event.values[SensorManager.DATA_Z]);
                } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    collectInfo.setMagnetism_x(event.values[SensorManager.DATA_X]);
                    collectInfo.setMagnetism_y(event.values[SensorManager.DATA_Y]);
                    collectInfo.setMagnetism_z(event.values[SensorManager.DATA_Z]);
                    collectInfo.setMagnetism_total(
                            Math.sqrt(Math.pow(collectInfo.getMagnetism_x(), 2)
                                    + Math.pow(collectInfo.getMagnetism_y(), 2)
                                    + Math.pow(collectInfo.getMagnetism_z(), 2)));

                    collectViewModel.getScanProgress().setValue(++collectedCount); //?????????????????????
                    collectViewModel.getDataDisplay().setValue(
                            String.format("???????????????%d/%d  ??????????????????%.2f",
                                    collectedCount,
                                    collectMaxCount,
                                    collectInfo.getMagnetism_total()));
                    if (collectedCount > 0) { //????????????????????????????????????1?????????????????????
                        if (collectedCount == 1) { //??????????????????????????????????????????
                            openHelper.delPointCoordById(collectInfo.getPosition_id()); //????????????????????????????????????
                            openHelper.insertPointCoord(collectInfo.getPosition_id(), new MapCoordResult(marker.getGroupId(), marker.getPosition())); //???????????????????????????
                        }
                        openHelper.insertPointMagnetic(collectInfo); //??????????????????????????????
                    }
                }
            } else {
                collectStop(); //???????????????
                collectViewModel.getDataDisplay().setValue("??????");
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    }

    private class OnScanStopClickListener implements View.OnClickListener { //????????????
        @Override
        public void onClick(View view) {
            collectStop();
            Toast.makeText(getActivity(), "?????????", Toast.LENGTH_SHORT).show();
        }
    }

    private void collectStop() { //????????????
        sensorManager.unregisterListener(sensorEventListener); //???????????????
        collecting = false;
    }
}