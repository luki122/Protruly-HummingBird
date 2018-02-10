package com.hb.thememanager.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hb.thememanager.R;
import com.hb.thememanager.manager.BannerManager;
import com.hb.thememanager.ui.adapter.BannerAdapter;
import com.hb.thememanager.model.Advertising;

import java.util.ArrayList;
import java.util.List;

import hb.widget.ViewPager;


/**
 * 广告轮播view
 *
 */
public class BannerView extends RelativeLayout {
    private static final String TAG = "BannerView";
    private BannerAdapter mAdapter;
    private final static long DEFAULT_DURING = 3000;
    private long mDuring = DEFAULT_DURING;
    private int mCurrent = 0;
    private int mSize = 0;
    private Runnable mRunnable;
    private boolean isBinded = false;
    private boolean isStop = false;
    private boolean showIndicator = true;

    private ViewPager mBanner;
    private LinearLayout mIndicators;

    public BannerView(Context context) {
        super(context);
        init();
    }

    public BannerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){
        mCurrent = 0;
        isBinded = false;

        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.banner_view, this, true);

        mBanner = (ViewPager) findViewById(R.id.banner_pager);
        mIndicators = (LinearLayout) findViewById(R.id.banner_indicator);

        mBanner.setOnPageChangeListener(mListener);

        BannerManager.getInstance().registerBanner(this);
    }

    public boolean bindData(List<Advertising> d){
        if(d != null && d.size() > 0) {
            isBinded = true;
            mSize = d.size();
            if(mSize > 1){
                mBanner.allowScroll(true);
            }else{
                mBanner.allowScroll(false);
            }
            mAdapter = new BannerAdapter(getContext());
            mAdapter.setData(d);
            mBanner.setAdapter(mAdapter);
            initIndicator(mSize);
            setCurrentIndex(1);
            setCurrentPage(1,false);
            return true;
        }
        isBinded = false;
        return false;
    }

    private void initIndicator(int count){
        if(showIndicator) {
            mIndicators.removeAllViews();
            if(count > 1) {
                for (int i = 0; i < count; i++) {
                    TextView indicator = new TextView(getContext());
                    indicator.setBackgroundResource(R.drawable.banner_indicator_background);
                    LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                            getResources().getDimensionPixelOffset(R.dimen.banner_indicator_size),
                            getResources().getDimensionPixelOffset(R.dimen.banner_indicator_size));
                    llp.leftMargin = llp.rightMargin = getResources().getDimensionPixelOffset(R.dimen.banner_indicator_margin) / 2;
                    mIndicators.addView(indicator, llp);
                }
            }
        }
    }

    public int getPageSize(){
        return mSize;
    }

    public void setCurrentPage(int index){
        setCurrentPage(index, true);
    }

    public void setCurrentPage(int index, boolean anim){
        if(isBinded && index >= 0 && index <= mSize + 1){
            mBanner.setCurrentItem(index, anim);
        }
    }

    private void setCurrentIndex(int index){
        if(isBinded && index >= 0 && index <= mSize + 1 && showIndicator) {
            int lastIndex = mCurrent;
            if (lastIndex == mSize + 1) {
                lastIndex = 0;
            }else if(lastIndex == 0){
                lastIndex = mSize - 1;
            }else{
                lastIndex--;
            }
            View lastIndicator = mIndicators.getChildAt(lastIndex);
            if(lastIndicator != null) {
                lastIndicator.setSelected(false);
            }

            int indicatorIndex = index;
            if (indicatorIndex == mSize + 1) {
                indicatorIndex = 0;
            }else if(indicatorIndex == 0){
                indicatorIndex = mSize - 1;
            }else{
                indicatorIndex--;
            }
            View indicator = mIndicators.getChildAt(indicatorIndex);
            if(indicator != null) {
                indicator.setSelected(true);
            }

            mCurrent = index;
        }
    }

    public boolean isBinded(){
        return isBinded;
    }

    public void setDuring(long time){
        mDuring = time;
    }

    public void startScroll(){
        if(mDuring > 0 && isBinded){
            isStop = false;
            if(mRunnable == null){
                mRunnable = new Runnable() {
                    @Override
                    public void run() {
                        setCurrentPage(mCurrent+1);
                        if(!isStop && mSize > 1) {
                            postDelayed(this, mDuring);
                        }
                    }
                };
            }else{
                removeCallbacks(mRunnable);
            }
            postDelayed(mRunnable,mDuring);
        }
    }

    public void stopScroll(){
        isStop = true;
        if(mRunnable != null){
            removeCallbacks(mRunnable);
            mRunnable = null;
        }
    }

    private ViewPager.OnPageChangeListener mListener = new ViewPager.OnPageChangeListener(){
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//            setCurrentIndex(position);
        }

        @Override
        public void onPageSelected(int position) {
            setCurrentIndex(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if(state == ViewPager.SCROLL_STATE_IDLE){
                int position = mBanner.getCurrentItem();
                if(mAdapter.isEnd(position)){
                    setCurrentPage(1,false);
                }else if(position == 0){
                    setCurrentPage(mSize,false);
                }
            }
        }
    };
}
