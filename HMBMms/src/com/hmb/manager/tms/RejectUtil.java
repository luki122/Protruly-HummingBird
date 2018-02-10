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

package com.hmb.manager.tms;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import com.hmb.manager.aidl.RejectSmsResult;

public class RejectUtil {

    private static final String TAG = "Mms/RejectUtil";
    private static final boolean DEBUG = true;

    public static Uri BLACK_URI = Uri.parse("content://com.hb.contacts/black");
    public static Uri WHITE_URI = Uri.parse("content://com.hb.contacts/white");
    private static final String[] ID_PROJECTION = {BaseColumns._ID};

    public static boolean isWhiteAddress(Context context, String address) {
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = null;
        String number = address.replace("-", "").replace(" ", "");
        try {
            cursor = cr.query(WHITE_URI, ID_PROJECTION, getPhoneNumberEqualString(number),
                    null, null);
            if (cursor == null || cursor.getCount() == 0) {
                return false;
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return true;
    }

    public static boolean isRejectAddress(Context context, String address) {
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = null;
        String number = address.replace("-", "").replace(" ", "");
        try {
            cursor = cr.query(BLACK_URI, ID_PROJECTION, getPhoneNumberEqualString(number),
                    null, null);
            if (cursor == null || cursor.getCount() == 0) {
                return false;
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return true;
    }

    public static BlackEntry getRejectEntryFromAddress(Context context, String address) {
        BlackEntry entry = null;
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = cr.query(BLACK_URI, new String[]{"black_name"},
                    getPhoneNumberEqualString(address), null, null);
        } catch (Exception e) {
            return null;
        }
        if (cursor == null || cursor.getCount() == 0) {
            return null;
        }
        if (cursor.moveToFirst()) {
            String note = cursor.getString(0);
            entry = new BlackEntry(1, note);
        }
        return entry;
    }
    /*public static final String getCurrentCountryIso(Context context) {
        CountryDetector detector = (CountryDetector) context
                .getSystemService(Context.COUNTRY_DETECTOR);
        return detector.detectCountry().getCountryIso();
    }*/

    /*public static final String ACTION_NOTIFY_SMS = "com.hb.interception.ACTION_NOTIFY_SMS";
    public static void sendRejectNotification(Context context) {
        Intent intent = new Intent();
        intent.setAction(ACTION_NOTIFY_SMS);
        context.sendBroadcast(intent);
    }*/

    public static String getPhoneNumberEqualString(String number) {
        return " PHONE_NUMBERS_EQUAL(number, \"" + number + "\", 0) ";
    }

    public static class BlackEntry {
        public int isBlack;
        public String blackNote;

        public BlackEntry(int isBlack, String blackNote) {
            this.isBlack = isBlack;
            this.blackNote = blackNote;
        }
    }

    //read reject settings
    public static Uri SETTING_URI = Uri.parse("content://com.hb.reject/setting");
    public static boolean sIsBlackReject;
    public static boolean sIsCloudReject;
    public static boolean sIsKeywordReject;
    //public static String[] sSettingName = {"sms_black", "sms_smart", "sms_keyword"};
    public static String[] sSettingName = {"name", "value"};

    public static void readBlackSettings(Context context) {
        Cursor c = context.getContentResolver().query(SETTING_URI, null, null, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                String name = c.getString(c.getColumnIndex("name"));
                boolean value = c.getInt(c.getColumnIndex("value")) > 0;
                if ("sms_black".equals(name)) {
                    sIsBlackReject = value;
                } else if ("sms_smart".equals(name)) {
                    sIsCloudReject = value;
                } else if ("sms_keyword".equals(name)) {
                    sIsKeywordReject = value;
                }
            }
        }
    }

    //0--not reject,1--black reject,2--smart cloud reject,3--keyword reject
    public static RejectSmsResult isRejectSms(Context context, String number,
                                              String smsContent) {
        //readBlackSettings(context);//next will register uri
        if (isWhiteAddress(context, number)) {
            return null;
        }
        if (sIsBlackReject) {
            if (isRejectAddress(context, number)) {
                if(DEBUG) Log.w(TAG, "isRejectSms(), Black Rejected! number="+number);
                return new RejectSmsResult(1, "");
            }
        }
        if (TextUtils.isEmpty(smsContent)) {
            return null;
        }
        TmsServiceManager manager = TmsServiceManager.getInstance(context);
        if (manager == null) {
            return null;
        }
        //manager.bindService();
        if (sIsCloudReject) {
            RejectSmsResult result = manager.canRejectSms(number, smsContent);
            if (result != null && result.getReject() == 1) {
                result.setReject(2);
                if(DEBUG) Log.w(TAG, "isRejectSms(), Cloud Rejected! number="+number+", smsContent="+smsContent);
                return result;
            }
        }
        if (sIsKeywordReject) {
            if (manager.canRejectSmsByKeyWord(smsContent)) {
                if(DEBUG) Log.w(TAG, "isRejectSms(), Keyword Rejected! smsContent="+smsContent);
                return new RejectSmsResult(3, "");
            }
        }
        //manager.unbindService();
        return null;
    }

    public static boolean isRejectMms(Context context, String number) {
        //readBlackSettings(context);//next will register uri
        if (isWhiteAddress(context, number)) {
            return false;
        }
        if (sIsBlackReject) {
            return isRejectAddress(context, number);
        }
        return false;
    }

    public static final String ACTION_NOTIFY_SMS = "com.hb.interception.ACTION_NOTIFY_SMS";
    public static void sendRejectNotification(Context context, Boolean isSms,
                                              String address, String smsBody) {
        Intent intent = new Intent();
        intent.setAction(ACTION_NOTIFY_SMS);
        intent.putExtra("isSms", isSms);
        intent.putExtra("number", address);
        if(isSms){
            intent.putExtra("content", smsBody);
        }
        if(DEBUG) Log.w(TAG, "sendRejectNotification(), sendBroadcast: "+intent.toString());
        context.sendBroadcast(intent);
    }

}