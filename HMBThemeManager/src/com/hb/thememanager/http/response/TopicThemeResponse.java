package com.hb.thememanager.http.response;

import com.hb.thememanager.http.response.adapter.ResponseBody;
import com.hb.thememanager.http.response.adapter.TopicBody;

/**
 *专题数据
 */
public class TopicThemeResponse extends Response{
	public TopicBody body;

	@Override
	public String toString() {
		return "TopicThemeResponse [body=" + body + "]";
	}

	@Override
	public ResponseBody returnBody() {
		return body;
	}
}
