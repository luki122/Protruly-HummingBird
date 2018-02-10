package com.android.quicksearchbox.hotsearch;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by lijun on 17-8-16.
 */

public class HotSearchInfo {

    public static String WEIBO_URL = "http://api.weibo.com/search/hot_word.json";
    public static String HMB_SOURCE = "848984564";
    public static String HMB_SID = "o_darling";
    public static int DEFAULT_COUNT = 50;

    public static String HMB_WEIBO_URL = "http://61.147.171.31/weibo-hot-search/search";

    public int s_id;//热搜榜排名
    public String word;//搜索词
    public int num;//搜索热度
    public int flag;//搜索词标记，1-新，2-热，4-爆
    public String app_query_link;//App唤起地址
    public String h5_query_link;//H5跳转地址
    public String flag_link;//搜索词标记图片地址

    public HotSearchInfo() {

    }

    public HotSearchInfo(int s_id, String word, int num, int flag, String app_query_link, String h5_query_link, String flag_link) {
        this.s_id = s_id;
        this.word = word;
        this.num = num;
        this.flag = flag;
        this.app_query_link = app_query_link;
        this.h5_query_link = h5_query_link;
        this.flag_link = flag_link;
    }

    public static String getHotSearchUrl(String source, String sid, int count) throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("source", source);
        jo.put("sid", sid);
        jo.put("count", count);
        String str = jo.toString().replace("\"", "%22").replace("{", "%7b").replace("}", "%7d");
        return WEIBO_URL + "?param=" + str;
    }

    public static String getHotSearchUrl() throws JSONException {
        return WEIBO_URL + "?source=" + HMB_SOURCE + "&sid=" + HMB_SID + "&count=" + DEFAULT_COUNT;
    }
}
