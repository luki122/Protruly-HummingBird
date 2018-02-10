package com.hb.thememanager.ui.adapter;

import java.util.List;

import com.hb.thememanager.ui.fragment.AbsHomeFragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.view.View;
import hb.widget.FragmentStatePagerAdapter;
import hb.widget.PagerAdapter;

public class HomePagerAdapter extends FragmentStatePagerAdapter {
	private List<Fragment> mFragments;

	public HomePagerAdapter(FragmentManager fm, List<Fragment> fragments) {
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

	@Override
	public CharSequence getPageTitle(int position) {
		AbsHomeFragment fm = (AbsHomeFragment) mFragments.get(position);
		
		return fm == null?"":fm.getTitle();
	}

}
