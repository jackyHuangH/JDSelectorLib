package com.zr.addressselector.model;

/**
 * 第一级:默认为省，不一定非得是省，按实际情况
 */
public class JdProvince {

    public String id;
    public String name;

    @Override
    public String toString() {
        return "JdProvince{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}