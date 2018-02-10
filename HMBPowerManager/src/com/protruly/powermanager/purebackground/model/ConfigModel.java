package com.protruly.powermanager.purebackground.model;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Config Model.
 */
public class ConfigModel {
	private Context context;
	private PackageManager pm;
	private AppInfoModel appInfoModel;

	private static ConfigModel instance;
	
	private ConfigModel(Context context) {
		this.context = context.getApplicationContext();
        appInfoModel = new AppInfoModel(context.getApplicationContext());
	}

	public static synchronized ConfigModel getInstance(Context context) {
		if (instance == null) {
			instance = new ConfigModel(context);
		}
		return instance;
	}

	public AppInfoModel getAppInfoModel() {
		return appInfoModel;
	}
	
	public PackageManager getPackageManager() {
		if (pm == null) {
			pm = context.getPackageManager();
		}
		return pm;
	}
	
	public static void releaseObject(){
		if (instance != null) {
			if (instance.appInfoModel != null) {
				instance.appInfoModel.releaseObject();
			}
		}
		instance = null;
	}
}