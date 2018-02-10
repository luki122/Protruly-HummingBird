package com.hmb.upload;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.hmb.upload.service.UploadService;

import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by zhaolaichao on 17-6-20.
 */

public class ToolUtils {
    /**
     * 中国移动
     */
    private final static String[] MOBILE_SIM = {"46000", "46002", "46007", "46020"};
    /**
     * 中国联通
     */
    private final static String[] UNICOM_SIM = {"46001", "46006", "46009"};
    /**
     * 中国电信
     */
    private final static String[] TELECOM_SIM = {"46003", "46005", "46011"};

    public static final String NET_TYPE_MOBILE = "MOBILE";
    public static final String NET_TYPE_WIFI = "WIFI";
    public static final int  SIM_FIRST_INDEX = 0;
    public static final int  SIM_SECOND_INDEX = 1;
    /**
     * 服务器版本协议
     */
    public static final String PROTOCAL_VER = "1";


    /**
     * 获取双卡手机的两个卡的IMSI
     * @param context
     * @return
     */
    public static String[] getIMSI(Context context) {
        TelephonyManager tm = getTeleManager(context);
        int phoneCount = tm.getPhoneCount();
        List<SubscriptionInfo> mSelectableSubInfos = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        if ( null == mSelectableSubInfos || mSelectableSubInfos.size() == 0) {
            return new String[phoneCount];
        }
        // 根据卡状态来创建卡imsi的数组
        String[] imsis = new String[phoneCount];
        for (int i = 0; i < mSelectableSubInfos.size(); i++) {
            SubscriptionInfo subscriptionInfo = mSelectableSubInfos.get(i);
            //获得subId;
            int subscriptionId = subscriptionInfo.getSubscriptionId();
            int simSlotIndex = subscriptionInfo.getSimSlotIndex();
            try {
                Method addMethod = tm.getClass().getDeclaredMethod("getSubscriberId", int.class);
                addMethod.setAccessible(true);
                imsis[simSlotIndex] = (String) addMethod.invoke(tm, subscriptionId);
            }  catch (Exception e) {
                e.printStackTrace();
            }
        }
        return imsis;
    }
    /**
     * 获得sim卡运营商
     * @param context
     * @param imsi
     */
    public static  String  getSimOperator(Context context, String imsi) {
        String simOperator = "";
        try {
            /**
             * 获取SIM卡的IMSI码
             * SIM卡唯一标识：IMSI 国际移动用户识别码（IMSI：International Mobile Subscriber Identification Number）是区别移动用户的标志，
             * 储存在SIM卡中，可用于区别移动用户的有效信息。IMSI由MCC、MNC、MSIN组成，其中MCC为移动国家号码，由3位数字组成，
             * 唯一地识别移动客户所属的国家，我国为460；MNC为网络id，由2位数字组成，
             * 用于识别移动客户所归属的移动网络，中国移动为00，中国联通为01,中国电信为03；MSIN为移动客户识别码，采用等长11位数字构成。
             * 唯一地识别国内GSM移动通信网中移动客户。所以要区分是移动还是联通，只需取得SIM卡中的MNC字段即可
             *  */
            if (!TextUtils.isEmpty(imsi)) {
                boolean isContains = false;
                boolean isMatch = false;
                if (!isMatch) {
                    for(int i = 0; i < MOBILE_SIM.length; i++) {
                        if (imsi.startsWith(MOBILE_SIM[i])) {
                            isContains = true;
                            break;
                        }
                    }
                    if (isContains) {
                        isMatch = true;
                        //中国移动
                        simOperator = context.getString(R.string.china_mobile);
                    }
                }

                if (!isMatch) {
                    for(int i = 0; i < UNICOM_SIM.length; i++) {
                        if (imsi.startsWith(UNICOM_SIM[i])) {
                            isContains = true;
                            break;
                        }
                    }
                    if (isContains) {
                        isMatch = true;
                        //中国联通
                        simOperator = context.getString(R.string.china_unicom);
                    }
                }

                if (!isMatch) {
                    for(int i = 0; i < TELECOM_SIM.length; i++) {
                        if (imsi.startsWith(TELECOM_SIM[i])) {
                            isContains = true;
                            break;
                        }
                    }
                    if (isContains) {
                        isMatch = true;
                        //中国电信
                        simOperator = context.getString(R.string.china_telecom);
                    }
                }
                if (!isMatch) {
                    //其它
                    simOperator = context.getString(R.string.un_operator);
                }
            } else {
                simOperator = "";
            }
        } catch (Exception e) {
            e.printStackTrace();
            simOperator = "";
        }
        return simOperator;
    }

