package com.hb.thememanager.ui.fragment.themedetail;

import java.util.ArrayList;

import hb.app.dialog.ProgressDialog;
import hb.widget.ViewPager;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Broadcaster;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;

import com.bumptech.glide.load.DecodeFormat;
import com.hb.thememanager.ThemeManagerApplication;
import com.hb.thememanager.job.loader.ImageLoader;
import com.hb.thememanager.job.loader.ImageLoaderConfig;
import com.hb.thememanager.listener.OnThemeStateChangeListener;
import com.hb.thememanager.model.PreviewTransitionInfo;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.R;
import com.hb.thememanager.state.ThemeState.State;
import com.hb.thememanager.ui.fragment.AbsThemeFragment;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.DialogUtils;
import com.hb.thememanager.utils.StringUtils;
import com.hb.thememanager.views.ThemePreviewDonwloadButton;
import android.content.res.HbConfiguration;
public class ThemePkgDetailFragment extends AbsThemeFragment implements ThemePkgDetailMVPView
,OnClickListener, OnThemeStateChangeListener{
	private static final String TAG = "ThemeDetail";

	private Theme mCurrentTheme;
	private LinearLayout mPreviewScroller;
	private ThemePkgDetailPresenter mPresenter;
	private TextView mDesigner;
	private TextView mThemeSize;
	private TextView mDescription;
	private View mContentView;
	private int mPreviewImageWidth;
	private int mPreviewImageHeight;
	private int mImageLeftMargin;
	private ThemePreviewDonwloadButton mOptionBtn;
	private boolean mRestartUI = false;
    private static ImageLoaderConfig sImageConfig ;
	/**
	 * Share drawable for preview ,it must set to null 
	 * when this fragment finished.
	 */
	private static  Drawable[] sPreviewDrawable;
	private  ArrayList<PreviewTransitionInfo> mInfos;
	private ProgressDialog mApplyProgress;
	private Handler mHandler = new Handler();
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Bundle args = getBundle();
		if(args != null){
			mCurrentTheme = args.getParcelable(Config.ActionKey.KEY_THEME_PKG_DETAIL);
		}


	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		mContentView = inflater.inflate(R.layout.theme_pkg_detail_layout, container,false);
		mPresenter = new ThemePkgDetailPresenter(getActivity(), mCurrentTheme);
		mPresenter.attachView(this);
		initView();
		mPreviewImageWidth = getResources().getDimensionPixelSize(R.dimen.theme_detail_preview_img_width);
		mPreviewImageHeight = getResources().getDimensionPixelSize(R.dimen.theme_detail_preview_img_height);
		mImageLeftMargin = getResources().getDimensionPixelSize(R.dimen.theme_detail_preview_img_margin_left);
		return mContentView;
	}

	@Override
	protected void initialImageLoader(com.hb.imageloader.ImageLoaderConfig.Size size, com.hb.imageloader.ImageLoaderConfig config) {
		super.initialImageLoader(size, config);
		size.setHeight(size.getHeight() * 5);
		size.setWidth(size.getWidth() * 4);
		config.setDecodeFormat(Bitmap.Config.ARGB_8888);
	}

	@Override
	protected void initView() {
		// TODO Auto-generated method stub
		mPreviewScroller = (LinearLayout)mContentView.findViewById(R.id.theme_pkg_detail_preview_scroller);
		mDesigner = (TextView)mContentView.findViewById(R.id.theme_detail_designer);
		mThemeSize = (TextView)mContentView.findViewById(R.id.theme_detail_size);
		mDescription = (TextView)mContentView.findViewById(R.id.theme_detail_description);
		mOptionBtn = (ThemePreviewDonwloadButton)mContentView.findViewById(R.id.theme_detail_option_btn);
		mOptionBtn.setTheme(mCurrentTheme);
		mOptionBtn.setOnStateChangeListener(this);
		new Handler().post(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				mPresenter.loadThemePreview();
			}
		});
		createApplyProgressDialog();
	}
	
	private void createApplyProgressDialog(){
		if(mApplyProgress == null){
			mApplyProgress = new ProgressDialog(getContext());
			mApplyProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mApplyProgress.setMessage(getResources().getString(R.string.msg_apply_theme));
			mApplyProgress.setCancelable(false);
			mApplyProgress.setCanceledOnTouchOutside(false);
		}
	}
	
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}



	@Override
	public void updateThemeInfo(Theme theme) {
		// TODO Auto-generated method stub
		if(theme != null){
			mDesigner.setText(getString(R.string.theme_detail_designer,theme.designer));
			mThemeSize.setText(getString(R.string.theme_detail_size,theme.size));
			mDescription.setText(getString(R.string.theme_detail_description,theme.description));
			createPreview(theme);
		}
	}



	private void createPreview(Theme theme){
		
		int imagesCount = theme.previewArrays.size();
		final boolean isDefaultTheme = theme.id == Config.DEFAULT_THEME_ID;
		if(isDefaultTheme){
			imagesCount = Config.DEFAUTL_THEME_PREVIEWS.length;
		}
		if(imagesCount == 0){
			return;
		}
		mPreviewScroller.removeAllViews();
		//第一张已经作为封面，在预览界面就不显示了
		if(imagesCount > 1 && !isDefaultTheme){
			theme.previewArrays.remove(0);
			imagesCount = theme.previewArrays.size();
		}

		for(int i = 0;i < imagesCount;i++){
			
			final ImageView image = new ImageView(getActivity());
			image.setScaleType(ScaleType.CENTER_CROP);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mPreviewImageWidth, mPreviewImageHeight);
			if(i >0){
				params.leftMargin = mImageLeftMargin;
			}
			mPreviewScroller.addView(image, params);
			image.setOnClickListener(this);
			image.setId(i);
			if(isDefaultTheme){
				getImageLoader().loadImage(Config.DEFAUTL_THEME_PREVIEWS[i],image);
			}else{
				getImageLoader().loadImage(theme.previewArrays.get(i),image);
			}
		}
		
	}

	public static Drawable[] getPreviewDrawables(){
		return sPreviewDrawable;
	}
	
	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
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

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		recyclePreviewDrawable();
		if(mPreviewScroller != null) {
			mPreviewScroller.removeAllViews();
		}
		if(Looper.myLooper() != Looper.getMainLooper()){
			Handler mainThread = new Handler(Looper.getMainLooper());
			mainThread.post(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					ImageLoader.cleanAll(getContext());
				}
			});
		}


	}


	public static void recyclePreviewDrawable(){
		if(sPreviewDrawable != null){
			int count = sPreviewDrawable.length;
			for(int i = 0 ;i < count;i++){
				sPreviewDrawable[i] = null;
			}
			sPreviewDrawable = null;
		}
	}



	@Override
	public void onStateChange(State state) {
		// TODO Auto-generated method stub
		if( !isAdded()){
			return;
		}
		if(state == State.STATE_APPLIED){
			Toast.makeText(getContext(), getResources().getString(R.string.msg_select_theme_applied), Toast.LENGTH_LONG).show();
		}else if(state == State.STATE_START_APPLY){
			mApplyProgress.show();
		}else if(state == State.STATE_APPLY_SUCCESS){
			//mApplyProgress.dismiss();
			//调用该方法去改变Configuration实现界面重新刷新
//			HbConfiguration.updateThemeConfiguration(1);

			if(isAdded()) {
				mApplyProgress.dismiss();
				startActivity(CommonUtil.getHomeIntent());
				ThemeManagerApplication app = (ThemeManagerApplication)getContext().getApplicationContext();
				app.getThemeManager().setThemeLoadListener(null);
			}
		}
	}
	

	
	
	

}
