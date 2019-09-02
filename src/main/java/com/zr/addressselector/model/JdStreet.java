package com.zr.addressselector.model;

/**
 * 第四级：默认为街道
 * @author HZJ
 */
public class JdStreet {
    public String id;
    public String parentId;
    public String name;
    //当前层级，省1，市2，区县3，街道4，村5
    public int grade;
}