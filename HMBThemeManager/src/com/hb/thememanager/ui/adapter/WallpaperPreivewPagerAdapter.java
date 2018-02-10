package com.hb.thememanager.ui.adapter;

import java.util.ArrayList;
import java.util.List;

import com.bumptech.glide.load.DecodeFormat;
import com.hb.imageloader.HbImageLoader;
import com.hb.thememanager.ThemeManagerApplication;
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
import android.view.ViewGroup;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import com.hb.imageloader.RecyclingImageView;

import java.io.File;

import com.hb.thememanager.R;

import android.widget.ImageView;
public class WallpaperPreivewPagerAdapter extends PagerAdapter {

	private List<Wallpaper> mWallpapers;

	private HbImageLoader mImageLoader;
	/**
	 * Saved items for ViewPager,ViewPager's item position used for key
	 */
	private SparseArray<View> mItemViews = new SparseArray<View>();
	private Context mContext;
	public WallpaperPreivewPagerAdapter(Context context,
			List<Wallpaper> wallpapers) {
		mWallpapers = wallpapers;
		mContext = context;
		initialItemViews();
	}

	private void initialItemViews() {
		// TODO Auto-generated method stub
		int childCount = getCount();
		//Initial all items' view first time
		if (childCount > 0) {
			for (int i = 0; i < childCount; i++) {
				View itemView = LayoutInflater.from(mContext).inflate(
						R.layout.list_item_wallpaper_preview, null);
				mItemViews.put(i, itemView);
			}

		}
	}

	public void setImageLoader(HbImageLoader imageLoader){
		mImageLoader = imageLoader;
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
	

	public ImageView getItemView(int position){
		if(mItemViews == null || mItemViews.size() == 0){
			return null;
		}
		return (ImageView)mItemViews.get(position);
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
		
		RecyclingImageView itemView = (RecyclingImageView)itemViewLoaded;
		ViewPager pager = (ViewPager)container;
		if(itemView.getParent() != null && itemView.getParent() == pager){
			pager.removeView(itemView);
		}
		pager.addView(itemView);
		Wallpaper wallpaper = mWallpapers.get(position);
		if(wallpaper != null){
			mImageLoader.loadImage(wallpaper.themeFilePath,itemView);
		}
		return itemView;
	}

	
	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		ViewPager pager = (ViewPager)container;
		RecyclingImageView itemView = (RecyclingImageView)mItemViews.get(position);
		pager.removeView(itemView);
		//Remove invisible item's view to avoid OOM
		mItemViews.remove(position);
		mItemViews.put(position, null);
	}

}
