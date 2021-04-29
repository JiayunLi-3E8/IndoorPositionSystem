package com.android.ipositionsystem.knn;

import androidx.annotation.NonNull;

/**
 * 点数据信息类
 * 实现比较借口，为了后续KNN算法排序
 */
public class PositionInfo implements Comparable<PositionInfo>, Cloneable {
    private int position_id;
    private double orientation_x, orientation_y, orientation_z;
    private double magnetism_x, magnetism_y, magnetism_z, magnetism_total;
    private double distance;

    public int getPosition_id() {
        return position_id;
    }

    public void setPosition_id(int position_id) {
        this.position_id = position_id;
    }

    public double getOrientation_x() {
        return orientation_x;
    }

    public void setOrientation_x(double orientation_x) {
        this.orientation_x = orientation_x;
    }

    public double getOrientation_y() {
        return orientation_y;
    }

    public void setOrientation_y(double orientation_y) {
        this.orientation_y = orientation_y;
    }

    public double getOrientation_z() {
        return orientation_z;
    }

    public void setOrientation_z(double orientation_z) {
        this.orientation_z = orientation_z;
    }

    public double getMagnetism_x() {
        return magnetism_x;
    }

    public void setMagnetism_x(double magnetism_x) {
        this.magnetism_x = magnetism_x;
    }

    public double getMagnetism_y() {
        return magnetism_y;
    }

    public void setMagnetism_y(double magnetism_y) {
        this.magnetism_y = magnetism_y;
    }

    public double getMagnetism_z() {
        return magnetism_z;
    }

    public void setMagnetism_z(double magnetism_z) {
        this.magnetism_z = magnetism_z;
    }

    public double getMagnetism_total() {
        return magnetism_total;
    }

    public void setMagnetism_total(double magnetism_total) {
        this.magnetism_total = magnetism_total;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     * 这里进行升序排序
     */
    @Override
    public int compareTo(PositionInfo positionInfo) {
        if (this.distance < positionInfo.getDistance()) {
            return -1;
        } else if (this.distance > positionInfo.getDistance()) {
            return 1;
        }
        return 0;
    }

    /**
     * 实现克隆接口
     *
     * @return 克隆点
     */
    @NonNull
    @Override
    public PositionInfo clone() {
        PositionInfo newPositionInfo = new PositionInfo();
        newPositionInfo.setPosition_id(this.getPosition_id());
        newPositionInfo.setOrientation_x(this.getOrientation_x());
        newPositionInfo.setOrientation_y(this.getOrientation_y());
        newPositionInfo.setOrientation_z(this.getOrientation_z());
        newPositionInfo.setMagnetism_x(this.getMagnetism_x());
        newPositionInfo.setMagnetism_y(this.getMagnetism_y());
        newPositionInfo.setMagnetism_z(this.getMagnetism_z());
        newPositionInfo.setMagnetism_total(this.getMagnetism_total());
        newPositionInfo.setDistance(this.getDistance());
        return newPositionInfo;
    }
}