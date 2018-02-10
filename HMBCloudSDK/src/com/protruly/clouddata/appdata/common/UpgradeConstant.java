 package com.protruly.clouddata.appdata.common;
 
 public class UpgradeConstant
 {
   public static final String AUTHORITY = "com.protruly.clouddata.appdata.download.downloadprovider";
   public static String ADVERT_DOWNLOAD_BASE_PATH_DATA = "/mnt/sdcard";
   public static String APK_PATH_DATA = "/upgrade/apk/";
   public static String INSTALL_DOWNLOAD_APK_PATH = ADVERT_DOWNLOAD_BASE_PATH_DATA + APK_PATH_DATA;
   public static final boolean DOWNLOAD_NEED_PROMPT = false;
   public static final int mMaxConcurrentDownloadsAllowed = 5;
   public static final String TYPEUPGRADE_SMARTUPDATE = "0";
   public static final String TYPEUPGRADE_ALLUPDATE = "1";
   public static final String UPGRADE_THIRDAPPUPDATE = "true";
   public static final String UPGRADE_RUNMODE = "normal";
   public static final String UPGRADE_CHANNELID = "appdata";
   public static final int UPDATESTATUS_OK = 1;
   public static final int UPDATESTATUS_NONEUPDATE = 0;
   public static final int UPDATESTATUS_NONEWIFI = -1;
   public static final int UPDATESTATUS_NONENETWORK = -2;
   public static final int UPDATESTATUS_ABNORMAL = -3;
 }
