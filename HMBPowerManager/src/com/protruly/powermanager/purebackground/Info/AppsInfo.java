package com.protruly.powermanager.purebackground.Info;

public class AppsInfo extends PBArrayList<ItemInfo> {

   public AppInfo getAppInfoByName(String packageName){
	   AppInfo appInfo = null;
	   
	   for (int i = 0; i < size(); i++) {
		   appInfo = (AppInfo)get(i);
		   if (appInfo.getPackageName().equals(packageName)) {
			   return appInfo;
		   }
	   }
	   return null;
   }
}