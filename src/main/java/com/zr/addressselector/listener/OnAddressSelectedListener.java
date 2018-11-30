package com.zr.addressselector.listener;


import com.zr.addressselector.model.JdCity;
import com.zr.addressselector.model.JdCounty;
import com.zr.addressselector.model.JdProvince;
import com.zr.addressselector.model.JdStreet;
import com.zr.addressselector.model.JdVillage;

public interface OnAddressSelectedListener {
    // 获取地址完成回调
    void onAddressSelected(JdProvince province, JdCity city, JdCounty county, JdStreet street, JdVillage village);

    // 选取省份完成回调
    void onProvinceSelected(JdProvince province);

    // 选取城市完成回调
    void onCitySelected(JdCity city);

    // 选取区/县完成回调
    void onCountySelected(JdCounty county);

    // 选取街道/乡镇完成回调
    void onStreetSelected(JdStreet street);

}
