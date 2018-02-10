package com.hb.thememanager.views;

import java.io.File;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.view.View.OnClickListener;
import com.hb.thememanager.R;
import hb.app.HbActivity;
import com.hb.thememanager.ThemeManager;
import com.hb.thememanager.ThemeManagerApplication;
import com.hb.thememanager.database.SharePreferenceManager;
import com.hb.thememanager.http.downloader.DownloadInfo;
import com.hb.thememanager.http.downloader.DownloadManagerImpl;
import com.hb.thememanager.http.downloader.DownloadService;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.WallpaperUtils;
import com.hb.thememanager.views.WallpaperButton.onWallpaperButtonClickListener;

public class WallpaperSetDialog extends Dialog implements OnClickListener {
	private TextView mSetLockscreen;
	private TextView mSetDesk;
	private TextView mSetTogether;
	private TextView mCancel;
	private Context mContext;
	private DownloadManagerImpl mDm;
	private Wallpaper mWallpaper;
	private ThemeManager mThemeManager;
	private OnWallpaperSetListener mListener;
	public static final int SET_WALLPAPER_LOCKSCREEN = 0;
	public static final int SET_WALLPAPER_DESK = 1;
	public static final int SET_WALLPAPER_TOGETHER = 2;
	
    public WallpaperSetDialog(Context context) {
        this(context, R.style.WallpaperSetDialog);
    }
    public WallpaperSetDialog(Context context, int themeResId) {
        super(context, themeResId);
        init(context);
    }

    public void setWallpaperRes(Wallpaper wallpaper) {
    	mWallpaper = wallpaper;
    }
    
    private void init(Context context) {
    	mContext = context;
    	Resources resources = context.getResources();
		DisplayMetrics dm = resources.getDisplayMetrics();
		
        View dialogView = View.inflate(context, R.layout.item_wallpaper_detail_set_dialog, null);
        initView(dialogView);
        setContentView(dialogView);
        setCanceledOnTouchOutside(false);
        
        Window dialogWindow = this.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = dm.widthPixels;
        dialogWindow.setGravity(Gravity.BOTTOM);
        dialogWindow.setAttributes(lp);
    }
    
    private void initView(View content) {
    	mSetLockscreen = (TextView)content.findViewById(R.id.wallpaper_detail_set_lockscreen);
    	mSetDesk = (TextView)content.findViewById(R.id.wallpaper_detail_set_desktop);
    	mSetTogether = (TextView)content.findViewById(R.id.wallpaper_detail_set_desktop_and_lockscreen);
    	mCancel = (TextView)content.findViewById(R.id.wallpaper_detail_dialog_cancel);
    	
    	mSetLockscreen.setOnClickListener(this);
    	mSetDesk.setOnClickListener(this);
    	mSetTogether.setOnClickListener(this);
    	mCancel.setOnClickListener(this);
    }
    
	@Override
	public void onClick(View v) {
		if(mDm == null) {
			mDm = (DownloadManagerImpl)DownloadService.getDownloadManager(mContext);
		}
		
		File file = null;
		
		if(mWallpaper.themeFilePath != null && mWallpaper.themeFilePath.length() > 0) {
			file = new File(mWallpaper.themeFilePath);
		}else if(mWallpaper.downloadUrl != null && mWallpaper.downloadUrl.length() > 0) {
			DownloadInfo info = mDm.getDownloadById(mWallpaper.downloadUrl.hashCode());
			if(info != null && info.getStatus() == DownloadInfo.STATUS_COMPLETED) {
				String path = info.getPath();
				file = new File(path);
			}
		}
		
		switch (v.getId()) {
			case R.id.wallpaper_detail_set_lockscreen:
				if(file != null && file.exists()) {
					setWallpaper(SET_WALLPAPER_LOCKSCREEN, file.getAbsolutePath());
				}else {
					if(mListener != null) {
						mListener.onSetWallpaper(SET_WALLPAPER_LOCKSCREEN);
					}
				}
				break;
			case R.id.wallpaper_detail_set_desktop:
				if(file != null && file.exists()) {
					setWallpaper(SET_WALLPAPER_DESK, file.getAbsolutePath());
				}else {
					if(mListener != null) {
						mListener.onSetWallpaper(SET_WALLPAPER_DESK);
					}
				}
				break;
			case R.id.wallpaper_detail_set_desktop_and_lockscreen:
				if(file != null && file.exists()) {
					setWallpaper(SET_WALLPAPER_TOGETHER, file.getAbsolutePath());
				}else {
					if(mListener != null) {
						mListener.onSetWallpaper(SET_WALLPAPER_TOGETHER);
					}
				}
				break;
			case R.id.wallpaper_detail_dialog_cancel:
//				dismiss();
				break;
			default:
				break;
		}
		dismiss();
	}
	
