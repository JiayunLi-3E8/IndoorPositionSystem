package com.android.ipositionsystem.fmMapUtils;

import com.fengmap.android.map.FMPickMapCoordResult;
import com.fengmap.android.map.geometry.FMMapCoord;

/**
 * 蜂鸟坐标信息类，继承 FMPickMapCoordResult ，因为父类构造函数权限无法直接访问
 */
public class MapCoordResult extends FMPickMapCoordResult {
    public MapCoordResult(int groupId, FMMapCoord mapCoord) {
        super(groupId, mapCoord);
    }
}
