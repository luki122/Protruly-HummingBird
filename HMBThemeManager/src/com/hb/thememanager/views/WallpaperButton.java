package com.hb.thememanager.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import com.hb.thememanager.R;
import com.hb.thememanager.http.downloader.DownloadInfo;
import com.hb.thememanager.http.downloader.DownloadManagerImpl;
import com.hb.thememanager.http.downloader.DownloadService;
import com.hb.thememanager.model.Wallpaper;

public class WallpaperButton extends LinearLayout implements OnClickListener {
	private TextView mSaveWallpaper;
	private TextView mSetWallpaper;
	private onWallpaperButtonClickListener mListener;
	private DownloadManagerImpl mDm;
	private Context mContext;
	private static final int TEXT_COLOR_WHITE = -1;
	private boolean isWhite;

	public WallpaperButton(Context context) {
        this(context, null);
	}
	public WallpaperButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	public WallpaperButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}
	
	private void init(Context context) {
		mContext = context;
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout root = (LinearLayout)inflater.inflate(R.layout.item_wallpaper_button, this);

		mSetWallpaper = (TextView)root.findViewById(R.id.wallpaper_set);
		mSaveWallpaper = (TextView)root.findViewById(R.id.wallpaper_save);
		
		mSetWallpaper.setOnClickListener(this);
		mSaveWallpaper.setOnClickListener(this);
	}
	
	public void setButtonColor(int color) {
    	if(color != TEXT_COLOR_WHITE) {
    		isWhite = false;
    		mSaveWallpaper.setBackgroundResource(R.drawable.shape_wallpaper_save_black);
        	mSetWallpaper.setBackgroundResource(R.drawable.shape_wallpaper_set_black);
	    	mSaveWallpaper.setTextColor(Color.WHITE);
	    	mSetWallpaper.setTextColor(Color.WHITE);
    	}else {
    		isWhite = true;
    		mSaveWallpaper.setBackgroundResource(R.drawable.shape_wallpaper_save_white);
        	mSetWallpaper.setBackgroundResource(R.drawable.shape_wallpaper_set_white);
	    	mSaveWallpaper.setTextColor(Color.BLACK);
	    	mSetWallpaper.setTextColor(Color.BLACK);
    	}
	}
	public void setButtonSavedStatus(boolean wallpaperSaved) {
    	if(wallpaperSaved) {
    		if (isWhite) {
    			mSaveWallpaper.setBackgroundResource(R.drawable.shape_wallpaper_save_apply_white);
			}else {
    			mSaveWallpaper.setBackgroundResource(R.drawable.shape_wallpaper_save_apply_black);
			}
    		mSaveWallpaper.setText(getResources().getString(R.string.wallpaper_detail_save_wallpaper_applied));
    	}else {
    		if (isWhite) {
    			mSaveWallpaper.setBackgroundResource(R.drawable.shape_wallpaper_save_white);
			}else {
    			mSaveWallpaper.setBackgroundResource(R.drawable.shape_wallpaper_save_black);
			}
    		mSaveWallpaper.setText(getResources().getString(R.string.wallpaper_detail_save_wallpaper));
    	}
	}
	public void setButtonSavedStatus(Wallpaper wallpaper) {
		if(mDm == null) {
			mDm = (DownloadManagerImpl)DownloadService.getDownloadManager(mContext);
		}
    	if(wallpaper.themeFilePath != null && wallpaper.themeFilePath.length() > 0) {
    		if (isWhite) {
    			mSaveWallpaper.setBackgroundResource(R.drawable.shape_wallpaper_save_apply_white);
			}else {
    			mSaveWallpaper.setBackgroundResource(R.drawable.shape_wallpaper_save_apply_black);
			}
    		mSaveWallpaper.setText(getResources().getString(R.string.wallpaper_detail_save_wallpaper_applied));
    	}else if(wallpaper.downloadUrl != null && wallpaper.downloadUrl.length() > 0) {
    		DownloadInfo info = mDm.getDownloadById(wallpaper.downloadUrl.hashCode());
        	if(info != null && info.getStatus() == DownloadInfo.STATUS_COMPLETED) {
        		if (isWhite) {
        			mSaveWallpaper.setBackgroundResource(R.drawable.shape_wallpaper_save_apply_white);
    			}else {
        			mSaveWallpaper.setBackgroundResource(R.drawable.shape_wallpaper_save_apply_black);
    			}
        		mSaveWallpaper.setText(getResources().getString(R.string.wallpaper_detail_save_wallpaper_applied));
        	}else {
        		if (isWhite) {
        			mSaveWallpaper.setBackgroundResource(R.drawable.shape_wallpaper_save_white);
    			}else {
        			mSaveWallpaper.setBackgroundResource(R.drawable.shape_wallpaper_save_black);
    			}
        		mSaveWallpaper.setText(getResources().getString(R.string.wallpaper_detail_save_wallpaper));
        	}
    	}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.wallpaper_set:
				if(mListener != null) {
					mListener.onSetButtonClick();
				}
				break;
			case R.id.wallpaper_save:
				if(mListener != null) {
					mListener.onSaveButtonClick();
				}
				break;
			default:
				break;
		}
	}
	
	public void setOnWallpaperButtonClickListener(onWallpaperButtonClickListener listener) {
		mListener = listener;
	}
	public interface onWallpaperButtonClickListener {
		void onSaveButtonClick();
		void onSetButtonClick();
	}
	
	
	
	
	
	
	
	
	
	
	

}




