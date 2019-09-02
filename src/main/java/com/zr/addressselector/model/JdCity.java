package com.zr.addressselector.model;

/**
 * 第二级：默认为市
 * @author HZJ
 */
public class JdCity {
    public String id;
    public String parentId;
    public String name;
    //当前层级，省1，市2，区县3，街道4，村5
    public int grade;
}