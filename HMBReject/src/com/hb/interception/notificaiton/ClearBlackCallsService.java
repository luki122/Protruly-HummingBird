package com.hb.interception.notification;

import com.hb.interception.InterceptionApplication;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.provider.CallLog.Calls;
import android.util.Log;

//add by liguangyu for black list 
public class ClearBlackCallsService extends IntentService {
    /** This action is used to clear missed calls. */
    public static final String ACTION_CLEAR_HANGUP_BLACK_CALLS =
            "com.android.phone.intent.CLEAR_HANGUP_BLACK_CALLS";
    
    public static final String ACTION_CLEAR_ADD_BLACK =
            "com.android.phone.intent.CLEAR_ADD_TO_BLACK";
    
    public static final String ACTION_CLEAR_MODE =
            "com.android.phone.intent.CLEAR_MODE";
    
    public static final int SMS_NOTIFY  = 0;
    public static final int CALL_NOTIFY  = 1;
    
    private InterceptionApplication mApp;

    public ClearBlackCallsService() {
        super(ClearBlackCallsService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mApp = InterceptionApplication.getInstance();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ACTION_CLEAR_HANGUP_BLACK_CALLS.equals(intent.getAction())) {
        	int mode =  intent.getIntExtra(ACTION_CLEAR_MODE, -1);
            mApp.mManageReject.notificationMgr.cancelHangupBlackCallNotification(mode);
        } else if (ACTION_CLEAR_ADD_BLACK.equals(intent.getAction())) {
            int id = intent.getIntExtra("id", -1);
            String number = intent.getStringExtra("number");
            mApp.mManageReject.notificationMgr.cancelAddBlackNotification(id, number);
        }
    }
}
