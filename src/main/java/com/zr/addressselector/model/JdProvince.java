package com.zr.addressselector.model;

/**
 * 第一级:默认为省，不一定非得是省，按实际情况
 * @author HZJ
 */
public class JdProvince {

    public String parentCode;
    public String id;
    public String name;
    //当前层级，省1，市2，区县3，街道4，村5
    public int grade;

    @Override
    public String toString() {
        return "JdProvince{" +
                "parentCode='" + parentCode + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", grade=" + grade +
                '}';
    }
}