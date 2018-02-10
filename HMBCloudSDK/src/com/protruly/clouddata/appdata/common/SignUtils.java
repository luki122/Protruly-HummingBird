 package com.protruly.clouddata.appdata.common;
 
 import android.content.Context;
 import android.content.pm.PackageInfo;
 import android.content.pm.PackageManager;
 import android.content.pm.Signature;
 
 public class SignUtils
 {
   public static String getSmartApkSignWithMD5(Context context, String pkgName)
   {
     String strSignWithMd5 = null;
     try
     {
       strSignWithMd5 = getApkSign(context, pkgName);
       if (!android.text.TextUtils.isEmpty(strSignWithMd5)) {
         strSignWithMd5 = MD5Utils.getStringMD5Value(strSignWithMd5);
       }
     } catch (Exception e) {
       e.printStackTrace();
     }
     
     return strSignWithMd5;
   }
   
   public static String getApkSign(Context context, String strPackageName) {
     String strSign = null;
     try
     {
       PackageManager mPackageMananger = context.getPackageManager();
       PackageInfo pi = mPackageMananger.getPackageInfo(strPackageName, 64);
       
       if (pi != null) {
         strSign = pi.signatures[0].toCharsString();
       }
     } catch (Exception e) {
       e.printStackTrace();
     }
     
     return strSign;
   }
 }
