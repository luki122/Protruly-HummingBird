package com.protruly.clouddata.appdata.common;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.provider.Settings.System;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import com.protruly.clouddata.appdata.object.GpuInfo;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.microedition.khronos.opengles.GL10;




public class CommonUtils
{
  public static final int NETWORK_TYPE_NONE = 0;
  public static final int NETWORK_TYPE_WIFI = 1;
  public static final int NETWORK_TYPE_MOBILE = 2;
  public static final int NETWORK_TYPE_WAP = 3;
   public static String province = "";
   public static String city = "";
  
  public static boolean isNetworkConnected(Context context)
  {
     ConnectivityManager manager = (ConnectivityManager)context
       .getApplicationContext().getSystemService("connectivity");
    
     if (manager == null) {
       return false;
    }
    
     NetworkInfo networkinfo = manager.getActiveNetworkInfo();
     if ((networkinfo == null) || (!networkinfo.isAvailable())) {
       return false;
    }
    
     return true;
  }
  
  public static String getAppKey(Context context) {
     String str = null;
    try {
       PackageManager localPackageManager = context
         .getPackageManager();
       ApplicationInfo localApplicationInfo = localPackageManager
         .getApplicationInfo(context.getPackageName(), 128);
       if (localApplicationInfo == null) {
         Log.warn("AppDataAgent", 
           "Could not read CLOUDDATA_APPKEY meta-data from AndroidManifest.xml.");
      } else {
         str = 
           localApplicationInfo.metaData.getString("CLOUDDATA_APPKEY");
         if (TextUtils.isEmpty(str)) {
           Log.warn("AppDataAgent", 
             "Could not read CLOUDDATA_APPKEY meta-data from AndroidManifest.xml.");
        } else {
           str = str.trim();
           if (str.length() != 16)
             str = null;
        }
      }
    } catch (Exception localException) {
       Log.error(
         "AppDataAgent", 
         "Could not read CLOUDDATA_APPKEY meta-data from AndroidManifest.xml.", 
         localException);
    }
     return str;
  }
  
  public static String getChannel(Context context) {
     String str = null;
    try {
       PackageManager localPackageManager = context
         .getPackageManager();
       ApplicationInfo localApplicationInfo = localPackageManager
         .getApplicationInfo(context.getPackageName(), 128);
       if (localApplicationInfo == null) {
         str = "";
         Log.warn("AppDataAgent", 
           "Could not read APPDATA_CHANNEL meta-data from AndroidManifest.xml.");
      }
      else {
         if (localApplicationInfo.metaData.containsKey("APPDATA_CHANNEL")) {
           Object localObject = localApplicationInfo.metaData
             .get("APPDATA_CHANNEL");
           if (localObject != null)
             str = localObject.toString();
        }
         if (TextUtils.isEmpty(str)) {
           str = "";
           Log.warn("AppDataAgent", 
             "Could not read APPDATA_CHANNEL meta-data from AndroidManifest.xml.");
        } else {
           str = str.trim();
        }
      }
    } catch (Exception localException) {
       str = "";
       Log.error(
         "AppDataAgent", 
         "Could not read APPDATA_CHANNEL meta-data from AndroidManifest.xml.", 
         localException);
    }
    
     return str;
  }
  
  public static String getAppVersionName(Context context) {
    try {
       PackageInfo localPackageInfo = context.getPackageManager()
         .getPackageInfo(context.getPackageName(), 0);
       return localPackageInfo.versionName;
    } catch (PackageManager.NameNotFoundException localNameNotFoundException) {
       Log.warn("AppDataAgent", "error in getAppVersionCode", 
         localNameNotFoundException);
    }
     return "Unknown";
  }
  
  public static String getAppName(Context context) {
    try {
       PackageInfo localPackageInfo = context.getPackageManager()
         .getPackageInfo(context.getPackageName(), 0);
       return localPackageInfo.applicationInfo.loadLabel(context.getPackageManager()).toString();
    } catch (PackageManager.NameNotFoundException localNameNotFoundException) {
       Log.warn("AppDataAgent", "error in getAppName", 
         localNameNotFoundException);
    }
     return "Unknown";
  }
  
