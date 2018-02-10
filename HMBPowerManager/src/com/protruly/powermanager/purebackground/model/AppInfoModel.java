package com.protruly.powermanager.purebackground.model;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.protruly.powermanager.purebackground.Info.AppInfo;
import com.protruly.powermanager.purebackground.Info.AppsInfo;
import com.protruly.powermanager.purebackground.interfaces.Subject;
import com.protruly.powermanager.utils.ApkUtils;
import com.protruly.powermanager.utils.LogUtils;
import com.protruly.powermanager.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * AppInfo Model, loading all application information.
 */
public class AppInfoModel extends Subject {
	private static final String TAG = AppInfoModel.class.getSimpleName();

	private Context context;
	private boolean isStopThread;
	private List<UpdateData> updateAppList;
	private AppInfoHandler mAppInfoHandler;
	private AppInfoProvider mAppInfoProvider;

	AppInfoModel(Context context) {
		this.context = context;
		isStopThread = false;
		mAppInfoProvider = new AppInfoProvider(context);
		mAppInfoHandler = new AppInfoHandler(Looper.getMainLooper());
		updateAppList = new ArrayList<>();
		loadAllAppInfo();
	}

	/**
	 * Start load all PackageInfo from PackageManager.
	 */
	public void startLoadAllAppInfo() {
		if (context != null) {
			UpdateData updateData = new UpdateData(UpdateData.GET_ALL_APP_INFO);
			updateData.setPackageInfos(context.getPackageManager().getInstalledPackages(
					PackageManager.GET_PERMISSIONS
					| PackageManager.GET_SERVICES
					| PackageManager.GET_RECEIVERS));
			addUpdateData(updateData);	
		}
	}

	/**
	 * Is already load all AppInfo.
	 *
	 * @return true - load done, false - not load completed
	 */
	public boolean loadAllAppInfoCompleted() {
		int appNumOfAlreadyLoad = mAppInfoProvider.getSysAppsInfo().size() +
				mAppInfoProvider.getThirdPartyAppsInfo().size();
		int appNumOfAll = context.getPackageManager().getInstalledPackages(0).size();
		LogUtils.d(TAG, "loadAllAppInfoCompleted() -> appNumOfAlreadyLoad = " + appNumOfAlreadyLoad
				+ ", appNumOfAll = " + appNumOfAll);
		return appNumOfAlreadyLoad == appNumOfAll;
	}

	/**
	 * Is loading Appinfo.
	 * @return true：loading
	 */
	public boolean isLoadingAppInfo() {
		synchronized (updateAppList) {
			return updateAppList.size() > 0;
		}
	}

