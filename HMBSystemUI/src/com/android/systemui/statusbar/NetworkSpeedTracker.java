package com.android.systemui.statusbar;

import android.R.integer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.Log;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;

import com.android.systemui.statusbar.phone.PhoneStatusBar;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.provider.Settings;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.TrafficStats;

public class NetworkSpeedTracker {
    
    private static final String TAG = "NetworkSpeedTracker";
    //private static final String SHOW_NETWORK_SPEED_SETTING = "realtime_speed_status";
    public static final String NOTIFICATION_SHOW_NETWORK_SPEED = "notification_show_network_speed";
    private static final boolean DEBUG = true;
    
    private HandlerThread mHandlerThread;
    private NetworkSpeedMonitoringHandler mMyHandler;
    private long mPrevReceived = 0;
    private double mNetSpeed = 0.00d;
    private String mActualSpeed = null;
    private boolean mIsCountNetSpeed = false;
    private Context mContext;
    private boolean mMobileDataType;
    private boolean mWifiType;
    private long mLastTime;

    
    private static final int MSG_CONNECTIVITY_CHANGED = 0;
    private static final int MSG_SCREEN_ON = 1;
    private static final int MSG_SCREEN_OFF = 2;
    
    private static final int MSG_UPDATE_UI = 3;
    
    private ArrayList<NetworkSpeedUpdateListener> mUpdateListeners = new ArrayList<NetworkSpeedUpdateListener>();
    