  public static String getAppVersionCode(Context context) {
    try {
       PackageInfo localPackageInfo = context.getPackageManager()
         .getPackageInfo(context.getPackageName(), 0);
      
       return String.valueOf(localPackageInfo.versionCode);
    } catch (PackageManager.NameNotFoundException localNameNotFoundException) {
       Log.warn("AppDataAgent", "error in getAppVersionCode", 
         localNameNotFoundException);
    }
     return "Unknown";
  }
  
  public static int getTimeZone(Context context) {
     int i = 8;
    try {
       Locale localLocale = context.getResources().getConfiguration().locale;
       Calendar localCalendar = Calendar.getInstance(localLocale);
       i = localCalendar.getTimeZone().getRawOffset() / 3600000;
    } catch (Exception localException) {
       Log.info("AppDataAgent", "error in getTimeZone", localException);
    }
     return i;
  }
  
  public static String getCountry(Context context) {
     String strCountry = null;
    try {
       Locale localLocale = context.getResources().getConfiguration().locale;;
       if (localLocale != null)
         strCountry = localLocale.getCountry();
       if (TextUtils.isEmpty(strCountry))
         strCountry = "Unknown";
    } catch (Exception localException) {
       Log.warn("AppDataAgent", "error in getCountry", localException);
    }
     return strCountry;
  }
  
  public static String getLanguage(Context context) {
     String strLang = null;
    try {
       Locale localLocale = context.getResources().getConfiguration().locale;//getConfigLocale(context);
       if (localLocale != null)
         strLang = localLocale.getLanguage();
       if (TextUtils.isEmpty(strLang))
         strLang = "Unknown";
    } catch (Exception localException) {
       Log.warn("AppDataAgent", "error in getLanguage", localException);
    }
     return strLang;
  }
  
//  private static Locale getConfigLocale(Context context) {
//     Locale localLocale = null;
//    try {
//       Configuration localConfiguration = new Configuration();
//       Settings.System.getConfiguration(context.getContentResolver(), 
//         localConfiguration);
//       if (localConfiguration != null)
//         localLocale = localConfiguration.locale;
//    } catch (Exception localException) {
//       Log.warn("AppDataAgent", "error in getConfigLocale");
//    }
//     if (localLocale == null)
//       localLocale = Locale.getDefault();
//     return localLocale;
//  }
//  






















  public static String getAppDeviceId(Context ctx)
  {
     TelephonyManager tm = (TelephonyManager)ctx.getSystemService("phone");
     String deviceId = tm.getDeviceId();
     if ((deviceId == null) || (deviceId.equals(""))) {
       deviceId = "NULL";
    }
    

     return deviceId;
  }
  
  public static String createSessionId(String strSession) {
     String str = null;
     if (strSession != null) {
       StringBuffer localStringBuffer = new StringBuffer();
       byte[] arrayOfByte1 = strSession.getBytes();
      try {
         MessageDigest localMessageDigest = 
           MessageDigest.getInstance("MD5");
         localMessageDigest.reset();
         localMessageDigest.update(arrayOfByte1);
         byte[] arrayOfByte2 = localMessageDigest.digest();
         for (int i = 0; i < arrayOfByte2.length; i++)
           localStringBuffer.append(String.format("%02X", 
             new Object[] { Byte.valueOf(arrayOfByte2[i]) }));
         str = localStringBuffer.toString();
      }
      catch (Exception localException) {}
    }
     return str;
  }
  
