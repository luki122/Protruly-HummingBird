package com.hb.thememanager.http.response.adapter;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.model.Fonts;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexluo on 17-8-3.
 */

public class SearchAssistBody extends ResponseBody{
    public List<Key> keyList;

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    public static class Key{
        public String key;
    }
}
