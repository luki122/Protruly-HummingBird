package com.hb.thememanager.ui.adapter;

import android.app.Fragment;
import android.app.FragmentManager;

import com.hb.thememanager.ui.fragment.AbsHomeFragment;
import com.hb.thememanager.ui.fragment.BaseFragment;
import com.hb.thememanager.ui.fragment.RankingFragment;

import java.util.List;

import hb.widget.FragmentStatePagerAdapter;

public class ThemePagerAdapter extends FragmentStatePagerAdapter {
	private List<Fragment> mFragments;

	public ThemePagerAdapter(FragmentManager fm, List<Fragment> fragments) {
		super(fm);
		// TODO Auto-generated constructor stub
		this.mFragments = fragments;
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

	public CharSequence getPageTitle(int position) {
		BaseFragment fm = (BaseFragment) mFragments.get(position);
		
		return fm == null?"":fm.getTitle();
	}

}
