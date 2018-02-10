package com.hb.thememanager.http.response;

import android.os.Parcel;
import android.os.Parcelable;

import com.hb.thememanager.http.response.adapter.CommentsBody;
import com.hb.thememanager.http.response.adapter.ResponseBody;
import com.hb.thememanager.model.Comments;

import java.util.ArrayList;
import java.util.List;

/**
 *主题首页头部数据，包括banner广告和快速入口图片 
 *
 */
public class ThemeCommentsResponse extends Response{


	public CommentsBody body;

	public static class Body{

	}

	public ArrayList<Comments> getComments(){
		return body.comment;
	}

	@Override
	public String toString() {
		return "ThemeCommentsResponse{" +
				"body=" + body +
				'}';
	}

	@Override
	public ResponseBody returnBody() {
		return body;
	}
}
