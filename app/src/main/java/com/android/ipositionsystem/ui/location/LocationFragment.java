package com.android.ipositionsystem.ui.location;

import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.android.ipositionsystem.IpsDbHelper;
import com.android.ipositionsystem.R;
import com.android.ipositionsystem.fmMapUtils.FileUtils;
import com.android.ipositionsystem.fmMapUtils.MapCoordResult;
import com.android.ipositionsystem.knn.KnnFcn;
import com.android.ipositionsystem.knn.PositionInfo;
import com.android.ipositionsystem.ui.MainViewModel;
import com.fengmap.android.FMErrorMsg;
import com.fengmap.android.data.OnFMDownloadProgressListener;
import com.fengmap.android.map.FMMap;
import com.fengmap.android.map.FMMapUpgradeInfo;
import com.fengmap.android.map.FMMapView;
import com.fengmap.android.map.FMViewMode;
import com.fengmap.android.map.event.OnFMMapInitListener;
import com.fengmap.android.map.layer.FMImageLayer;
import com.fengmap.android.map.marker.FMImageMarker;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LocationFragment extends Fragment { //此类相似注释在 CollectFragment.java
    public int minDataRow;
    private LocationViewModel locationViewModel;
    private boolean locating = false;
    private int locatedCount;
    private final PositionInfo locateInfo = new PositionInfo();
    private List<PositionInfo> positionInfoList;

    private SensorManager sensorManager;
    private LocateSensorEventListener sensorEventListener;
    private IpsDbHelper openHelper;

    private EditText selectK;
    private TextView infoText;

    private FMMap fmMap;
    private FMImageMarker marker;

    @SuppressLint("SetTextI18n")
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_location, container, false);

        MainViewModel viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        sensorManager = viewModel.getSensorManager();
        sensorEventListener = new LocateSensorEventListener();
        openHelper = viewModel.getOpenHelper();

        infoText = root.findViewById(R.id.mapInfo);
        selectK = root.findViewById(R.id.select_K);
        selectK.addTextChangedListener(new KChangListener());
        Button locateStart = root.findViewById(R.id.locateStart);
        locateStart.setOnClickListener(new OnLocateStartClickListener());

        locationViewModel = new ViewModelProvider(this).get(LocationViewModel.class);
        locationViewModel.getKValue().observe(getViewLifecycleOwner(), k -> {
            Integer maxK = locationViewModel.getMaxK().getValue();
            if (maxK == null) {
                Toast.makeText(requireActivity(), "无数据", Toast.LENGTH_SHORT).show();
            } else if (k == null) {
                infoText.setText("K:NULL");
            } else if (k > maxK) {
                locationViewModel.getKValue().setValue(maxK);
            } else {
                infoText.setText("K:" + k.toString());
            }
        });
        locationViewModel.getMaxK().observe(getViewLifecycleOwner(), maxK -> selectK.setHint("K值（0~" + maxK + "）"));

