package com.android.ipositionsystem.knn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * KNN算法实现类
 */
public class KnnFcn {
    private final List<PositionInfo> infoList;
    private Map<String, Double> map;

    public KnnFcn(List<PositionInfo> infoList) {
        this.infoList = autoNorm(infoList);
    }

    /**
     * 算法核心
     *
     * @param info 待测对象
     * @param k    取前k个对象
     * @return 计算出的位置id
     */
    public int knn(PositionInfo info, int k) throws ArrayIndexOutOfBoundsException {
        PositionInfo normInfo = calNormInfo(info);
        for (PositionInfo info1 : infoList) {
            double distance = calDistance(normInfo, info1);
            info1.setDistance(distance);
        }
        // 对距离进行排序，升序
        Collections.sort(infoList);
        // 从前k个样本中，找到出现频率最高的类别
        int[] position_id = new int[180];

        for (int i = 0; i < k; i++) {
            PositionInfo info1 = infoList.get(i);
            int id = info1.getPosition_id();
            position_id[id]++;
        }

        int positionId = 0, max = position_id[0];
        for (int i = 1; i < position_id.length; i++) {
            if (position_id[i] > max) {
                max = position_id[i];
                positionId = i;
            }
        }
        return positionId;
    }

    /**
     * 计算两个样本点之间的距离
     *
     * @param info  待测数据
     * @param info1 基准数据
     * @return 两个样本点之间的距离
     */
    private double calDistance(PositionInfo info, PositionInfo info1) {
        double sum = Math.pow((info.getOrientation_x() - info1.getOrientation_x()), 2)
                + Math.pow((info.getOrientation_y() - info1.getOrientation_y()), 2)
                + Math.pow((info.getOrientation_z() - info1.getOrientation_z()), 2)
                + Math.pow((info.getMagnetism_x() - info1.getMagnetism_x()), 2)
                + Math.pow((info.getMagnetism_y() - info1.getMagnetism_y()), 2)
                + Math.pow((info.getMagnetism_z() - info1.getMagnetism_z()), 2)
                + Math.pow((info.getMagnetism_total() - info1.getMagnetism_total()), 2);
        return Math.sqrt(sum);
    }

    /**
     * 将数据集归一化处理
     * newValue = (oldValue - min) / (max - min)
     *
     * @param oldInfoList 当前列表
     * @return 归一化后的列表
     */
    private List<PositionInfo> autoNorm(List<PositionInfo> oldInfoList) {
        List<PositionInfo> newInfoList = new ArrayList<>();
        // find max and min
        map = findMaxAndMin(oldInfoList);
        for (PositionInfo info : oldInfoList) {
            newInfoList.add(calNormInfo(info));
        }
        return newInfoList;
    }

    /**
     * 将数据归一化处理
     * newValue = (oldValue - min) / (max - min)
     *
     * @param oldInfo 待处理点信息
     * @return 归一化后的点
     */
    private PositionInfo calNormInfo(PositionInfo oldInfo) {
        PositionInfo positionInfo = new PositionInfo();
        positionInfo.setPosition_id(oldInfo.getPosition_id());
        positionInfo.setOrientation_x(calNewValue(oldInfo.getOrientation_x(), map.get("maxOrientation_x"), map.get("minOrientation_x")));
        positionInfo.setOrientation_y(calNewValue(oldInfo.getOrientation_y(), map.get("maxOrientation_y"), map.get("minOrientation_y")));
        positionInfo.setOrientation_z(calNewValue(oldInfo.getOrientation_z(), map.get("maxOrientation_z"), map.get("minOrientation_z")));
        positionInfo.setMagnetism_x(calNewValue(oldInfo.getMagnetism_x(), map.get("maxMagnetism_x"), map.get("minMagnetism_x")));
        positionInfo.setMagnetism_y(calNewValue(oldInfo.getMagnetism_y(), map.get("maxMagnetism_y"), map.get("minMagnetism_y")));
        positionInfo.setMagnetism_z(calNewValue(oldInfo.getMagnetism_z(), map.get("maxMagnetism_z"), map.get("minMagnetism_z")));
        positionInfo.setMagnetism_total(calNewValue(oldInfo.getMagnetism_total(), map.get("maxMagnetism_total"), map.get("minMagnetism_total")));
        positionInfo.setDistance(oldInfo.getDistance());
        return positionInfo;
    }

