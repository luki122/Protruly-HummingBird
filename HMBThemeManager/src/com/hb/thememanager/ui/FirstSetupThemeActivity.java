package com.hb.thememanager.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hb.imageloader.HbImageLoader;
import com.hb.imageloader.ImageLoaderConfig;
import com.hb.thememanager.R;
import com.hb.thememanager.ThemeManager;
import com.hb.thememanager.ThemeManagerApplication;
import com.hb.thememanager.listener.OnThemeStateChangeListener;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.state.StateManager;
import com.hb.thememanager.state.ThemeState;
import com.hb.thememanager.ui.fragment.themelist.LocalThemeListPresenter;
import com.hb.thememanager.ui.fragment.themelist.ThemeListMVPView;
import com.hb.thememanager.utils.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import hb.app.HbActivity;
import hb.app.dialog.ProgressDialog;
import hb.widget.PagerAdapter;
import hb.widget.ViewPager;
import com.hb.thememanager.views.CenterItemViewPager;
/**
 * Created by alexluo on 17-7-7.
 */

public class FirstSetupThemeActivity extends HbActivity implements ThemeListMVPView,
        ViewPager.OnPageChangeListener, OnThemeStateChangeListener {

    private LocalThemeListPresenter mThemeListPresenter;
    private ThemeManagerApplication mApp;
    private HbImageLoader mImageLoader;
    private ImageLoaderConfig mImageLoaderConfig;
    private int mImageWidth,mImageHeight;
    private CenterItemViewPager mItemPager;
    private ItemPagerAdapter mAdapter;
    private int mItemMargin;
    private int mCurrentItem = -1;
    private Theme mCurrentTheme;
    private TextView mCurrentThemeTitle;
    private ProgressDialog mApplyProgress;
    private ThemeManager mThemeManager;
    private StateManager mState;
    private RelativeLayout mParent;
    private Runnable mStartNextAction = new Runnable() {
        @Override
        public void run() {
            startNextActivity();
        }
    };

    private Handler mHandler = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_setup_theme);
        mParent = (RelativeLayout)findViewById(R.id.view_pager_parent);
        mThemeListPresenter = new LocalThemeListPresenter(getApplicationContext());
        mThemeListPresenter.attachView(this);
        mApp = (ThemeManagerApplication) getApplication();
        mThemeManager = mApp.getThemeManager();
        mApp.loadInternalTheme();
        mImageWidth = getResources().getDimensionPixelSize(R.dimen.first_setup_page_item_width);
        mImageHeight = getResources().getDimensionPixelSize(R.dimen.first_setup_page_scroller_height);
        mItemMargin = getResources().getDimensionPixelOffset(R.dimen.first_setup_page_item_margin);
        mItemPager = (CenterItemViewPager)findViewById(R.id.setup_page_scroller) ;
        mAdapter = new ItemPagerAdapter(getApplicationContext());
        mCurrentThemeTitle = (TextView)findViewById(android.R.id.text2);
        configImageLoader();
        mAdapter.attachImageLoader(mImageLoader);
        mItemPager.setOffscreenPageLimit(3);
        mParent.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return mItemPager.dispatchTouchEvent(motionEvent);
            }
        });
        mItemPager.setOnPageChangeListener(this);
        mItemPager.setAdapter(mAdapter);
        mCurrentTheme = new Theme();
        mCurrentTheme.id = Config.DEFAULT_THEME_ID;
        updateCurrentTheme(mCurrentTheme);
        mState = StateManager.getInstance(getApplicationContext());
        mState.setStateChangeListener(this);
    }


    private void createApplyProgressDialog(){
        if(mApplyProgress == null){
            mApplyProgress = new ProgressDialog(this);
            mApplyProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mApplyProgress.setMessage(getResources().getString(R.string.msg_apply_theme));
            mApplyProgress.setCancelable(false);
            mApplyProgress.setCanceledOnTouchOutside(false);
        }
    }


    public void applyTheme(View view){
        mThemeManager.applyTheme(mCurrentTheme,getApplicationContext(), mState);
    }

    private void configImageLoader(){
        mImageLoader = HbImageLoader.getInstance(getApplicationContext());
        mImageLoaderConfig = new ImageLoaderConfig();
        ImageLoaderConfig.Size size = new ImageLoaderConfig.Size(mImageWidth,mImageHeight);
        mImageLoaderConfig.setDecodeFormat(Bitmap.Config.ARGB_8888);//Default is ARGB_8888
        mImageLoaderConfig.setSize(size);
        mImageLoader.setConfig(mImageLoaderConfig);

    }



    @Override
    public void updateThemeList(Theme theme) {
        if(theme != null) {
            mAdapter.addTheme(theme);
        }

        if(mCurrentItem == -1){
            mCurrentItem = 0;
            mItemPager.setCurrentItem(mCurrentItem);
        }

    }

    @Override
    public void updateThemeLists(List theme) {

    }

    @Override
    public void showTips(int status) {

    }

    @Override
    public void showEmptyView(boolean show) {

    }

    @Override
    public void showNetworkErrorView(boolean show) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mThemeListPresenter != null){
            mThemeListPresenter.onDestory();
        }
        verifyHandlerCallback();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        mCurrentTheme = mAdapter.getCurrentTheme(position);
        if(mCurrentTheme != null){
            updateCurrentTheme(mCurrentTheme);
        }

    }

    private void updateCurrentTheme(Theme theme){

        if(theme.isDefaultTheme()){
            mCurrentThemeTitle.setText(getResources().getString(R.string.default_theme_name));
        }else {
            theme.name = Config.getSystemThemeName(theme.themeFilePath,getApplicationContext());
            mCurrentThemeTitle.setText(theme.name);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }


    @Override
    public void onStateChange(ThemeState state) {
        if(state == ThemeState.STATE_START_APPLY){
            if(mApplyProgress == null){
                createApplyProgressDialog();
            }
            mApplyProgress.show();
        }else if(state == ThemeState.STATE_APPLIED){
            mHandler.post(mStartNextAction);
            prepareStartNextActivity();
        }else if(state == ThemeState.STATE_APPLY_SUCCESS){
            if(mApplyProgress != null && mApplyProgress.isShowing()){
                mApplyProgress.dismiss();
                prepareStartNextActivity();
            }
        }
    }

    private void prepareStartNextActivity(){
        verifyHandlerCallback();
        mHandler.post(mStartNextAction);
    }

    private void verifyHandlerCallback(){
        if(mHandler.hasCallbacks(mStartNextAction)){
            mHandler.removeCallbacks(mStartNextAction);
        }
    }

    private void startNextActivity(){
        Intent next = new Intent("com.android.provision.TRANSFER");
        next.putExtra("from", "theme");
        startActivity(next);
        mApp.getThemeManager().setThemeLoadListener(null);
    }


    static class ItemPagerAdapter extends PagerAdapter{

        private LinkedList<ImageView> mItemViews = new LinkedList<ImageView>();
        private ArrayList<Theme> mThemes = new ArrayList<Theme>();
        private HbImageLoader mImageLoader;
        private Context mContext;
        public ItemPagerAdapter(Context context){
            mContext = context;
        }
        public void addTheme(Theme theme){
            synchronized (mThemes){
                if(!mThemes.contains(theme)){
                    mThemes.add(theme);
                    notifyDataSetChanged();
                }
            }
        }

        public void attachImageLoader(HbImageLoader loader){
            mImageLoader = loader;
        }

        @Override
        public int getCount() {
            return mThemes.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        public Theme getCurrentTheme(int index){
            if(mThemes.size() == 0){
                return null;
            }
            return mThemes.get(index);
        }


        public void setItemWidth(int width){

        }

        public View getItemView(int position){
            return mItemViews.get(position);
        }


        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            ImageView itemView = (ImageView)LayoutInflater.from(mContext).inflate(R.layout.list_item_first_setup_item,null);
            mItemViews.add(itemView);
            Theme theme = mThemes.get(position);

            if(mImageLoader != null){
                if(theme != null){
                    StringBuilder builder = new StringBuilder();
                    builder.append(theme.loadedPath);
                    builder.append(File.separatorChar);
                    builder.append(Config.LOCAL_THEME_PREVIEW_DIR_NAME);
                    String previewPath = builder.toString();
                    File file = new File(previewPath);
                    if(file.exists()){
                        String[] images = file.list();
                        if(images != null){
                            for(String s:images){
                                theme.previewArrays.add(previewPath+s);
                            }
                        }
                    }

                    if(theme.isDefaultTheme()){
                        mImageLoader.loadImage(Config.DEFAULT_THEME_COVER,itemView);
                    }else{
                        if(theme.previewArrays != null && theme.previewArrays.size() > 0) {
                            mImageLoader.loadImage(theme.previewArrays.get(0), itemView);
                        }
                    }
                }
            }

            container.addView(itemView);

            return itemView;
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
        }


    }





}
