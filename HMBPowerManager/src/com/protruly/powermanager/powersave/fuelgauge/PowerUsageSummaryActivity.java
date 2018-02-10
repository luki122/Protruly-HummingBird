package com.protruly.powermanager.powersave.fuelgauge;


import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.View;

import com.protruly.powermanager.R;

import java.util.ArrayList;
import java.util.List;

import hb.app.HbActivity;
import hb.widget.FragmentPagerAdapter;
import hb.widget.PagerAdapter;
import hb.widget.ViewPager;
import hb.widget.tab.TabLayout;

public class PowerUsageSummaryActivity extends HbActivity {

    private TabLayout mTabLayout;
    private ViewPager mViewPager;

    private List<String> mTitles;
    private List<Fragment> mFragments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHbContentView(R.layout.activity_power_usage_summary);
        initView();
        initData();
        setAdapter();
    }

    private void initView() {
        getToolbar().setTitle(R.string.power_usage_detail);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mTitles = new ArrayList<>();
        mTitles.add(getString(R.string.power_detail_software));
        mTitles.add(getString(R.string.power_detail_hardware));
        mTabLayout.addTab(mTabLayout.newTab().setText(mTitles.get(0)));
        mTabLayout.addTab(mTabLayout.newTab().setText(mTitles.get(1)));
    }

    private void initData() {
        mFragments = new ArrayList<>();
        addFragment2List();
    }

    private void addFragment2List() {
        for (int i = 0; i < 2; i++) {
            Fragment fragment = new PowerUsageSummaryFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("fragment_key", i);
            fragment.setArguments(bundle);
            mFragments.add(fragment);
        }
    }

    private void setAdapter() {
        PagerAdapter adapter = new PowerUsagePagerAdapter(getFragmentManager());
        mViewPager.setAdapter(adapter);
        mTabLayout.setupWithViewPager(mViewPager);
    }

    class PowerUsagePagerAdapter extends FragmentPagerAdapter {

        PowerUsagePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles.get(position);
        }
    }
}