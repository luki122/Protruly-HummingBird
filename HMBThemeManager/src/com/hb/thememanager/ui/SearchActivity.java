package com.hb.thememanager.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.HbSearchView;
import hb.app.HbActivity;

import com.hb.thememanager.R;
import com.hb.thememanager.manager.SearchHistoryManager;
import com.hb.thememanager.ui.fragment.HomeRingTongFragment;
import com.hb.thememanager.ui.fragment.OnSearchItemClickListener;
import com.hb.thememanager.ui.fragment.SearchAssistFragment;
import com.hb.thememanager.ui.fragment.SearchHistoryFragment;
import com.hb.thememanager.ui.fragment.SearchResultFragment;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.FileUtils;
import com.hb.thememanager.utils.StringUtils;

import android.content.res.HbFontsManager;
import android.content.res.HbConfiguration;

public class SearchActivity extends SecondActivity implements HbSearchView.OnQueryTextListener,View.OnClickListener{
	private static final String TAG = "SearchActivity";
	private static final int MAX_HISTORY_SIZE = 10;

	private static final int SEARCH_HISTORY = 0;
	private static final int SEARCH_ASSIST = 1;
	private static final int SEARCH_RESULT = 2;

	private HbSearchView mSearchView;
	private FragmentManager mFragmentManager;
	private SparseArray<Fragment> mFragments;
	private Fragment currentFragment;

	private OnSearchItemClickListener mOnSearchItemClickListener = new OnSearchItemClickListener() {
		@Override
		public void onItemClick(String str) {
			mSearchView.setQuery(str, true);
		}
	};

	@Override
	protected void onCreate(Bundle arg0) {
		// TODO Auto-generated method stub
		super.onCreate(arg0);
		setHbContentView(R.layout.activity_search);

		getToolbar().inflateMenu(R.menu.menu_search);
		initialSearchView();

		mFragmentManager = getFragmentManager();
		initFragments();

		showHistory();
	}

	@Override
	protected void onResume() {
		super.onResume();
		showInputMethod();
	}

	@Override
	protected void onPause() {
		super.onPause();
		hideInputMethod();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mFragments.clear();
	}

	private void initialSearchView(){
		mSearchView = (HbSearchView)getToolbar().getMenu().findItem(R.id.search).getActionView();
		mSearchView.setSubmitButtonEnabled(true);
		mSearchView.setIconifiedByDefault(false);
		mSearchView.setOnQueryTextListener(this);
		mSearchView.setOnSearchClickListener(this);
	}

	private void initFragments(){
		if(mFragments == null) {
			mFragments = new SparseArray<>();
		}
		createFragment(SEARCH_HISTORY);
		createFragment(SEARCH_ASSIST);
		createFragment(SEARCH_RESULT);
	}

	private Fragment getFragment(int id){
		return mFragments.get(id);
	}

	private Fragment createFragment(int id){
		Fragment dropFragment = mFragmentManager.findFragmentByTag(String.valueOf(id));
		if(dropFragment != null){
			mFragmentManager.beginTransaction().remove(dropFragment).commit();
		}
		Fragment f = null;
		switch (id) {
			case SEARCH_HISTORY:
				f = new SearchHistoryFragment();
				((SearchHistoryFragment)f).setOnSearchItemClickListener(mOnSearchItemClickListener);
				break;

			case SEARCH_ASSIST:
				f = new SearchAssistFragment();
				((SearchAssistFragment)f).setOnSearchItemClickListener(mOnSearchItemClickListener);
				break;

			case SEARCH_RESULT:
				f = new SearchResultFragment();
				break;
		}
		if(f != null) {
			mFragmentManager.beginTransaction().add(R.id.search_result_panel,f,String.valueOf(id)).hide(f).commit();
			mFragments.put(id, f);
		}
		return f;
	}

	private void removeFragment(int id){
		mFragmentManager.beginTransaction().remove(mFragments.get(id)).commit();
		mFragments.remove(id);
	}

	private void showResult(String search){
		Fragment resultFragment = getFragment(SEARCH_RESULT);
		showFragment(resultFragment);
		if(resultFragment instanceof SearchResultFragment && resultFragment.isAdded()){
			((SearchResultFragment)resultFragment).search(search);
		}
	}

	private void showHistory(){
		Fragment f = getFragment(SEARCH_HISTORY);
		showFragment(f);
		if(f instanceof SearchHistoryFragment && f.isAdded()){
			((SearchHistoryFragment)f).initHistory();
		}
	}

	private void showAssist(String search){
		Fragment f = getFragment(SEARCH_ASSIST);
		showFragment(f);
		if(f instanceof SearchAssistFragment){
			((SearchAssistFragment)f).search(search);
		}
	}

	private void showFragment(Fragment f){
		if(currentFragment != null){
			mFragmentManager.beginTransaction().hide(currentFragment).commit();
		}
		if(f != null){
			mFragmentManager.beginTransaction().show(f).commit();
			currentFragment = f;
		}
	}

	private void saveHistory(String history){
		SearchHistoryManager manager = SearchHistoryManager.getInstance();
		if(!manager.has(this,history)) {
			if(manager.size(this) >= MAX_HISTORY_SIZE){
				manager.remove(this, 0);
			}
			manager.append(this, history);
		}
	}

	@Override
	public void onBackPressed() {
		if (currentFragment instanceof SearchResultFragment) {
			Fragment ringtone = ((SearchResultFragment)currentFragment).getCurrentFragment();
			if(ringtone instanceof HomeRingTongFragment) {
				if (((HomeRingTongFragment)ringtone).mWebView.canGoBack()) {
					((HomeRingTongFragment)ringtone).mWebView.goBack();
					return;
				}
			}
		}
		super.onBackPressed();
	}

	@Override
	public boolean onQueryTextChange(String query) {
		// TODO Auto-generated method stub
		if(StringUtils.isEmpty(query)){
			showHistory();
		}else{
			showAssist(query);
		}
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onQueryTextSubmit : query = "+query);
		showResult(query);
		saveHistory(query);
		return false;
	}

	@Override
	public void onClick(View v){

	}

}
