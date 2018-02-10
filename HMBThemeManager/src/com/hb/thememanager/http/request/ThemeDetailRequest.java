package com.hb.thememanager.http.request;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.response.IResponseHandler;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.ThemeResponse;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.User;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 所有的主题详情请求都由这个类来构造，通过{@link #id}
 * 来唯一标识
 */
public  class ThemeDetailRequest extends ThemeRequest{

	private Context mContext;


	public ThemeDetailRequest(Context context,int themeType) {
		super(context,themeType);
		mContext = context;
		setUrl(Config.HttpUrl.getDetailUrl(themeType));
	}



	@Override
	public void request() {

	}

	@Override
	public void generateRequestBody() {
		ThemeDetailBody body = new ThemeDetailBody();
		body.id = getId();
		body.type = getThemeType();
		User user = User.getInstance(mContext);
		body.setQlcId(user.isLogin()?Long.parseLong(user.getId()):0L);
		setBody(body);

		body.setupAvaliableProperties("id","type","qlcId");
	}

	@Override
	public Response parseResponse(String responseStr) {
		return null;
	}

	public class ThemeDetailBody extends RequestBody{
		long qlcId;

		public long getQlcId() {
			return qlcId;
		}

		public void setQlcId(long qlcId) {
			this.qlcId = qlcId;
		}
	}
}
