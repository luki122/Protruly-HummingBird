/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.hmb.manager.tms;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.widget.Toast;

import com.android.mms.MmsApp;
import com.android.mms.transaction.SmsReceiverService;
import com.android.mms.util.MmsLog;
import com.mediatek.mms.util.PermissionCheckUtil;

/**
 * Receive Intent.SMS_REJECTED.  Handle notification that received SMS messages are being
 * rejected. This can happen when the device is out of storage.
 */
public class BlackChangedReceiver extends BroadcastReceiver {
    public final static String BLACK_DELETE_ACTION = "android.intent.action.BLACK_DATABASE_DELETE";
    @Override
    public void onReceive(Context context, Intent intent) {
        /// M: Avoid runtime permission JE @{
        if (!PermissionCheckUtil.checkRequiredPermissions(context)) {
            MmsLog.d(MmsApp.TXN_TAG, "SmsRejectedReceiver: onReceive()"
                    + " no runtime permissions intent=" + intent);
            return;
        }
        if (BLACK_DELETE_ACTION.equals(intent.getAction())) {
            String[] numbers = intent.getStringArrayExtra( BlackChangedService.INTENT_EXTRA_BLACK_NUMS );
            if(numbers == null || numbers.length == 0) {
                return;
            }
            intent.setClass(context, BlackChangedService.class);
            context.startService(intent);
        }
    }
}
