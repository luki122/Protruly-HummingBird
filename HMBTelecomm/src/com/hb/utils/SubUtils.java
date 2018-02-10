package com.hb.utils;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.IccCardConstants;
import com.android.server.telecom.R;

import java.util.HashMap; 

  
public class SubUtils {  
    private static final String TAG = "SubUtils";
    
    private static HashMap<Integer,Integer> mSubIdSlotIdPairs = new HashMap<Integer,Integer>();

    public static boolean isDoubleCardInsert() {
//    	return SubscriptionManager.from(InCallApp.getInstance()).getActiveSubscriptionInfoCount() == 2;
        String simState0 = TelephonyManager.getTelephonyProperty(0, TelephonyProperties.PROPERTY_SIM_STATE, "UNKNOWN");
        String simState1 = TelephonyManager.getTelephonyProperty(1, TelephonyProperties.PROPERTY_SIM_STATE, "UNKNOWN");
        boolean isSimInsert0 = !TextUtils.isEmpty(simState0) && simState0.equalsIgnoreCase(IccCardConstants.INTENT_VALUE_ICC_READY);
        boolean isSimInsert1 = !TextUtils.isEmpty(simState1) && simState1.equalsIgnoreCase(IccCardConstants.INTENT_VALUE_ICC_READY);
        return isSimInsert0 && isSimInsert1;
    }

    public static int getSubIdbySlot(Context ctx, int slot) {  
    	int subid[] =  SubscriptionManager.getSubId(slot);
    	if(subid != null) {
    		return subid[0];    		
    	}
    	return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }
    
    public static int getSlotBySubId(int subId) {
//     	SubscriptionManager mSubscriptionManager = SubscriptionManager.from(ctx); 
//     	int slot = mSubscriptionManager.getSlotId(subId);
//     	if(slot > -1) {
//     		mSubIdSlotIdPairs.put(subId , slot);
//     	} else if(mSubIdSlotIdPairs.get(subId) != null){
//     		slot = mSubIdSlotIdPairs.get(subId);
//     	}     	
//		return slot;
    	return SubscriptionManager.getSlotId(subId);
    }

    public static boolean isValidPhoneId(int slot) {
//    	return SubscriptionManager.isValidPhoneId(slot);
        return slot == 0 || slot ==1;    	
    }
    
//    public static void setSimSubId(ImageView mSimIcon, int slot) {
//		if (isValidPhoneId(slot) && isDoubleCardInsert()) {
//			mSimIcon.setImageResource(slot > 0 ? R.drawable.sim_icon_2
//					: R.drawable.sim_icon_1);
//			mSimIcon.setVisibility(View.VISIBLE);
//		} else {
//			mSimIcon.setVisibility(View.GONE);
//		}
//    }
    
    public static int getSimRes(int subId) {
    	int slot = getSlotBySubId(subId);
    	if (isValidPhoneId(slot) && isDoubleCardInsert()) {
    		return slot > 0 ? R.drawable.sim_icon_2
				: R.drawable.sim_icon_1;
    	} else {
    		return 0;
    	}
    }

}  