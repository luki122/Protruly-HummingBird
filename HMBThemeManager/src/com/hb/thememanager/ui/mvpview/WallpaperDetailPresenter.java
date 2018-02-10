package com.hb.thememanager.ui.mvpview;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.util.Log;
import android.text.TextUtils;
import com.hb.thememanager.BasePresenter;
import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.downloader.DownloadInfo;
import com.hb.thememanager.http.downloader.DownloadManagerImpl;
import com.hb.thememanager.http.downloader.DownloadService;
import com.hb.thememanager.http.request.ThemeRequest;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.job.loader.IRequestTheme;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;

public class WallpaperDetailPresenter extends BasePresenter<WallpaperDetailMVPView> implements IRequestTheme {
	private static final String TAG = "WallpaperDetailPresenter";
	private Http mHttp;
	private Context mContext;
	private DownloadManagerImpl mDm;
	private static List<Wallpaper> mWallpapers = new ArrayList<>();
	public static final int LIMIT_SIZE = 9;
	public static int mCurrentClickWallpaper;
	
	public List<Wallpaper> getWallpapers() {
		return mWallpapers;
	}
	public static void setWallpapers(List<Wallpaper> wallpapers, int currentPage) {
		mWallpapers.clear();
		mWallpapers.addAll(wallpapers);
		mCurrentClickWallpaper = currentPage;
	}
	public void clear() {
		mWallpapers.clear();
	}
	
	public WallpaperDetailPresenter(Context context){
		mContext = context;
		mHttp = Http.getHttp(mContext);
	}
	
	public void updateSaveWallpaperInfo(Wallpaper wallpaper){
		if(mDm == null) {
			mDm = (DownloadManagerImpl)DownloadService.getDownloadManager(mContext);
		}
		if(wallpaper.downloadUrl != null && wallpaper.downloadUrl.length() > 0) {
			DownloadInfo getInfo = mDm.getDownloadById(wallpaper.downloadUrl.hashCode());
			if(getInfo != null && (getInfo.getStatus() == DownloadInfo.STATUS_DOWNLOADING || getInfo.getStatus() == DownloadInfo.STATUS_COMPLETED)) {
				return;
			}
		//	String[] split = wallpaper.downloadUrl.split("/");
		//	String fileName = Config.LOCAL_THEME_WALLPAPER_PATH + split[split.length - 1];
			String fileName = Config.LOCAL_THEME_WALLPAPER_PATH + wallpaper.downloadUrl.hashCode();
//			long createAt = System.currentTimeMillis();
			DownloadInfo info = new DownloadInfo.Builder()
			                         .setUrl(wallpaper.downloadUrl)
//			                         .setCreateAt(createAt)
			                         .setPath(new File(fileName).getAbsolutePath())
			                         .setId(wallpaper.downloadUrl).build();
			getMvpView().updateSaveWallpaperInfo(info);
			mDm.download(info);
		}
	}

	@Override
	public void requestTheme(final ThemeRequest themeType) {
		themeType.request(mHttp, new RawResponseHandler() {
			@Override
			public void onFailure(int statusCode, String error_msg) {
				if(!CommonUtil.hasNetwork(mContext) || statusCode == Http.STATUS_CODE_NETWORK_ERROR) {
					getMvpView().showNetworkErrorView(true);
				}
			}
			
			@Override
			public void onSuccess(int statusCode, String response) {
				if(!TextUtils.isEmpty(response)){
                    Response responseObj = themeType.parseResponse(response);
                    if(responseObj != null){
                        getMvpView().updateViewPagerData(responseObj);
                    }else{
//                        getMvpView().showEmptyView(true);
                    }
                }
			}
		});
	}

	@Override
	public void refresh(ThemeRequest themeType) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void loadMore(ThemeRequest themeType) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onDestory() {
		// TODO Auto-generated method stub
	}
}






