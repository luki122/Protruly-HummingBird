package com.protruly.powermanager.purebackground.Info;

import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;

/**
 * Contains some the info necessary to manage an application.
 */
public class AppInfo extends ItemInfo {
	private static final String TAG = AppInfo.class.getSimpleName();

	private int uid;
	private String appName;
	private String appNamePinYin;
	private int versionCode;
	private String versionName;
	private String packageName;
	private ApplicationInfo appInfo;
	private Drawable appIcon;

	private boolean isUserApp = true;
	private boolean isInstalled = true;

	private boolean isHome;
	private boolean isHaveLauncher;

	private ServiceInfo[] services;
	private ActivityInfo[] receivers;

	public AppInfo() {
		super(TAG);
	}

	public void updateObject(AppInfo appInfo){
		if(appInfo != null){
			uid = appInfo.getUid();
			appName = appInfo.getAppName();
			appNamePinYin = appInfo.getAppNamePinYin();
			versionCode = appInfo.getVersionCode();
			versionName = appInfo.getVersionName();
			packageName = appInfo.getPackageName();
			isUserApp = appInfo.getIsUserApp();
			isInstalled = appInfo.getIsInstalled();
			isHome = appInfo.isHome();
			isHaveLauncher = appInfo.isHaveLauncher();
			appIcon = appInfo.getIconDrawable();
			services = appInfo.getServices();
			receivers = appInfo.getReceivers();
		}
	}

	public void setUid(int uid){
		this.uid = uid;
	}

	public int getUid(){
		return this.uid;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getAppNamePinYin(){
		return this.appNamePinYin;
	}

	public void setAppNamePinYin(String appNamePinYin){
		this.appNamePinYin = appNamePinYin;
	}

	public int getVersionCode(){
		return this.versionCode;
	}

	public void setVersionCode(int versionCode){
		this.versionCode = versionCode;
	}

	public String getVersionName() {
		return versionName;
	}

	public void setVersionName(String versionName) {
		this.versionName = versionName;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public void setApplicationInfo(ApplicationInfo appInfo){
		this.appInfo = appInfo;
	}

	public ApplicationInfo getApplicationInfo(){
		return this.appInfo;
	}

	public Boolean getIsUserApp() {
		return isUserApp;
	}

	public void setIsUserApp(Boolean isUserApp) {
		this.isUserApp = isUserApp;
	}

	public void setIsInstalled(boolean isInstalled){
		this.isInstalled = isInstalled;
	}

	public boolean getIsInstalled(){
		return this.isInstalled;
	}

	public boolean isHome(){
		return this.isHome;
	}

	public void setIsHome(boolean isHome){
		this.isHome = isHome;
	}

	public boolean isHaveLauncher(){
		return this.isHaveLauncher;
	}

	public void setIsHaveLauncher(boolean isHaveLauncher){
		this.isHaveLauncher = isHaveLauncher;
	}

	public void setIconDrawable(Drawable appIcon) {
		 this.appIcon = appIcon;
	}

	public Drawable getIconDrawable() {
		return appIcon;
	}

	public ServiceInfo[] getServices() {
		return services;
	}

	public void setServices(ServiceInfo[] services) {
		this.services = services;
	}

	public ActivityInfo[] getReceivers() {
		return receivers;
	}

	public void setReceivers(ActivityInfo[] receivers) {
		this.receivers = receivers;
	}
}