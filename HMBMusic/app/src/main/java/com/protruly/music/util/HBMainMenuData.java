package com.protruly.music.util;

import java.io.Serializable;

/**
 * Created by hujianwei on 17-9-4.
 */

public class HBMainMenuData implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private int resouceId;

    // -1 不显示， 0表示 n首， 1表示 n首歌曲， 2表示 n个歌手，3表示
    private int songSizeType;

    // n个文件夹,4表示 我喜欢的歌曲，5表示 最近添加的歌曲 6表示 收藏歌单
    private boolean isShowArrow;
    
    // arrow
    private int playlistId = -1;

    public HBMainMenuData() {

    }

    public HBMainMenuData(String name, int id, int is) {
        this.name = name;
        this.resouceId = id;
        this.songSizeType = is;
        this.isShowArrow = true;
    }

    public HBMainMenuData(String name, int id, int is, int playlistid) {
        this.name = name;
        this.resouceId = id;
        this.songSizeType = is;
        this.isShowArrow = true;
        this.playlistId = playlistid;
    }

    public HBMainMenuData(String name, int id, int is, boolean isArrow) {
        this.name = name;
        this.resouceId = id;
        this.songSizeType = is;
        this.isShowArrow = isArrow;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getResouceId() {
        return resouceId;
    }

    public void setResouceId(int resouceId) {
        this.resouceId = resouceId;
    }

    public int getSongSizeType() {
        return songSizeType;
    }

    public void setSongSizeType(int songSizeType) {
        this.songSizeType = songSizeType;
    }

    public boolean isShowArrow() {
        return isShowArrow;
    }

    public void setShowArrow(boolean isShowSize) {
        this.isShowArrow = isShowSize;
    }


    public int getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(int playlistId) {
        this.playlistId = playlistId;
    }

    @Override
    public String toString() {
        return "HBMainMenuData [name=" + name + ", resouceId=" + resouceId
                + ", songSizeType=" + songSizeType + ", isShowArrow="
                + isShowArrow + ", playlistId=" + playlistId + "]";
    }

}
