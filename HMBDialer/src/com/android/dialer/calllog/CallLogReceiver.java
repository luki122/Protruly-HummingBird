/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.dialer.calllog;

import com.mediatek.dialer.util.PhoneInfoUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.VoicemailContract;
import android.util.Log;

/**
 * Receiver for call log events.
 * <p>
 * It is currently used to handle {@link VoicemailContract#ACTION_NEW_VOICEMAIL} and
 * {@link Intent#ACTION_BOOT_COMPLETED}.
 */
public class CallLogReceiver extends BroadcastReceiver {
	private static final String TAG = "CallLogReceiver";
	private Context context;

	//桌面角标
	private boolean setUnreadNumber(int count){
		String method = "setBadge";
		Bundle b = new Bundle();
		b.putInt("count",count);
		try {
			Uri uri = Uri.parse("content://com.android.dlauncher.badge/badge");
			Bundle bundle = context.getContentResolver().call(uri, method, null, b);
			if (bundle != null && bundle.getBoolean("result")) {
				Log.d("Badge", "setUnreadNumber true");
				return true;
			} else {
				Log.d("Badge", "setUnreadNumber false");
				return false;
			}
		}catch (Exception e){
			Log.d("Badge", "setUnreadNumber exception : " + e.toString());
			e.printStackTrace();
			return false;
		}
	}

	public static final String ACTION_UNREAD_CHANGED = "com.hb.dialer.action.UNREAD_CHANGED";
	public static final String EXTRA_UNREAD_NUMBER = "com.mediatek.intent.extra.UNREAD_NUMBER";
	
	@Override
	public void onReceive(Context context, Intent intent) {   	
		this.context=context;
		Log.d(TAG,"onReceive:"+intent);
		if (VoicemailContract.ACTION_NEW_VOICEMAIL.equals(intent.getAction())) {
			CallLogNotificationsService.updateVoicemailNotifications(context, intent.getData());
		} else if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			CallLogNotificationsService.updateVoicemailNotifications(context, null);
			//            CallLogNotificationsService.initializeYellowPage(context);
		} else if (ACTION_UNREAD_CHANGED.equals(intent.getAction())) {//显示桌面角标
			int newCallsCount=intent.getIntExtra(EXTRA_UNREAD_NUMBER, 0);
			Log.d(TAG,"ACTION_UNREAD_CHANGED,newCallsCount:"+newCallsCount);
			setUnreadNumber(newCallsCount);
		} else {
			Log.w(TAG, "onReceive: could not handle: " + intent);
		}
	}
}
