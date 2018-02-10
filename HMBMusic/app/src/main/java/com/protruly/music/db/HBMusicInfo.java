package com.protruly.music.db;

/**
 * Created by hujianwei on 17-8-29.
 */

public class HBMusicInfo {

    protected String lrcPath;
    protected String picPath;

    public HBMusicInfo(String lrcPath, String picPath) {
        this.lrcPath = lrcPath;
        this.picPath = picPath;
    }

    public String getLrcPath() {
        return lrcPath;
    }

    public String getPicPath() {
        return picPath;
    }

    @Override
    public String toString() {
        return "HBMusicInfo [lrcPath=" + lrcPath + ", picPath=" + picPath
                + "]";
    }

}
