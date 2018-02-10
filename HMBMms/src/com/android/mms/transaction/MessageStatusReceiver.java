/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2007 Esmertec AG.
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.mms.transaction;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;
import android.os.Parcelable;
import android.util.Log;

public class MessageStatusReceiver extends BroadcastReceiver {
    public static final String TAG = "Mms/StatusReceiver";

    public static final String MESSAGE_STATUS_RECEIVED_ACTION =
            "com.android.mms.transaction.MessageStatusReceiver.MESSAGE_STATUS_RECEIVED";

    /// M:Code analyze 001, new members @{
    public static final String MMS_READ_STATE_CHANGE = "MMS_READ_STATE_CHANGE";
    /// @}

    //lichao add for show Unread number on hummingbird Luancher
    public static final String MMS_UNREAD_CHANGED = "com.android.mms.UNREAD_CHANGED";
    public static final int INVALID_COUNT = -1;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (MESSAGE_STATUS_RECEIVED_ACTION.equals(intent.getAction()) ||
                /// M:Code analyze 002, add a action @{
                MMS_READ_STATE_CHANGE.equals(intent.getAction())) {
            /// @}
            intent.setClass(context, MessageStatusService.class);
            context.startService(intent);
        }
        //lichao add for show Unread number on hummingbird Luancher begin
        else if (MMS_UNREAD_CHANGED.equals(intent.getAction())) {
            ComponentName component = intent.getParcelableExtra(Intent.EXTRA_UNREAD_COMPONENT);
            ComponentName c = new ComponentName("com.android.mms", "com.android.mms.ui.BootActivity");
            int unreadCount = intent.getIntExtra(Intent.EXTRA_UNREAD_NUMBER, INVALID_COUNT);
            if (component.equals(c) && unreadCount != INVALID_COUNT) {
                setUnreadNumber(context, unreadCount);
            }
        }
        //lichao add for show Unread number on hummingbird Luancher end
    }

    //lichao add for show Unread number on hummingbird Luancher begin
    private boolean setUnreadNumber(Context context, int count){
        String method = "setBadge";
        Bundle b = new Bundle();
        b.putInt("count",count);
        try {
            Uri uri = Uri.parse("content://com.android.dlauncher.badge/badge");
            Bundle bundle = context.getContentResolver().call(uri, method, null, b);
            if (bundle != null && bundle.getBoolean("result")) {
                Log.d(TAG, "setUnreadNumber true");
                return true;
            } else {
                Log.d(TAG, "setUnreadNumber false");
                return false;
            }
        }catch (Exception e){
            Log.d(TAG, "setUnreadNumber exception : " + e.toString());
            e.printStackTrace();
            return false;
        }
    }
    //lichao add for show Unread number on hummingbird Luancher end
}
