package com.zr.addressselector.model;

/**
 * @author:Hzj
 * @date :2018/11/28/028
 * desc  ：第五级：默认为村
 * record：
 */
public class JdVillage {
    public String id;
    public String parentId;
    public String name;
    //当前层级，省1，市2，区县3，街道4，村5
    public int grade;
}
