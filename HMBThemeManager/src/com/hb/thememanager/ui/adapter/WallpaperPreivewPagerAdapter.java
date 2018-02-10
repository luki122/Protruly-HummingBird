package com.hb.thememanager.ui.adapter;

import java.util.ArrayList;
import java.util.List;

import com.bumptech.glide.load.DecodeFormat;
import com.hb.thememanager.http.downloader.DownloadInfo;
import com.hb.thememanager.http.downloader.DownloadManagerImpl;
import com.hb.thememanager.http.downloader.DownloadService;
import com.hb.thememanager.http.response.adapter.ThemeResource;
import com.hb.thememanager.job.loader.ImageLoader;
import com.hb.thememanager.job.loader.ImageLoaderConfig;
import com.hb.thememanager.job.loader.ImageLoaderListener;
import com.hb.thememanager.model.Wallpaper;

import hb.widget.PagerAdapter;
import hb.widget.ViewPager;
import android.content.Context;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import com.hb.imageloader.RecyclingImageView;

import java.io.File;

import com.hb.thememanager.R;

import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
public class WallpaperPreivewPagerAdapter extends PagerAdapter implements OnClickListener {
	
	private List<Wallpaper> mWallpapers = new ArrayList<>();
	/**
	 * Saved items for ViewPager,ViewPager's item position used for key
	 */
	private SparseArray<View> mItemViews = new SparseArray<View>();
	public static final int STATUS_IMAGE_LOAD_FINISH = 1;
	public static final int STATUS_IMAGE_LOAD_ERROR = 2;
	private Context mContext;
	private static ImageLoaderConfig sImageConfig ;
	private CurrentPageClickListener mClickListener;
	private ImageLoadFinishListener mImageLoadListener;
	private DownloadManagerImpl mDm;
	
	public WallpaperPreivewPagerAdapter(Context context,
			List<Wallpaper> wallpapers, int clickPosition) {
		mWallpapers.addAll(wallpapers);
		mContext = context;
		initialItemViews(clickPosition);
		//Initial imageConfig for ImageLoader
		sImageConfig = new ImageLoaderConfig.Builder().
				setCropType(ImageLoaderConfig.CENTER_CROP).
		        setAsBitmap(true).
		        setFormat(DecodeFormat.PREFER_ARGB_8888).
		        setDiskCacheStrategy(ImageLoaderConfig.DiskCache.RESULT).
		        setSkipMemoryCache(true).
		        setPrioriy(ImageLoaderConfig.LoadPriority.HIGH).build();
	}

	private void initialItemViews(int clickPosition) {
		// TODO Auto-generated method stub
		int childCount = getCount();
		//Initial all items' view first time
		if (childCount > 0) {
			if (clickPosition == -1) {			//clickPosition＝-1，加载所有
				for (int i = 0; i < childCount; i++) {
					View itemView = LayoutInflater.from(mContext).inflate(
							R.layout.list_item_wallpaper_preview, null);
					mItemViews.put(i, itemView);
				}
			}else {
				for (int i = 0; i < childCount; i++) {
					if (i >= (clickPosition - 2) && i <= (clickPosition + 2)) {		//传入clickPosition，解决mWallpapers数据量过大，for循环导致的卡顿
						View itemView = LayoutInflater.from(mContext).inflate(
								R.layout.list_item_wallpaper_preview, null);
						mItemViews.put(i, itemView);
					}
				}
			}
		}
	}
	
	public void addData(List<Wallpaper> data) {
		synchronized (mWallpapers) {
//			if(!mWallpapers.containsAll(data)) {
				mWallpapers.addAll(data);
				notifyDataSetChanged();
//			}
		}
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mWallpapers.size();
	}

	/**
	 * 
	 * If ViewPager's parent Activity or Fragment is destroyed,Call this method to 
	 * clear datas
	 * 
	 */
	public void onDestory(){
		mItemViews.clear();
		mWallpapers.clear();
	}
	
	@Override
	public boolean isViewFromObject(View view, Object object) {
		// TODO Auto-generated method stub
		return view == object;
	}
	
	public Wallpaper getItemWallpaper(int position) {
		if(mWallpapers == null || mWallpapers.size() == 0){
			return null;
		}
		return mWallpapers.get(position);
	}

	public ImageView getItemView(int position){
		if(mItemViews == null || mItemViews.size() == 0){
			return null;
		}
		return (ImageView)((FrameLayout)mItemViews.get(position)).findViewById(R.id.wallpaper_preview_item_img);
	}
	
