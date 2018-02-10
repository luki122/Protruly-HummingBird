package com.hb.thememanager.ui.fragment.themelist;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.HbSearchView;

import com.hb.thememanager.R;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.model.Tab;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.ui.SecondActivity;
import com.hb.thememanager.ui.SimpleRequestView;
import com.hb.thememanager.ui.adapter.ThemePagerAdapter;
import com.hb.thememanager.ui.fragment.CategoryDetailFragment;
import com.hb.thememanager.ui.fragment.PurchaseRecordFragment;
import com.hb.thememanager.utils.Config;

import java.util.ArrayList;
import java.util.List;

import hb.widget.ViewPager;
import hb.widget.tab.TabLayout;
import hb.widget.toolbar.Toolbar;

public class PurchaseActivity extends SecondActivity implements SimpleRequestView{

	private Toolbar mToolbar;
	private TabLayout mTab;
	private ViewPager mTabPager;
	private HbSearchView mSearchView;
	private ThemePagerAdapter mAdapter;
	private List<Fragment> mFragments;

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
		mTab = (TabLayout)findViewById(R.id.tab_layout);
		mTabPager = (ViewPager)findViewById(R.id.view_pager);

		List<Tab> tags = new ArrayList<Tab>();
		Tab tab= new Tab();
		tab.type = Tab.PURCHASE_THEME;
		tab.title = getResources().getString(R.string.tab_title_theme);
		tags.add(tab);

		tab = new Tab();
		tab.type = Tab.PURCHASE_FONT;
		tab.title = getResources().getString(R.string.tab_title_font);
		tags.add(tab);

		initFragment(tags);
		setupPager();
	}

	private void initFragment(List<Tab> tabs){
		//初始化ViewPager的数据集
		mFragments = new ArrayList<Fragment>();
		//初始化TabLayout的title
		for(Tab t : tabs){
			mTab.addTab(mTab.newTab());
			mFragments.add(new PurchaseRecordFragment(t.title, Tab.CHARG, t.type));
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
		setTitle(R.string.text_purchase_record);
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
