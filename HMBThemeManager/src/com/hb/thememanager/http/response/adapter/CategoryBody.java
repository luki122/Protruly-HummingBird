package com.hb.thememanager.http.response.adapter;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.model.Category;
import com.hb.thememanager.model.Fonts;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;

import java.util.ArrayList;

/**
 * Created by alexluo on 17-8-3.
 */

public class CategoryBody extends ResponseBody{

    public ArrayList<Category> type;

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
