package com.hb.thememanager.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hb.thememanager.R;
import com.hb.thememanager.model.Tab;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.ui.SearchPresenter;
import com.hb.thememanager.ui.SearchView;
import com.hb.thememanager.ui.adapter.HomePagerAdapter;
import com.hb.thememanager.ui.adapter.SearchResultAdapter;
import com.hb.thememanager.utils.ToastUtils;
import com.hb.thememanager.views.ThemeListView;
import hb.widget.tab.TabLayout;
import hb.widget.ViewPager;

import java.util.ArrayList;
import java.util.List;

/**
 * 主题包Tab内容页面
 *
 */
public class SearchResultFragment extends Fragment {
	private static final String TAG = "SearchResultFragment";

	private ArrayList<Fragment> mFragments;
	private SearchResultAdapter mAdapter;

	private View mContentView;
	private TabLayout mTab;
	private ViewPager mViewPager;


	public SearchResultFragment(){}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		Log.e(TAG, "onActivityCreated");
		setupFragments();
	}

	private void setupFragments(){
		setupFragments(null);
	}
	private void setupFragments(String str){
		initFragments();
		if(!TextUtils.isEmpty(str)) {
			if (mFragments != null) {
				for (Fragment f : mFragments) {
					if (f instanceof SearchResultThemeFragment) {
						((SearchResultThemeFragment) f).setSearchString(str);
					} else if (f instanceof HomeRingTongFragment) {
						((HomeRingTongFragment) f).searchRingtone(str);
					}
				}
			}
		}
		mAdapter = new SearchResultAdapter(getContext(),getChildFragmentManager(), mFragments);
		mViewPager.setAdapter(mAdapter);
		mTab.setupWithViewPager(mViewPager);
		mTab.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager) {

			@Override
			public void onTabReselected(TabLayout.Tab tab) {
				super.onTabReselected(tab);
			}

			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				super.onTabSelected(tab);
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {
				super.onTabUnselected(tab);
				Fragment fragment = mFragments.get(tab.getPosition());
				if(fragment instanceof HomeRingTongFragment && ((HomeRingTongFragment)fragment).mWebView != null){
					((HomeRingTongFragment)fragment).mWebView.loadUrl("javascript:KY.ine.stopPlay()");
				}
			}

		});
	}

	public int getCurrentIndex(){
		return mViewPager == null ? 0 : mViewPager.getCurrentItem();
	}

	public Fragment getCurrentFragment(){
		return getFragment(getCurrentIndex());
	}

	public Fragment getFragment(int index){
		return mFragments.get(index);
	}

	private void initFragments(){
		if(mFragments == null) {
			mFragments = new ArrayList<Fragment>();
			mFragments.add(new SearchResultThemeFragment(Theme.THEME_PKG));
			mFragments.add(new SearchResultThemeFragment(Theme.WALLPAPER));
			mFragments.add(new HomeRingTongFragment("", true));
			mFragments.add(new SearchResultThemeFragment(Theme.FONTS));
		}
	}

	public void search(String str){
		setupFragments(str);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		Log.e(TAG, "onViewCreated");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		Log.e(TAG, "onCreateView");
		if(mContentView == null) {
			mContentView = inflater.inflate(R.layout.fragment_search_result, container, false);
			mTab = (TabLayout) mContentView.findViewById(R.id.tab_layout);
			mViewPager = (ViewPager) mContentView.findViewById(R.id.view_pager);
		}
		return mContentView;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		release();
	}

	private void release(){
		for(int i = 0; i < mTab.getTabCount(); i++){
			mTab.newTab();
		}
		mFragments.clear();
		mViewPager.removeAllViews();
	}
}
