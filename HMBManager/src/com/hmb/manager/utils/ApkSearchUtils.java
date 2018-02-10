package com.hmb.manager.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hb.themeicon.theme.IconManager;
import com.hmb.manager.bean.APKFile;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.util.Log;
import com.hmb.manager.R;
public class ApkSearchUtils {
	public static int INSTALLED = 0;
	public static int UNINSTALLED = 1;
	public static int INSTALLED_UPDATE = 2;

	private Context context;
	private Map<String,APKFile> sdFiles = new HashMap<String,APKFile>();



	public Map<String, APKFile> getSdFiles() {
		return sdFiles;
	}

	public void setSdFiles(Map<String, APKFile> sdFiles) {
		this.sdFiles = sdFiles;
	}

	public ApkSearchUtils(Context context) {
		super();
		this.context = context;
	}

	public void findAllAPKFile(File file) {

		APKFile myFile = null;
		if (file.isFile()) {
			String name_s = file.getName();
			myFile = new APKFile();
			String apk_path = null;
			if (name_s.toLowerCase().endsWith(".apk")) {
				apk_path = file.getAbsolutePath();
				PackageManager pm = context.getPackageManager();
				PackageInfo packageInfo = pm.getPackageArchiveInfo(apk_path, PackageManager.GET_ACTIVITIES);
				if(packageInfo!=null){
				ApplicationInfo appInfo = packageInfo.applicationInfo;

				appInfo.sourceDir = apk_path;
				appInfo.publicSourceDir = apk_path;

                IconManager iconManager = IconManager.getInstance(context, true, false);
                Drawable apk_icon = iconManager.getIconDrawable(appInfo.packageName, UserHandle.CURRENT);
                if (apk_icon == null) {
                    apk_icon = appInfo.loadIcon(pm);
                }

				String appLabel=pm.getApplicationLabel(appInfo).toString();
				String packageName = packageInfo.packageName;
				if(appLabel!=null&&!appLabel.trim().equals("")){
				myFile.setAppLabel(appLabel);
				}else if(packageName!=null){
					myFile.setAppLabel(packageName);
				}else{
					myFile.setAppLabel(context.getString(R.string.apk_unknow_name));
				}
				myFile.setIcon(apk_icon);
				myFile.setmPackageName(packageName);
				myFile.setmFilePath(file.getAbsolutePath());
				myFile.setmSize(file.length());
				String versionName = packageInfo.versionName;
				myFile.setmVersionName(versionName);
				int versionCode = packageInfo.versionCode;
				myFile.setmVersionCode(versionCode);
				int type = doType(pm, packageName, versionCode);
				myFile.setmInstalled(type);
                if(!sdFiles.containsKey(packageName)){
				sdFiles.put(packageName,myFile);
                }
				}
			}
			// String apk_app = name_s.substring(name_s.lastIndexOf("."));
		} else {
			File[] files = file.listFiles();
			if (files != null && files.length > 0) {
				for (File file_str : files) {
					findAllAPKFile(file_str);
				}
			}
		}
	}

	private int doType(PackageManager pm, String packageName, int versionCode) {
		List<PackageInfo> pakageinfos = pm.getInstalledPackages(0);
		for (PackageInfo pi : pakageinfos) {
			String pi_packageName = pi.packageName;
			int pi_versionCode = pi.versionCode;
			if (packageName.equals(pi_packageName)) {
				// if (versionCode == pi_versionCode) {
				return INSTALLED;
				// } else if (versionCode > pi_versionCode) {
				// return INSTALLED_UPDATE;
				// }
			}
		}
		return UNINSTALLED;
	}
}
