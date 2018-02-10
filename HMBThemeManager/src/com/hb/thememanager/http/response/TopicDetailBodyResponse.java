package com.hb.thememanager.http.response;

import com.hb.thememanager.http.response.adapter.ResponseBody;
import com.hb.thememanager.http.response.adapter.TopicDetailBody;

public class TopicDetailBodyResponse extends Response {
	public TopicDetailBody body;

	@Override
	public String toString() {
		return "TopicDetailBodyResponse [body=" + body + "]";
	}

	@Override
	public ResponseBody returnBody() {
		return body;
	}
	
}
