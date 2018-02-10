package com.protruly.powermanager.purebackground.model;

import android.content.Context;
import android.content.pm.PackageInfo;

import com.protruly.powermanager.purebackground.Info.AppInfo;
import com.protruly.powermanager.purebackground.Info.AppsInfo;
import com.protruly.powermanager.utils.ApkUtils;
import com.protruly.powermanager.utils.LogUtils;

import java.util.List;

/**
 * The AppInfoProvider collect all installed application information.
 */
public class AppInfoProvider {
	private final String TAG = AppInfoProvider.class.getSimpleName();

	private final Context context;
	private final AppsInfo sysAppsInfo;
	private final AppsInfo thirdPartyAppsInfo;

	public AppInfoProvider(Context context) {
	    this.context = context;
	    sysAppsInfo = new AppsInfo();
	    thirdPartyAppsInfo = new AppsInfo();
	}

	public int getTotalAppNum() {
		return sysAppsInfo.size() + thirdPartyAppsInfo.size();
	}

	public AppsInfo getSysAppsInfo() {
		return this.sysAppsInfo;
	}

	public AppsInfo getThirdPartyAppsInfo() {
		return this.thirdPartyAppsInfo;
	}

	/**
	 * Initialize AppsInfo according to PackageInfo.
	 * @param packages
     */
	public void initAllAppsInfo(List<PackageInfo> packages) {
		AppsInfo tmpSysAppsInfo = new AppsInfo();
		AppsInfo tmpThirdPartyAppsInfo = new AppsInfo();

		int packagesSize = packages == null ? 0 : packages.size();
		LogUtils.d(TAG, "initAllAppsInfo() -> Parse Begin>>>> packagesSize = " + packagesSize);
		for (int i = 0; i < packagesSize; i++) {
			AppInfo appInfo = null;
			PackageInfo packageInfo = packages.get(i);
			// parse user app
			if (ApkUtils.filterApp(packageInfo.applicationInfo)
					&& (!packageInfo.packageName.equals("com.baidu.map.location"))
					&& (!packageInfo.packageName.equals("com.tencent.android.location"))){
				appInfo = ApkUtils.getAppFullInfo(context, packageInfo);
				if (appInfo == null) {
					continue;
				}
				addForInitAllAppsInfo(tmpThirdPartyAppsInfo, appInfo);
			} else {
				// parse system app
				appInfo = ApkUtils.getAppFullInfo(context, packageInfo);
				if (appInfo == null) {
					continue;
				}
				addForInitAllAppsInfo(tmpSysAppsInfo, appInfo);
			}
		}
		resetDataList(tmpSysAppsInfo, tmpThirdPartyAppsInfo);
		LogUtils.d(TAG, "initAllAppsInfo() -> Parse End>>>> thirdPartyApps = "
				+ thirdPartyAppsInfo.size() + ", sysAppsInfo = " + sysAppsInfo.size());
	}
	
	private void resetDataList(AppsInfo tmpSysAppsInfo, AppsInfo tmpThirdPartyAppsInfo){
		sysAppsInfo.clear();
		thirdPartyAppsInfo.clear();

		if (tmpSysAppsInfo != null) {
			for (int i = 0; i < tmpSysAppsInfo.size(); i++) {
				sysAppsInfo.add(tmpSysAppsInfo.get(i));
			}
		}
		
		if (tmpThirdPartyAppsInfo != null) {
			for (int i = 0; i < tmpThirdPartyAppsInfo.size(); i++) {
				thirdPartyAppsInfo.add(tmpThirdPartyAppsInfo.get(i));
			}		
		}
	}
	
	private void addForInitAllAppsInfo(AppsInfo toAppsInfo, AppInfo needAddAppInfo) {
		String needAddPkgName = needAddAppInfo.getPackageName();
		
		AppsInfo oldAppsInfo;
		if (needAddAppInfo.getIsUserApp()) {
			oldAppsInfo = thirdPartyAppsInfo;
		} else {
			oldAppsInfo = sysAppsInfo;
		}

		for (int i = 0; i < oldAppsInfo.size(); i++) {
            AppInfo oldAppInfo = (AppInfo)oldAppsInfo.get(i);
            if (needAddPkgName.equals(oldAppInfo.getPackageName())) {
                oldAppInfo.updateObject(needAddAppInfo);
                needAddAppInfo = oldAppInfo;
                break;
            }
        }
		toAppsInfo.add(needAddAppInfo);
	}

	/**
	 * Add Appinfo to Appsinfo list.
	 *
	 * @param pkgList
     */
	public synchronized void addAppsInfo(List<String> pkgList){
		if (pkgList == null || pkgList.size() == 0) {
			return ;
		}
		
		PackageInfo packageInfo;
		for (int i = 0; i < pkgList.size(); i++) {
			packageInfo = ApkUtils.getPackageInfo(context, pkgList.get(i));
			if (packageInfo == null) {
				continue;
			}
			
			AppInfo appInfo = null;
			if (ApkUtils.filterApp(packageInfo.applicationInfo)
					&& (!packageInfo.packageName.equals("com.baidu.map.location"))
					&& (!packageInfo.packageName.equals("com.tencent.android.location"))) {
			   appInfo = ApkUtils.getAppFullInfo(context, packageInfo);
			   if (appInfo == null) {
				   continue;
			   }
				addForAddAppsInfo(thirdPartyAppsInfo, appInfo);
			} else {
				appInfo = ApkUtils.getAppFullInfo(context, packageInfo);
				if (appInfo == null) {
					continue;
				}
				addForAddAppsInfo(sysAppsInfo, appInfo);
            }
		}
	}
	
	private void addForAddAppsInfo(AppsInfo toAppsInfo, AppInfo needAddAppInfo){
		String needAddPkgName = needAddAppInfo.getPackageName();
		for(int i = 0; i < toAppsInfo.size(); i++){
			AppInfo oldAppInfo = (AppInfo)toAppsInfo.get(i);
			if (oldAppInfo == null) {
				continue ;
			}
			if (needAddPkgName.equals(oldAppInfo.getPackageName())) {
				oldAppInfo.updateObject(needAddAppInfo);
				return ;
			}
		}		
		toAppsInfo.add(needAddAppInfo);
	}
	
	/**
	 * Remove AppInfo from AppsInfo list
	 * @param pkgList
	 */
	public void removeAppsInfo(List<String> pkgList){
		if (pkgList == null || pkgList.size() == 0) {
			return ;
		}
		
		String pkgName = null;
		for (int i = 0; i < pkgList.size(); i++) {
			pkgName = pkgList.get(i);
			if (!removeAppInfo(thirdPartyAppsInfo, pkgName)) {
				removeAppInfo(sysAppsInfo, pkgName);
			}
		}
	}
	
	private boolean removeAppInfo(AppsInfo fromAppsInfo, String needRemovePkg) {
		boolean result = false;
		if (fromAppsInfo == null || needRemovePkg == null) {
			return result ;
		}
		
		AppInfo checkAppInfo = null;
		for(int i = 0; i < fromAppsInfo.size(); i++){
			checkAppInfo = (AppInfo)fromAppsInfo.get(i);
			if(checkAppInfo != null && needRemovePkg.equals(checkAppInfo.getPackageName())){
				fromAppsInfo.remove(i);
				result = true;
				break;
			}
		}
		return result;
	}
}