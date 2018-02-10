package com.hb.thememanager.http.response;

import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.http.response.adapter.RankTabBody;
import com.hb.thememanager.model.Tab;
import com.hb.thememanager.model.Theme;

import java.util.List;

public class RankingTabsResponse extends Response {

	public RankTabBody body;

	@Override
	public String toString(){
		return JSON.toJSONString(this);
	}
}
