package com.hb.netmanage.utils;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.hb.netmanage.DataCorrect;
import com.hb.netmanage.DataManagerApplication;
import com.hb.netmanage.R;
import com.hb.netmanage.receiver.NetManagerReceiver;
import com.hb.netmanage.receiver.SimStateReceiver;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 工具类
 * 
 * @author zhaolaichao
 */
public class ToolsUtil {

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
	/**
	 *  获得包含联网权限的应用
	 * @param context
	 * @return
	 */
	public static ArrayList<ResolveInfo>  getResolveInfos(Context context) {
		ArrayList<ResolveInfo> netAppInfos = new ArrayList<ResolveInfo>();
		PackageManager pm = context.getPackageManager();
		Intent launchIntent = new Intent(Intent.ACTION_MAIN, null)
				.addCategory(Intent.CATEGORY_LAUNCHER);
		ArrayList<PackageInfo> packageInfos =  (ArrayList<PackageInfo>) pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES | PackageManager.GET_PERMISSIONS);
		List<ResolveInfo> intents = pm.queryIntentActivities(launchIntent,PackageManager.GET_DISABLED_COMPONENTS);
		try {
			for (ResolveInfo resolveInfo : intents) {
//	                  if (0 != (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)) {
//	             	       //过虑系统自身应用
//	                       continue;
//	                   }
				String packageName = resolveInfo.activityInfo.packageName;
				if (isNetworkApp(context, packageName)) {
					boolean isSaved = false;
					for (ResolveInfo addedResolveInfo : netAppInfos) {
						if (resolveInfo.activityInfo.applicationInfo.uid == addedResolveInfo.activityInfo.applicationInfo.uid) {
							isSaved = true;
							break;
						}
					}
					if (!isSaved) {
						netAppInfos.add(resolveInfo);
					}
				}
			}
		} catch ( Exception e) {
			e.printStackTrace();
		}
		return netAppInfos;
	}
	
	 public static boolean isNetworkApp(Context context, String packageName) {
	        return context.getPackageManager().checkPermission("android.permission.INTERNET", packageName) == 0;
	 }
	 
	/**
	 * 检测Service是否已启动
	 * @param context
	 * @param serviceClassName
	 * @return
	 */
	public static boolean isServiceRunning(Context context, String serviceClassName){ 
        final ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE); 
        final List<RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        for (RunningServiceInfo runningServiceInfo : services) { 
        	LogUtil.v("Tootuls", "service>>" + runningServiceInfo.service.getClassName() + "serviceClassName>>>" + serviceClassName);
            if (runningServiceInfo.service.getClassName().equals(serviceClassName)){ 
                return true; 
            } 
        } 
        return false; 
     }

	/**
	 * 检测指定应用进程是否已启动
	 * @param context
	 * @param pkgName
     * @return
     */
	public static boolean isAppRunning(Context context, String pkgName){
		final ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		final List<RunningAppProcessInfo> apps = activityManager.getRunningAppProcesses();

		for (RunningAppProcessInfo runningAppInfo : apps) {
			if (runningAppInfo.processName.equals(pkgName)){
				return true;
			}
		}
		return false;
	}

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
		String simOperator = null;
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
				simOperator = context.getString(R.string.no_card) + context.getString(R.string.sim);
			}
		} catch (Exception e) {
			e.printStackTrace();
			simOperator = context.getString(R.string.un_operator);
		}
		return simOperator;
	}
	
	/**
	 * 获取当前上网卡的卡槽索引
	 * @param context
	 * @return
	 */
	public static int getCurrentNetSimSubInfo(Context context) {
		if (context == null) {
			return -1;
		}
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
	 * 切换成当前上网卡
	 * @param context
	 */
	public static boolean changeNetSim(Context context, int simIndex) {
		boolean state = false;
		SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
		Method method;
		try {
//			getDefaultDataSubId
			method = subscriptionManager.getClass().getDeclaredMethod("getDefaultDataSubId");
			method.setAccessible(true);  
			int mDefaultDataSubid = (Integer) method.invoke(subscriptionManager);
			//获得要上网卡的subId
			int simSub = getIdInDbBySimId(context, simIndex);
			if (mDefaultDataSubid != simSub && simSub >= 0) {
				//设置目标上网卡的subId
				Method defaultDataSubIdMethod = subscriptionManager.getClass().getDeclaredMethod("setDefaultDataSubId", int.class);
				defaultDataSubIdMethod.invoke(subscriptionManager, simSub);
				Toast.makeText(context, R.string.data_switch_started, Toast.LENGTH_SHORT).show();
				state = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return state;
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
		  	LogUtil.d("ToolsUtil", "getActiveSubscriberId=" + retVal + " subId=" + subId);
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
	 *获得当前上网卡的IMSI
	 * @param context
	 * @return
	 */
	public static String getActiveSimImsi(Context context) {
		 int simSlotIndex = ToolsUtil.getCurrentNetSimSubInfo(context);
         if (simSlotIndex == -1) {
       	  return null;
         }
		  int subId = ToolsUtil.getIdInDbBySimId(context, simSlotIndex);
		  String activeDataImsi = ToolsUtil.getActiveSubscriberId(context, subId);
		  return activeDataImsi;
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
     * 设置手机流量上网状态
     * @param context
     * @param mobileDataEnabled
     */
    public static void setMobileDataState(Context context, boolean mobileDataEnabled) {
		TelephonyManager telephonyService = getTeleManager(context);
		try {
			Method setMobileDataEnabledMethod = telephonyService.getClass().getDeclaredMethod("setDataEnabled", boolean.class);
			if (null != setMobileDataEnabledMethod) {
				setMobileDataEnabledMethod.invoke(telephonyService, mobileDataEnabled);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    /**
     * 判断手机流量上网状态
     * @param context
     * @return
     */
	public static boolean getMobileDataState(Context context) {
		TelephonyManager telephonyService = getTeleManager(context);
		try {
			Method getMobileDataEnabledMethod = telephonyService.getClass().getDeclaredMethod("getDataEnabled");
			if (null != getMobileDataEnabledMethod) {
				boolean mobileDataEnabled = (Boolean) getMobileDataEnabledMethod.invoke(telephonyService);
				return mobileDataEnabled;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 检测当的网络（WLAN、3G/2G）状态
	 * @param context Context
	 * @return true 表示网络可用
	 */
	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager connectivity = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {
			NetworkInfo info = connectivity.getActiveNetworkInfo();
			if (info != null && info.isConnected()){
				// 当前网络是连接的
				if (info.getState() == NetworkInfo.State.CONNECTED){
					// 当前所连接的网络可用
					return true;
				}
			}
		}
		return false;
	}
	/**
	 * 更新桌面图标广播
	 */
	public static void updateIconReceiver() {
		String activeImsi = ToolsUtil.getActiveSimImsi(DataManagerApplication.getInstance());
		Intent dataIntent = new Intent(DataCorrect.UPDATE_DATAPLAN_ACTION);
		long sim1TotalData = 0;
		long sim2TotalData = 0;
		sim1TotalData = PreferenceUtil.getLong(DataManagerApplication.getInstance(), DataManagerApplication.mImsiArray[0], PreferenceUtil.DATAPLAN_COMMON_KEY, 0);
		sim2TotalData = PreferenceUtil.getLong(DataManagerApplication.getInstance(), DataManagerApplication.mImsiArray[1], PreferenceUtil.DATAPLAN_COMMON_KEY, 0);
		long totalData = PreferenceUtil.getLong(DataManagerApplication.getInstance(), activeImsi, PreferenceUtil.DATAPLAN_COMMON_KEY, 0);
		long remainData = PreferenceUtil.getLong(DataManagerApplication.getInstance(), activeImsi, PreferenceUtil.REMAIN_DATAPLAN_AFTER_COMMON_KEY, 0);
		dataIntent.putExtra("package_name", DataManagerApplication.getInstance().getPackageName());
		dataIntent.putExtra("sim1_total_data", sim1TotalData);
		dataIntent.putExtra("sim2_total_data", sim2TotalData);
		if (totalData == 0) {
			dataIntent.putExtra("data_icon", R.drawable.tcl_undata);
		} else if (totalData > 0) {
			float reaminRate = remainData * 100 / totalData;
			if (reaminRate > 0 && reaminRate <= 20) {
				dataIntent.putExtra("remain_data_unit", StringUtil.formatFloatDataFlowSizeByKB(DataManagerApplication.getInstance(), remainData));
				dataIntent.putExtra("data_icon", R.drawable.tcl_data_20);
			} else if (reaminRate > 20 && reaminRate <= 40) {
				dataIntent.putExtra("remain_data_unit", StringUtil.formatFloatDataFlowSizeByKB(DataManagerApplication.getInstance(), remainData));
				dataIntent.putExtra("data_icon", R.drawable.tcl_data_40);
			} else if (reaminRate > 40 && reaminRate <= 60) {
				dataIntent.putExtra("remain_data_unit", StringUtil.formatFloatDataFlowSizeByKB(DataManagerApplication.getInstance(), remainData));
				dataIntent.putExtra("data_icon", R.drawable.tcl_data_60);
			} else if (reaminRate > 60 && reaminRate <= 80) {
				dataIntent.putExtra("remain_data_unit", StringUtil.formatFloatDataFlowSizeByKB(DataManagerApplication.getInstance(), remainData));
				dataIntent.putExtra("data_icon", R.drawable.tcl_data_80);
			} else if (reaminRate > 80 && reaminRate <= 100) {
				dataIntent.putExtra("remain_data_unit", StringUtil.formatFloatDataFlowSizeByKB(DataManagerApplication.getInstance(), remainData));
				dataIntent.putExtra("data_icon", R.drawable.tcl_data_100);
			} else {
				dataIntent.putExtra("data_icon", R.drawable.tcl_out_data);
			}
		}
		DataManagerApplication.getInstance().sendBroadcast(dataIntent);
	}
	
	public static void registerHomeKeyReceiver(Context context) {
	     IntentFilter homeFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
	     context.registerReceiver(NetManagerReceiver.getInstance(), homeFilter);
	 }

	 public static  void unregisterHomeKeyReceiver(Context context) {
	     if (null != NetManagerReceiver.getInstance()) {
	    	 context.unregisterReceiver(NetManagerReceiver.getInstance());
	     }
	 }


	/**
	 * 弹出输入法
	 * @param context
     */
	public static void showInputMethod(final Context context) {
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				//自动弹出键盘
				InputMethodManager inputManager=(InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
				inputManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
				//强制隐藏Android输入法窗口
				// inputManager.hideSoftInputFromWindow(edit.getWindowToken(),0);
			}
		},100);

	}

	/**
	 * 飞行模式状态
	 * @param context
	 * @return
     */
	public static boolean getAirPlanMode(Context context) {
		boolean state = false;
		String airState = Settings.System.getString(context.getContentResolver(), android.provider.Settings.Global.AIRPLANE_MODE_ON);
		if (TextUtils.equals(airState, "" + SimStateReceiver.AIRPLANE_MODE_ON)) {
			state = true;
		} else if (TextUtils.equals(airState, "" + SimStateReceiver.AIRPLANE_MODE_OFF)) {
			state = false;
		}
		return state;
	}

	/**
	 * 当前系统语言环境
	 * @param context
	 * @return
     */
	public static boolean isEnglish(Context context) {
		Locale locale = context.getResources().getConfiguration().locale;
		String language = locale.getLanguage();
		LogUtil.v("language>>>", "language>>" + language);
		if (language.endsWith("en")) {
			return true;
		}
		return false;
	}
}
