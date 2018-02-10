package com.protruly.music.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;

import com.protruly.music.R;
import com.protruly.music.adapter.HBPlayerPagerAdapter;
import com.protruly.music.util.Globals;
import com.protruly.music.util.LogUtil;
import com.protruly.music.widget.HBViewPager;

import java.util.ArrayList;
import java.util.List;

import hb.app.HbActivity;

/**
 * Created by hujianwei on 17-9-4.
 */

public class HBHomeFragment extends Fragment {

    private static final String TAG = "HBHomeFragment";
    private HBMyMusicFragment myMusicfrag = null;
    private HBViewPager mViewPager = null;
    private OnMainPageChangeListener mOnMainPageChangeListener;
    private HBFindMusicFragment mHBFindMusicFragment = null;
    private View findMusicView, findMainMusic;
    private LayoutInflater inflater;
    private ViewStub stubView;

    public void setOnMainPageChangeListener(OnMainPageChangeListener l) {
        mOnMainPageChangeListener = l;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        List<View> mViews = new ArrayList<View>();
        inflater = LayoutInflater.from(getActivity());
        View myMusicView = inflater.inflate(R.layout.hb_mymusic_fragement, null);
        mViews.add(myMusicView);
        
        // 发现歌曲页面是否隐藏
        if (Globals.SWITCH_FOR_ONLINE_MUSIC) {
            findMusicView = inflater.inflate(R.layout.hb_findmusic_main, null);
            stubView = (ViewStub) findMusicView.findViewById(R.id.find_music_stub);
            mViews.add(findMusicView);
        }
        myMusicfrag = new HBMyMusicFragment();
        myMusicfrag.initview(myMusicView, getActivity());
        HBPlayerPagerAdapter mPagerAdapter = new HBPlayerPagerAdapter(mViews);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setCurrentItem(0);
        mViewPager.setOnPageChangeListener(mPageChangeListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.hb_fragment_home_page, container, false);
        mViewPager = (HBViewPager) view.findViewById(R.id.id_container);
        return view;

    }

    private ViewPager.OnPageChangeListener mPageChangeListener = new ViewPager.OnPageChangeListener() {
        private int mPosition = 0;

        @Override
        public void onPageSelected(int position) {
            mPosition = position;
            if (mOnMainPageChangeListener != null) {
                mOnMainPageChangeListener.onPageSelected(position);
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (mOnMainPageChangeListener != null) {
                mOnMainPageChangeListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            LogUtil.d(TAG, "onPageScrollStateChanged " + state + " mPosition:" + mPosition);
            if (state == 0) {
                setOnlineMusic(mPosition);
            }
        }
    };

    public void setCurrentPage(int item) {
        if (mViewPager != null) {
            mViewPager.setCurrentItem(item);
            setOnlineMusic(item);
        }
    }

    @Override
    public void onPause() {
        myMusicfrag.onPause();
        if (mHBFindMusicFragment != null)
            mHBFindMusicFragment.onPause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        myMusicfrag.onResume();
        if (mHBFindMusicFragment != null)
            mHBFindMusicFragment.onResume();
    }

    public interface OnMainPageChangeListener {
        public void onPageSelected(int position);

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels);
    }

    public void setOnlineMusic(int position) {
        if (findMusicView == null)
            return;
        if (position == 1) {
            LogUtil.d(TAG, "setOnlineMusic HBFindMusicFragment:" + mHBFindMusicFragment);
            if (mHBFindMusicFragment == null) {
                mHBFindMusicFragment = new HBFindMusicFragment();
                if (findMainMusic == null) {
                    findMainMusic = stubView.inflate();
                }
                mHBFindMusicFragment.initview(findMainMusic, (HbActivity) getActivity(), mViewPager);
            }
            mHBFindMusicFragment.isLoadData();

        }
    }



    public void onMediaDbChange() {
        if (myMusicfrag != null)
            myMusicfrag.notifiData();
    }

    // add by tangjie 2014/07/30 start
	/*public void changeButton(int type) {
		if (mHBFindMusicFragment != null) {
			mHBFindMusicFragment.changeButton(type);
		}
	}*/

    public void setPlayAnimation() {
        if (mHBFindMusicFragment != null)
            mHBFindMusicFragment.setPlayAnimation();
    }

    @Override
    public void onDestroy() {

        if (mHBFindMusicFragment != null) {
            mHBFindMusicFragment.destroy();
        }
        if (myMusicfrag != null) {
            myMusicfrag.destroy();
        }
        super.onDestroy();
    }

    // add by end

    public void hideSearchviewLayout() {
        if (mHBFindMusicFragment != null) {
            mHBFindMusicFragment.hideSearchviewLayout();
        }
    }

    public boolean isSearchBack() {
        if (mHBFindMusicFragment != null) {
            View layout = mHBFindMusicFragment.getSearchviewLayout();
            if (layout != null && layout.getVisibility() == View.VISIBLE) {
                return true;
            }
        }
        return false;
    }

}
