package com.android.packageinstaller.permission.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import hb.app.HbActivity;
import hb.widget.PagerAdapter;
import hb.widget.ViewPager;
import hb.widget.tab.TabLayout;
import hb.widget.toolbar.Toolbar;

import android.os.SystemProperties;

import com.android.packageinstaller.R;

/**
 * Created by xiaobin on 17-7-3.
 */

public class HMBManagePermissionsActivity extends HbActivity {

    static final String TAG = "PermControlPageActivity";

    protected Toolbar mToolbar;

    private static final int PERMISSIONS_INFO = 0;
    private static final int APPS_INFO = 1;
    private static final int NUM_TABS = 2;

    private TabLayout tabLayout;
    private ViewPager mTabPager;
    private TabPagerAdapter mTabPagerAdapter;
    //    private ActionBarAdapter mActionBarAdapter;
    private final TabPagerListener mTabPagerListener = new TabPagerListener();

    final String mPermissionsTag = "tab-pager-perms";
    final String mAppsTag = "tab-pager-apps";
    private HMBManagePermissionsFragment mPermissionsFragment;
    private HMBManageAppsFragment mAppsFragment;

    private FrameLayout mEmptyView;

    private class TabPagerAdapter extends PagerAdapter {
        private final FragmentManager mFragmentManager;
        private FragmentTransaction mCurTransaction = null;
        private Fragment mCurrentPrimaryItem;

        public TabPagerAdapter() {
            mFragmentManager = getFragmentManager();
        }

        @Override
        public int getCount() {
            return NUM_TABS;
        }

        /** Gets called when the number of items changes. */
        @Override
        public int getItemPosition(Object object) {
            if (object == mPermissionsFragment) {
                return PERMISSIONS_INFO;
            }

            if (object == mAppsFragment) {
                return APPS_INFO;
            }
            return POSITION_NONE;
        }

        @Override
        public void startUpdate(View container) {
        }

        private Fragment getFragment(int position) {
            if (position == PERMISSIONS_INFO) {
                return mPermissionsFragment;
            } else if (position == APPS_INFO) {
                return mAppsFragment;
            }

            throw new IllegalArgumentException("position: " + position);
        }

        @Override
        public Object instantiateItem(View container, int position) {
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            Fragment f = getFragment(position);
            return f;
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
        }

        @Override
        public void finishUpdate(View container) {
            if (mCurTransaction != null) {
                mCurTransaction.commitAllowingStateLoss();
                mCurTransaction = null;
                mFragmentManager.executePendingTransactions();
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return ((Fragment) object).getView() == view;
        }

        @Override
        public void setPrimaryItem(View container, int position, Object object) {
            Fragment fragment = (Fragment) object;
            if (mCurrentPrimaryItem != fragment) {
                if (mCurrentPrimaryItem != null) {
                    mCurrentPrimaryItem.setUserVisibleHint(false);
                }
                if (fragment != null) {
                    fragment.setUserVisibleHint(true);
                }
                mCurrentPrimaryItem = fragment;
            }
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == PERMISSIONS_INFO) {
                return getString(R.string.tab_permissions);
            } else if (position == APPS_INFO) {
                return getString(R.string.tab_apps);
            }
            return "";
        }
    }


    private class TabPagerListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.hmb_activity_manage_permissions);

        mToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        mToolbar.setNavigationIcon(com.hb.R.drawable.ic_toolbar_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mEmptyView = (FrameLayout) findViewById(R.id.empty_view);

        // hide fragment firstly , then update it in onResume() according to switch status
        final FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        mPermissionsFragment = (HMBManagePermissionsFragment) fragmentManager.findFragmentByTag(mPermissionsTag);
        mAppsFragment = (HMBManageAppsFragment) fragmentManager.findFragmentByTag(mAppsTag);

        if (mPermissionsFragment == null) {
            mPermissionsFragment = HMBManagePermissionsFragment.newInstance();
            mAppsFragment = HMBManageAppsFragment.newInstance();
            transaction.add(R.id.tab_pager, mPermissionsFragment, mPermissionsTag);
            transaction.add(R.id.tab_pager, mAppsFragment, mAppsTag);
        }

        transaction.hide(mPermissionsFragment);
        transaction.hide(mAppsFragment);
        transaction.commit();
        fragmentManager.executePendingTransactions();

        tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        // set page adapter
        mTabPager = (ViewPager) findViewById(R.id.tab_pager);
        mTabPagerAdapter = new TabPagerAdapter();
        mTabPager.setAdapter(mTabPagerAdapter);
        mTabPager.setOnPageChangeListener(mTabPagerListener);

        tabLayout.setupWithViewPager(mTabPager);
        tabLayout.setTabsFromPagerAdapter(mTabPagerAdapter);

        addUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void onNavigationClicked(View view) {
        //在这里处理Toolbar上的返回按钮的点击事件
        onBackPressed();
    }

    protected void addUI() {
        // must get a new transaction each time
        FragmentTransaction transaction = getFragmentManager()
                .beginTransaction();
        // set empty view to gone
        mEmptyView.setVisibility(View.GONE);
        // add all the fragment
        mPermissionsFragment = (HMBManagePermissionsFragment) getFragmentManager()
                .findFragmentByTag(mPermissionsTag);
        mAppsFragment = (HMBManageAppsFragment) getFragmentManager().findFragmentByTag(
                mAppsTag);

        if (mPermissionsFragment == null) {
            mPermissionsFragment = HMBManagePermissionsFragment.newInstance();
            mAppsFragment = HMBManageAppsFragment.newInstance();
            transaction.add(R.id.tab_pager, mPermissionsFragment, mPermissionsTag);
            transaction.add(R.id.tab_pager, mAppsFragment, mAppsTag);
        }
        transaction.show(mPermissionsFragment);
        transaction.show(mAppsFragment);

        transaction.commit();

        getFragmentManager().executePendingTransactions();
        tabLayout.setVisibility(View.VISIBLE);
    }

    protected void removeUI() {
        FragmentTransaction transaction = getFragmentManager()
                .beginTransaction();
        mEmptyView.setVisibility(View.VISIBLE);
        mPermissionsFragment = (HMBManagePermissionsFragment) getFragmentManager()
                .findFragmentByTag(mPermissionsTag);

        if (mPermissionsFragment != null) {
            transaction.hide(mPermissionsFragment);
            transaction.hide(mAppsFragment);
        }
        transaction.commit();
        getFragmentManager().executePendingTransactions();
        tabLayout.setVisibility(View.GONE);
    }

}
