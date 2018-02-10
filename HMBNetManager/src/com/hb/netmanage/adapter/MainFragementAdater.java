package com.hb.netmanage.adapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Parcelable;
import android.view.ViewGroup;

import com.hb.netmanage.utils.LogUtil;

import java.util.ArrayList;

import hb.widget.PagerAdapter;


/**
 * sim1/sim2之间切换
 * 
 * @author zhaolaichao
 */
public class MainFragementAdater extends hb.widget.FragmentStatePagerAdapter {

	private ArrayList<Fragment> mFragments = new ArrayList<>();
	private FragmentManager mFm;
	private Context mContext;
    private String[] mTitles;
	public MainFragementAdater(Context context, FragmentManager fm) {
		super(fm);
		mFm = fm;
		mContext = context;
	}

	public MainFragementAdater(Context context, FragmentManager fm, ArrayList<Fragment> mFragments, String[] mTitles) {
		super(fm);
		mFm = fm;
		mContext = context;
		this.mFragments = mFragments;
		this.mTitles = mTitles;
	}

	public String[] getmTitles() {
		return mTitles;
	}

	public void setmTitles(String[] mTitles) {
		this.mTitles = mTitles;
	}

	public void setmFragments(ArrayList<Fragment> fragments) {
		mFragments.clear();
		this.mFragments.addAll(fragments);
	}

	@Override
	public int getItemPosition(Object object) {
		return PagerAdapter.POSITION_NONE;
	}


	@Override
	public Fragment getItem(int position) {
		if (position < mFragments.size()) {

			return mFragments.get(position);
		}
		return null;
	}

	@Override
	public int getCount() {
		return mFragments.size();
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		try {
			LogUtil.e("instantiateItem", "开始加载viewpager");
			Fragment fragment = (Fragment) super.instantiateItem(container, position);
//		Fragment updateFragment = mFragments.get(position);
//		Bundle bundle = updateFragment.getArguments();
//		fragment.getArguments().clear();
//		fragment.getArguments().putAll(bundle);
			return fragment;
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		LogUtil.e("destroyItem", "destroyItem");
		try {
			super.destroyItem(container, position, object);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}

	@Override
	public CharSequence getPageTitle(int position) {
		if (mTitles != null) {
			return mTitles[position];
		}
		return super.getPageTitle(position);
	}

	@Override
	public void notifyDataSetChanged() {
		try {
			if (mFm == null || mContext == null) {
				return;
			}
			super.notifyDataSetChanged();
		}catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void restoreState(Parcelable state, ClassLoader loader) {
		//super.restoreState(state, loader);
	}

	public void clearFragments() {
		if (this.mFragments != null && mFm != null) {
			FragmentTransaction ft = mFm.beginTransaction();
			for (Fragment f : this.mFragments) {
				ft.remove(f);
			}
			ft.commit();
			mFm.executePendingTransactions();
		}
	}
//
//	public void updateFragments(ArrayList<Fragment>  fragments) {
//		if (fragments != null) {
//			//mFm.executePendingTransactions();
//			this.mFragments = fragments;
//			notifyDataSetChanged();
//		}
//	}
}
