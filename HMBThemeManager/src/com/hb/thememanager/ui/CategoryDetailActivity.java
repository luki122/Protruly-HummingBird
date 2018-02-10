package com.hb.thememanager.ui;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.HbSearchView;

import com.hb.thememanager.R;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.model.Tab;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.ui.adapter.ThemePagerAdapter;
import com.hb.thememanager.ui.fragment.CategoryDetailFragment;
import com.hb.thememanager.ui.fragment.RankingFragment;
import com.hb.thememanager.utils.Config;

import java.util.ArrayList;
import java.util.List;

import hb.widget.ViewPager;
import hb.widget.tab.TabLayout;
import hb.widget.toolbar.Toolbar;

public class CategoryDetailActivity extends SecondActivity implements SimpleRequestView{

	private Toolbar mToolbar;
	private TabLayout mTab;
	private ViewPager mTabPager;
	private HbSearchView mSearchView;
	private ThemePagerAdapter mAdapter;
	private List<Fragment> mFragments;
	private String mTitleString;
	private int mType;
	private int mCategoryId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setHbContentView(R.layout.activity_ranking_page);

		initialUI(savedInstanceState);
	}


	protected void initialUI(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		mToolbar = getToolbar();
//		mToolbar.inflateMenu(R.menu.menu_search);
		mTab = (TabLayout)findViewById(R.id.tab_layout);
		mTabPager = (ViewPager)findViewById(R.id.view_pager);
		setupPager();


		Intent intent = getIntent();
		mType = intent.getIntExtra(Config.ActionKey.KEY_FAST_ENTRY,0);
		mCategoryId = intent.getIntExtra(Config.ActionKey.KEY_CATEGORY_ID,0);
		mTitleString = intent.getStringExtra(Config.ActionKey.KEY_JUMP_TITLE);
		List<Tab> tags = new ArrayList<>();
		switch (mType){
			case Theme.THEME_PKG:
				Tab tab = new Tab();
				tab.type = Tab.CHARG;
				tab.title = getResources().getString(R.string.tab_title_charge);
				tags.add(tab);

				tab = new Tab();
				tab.type = Tab.FREE;
				tab.title = getResources().getString(R.string.tab_title_free);
				tags.add(tab);

				tab = new Tab();
				tab.type = Tab.NEW;
				tab.title = getResources().getString(R.string.tab_title_new);
				tags.add(tab);
				break;
			case Theme.RINGTONE:
				break;
			case Theme.WALLPAPER:
				tab = new Tab();
				tab.type = Tab.WALLPAPER_HOT;
				tab.title = getResources().getString(R.string.tab_title_hot);

				tags.add(tab);
				tab = new Tab();
				tab.type = Tab.WALLPAPER_NEW;
				tab.title = getResources().getString(R.string.tab_title_new);
				tags.add(tab);
				break;
			case Theme.FONTS:
				tab = new Tab();
				tab.type = Tab.CHARG;
				tab.title = getResources().getString(R.string.tab_title_charge);
				tags.add(tab);

				tab = new Tab();
				tab.type = Tab.FREE;
				tab.title = getResources().getString(R.string.tab_title_free);
				tags.add(tab);

				tab = new Tab();
				tab.type = Tab.NEW;
				tab.title = getResources().getString(R.string.tab_title_new);
				tags.add(tab);
				break;
		}

		initFragment(tags);
		setupPager();
	}

	private void initFragment(List<Tab> tabs){
		//初始化ViewPager的数据集
		mFragments = new ArrayList<Fragment>();
		//初始化TabLayout的title
		for(Tab t : tabs){
			mTab.addTab(mTab.newTab());
			mFragments.add(new CategoryDetailFragment(t.title, t.type, mType, mCategoryId));
		}
		//创建ViewPager的adapter
		mAdapter = new ThemePagerAdapter(getFragmentManager(), mFragments);
		mTabPager.setAdapter(mAdapter);
		//千万别忘了，关联TabLayout与ViewPager
		//同时也要覆写PagerAdapter的getPageTitle方法，否则Tab没有title
		mTab.setupWithViewPager(mTabPager);
	}
	
	/**
	 * Gets global SearchView
	 * @return
	 */
	public HbSearchView getSearchView(){
		if(mSearchView == null){
			mSearchView = (HbSearchView)mToolbar.getMenu().findItem(R.id.search).getActionView();
		}
		
		return mSearchView;
	}


	@Override
	public void onNavigationClicked(View view) {
		onBackPressed();

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onDestroy(){
		super.onDestroy();
//		BannerManager.getInstance().releaseBanner(getComponentName().getClassName());
	}
	
	
	private void setupPager(){
		//设置标题
		setTitle(mTitleString);
	}

	@Override
	public void showToast(String msg) {

	}

	@Override
	public void update(Response result) {

	}

	@Override
	public void showRequestFailView(boolean show) {

	}

	@Override
	public void showEmptyView(boolean show) {

	}

	@Override
	public void showNetworkErrorView(boolean show) {

	}
}
