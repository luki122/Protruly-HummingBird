package com.android.launcher3.dynamicui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by liuzuo on 17-6-7.
 */

public class DymBroadcastReceiver extends BroadcastReceiver {
    private final String TAG = "DeskClockDynamic";
    private boolean mIsUpdate =true;
    @Override
    public void onReceive(Context context, Intent intent) {

    }


    public boolean isUpdate() {
        return mIsUpdate;
    }

    public void setUpdate(boolean update) {
        Log.d(TAG,"setUpdate="+update);
        mIsUpdate = update;
    }



}