//        openMapByPath(root); //加载本地默认地图
        openMapById(root); //加载地图
        refreshK(); //刷新数据列表以及K值

        return root;
    }

    private void openMapById(@NotNull View view) { //注释see --> CollectFragment.java
        FMMapView mapView = view.findViewById(R.id.mapView);
        fmMap = mapView.getFMMap();
        fmMap.setOnFMMapInitListener(new OnFMMapInitListener() {
            @Override
            public void onMapInitSuccess(String path) {
                fmMap.loadThemeById("1384497800603836418");
                fmMap.setFMViewMode(FMViewMode.FMVIEW_MODE_3D);
                fmMap.setZoomLevel(22, true);
            }

            @Override
            public void onMapInitFailure(String path, int errorCode) {
                Log.e("onMapInitFailure", path);
                Log.e("onMapInitFailure", FMErrorMsg.getErrorMsg(errorCode));
            }

            @Override
            public boolean onUpgrade(FMMapUpgradeInfo upgradeInfo) {
                fmMap.upgrade(upgradeInfo, new OnFMDownloadProgressListener() {
                    @SuppressLint("DefaultLocale")
                    @Override
                    public void onProgress(long bytesWritten, long totalSize) {
                        TextView downloadProgress = requireActivity().findViewById(R.id.mapInfo);
                        downloadProgress.setText(String.format("正在下载：%.2fKB/%.2fKB", bytesWritten / 1024.0, totalSize / 1024.0));
                    }

                    @Override
                    public void onCompleted(String mapPath) {
                        TextView downloadProgress = requireActivity().findViewById(R.id.mapInfo);
                        downloadProgress.setText("下载完成");
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
        fmMap.openMapById("1384107483851476993", false);
    }

    private void openMapByPath(@NotNull View view) {
        FMMapView mapView = view.findViewById(R.id.mapView);
        fmMap = mapView.getFMMap();
        fmMap.setOnFMMapInitListener(new OnFMMapInitListener() {
            @Override
            public void onMapInitSuccess(String path) {
                //加载离线主题
                String s = FileUtils.getDefaultThemePath(requireContext());
                fmMap.loadThemeByPath(s);
                fmMap.setFMViewMode(FMViewMode.FMVIEW_MODE_3D); //设置地图3D显示模式
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
        //加载离线数据
        String path = FileUtils.getDefaultMapPath(requireContext());
        fmMap.openMapByPath(path);
    }

    @Override
    public void onDestroyView() {
        if (fmMap != null) {
            fmMap.onDestroy();
        }
        super.onDestroyView();
    }

    private void refreshK() {
        positionInfoList = openHelper.queryPointsMagnetic(this); //从数据库加载数据列表，传入的this会带出 minDataRow
        if (positionInfoList == null) {
            Toast.makeText(requireActivity(), "无数据", Toast.LENGTH_SHORT).show();
            return;
        }
        int maxK = positionInfoList.size(); //K的最大值
        locationViewModel.getMaxK().setValue(maxK);
        locationViewModel.getKValue().setValue(minDataRow); //设置适当的K
        Toast.makeText(requireActivity(), "数据刷新√", Toast.LENGTH_SHORT).show();
    }

    private class OnLocateStartClickListener implements View.OnClickListener { //定位按钮
        @Override
        public void onClick(View view) {
            if (positionInfoList == null) {
                Toast.makeText(requireActivity(), "无数据", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!locating) {
                locatedCount = -1;
                locating = true;
                Sensor orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION); //注册监听，逻辑实现在下面的 location 方法
                sensorManager.registerListener(sensorEventListener, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL);
                Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Toast.makeText(requireActivity(), "正在定位...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private final class KChangListener implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s.toString().equals("")) {
                locationViewModel.getKValue().setValue(null);
            } else {
                locationViewModel.getKValue().setValue(Integer.parseInt(s.toString()));
            }
        }
    }

    private class LocateSensorEventListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            location(sensorEvent);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    }

    private void location(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            locateInfo.setOrientation_x(event.values[SensorManager.DATA_X]);
            locateInfo.setOrientation_y(event.values[SensorManager.DATA_Y]);
            locateInfo.setOrientation_z(event.values[SensorManager.DATA_Z]);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            locateInfo.setMagnetism_x(event.values[SensorManager.DATA_X]);
            locateInfo.setMagnetism_y(event.values[SensorManager.DATA_Y]);
            locateInfo.setMagnetism_z(event.values[SensorManager.DATA_Z]);
            locateInfo.setMagnetism_total(
                    Math.sqrt(Math.pow(locateInfo.getMagnetism_x(), 2)
                            + Math.pow(locateInfo.getMagnetism_y(), 2)
                            + Math.pow(locateInfo.getMagnetism_z(), 2)));

            if (++locatedCount > 0) {
                locationScanStop();
                Integer K = locationViewModel.getKValue().getValue();
                if (K == null) {
                    Toast.makeText(requireActivity(), "K值为空", Toast.LENGTH_SHORT).show();
                } else {
                    KnnFcn knnFcn = new KnnFcn(positionInfoList); //初始化KNN类，positionInfoList为knn的模型
                    int positionId = knnFcn.knn(locateInfo, K); //用knn计算当前位置房间id；locateInfo：当前位置传感器信息，K：K值
                    mapLocationById(positionId); //地图显示当前房间的定位点
                    infoText.setText(String.format("当前房间号：%s", positionId)); //显示结果信息
                    Toast.makeText(requireActivity(), "定位√", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void mapLocationById(int positionId) { //地图显示当前房间的定位点
        MapCoordResult coordResult = openHelper.queryPointCoordById(positionId); //从数据库查询房间id号的坐标信息
        if (coordResult != null) { //放置定位标记
            if (marker == null) {
                FMImageLayer imageLayer = fmMap.getFMLayerProxy().getFMImageLayer(coordResult.getGroupId());
                marker = new FMImageMarker(coordResult.getMapCoord(), R.drawable.ic_marker_blue);
                marker.setFMImageMarkerOffsetMode(FMImageMarker.FMImageMarkerOffsetMode.FMNODE_CUSTOM_HEIGHT);
                marker.setCustomOffsetHeight(1);
                imageLayer.addMarker(marker);
            } else {
                marker.updatePosition(coordResult.getMapCoord());
            }
        }
    }

    private void locationScanStop() {
        locating = false;
        sensorManager.unregisterListener(sensorEventListener);
    }
}