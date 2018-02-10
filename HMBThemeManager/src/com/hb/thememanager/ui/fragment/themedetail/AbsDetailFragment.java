package com.hb.thememanager.ui.fragment.themedetail;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hb.thememanager.R;
import com.hb.thememanager.model.PreviewTransitionInfo;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.views.ExpandableTextView;
import com.hb.thememanager.views.ThemePreviewDonwloadButton;

import java.util.ArrayList;

import hb.app.HbActivity;
import hb.app.dialog.ProgressDialog;
import hb.widget.toolbar.Toolbar;

/**
 * Created by alexluo on 17-8-8.
 */

public abstract class AbsDetailFragment extends Fragment implements IFragment,ThemePkgDetailMVPView{

    private LayoutInflater mInflater;
    protected TextView mThemeName;
    protected View mContentView;
    protected Theme mCurrentTheme;
    protected LinearLayout mPreviewScroller;
    private ThemePkgDetailPresenter mPresenter;
    protected TextView mDesigner;
    protected TextView mThemeSize;
    protected ExpandableTextView mDescription;
    protected ThemePreviewDonwloadButton mOptionBtn;
    protected static  Drawable[] sPreviewDrawable;
    protected ProgressDialog mApplyProgress;
    /**
     * Share drawable for preview ,it must set to null
     * when this fragment finished.
     */

    protected  ArrayList<PreviewTransitionInfo> mInfos;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflater = inflater;
        mContentView = inflater.inflate(getLayoutRes(), container,false);
        mPresenter = new ThemePkgDetailPresenter(getActivity(), mCurrentTheme);
        mPresenter.attachView(this);
        initialWhenCreateView(mContentView,savedInstanceState);
        return mContentView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createApplyProgressDialog();
    }
    private void createApplyProgressDialog(){
        if(mApplyProgress == null){
            mApplyProgress = new ProgressDialog(getContext());
            mApplyProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mApplyProgress.setMessage(getResources().getString(R.string.msg_apply_theme));
        }
    }

    public LayoutInflater getInflater(){
        return mInflater;
    }


    protected ThemePkgDetailPresenter getPresenter(){
        return mPresenter;
    }

    /**
     * 需要在ｏｎＣｒｅａｔｅＶｉｅｗ中操作的逻辑在这个方法中执行
     * @param contentView
     * @param savedInstanceState
     */
    protected abstract void initialWhenCreateView(View contentView,Bundle savedInstanceState);

    @Override
    public Bundle getBundle() {
        return getArguments();
    }

    @Override
    public View findViewById(int id) {
        return mContentView.findViewById(id);
    }


    public void showApplyProgressDialog(boolean show){
        if(show){
            mApplyProgress.show();
        }else{
            mApplyProgress.dismiss();
        }
    }

    protected Toolbar getToolbar(){
        if(getActivity() instanceof HbActivity){
            return ((HbActivity)getActivity()).getToolbar();
        }
        return null;
    }


    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if(sPreviewDrawable != null){
            int count = sPreviewDrawable.length;
            for(int i = 0 ;i < count;i++){
                sPreviewDrawable[i] = null;
            }
            sPreviewDrawable = null;
        }
        if(mOptionBtn != null) {
            mOptionBtn.setOnStateChangeListener(null);
        }
    }

    protected void startPreviewPage(View v){
        int previewCount = mPreviewScroller.getChildCount();
        sPreviewDrawable = new Drawable[previewCount];
        mInfos = new ArrayList<PreviewTransitionInfo>();
        for(int i = 0;i < previewCount ;i++){
            ImageView image = (ImageView) mPreviewScroller.getChildAt(i);
            sPreviewDrawable[i] = image.getDrawable();
            PreviewTransitionInfo info = new PreviewTransitionInfo();
            info.index = i;
            final View imageView = mPreviewScroller.getChildAt(i);
            int[] position = imageView.getLocationOnScreen();
            info.x = position[0];
            info.y = position[1];
            mInfos.add(info);
        }

        Intent intent = new Intent(getActivity(), ThemePreviewActivity.class);
        intent.putExtra(PreviewTransitionInfo.KEY_ID,v.getId());
        intent.putParcelableArrayListExtra(PreviewTransitionInfo.KEY_INFO, mInfos);
        startActivity(intent);
    }

}