	/**
	 * Load all AppInfo.
	 */
	private void loadAllAppInfo() {
		new Thread() {
			@Override
			public void run() {
				UpdateData updateData = null;
				while (!isStopThread) {
					synchronized (updateAppList) {
						if (updateAppList.size() == 0) {
							try {
								LogUtils.d(TAG, "loadAllAppInfo() -> wait loading app data!");
								updateAppList.wait();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						updateData = updateAppList.get(0);
					}

					if (updateData.getType() == UpdateData.GET_ALL_APP_INFO) {
						mAppInfoProvider.initAllAppsInfo(updateData.getPackageInfos());
					} else if (updateData.getType() == UpdateData.INSTALLER_APP) {
						mAppInfoProvider.addAppsInfo(updateData.getPkgNameList());
					} else if (updateData.getType() == UpdateData.UNINSTALLER_APP) {
						mAppInfoProvider.removeAppsInfo(updateData.getPkgNameList());
					} else if (updateData.getType() == UpdateData.EXTERNAL_APP_AVAILABLE) {
						mAppInfoProvider.addAppsInfo(updateData.getPkgNameList());
					} else if (updateData.getType() == UpdateData.EXTERNAL_APP_NOT_AVAILABLE) {
						mAppInfoProvider.removeAppsInfo(updateData.getPkgNameList());
					}

					Message msg = new Message();
					msg.obj = updateData;
					mAppInfoHandler.sendMessage(msg);

					synchronized (updateAppList) {
						LogUtils.d(TAG, "loadAllAppInfo() -> count = " + updateAppList.size());
						updateAppList.remove(updateData);
					}
				}
			}
		}.start();
	}

	private void addUpdateData(UpdateData updateData) {
		synchronized (updateAppList) {
			updateAppList.add(updateData);
			LogUtils.d(TAG, "addUpdateData() -> updateAppList.size = " + updateAppList.size()
					+ ", type = " + updateData.getType());
			if (updateAppList.size() > 0){
				updateAppList.notify();
			}
		}
	}

	public void installOrCoverPackage(String pkgName) {
		if (pkgName == null || StringUtils.isEmpty(pkgName)) {
			return;
		}
		List<String> pkgList = new ArrayList<>();
		pkgList.add(pkgName);
		UpdateData updateData = new UpdateData(UpdateData.INSTALLER_APP);
		updateData.setPkgNameList(pkgList);
		addUpdateData(updateData);
	}

	public void UninstallPackage(String pkgName){
		if (pkgName == null || StringUtils.isEmpty(pkgName)) {
			return ;
		}
		List<String> pkgList = new ArrayList<>();
		pkgList.add(pkgName);
		
		UpdateData updateData = new UpdateData(UpdateData.UNINSTALLER_APP);
		updateData.setPkgNameList(pkgList);
		addUpdateData(updateData);	
	}

	public void externalAppAvailable(List<String> pkgList) {
		UpdateData updateData = new UpdateData(UpdateData.EXTERNAL_APP_AVAILABLE);
		updateData.setPkgNameList(pkgList);
		addUpdateData(updateData);	
	}

	public void externalAppUnAvailable(List<String> pkgList) {
		UpdateData updateData = new UpdateData(UpdateData.EXTERNAL_APP_NOT_AVAILABLE);
		updateData.setPkgNameList(pkgList);
		addUpdateData(updateData);
	}

	private class AppInfoHandler extends Handler {
		public AppInfoHandler(Looper looper) {
           super(looper);
        }

		@Override
	    public void handleMessage(Message msg) {
			UpdateData updateData = (UpdateData)msg.obj;
			if (updateData == null) {
				return;
			}
			
			if (updateData.getType() == UpdateData.GET_ALL_APP_INFO) {
			    if (mAppInfoProvider.getTotalAppNum() > 0) {
				   notifyObserversOfInit();
			    }
			} else if (updateData.getType() == UpdateData.INSTALLER_APP) {
				notifyObserversOfInStall(updateData.getPkgNameList().get(0));
			} else if (updateData.getType() == UpdateData.UNINSTALLER_APP) {
				notifyObserversOfUnInstall(updateData.getPkgNameList().get(0));
			} else if (updateData.getType() == UpdateData.EXTERNAL_APP_AVAILABLE) {
				notifyObserversOfExternalAppAvailable(updateData.getPkgNameList());
			} else if (updateData.getType() == UpdateData.EXTERNAL_APP_NOT_AVAILABLE) {
				notifyObserversOfExternalAppUnAvailable(updateData.getPkgNameList());
			}						
	    }
	}

	public AppInfo findAppInfo(String packageName){
		return findAppInfo(packageName,
				ApkUtils.isUserApp(ApkUtils.getApplicationInfo(context, packageName))
						&& (!packageName.equals("com.baidu.map.location"))
						&& (!packageName.equals("com.tencent.android.location")));
	}

	private AppInfo findAppInfo(String packageName, boolean isUserApp){
		AppInfo appInfo = null;
		if (isUserApp) {
			appInfo = getAppInfo(mAppInfoProvider.getThirdPartyAppsInfo(), packageName);
		} else {
			appInfo = getAppInfo(mAppInfoProvider.getSysAppsInfo(), packageName);
		}
		
		if (appInfo == null) {
			// Not load, load first.
			ArrayList<String> pkgList = new ArrayList<String>();
			pkgList.add(packageName);
			mAppInfoProvider.addAppsInfo(pkgList);
			if (isUserApp) {
				appInfo = getAppInfo(mAppInfoProvider.getThirdPartyAppsInfo(), packageName);
			} else {
				appInfo = getAppInfo(mAppInfoProvider.getSysAppsInfo(), packageName);
			}
		}
		return appInfo;
	}

	private AppInfo getAppInfo(AppsInfo appsInfo, String packageName) {
		if (appsInfo == null || packageName == null) {
			return null;
		}
		
		for (int i = 0; i < appsInfo.size(); i++) {
			AppInfo appInfo = (AppInfo)appsInfo.get(i);
			if (appInfo == null) {
				continue;
			}
			if (packageName.equals(appInfo.getPackageName())) {
				return appInfo;
			}
		}
		return null;	
	}

	public int getUserAppsNum() {
		int num = 0;
		for (int i = 0; i < mAppInfoProvider.getThirdPartyAppsInfo().size(); i++) {
    		AppInfo appInfo = (AppInfo) mAppInfoProvider.getThirdPartyAppsInfo().get(i);
    		if (appInfo == null || !appInfo.getIsInstalled()) {
    			continue;
    		}
    		num++;
    	}
		return num;	
	}

	public int getSysAppsNum() {
		int num = 0;
		for (int i = 0; i < mAppInfoProvider.getSysAppsInfo().size(); i++) {
			AppInfo appInfo = (AppInfo) mAppInfoProvider.getSysAppsInfo().get(i);
			if (appInfo != null && (appInfo.isHaveLauncher() || appInfo.isHome())) {
				num++;
			}				
		}
		return num;
	}

	public AppsInfo getSysAppsInfo() {
		return mAppInfoProvider.getSysAppsInfo();
	}

	public AppsInfo getThirdPartyAppsInfo() {
		return mAppInfoProvider.getThirdPartyAppsInfo();
	}	
	
	public void releaseObject() {
		mAppInfoProvider.getSysAppsInfo().releaseObject();
		mAppInfoProvider.getThirdPartyAppsInfo().releaseObject();
		isStopThread = true;
	}
	
	class UpdateData {
		public static final int GET_ALL_APP_INFO = 1;
		public static final int INSTALLER_APP = 2;
		public static final int UNINSTALLER_APP = 3;
		public static final int EXTERNAL_APP_AVAILABLE = 4;
		public static final int EXTERNAL_APP_NOT_AVAILABLE = 5;
		
		int type;
		List<PackageInfo> PackageInfos;
		List<String> pkgNameList;

		public UpdateData(int type) {
			this.type = type;
		}
		
		/**
		 * Set PackageInfos by PackageManager.
		 * @param PackageInfos
		 */
		public void setPackageInfos(List<PackageInfo> PackageInfos) {
			this.PackageInfos = PackageInfos;
		}

		public int getType() {
			return this.type;
		}

		public List<PackageInfo> getPackageInfos() {
			return this.PackageInfos;
		}
		
		public void setPkgNameList(List<String> pkgNameList) {
			this.pkgNameList = pkgNameList;
		}
		
		public List<String> getPkgNameList() {
			return this.pkgNameList;
		}
	}
}