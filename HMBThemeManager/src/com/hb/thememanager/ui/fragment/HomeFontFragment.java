package com.hb.thememanager.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.hb.thememanager.http.request.HomeThemeHeaderRequest;
import com.hb.thememanager.http.request.HomeThemeRequest;
import com.hb.thememanager.http.response.HomeThemeBodyResponse;
import com.hb.thememanager.http.response.HomeThemeHeaderResponse;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.model.HomeThemeCategory;
import com.hb.thememanager.model.HomeThemeHeaderCategory;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.ui.adapter.HomeFontsListAdapter;
import com.hb.thememanager.ui.adapter.HomeHeaderListAdapter;
import com.hb.thememanager.ui.adapter.HomeThemeListAdapter;
import com.hb.thememanager.ui.adapter.MultipleListAdapter;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.ui.adapter.AbsHomeThemeListAdapter;
import com.hb.thememanager.utils.TLog;

import java.util.List;

/**
 * 字体Tab内容页面
 *
 */
public class HomeFontFragment extends AbsHomeFragment {

	private static final String TAG = "HomeFontFragment";
	private MultipleListAdapter mAdapter;
	private HomeHeaderListAdapter mHeaderAdapter;
	private MultipleListAdapter mBodyAdapter;
	public HomeFontFragment(){}
	public HomeFontFragment(CharSequence title) {
		super(title);
		// TODO Auto-generated constructor stub

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);

//		getPresenter().requestTheme(new HomeThemeHeaderRequest(getContext(), Theme.FONTS));
//		getPresenter().requestTheme(new HomeThemeRequest(getContext(), Theme.FONTS));

	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onViewCreated(view, savedInstanceState);
		mAdapter = new MultipleListAdapter(getContext());
		mHeaderAdapter = new HomeHeaderListAdapter(getContext());
		mHeaderAdapter.setThemeType(Theme.FONTS);
		mBodyAdapter = new MultipleListAdapter(getContext());
		mBodyAdapter.setTypeCount(3);
		mAdapter.addAdapter(mHeaderAdapter);
		mAdapter.addAdapter(mBodyAdapter);
		getListView().setAdapter(mAdapter);
	}


	@Override
	public void update(Response result) {
		// TODO Auto-generated method stub
		if(Config.DEBUG) Log.d(TAG, "updateThemeList->"+result.toString());
		Context context = getContext();
		if(context == null) return;
		boolean hasMore = true;
		showLoadingView(false);
		if(result instanceof HomeThemeHeaderResponse){
			if(Config.DEBUG) Log.d(TAG, "updateThemeList->"+1);
			HomeThemeHeaderCategory header = new HomeThemeHeaderCategory((HomeThemeHeaderResponse)result);
			mHeaderAdapter.setAdvertisings(header.getBanners());
		}else if(result instanceof HomeThemeBodyResponse){
			if(Config.DEBUG) Log.d(TAG, "updateThemeList->"+2);
			if(isRefresh()){
				mBodyAdapter.clearAdapter();
				setRefresh(false);
			}
			HomeThemeBodyResponse response = (HomeThemeBodyResponse)result;
			mCurrentPage = response.body.getPageNum();
			List<HomeThemeCategory> categories = response.getThemes(Theme.FONTS);
			if(categories != null && categories.size() > 0){
				for(HomeThemeCategory c : categories){
					HomeFontsListAdapter fontAdapter = new HomeFontsListAdapter(context);
					fontAdapter.setHeaderTitle(c.getName());
					List<Theme> originList = c.getThemes();
					if(originList.size() > 6){
						fontAdapter.setData(originList.subList(0, 6));
					}else{
						fontAdapter.setData(originList);
					}
					mBodyAdapter.addAdapter(fontAdapter);
				}
			}else{
				if(mCurrentPage == 0){
					showEmptyView(true);
				}
				hasMore = false;
			}
		}
		sendMessage(COMPLETE_REQUEST,hasMore);
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void showNetworkErrorView(boolean show){
		TLog.d(TAG,"showNetworkError has adapter->"+(mAdapter != null));
		if(mAdapter != null){
			boolean hasData = mBodyAdapter.getCount() > 0 || mHeaderAdapter.getCount() > 0;
			if(hasData){
				showNetworkErrorPanel(true);
			}else {
				setState(EMPTY_STATE_NO_NETWORK);
			}
		}else{
			setState(EMPTY_STATE_NO_NETWORK);
		}

	}

	@Override
	public void onResume() {
		super.onResume();
		if(mAdapter != null){
			mAdapter.notifyDataSetChanged();
		}
	}

	protected boolean hasData(){
		if(mBodyAdapter == null || mHeaderAdapter == null){
			return false;
		}
		return mBodyAdapter.getCount() > 0 || mHeaderAdapter.getCount() > 0;
	}

	public void showEmptyView(boolean show){
		sendMessage(COMPLETE_REQUEST,false);
	}

	@Override
	protected AbsHomeThemeListAdapter createAdapter() {
		return null;
	}

	@Override
	protected int getThemeType() {
		return Theme.FONTS;
	}
}

