package com.android.quicksearchbox.hotsearch;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.os.SystemProperties;

/**
 * Created by lijun on 17-9-7.
 */

public class MobileInfoUtil {

    public static String s_imei;

    public static final String getIMEI(Context context) {
        if(s_imei != null && !s_imei.equals("")){
            return s_imei;
        }
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String imei = telephonyManager.getDeviceId();
            if (imei == null) {
                imei = "";
            }
            s_imei = imei;
            return imei;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

    }

    public static String getIMSI(Context context){
        try {
            TelephonyManager telephonyManager=(TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
            String imsi=telephonyManager.getSubscriberId();
            if(null==imsi){
                imsi="";
            }
            return imsi;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getHMBVersion(){
        String version = SystemProperties.get("ro.sw.version", Build.UNKNOWN);
        if(version!=null && version.length()>0 && !version.equals(Build.UNKNOWN)){
            String[] versions = version.split("_");
            if(versions!=null && versions.length == 5){
                String realVersion = versions[0]+" "+versions[3];
                return realVersion;
            }
        }
        return "";
    }
}
