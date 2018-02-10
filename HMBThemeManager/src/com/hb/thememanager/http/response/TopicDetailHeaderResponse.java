package com.hb.thememanager.http.response;

import com.hb.thememanager.model.TopicDetailHeader;


/**
 *专题详情页数据
 */
public class TopicDetailHeaderResponse extends Response{

	public TopicDetailHeader body;

	@Override
	public String toString() {
		return "TopicDetailThemeResponse [body=" + body + "]";
	}
	
}
