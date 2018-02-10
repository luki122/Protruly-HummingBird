package com.hb.thememanager.ui.fragment.ringtone;

import java.io.File;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.hb.thememanager.ThemeManager;
import com.hb.thememanager.ThemeManagerImpl;
import com.hb.thememanager.database.DatabaseFactory;
import com.hb.thememanager.database.SharePreferenceManager;
import com.hb.thememanager.database.ThemeDatabaseController;
import com.hb.thememanager.http.downloader.DownloadInfo;
import com.hb.thememanager.http.downloader.DownloadManager;
import com.hb.thememanager.http.downloader.DownloadService;
import com.hb.thememanager.http.downloader.callback.DownloadListener;
import com.hb.thememanager.http.downloader.exception.DownloadException;
import com.hb.thememanager.model.Ringtone;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.security.MD5Utils;
import com.hb.thememanager.state.ThemeState;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.DialogUtils;
import com.hb.thememanager.utils.FileUtils;
import com.hb.thememanager.utils.TLog;
import com.hb.thememanager.utils.ToastUtils;
import com.hb.thememanager.R;

public class RingtoneDownload  implements DownloadListener{
	
	private DownloadManager mDownloadManager;
	private DownloadInfo mDownloadInfo;
	private Context mContext;
	private Ringtone mRingtone;
	private Downloadinfo mDownloadinfo;
	
	public RingtoneDownload(Context context) {
		// TODO Auto-generated constructor stub
		mContext = context;
		mDownloadManager = DownloadService.getDownloadManager(context);
	}

	public void setDownloadinfo(Downloadinfo info ) {
		mDownloadinfo = info;
	} 
	
	public void startDownload(){
		if(!CommonUtil.hasNetwork(mContext) ){
			ToastUtils.showShortToast(mContext,  R.string.no_network);
			return ;
		}

		mDownloadInfo = buildDownloadInfo(mRingtone);
		mDownloadInfo.setDownloadListener(this);
        
    	mDownloadManager.download(mDownloadInfo);
		Log.e("huliang", "mRingtone.themeFilePath:" + mRingtone.themeFilePath);
	}

	private DownloadInfo buildDownloadInfo(Theme theme){
		mRingtone.themeFilePath = Config.getThemeDownloadPath(theme.type)+MD5Utils.encryptString(theme.name) + ".mp3";
		DownloadInfo info = new DownloadInfo.Builder()
				.setId(theme.id+"")
				.setUrl(theme.downloadUrl)
				.setPath(mRingtone.themeFilePath)
				.build();
		
		return info;
	}
	
	public void setTheme(Ringtone theme ) {
		mRingtone = theme;
	}

	public void onDownloadSuccess() {
		Log.e("huliang", "download success");
		saveToDB();
		scanFile(mContext, mRingtone.themeFilePath);
		if (null != mDownloadinfo) {
			mDownloadinfo.onDownloadSuccess();
		}
	}

	@Override
	public void onDownloadFailed(DownloadException e) {
		Log.d("huliang",""+e);
		if (null != mDownloadinfo) {
			mDownloadinfo.onDownloadFailed(e);
		}
	}

	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		Log.d("huliang","onStart");
	}

	@Override
	public void onWaited() {
		// TODO Auto-generated method stub
		Log.d("huliang","onWaited");
	}

	@Override
	public void onPaused() {
		// TODO Auto-generated method stub
		Log.d("huliang","onPaused");
	}

	@Override
	public void onDownloading(long progress, long size) {
		// TODO Auto-generated method stub
		Log.d("huliang","onDownloading->"+progress);
	}

	@Override
	public void onRemoved() {
		// TODO Auto-generated method stub
		Log.d("huliang","onRemoved");
	}
	
	private void saveToDB() {
        ContentValues values = new ContentValues();
        Cursor cursor = null;

		ThemeDatabaseController<Ringtone> dbController = DatabaseFactory.createDatabaseController(Theme.RINGTONE,  mContext);
        try {
        	cursor = dbController.query(new String[]{Config.DatabaseColumns._ID,Config.DatabaseColumns.LOADED_PATH}, 
        			Config.DatabaseColumns._ID + " = '" + mRingtone.id + "'", null, null, null, null);

			if(cursor != null && cursor.moveToNext()){
        		dbController.updateTheme((Ringtone) mRingtone);
        	}else{
        		dbController.insertTheme((Ringtone) mRingtone);
        	}
			dbController.close();
        } catch (Exception e) {
        	Log.e("huliang", "", e);
        } finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
	
	//
	public boolean hasDownloaded(Ringtone ringtone) {
        ContentValues values = new ContentValues();
        Cursor cursor = null;
        boolean bRet = false;

		ThemeDatabaseController<Ringtone> dbController = DatabaseFactory.createDatabaseController(Theme.RINGTONE,  mContext);
        try {
        	cursor = dbController.query(new String[]{Config.DatabaseColumns.FILE_PATH}, 
        			Config.DatabaseColumns._ID+ " = '" + ringtone.id + "'", null, null, null, null);

			if(cursor != null && cursor.moveToNext()){
				ringtone.themeFilePath = cursor.getString(0);
                Log.e("huliang", "themeFilePath: " + ringtone.themeFilePath );
				File f = new File(cursor.getString(0));
	            if (f.exists()) {
	                bRet = true;
	            }
            } 
            dbController.close();
        } catch (Exception e) {
        	Log.e("huliang", "", e);
        } finally {
			if (cursor != null) {
				cursor.close();
			}
			return bRet;
		}
	}

	/**
     * 通知媒体库更新文件
     * @param context
     * @param filePath 文件全路径
     * 
     * */
    public void scanFile(Context context, String filePath) {
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setData(Uri.fromFile(new File(filePath)));
        context.sendBroadcast(scanIntent);
    }
}
