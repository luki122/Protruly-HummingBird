package com.hb.thememanager.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hb.thememanager.R;
import com.hb.thememanager.http.request.SearchResultThemeRequest;
import com.hb.thememanager.http.response.SearchResultThemeResponse;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.ui.SearchPresenter;
import com.hb.thememanager.ui.SearchView;
import com.hb.thememanager.ui.adapter.ThemeListAdapter;
import com.hb.thememanager.utils.StringUtils;
import com.hb.thememanager.utils.ToastUtils;
import com.hb.thememanager.views.AutoLoadListView;
import com.hb.thememanager.views.ThemeListView;
import com.hb.thememanager.views.pulltorefresh.PullToRefreshBase;

import java.util.List;

import hb.widget.ViewPager;
import hb.widget.tab.TabLayout;

/**
 * 主题包Tab内容页面
 *
 */
public class SearchResultThemeFragment extends EmptyViewFragment implements SearchView, PullToRefreshBase.OnRefreshListener, AutoLoadListView.OnAutoLoadListener {
	private static final String TAG = "SearchResultThemeFragment";
	private ThemeListAdapter mAdapter;

	private View mContentView;
	private ThemeListView mListView;
	private TextView mEmptyTextView;

	private SearchPresenter mPresenter;
	private String mSearchStr;
	private int mType;

	private static final int PAGE_SIZE = 9;
	private int currentPage = 0;
	private boolean isLoadMore = false;

	public SearchResultThemeFragment(){}
	public SearchResultThemeFragment(int type){
		mType = type;
	}

	public void setSearchString(String str){
		mSearchStr = str;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		Log.e(TAG, "onActivityCreated : mSearchStr = "+mSearchStr);
		mPresenter = new SearchPresenter(getContext().getApplicationContext());
		mPresenter.attachView(this);

		showLoadingView(true);
		isLoadMore = false;
		search(mSearchStr);
	}

	public void search(String str){
		if(!StringUtils.isEmpty(str)) {
			mSearchStr = str;
			if(isAdded()) {
				SearchResultThemeRequest request = new SearchResultThemeRequest(getContext(),mType);
				request.setKey(str);
				request.setPageSize(PAGE_SIZE);
				if(isLoadMore){
					currentPage++;
					request.setPageNumber(currentPage);
				}else{
					currentPage = 0;
					request.setPageNumber(currentPage);
				}
				mPresenter.requestTheme(request);
			}
		}
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		Log.e(TAG, "onViewCreated");
	}

	@Override
	public void onResume() {
		super.onResume();
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public View onCreateNormalView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		Log.e(TAG, "onCreateNormalView");
		if(mContentView == null) {
			mContentView = inflater.inflate(R.layout.fragment_search_result_list, container, false);
			mListView = (ThemeListView) mContentView.findViewById(R.id.list_view);
			mListView.setOnRefreshListener(this);
			mListView.setOnAutoLoadListener(this);
			mEmptyTextView = (TextView) mContentView.findViewById(R.id.empty_message);
		}
		mAdapter = new ThemeListAdapter(getContext());
		mAdapter.setJumpWallpaperDetailData(ThemeListAdapter.URL_TYPE_SEARCH, ThemeListAdapter.NULL, ThemeListAdapter.NULL, mSearchStr);
		mListView.setAdapter(mAdapter);

		return mContentView;
	}

	@Override
	public void updateList(Object response) {
		Log.e(TAG, "updateList : response = "+response);
		boolean hasResult = false;
		boolean hasMore = true;
		if(response != null && response instanceof SearchResultThemeResponse){
			if(((SearchResultThemeResponse) response).body != null) {
				int totalNum = ((SearchResultThemeResponse) response).body.getTotalNum();
				if(currentPage >= totalNum-1){
					hasMore = false;
				}
				List<Theme> data = ((SearchResultThemeResponse) response).body.getThemes(mType);
				mAdapter.setData(data);
				mAdapter.setType(mType);
				mAdapter.notifyDataSetChanged();
				if (data != null && data.size() > 0) {
					hasResult = true;
				}
			}
		}

		if(hasResult){
			mListView.setVisibility(View.VISIBLE);
			mEmptyTextView.setVisibility(View.GONE);
		}else{
			hasMore = false;
			mListView.setVisibility(View.GONE);
			mEmptyTextView.setVisibility(View.VISIBLE);
		}

		mListView.onRefreshComplete();
		mListView.onLoadingComplete(hasMore);
		showLoadingView(false);
	}

	@Override
	public void showToast(String msg) {
		ToastUtils.showShortToast(getContext(), msg);
	}

	@Override
	public void showMyDialog(int dialogId) {

	}

	@Override
	public void showEmptyView(boolean show) {

	}

	@Override
	public void showNetworkErrorView(boolean show) {
		setState(EMPTY_STATE_NO_NETWORK);
		showLoadingView(false);
	}

	@Override
	public void onLoading(boolean hasMore) {
		if(hasMore){
			isLoadMore = true;
			search(mSearchStr);
		}
	}

	@Override
	public void onRefresh(PullToRefreshBase refreshView) {
		isLoadMore = false;
		search(mSearchStr);
	}

	@Override
	protected void onEmptyButtonClick(View v, int state) {
		super.onEmptyButtonClick(v, state);
		if(state == EMPTY_STATE_NO_DATA){
			showLoadingView(true);
			isLoadMore = false;
			search(mSearchStr);
		}
	}
}
