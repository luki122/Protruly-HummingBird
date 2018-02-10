package com.hb.thememanager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import hb.app.HbActivity;
import com.hb.thememanager.R;
import com.hb.thememanager.http.request.CategoryRequest;
import com.hb.thememanager.http.response.CategoryResponse;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.ui.adapter.CategoryListAdapter;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.views.AutoLoadListView;
import com.hb.thememanager.views.ThemeListView;
import com.hb.thememanager.views.pulltorefresh.PullToRefreshBase;

import hb.widget.toolbar.Toolbar;

public class CategoryActivity extends SecondActivity implements CategoryView,PullToRefreshBase.OnRefreshListener {

	private Toolbar mToolbar;
	private ThemeListView mList;

	private CategoryListAdapter mAdapter;

	private String mTitleString;
	private CategoryPresenter mPresenter;
	private int mType;

	private static final int COMPLETE_REQUEST = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setHbContentView(R.layout.activity_category);

		mPresenter = new CategoryPresenter(getApplicationContext());
		mPresenter.attachView(this);

		Intent intent = getIntent();
		mType = intent.getIntExtra(Config.ActionKey.KEY_FAST_ENTRY,0);
		switch (mType){
			case Theme.THEME_PKG:
				mTitleString = getResources().getString(R.string.tab_title_theme)+getResources().getString(R.string.text_category);
				break;
			case Theme.RINGTONE:
				mTitleString = getResources().getString(R.string.tab_title_ringtong)+getResources().getString(R.string.text_category);
				break;
			case Theme.WALLPAPER:
				mTitleString = getResources().getString(R.string.tab_title_wallpaper)+getResources().getString(R.string.text_category);
				break;
			case Theme.FONTS:
				mTitleString = getResources().getString(R.string.tab_title_font)+getResources().getString(R.string.text_category);
				break;
			default:
				mTitleString = intent.getStringExtra(Config.ActionKey.KEY_JUMP_TITLE);
				break;
		}

		initialUI(savedInstanceState);
		if(!CommonUtil.hasNetwork(this)){
			setState(EMPTY_STATE_NO_NETWORK);
		}else{
			request();
			showLoadingView(true);
		}
	}

	private void request(){
		CategoryRequest request = new CategoryRequest(this,mType);
		mPresenter.requestTheme(request);
	}

	protected void initialUI(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		mToolbar = getToolbar();
		mList = (ThemeListView) findViewById(android.R.id.list);
		mList.hasFooterView(false);
		mList.setOnRefreshListener(this);
		mAdapter = new CategoryListAdapter(this, mType);
		mList.setAdapter(mAdapter);
		setupPager();
	}

	@Override
	public void onNavigationClicked(View view) {
		onBackPressed();
	}

	private void setupPager(){
		//设置标题
		setTitle(mTitleString);
	}


	@Override
	public void updateList(Object response) {
		showLoadingView(false);
		boolean dataNoral = false;
		if(response != null){
			if(((CategoryResponse)response).body != null) {
				if(((CategoryResponse) response).body.type != null && ((CategoryResponse) response).body.type.size() > 0){
					dataNoral = true;
				}
				mAdapter.setData(((CategoryResponse) response).body.type);
				mAdapter.notifyDataSetChanged();
			}
		}
		if(dataNoral){
			setState(EMPTY_STATE_NONE);
		}else{
			setState(EMPTY_STATE_NO_DATA);
		}
		mList.onRefreshComplete();
	}

	@Override
	public void networkError(int statusCode, String error_msg) {
		setState(EMPTY_STATE_NO_NETWORK);
	}

	@Override
	public void showToast(String msg) {

	}

	@Override
	public void showMyDialog(int dialogId) {

	}

	@Override
	public void showEmptyView(boolean show) {

	}

	@Override
	public void showNetworkErrorView(boolean show) {

	}


	@Override
	public void onRefresh(PullToRefreshBase refreshView) {
		request();
	}

	@Override
	protected void onEmptyButtonClick(View v, int state) {
		super.onEmptyButtonClick(v, state);
		if(CommonUtil.hasNetwork(this)) {
			request();
			showLoadingView(true);
		}
	}
}
