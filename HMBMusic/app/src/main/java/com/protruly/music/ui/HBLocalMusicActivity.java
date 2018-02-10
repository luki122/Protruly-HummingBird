package com.protruly.music.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.TextView;

import com.protruly.music.R;
import com.protruly.music.widget.HBNoScrollViewPager;

import hb.widget.PagerAdapter;
import hb.widget.ViewPager;
import hb.widget.tab.TabLayout;
import hb.widget.toolbar.Toolbar;

/**
 * Created by xiaobin on 17-9-14.
 */

public class HBLocalMusicActivity extends AbstractBaseActivity {

    protected Toolbar mToolbar;

    private static final int MUSIC_INFO = 0;
    private static final int ARTIST_INFO = 1;
    private static final int NUM_TABS = 2;

    private TabLayout tabLayout;
    private HBNoScrollViewPager mTabPager;
    private TabPagerAdapter mTabPagerAdapter;
    private TextView tv_music_count;
    private final TabPagerListener mTabPagerListener = new TabPagerListener();

    final String mMusicTag = "tab-pager-music";
    final String mArtistTag = "tab-pager-artist";
    private HBMusicTagFragment musicTagFragment;
    private HBArtistTagFragment artistTagFragment;

    private int musicCount = 0;
    private long artistCount = 0;
    private long sizeCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_local_music);

        // hide fragment firstly , then update it in onResume() according to switch status
        final FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        musicTagFragment = (HBMusicTagFragment) fragmentManager.findFragmentByTag(mMusicTag);
        artistTagFragment = (HBArtistTagFragment) fragmentManager.findFragmentByTag(mArtistTag);

        if (musicTagFragment == null) {
            musicTagFragment = HBMusicTagFragment.newInstance();
            artistTagFragment = HBArtistTagFragment.newInstance();
            transaction.add(R.id.tab_pager, musicTagFragment, mMusicTag);
            transaction.add(R.id.tab_pager, artistTagFragment, mArtistTag);
        }

        transaction.commit();
        fragmentManager.executePendingTransactions();

        tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        // set page adapter
        mTabPager = (HBNoScrollViewPager) findViewById(R.id.tab_pager);
        mTabPagerAdapter = new TabPagerAdapter();
        mTabPager.setAdapter(mTabPagerAdapter);
        mTabPager.setOnPageChangeListener(mTabPagerListener);

        tabLayout.setupWithViewPager(mTabPager);
        tabLayout.setTabsFromPagerAdapter(mTabPagerAdapter);

        tv_music_count = (TextView) findViewById(R.id.tv_music_count);
        updateLocalMusicCountUI();
    }

    @Override
    public void onMediaDbChange(boolean selfChange) {

    }

    public void setMusicCount(int musicCount) {
        this.musicCount = musicCount;
        updateLocalMusicCountUI();
    }

    public void setArtistCount(int artistCount) {
        this.artistCount = artistCount;
        updateLocalMusicCountUI();
    }

    public void setSizeCount(long sizeCount) {
        this.sizeCount = sizeCount;
        updateLocalMusicCountUI();
    }

    public void setMusicCountAndSizeCount(int musicCount, long sizeCount) {
        this.musicCount = musicCount;
        this.sizeCount = sizeCount;
        updateLocalMusicCountUI();
    }

    public void updateLocalMusicCountUI() {
        tv_music_count.setText(getString(R.string.local_music_count, musicCount, artistCount, sizeCount));
    }

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
            if (object == musicTagFragment) {
                return MUSIC_INFO;
            }

            if (object == artistTagFragment) {
                return ARTIST_INFO;
            }
            return POSITION_NONE;
        }

        @Override
        public void startUpdate(View container) {
        }

        private Fragment getFragment(int position) {
            if (position == MUSIC_INFO) {
                return musicTagFragment;
            } else if (position == ARTIST_INFO) {
                return artistTagFragment;
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
            if (position == MUSIC_INFO) {
                return getString(R.string.tag_music);
            } else if (position == ARTIST_INFO) {
                return getString(R.string.tag_artist);
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

}
