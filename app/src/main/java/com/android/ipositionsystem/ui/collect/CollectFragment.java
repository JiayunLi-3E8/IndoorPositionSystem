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

        openMapByPath(root);
//        openMapById(root);
        fmMap.setOnFMMapClickListener(new OnFmMapClickListener());

        return root;
    }

//    private void openMapById(@NotNull View view) { //加载地图资源
//        FMMapView mapView = view.findViewById(R.id.mapView);
//        fmMap = mapView.getFMMap();
//        fmMap.setOnFMMapInitListener(new OnFMMapInitListener() { //地图初始化监听
//            @Override
//            public void onMapInitSuccess(String path) { //初始化成功回调
//                fmMap.loadThemeById("1384497800603836418"); //加载主题
//                fmMap.setFMViewMode(FMViewMode.FMVIEW_MODE_3D); //设置地图3D显示模式
//                fmMap.setZoomLevel(22, true); //设置显示级别（大小）
//            }
//
//            @Override
//            public void onMapInitFailure(String path, int errorCode) { //初始化失败回调
//                Log.e("onMapInitFailure", path);
//                Log.e("onMapInitFailure", FMErrorMsg.getErrorMsg(errorCode));
//            }
//
//            @Override
//            public boolean onUpgrade(FMMapUpgradeInfo upgradeInfo) { //手动升级时回调
//                fmMap.upgrade(upgradeInfo, new OnFMDownloadProgressListener() {
//                    @SuppressLint("DefaultLocale")
//                    @Override
//                    public void onProgress(long bytesWritten, long totalSize) {
//                        TextView downloadProgress = requireActivity().findViewById(R.id.dataDisplay);
//                        downloadProgress.setText(String.format("正在下载：%.2fKB/%.2fKB", bytesWritten / 1024.0, totalSize / 1024.0));
//                    }
//
//                    @Override
//                    public void onCompleted(String mapPath) {
//                        TextView downloadProgress = requireActivity().findViewById(R.id.dataDisplay);
//                        downloadProgress.setText("下载完成");
//                    }
//
//                    @Override
//                    public void onFailure(String mapPath, int errorCode) {
//                        Log.e("MapUpgradeFailure", mapPath);
//                        Log.e("MapUpgradeFailure", FMErrorMsg.getErrorMsg(errorCode));
//                    }
//                });
//                return true;
//            }
//        });
//        fmMap.openMapById("1384107483851476993", false); //加载地图，不自动升级
//    }

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
            fmMap.onDestroy(); //地图对象销毁
        }
        super.onDestroyView();
    }

    private class OnFmMapClickListener implements OnFMMapClickListener { //地图点击事件
        @Override
        public void onMapClick(float x, float y) {
            FMPickMapCoordResult result = fmMap.pickMapCoord(x, y); //从屏幕坐标拾取地图坐标
            if (result != null) {
                if (marker == null) {
                    FMImageLayer imageLayer = fmMap.getFMLayerProxy().getFMImageLayer(result.getGroupId()); //获取图层管理
                    marker = new FMImageMarker(result.getMapCoord(), R.drawable.ic_marker_blue); //新建地图标记
                    marker.setFMImageMarkerOffsetMode(FMImageMarker.FMImageMarkerOffsetMode.FMNODE_CUSTOM_HEIGHT); //设置显示高度为自定义模式
                    marker.setCustomOffsetHeight(1); //自定义显示高度
                    imageLayer.addMarker(marker); //添加标记到图层
                } else {
                    marker.updatePosition(result.getMapCoord()); //更新标记坐标
                }
            }
        }
    }

    private class OnScanStartAddClickListener implements View.OnClickListener { //追加采集按钮
        @Override
        public void onClick(View view) {
            collectStart(false);
        }
    }

    private class OnScanStartCoverClickListener implements View.OnClickListener { //覆盖采集按钮
        @Override
        public void onClick(View view) {
            collectStart(true);
        }
    }

    @SuppressWarnings("deprecation")
    private void collectStart(boolean Cover) { //采集初始化；Cover：true覆盖，false追加
        if (!collecting) {
            if (marker == null) { //信息完整判断
                Toast.makeText(getActivity(), "请在地图上标点", Toast.LENGTH_SHORT).show();
                return;
            }
            String id = position_id.getEditableText().toString();
            String count = scanCount.getEditableText().toString();
            if (id.equals("") || count.equals("")) {
                Toast.makeText(getActivity(), "信息未填写完整", Toast.LENGTH_SHORT).show();
                return;
            }
            collectInfo.setPosition_id(Integer.parseInt(id)); //将房间id放入缓存
            if (Cover) { //如果是覆盖模式就删掉原来的数据
                openHelper.delPointsMagneticById(collectInfo.getPosition_id());
            }
            collectMaxCount = Integer.parseInt(count); //初始化采集次数
            collectViewModel.getScanProgress_max().setValue(collectMaxCount); //设置进度条最大值
            collectedCount = -1; //初始化进度
            collecting = true;
            Sensor orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION); //注册传感器监听器
            sensorManager.registerListener(sensorEventListener, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL);
            Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(getActivity(), "正在采集...", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressWarnings("deprecation")
    private class CollectSensorEventListener implements SensorEventListener { //传感器监听器
        @SuppressLint("DefaultLocale")
        @Override
        public void onSensorChanged(SensorEvent event) { //所注册的传感器值改变时触发
            if (collectedCount < collectMaxCount) { //如果没采集完设置的次数
                if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) { //根据传感器类型分段送入缓存
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

                    collectViewModel.getScanProgress().setValue(++collectedCount); //更新进度条进度
                    collectViewModel.getDataDisplay().setValue(
                            String.format("采集进度：%d/%d  当前磁总量：%.2f",
                                    collectedCount,
                                    collectMaxCount,
                                    collectInfo.getMagnetism_total()));
                    if (collectedCount > 0) { //丢弃第一条不完整数据，从1开始录入数据库
                        if (collectedCount == 1) { //坐标信息只在第一次录入时更新
                            openHelper.delPointCoordById(collectInfo.getPosition_id()); //删除原坐标信息（如果有）
                            openHelper.insertPointCoord(collectInfo.getPosition_id(), new MapCoordResult(marker.getGroupId(), marker.getPosition())); //数据库录入坐标信息
                        }
                        openHelper.insertPointMagnetic(collectInfo); //数据库录入传感器信息
                    }
                }
            } else {
                collectStop(); //采完就停止
                collectViewModel.getDataDisplay().setValue("完成");
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    }

    private class OnScanStopClickListener implements View.OnClickListener { //停止按钮
        @Override
        public void onClick(View view) {
            collectStop();
            Toast.makeText(getActivity(), "已停止", Toast.LENGTH_SHORT).show();
        }
    }

    private void collectStop() { //停止采集
        sensorManager.unregisterListener(sensorEventListener); //注销监听器
        collecting = false;
    }
}