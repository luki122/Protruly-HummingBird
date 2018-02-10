package com.hb.thememanager.http.request;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;

/**
 * 网络请求请求体
 */

public class  RequestBody {

    /**
     *
     */
    public int pageSize;
    /**
     * 请求的资源类型，主题，壁纸，字体
     */
    public String id;
    public int pageNum;

    /**
     * font | theme ?
     */
    public int type; 

    @JSONField(serialize = false)
    private String[] mMyProperties;


    public SimplePropertyPreFilter createPropertyFilter(){

        return new SimplePropertyPreFilter(this.getClass(), mMyProperties);
    }

    /**
     * 配置需要的属性，通过过滤器把需要的留下，不需要的剔除
     * @param neededProperties 在网络请求中需要的属性集合
     */
    public void setupAvaliableProperties(String... neededProperties){
        mMyProperties = neededProperties;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "RequestBody{" +
                "pageSize=" + pageSize +
                ", id='" + id + '\'' +
                ", pageNum=" + pageNum +
                ", type=" + type +
                '}';
    }
}
