package com.hb.thememanager.http.response.adapter;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.model.Tab;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexluo on 17-8-3.
 */

public class RankTabBody extends ResponseBody{

    public List<Tab> tabs;

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
