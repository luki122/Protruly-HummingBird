package com.hb.thememanager.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.hb.thememanager.http.request.PurchaseRecordRequest;
import com.hb.thememanager.http.request.ThemeRankingRequest;
import com.hb.thememanager.http.request.ThemeRankingTabRequest;
import com.hb.thememanager.http.request.WallpaperRankingRequest;
import com.hb.thememanager.http.request.WallpaperRankingTabRequest;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.ThemeListResponse;
import com.hb.thememanager.model.Tab;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.ui.SimpleRequestPresenter;
import com.hb.thememanager.ui.SimpleRequestView;
import com.hb.thememanager.ui.adapter.ThemeListAdapter;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.ToastUtils;
import com.hb.thememanager.views.AutoLoadListView;
import com.hb.thememanager.views.ThemeListView;
import com.hb.thememanager.R;
import com.hb.thememanager.views.pulltorefresh.PullToRefreshBase;

import java.util.List;

/**
 * 主题包Tab内容页面
 *
 */
public class PurchaseRecordFragment extends EmptyViewFragment implements SimpleRequestView
		,AutoLoadListView.OnAutoLoadListener,PullToRefreshBase.OnRefreshListener {
	private static final String TAG = "RankingFragment";

	private static final int PAGE_SIZE = 15;
	private ThemeListAdapter mAdapter;
	private int mType;
	private int mCategory;

	private View mContentView;
	private ThemeListView mList;
	private int mCurrentPage;
	private boolean isRefresh = false;

	private SimpleRequestPresenter mPresenter;


	public PurchaseRecordFragment(){}

    /**
     * 
     * @param title
     * @param category  1-付费、2-免费、3-新品、4-热门
     * @param type :  theme | font | wallpaper
     */
	public PurchaseRecordFragment(CharSequence title, int category, int type) {
		mCategory = category;
		mType = type;
		setTitle(title);
	}

	private void request(boolean loadMore){
		PurchaseRecordRequest request = new PurchaseRecordRequest(getContext(),mCategory,mType);
		if(loadMore){
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
		mAdapter = new ThemeListAdapter(getContext());
		mAdapter.setType(mType);
		mAdapter.setCategory(mCategory);
		mAdapter.showPrice(false);
		mList.setAdapter(mAdapter);

		mPresenter = new SimpleRequestPresenter(getContext().getApplicationContext());
		mPresenter.attachView(this);

		request(false);
		showLoadingView(true);
	}

	@Override
	public View onCreateNormalView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		mContentView = inflater.inflate(R.layout.list_home_fragment, container,false);
		mList = (ThemeListView) mContentView.findViewById(android.R.id.list);
		mList.setOnAutoLoadListener(this);
		mList.setOnRefreshListener(this);
		return mContentView;
	}

	@Override
	public void update(Response response) {
		boolean hasData = false;
		boolean hasMore = false;

		if(response instanceof ThemeListResponse){
			if(((ThemeListResponse)response).body != null) {
				List<Theme> data = ((ThemeListResponse) response).body.getThemes(mType);
				if(data != null && data.size() > 0){
					hasData = true;
				}
				if(mCurrentPage < ((ThemeListResponse)response).body.getTotalNum() - 1){
					hasMore = true;
				}
				if(isRefresh) {
					mAdapter.setData(data);
				}else{
					mAdapter.addData(data);
				}
				mAdapter.notifyDataSetChanged();
			}
		}
		Log.e("cai", "hasData = "+hasData+" ; isRefresh = "+isRefresh+" ; hasMore = "+hasMore);
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
		setState(EMPTY_STATE_NO_DATA);
	}

	@Override
	public void showNetworkErrorView(boolean show) {
		setState(EMPTY_STATE_NO_NETWORK);
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

	}

	@Override
	protected void onEmptyButtonClick(View v, int state) {
		super.onEmptyButtonClick(v, state);

		if(state == EMPTY_STATE_NO_DATA) {
			request(false);
			showLoadingView(true);
		}
	}
	
	@Override
	public void showNoDataView(){
		showErrorView();
		TextView textView = getEmptyTextView();
		textView.setText(R.string.no_purchase_record);
//		getEmptyButton().setText(R.string.click_to_refresh);
		getEmptyButton().setVisibility(View.GONE);
	}

}