    public NetworkSpeedTracker(Context context) {
        mContext = context;
        mHandlerThread = new HandlerThread("network_speed_handler_thread");
        mHandlerThread.start();
        mMyHandler = new NetworkSpeedMonitoringHandler(mHandlerThread.getLooper());
        mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(NOTIFICATION_SHOW_NETWORK_SPEED), false, mNetworkSpeedObserver);
        mIsCountNetSpeed = 0 != Settings.Secure.getInt(mContext.getContentResolver(), NOTIFICATION_SHOW_NETWORK_SPEED, 0);
        mPrevReceived = 0;
        openOrColseNetworkSpeedTracker();
        setBroadcastListening();
    }
    
    public void addUpdateListener(NetworkSpeedUpdateListener listener) {
        if(mUpdateListeners.contains(listener)) return;
        mUpdateListeners.add(listener);
    }

    public void setBroadcastListening() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mIntentReceiver, filter);
    }
    
    public void removeBroadcastListening() {
        mContext.unregisterReceiver(mIntentReceiver);
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CONNECTIVITY_CHANGED: 
                    mMobileDataType = getMobileConnectState();
                    mWifiType = getWifiConnectState();
                    openOrColseNetworkSpeedTracker();
                    break;
                case MSG_SCREEN_ON:
                    mMobileDataType = getMobileConnectState();
                    mWifiType = getWifiConnectState();
                    if (!(mMobileDataType || mWifiType)) {
                        return;
                    }
                    openOrColseNetworkSpeedTracker();
                    break;
                case MSG_SCREEN_OFF:
                    mMyHandler.removeCallbacks(mHandlerThreadRunnable);
                    break;
            }
        };
    };
    
    private Runnable mHandlerThreadRunnable = new Runnable() {
        @Override
        public void run() {
            mActualSpeed = calculateNetworkSpeed();
            mMyHandler.obtainMessage(MSG_UPDATE_UI).sendToTarget();
            mMyHandler.removeCallbacks(mHandlerThreadRunnable);
            mMyHandler.postDelayed(mHandlerThreadRunnable, 1000);
        }
    };

    private Runnable UpdateUI = new Runnable() {
        public void run() {
            if (mActualSpeed == null || mActualSpeed.equals("")) {
                mActualSpeed = "0.00K/s";
            }
            if (DEBUG) {
                Log.d(TAG, "UpdateUI mActualSpeed = " + mActualSpeed);        
            }
            for(NetworkSpeedUpdateListener listener : mUpdateListeners) {
                listener.onNetworkSpeedUpdated(mActualSpeed);
            }
        }
    };

    private ContentObserver mNetworkSpeedObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            mIsCountNetSpeed = 0 != Settings.Secure.getInt(mContext.getContentResolver(), NOTIFICATION_SHOW_NETWORK_SPEED, 0);
            Log.d(TAG, "onChange mIsCountNetSpeed = " + mIsCountNetSpeed);
           openOrColseNetworkSpeedTracker();
        }
    };
    
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mHandler.removeMessages(MSG_SCREEN_OFF);
                mHandler.sendEmptyMessage(MSG_SCREEN_OFF);
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                mHandler.removeMessages(MSG_SCREEN_ON);
                mHandler.sendEmptyMessageDelayed(MSG_SCREEN_ON, 300);
            } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                mHandler.removeMessages(MSG_CONNECTIVITY_CHANGED);
                mHandler.sendEmptyMessageDelayed(MSG_CONNECTIVITY_CHANGED, 100);
            }
        }
    };
    
    private class NetworkSpeedMonitoringHandler extends Handler {
       public NetworkSpeedMonitoringHandler(Looper looper) {
           super(looper);
       }
          
       @Override
       public void handleMessage(Message msg) {
           if(msg.what == MSG_UPDATE_UI) {
               mHandler.post(UpdateUI);
           }
       }
    }
    
    /*
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mIsOnAttachedToWindow = true;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mIntentReceiver, filter);
        openAndColseNetworkSpeedTracker();
    }
    */

    /*
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mIsOnAttachedToWindow) {
            mMyHandler.removeCallbacks(mHandlerThreadRunnable);
            if (mMyHandler != null) {
                mMyHandler.removeCallbacksAndMessages(null);
                mMyHandler = null;
            }
            if (mHandler != null) {
                mHandler.removeCallbacksAndMessages(null);
                mHandler = null;
            }
            mContext.getContentResolver().unregisterContentObserver(mNetworkSpeedObserver);
            mIsOnAttachedToWindow = false;    
        }
    }
    */
    
    /*
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mNetSpeedView.measure(0, 0);
        final int fatherWidth = MeasureSpec.getSize(heightMeasureSpec);
        final int fatherHeight = MeasureSpec.getSize(heightMeasureSpec);
        final int width = mNetSpeedView.getMeasuredWidth();
        final int height = mNetSpeedView.getMeasuredHeight();
        int widthTemp = 0;
        if (mBackground.isShown()) {
            widthTemp = width + 16;
        } else {
            widthTemp = width;
        }
        setMeasuredDimension(widthTemp, fatherHeight);
    }
    */
    
    /*
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // super.onLayout(changed, l, t, r, b);
        final int width = mNetSpeedView.getMeasuredWidth();
        final int height = mNetSpeedView.getMeasuredHeight();
        mBackground.layout(l, ((b - t) - height)/2, l + (r - l), height + ((b - t) - height)/2);
        mNetSpeedView.layout(l, ((b - t) - height)/2, l + (r - l), height + ((b - t) - height)/2);
    }
    */
    
    /*
    private boolean isWifiOnlyDevice() {
        ConnectivityManager cm = ( ConnectivityManager ) mContext.getSystemService(
                mContext.CONNECTIVITY_SERVICE);
        return !(cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE));
    }
    */
    
    private long getTotalReceivedBytes() {
        String line;
        String[] segs;
        long received = 0;
        int i;
        long tmp = 0;
        boolean isNum;
        FileReader fr = null;
        BufferedReader in = null;
        try {
            fr = new FileReader("/proc/net/dev");
            in = new BufferedReader(fr, 500);
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("rmnet") || line.startsWith("eth")
                        || line.startsWith("wlan") || line.startsWith("netts")
                        || line.startsWith("ccmni") || line.startsWith("r_rmnet")
                        || line.startsWith("dummy") || line.startsWith("sit0")
                        || line.startsWith("lo")) {
                    segs = line.split(":")[1].split(" ");
                    for (i = 0; i < segs.length; i++) {
                        isNum = true;
                        try {
                            // tmp = Integer.parseInt(segs[i]);
                            tmp = Long.parseLong(segs[i]);
                        } catch (Exception e) {
                            isNum = false;
                        }
                        if (isNum == true) {
                            received = received + tmp;
                            break;
                        }
                    }
                }
            }
            if (in != null) {
                in.close();
                in = null;
            }
            if (fr != null) {
                fr.close();
                fr = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "getTotalReceivedBytes IOException : " + e.getMessage());
            return -1;
        } finally{
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                in = null;
            }
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                fr = null;
            }
        }
        return received;
    }
    
    private static String formatSpeed(double paramLong){
        double f1 = (double)paramLong;
        String str = "K/s";
        double f2 = f1 / 1024.0F;
        if (f2 > 999.0F){
          str = "M/s";
          f2 /= 1024.0F;
        }
        if (f2 < 10.0F){
            Object[] arrayOfObject3 = new Object[2];
            arrayOfObject3[0] = Double.valueOf(f2);
            arrayOfObject3[1] = str;
            return String.format("%.2f%s", arrayOfObject3);
        }
        if (f2 < 100.0F){
            Object[] arrayOfObject2 = new Object[2];
            arrayOfObject2[0] = Double.valueOf(f2);
            arrayOfObject2[1] = str;
            return String.format("%.1f%s", arrayOfObject2);
        }
        Object[] arrayOfObject1 = new Object[2];
        arrayOfObject1[0] = Double.valueOf(f2);
        arrayOfObject1[1] = str;
        return String.format("%.0f%s", arrayOfObject1);
    }
    
    public String calculateNetworkSpeed() {
        //long receivedBytes = getTotalReceivedBytes();
        long receivedBytes = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes();
        long receivedPackages = TrafficStats.getTotalRxPackets() + TrafficStats.getTotalTxPackets();
        if (receivedBytes >= Long.MAX_VALUE) {
            receivedBytes = 0;
        }
        long currentTime = System.currentTimeMillis();
        long received = receivedBytes - receivedPackages * 32;
        if (received <= 0L) {
            received = 0L;
            mLastTime = 0L;
            mPrevReceived = 0L;
        }
        if (DEBUG) {
            Log.d(TAG, "CalculateNetworkSpeed receivedBytes = " + receivedBytes + " receivedPackages = " + receivedPackages + " received = " + received);
        }
        if (currentTime > mLastTime) {
            mNetSpeed = 1000L * (received - mPrevReceived) / (currentTime - mLastTime);
            mLastTime = currentTime;
        } else {
        	mNetSpeed = 0.00d;
        }
        if (mNetSpeed < 0) {
            mNetSpeed = 0.00d;
        }
        /*
        if (mMobileDataType) {
            if (mNetSpeed >= 500 * 1024 && mNetSpeed < 1024 * 1024) {
                mSpeedLevel = 1;
            } else if (mNetSpeed >= 1024 * 1024) {
                mSpeedLevel = 2;
            } else {
                mSpeedLevel = -1;
            }
        } else {
            mSpeedLevel = -1;
        }
        */
        mPrevReceived = received;
        return formatSpeed(mNetSpeed);
    }
    
    private boolean getMobileConnectState() {
        ConnectivityManager manager = null;
        State mobile = State.UNKNOWN;
        try {
            manager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            mobile = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
            Log.d(TAG, "getMobileConnectState state = " + mobile);
        } catch (Exception e) {
            return false;
        }
        if (mobile == State.CONNECTED) {
            return true;
        } else {
            return false;
        }
    }
    
    private boolean getWifiConnectState() {
        boolean isConnected = false;
        try {
            ConnectivityManager manager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = (NetworkInfo) manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            isConnected = info.isConnected();
            Log.d(TAG, "getWifiConnectState state = " + isConnected);
        } catch (Exception e) {
            // TODO: handle exception
        }
        return isConnected;
    }
    
    private String getNetworkSpeed(String speed) {
        if (!speed.contains(".")) {
            if (speed.length() == 1) {
                return speed + ".00";
            } else if (speed.length() == 2) {
                return speed + ".0";
            }
            return speed;
        } else {
            String[] separationSpeed = speed.split("\\.");
            if (separationSpeed[0].length() == 1) {
                if (separationSpeed[1].length() < 2) {
                    return speed + "0";
                }
                return speed;
            } else if (separationSpeed[0].length() == 2) {
                return (new BigDecimal(speed).setScale(1,
                        BigDecimal.ROUND_HALF_UP)).toString();
            } else {
                return (new BigDecimal(speed).setScale(0,
                        BigDecimal.ROUND_HALF_UP)).toString();
            }
        }
    }
    
    private void openOrColseNetworkSpeedTracker() {
        if (mIsCountNetSpeed) {
            mMyHandler.removeCallbacks(mHandlerThreadRunnable);
            mMyHandler.post(mHandlerThreadRunnable);
            for(NetworkSpeedUpdateListener listener : mUpdateListeners) {
                listener.onNetworkSpeedOpened();
            }
        } else {
            mMyHandler.removeCallbacks(mHandlerThreadRunnable);
            for(NetworkSpeedUpdateListener listener : mUpdateListeners) {
                listener.onNetworkSpeedClosed();
            }
        }
    }
   
    public interface NetworkSpeedUpdateListener {
        public void onNetworkSpeedUpdated(String speedText);
        public void onNetworkSpeedClosed();
        public void onNetworkSpeedOpened();
    }
}
