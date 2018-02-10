package com.hb.thememanager.job.loader;

import java.util.ArrayList;

import com.hb.themeicon.theme.IconManager;
import com.hb.thememanager.model.PreviewIcon;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.UserHandle;
import android.util.Log;

public class WallpaperPreviewIconLoader extends
		AsyncTask<String[], Void, ArrayList<PreviewIcon[]>> {

	private IconManager mIconManager;
	private PackageManager mPm;
	private Context mContext;
	private IconLoadCallBack mCallback;

	public interface IconLoadCallBack {
		public void onIconLoaded(ArrayList<PreviewIcon[]> icons);
	}

	public WallpaperPreviewIconLoader(Context context) {
		mContext = context;
		mIconManager = IconManager.getInstance(mContext, true, false);
		mPm = mContext.getPackageManager();
	}

	public void setIconLoadCallback(IconLoadCallBack callback) {
		mCallback = callback;
	}

	@Override
	protected ArrayList<PreviewIcon[]> doInBackground(String[]... pkgName) {
		// TODO Auto-generated method stub
		ArrayList<PreviewIcon[]> icons = new ArrayList<PreviewIcon[]>();
		final PreviewIcon[] topIcons = new PreviewIcon[pkgName[0].length];
		final PreviewIcon[] bottomIcons = new PreviewIcon[pkgName[1].length];
		for (int i = 0; i < pkgName[0].length; i++) {
			final String pkg = pkgName[0][i];
			topIcons[i] = getIconFromPm(pkg);
		}
		for (int i = 0; i < pkgName[1].length; i++) {
			final String pkg = pkgName[1][i];
			bottomIcons[i] = getIconFromPm(pkg);
		}
		icons.add(topIcons);
		icons.add(bottomIcons);

		return icons;
	}

	private PreviewIcon getIconFromPm(String pkgName) {
		ApplicationInfo appInfo = null;
		try {
			appInfo = mPm.getApplicationInfo(pkgName,
					PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (appInfo == null) {
			return null;
		}
		final PreviewIcon icon = new PreviewIcon();
		Drawable dw = mIconManager.getIconDrawable(appInfo.packageName,
				UserHandle.CURRENT);
		if (dw == null) {
			dw = mPm.getApplicationIcon(appInfo);
		}
		icon.setThemeIcon(dw);
		CharSequence appName = mPm.getApplicationLabel(appInfo);
		icon.setName(appName);
		return icon;
	}

	@Override
	protected void onPostExecute(ArrayList<PreviewIcon[]> result) {
		// TODO Auto-generated method stub
		if (mCallback != null) {
			mCallback.onIconLoaded(result);
		}
	}

	public void onDestory(){
		if(mIconManager != null){
			mIconManager.clearCaches();
		}
	}

}
