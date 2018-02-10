package com.hb.thememanager.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.hb.thememanager.http.downloader.DownloadInfo;
import com.hb.thememanager.http.downloader.callback.DownloadListener;
import com.hb.thememanager.http.downloader.exception.DownloadException;
import com.hb.thememanager.http.request.CategoryDetailRequest;
import com.hb.thememanager.http.request.SearchResultThemeRequest;
import com.hb.thememanager.http.request.ThemeRankingRequest;
import com.hb.thememanager.http.request.ThemeRequest;
import com.hb.thememanager.http.request.TopicDetailBodyRequest;
import com.hb.thememanager.http.response.SimpleThemeResponse;
import com.hb.thememanager.http.response.ThemeListResponse;
import com.hb.thememanager.http.response.TopicDetailBodyResponse;
import com.hb.thememanager.http.response.adapter.ThemeResource;
import com.hb.thememanager.job.BitmapColorPickerThread;
import com.hb.thememanager.job.BitmapColorPickerThread.OnColorPickerListener;
import com.hb.thememanager.job.loader.WallpaperPreviewIconLoader;
import com.hb.thememanager.job.loader.WallpaperPreviewIconLoader.IconLoadCallBack;
import com.hb.thememanager.job.pay.Pay;
import com.hb.thememanager.model.PreviewIcon;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.ui.LoadMoreActivity.MoreRequest;
import com.hb.thememanager.ui.adapter.WallpaperPreivewPagerAdapter;
import com.hb.thememanager.ui.adapter.WallpaperPreivewPagerAdapter.CurrentPageClickListener;
import com.hb.thememanager.ui.adapter.WallpaperPreivewPagerAdapter.ImageLoadFinishListener;
import com.hb.thememanager.ui.fragment.themedetail.WallpaperPreviewActivity;
import com.hb.thememanager.ui.fragment.themedetail.WallpaperPreviewActivity.IconAdapter;
import com.hb.thememanager.ui.mvpview.WallpaperDetailMVPView;
import com.hb.thememanager.ui.mvpview.WallpaperDetailPresenter;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.views.PreviewIconGrid;
import com.hb.thememanager.views.TimeWidget;
import com.hb.thememanager.views.WallpaperButton;
import com.hb.thememanager.views.WallpaperButton.onWallpaperButtonClickListener;
import com.hb.thememanager.views.WallpaperSetDialog;
import com.hb.thememanager.views.WallpaperSetDialog.OnWallpaperSetListener;
import hb.app.HbActivity;
import hb.widget.ViewPager;
import com.hb.thememanager.R;
import com.hb.thememanager.ThemeManagerApplication;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import com.hb.thememanager.utils.TLog;
import com.hb.thememanager.utils.ToastUtils;
import android.os.Handler;
import hb.widget.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WallpaperDetailActivity extends HbActivity implements WallpaperDetailMVPView, IconLoadCallBack, 
																OnClickListener, CurrentPageClickListener, OnColorPickerListener, OnPageChangeListener, 
																onWallpaperButtonClickListener, OnWallpaperSetListener, ImageLoadFinishListener {
	private static final String TAG = "Wallpaper";
	private WallpaperPreviewIconLoader mIconLoader;
	private IconAdapter mIconsAdapter;
	private PreviewIconGrid mIconsList;
	private LinearLayout mDeskPreview;
	private boolean mNoNetwork;
	private boolean showLockscreen = true;
    private WallpaperButton mWallpaperButton;
    private WallpaperSetDialog mWallpaperSetDialog;
	private WallpaperDetailPresenter mPresenter;
	private TimeWidget mTimeWidget;
	private ViewPager mWallpaperViewPager;
	private WallpaperPreivewPagerAdapter mAdapter;
	private int mCurrentPage;
	private List<Wallpaper> mWallpapers;
	private ImageView mFirstImage;
	private static final int TEXT_COLOR_WHITE = -1;
	private static final int TEXT_COLOR_DARK = 0xff4c4c4c;
	private static final int VIEWPAGER_LAST_PAGE = 3;
	private String mLoadMoreUrl;
	private int mRecommendId;
	private int mWallpaperType;
	private String mSearchKey;
	private ThemeRequest mMoreRequest;
	private boolean mImageLoaded = false;
	private int mPageSize;
	private static final int PAGE_SIZE = 9;
	private int mNeedRemoveCount = 0;
	private int mCurrentDatePage = 0;
	private int mTotalNum = -1;
	private Dialog mDownloadDialog;
	private SparseArray<Integer> mIconNameColorCache = new SparseArray<Integer>();
	private SparseArray<Integer> mImageLoadStatus = new SparseArray<Integer>();
	private boolean mNeedSetWallpaper = false;
	private boolean isRequest = false;
	private int mSetWallpaperWay = -1;
	private Handler mHandler = new Handler();
	private Runnable mColorAction = new Runnable(){
		@Override
		public void run() {
			int position = mCurrentPage;
			if(mIconNameColorCache.get(position) != null){
				int color = mIconNameColorCache.get(position).intValue();
				onColorPicked(color, position);
			}else{
				ImageView itemView = mAdapter.getItemView(position);
				BitmapColorPickerThread colorPicker = new BitmapColorPickerThread(itemView, position);
				colorPicker.setOnColorPickerListener(WallpaperDetailActivity.this);
				colorPicker.execute();
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedState) {
		// TODO Auto-generated method stub
		super.onCreate(savedState);
		setContentView(R.layout.wallpaper_detail_layout);
		
		getInitData();
		initView();
	}
	
	private void getInitData() {
		ThemeManagerApplication app = (ThemeManagerApplication)getApplicationContext();		//old bug,未加载过自带壁纸，设置壁纸时，避免不加载自带壁纸。
		app.loadInternalWallpaper();
		app.loadInternalLockScreenWallpaper();
		
		Intent intent = getIntent();
		mPresenter = new WallpaperDetailPresenter(this);
		mPresenter.attachView(this);
		
		Bundle bundle = intent.getBundleExtra(Config.ActionKey.KEY_WALLPAPER_PREVIEW_BUNDLE);
		
		mWallpapers = mPresenter.getWallpapers();
		mCurrentPage = mPresenter.mCurrentClickWallpaper;
//		mWallpapers = bundle.getParcelableArrayList(Config.ActionKey.KEY_WALLPAPER_PREVIEW_LIST);
//		mCurrentPage = bundle.getInt(Config.ActionKey.KEY_WALLPAPER_PERVIEW_CURRENT_ITEM, 0);
		mLoadMoreUrl = bundle.getString(Config.ActionKey.KEY_WALLPAPER_DETAIL_URL);
		mRecommendId = bundle.getInt(Config.ActionKey.KEY_WALLPAPER_DETAIL_ID, -1);
		mWallpaperType = bundle.getInt(Config.ActionKey.KEY_WALLPAPER_DETAIL_TYPE, -1);
		mSearchKey = bundle.getString(Config.ActionKey.KEY_WALLPAPER_DETAIL_SEARCH_KEY);
		
		int size = mWallpapers.size();
		int more = size % PAGE_SIZE;
		if (mLoadMoreUrl != null && size > PAGE_SIZE && mCurrentPage < (size - more)) {
			mPageSize = PAGE_SIZE;
			mCurrentDatePage = size / PAGE_SIZE - 1;
			for (int i = 0; i < more; i++) {
				mWallpapers.remove(size - i - 1);
			}
		}else if (mLoadMoreUrl != null && size > PAGE_SIZE && mCurrentPage >= (size - more)) {
			mPageSize = PAGE_SIZE;
			mCurrentDatePage = size / PAGE_SIZE - 1;
			mNeedRemoveCount = more;
		}else {
			mPageSize = size;
		}
	}
	private void initView() {
		mTimeWidget = (TimeWidget)findViewById(R.id.time_widget);
		mWallpaperViewPager = (ViewPager)findViewById(R.id.wallpaper_detail_pager);
		mDeskPreview = (LinearLayout)findViewById(R.id.desk_wallpaper_preview);
		mWallpaperButton = (WallpaperButton)findViewById(R.id.wallpaper_detail_buttons);
		mIconsList = (PreviewIconGrid)findViewById(R.id.wallpaper_preview_icon_list);
		mFirstImage = (ImageView)findViewById(R.id.image_first);
		if(Config.sStaringImageInPreview != null){
			mFirstImage.setImageDrawable(Config.sStaringImageInPreview);
		}
		
		mWallpaperButton.setOnWallpaperButtonClickListener(this);
		mAdapter = new WallpaperPreivewPagerAdapter(this, mWallpapers, mCurrentPage);
		mAdapter.setOnPageClickListener(this);
		mAdapter.setOnImageLoadFinishListener(this);
		mWallpaperViewPager.setOffscreenPageLimit(2);
		mWallpaperViewPager.setAdapter(mAdapter);
		mWallpaperViewPager.setOnPageChangeListener(this);
		mWallpaperViewPager.setCurrentItem(mCurrentPage);
		
		mTimeWidget.updateWidgetColor(Color.BLACK);
        
		mIconsAdapter = new WallpaperPreviewActivity.IconAdapter(WallpaperPreviewActivity.PREVIEW_TOP_ICONS, this, WallpaperPreviewActivity.ICON_TOP);
		mIconsList.setAdapter(mIconsAdapter);
		mIconLoader = new WallpaperPreviewIconLoader(getApplicationContext());
		mIconLoader.setIconLoadCallback(this);
		mIconLoader.execute(WallpaperPreviewActivity.PREVIEW_TOP_ICONS);
		
		if (mCurrentPage == 0)
			loadingWidgetShow(mCurrentPage);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_setup_network:
				errorViewDismiss(mCurrentPage);
				loadingWidgetShow(mCurrentPage);
				if(mNoNetwork) {		//无网络，刷新加载更多数据。
					request();
				}else {				//图片加载失败，刷新图片。
					mAdapter.instantiateItem(mWallpaperViewPager, mCurrentPage);
				}
				break;
		}
	}
	
	@Override
	public void updateSaveWallpaperInfo(Object result) {
		if (mDownloadDialog == null) {
			mDownloadDialog = new Dialog(this);
			View dialogView = View.inflate(this, R.layout.item_wallpaper_dialog_loading, null);
			mDownloadDialog.setContentView(dialogView);
			mDownloadDialog.setCanceledOnTouchOutside(false);
		}
		final DownloadInfo info = (DownloadInfo)result;
		info.setDownloadListener(new DownloadListener() {
			@Override
			public void onWaited() {
			}
			@Override
			public void onStart() {
		        mDownloadDialog.show();
			}
			@Override
			public void onRemoved() {
			}
			@Override
			public void onPaused() {
			}
			@Override
			public void onDownloading(long progress, long size) {
			}
			@Override
			public void onDownloadSuccess() {
				if(mNeedSetWallpaper) {
					mWallpaperSetDialog.setWallpaper(mSetWallpaperWay, info.getPath());
					mNeedSetWallpaper = false;
				}
				mDownloadDialog.dismiss();
				mWallpaperButton.setButtonSavedStatus(true);
				if(mWallpaperSetDialog == null) {
					mWallpaperSetDialog = new WallpaperSetDialog(WallpaperDetailActivity.this);
					mWallpaperSetDialog.setOnWallpaperSetListener(WallpaperDetailActivity.this);
				}
				mWallpaperSetDialog.saveDeskWallpaper(new File(info.getPath()));
				mWallpaperSetDialog.saveLockScreenWallpaper(new File(info.getPath()));
			}
			@Override
			public void onDownloadFailed(DownloadException e) {
				mDownloadDialog.dismiss();
				TLog.e(TAG,"download exception->"+e);
				ToastUtils.showShortToast(WallpaperDetailActivity.this,R.string.download_failed);
			}
		});
	}

	@Override
	public void updateViewPagerData(Object result) {
		if (mNoNetwork && mAdapter.getLoadingWidget(mCurrentPage).isShown()) {
			loadingWidgetDismiss(mCurrentPage);
		}
		if (mNoNetwork && mAdapter.getErrorWidget(mCurrentPage).isShown()) {
			errorViewDismiss(mCurrentPage);
		}
		mNoNetwork = false;
		mWallpapers.clear();
		ArrayList<Theme> themes = null;
		if(result instanceof ThemeListResponse) {
			ThemeListResponse tlr = (ThemeListResponse) result;
			themes = tlr.body.getThemes(Theme.WALLPAPER);
			mTotalNum = ((ThemeListResponse) result).body.getTotalNum();
		}else if(result instanceof SimpleThemeResponse) {
            SimpleThemeResponse str = (SimpleThemeResponse)result;
            themes = str.body.getThemes(Theme.WALLPAPER);
            mTotalNum = str.body.getTotalNum();
		}else if(result instanceof TopicDetailBodyResponse) {
			TopicDetailBodyResponse tdbr = (TopicDetailBodyResponse)result;
            themes = tdbr.body.getThemes();
            mTotalNum =tdbr.body.getTotalNum();
		}
		Wallpaper wallpaper;
		for (Theme theme : themes) {
			wallpaper = new Wallpaper();
			wallpaper.downloadUrl = theme.downloadUrl;
			if (mNeedRemoveCount != 0) {
				mNeedRemoveCount--;
			}else {
				mWallpapers.add(wallpaper);
			}
		}
		mAdapter.addData(mWallpapers);
		
		mMoreRequest.setPageNumber(++mCurrentDatePage);
		isRequest = false;
	}

	@Override
	public void onIconLoaded(ArrayList<PreviewIcon[]> icons) {
		if(icons != null && icons.size() == 1){
			mIconsAdapter.setIcons(icons.get(0));
		}
	}

    @Override
    protected void onResume() {
        super.onResume();
    }

	@Override
	protected void onPause() {
		super.onPause();
		Config.sStaringImageInPreview = null;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mPresenter.clear();
		mPresenter.detachView();
		mHandler.removeCallbacks(mColorAction);
		mWallpapers.clear();
		mAdapter.onDestory();
	}

	@Override
	public void showEmptyView(boolean show) {
	}
	@Override
	public void showNetworkErrorView(boolean show) {
		if(mCurrentPage >= (mAdapter.getCount() - 1)) {
			mNoNetwork = true;
			errorViewShow(mCurrentPage, getResources().getString(R.string.no_network_cannot_load));
		}
		isRequest = false;
	}

	@Override
	public void currentPageClick() {
		if(mImageLoaded) {
			if(showLockscreen) {
				showLockscreen = false;
				mTimeWidget.setVisibility(View.GONE);
				mDeskPreview.setVisibility(View.VISIBLE);
			}else {
				showLockscreen = true;
				mTimeWidget.setVisibility(View.VISIBLE);
				mDeskPreview.setVisibility(View.GONE);
			}
		}
	}
	
	@Override
	public void onEnterAnimationComplete() {
		Config.sStaringImageInPreview = null;
	}
	
	private void startPickerColor(int position){
		mHandler.post(mColorAction);
	}
	
	@Override
	public void onColorPicked(int color, int position) {
		if(mIconNameColorCache.get(position) == null){
			mIconNameColorCache.put(position, Integer.valueOf(color));
		}
		mWallpaperButton.setButtonColor(color);
		startPickerSaveButtonStatus(position);
		
		int flag = getWindow().getDecorView().getSystemUiVisibility();
		if(color == Config.Color.WHITE){
			getWindow().getDecorView().setSystemUiVisibility(
					flag &~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
					&~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
		}else{
			getWindow().getDecorView().setSystemUiVisibility(
					flag |View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR 
					|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
		}
		if(color == TEXT_COLOR_WHITE) {
			mTimeWidget.updateWidgetColor(color);
			mIconsAdapter.setIconNameColor(color);
		}else {
			mTimeWidget.updateWidgetColor(TEXT_COLOR_DARK);
			mIconsAdapter.setIconNameColor(TEXT_COLOR_DARK);
		}
	}

	@Override
	public void onPageScrollStateChanged(int status) {
		if(mFirstImage.getVisibility() == View.VISIBLE && status == ViewPager.SCROLL_STATE_IDLE){
			mFirstImage.setVisibility(View.GONE);
		}
	}
	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
	}
	@Override
	public void onPageSelected(int position) {
		Integer integer = mImageLoadStatus.get(position);
		int status = -1;
		if(integer != null) {
			status = integer.intValue();
			if(status == WallpaperPreivewPagerAdapter.STATUS_IMAGE_LOAD_ERROR) {
				mNoNetwork = false;
				errorViewShow(position, getResources().getString(R.string.pic_load_error));
			}
			if(status == WallpaperPreivewPagerAdapter.STATUS_IMAGE_LOAD_FINISH){
				loadingWidgetDismiss(position);
			}
		}else {				//status为null，表示图片加载中
			loadingWidgetShow(position);
		}
		mCurrentPage = position;
		
		if(mCurrentPage >= (mAdapter.getCount() - VIEWPAGER_LAST_PAGE)) {
			request();
		}
	}
	private void request() {
        if (!isRequest) {
            synchronized (WallpaperDetailActivity.class) {
                if (!isRequest) {
                	isRequest = true;
            		if(mCurrentDatePage >= mTotalNum && mTotalNum != -1) {
//            			ToastUtils.showShortToast(this, R.string.滑动到最后一页的提示语);
                    	isRequest = false;
            			return;
            		}
            		if(mLoadMoreUrl != null && mMoreRequest == null) {
            			if(mLoadMoreUrl.equals(Config.HttpUrl.getCategoryUrl(Theme.WALLPAPER))) {
            				mMoreRequest = new CategoryDetailRequest(this, Theme.WALLPAPER, mWallpaperType, mRecommendId);
            			}else if(mLoadMoreUrl.equals(Config.HttpUrl.getRankUrl(Theme.WALLPAPER))) {
            				mMoreRequest = new ThemeRankingRequest(this, Theme.WALLPAPER, mWallpaperType);
            			}else if(mLoadMoreUrl.equals(Config.HttpUrl.getHotRecommendUrl(Theme.WALLPAPER))) {
            				mMoreRequest = new MoreRequest(this, Theme.WALLPAPER);
            				mMoreRequest.setId(String.valueOf(mRecommendId));
            			}else if(mLoadMoreUrl.equals(Config.HttpUrl.SEARCH_URL)) {
            				mMoreRequest = new SearchResultThemeRequest(this, Theme.WALLPAPER);
            				((SearchResultThemeRequest)mMoreRequest).setKey(mSearchKey);
            			}else if(mLoadMoreUrl.equals(Config.HttpUrl.getTopicDetailListUrl(Theme.WALLPAPER))) {
            				mMoreRequest = new TopicDetailBodyRequest(this, Theme.WALLPAPER);
            				mMoreRequest.setId(String.valueOf(mRecommendId));
            			}
            			mMoreRequest.setPageNumber(++mCurrentDatePage);
            			mMoreRequest.setPageSize(mPageSize);
            		}
            		if(mLoadMoreUrl != null) {
            			mPresenter.requestTheme(mMoreRequest);
            		}
                }
            }
        }
	}

	private void startPickerSaveButtonStatus(int position) {
		mWallpaperButton.setButtonSavedStatus(mAdapter.getItemWallpaper(position));
	}

	@Override
	public void onSaveButtonClick() {
		mPresenter.updateSaveWallpaperInfo(mAdapter.getItemWallpaper(mCurrentPage));
	}
	@Override
	public void onSetButtonClick() {
		if(mWallpaperSetDialog == null) {
			mWallpaperSetDialog = new WallpaperSetDialog(this);
			mWallpaperSetDialog.setOnWallpaperSetListener(this);
		}
		mWallpaperSetDialog.setWallpaperRes(mAdapter.getItemWallpaper(mCurrentPage));
		mWallpaperSetDialog.show();
	}
	@Override
	public void onSetWallpaper(int way) {
		mSetWallpaperWay = way;
		mNeedSetWallpaper = true;
		mPresenter.updateSaveWallpaperInfo(mAdapter.getItemWallpaper(mCurrentPage));
	}

	@Override
	public void imageLoadFinish(int position, int status) {
		mImageLoadStatus.put(position, Integer.valueOf(status));
		if(mCurrentPage == position) {
			loadingWidgetDismiss(mCurrentPage);
			mFirstImage.setVisibility(View.GONE);
			errorViewDismiss(position);
//			if(mCurrentPage >= (mAdapter.getCount() - 1) && mNoNetwork) {
//				request();
//			}
		}
	}
	@Override
	public void imageLoadError(int position, int status) {
		mImageLoadStatus.put(position, Integer.valueOf(status));
		if(mCurrentPage == position) {
			mFirstImage.setVisibility(View.GONE);
			mNoNetwork = false;
			errorViewShow(position, getResources().getString(R.string.pic_load_error));
		}
	}
	
	private void errorViewDismiss(int position) {
		mAdapter.getErrorWidget(position).setVisibility(View.GONE);
	}
	private void errorViewShow(int position, String des) {
		mImageLoaded = false;
		mTimeWidget.setVisibility(View.GONE);
		mDeskPreview.setVisibility(View.GONE);
		mWallpaperButton.setVisibility(View.GONE);
		LinearLayout loadingWidget = mAdapter.getLoadingWidget(position);
		loadingWidget.setVisibility(View.GONE);
		LinearLayout errorWidget = mAdapter.getErrorWidget(position);
		errorWidget.setVisibility(View.VISIBLE);
		TextView errorText = (TextView)errorWidget.findViewById(R.id.error_text);
		errorText.setText(des);
		Button refreshButton = (Button)errorWidget.findViewById(R.id.btn_setup_network);
		refreshButton.setOnClickListener(this);
	}
	
	private void loadingWidgetShow(int position) {
		mImageLoaded = false;
		mTimeWidget.setVisibility(View.GONE);
		mDeskPreview.setVisibility(View.GONE);
		LinearLayout loadingWidget = mAdapter.getLoadingWidget(position);
		loadingWidget.setVisibility(View.VISIBLE);
		mWallpaperButton.setVisibility(View.GONE);
	}
	private void loadingWidgetDismiss(int position) {
		mImageLoaded = true;
		startPickerColor(position);
		if(!mTimeWidget.isShown() && !mDeskPreview.isShown()) {
			mTimeWidget.setVisibility(View.VISIBLE);
		}
		if(mAdapter.getErrorWidget(position).isShown()) {
			errorViewDismiss(position);
		}
		LinearLayout loadingWidget = mAdapter.getLoadingWidget(position);
		loadingWidget.setVisibility(View.GONE);
		mWallpaperButton.setVisibility(View.VISIBLE);
	}
	
	
	
	
	
	
	
}