  public static Location getLocation(Context context) {
     LocationManager localLocationManager = null;
     Location localLocation = null;
    try
    {
       localLocationManager = (LocationManager)context.getSystemService("location");
       if (checkPermission(context, "android.permission.ACCESS_FINE_LOCATION"))
      {

         localLocation = localLocationManager.getLastKnownLocation("gps");
      }
      
       if (localLocation != null) {
         Log.info("AppDataAgent", "get location from gps:" + localLocation.getLatitude() + 
           "," + localLocation.getLongitude());
         updateAddr(context, localLocation.getLatitude(), localLocation.getLongitude());
      }
      else {
         if (checkPermission(context, "android.permission.ACCESS_COARSE_LOCATION")) {
           Log.info("AppDataAgent", "localLocation from network");
           localLocation = localLocationManager.getLastKnownLocation("network");
        }
        
         if (localLocation != null) {
           Log.info("AppDataAgent", "get location from network:" + 
             localLocation.getLatitude() + "," + 
             localLocation.getLongitude());
           updateAddr(context, localLocation.getLatitude(), localLocation.getLongitude());
        }
      }
      
       if (localLocation == null)
         Log.info(
           "AppDataAgent", 
           "Could not get location from GPS or Cell-id, lack ACCESS_COARSE_LOCATION or ACCESS_COARSE_LOCATION permission?");
    } catch (Exception localException) {
       Log.warn("AppDataAgent", localException.getMessage());
    }
     return localLocation;
  }
  





  public static void updateAddr(Context context, double lat, double lon)
  {
     Geocoder geocoder = new Geocoder(context);
    try
    {
       List<Address> addList = geocoder.getFromLocation(lat, lon, 1);
       if ((addList != null) && (addList.size() > 0)) {
         for (int i = 0; i < addList.size(); i++) {
           Address addr = (Address)addList.get(i);
           province = addr.getAdminArea();
           city = addr.getLocality();
           Log.info("AppDataAgent", "======province:" + province);
           Log.info("AppDataAgent", "======city:" + city);
        }
      }
    } catch (IOException e) {
       e.printStackTrace();
    }
  }
  
  public static boolean checkPermission(Context context, String strPermission) {
     PackageManager localPackageManager = context.getPackageManager();
     return localPackageManager.checkPermission(strPermission, 
       context.getPackageName()) == 0;
  }
  
  public static String getMacAdress(Context context)
  {
    try {
       WifiManager localWifiManager = (WifiManager)context.getSystemService("wifi");
       if (checkPermission(context, "android.permission.ACCESS_WIFI_STATE")) {
         WifiInfo localWifiInfo = localWifiManager.getConnectionInfo();
         String str = localWifiInfo.getMacAddress();
         if (str != null) {
           str = str.toUpperCase();
        }
         return "";
      }
      
       Log.warn(
         "AppDataAgent", 
         "Could not get mac address.[lack permission android.permission.ACCESS_WIFI_STATE");
    } catch (Exception localException) {
       Log.warn("AppDataAgent", "Could not get mac address." + 
         localException.toString());
    }
     return "";
  }
  
  public static String getPackageName(Context context) {
     return context.getPackageName();
  }
  



  public static boolean isNetworkAvailable(Context context)
  {
    try
    {
       ConnectivityManager localConnectivityManager = (ConnectivityManager)context
         .getSystemService("connectivity");
       NetworkInfo localNetworkInfo = localConnectivityManager
         .getActiveNetworkInfo();
       if (localNetworkInfo != null) {
         return localNetworkInfo.isConnected();
      }
    } catch (Exception localException) {}
     return false;
  }
  
  public static String addZeroAtSuffix(String strContent, int nCount) {
     StringBuffer localStringBuffer = new StringBuffer();
     int i = 0;
     if (!TextUtils.isEmpty(strContent)) {
       i = strContent.length();
       localStringBuffer.append(strContent);
    }
     if (i < nCount) {
       for (int j = i; j < nCount; j++)
         localStringBuffer.append("0");
    } else
       return localStringBuffer.substring(0, nCount);
     return localStringBuffer.toString();
  }
  
