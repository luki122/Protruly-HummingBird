package com.hb.thememanager.ui.fragment;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hb.thememanager.R;
import com.hb.thememanager.http.request.HomeThemeHeaderRequest;
import com.hb.thememanager.http.request.HomeThemeRequest;
import com.hb.thememanager.http.request.ThemeRequest;
import com.hb.thememanager.http.response.HomeThemeBodyResponse;
import com.hb.thememanager.http.response.HomeThemeHeaderResponse;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.model.HomeThemeCategory;
import com.hb.thememanager.model.HomeThemeHeaderCategory;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.ui.HomePage;
import com.hb.thememanager.ui.SimpleRequestPresenter;
import com.hb.thememanager.ui.SimpleRequestView;
import com.hb.thememanager.ui.adapter.AbsHomeThemeListAdapter;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.TLog;
import com.hb.thememanager.utils.ToastUtils;
import com.hb.thememanager.views.AutoLoadListView;
import com.hb.thememanager.views.ThemeListView;
import com.hb.thememanager.views.pulltorefresh.PullToRefreshBase;

import java.util.List;
/**
 *主界面每个Tab对应的界面需要继承这个类去实现自己的内容 
 *
 */
public abstract class AbsHomeFragment extends EmptyViewFragment implements SimpleRequestView,AutoLoadListView.OnAutoLoadListener
		,PullToRefreshBase.OnRefreshListener,View.OnClickListener {
	private static final String TAG = "AbsHomeFragment";
	private static final int PAGE_SIZE = 14;
	private static final int BANNER_PAGE_SIZE = 5;
	private static final int MSG_SHOW_NETWORK_ERROR = 1;
	protected static final int COMPLETE_REQUEST = 0;
	protected CharSequence mTitle;
	protected ThemeListView mList;
	protected AbsHomeThemeListAdapter mAdapter;
	protected Dialog mDialog;
	protected SimpleRequestPresenter mPresenter;
	private ThemeRequest mHeaderRequest;
	private ThemeRequest mThemeRequest;
	protected int mCurrentPage = 0;
	private int mPageSize = 1;
	private int mMaxPage;
	private boolean mIsRefresh = false;
	private boolean mRequestError = false;
	/**
	 * 用于处理启动时会收到网络变化的广播
	 */
	private boolean mFirtTimeEnter = false;
	private NetworkChangeReceiver mNetworkReceiver;

	public AbsHomeFragment(){}
	public AbsHomeFragment(CharSequence title){
		mTitle = title;
		
	}

	@Override
	public void onLoading(boolean hasMore) {
		if(hasMore) {
			mCurrentPage ++;
			request();
		}
	}


	@Override
	public void onRefresh(PullToRefreshBase refreshView) {
		mIsRefresh = true;
		getFirstPage();
	}

	private void getFirstPage(){
		mCurrentPage = 0;
		mPageSize = PAGE_SIZE;
		showLoadingView(!hasData());
		request();
	}

	protected boolean hasData(){
		if(mAdapter == null){
			return  false;
		}
		return mAdapter.getCount() > 1;
	}

	protected boolean isRefresh(){
		return mIsRefresh;
	}
	protected void setRefresh(boolean refresh){
		mIsRefresh = refresh;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
	}

	private void request(){
		int themeType = getThemeType();
		if(themeType == Theme.THEME_NULL || themeType == -1){
			return;
		}
		if(mHeaderRequest == null){
			mHeaderRequest =new HomeThemeHeaderRequest(getContext(), themeType);
		}

		if(mThemeRequest == null){
			mThemeRequest =new HomeThemeRequest(getContext(), themeType);
		}
		mThemeRequest.setPageNumber(mCurrentPage);
		mThemeRequest.setPageSize(mPageSize);
		if(mCurrentPage == 0) {
			mHeaderRequest.setPageSize(BANNER_PAGE_SIZE);
			getPresenter().requestTheme(mHeaderRequest);
		}
		getPresenter().requestTheme(mThemeRequest);
	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		mFirtTimeEnter = true;
		if(getThemeType() != -1) {
			super.onViewCreated(view,savedInstanceState);
			if (getThemeType() != Theme.RINGTONE) {
				mList.onLoadingComplete(true);
				getPresenter();
				setListAdapter(createAdapter());
				getFirstPage();
			}

			mNetworkReceiver = new NetworkChangeReceiver();
			IntentFilter filter = new IntentFilter();
			filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
			getContext().registerReceiver(mNetworkReceiver,filter);
		}

	}

	@Override
	public void onClick(View view) {


	}


	@Override
	public void onResume() {
		super.onResume();
		if(mAdapter != null && hasData()){
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	protected void onEmptyButtonClick(View v, int state) {
	
		if(state == EMPTY_STATE_NO_DATA){
			getFirstPage();
		}else if(state == EMPTY_STATE_NO_NETWORK){
			startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
		}

	}

	protected abstract  AbsHomeThemeListAdapter createAdapter();

	protected abstract int getThemeType();

	public SimpleRequestPresenter getPresenter(){
		if(mPresenter == null){
			mPresenter = new SimpleRequestPresenter(getContext().getApplicationContext());
			mPresenter.attachView(this);
		}
		return mPresenter;
	}
	
	public CharSequence getTitle(){
		return mTitle;
	}
	
	public void setTitle(CharSequence title){
		mTitle = title;
	}
	
	public View findViewById(int id){
		if(getCustomPanel() != null){
			return getCustomPanel().findViewById(id);
		}
		return null;
	}



	@Override
	public void onDestroy() {
		super.onDestroy();
		getContext().unregisterReceiver(mNetworkReceiver);
		mFirtTimeEnter = false;
	}


	@Override
	public View onCreateNormalView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.list_home_fragment, container,false);
		mList = (ThemeListView) view.findViewById(android.R.id.list);
		mList.setOnAutoLoadListener(this);
		mList.setOnRefreshListener(this);
		return view;
	}

	/**
	 * 设置Adapter给ListView
	 * @param adapter
	 */
	public void setListAdapter(AbsHomeThemeListAdapter adapter){
		if(adapter == null){
			return;
		}
		mAdapter = adapter;
		if(mList != null){
			mList.setAdapter(adapter);
		}
	}

	public ThemeListView getListView() {
		return mList;
	}

	@Override
	public void update(Response result) {
		// TODO Auto-generated method stub
		Context context = getContext();
		mFirtTimeEnter = false;
		if(context == null) return;
		boolean hasMore = true;
		//sendMessage(COMPLETE_REQUEST,null);
		if(result == null || mAdapter == null){
			return;
		}
		boolean hasNetwork = CommonUtil.hasNetwork(context);
		if(result instanceof HomeThemeHeaderResponse){
			if(mIsRefresh){
				mAdapter.removeAllCategories(HomeThemeCategory.TYPE_HEADER);
			}
			HomeThemeCategory header = new HomeThemeHeaderCategory((HomeThemeHeaderResponse)result);
			header.setType(HomeThemeCategory.TYPE_HEADER);
			mAdapter.addCategory(header);
		}else if(result instanceof HomeThemeBodyResponse){
			showLoadingView(false);
			HomeThemeBodyResponse response = (HomeThemeBodyResponse)result;
			if(response == null){
				return;
			}

			if(mIsRefresh){
				mAdapter.removeAllCategories(HomeThemeCategory.TYPE_CATEGORY);
				mIsRefresh = false;
			}

			hasMore = mCurrentPage < response.body.getTotalNum() -1;
			List<HomeThemeCategory> categories = response.getThemes(getThemeType());

			if(categories != null && categories.size() > 0){
				mAdapter.addCategories(categories);
				setState(EMPTY_STATE_NONE);
			}else{
				if(!hasData()){
					setState(EMPTY_STATE_NO_DATA);
				}
				hasMore = false;
			}
		}
		sendMessage(COMPLETE_REQUEST,hasMore);

	}
	
	public void addHeaderView(View header){

	}
	
	public void addFooterView(View footer){
		
	}
	
	/**
	 * 显示Toast提示
	 * @param msg
	 */
	public void showToast(String msg){
		ToastUtils.showShortToast(getContext(), msg);
	}
	
	/**
	 * 显示网络错误或者无网络视图
	 */
	@Override
	public void showNetworkErrorView(boolean show){
		mFirtTimeEnter = false;
		TLog.d(TAG,"showNetworkError has adapter->"+(mAdapter != null));
		if(mAdapter != null){
			boolean hasData = mAdapter.getCount() > 0;
			if(hasData){
				showNetworkErrorPanel(true);
			}else {
				setState(EMPTY_STATE_NO_NETWORK);
			}
		}else{
			setState(EMPTY_STATE_NO_NETWORK);
		}

	}


	private void showNetworkErrorOnly(boolean show){
		setState(EMPTY_STATE_NO_NETWORK);
	}


	public void showNetworkErrorPanel(boolean show){
		if(hasData()) {
			HomePage homePage = (HomePage) getActivity();
			if (homePage != null) {
				homePage.showNetworkErrorPanel(show);
			}
		}
	}

	@Override
	public void showRequestFailView(boolean show) {
		mFirtTimeEnter = false;
		if(!hasData()) {
			if(CommonUtil.hasNetwork(getContext())) {
				setState(EMPTY_STATE_NO_DATA);
			}else{
				setState(EMPTY_STATE_NO_NETWORK);
			}
			showLoadingView(false);
		}

		if(mList != null){
			mList.onRefreshComplete();
		}
	}



	/**
	 * 显示空视图
	 * @param show
	 */
	public void showEmptyView(boolean show){
		
	}
	
	/**
	 * 显示指定ID的Dialog
	 * @param dialogId
	 */
	public void showDialog(int dialogId){
		
		dismissDialog();
		
		mDialog = onCreateDialog(dialogId);
		if(mDialog != null){
			mDialog.show();
		}
	}
	
	/**
	 * 隐藏Dialog
	 */
	public void dismissDialog(){
		if(mDialog != null && mDialog.isShowing()){
			mDialog.dismiss();
		}
	}
	
	/**
	 * 根据不同的ID去创建不同作用的Dialog
	 * @param dialogId
	 * @return
	 */
	protected Dialog onCreateDialog(int dialogId){
		return null;
	}

	protected void handleCompleteMessage(Message msg){
		boolean hasMore = false;
		if(msg.obj instanceof Boolean){
			hasMore = (boolean) msg.obj;
		}
		mList.onLoadingComplete(hasMore);
		mList.onRefreshComplete();
	}

	private class NetworkChangeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(getThemeType() == Theme.RINGTONE){
				return;
			}
			if("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())){
				TLog.d(TAG,"onNet change, has network->"+CommonUtil.hasNetwork(context)
				+" mFirtTimeEnter"+mFirtTimeEnter+" hasData->"+hasData());
					if(CommonUtil.hasNetwork(context)) {
						if(mFirtTimeEnter){
							mFirtTimeEnter =false;
							return;
						}
						AbsHomeFragment.this.showNetworkErrorPanel(false);
						if(!hasData()){
							AbsHomeFragment.this.getFirstPage();
						}
					}else{
						AbsHomeFragment.this.showNetworkErrorView(true);
					}
			}
		}
	}
	
}
