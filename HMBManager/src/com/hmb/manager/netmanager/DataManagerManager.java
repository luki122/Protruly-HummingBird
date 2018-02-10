package com.hmb.manager.netmanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import tmsdk.common.IDualPhoneInfoFetcher;
import tmsdk.common.TMSDKContext;

/**
 * add by zhaolaichao 20170519
 * 流量管理初始TMSDK参数
 */
public class DataManagerManager {
	private final String TAG = "DataManagerManager";
	// singleton
	private static DataManagerManager sInstance = new DataManagerManager();
	//校正状态
    public boolean[] mCorrectStates = new boolean[2];
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

	public static DataManagerManager getInstance() {
		return sInstance;
	}

	public static void setDualPhoneInfoFetcher(final String[] imsiArray) {
		// TMSDKContext.setDualPhoneInfoFetcher()方法为流量校准支持双卡情况设置，其它情况不需要调用该函数。
		// 该函数中需要返回第一卡槽和第二卡槽imsi的读取内容。
		// 实现此方法时。一定在TMSDKContext.init前调用
		TMSDKContext.setDualPhoneInfoFetcher(new IDualPhoneInfoFetcher() {
			@Override
			public String getIMSI(int simIndex) {
				String imsi = "";
				if (simIndex == IDualPhoneInfoFetcher.FIRST_SIM_INDEX) {
					if (!TextUtils.isEmpty(imsiArray[0])) {
						imsi = imsiArray[0]; // 卡槽1的imsi，需要厂商自己实现获取方法
					}
					Log.v("imsi", "??>>imsi>>11111>>>>" + imsi);
				} else if (simIndex == IDualPhoneInfoFetcher.SECOND_SIM_INDEX) {
					if (!TextUtils.isEmpty(imsiArray[1])) {
						imsi = imsiArray[1]; // 卡槽2的imsi，需要厂商自己实现获取方法
					}
					Log.v("imsi", "??>>imsi>>>22222>>>" + imsi);
				}
				return imsi;
			}
		});
		Log.v("imsi", "??>>imsi>>>>Arrays>>" + Arrays.toString(imsiArray));
	}


	/**
	 * 获取双卡手机的两个卡的IMSI
	 * @param context
	 * @return
	 */
	public static String[] getIMSI(Context context, boolean saveState) {
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		int phoneCount = tm.getPhoneCount();
		List<SubscriptionInfo> mSelectableSubInfos = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
		if ( null == mSelectableSubInfos || mSelectableSubInfos.size() == 0) {
			return new String[phoneCount];
		}
		// 根据卡状态来创建卡imsi的数组
		SharedPreferences.Editor et = PreferenceManager.getDefaultSharedPreferences(context).edit();
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
				if (saveState) {
					et.putString("" + simSlotIndex, imsis[simSlotIndex]);
				}
			}  catch (Exception e) {
				e.printStackTrace();
			}
		}
		et.commit();
		return imsis;
	}

	/**
	 * 检测当的网络（WLAN、3G/2G）状态
	 * @param context Context
	 * @return true 表示网络可用
	 */
	public boolean isNetworkAvailable(Context context) {
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
	 * 运营商代码
	 * @param context
	 * @param simIndex
     * @return
     */
	public static String getOperatorCode(Context context, int simIndex) {
		String code = "";
		String[] imsis = getIMSI(context, false);
		String imsi = imsis[simIndex];
		if (TextUtils.isEmpty(imsi)) {
			return code;
		}
		boolean isContains = false;
		boolean isMatch = false;
		if (!isMatch) {
			for (int i = 0; i < MOBILE_SIM.length; i++) {
				if (imsi.startsWith(MOBILE_SIM[i])) {
					isContains = true;
					break;
				}
			}
			if (isContains) {
				isMatch = true;
				//中国移动
				code = "10086";
			}
		}

		if (!isMatch) {
			for (int i = 0; i < UNICOM_SIM.length; i++) {
				if (imsi.startsWith(UNICOM_SIM[i])) {
					isContains = true;
					break;
				}
			}
			if (isContains) {
				isMatch = true;
				//中国联通
				code = "10010";
			}
		}

		if (!isMatch) {
			for (int i = 0; i < TELECOM_SIM.length; i++) {
				if (imsi.startsWith(TELECOM_SIM[i])) {
					isContains = true;
					break;
				}
			}
			if (isContains) {
				isMatch = true;
				//中国电信
				code = "10001";
			}
		}
		return code;
	}
}
