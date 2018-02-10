/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License.
 */

package com.android.server.telecom;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

public final class TelecomBroadcastIntentProcessor {
    /** The action used to send SMS response for the missed call notification. */
    public static final String ACTION_SEND_SMS_FROM_NOTIFICATION =
            "com.android.server.telecom.ACTION_SEND_SMS_FROM_NOTIFICATION";

    /** The action used to call a handle back for the missed call notification. */
    public static final String ACTION_CALL_BACK_FROM_NOTIFICATION =
            "com.android.server.telecom.ACTION_CALL_BACK_FROM_NOTIFICATION";

    /** The action used to clear missed calls. */
    public static final String ACTION_CLEAR_MISSED_CALLS =
            "com.android.server.telecom.ACTION_CLEAR_MISSED_CALLS";
    
    //add by lgy
    public static final String ACTION_DELETE_CALLLOG=
            "com.android.server.telecom.ACTION_DELETE_CALLLOG";
    
    public static final String ACTION_SHOW_CALLLOG_DETAIL=
            "com.android.server.telecom.ACTION_SHOW_CALLLOG_DETAIL";

    private final Context mContext;
    private final CallsManager mCallsManager;

    public TelecomBroadcastIntentProcessor(Context context, CallsManager callsManager) {
        mContext = context;
        mCallsManager = callsManager;
    }

    public void processIntent(Intent intent) {
        String action = intent.getAction();

        Log.v(this, "Action received: %s.", action);

        MissedCallNotifier missedCallNotifier = mCallsManager.getMissedCallNotifier();
        
        //add by lgy 
        int notificationId = intent.getIntExtra("notificationId",-1);

        // Send an SMS from the missed call notification.
        if (ACTION_SEND_SMS_FROM_NOTIFICATION.equals(action)) {
            // Close the notification shade and the notification itself.
            closeSystemDialogs(mContext);
        	//modify by lgy
            missedCallNotifier.clearMissedCallsById(notificationId);

            Intent callIntent = new Intent(Intent.ACTION_SENDTO, intent.getData());
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivityAsUser(callIntent, UserHandle.CURRENT);

        // Call back recent caller from the missed call notification.
        } else if (ACTION_CALL_BACK_FROM_NOTIFICATION.equals(action)) {
            // Close the notification shade and the notification itself.
            closeSystemDialogs(mContext);
        	//modify by lgy
            missedCallNotifier.clearMissedCallsById(notificationId);

            Intent callIntent = new Intent(Intent.ACTION_CALL_PRIVILEGED, intent.getData());
            //add by lgy for 3429114 start
            int slot = intent.getIntExtra("slot",-2);//-1 不指定；0指定卡1拨号；1指定卡2拨号
            if(slot > -1) {
                callIntent.putExtra("slot",slot);
            }
            Log.d(this, "ACTION_CALL_BACK_FROM_NOTIFICATION slot = " + slot);
            //add by lgy for 3429114 end
            callIntent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            mContext.startActivityAsUser(callIntent, UserHandle.CURRENT);

        // Clear the missed call notification and call log entries.
        } else if (ACTION_CLEAR_MISSED_CALLS.equals(action)) {
        	//modify by lgy
            missedCallNotifier.clearMissedCallsById(notificationId);
        } else if(ACTION_DELETE_CALLLOG.equals(action)) {
            closeSystemDialogs(mContext);
            missedCallNotifier.deleteCallLogById(notificationId);
        } else if(ACTION_SHOW_CALLLOG_DETAIL.equals(action)) {
            closeSystemDialogs(mContext);
            missedCallNotifier.showCallLogDetailById(notificationId);
            missedCallNotifier.clearMissedCallsById(notificationId);
        }
    }

    /**
     * Closes open system dialogs and the notification shade.
     */
    private void closeSystemDialogs(Context context) {
        Intent intent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcastAsUser(intent, UserHandle.ALL);
    }
}
