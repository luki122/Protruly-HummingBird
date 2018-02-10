package com.hb.thememanager.ui.adapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;

import com.hb.thememanager.ui.fragment.AbsHomeFragment;
import com.hb.thememanager.R;

import java.util.List;

import hb.widget.FragmentStatePagerAdapter;

public class SearchResultAdapter extends FragmentStatePagerAdapter {
	private List<Fragment> mFragments;
	private Context mContext;

	public SearchResultAdapter(Context context, FragmentManager fm, List<Fragment> fragments) {
		super(fm);
		// TODO Auto-generated constructor stub
		this.mFragments = fragments;
		mContext = context;
	}

	@Override
	public Fragment getItem(int position) {
		// TODO Auto-generated method stub
		return mFragments.get(position);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mFragments == null ? 0 : mFragments.size();
	}

	@Override
	public CharSequence getPageTitle(int position) {
		switch (position){
			case 0:
				return mContext.getResources().getString(R.string.tab_title_theme);
			case 1:
				return mContext.getResources().getString(R.string.tab_title_wallpaper);
			case 2:
				return mContext.getResources().getString(R.string.tab_title_ringtong);
			case 3:
				return mContext.getResources().getString(R.string.tab_title_font);
		}
		return "";
	}

}