    /**
     * @param oldValue 当前值
     * @param maxValue 最大值
     * @param minValue 最小值
     * @return newValue = (oldValue - min) / (max - min)
     */
    private double calNewValue(double oldValue, double maxValue, double minValue) {
        return (oldValue - minValue) / (maxValue - minValue);
    }

    /**
     * find the max and the min
     *
     * @param infoList 数据列表
     * @return 最值字典
     */
    private Map<String, Double> findMaxAndMin(List<PositionInfo> infoList) {
        Map<String, Double> map = new HashMap<>();

        double maxOrientation_x = Double.MIN_VALUE;
        double minOrientation_x = Double.MAX_VALUE;
        double maxOrientation_y = Double.MIN_VALUE;
        double minOrientation_y = Double.MAX_VALUE;
        double maxOrientation_z = Double.MIN_VALUE;
        double minOrientation_z = Double.MAX_VALUE;

        double maxMagnetism_x = Double.MIN_VALUE;
        double minMagnetism_x = Double.MAX_VALUE;
        double maxMagnetism_y = Double.MIN_VALUE;
        double minMagnetism_y = Double.MAX_VALUE;
        double maxMagnetism_z = Double.MIN_VALUE;
        double minMagnetism_z = Double.MAX_VALUE;
        double maxMagnetism_total = Double.MIN_VALUE;
        double minMagnetism_total = Double.MAX_VALUE;

        for (PositionInfo info : infoList) {
            if (info.getOrientation_x() > maxOrientation_x) {
                maxOrientation_x = info.getOrientation_x();
            }
            if (info.getOrientation_x() < minOrientation_x) {
                minOrientation_x = info.getOrientation_x();
            }
            if (info.getOrientation_y() > maxOrientation_y) {
                maxOrientation_y = info.getOrientation_y();
            }
            if (info.getOrientation_y() < minOrientation_y) {
                minOrientation_y = info.getOrientation_y();
            }
            if (info.getOrientation_z() > maxOrientation_z) {
                maxOrientation_z = info.getOrientation_z();
            }
            if (info.getOrientation_z() < minOrientation_z) {
                minOrientation_z = info.getOrientation_z();
            }
            if (info.getMagnetism_x() > maxMagnetism_x) {
                maxMagnetism_x = info.getMagnetism_x();
            }
            if (info.getMagnetism_x() < minMagnetism_x) {
                minMagnetism_x = info.getMagnetism_x();
            }
            if (info.getMagnetism_y() > maxMagnetism_y) {
                maxMagnetism_y = info.getMagnetism_y();
            }
            if (info.getMagnetism_y() < minMagnetism_y) {
                minMagnetism_y = info.getMagnetism_y();
            }
            if (info.getMagnetism_z() > maxMagnetism_z) {
                maxMagnetism_z = info.getMagnetism_z();
            }
            if (info.getMagnetism_z() < minMagnetism_z) {
                minMagnetism_z = info.getMagnetism_z();
            }
            if (info.getMagnetism_total() > maxMagnetism_total) {
                maxMagnetism_total = info.getMagnetism_total();
            }
            if (info.getMagnetism_total() < minMagnetism_total) {
                minMagnetism_total = info.getMagnetism_total();
            }
        }

        map.put("maxOrientation_x", maxOrientation_x);
        map.put("minOrientation_x", minOrientation_x);
        map.put("maxOrientation_y", maxOrientation_y);
        map.put("minOrientation_y", minOrientation_y);
        map.put("maxOrientation_z", maxOrientation_z);
        map.put("minOrientation_z", minOrientation_z);
        map.put("maxMagnetism_x", maxMagnetism_x);
        map.put("minMagnetism_x", minMagnetism_x);
        map.put("maxMagnetism_y", maxMagnetism_y);
        map.put("minMagnetism_y", minMagnetism_y);
        map.put("maxMagnetism_z", maxMagnetism_z);
        map.put("minMagnetism_z", minMagnetism_z);
        map.put("maxMagnetism_total", maxMagnetism_total);
        map.put("minMagnetism_total", minMagnetism_total);

        return map;
    }
}