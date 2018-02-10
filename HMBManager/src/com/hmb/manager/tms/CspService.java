package com.hmb.manager.tms;

import com.hmb.manager.aidl.MarkResult;
import com.hmb.manager.aidl.ICspService;
import com.hmb.manager.aidl.RejectSmsResult;
import com.hmb.manager.mark.MarkManager;
import com.hmb.manager.sms.KeyWordFilter;
import com.hmb.manager.sms.SmartSmsManager;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.module.location.LocationManager;
import android.os.Process;

/**
 * 
 * @author lgy
 *
 */
public class CspService extends Service {
	private static final String TAG = "CspService";

	ICspService.Stub stup = new ICspService.Stub() {
		
		public MarkResult getMark(int type, String number) {
			return MarkManager.getMark(type, number);
		}
		
	    public String getTagName(int tagType) {
			return MarkManager.getTagName(tagType);
	    }

	    //tangyisen modify from boolean to RejectSmsResult
		public RejectSmsResult canRejectSms(String number, String smscontent) {
			return SmartSmsManager.canRejectSms(number, smscontent);
		}
		
		public boolean canRejectSmsByKeyWord(String smscontent) {
			return  KeyWordFilter.doFilter(smscontent);
		} 
		
		@Override
		public void updateMark(String name, String number) {
			  MarkManager.UpdateMark(name, number);
		}
		
		@Override
		public String getLocation(String number) {
			LocationManager mLocationManager = ManagerCreatorC.getManager(LocationManager.class);
			String location = mLocationManager.getLocation(number);
			Log.d(TAG, "getLocation() -> mPhoneNumber = "+number + " location = " + location);
			return location;
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return stup;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent != null && intent.getAction().equals("android.intent.action.KillCspService")) {
			killMyself();
		}
		return super.onStartCommand(intent, flags, startId);
	}
	
	private void killMyself() {
        Process.killProcess(Process.myPid());       
	}
}
