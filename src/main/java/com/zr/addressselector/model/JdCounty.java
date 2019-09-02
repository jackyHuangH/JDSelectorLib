package com.zr.addressselector.model;

/**
 * 第三级：默认为区
 * @author HZJ
 */
public class JdCounty {
    public String id;
    public String parentId;
    public String name;
    //当前层级，省1，市2，区县3，街道4，村5
    public int grade;
}