    /**
     * Returns the unique subscriber ID, for example, the IMSI for a GSM phone.
     * Return null if it is unavailable.
     * <p>
     * Requires Permission:
     *   {@link android.Manifest.permission#READ_PHONE_STATE READ_PHONE_STATE}
     */
    public static String getActiveSubscriberId(Context context, int subId) {
        final TelephonyManager tm = getTeleManager(context);
        Method addMethod =  null;
        String retVal = null;
        try {
            addMethod = tm.getClass().getDeclaredMethod("getSubscriberId", int.class);
            addMethod.setAccessible(true);
            retVal = (String) addMethod.invoke(tm, subId);
            Log.d("ToolsUtil", "getActiveSubscriberId=" + retVal + " subId=" + subId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retVal;
    }

    /**
     * 获得TelephonyManager
     * @param context
     * @return
     */
    public static TelephonyManager getTeleManager(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm;
    }

    /**
     * 获取当前上网卡的卡槽索引
     * @param context
     * @return
     */
    public static int getCurrentNetSimSubInfo(Context context) {
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        Method method;
        SubscriptionInfo  subscriptionInfo;
        int simSlotIndex = -1;
        try {
            //通过反射来获取当前上网卡的信息
            method = subscriptionManager.getClass().getDeclaredMethod("getDefaultDataSubscriptionInfo");
            method.setAccessible(true);
            subscriptionInfo = (SubscriptionInfo) method.invoke(subscriptionManager);
            if (subscriptionInfo != null) {
                simSlotIndex = subscriptionInfo.getSimSlotIndex();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return simSlotIndex;
    }

    /**
     * 通过simId来获得subId
     * @param simId 当前sim卡所在的卡槽位置
     * @return
     */
    public static int getIdInDbBySimId(Context context, int simId) {
        Cursor cursor = null;
        Uri uri = Uri.parse("content://telephony/siminfo");
        ContentResolver resolver = context.getContentResolver();
        try {
            cursor = resolver.query(uri, new String[]{"_id", "sim_id"}, "sim_id = ?", new String[]{String.valueOf(simId)}, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(cursor.getColumnIndex("_id"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        return -1;
    }
    /**
     *获得当前上网卡的IMSI
     * @param context
     * @return
     */
    public static String getActiveSimImsi(Context context) {
        int simSlotIndex = ToolUtils.getCurrentNetSimSubInfo(context);
        if (simSlotIndex == -1) {
            return null;
        }
        int subId = ToolUtils.getIdInDbBySimId(context, simSlotIndex);
        String activeDataImsi = ToolUtils.getActiveSubscriberId(context, subId);
        return activeDataImsi;
    }

    /**
     * 获取版本号
     * @return 当前应用的版本号
     */
    public static String getVersion(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            String version = info.versionName;
            return "" + version;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 获得imei
     * @param context
     * @return
     */
    public static String[] getIMEI(Context context) {
        final TelephonyManager tm = getTeleManager(context);
        Method addMethod =  null;
        String[] imeis = null;
        try {
            addMethod = tm.getClass().getDeclaredMethod("getImei", int.class);
            addMethod.setAccessible(true);
            String imei1 = (String) addMethod.invoke(tm, SIM_FIRST_INDEX);
            String imei2 = (String) addMethod.invoke(tm, SIM_SECOND_INDEX);
            imeis = new String[]{imei1, imei2};
            Log.d("ToolsUtil", "getImei=" + imeis);
            return imeis;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String[2];
    }

    /**
     * 获取手机IMEI号
     */
    public static String[] getPhoneIMEIs(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
        int phoneCount = telephonyManager.getPhoneCount();
        String[] imeis = new String[phoneCount];
        if (phoneCount > 1) {
            imeis[0] = telephonyManager.getDeviceId(0);
            imeis[1] = telephonyManager.getDeviceId(1);
        } else {
            imeis[0] = telephonyManager.getDeviceId();
        }
        return imeis;
    }

    /**
     * 是否漫游
     * @param context
     * @return
     */
    public static boolean isRoaming(Context context) {
        final TelephonyManager tm = getTeleManager(context);
        return tm.isNetworkRoaming();
    }

    /**
     * 获取网络状态，wifi,wap,2g,3g.
     *
     * @param context 上下文
     * @return 联网类型
     *
     */
    public static String getNetWorkType(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            String type = networkInfo.getTypeName();
            if (type.equalsIgnoreCase(NET_TYPE_MOBILE)) {
                String proxyHost = System.getProperty("http.proxyHost");
                if(TextUtils.isEmpty(proxyHost)) {
                    return NET_TYPE_MOBILE;
                }
            } else if (type.equalsIgnoreCase(NET_TYPE_WIFI)) {
                return NET_TYPE_WIFI;
            }
        }
        return null;
    }
    /**
     * 获得mac地址
     * @return
     */
    public static String getMacAddress(){
 /*获取mac地址有一点需要注意的就是android 6.0版本后，以下注释方法不再适用，不管任何手机都会返回"02:00:00:00:00:00"这个默认的mac地址，这是googel官方为了加强权限管理而禁用了getSYstemService(Context.WIFI_SERVICE)方法来获得mac地址。*/
        //        String macAddress= "";
//        WifiManager wifiManager = (WifiManager) MyApp.getContext().getSystemService(Context.WIFI_SERVICE);
//        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//        macAddress = wifiInfo.getMacAddress();
//        return macAddress;

        String macAddress = null;
        StringBuffer buf = new StringBuffer();
        NetworkInterface networkInterface = null;
        try {
            networkInterface = NetworkInterface.getByName("eth1");
            if (networkInterface == null) {
                networkInterface = NetworkInterface.getByName("wlan0");
            }
            if (networkInterface == null) {
                return "02:00:00:00:00:02";
            }
            byte[] addr = networkInterface.getHardwareAddress();
            for (byte b : addr) {
                buf.append(String.format("%02X:", b));
            }
            if (buf.length() > 0) {
                buf.deleteCharAt(buf.length() - 1);
            }
            macAddress = buf.toString();
        } catch (SocketException e) {
            e.printStackTrace();
            return "02:00:00:00:00:02";
        }
        return macAddress;
    }


    /**
     * 指定范围内的随机时间
     * @param begin
     * @param end
     * @return
     */
    public static long random(long begin, long end) {
        long rtn = begin + (long) (Math.random() * (end - begin));
        // 如果返回的是开始时间和结束时间，则递归调用本函数查找随机值
        if (rtn == begin || rtn == end) {
            return random(begin, end);
        }
        return rtn;
    }

    /**
     * 获得当前天的起始时间
     * @param hour
     * @param minute
     * @param second
     * @return
     */
    public static  long getStartTime(int hour, int minute, int second){
        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR_OF_DAY, hour);
        todayStart.set(Calendar.MINUTE, minute);
        todayStart.set(Calendar.SECOND, second);
        todayStart.set(Calendar.MILLISECOND, 0);
        return todayStart.getTimeInMillis();
    }

    /**
     * 获得当前天的结束时间
     * @param hour
     * @param minute
     * @param second
     * @return
     */
    public static long getEndTime(int hour, int minute, int second){
        Calendar todayEnd = Calendar.getInstance();
        todayEnd.set(Calendar.HOUR_OF_DAY, hour);
        todayEnd.set(Calendar.MINUTE, minute);
        todayEnd.set(Calendar.SECOND, second);
        todayEnd.set(Calendar.MILLISECOND, 999);
        return todayEnd.getTimeInMillis();
    }

    /**
     *
     * @param context
     * @param mAlarm
     * @param currentStart  是否从当前时间开始计时
     */
    public static void setAlarm(Context context, AlarmManager mAlarm, boolean currentStart) {
        PendingIntent pi = getIntent(context);
        long endTime = ToolUtils.getEndTime(23, 59, 59);
        long currentTime = System.currentTimeMillis();
        //第二天随机时间
        long repeatRandomTime = ToolUtils.random(1 * 60 * 60 * 1000, 23 * 60 * 60 * 1000);
        Log.e("repeatRandomTime", "repeatRandomTime>>>" + repeatRandomTime);
        if (currentStart) {
            Log.e("repeatRandomTime", "下次触发时间>>>" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(endTime + repeatRandomTime)));
            mAlarm.setRepeating(AlarmManager.RTC, currentTime, endTime + repeatRandomTime,  pi);
        } else {
            //从第二天开始计时
            long triggerAtMillis = endTime + repeatRandomTime;
            mAlarm.setRepeating(AlarmManager.RTC, triggerAtMillis, (endTime + 24 * 60 * 60 * 1000) + repeatRandomTime,  pi);
            Log.e("repeatRandomTime", "激活后第二天触发时间>>>" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(endTime + repeatRandomTime)));

        }
    }

    public static PendingIntent getIntent(Context context) {
        Intent alarmIntent = new Intent(context, UploadService.class);
        alarmIntent.putExtra("alarm", "1111");
        PendingIntent pi = PendingIntent.getService(context, 0, alarmIntent, 0);
        return pi;
    }
}