	public void setWallpaper(int way, String path) {
		if(mThemeManager == null) {
			ThemeManagerApplication app = (ThemeManagerApplication) ((HbActivity)mContext).getApplicationContext();
			mThemeManager = app.getThemeManager();
		}
		switch (way) {
		case SET_WALLPAPER_LOCKSCREEN:
			WallpaperUtils.setLockScreenWallpaper(mContext, path);	
			SharePreferenceManager.setStringPreference(mContext, SharePreferenceManager.KEY_APPLIED_LOCKSCREEN_WALLPAPER_ID, new File(path).getAbsolutePath());
//			saveLockScreenWallpaper(new File(path));
			break;
		case SET_WALLPAPER_DESK:
			WallpaperUtils.setDesktopWallpaper(mContext, path);
			SharePreferenceManager.setStringPreference(mContext, SharePreferenceManager.KEY_APPLIED_WALLPAPER_ID, new File(path).getAbsolutePath());
//			saveDeskWallpaper(new File(path));
			break;
		case SET_WALLPAPER_TOGETHER:
			WallpaperUtils.setDesktopWallpaper(mContext, path);
			SharePreferenceManager.setStringPreference(mContext, SharePreferenceManager.KEY_APPLIED_WALLPAPER_ID, new File(path).getAbsolutePath());
			WallpaperUtils.setLockScreenWallpaper(mContext, path);
			SharePreferenceManager.setStringPreference(mContext, SharePreferenceManager.KEY_APPLIED_LOCKSCREEN_WALLPAPER_ID, new File(path).getAbsolutePath());
//			saveDeskWallpaper(new File(path));
//			saveLockScreenWallpaper(new File(path));
			break;
		default:
			break;
		}
	}
	
	public void saveDeskWallpaper(File file) {
		if(mThemeManager == null) {
			ThemeManagerApplication app = (ThemeManagerApplication) ((HbActivity)mContext).getApplicationContext();
			mThemeManager = app.getThemeManager();
		}
		if (file.getAbsolutePath() != null) {
	    	long now = System.currentTimeMillis() / 1000;
	        try {
	        	Wallpaper w = new Wallpaper();
				w.id = Config.LocalId.WALLPAPER_PREFIX+now;
				w.loadedPath = file.getAbsolutePath();
				w.name = file.getName();
				w.lastModifiedTime = now;
				w.themeFilePath = file.getAbsolutePath();
				w.loaded = Theme.LOADED;
				w.type = Theme.WALLPAPER;
				mThemeManager.loadThemeIntoDatabase(w);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public void saveLockScreenWallpaper(File file) {
		if(mThemeManager == null) {
			ThemeManagerApplication app = (ThemeManagerApplication) ((HbActivity)mContext).getApplicationContext();
			mThemeManager = app.getThemeManager();
		}
		if (file.getAbsolutePath() != null) {
	    	long now = System.currentTimeMillis() / 1000;
	        try {
	        	Wallpaper w = new Wallpaper();
				w.loadedPath = file.getAbsolutePath();
				w.name = file.getName();
				w.lastModifiedTime = now;
				w.themeFilePath = file.getAbsolutePath();
				w.loaded = Theme.LOADED;
				w.type = Theme.LOCKSCREEN_WALLPAPER;
				w.id = Config.LocalId.LOCK_WALLPAPER_PREFIX+now;
				mThemeManager.loadThemeIntoDatabase(w);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void setOnWallpaperSetListener(OnWallpaperSetListener listener) {
		mListener = listener;
	}
	public interface OnWallpaperSetListener {
		void onSetWallpaper(int way);
	}
}