  public static String getResolution(Context context) {
    try {
       DisplayMetrics localDisplayMetrics = new DisplayMetrics();
       WindowManager localWindowManager = (WindowManager)context
         .getSystemService("window");
       localWindowManager.getDefaultDisplay().getMetrics(
         localDisplayMetrics);
       int i = -1;
       int j = -1;
       if ((context.getApplicationInfo().flags & 0x2000) == 0) {
         i = getDisplayMetricsFieldValueByReflect(localDisplayMetrics, 
           "noncompatWidthPixels");
         j = getDisplayMetricsFieldValueByReflect(localDisplayMetrics, 
           "noncompatHeightPixels");
      }
       if ((i == -1) || (j == -1)) {
         i = localDisplayMetrics.widthPixels;
         j = localDisplayMetrics.heightPixels;
      }
       StringBuffer localStringBuffer = new StringBuffer();
       localStringBuffer.append(i);
       localStringBuffer.append("*");
       localStringBuffer.append(j);
       return localStringBuffer.toString();
    } catch (Exception localException) {
       Log.warn("AppDataAgent", "error in getResolution", localException);
    }
     return "Unknown";
  }
  
  private static int getDisplayMetricsFieldValueByReflect(Object paramObject, String paramString)
  {
    try {
       Field localField = DisplayMetrics.class
         .getDeclaredField(paramString);
       localField.setAccessible(true);
       return localField.getInt(paramObject);
    } catch (Exception localException) {
       localException.printStackTrace();
    }
     return -1;
  }
  
  public static String[] getNetWorkInfo(Context context) {
     String[] arrayOfString = { "Unknown", "Unknown" };
    
     if (checkPermission(context, "android.permission.ACCESS_NETWORK_STATE")) {
       ConnectivityManager localConnectivityManager = (ConnectivityManager)context
         .getSystemService("connectivity");
       if (localConnectivityManager != null) {
         NetworkInfo localNetworkInfo = localConnectivityManager
           .getActiveNetworkInfo();
         if (localNetworkInfo != null)
           if (localNetworkInfo.getType() == 1) {
             arrayOfString[0] = "Wi-Fi";
           } else if (localNetworkInfo.getType() == 0) {
             int i = localNetworkInfo.getSubtype();
             if ((i == 1) || (i == 4) || (i == 2)) {
               arrayOfString[0] = "2G";
            } else
               arrayOfString[0] = "3G";
             arrayOfString[1] = localNetworkInfo.getSubtypeName();
          }
      }
    }
     return arrayOfString;
  }
  
  public static int getNetwrokType(Context context)
  {
     ConnectivityManager manager = (ConnectivityManager)context
       .getApplicationContext().getSystemService("connectivity");
    
     if (manager == null) {
       return 0;
    }
    
     NetworkInfo networkinfo = manager.getActiveNetworkInfo();
     if (networkinfo == null) {
       return 0;
    }
    
     int type = networkinfo.getType();
    
     if (type == 1) {
       return 1;
    }
    
     if (type == 0)
    {


















       return 2;
    }
    
     return 0;
  }
  
  public static String getOperatorName(Context context) {
    try {
       return 
         ((TelephonyManager)context.getSystemService("phone")).getNetworkOperatorName();
    } catch (Exception localException) {
       Log.info("AppDataAgent", "error in getOperatorName", 
         localException);
    }
     return "Unknown";
  }
  
  
  public static String getCupInfo()
  {
	  return "";
  }
  
  public static GpuInfo getGpuInfo(GL10 paramGL10)
  {
     GpuInfo localGpuInfo = null;
    try {
       localGpuInfo = new GpuInfo();
       localGpuInfo.vendor = paramGL10.glGetString(7936);
       localGpuInfo.renderer = paramGL10.glGetString(7937);
    } catch (Exception localException) {
       Log.error("AppDataAgent", "error in getGpuInfo", localException);
    }
     return localGpuInfo;
  }
  
  public static String getNetWorkProxy(Context context) {
    try {
       ConnectivityManager localConnectivityManager = (ConnectivityManager)context
         .getSystemService("connectivity");
       NetworkInfo localNetworkInfo = localConnectivityManager
         .getActiveNetworkInfo();
       if (localNetworkInfo == null)
         return null;
       if (localNetworkInfo.getType() == 1)
         return null;
       String strNetType = localNetworkInfo.getExtraInfo();
       Log.info("AppDataAgent", "net type:" + strNetType);
       if (strNetType == null)
         return null;
       if ((strNetType.equals("cmwap")) || (strNetType.equals("3gwap")) || 
         (strNetType.equals("uniwap")))
         return "10.0.0.172";
    } catch (Exception localException) {
       localException.printStackTrace();
    }
     return null;
  }
  
