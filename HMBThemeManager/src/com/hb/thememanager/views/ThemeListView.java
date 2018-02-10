package com.hb.thememanager.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;

/**
 * 在线数据页面的数据显示列表使用这个ListView进行显示，该ListView
 * 包含了下拉刷新和上拉加载更多的功能
 * @author alexluo
 *
 */
public class ThemeListView extends AutoLoadListView {


	public ThemeListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public ThemeListView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public void addHeaderView(View headerView){
		if(getListView() != null){
			getListView().addHeaderView(headerView);
		}
	}

	public void removeHeaderView(View header){
		if(getListView() != null){
			getListView().removeHeaderView(header);
		}
	}

}
