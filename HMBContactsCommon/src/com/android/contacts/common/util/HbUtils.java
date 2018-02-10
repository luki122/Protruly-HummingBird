package com.android.contacts.common.util;

import android.text.format.DateUtils;
import com.android.contacts.common.R;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.provider.Settings;

import android.content.Context;
import android.os.storage.StorageManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

public class HbUtils {
	private static final String TAG = "HbUtils";
	public static boolean isMsimIccCardActive() {
		if (isMultiSimEnabledMms()) {
			if (isIccCardActivated(0) && isIccCardActivated(1)) {
				return true;
			}
		}
		return false;
	}
	public static boolean isIccCardActivated(int subscription) {
		TelephonyManager tm = TelephonyManager.getDefault();
		//		log("isIccCardActivated subscription " + tm.getSimState(subscription));
		return (tm.getSimState(subscription) != TelephonyManager.SIM_STATE_ABSENT)
				&& (tm.getSimState(subscription) != TelephonyManager.SIM_STATE_UNKNOWN);
	}
	public static boolean isMultiSimEnabledMms() {
		return TelephonyManager.getDefault().isMultiSimEnabled();
	}

	

	public static String formatTimeStampStringForItem(Context context, long when) {
		boolean is24Hr = "24".equals(Settings.System.getString(context.getContentResolver(), Settings.System.TIME_12_24));
		Time then = new Time();
		then.set(when);
		Time now = new Time();
		now.setToNow();
		int format_flags = 0;/*DateUtils.FORMAT_NO_NOON_MIDNIGHT |
        /// M: Fix ALPS00419488 to show 12:00, so mark DateUtils.FORMAT_ABBREV_ALL
        //DateUtils.FORMAT_ABBREV_ALL |
        ;*/

		// If the message is from a different year, show the date and year.
		if (then.year != now.year) {
			format_flags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
		} else if (now.yearDay - then.yearDay >= 7 ) {
			// If it is from a different day than today, show only the date.
			format_flags |= DateUtils.FORMAT_SHOW_DATE;
		} else if (now.yearDay - then.yearDay >= 3){
			// Otherwise, if the message is from today, show the time.
			format_flags |= DateUtils.FORMAT_SHOW_WEEKDAY;
		} else if (now.yearDay - then.yearDay == 2){
			return context.getString(R.string.date_the_day_before);
		}else if (now.yearDay - then.yearDay == 1){
			return context.getString(R.string.date_yesterday);
		}else if ((now.hour * 60 + now.minute) - (then.hour * 60 + then.minute) >= 5){
			format_flags |= DateUtils.FORMAT_SHOW_TIME;
			if(!is24Hr) {
				format_flags |= DateUtils.FORMAT_CAP_AMPM;
			}
		}else {
			return context.getString(R.string.date_just_now);
		}

		return DateUtils.formatDateTime(context, when, format_flags);
	}

	public static HashMap<String, File> getAllStorages(Context c) {
		StorageManager storageManager = (StorageManager) c
				.getSystemService(Context.STORAGE_SERVICE);
		HashMap<String, File> map=new HashMap<>();
		try {
			Class<?>[] paramClasses = {};
			Method getVolumePathsMethod = StorageManager.class.getMethod(
					"getVolumeList", paramClasses);
			getVolumePathsMethod.setAccessible(true);
			Object[] params = {};
			Object[] invoke = (Object[]) getVolumePathsMethod.invoke(storageManager, params);
			Class<?> volume = Class.forName("android.os.storage.StorageVolume");
			Field[] fields = volume.getDeclaredFields();
			for (Field f:fields) 
				Log.d(TAG,f.getName());
			Field fieldPath = volume.getDeclaredField("mPath");
			Field fieldEmulated = volume.getDeclaredField("mEmulated");
			Field fieldRemove = volume.getDeclaredField("mRemovable");
			fieldPath.setAccessible(true);
			fieldEmulated.setAccessible(true);
			fieldRemove.setAccessible(true);
			for (int i = 0; i < invoke.length; i++) {
				File filePath = (File) fieldPath.get(invoke[i]);
				boolean emulated = fieldEmulated.getBoolean(invoke[i]);
				boolean remove = fieldRemove.getBoolean(invoke[i]);
				Log.i(TAG,"\npath : " + filePath.getPath() + "  isEmulated : " + emulated + "  isRemovable : " + remove);
				if(emulated && !remove) map.put("emulatedPath", filePath);
				else if(!emulated && remove) map.put("sdcardPath", filePath);
			}
			return map;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	} 
}
