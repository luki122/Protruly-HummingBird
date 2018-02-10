package com.hb.netmanage;

import android.app.Application;
import android.content.Context;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;

import com.hb.netmanage.observer.UpdateObserver;
import com.hb.netmanage.service.AppTaskService;
import com.hb.netmanage.utils.LogUtil;
import com.hb.netmanage.utils.PreferenceUtil;
import com.hb.netmanage.utils.ToolsUtil;

import java.util.Arrays;

/**
 * 流量管理应用
 * 
 * @author zhaolaichao
 *
 */
public class DataManagerApplication extends Application {
    private final String TAG = "DataManagerApplication";
	private static DataManagerApplication mApplication;
	/**
	 * 获得sim卡的IMSI
	 */
	public static String[] mImsiArray;

	private UpdateObserver mUpdateObserver;

	public DataManagerApplication() {

	}
	@Override
	public void onCreate() {
		super.onCreate();
		mApplication = this;
		String activeSimImsi = ToolsUtil.getActiveSimImsi(getApplicationContext());
		if (!TextUtils.isEmpty(activeSimImsi)) {
			PreferenceUtil.putString(this, "", PreferenceUtil.CURRENT_ACTIVE_IMSI_KEY, activeSimImsi);
		}
		
		mImsiArray = ToolsUtil.getIMSI(this);
		PreferenceUtil.putString(this, PreferenceUtil.SIM_1, PreferenceUtil.IMSI_KEY, mImsiArray[0]);
		PreferenceUtil.putString(this, PreferenceUtil.SIM_2, PreferenceUtil.IMSI_KEY, mImsiArray[1]);
		LogUtil.e("DataManagerApplication", "imsiArray>>>" + Arrays.toString(mImsiArray));
	}

	public static DataManagerApplication getInstance() {

		return mApplication;
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}

	public void startAccessService(Context context) {
		try {
			if (isAccessibilitySettingsOn(context)) {
				return;
			}
			String settingValue = Settings.Secure.getString(context.getApplicationContext().getContentResolver(),
					Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
			StringBuilder builder = new StringBuilder();
			builder.append(settingValue).append(":").append(AppTaskService.APPTASK_SERVICE);
			Settings.Secure.putString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, builder.toString());
			Settings.Secure.putInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 1);
		} catch (SecurityException e) {
			e.printStackTrace();
		}

	}

	/**
	 * to check if service is enabled
	 * @param mContext
	 * @return
	 */
	private boolean isAccessibilitySettingsOn(Context mContext) {
	     int accessibilityEnabled = 0;
	     boolean accessibilityFound = false;
	     try {
	         accessibilityEnabled = Settings.Secure.getInt(
	                 mContext.getApplicationContext().getContentResolver(),
	                 Settings.Secure.ACCESSIBILITY_ENABLED);
	     } catch (SettingNotFoundException e) {
	         LogUtil.e(TAG, "Error finding setting, default accessibility to not found: "
	                         + e.getMessage());
	     }
	     TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

	     if (accessibilityEnabled == 1) {
	         String settingValue = Settings.Secure.getString(
	                 mContext.getApplicationContext().getContentResolver(),
	                 Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
			 LogUtil.v(TAG, "settingValue>>>" + settingValue);
	         if (settingValue != null) {
	             TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
	             splitter.setString(settingValue);
	             while (splitter.hasNext()) {
	                 String accessabilityService = splitter.next();
	                 if (accessabilityService.equalsIgnoreCase(AppTaskService.APPTASK_SERVICE)) {
	                     return true;
	                 }
	             }
	         }
	     }
		 LogUtil.v(TAG, "accessibilityFound>>>" + accessibilityFound);
	     return accessibilityFound;
    }

	/**
	 * 监听状态改变
	 * @return
	 */
	public UpdateObserver getUpdateObserver() {
		if (mUpdateObserver == null) {
			mUpdateObserver = new UpdateObserver();
		}
		return mUpdateObserver;
	}
}
