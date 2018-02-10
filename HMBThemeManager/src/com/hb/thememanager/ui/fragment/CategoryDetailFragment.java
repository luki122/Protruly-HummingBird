package com.hb.thememanager.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hb.thememanager.R;
import com.hb.thememanager.http.request.CategoryDetailRequest;
import com.hb.thememanager.http.request.ThemeRankingRequest;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.ThemeListResponse;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.ui.SimpleRequestPresenter;
import com.hb.thememanager.ui.SimpleRequestView;
import com.hb.thememanager.ui.adapter.ThemeListAdapter;
import com.hb.thememanager.utils.ToastUtils;
import com.hb.thememanager.views.AutoLoadListView;
import com.hb.thememanager.views.ThemeListView;
import com.hb.thememanager.views.pulltorefresh.PullToRefreshBase;

import java.util.List;

/**
 * 主题包Tab内容页面
 *
 */
public class CategoryDetailFragment extends EmptyViewFragment implements SimpleRequestView
		,AutoLoadListView.OnAutoLoadListener,PullToRefreshBase.OnRefreshListener {
	private static final String TAG = "CategoryDetailFragment";

	private static final int PAGE_SIZE = 9;
	private int mCurrentPage = 0;
	private ThemeListAdapter mAdapter;
	private int mType;
	private int mCategory;
	private int mId;
	private boolean isRefresh;

	private View mContentView;
	private ThemeListView mList;

	private SimpleRequestPresenter mPresenter;

	private static final int COMPLETE_REQUEST = 0;

	public CategoryDetailFragment(){}

	public CategoryDetailFragment(CharSequence title, int category, int type, int id) {
		setTitle(title);
		mCategory = category;
		mType = type;
		mId = id;
	}

	private void request(boolean loadmore){
		CategoryDetailRequest request = new CategoryDetailRequest(getContext(),mType,mCategory,mId);
		if(loadmore){
			mCurrentPage++;
		}else{
			mCurrentPage = 0;
		}
		isRefresh = mCurrentPage == 0;
		request.setPageNumber(mCurrentPage);
		request.setPageSize(PAGE_SIZE);
		mPresenter.requestTheme(request);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		Log.e(TAG, "onViewCreated");
		mAdapter = new ThemeListAdapter(getContext());
		mAdapter.setJumpWallpaperDetailData(ThemeListAdapter.URL_TYPE_CATEGORY_DETAIL, mId, mCategory, "");
		mAdapter.setType(mType);
		mAdapter.setCategory(mCategory);
		mAdapter.setHeaderMargin(getResources().getDimensionPixelOffset(R.dimen.padding_top_theme_list));

		mList.setAdapter(mAdapter);

		mPresenter = new SimpleRequestPresenter(getContext().getApplicationContext());
		mPresenter.attachView(this);
		request(false);
		showLoadingView(true);
	}

	@Override
	public View onCreateNormalView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		Log.e(TAG, "onCreateNormalView");
		if(mContentView == null) {
			mContentView = inflater.inflate(R.layout.list_home_fragment, container, false);
			mList = (ThemeListView) mContentView.findViewById(android.R.id.list);
			mList.setOnAutoLoadListener(this);
			mList.setOnRefreshListener(this);
		}
		return mContentView;
	}

	@Override
	public void onResume() {
		super.onResume();
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void update(Response response) {
		boolean hasMore = false;
		boolean hasData = false;
		if(response instanceof ThemeListResponse){
			if(((ThemeListResponse) response).body != null) {
				List<Theme> datas = ((ThemeListResponse) response).body.getThemes(mType);
				if (isRefresh) {
					if(datas != null && datas.size() > 0){
						hasData = true;
					}else{
						hasData = false;
					}
					mAdapter.setData(datas);
				} else {
					mAdapter.addData(datas);
				}
				mAdapter.notifyDataSetChanged();
				int totalPage = ((ThemeListResponse) response).body.getTotalNum();
				if(mCurrentPage < totalPage - 1){
					hasMore = true;
				}
			}
		}
		Log.e(TAG, "update : hasData = "+hasData);
		if(hasData){
			setState(EMPTY_STATE_NONE);
		}else if(isRefresh){
			setState(EMPTY_STATE_NO_DATA);
		}
		showLoadingView(false);
		mList.onLoadingComplete(hasMore);
		mList.onRefreshComplete();
	}


	@Override
	public void showToast(String msg) {
		ToastUtils.showShortToast(getContext(), msg);
	}


	@Override
	public void showEmptyView(boolean show) {
		Log.e(TAG, "showEmptyView : show = "+show);
		setState(EMPTY_STATE_NO_DATA);
		showLoadingView(false);
	}

	@Override
	public void showNetworkErrorView(boolean show) {
		Log.e(TAG, "showNetworkErrorView : show = "+show);
		setState(EMPTY_STATE_NO_NETWORK);
		showLoadingView(false);
	}

	@Override
	public void onLoading(boolean hasMore) {
		if(hasMore) {
			request(true);
		}
	}

	@Override
	public void onRefresh(PullToRefreshBase refreshView) {
		request(false);
	}

	@Override
	public void showDialog(int dialogId) {

	}

	@Override
	public void showRequestFailView(boolean show) {
		Log.e(TAG, "showRequestFailView");
		setState(EMPTY_STATE_NO_NETWORK);
		showLoadingView(false);
	}

	@Override
	protected void onEmptyButtonClick(View v, int state) {
		super.onEmptyButtonClick(v, state);
		if(state == EMPTY_STATE_NO_DATA) {
			request(false);
			showLoadingView(true);
		}
	}
}
