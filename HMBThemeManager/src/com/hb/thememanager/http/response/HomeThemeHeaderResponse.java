package com.hb.thememanager.http.response;

import android.util.Log;

import java.util.ArrayList;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.http.response.adapter.AdvertisinBody;
import com.hb.thememanager.model.Advertising;

/**
 *主题首页头部数据，包括banner广告和快速入口图片 
 *
 */
public class HomeThemeHeaderResponse extends Response{

	public AdvertisinBody body;


	@Override
	public String toString() {
		return "HomeThemeHeaderResponse{" +
				"body=" + body +
				'}';
	}
}
