/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.dialer;

import com.cootek.smartdialer_oem_module.sdk.CooTekPhoneService;

import android.telephony.SubscriptionManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.android.internal.telephony.ITelephony;
import com.baidu.location.b.c;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.RemoteException;
import android.os.Trace;
import android.util.Log;
import android.os.ServiceManager;
import com.android.contacts.common.extensions.ExtensionsFactory;
import com.android.contacts.commonbind.analytics.AnalyticsUtil;
import com.hb.privacy.PrivacyUtils;
import com.mediatek.contacts.GlobalEnv;
import com.mediatek.dialer.dialersearch.DialerSearchHelper;
import com.mediatek.dialer.ext.ExtensionManager;

public class DialerApplication extends Application {

	private static final String TAG = "DialerApplication";
	public static List<Activity> mPrivacyActivityList = new ArrayList<Activity>();
	public static boolean isMultiSimEnabled;//是否启用双卡
	@Override
	public void onCreate() {
		Trace.beginSection(TAG + " onCreate");
		super.onCreate();
		Trace.beginSection(TAG + " ExtensionsFactory initialization");
		ExtensionsFactory.init(getApplicationContext());
		Trace.endSection();
		Trace.beginSection(TAG + " Analytics initialization");
		AnalyticsUtil.initialize(this);
		Trace.endSection();
		/// M: for ALPS01907201, init GlobalEnv for mediatek ContactsCommon
		GlobalEnv.setApplicationContext(getApplicationContext());
		/// M: [MTK Dialer Search] fix ALPS01762713 @{
		DialerSearchHelper.initContactsPreferences(getApplicationContext());
		/// @}
		/// M: For plug-in @{
		ExtensionManager.getInstance().init(this);
		com.mediatek.contacts.ExtensionManager.registerApplicationContext(this);
		/// @}

		//		PrivacyUtils.bindService(this);
		Trace.endSection();
	}



	@Override
	public void onTerminate() {
		// TODO Auto-generated method stub
		super.onTerminate();
		if (CooTekPhoneService.isInitialized()) {
			CooTekPhoneService.getInstance().deinitialize();
		}
	}



	public static boolean isMultiSimEnabled(Context mContext){
		if(mContext==null) return false;
		int activeSubscriptionInfoCount=0;
		try{
			activeSubscriptionInfoCount=SubscriptionManager.from(mContext).getActiveSubscriptionInfoCount();
			Log.d(TAG,"activeSubscriptionInfoCount:"+activeSubscriptionInfoCount);
		}catch(SecurityException e){
			Log.d(TAG,"e:"+e);
			isMultiSimEnabled=false;
			return false;
		}

		if(activeSubscriptionInfoCount<2) {
			isMultiSimEnabled=false;
			return isMultiSimEnabled;
		}

		int[] subIds0=SubscriptionManager.getSubId(0);
		int[] subIds1=SubscriptionManager.getSubId(1);
		int subId0=-1,subId1=-1;
		if(subIds0!=null&&subIds0.length>0) subId0=subIds0[0];
		if(subIds1!=null&&subIds1.length>0) subId1=subIds1[0];

		Log.d(TAG, "subId0:"+subId0+" subId1:"+subId1);

		boolean isRadioOn0=isRadioOn(subId0, mContext);
		boolean isRadioOn1=isRadioOn(subId1,mContext);

		Log.d(TAG, "isRadioOn0:"+isRadioOn0+" isRadioOn1"+isRadioOn1);
		if(isRadioOn0 && isRadioOn1) isMultiSimEnabled=true;
		else isMultiSimEnabled=false;
		return isMultiSimEnabled;
	}

	public static boolean isAnySimEnabled(Context mContext){
		int activeSubscriptionInfoCount=SubscriptionManager.from(mContext).getActiveSubscriptionInfoCount();
		Log.d(TAG,"activeSubscriptionInfoCount:"+activeSubscriptionInfoCount);

		if(activeSubscriptionInfoCount<1) return false;

		int[] subIds0=SubscriptionManager.getSubId(0);
		int[] subIds1=SubscriptionManager.getSubId(1);
		int subId0=-1,subId1=-1;
		if(subIds0!=null&&subIds0.length>0) subId0=subIds0[0];
		if(subIds1!=null&&subIds1.length>0) subId1=subIds1[0];

		boolean isRadioOn0=isRadioOn(subId0, mContext);
		boolean isRadioOn1=isRadioOn(subId1,mContext);

		Log.d(TAG, "subId0:"+subId0+" subId1:"+subId1+" isRadioOn0:"+isRadioOn0+" isRadioOn1:"+isRadioOn1);
		return isRadioOn0 || isRadioOn1;
	}

	public static int getSubId(int slotId){
		int[] subIds=SubscriptionManager.getSubId(slotId);
		int subId=-1;
		if(subIds!=null&&subIds.length>0) {
			subId=subIds[0];
		}
		Log.d(TAG, "subId:"+subId);
		return subId;
	}


	public static boolean isRadioOn(int subId, Context context) {
		ITelephony phone = ITelephony.Stub.asInterface(ServiceManager
				.getService(Context.TELEPHONY_SERVICE));
		boolean isOn = false;
		try {
			// for ALPS02460942, during SIM switch, radio is unavailable, consider it as OFF
			if (phone != null) {
				isOn = subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID ? false :
					phone.isRadioOnForSubscriber(subId, context.getPackageName());
			} else {
				Log.d(TAG, "capability switching, or phone is null ? " + (phone == null));
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		Log.d(TAG, "isOn = " + isOn + ", subId: " + subId);
		return isOn;
	}

	public static int getSingleSubId(Context mContext){
		int[] subIds0=SubscriptionManager.getSubId(0);
		int[] subIds1=SubscriptionManager.getSubId(1);
		int subId0=-1,subId1=-1;
		if(subIds0!=null&&subIds0.length>0) subId0=subIds0[0];
		if(subIds1!=null&&subIds1.length>0) subId1=subIds1[0];

		boolean isRadioOn0=isRadioOn(subId0, mContext);
		boolean isRadioOn1=isRadioOn(subId1,mContext);

		if(isRadioOn0 && !isRadioOn1) return subId0;
		else return subId1;
	}

	private  static DialerApplication sMe;
	public static DialerApplication getInstance() {
		return sMe;
	}

}
