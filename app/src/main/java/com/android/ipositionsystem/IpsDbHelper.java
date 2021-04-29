package com.android.ipositionsystem;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import com.android.ipositionsystem.fmMapUtils.MapCoordResult;
import com.android.ipositionsystem.knn.PositionInfo;
import com.android.ipositionsystem.ui.location.LocationFragment;
import com.fengmap.android.map.geometry.FMMapCoord;

import java.util.ArrayList;
import java.util.List;

public class IpsDbHelper extends SQLiteOpenHelper {
    public static String DB_NAME = "ips"; //数据库名称
    private static final int DB_VERSION = 1; //数据库版本

    public IpsDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table MagneticSensor_Data (" +
                "_id integer primary key autoincrement," +
                "position_id integer," +
                "orientation_x real," + "orientation_y real," + "orientation_z real," +
                "magnetism_x real," + "magnetism_y real," + "magnetism_z real," + "magnetism_total real)"); //建立传感器信息表

        db.execSQL("create table FmMapPosition_Data (" +
                "position_id integer primary key," +
                "GroupId integer," +
                "FMMapCoord_x real," + "FMMapCoord_y real)"); //建立坐标信息表
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public void insertPointMagnetic(PositionInfo info) { //传感器信息插入方法
        SQLiteDatabase db = getWritableDatabase();
        ContentValues MsData = new ContentValues();
        MsData.put("position_id", info.getPosition_id());
        MsData.put("orientation_x", info.getOrientation_x());
        MsData.put("orientation_y", info.getOrientation_y());
        MsData.put("orientation_z", info.getOrientation_z());
        MsData.put("magnetism_x", info.getMagnetism_x());
        MsData.put("magnetism_y", info.getMagnetism_y());
        MsData.put("magnetism_z", info.getMagnetism_z());
        MsData.put("magnetism_total", info.getMagnetism_total());
        db.insert("MagneticSensor_Data", null, MsData);
    }

    public List<PositionInfo> queryPointsMagnetic(LocationFragment context) { //传感器信息查询方法，加切豆腐算法
        List<DataCounter> counts = new ArrayList<>();
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.rawQuery("select position_id,count(*) from MagneticSensor_Data group by position_id", null)) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            do {
                DataCounter counter = new DataCounter();
                counter.setPosition_id(cursor.getInt(0));
                counter.setIdCount(cursor.getInt(1));
                counts.add(counter);
            } while (cursor.moveToNext());
        } catch (SQLException e) {
            Toast.makeText(context.getActivity(), "数据库读取错误：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
        int minCount = Integer.MAX_VALUE;
        for (DataCounter counter : counts) {
            if (counter.getIdCount() < minCount) {
                minCount = counter.getIdCount();
            }
        }
        context.minDataRow = minCount;

        List<PositionInfo> positionInfoList = new ArrayList<>();
        for (DataCounter counter : counts) {
            try (SQLiteDatabase db = getReadableDatabase();
                 Cursor cursor = db.rawQuery("select * from MagneticSensor_Data where position_id=? order by _id desc limit ?",
                         new String[]{String.valueOf(counter.getPosition_id()), String.valueOf(minCount)})) {
                cursor.moveToFirst();
                do {
                    PositionInfo positionInfo = new PositionInfo();
                    positionInfo.setPosition_id(cursor.getInt(cursor.getColumnIndex("position_id")));
                    positionInfo.setOrientation_x(cursor.getDouble(cursor.getColumnIndex("orientation_x")));
                    positionInfo.setOrientation_y(cursor.getDouble(cursor.getColumnIndex("orientation_y")));
                    positionInfo.setOrientation_z(cursor.getDouble(cursor.getColumnIndex("orientation_z")));
                    positionInfo.setMagnetism_x(cursor.getDouble(cursor.getColumnIndex("magnetism_x")));
                    positionInfo.setMagnetism_y(cursor.getDouble(cursor.getColumnIndex("magnetism_y")));
                    positionInfo.setMagnetism_z(cursor.getDouble(cursor.getColumnIndex("magnetism_z")));
                    positionInfo.setMagnetism_total(cursor.getDouble(cursor.getColumnIndex("magnetism_total")));
                    positionInfoList.add(positionInfo);
                } while (cursor.moveToNext());
            } catch (SQLException e) {
                Toast.makeText(context.getActivity(), "数据库读取错误：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                return null;
            }
        }
        return positionInfoList;
    }

    public void delPointsMagneticById(int id) { //传感器信息删除方法
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("delete from MagneticSensor_Data where position_id=" + id);
    }

    public void insertPointCoord(int positionId, MapCoordResult result) { //坐标信息插入方法
        SQLiteDatabase db = getWritableDatabase();
        ContentValues MsData = new ContentValues();
        MsData.put("position_id", positionId);
        MsData.put("GroupId", result.getGroupId());
        MsData.put("FMMapCoord_x", result.getMapCoord().x);
        MsData.put("FMMapCoord_y", result.getMapCoord().y);
        db.insert("FmMapPosition_Data", null, MsData);
    }

    public MapCoordResult queryPointCoordById(int id) { //坐标信息查询方法
        MapCoordResult result = null;
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.rawQuery("select * from FmMapPosition_Data where position_id=?", new String[]{String.valueOf(id)})) {
            if (cursor.moveToFirst()) {
                FMMapCoord coord = new FMMapCoord(
                        cursor.getDouble(cursor.getColumnIndex("FMMapCoord_x")),
                        cursor.getDouble(cursor.getColumnIndex("FMMapCoord_y")));
                result = new MapCoordResult(cursor.getInt(cursor.getColumnIndex("GroupId")), coord);
            }
        }
        return result;
    }

    public void delPointCoordById(int id) { //坐标信息删除方法
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("delete from FmMapPosition_Data where position_id=" + id);
    }
}

class DataCounter { //切豆腐算法用的 Counter 类
    private int position_id, idCount;

    public int getIdCount() {
        return idCount;
    }

    public void setIdCount(int idCount) {
        this.idCount = idCount;
    }

    public int getPosition_id() {
        return position_id;
    }

    public void setPosition_id(int position_id) {
        this.position_id = position_id;
    }
}