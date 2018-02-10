package com.hmb.manager.netmanager;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;

import tmsdk.common.TMSDKContext;

/**
 * 插拔手机卡监听广播
 * @author zhaolaichao
 */
public class SimStateReceiver extends BroadcastReceiver {
	private final String TAG = "SimStateReceiver";
	private final static String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
	private final static int DELAY_CHANGED_SEND = 8 * 1000;
	public final static int SIM_VALID = 0;
	public final static int SIM_INVALID = 1;
	private int simState = SIM_INVALID;
	private Context mContext;
	public int getSimState() {
		return simState;
	}

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case DELAY_CHANGED_SEND:
					if (mContext == null) return;
					//更新当前上网卡的imsi
					//这个的作用是通知后台sim卡已变更作一个信息的同步，然后会下发对应的查询码与端口号
					DataManagerManager.setDualPhoneInfoFetcher(DataManagerManager.getIMSI(mContext, true));
					if (DataManagerManager.getInstance().isNetworkAvailable(mContext)) {
						TMSDKContext.onImsiChanged();
					}
					Log.v("SimStateReceiver", ">>" + Arrays.toString(DataManagerManager.getIMSI(mContext, false)));
					break;
			}
		}
	};

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v("SimStateReceiver", "sim state changed>>" + intent.getAction());
		mContext = context;
		if (intent.getAction().equals(ACTION_SIM_STATE_CHANGED)) {
			TelephonyManager tm = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
			int state = tm.getSimState();
			switch (state) {
				case TelephonyManager.SIM_STATE_READY:
					simState = SIM_VALID;
					break;
				case TelephonyManager.SIM_STATE_UNKNOWN:
				case TelephonyManager.SIM_STATE_ABSENT:
				case TelephonyManager.SIM_STATE_PIN_REQUIRED:
				case TelephonyManager.SIM_STATE_PUK_REQUIRED:
				case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
				default:
					simState = SIM_INVALID;
					break;
			}
			if (simState == SIM_VALID) {
				String[] imsis = DataManagerManager.getIMSI(context, false);
				if (TextUtils.isEmpty(imsis[0]) && TextUtils.isEmpty(imsis[1])) {
					return;
				}
				for (int i = 0; i < DataManagerManager.getIMSI(mContext, false).length; i++) {
					TrafficCorrectionWrapper.getInstance().clearTrafficInfo(mContext, i);
				}
				mHandler.sendEmptyMessageDelayed(DELAY_CHANGED_SEND, DELAY_CHANGED_SEND);
			}

		} else if (TextUtils.equals(intent.getAction(), ConnectivityManager.CONNECTIVITY_ACTION)) {
			if (DataManagerManager.getInstance().isNetworkAvailable(mContext)) {
				mHandler.sendEmptyMessageDelayed(DELAY_CHANGED_SEND, 8000);
			}
		}
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
			if (type.equalsIgnoreCase("MOBILE")) {
				String proxyHost = System.getProperty("http.proxyHost");
				if(TextUtils.isEmpty(proxyHost)) {
					return "MOBILE";
				}
			} else if (type.equalsIgnoreCase("WIFI")) {
				return "WIFI";
			}
		}
		return null;
	}

	private boolean isImsiChanged(Context context) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		String sim1Imsi = sp.getString("0", "");
		String sim2Imsi = sp.getString("1", "");
		String[] imsis = DataManagerManager.getIMSI(context, false);
		if (TextUtils.equals(sim1Imsi, imsis[0]) && TextUtils.equals(sim2Imsi, imsis[1])) {
			return false;
		} else {
			return true;
		}
	}
}
