package com.hb.thememanager.ui.fragment.themedetail;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.job.loader.ImageLoader;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.IntentUtils;
import com.hb.thememanager.views.ExpandableTextView;
import com.hb.thememanager.views.ThemePreviewDonwloadButton;
import com.hb.thememanager.R;
import hb.app.dialog.ProgressDialog;
import hb.widget.toolbar.Toolbar;

/**
 * Created by alexluo on 17-8-10.
 */

public class LocalThemeDetailFragment extends AbsDetailFragment implements View.OnClickListener{
    protected int mPreviewImageWidth;
    protected int mPreviewImageHeight;
    protected int mImageLeftMargin;
    private boolean mIsSystemTheme;
    private Toolbar mToolbar;
    private Runnable mUpdateThemeInfoAction = new Runnable() {
        @Override
        public void run() {
            updateThemeInfo(mCurrentTheme);
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        Bundle args = getBundle();
        if(args != null){
            mCurrentTheme = args.getParcelable(Config.ActionKey.KEY_THEME_PKG_DETAIL);

        }
        mIsSystemTheme = mCurrentTheme.isSystemTheme();
        mPreviewImageWidth = getResources().getDimensionPixelSize(R.dimen.theme_detail_preview_img_width);
        mPreviewImageHeight = getResources().getDimensionPixelSize(R.dimen.theme_detail_preview_img_height);
        mImageLeftMargin = getResources().getDimensionPixelSize(R.dimen.theme_detail_preview_img_margin_left);

    }

    protected void initView() {
        // TODO Auto-generated method stub
        mPreviewScroller = (LinearLayout)findViewById(R.id.theme_pkg_detail_preview_scroller);
        mDesigner = (TextView)findViewById(R.id.theme_detail_designer);
        mThemeSize = (TextView)findViewById(R.id.theme_detail_size);
        mThemeName = (TextView)findViewById(R.id.theme_detail_name);
        mOptionBtn = (ThemePreviewDonwloadButton)findViewById(R.id.theme_detail_option_btn);
        mOptionBtn.setTheme(mCurrentTheme);
        if(hasOnlinePage()){
            mToolbar = getToolbar();
            if(mToolbar != null){
                mToolbar.getMenu().clear();
                mToolbar.inflateMenu(R.menu.menu_go_to_online);
                mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if(item.getItemId() == R.id.go_to_online){
                            Intent intent = IntentUtils.buildHomeThemeListIntent(Config.Action.ACTION_HOME_THEME_LIST_ITEM_DETAIL, mCurrentTheme);
                            getContext().startActivity(intent);
                        }
                        return false;
                    }
                });
            }
        }
        new Handler().post(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                getPresenter().loadThemePreview();
            }
        });
    }

    private boolean hasOnlinePage(){
        return mCurrentTheme.isSystemTheme == Theme.LOCAL_THEME;
    }


    @Override
    public void onPause() {
        super.onPause();
        mOptionBtn.onPausedTimer();
    }

    @Override
    public void onResume() {
        super.onResume();
        mOptionBtn.onResumeTimer();
    }



    @Override
    public void updateThemeInfo(Theme theme) {
        if(theme != null){
            mThemeName.setText(theme.name);
            mDesigner.setText(theme.designer);
            mThemeSize.setText(theme.size);
            createPreview(theme);
        }
    }

    private  void createPreview(Theme theme){

        int imagesCount = theme.previewArrays.size();
        if(theme.isDefaultTheme()){
            imagesCount = Config.DEFAUTL_THEME_PREVIEWS.length;
        }
        if(imagesCount == 0){
            return;
        }
        mPreviewScroller.removeAllViews();
        for(int i = 0;i < imagesCount;i++){
            final ImageView image = new ImageView(getActivity());
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mPreviewImageWidth, mPreviewImageHeight);
            if(i >0){
                params.leftMargin = mImageLeftMargin;
            }
            mPreviewScroller.addView(image, params);
            image.setOnClickListener(this);
            image.setId(i);
            if(theme.isDefaultTheme()){
                ImageLoader.loadStringRes(image, Config.DEFAUTL_THEME_PREVIEWS[i], null, null);
            }else{
                ImageLoader.loadStringRes(image, theme.previewArrays.get(i), null,null);
            }

        }
    }

    @Override
    public void onClick(View view) {
        startPreviewPage(view);
    }

    @Override
    public void update(Response result) {

    }

    @Override
    public int getLayoutRes() {
        return R.layout.local_theme_pkg_detail_layout;
    }

    @Override
    public void showEmptyView(boolean show) {

    }

    @Override
    public void showNetworkErrorView(boolean show) {

    }

    @Override
    public void showRequestFailView(boolean show) {

    }

    @Override
    protected void initialWhenCreateView(View contentView, Bundle savedInstanceState) {
        initView();
    }
}
