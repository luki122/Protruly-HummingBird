package com.android.systemui.statusbar;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.android.systemui.SystemUIApplication;
import com.android.systemui.statusbar.NotificationData.Entry;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.phone.StatusBarNotificationBoxStatusListener;

import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
//import com.android.settings.notification.NotificationBackend;
import android.app.INotificationManager;
import android.os.ServiceManager;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

/**
 * This file is added by ShenQianfeng
 */

public class CustomStatusBarManager {
    
    private static final String TAG = "CustomStatusBarManager";
    
    private static INotificationManager mInm =
            INotificationManager.Stub.asInterface(ServiceManager.getService(Context.NOTIFICATION_SERVICE));
    
    private Context mContext;
    private Handler mHandler;
    
    private NotificationData mNotificationData;
    private StatusBarIconController mIconController;
    
    public static final String NOTIFICATION_FOLD_NON_PRIORITY_NOTIFICATION = "notification_fold_non_priority_notification";
    private Uri mUri = Settings.Secure.getUriFor(NOTIFICATION_FOLD_NON_PRIORITY_NOTIFICATION);
    public static final int DEFAULT_NOTIFICATION_FOLD_NON_PRIORITY_VALUE = 1;

    private boolean mNotificationBoxOn;
    private boolean mTestStatus = true; 
    
    private StatusBarNotificationBoxStatusListener mListener;
    
    public CustomStatusBarManager(Context context, Handler handler, StatusBarIconController iconController) {
        mContext = context;
        mHandler = handler;
        mIconController = iconController;
        initNotificationBoxStatus();
        
        //debugAndTest();
    }
    
    public void initNotificationBoxStatus() {
        //TODO: init ...
        mNotificationBoxOn = Settings.Secure.getInt(mContext.getContentResolver(),
                NOTIFICATION_FOLD_NON_PRIORITY_NOTIFICATION, 
                DEFAULT_NOTIFICATION_FOLD_NON_PRIORITY_VALUE) == 1;
        
        //Just for test
        //mNotificationBoxOn = false;
    }
    
    public void setStatusBarNotificationBoxStatusListener(StatusBarNotificationBoxStatusListener listener) {
        mListener = listener;
    }
    
    public boolean isNotificationBoxOn() {
        return mNotificationBoxOn;
    }

    public void setNotificationData(NotificationData data) {
        mNotificationData = data;
        mNotificationData.setCustomStatusBarManager(this);
    }
    
    public void setIconController(StatusBarIconController iconController) {
        mIconController = iconController;
    }
    
    /**
     * Just for test
     */
    /*
    public void debugAndTest() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                //boolean hasPriorityMaxNotifications = hasNotificationWithPriorityMax();
                mIconController.notifyStatusBarNotificationBoxStatusChanged(mTestStatus);
                mTestStatus = ! mTestStatus;
            }
        };
        Timer t = new Timer();
        t.schedule(task, 0, 6000);
    }
    */
    
    final private ContentObserver mStatusBarNotificationBoxObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            int status = Settings.Secure.getInt(mContext.getContentResolver(), 
                    NOTIFICATION_FOLD_NON_PRIORITY_NOTIFICATION, 
                    DEFAULT_NOTIFICATION_FOLD_NON_PRIORITY_VALUE);
            Log.i(TAG, "CustomStatusBarManager::onChange new status Value:" + status);
            mNotificationBoxOn = status == 1;
            //mIconController.notifyStatusBarNotificationBoxStatusChanged(mNotificationBoxOn);
            //mIconController.updateNotificationIcons(mNotificationData);
            if(mListener != null) {
                mListener.onStatusBarNotificationBoxStatusChange(mNotificationBoxOn);
            }
        }
    };
    
    public void registerObserver() {
        mContext.getContentResolver().registerContentObserver(mUri, true, mStatusBarNotificationBoxObserver);
    }
    
    public void unregisterObserver() {
        mContext.getContentResolver().unregisterContentObserver(mStatusBarNotificationBoxObserver);
    }

    public boolean hasNotificationWithPriorityMax() {
        ArrayList<Entry> entries = mNotificationData.getActiveNotifications();
        for(Entry entry : entries) {
            //Notification notification = entry.notification.getNotification();
            if(isNotificationPriorityMax(entry.notification)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasNotificationWithPriorityNormal() {
        ArrayList<Entry> entries = mNotificationData.getActiveNotifications();
        for(Entry entry : entries) {
            //Notification notification = entry.notification.getNotification();
            if( ! isNotificationPriorityMax(entry.notification)) {
                return true;
            }
        }
        return false;
    }
    
    public int getPackageUid(String pkg) {
        try {
            PackageManager pm = mContext.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA);
            return ai.uid;
        } catch(Exception e) {
            Log.e(TAG, "getPackageUid:" + e.getMessage());
        }
        return -1;
    }
    
    public boolean isNotificationPriorityMax(StatusBarNotification notification) {
        //int packagePriority = mBackend.getHighPriority(notification.pkg, notification.uid);
        try {
            String pkg = notification.getPackageName();
            int packageUid =  getPackageUid(pkg);
            Log.i(TAG, "CustomStatusBarManager::isNotificationPriorityMax  packageUid: " + packageUid + " pkg:" + pkg);
            int packagePriority = mInm.hbGetPriority(pkg, packageUid);
            if(packagePriority == Notification.PRIORITY_MAX) {
                return true;
            }
        } catch(Exception e) {
            Log.i(TAG, "isNotificationPriorityMax:" + e.getMessage());
        }
        return false;
    }
    
    /*
    public boolean isNotificationPriorityMax(Notification notification) {
        RuntimeException e = new RuntimeException();
        e.fillInStackTrace();
        Log.i(TAG, "isNotificationPriorityMax notification:" + notification, e);
  
        if(notification.priority >= Notification.PRIORITY_MAX) {
            Log.i(TAG, "----isNotificationPriorityMax true");
            return true;
        }
        //Notification notification = entry.notification.getNotification();
        
        Log.i(TAG, "----isNotificationPriorityMax false");
        return false;
    }
    */
    //ShenQianfeng add end
}
