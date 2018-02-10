package com.hmb.upload.receiver;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.hmb.upload.ToolUtils;
import com.hmb.upload.net.HttpUtil;
import com.hmb.upload.service.UploadService;

import java.util.Arrays;

/**
 * Created by zhaolaichao on 17-6-28.
 */

public class AlarmReceiver extends BroadcastReceiver{
    private final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "AlarmReceiver>>>" + intent.getAction());
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (TextUtils.equals(Intent.ACTION_DATE_CHANGED, intent.getAction())) {
            //清除上传标志
            sp.edit().putLong(UploadService.mUploadOkKey, 0).commit();
            Log.v(TAG, ">>>updateTimeClear>>>");
        } else if (TextUtils.equals(ConnectivityManager.CONNECTIVITY_ACTION, intent.getAction())) {
            if (TextUtils.isEmpty(HttpUtil.getNetWorkType(context))) {
                Log.v(TAG, ">net failed>>");
                //没有网络不上传
                return;
            }
            long netTime = sp.getLong("CONNECTIVITY_TIME", 0);
            long startTime = ToolUtils.getStartTime(0, 0, 0);
            if (System.currentTimeMillis() - netTime < 2 * 60 * 1000){
                //过滤重复广播
                return;
            } else {
                sp.edit().putLong("CONNECTIVITY_TIME", System.currentTimeMillis()).commit();
            }
            long updateTimeOk = PreferenceManager.getDefaultSharedPreferences(context).getLong(UploadService.mUploadOkKey, 0);
            Log.v(TAG, ">>>updateTimeOk>>>" + updateTimeOk + ">>startTime>>>" + startTime);
            if (updateTimeOk == 0 || startTime >= updateTimeOk) {
                PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(UploadService.mUploadOkKey, 0).commit();
                setUploadTime(context);
            }
        }

    }

    private synchronized void setUploadTime(Context context) {
        String[] phoneIMEIs = ToolUtils.getPhoneIMEIs(context);
        Log.v(TAG, ">>>phoneIMEIs>>>" + Arrays.toString(phoneIMEIs));
        if(TextUtils.isEmpty(phoneIMEIs[0])) {
            return;
        }
        if (phoneIMEIs.length > 1 && TextUtils.isEmpty(phoneIMEIs[1])) {
            return;
        }
        AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(ToolUtils.getIntent(context));
        ToolUtils.setAlarm(context, alarm, true);
    }
}
