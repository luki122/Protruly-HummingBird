package com.protruly.music.model;

/**
 * Created by hujianwei on 17-9-4.
 */

public class HBDeleteItem {
    private long[] id;
    private String[] path;
    public long[] getId() {
        return id;
    }
    public void setId(long[] id) {
        this.id = id;
    }
    public String[] getPath() {
        return path;
    }
    public void setPath(String[] path) {
        this.path = path;
    }
}
