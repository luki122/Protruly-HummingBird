package com.hb.thememanager.http.response.adapter;

import com.hb.thememanager.http.response.ThemePkgDetailResponse;

import java.util.ArrayList;

/**
 * Created by alexluo on 17-8-3.
 */

public class ThemeDetailBody extends ResponseBody{
    public String name;
    public String version;
    public int isCharge;
    public int price;
    public float score;
    public long downloadCount;
    public String downloadUrl;
    public long size;
    public String updateTime;
    public String updateContent;
    public String putawayStatus;
    public String description;
    public int designer;
    public int buyStatus;
    public String designerNickname;
    public ArrayList<String> romVersion;
    public ScreenShots screenshots;

    @Override
    public String toString() {
        return "Body{" +
                "id='" + getId() + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", isCharge=" + isCharge +
                ", price=" + price +
                ", score=" + score +
                ", downloadCount=" + downloadCount +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", size=" + size +
                ", updateTime='" + updateTime + '\'' +
                ", updateContent='" + updateContent + '\'' +
                ", putawayStatus='" + putawayStatus + '\'' +
                ", description='" + description + '\'' +
                ", designer=" + designer +
                ", designerNickname='" + designerNickname + '\'' +
                ", romVersion=" + romVersion +
                ", screenshots=" + screenshots +
                '}';
    }
}