  public static long getUnixTimestamp() {
     return new Date().getTime() / 1000L;
  }
  
  public static boolean inputCheck(String paramString) {
     Pattern localPattern = Pattern.compile("[\\w\\.]*");
     Matcher localMatcher = localPattern.matcher(paramString);
     return localMatcher.matches();
  }
  





  public static String getSN()
  {
     String strSN = null;
    try {
       strSN = (String)invokeStaticMethod("com.protruly.clouddata.android.server.systeminterface.util.SystemUtil", "getSN");
    }
    catch (Exception e) {
       e.printStackTrace();
    }
     return strSN;
  }
  
  public static Object invokeStaticMethod(String className, String methodName) {
     Object obj = null;
    try
    {
       Class ownerClass = Class.forName(className);
       Method method = ownerClass.getMethod(methodName, null);
       if (method != null) {
         obj = method.invoke(null, new Object[0]);
      }
    } catch (Exception e) {
       e.printStackTrace();
    }
    
     return obj;
  }
  
  public static String getProductName() {
     String strProduct = Build.MODEL;
     strProduct = strProduct.replaceAll(" ", "");
    
     return strProduct;
  }
  



  public static int getSystemVersion()
  {
     int platyer = Build.VERSION.SDK_INT;
    
     return platyer;
  }
  

  public static String getPhoneNum(Context context)
  {
     TelephonyManager tm = (TelephonyManager)context
       .getSystemService("phone");
     String phoneNum = tm.getLine1Number();
     return TextUtils.isEmpty(phoneNum) ? "0000000000" : phoneNum;
  }
  
















  public static String getIMEI(Context context)
  {
     return 
    
       ((TelephonyManager)context.getSystemService("phone")).getSimSerialNumber();
  }
  







  public static String getIMSI(Context context)
  {
     return 
       ((TelephonyManager)context.getSystemService("phone")).getSubscriberId();
  }
  





  public static boolean isRomaing(Context context)
  {
     return 
    
       ((TelephonyManager)context.getSystemService("phone")).isNetworkRoaming();
  }
  
  public static String getCPBVersion() {
     return Build.DISPLAY;
  }
  
  public static String getOSVersion() {
     return Build.VERSION.RELEASE;
  }
  
  public static String getManufacturer() {
     return Build.MANUFACTURER;
  }
  





  public static String getUserCode(String model, String deviceId)
  {
     String userCode = "";
     if ((model != null) && (deviceId != null)) {
       userCode = model + "|" + deviceId;
    }
     return userCode;
  }
  
  public static long getDateTimeBySecond(String str) {
     SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
     long seconds = 0L;
    try {
       seconds = sdf.parse(str).getTime() / 1000L;
    } catch (ParseException e) {
       e.printStackTrace();
    }
     return seconds;
  }
  




  public static boolean isExceedMaxSize(Context context, String str, long maxSize)
  {
     if (str != null) {
       Log.info("AppDataAgent", "str: " + str);
       File f = new File(str);
       if (f.exists()) {
         Log.info("AppDataAgent", "cache data file: " + f.length());
         if (f.length() > maxSize) {
           Log.info("AppDataAgent", "cache data file is full.");
           return true;
        }
      }
    }
     return false;
  }
  




  public static long getFileLength(String filePath)
  {
     long fileLength = 0L;
     if (filePath != null) {
       Log.info("AppDataAgent", "str: " + filePath);
       File f = new File(filePath);
       if (f.exists()) {
         Log.info("AppDataAgent", "cache data file: " + f.length());
         fileLength = f.length();
      }
    }
     return fileLength;
  }
  















  public static String getMEID(Context context)
  {
     String result = null;
     TelephonyManager tm = (TelephonyManager)context.getSystemService("phone");
     if (tm != null) {
       if (tm.getSimSerialNumber() != null) {
         result = tm.getSimSerialNumber();
      } else {
         result = "0000000000";
      }
    }
     return result;
  }
}