	public LinearLayout getLoadingWidget(int position) {
		if(mItemViews == null || mItemViews.size() == 0){
			return null;
		}
		return (LinearLayout)((FrameLayout)mItemViews.get(position)).findViewById(R.id.loading_widget);
	}
	
	public LinearLayout getErrorWidget(int position) {
		if(mItemViews == null || mItemViews.size() == 0){
			return null;
		}
		return (LinearLayout)((FrameLayout)mItemViews.get(position)).findViewById(R.id.no_network_view_large);
	}
	
	@Override
	public Object instantiateItem(View container, int position) {
		// TODO Auto-generated method stub
		
		final int itemViewPosition = position;
		/*
		 * Gets item's view from mItemViews always,if result view
		 * is null,we need inflate a new one,and put it into mItemView
		 * in order to we can recycle it when this item is going to destroy
		 */
		View itemViewLoaded = mItemViews.get(position);
		if(itemViewLoaded == null){
			itemViewLoaded = LayoutInflater.from(mContext).inflate(
					R.layout.list_item_wallpaper_preview, null);
			mItemViews.put(position, itemViewLoaded);
		}
		
		View itemView = itemViewLoaded;
		ViewPager pager = (ViewPager)container;
		if(itemView.getParent() != null && itemView.getParent() == pager){
			pager.removeView(itemView);
		}
		pager.addView(itemView);
		RecyclingImageView imageView = (RecyclingImageView)itemView.findViewById(R.id.wallpaper_preview_item_img);
		Wallpaper wallpaper = mWallpapers.get(position);
		if(wallpaper != null){
			if(wallpaper.themeFilePath != null && wallpaper.themeFilePath.length() > 0) {
				ImageLoader.loadFile(imageView, new File(wallpaper.themeFilePath), sImageConfig, new ImageLoaderListener() {
					@Override
					public void onSuccess() {
						if(mImageLoadListener != null)
							mImageLoadListener.imageLoadFinish(itemViewPosition, STATUS_IMAGE_LOAD_FINISH);
					}
					@Override
					public void onError() {
						if(mImageLoadListener != null)
							mImageLoadListener.imageLoadError(itemViewPosition, STATUS_IMAGE_LOAD_ERROR);
					}
				});
			}else if(wallpaper.downloadUrl != null && wallpaper.downloadUrl.length() > 0) {
				if(mDm == null) {
					mDm = (DownloadManagerImpl)DownloadService.getDownloadManager(mContext);
				}
				DownloadInfo info = mDm.getDownloadById(wallpaper.downloadUrl.hashCode());
				if(info != null && info.getStatus() == DownloadInfo.STATUS_COMPLETED) {
					File file = new File(info.getPath());
					if(file.exists() && file.isFile()) {
						ImageLoader.loadFile(imageView, file, sImageConfig,  new ImageLoaderListener() {
							@Override
							public void onSuccess() {
								if(mImageLoadListener != null)
									mImageLoadListener.imageLoadFinish(itemViewPosition, STATUS_IMAGE_LOAD_FINISH);
							}
							@Override
							public void onError() {
								if(mImageLoadListener != null)
									mImageLoadListener.imageLoadError(itemViewPosition, STATUS_IMAGE_LOAD_ERROR);
							}
						});
					}
				}else {
					ImageLoader.loadStringRes(imageView, wallpaper.downloadUrl, sImageConfig, new ImageLoaderListener() {
						@Override
						public void onSuccess() {
							if(mImageLoadListener != null)
								mImageLoadListener.imageLoadFinish(itemViewPosition, STATUS_IMAGE_LOAD_FINISH);
						}
						@Override
						public void onError() {
							if(mImageLoadListener != null)
								mImageLoadListener.imageLoadError(itemViewPosition, STATUS_IMAGE_LOAD_ERROR);
						}
					});
				}
			}
		}
		itemView.setOnClickListener(this);
		return itemView;
	}
	
	public void setOnImageLoadFinishListener(ImageLoadFinishListener listener) {
		mImageLoadListener = listener;
	}
	public interface ImageLoadFinishListener {
		void imageLoadFinish(int position, int status);
		void imageLoadError(int position, int status);
	}
	
	public void setOnPageClickListener(CurrentPageClickListener listener) {
		mClickListener = listener;
	}
	public interface CurrentPageClickListener {
		void currentPageClick();
	}

	@Override
	public void onClick(View v) {
		if(mClickListener != null) {
			mClickListener.currentPageClick();
		}
	}
	
	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		ViewPager pager = (ViewPager)container;
		View itemView = mItemViews.get(position);
		pager.removeView(itemView);
		//Remove invisible item's view to avoid OOM
		mItemViews.remove(position);
		mItemViews.put(position, null);
	}

}



