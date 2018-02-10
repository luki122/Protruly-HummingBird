/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.mediatek.keyguard.PowerOffAlarm;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.Button;
//import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.FrameLayout;

import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardSecurityCallback;
import com.android.keyguard.KeyguardSecurityView;

import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.android.keyguard.R;

import android.os.UserHandle;

/**
 * M: The view for power-off alarm boot.
 */
public class PowerOffAlarmViewEx extends RelativeLayout implements
        KeyguardSecurityView/*, View.OnTouchListener*/ {
    private static final String TAG = "PowerOffAlarmViewEx";
    private static final boolean DEBUG = false;

	//private PowerOffAlarmViewEx mPowerOffAlarmViewEx = null;
    private TextView mDateView = null;
    private TextView mTitleView = null;
    private Button mBtnSnoozeView = null;
    private Button mBtnPwrOnView = null;

    private LockPatternUtils mLockPatternUtils;
    private KeyguardSecurityCallback mCallback;
    private Context mContext;

	private final Date mCurrentTime = new Date();
	private SimpleDateFormat mDateFormat;
	private SimpleDateFormat mDateWeekFormat;
	private String mLastText = null;
	private String mDatePattern = null;
	private String mDatePattern2 = null;

    private boolean mIsDocked = false;
    private static final int UPDATE_LABEL = 99;
    
    /**hb tangjun add for disable and enable qspanelforpullup begin*/
    public static final String DISABLE_PULLUP_QSPANEL = "disable_pullup_qspanel";
    public static final String ENABLE_PULLUP_QSPANEL = "enable_pullup_qspanel";
    /**hb tangjun add for disable and enable qspanelforpullup end*/

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_LABEL:
                    if (mTitleView != null) {
                        mTitleView.setText(msg.getData().getString("label"));
                    }
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * Constructor.
     * @param context context
     */
    public PowerOffAlarmViewEx(Context context) {
        this(context, null);
    }

    /**
     * Constructor.
     * @param context context
     * @param attrs attributes
     */
    public PowerOffAlarmViewEx(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
		mDatePattern = context.getString(R.string.power_off_alarm_date_pattern);
		mDatePattern2 = context.getString(R.string.power_off_alarm_date_pattern2);
    }

    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        mCallback = callback;
		if (mCallback != null) {
			Log.w(TAG, " lq_pwroffa setKeyguardCallback ... setPwroffAlarmBg true!!");
			mCallback.setPwroffAlarmBg(true);
		}
    }

    /**
     * set lockpattern utils.
     * @param utils LockPatternUtils
     */
    public void setLockPatternUtils(LockPatternUtils utils) {
        mLockPatternUtils = utils;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Log.w(TAG, "onFinishInflate ... ");
        setKeepScreenOn(true);
		if (mCallback != null) {
			Log.w(TAG, " lq_pwroffa onFinishInflate ... setPwroffAlarmBg true!!");
			mCallback.setPwroffAlarmBg(true);
		}
		//mPowerOffAlarmViewEx = (PowerOffAlarmViewEx) findViewById(R.id.power_off_alarm_view_ex);
		mDateView = (TextView) findViewById(R.id.date_text);
        mTitleView = (TextView) findViewById(R.id.alertTitle);
        mBtnSnoozeView = (Button) findViewById(R.id.btn_alert_snooze);
        mBtnSnoozeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
				snooze();
            }
        });
        mBtnPwrOnView = (Button) findViewById(R.id.btn_alert_dismiss_pwron);
        mBtnPwrOnView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
				powerOn();
            }
        });
        setFocusableInTouchMode(true);
		//mPowerOffAlarmViewEx.setOnTouchListener(this);

        // Check the docking status , if the device is docked , do not limit rotation
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
        Intent dockStatus = mContext.registerReceiver(null, ifilter);
        if (dockStatus != null) {
            mIsDocked = dockStatus.getIntExtra(Intent.EXTRA_DOCK_STATE, -1)
                    != Intent.EXTRA_DOCK_STATE_UNDOCKED;
        }

        // Register to get the alarm killed/snooze/dismiss intent.
        IntentFilter filter = new IntentFilter(Alarms.ALARM_KILLED);
        filter.addAction(Alarms.ALARM_SNOOZE_ACTION);
        filter.addAction(Alarms.ALARM_DISMISS_ACTION);
        filter.addAction(UPDATE_LABEL_ACTION);
        mContext.registerReceiver(mReceiver, filter);
		registerTimeReceiver(mContext);
		updateClock();

        mLockPatternUtils = mLockPatternUtils == null ? new LockPatternUtils(
                mContext) : mLockPatternUtils;
        enableEventDispatching(true);
    }

    // Attempt to snooze this alert.
    private void snooze() {
        Log.d(TAG, "snooze selected");
        sendBR(SNOOZE);
    }

    // power on the device
    private void powerOn() {
        enableEventDispatching(false);
        Log.d(TAG, "powerOn selected");
        sendBR(DISMISS_AND_POWERON);
        sendBR(NORMAL_BOOT_ACTION);
    }

    // power off the device
    private void powerOff() {
        Log.d(TAG, "powerOff selected");
        sendBR(DISMISS_AND_POWEROFF);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        //boolean result = super.onTouchEvent(ev);
        //TODO: if we need to add some logic here ?
        int action = ev.getAction();
        int x = (int) ev.getRawX();
        int y = (int) ev.getRawY();
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            slidePointForDown(ev, y);
            break;
        case MotionEvent.ACTION_MOVE:
            slidePointForMove(ev, y);
            break;
        case MotionEvent.ACTION_UP:
            slidePointForUp(ev, y);
            break;
        }
        return true;
    }

    @Override
    public void showUsabilityHint() {
    }

    /** TODO: hook this up. */
    public void cleanUp() {
        if (DEBUG) {
            Log.v(TAG, "Cleanup() called on " + this);
        }
        mLockPatternUtils = null;
    }

    @Override
    public boolean needsInput() {
        return false;
    }

    @Override
    public void onPause() {
    	/**hb tangjun add for disable and enable qspanelforpullup begin*/
    	Log.v(TAG, "onPause");
    	Intent intent = new Intent();
    	intent.setAction(ENABLE_PULLUP_QSPANEL);
    	getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
    	/**hb tangjun add for disable and enable qspanelforpullup end*/
    }

    @Override
    public void onResume(int reason) {
        reset();
        Log.v(TAG, "onResume");
        /**hb tangjun add for disable and enable qspanelforpullup begin*/
    	Intent intent = new Intent();
    	intent.setAction(DISABLE_PULLUP_QSPANEL);
    	getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
        /**hb tangjun add for disable and enable qspanelforpullup end*/
    }

    @Override
    public KeyguardSecurityCallback getCallback() {
        return mCallback;
    }

    @Override
    public void onDetachedFromWindow() {
        Log.v(TAG, "onDetachedFromWindow ....");
        mContext.unregisterReceiver(mReceiver);
		unregisterTimeReceiver(mContext);
		if (mCallback != null) {
			Log.w(TAG, " lq_pwroffa onDetachedFromWindow ... setPwroffAlarmBg false!!");
			mCallback.setPwroffAlarmBg(false);
			mCallback = null;
		}
    }

    @Override
    public void showPromptReason(int reason) {
    }


    private void enableEventDispatching(boolean flag) {
        try {
            final IWindowManager wm = IWindowManager.Stub
                    .asInterface(ServiceManager
                            .getService(Context.WINDOW_SERVICE));
            if (wm != null) {
                wm.setEventDispatching(flag);
            }
        } catch (RemoteException e) {
            Log.w(TAG, e.toString());
        }
    }

    private void sendBR(String action) {
        Log.w(TAG, "send BR: " + action);
        mContext.sendBroadcast(new Intent(action));
    }

    // Receives the ALARM_KILLED action from the AlarmKlaxon,
    // and also ALARM_SNOOZE_ACTION / ALARM_DISMISS_ACTION from other
    // applications
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
       @Override
       public void onReceive(Context context, Intent intent) {
          String action = intent.getAction();
          Log.v(TAG, "receive action : " + action);
          if (UPDATE_LABEL_ACTION.equals(action)) {
              Message msg = new Message();
              msg.what = UPDATE_LABEL;
              Bundle data = new Bundle();
              data.putString("label", intent.getStringExtra("label"));
              msg.setData(data);
              mHandler.sendMessage(msg);
          } else if (PowerOffAlarmManager.isAlarmBoot()) {
              snooze();
          }
       }
    };

    @Override
    public void reset() {
        // TODO Auto-generated method stub
    }

    @Override
    public void startAppearAnimation() {
        // noop.
    }

    @Override
    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return false;
    }

    private static final String SNOOZE = "com.android.deskclock.SNOOZE_ALARM";
    private static final String DISMISS_AND_POWEROFF = "com.android.deskclock.DISMISS_ALARM";
    private static final String DISMISS_AND_POWERON = "com.android.deskclock.POWER_ON_ALARM";
    private static final String UPDATE_LABEL_ACTION = "update.power.off.alarm.label";
    private static final String NORMAL_BOOT_ACTION = "android.intent.action.normal.boot";
    private static final String NORMAL_BOOT_DONE_ACTION = "android.intent.action.normal.boot.done";
    private static final String DISABLE_POWER_KEY_ACTION =
        "android.intent.action.DISABLE_POWER_KEY";

    ///M: volume key does nothing when Power Off Alarm Boot
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (PowerOffAlarmManager.isAlarmBoot()) {
            switch(keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    Log.d(TAG, "onKeyDown() - KeyEvent.KEYCODE_VOLUME_UP, do nothing.") ;
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    Log.d(TAG, "onKeyDown() - KeyEvent.KEYCODE_VOLUME_DOWN, do nothing.") ;
                    return true ;
                default:
                    break;
            }
        }
     return super.onKeyDown(keyCode, event);
    }

    ///M: volume key does nothing when Power Off Alarm Boot
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (PowerOffAlarmManager.isAlarmBoot()) {
            switch(keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    Log.d(TAG, "onKeyUp() - KeyEvent.KEYCODE_VOLUME_UP, do nothing.") ;
					snooze();
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    Log.d(TAG, "onKeyUp() - KeyEvent.KEYCODE_VOLUME_DOWN, do nothing.") ;
					snooze();
                    return true ;
                default:
                    break;
            }
        }
     return super.onKeyUp(keyCode, event);
    }

	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (Intent.ACTION_TIME_TICK.equals(action)
					|| Intent.ACTION_TIME_CHANGED.equals(action)
					|| Intent.ACTION_TIMEZONE_CHANGED.equals(action)
					|| Intent.ACTION_LOCALE_CHANGED.equals(action)) {
				if (Intent.ACTION_LOCALE_CHANGED.equals(action)
						|| Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
					// need to get a fresh date format
					mDateFormat = null;
					mDateWeekFormat = null;
				}
				updateClock();
			}
		}
	};

	private void registerTimeReceiver(Context context) {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_TIME_TICK);
		filter.addAction(Intent.ACTION_TIME_CHANGED);
		filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		filter.addAction(Intent.ACTION_LOCALE_CHANGED);
		context.registerReceiver(mIntentReceiver, filter);
	}

	private void unregisterTimeReceiver(Context context) {
        mDateFormat = null; // reload the locale next time
		mDateWeekFormat = null;
		context.unregisterReceiver(mIntentReceiver);
	}

    private void updateClock() {
		if (mDateFormat == null) {
			final Locale l = Locale.getDefault();
			final String fmt = DateFormat.getBestDateTimePattern(l, mDatePattern);
			mDateFormat = new SimpleDateFormat(fmt, l);
		}

		if (mDateWeekFormat == null) {
			final Locale l = Locale.getDefault();
			final String fmt = DateFormat.getBestDateTimePattern(l, mDatePattern2);
			mDateWeekFormat = new SimpleDateFormat(fmt, l);
		}

		mCurrentTime.setTime(System.currentTimeMillis());

		final String text = mDateFormat.format(mCurrentTime) + " " + mDateWeekFormat.format(mCurrentTime);
		if (!text.equals(mLastText)) {
			mDateView.setText(text);
			mLastText = text;
		}
	}


	private final int SLIDE_AREA_Y = 1300;
	private final int SLIDE_MAX_Y = 400;
	private boolean mSupportSlideFlag = false;
	private int mSlideStartY = 0;
	private int mSlideMoveY = 0;
	private int mLastY;
	private VelocityTracker mVelocityTracker = null;
	private static final int SNAP_VELOCITY = 500;
	private void slidePointForDown(MotionEvent event, int y) {
		mSlideStartY = y;
		mSlideMoveY = y;
		mLastY = y;
		if (y > SLIDE_AREA_Y) {
			mSupportSlideFlag = true;
			if (mVelocityTracker == null) {
				mVelocityTracker = VelocityTracker.obtain();
			}
			mVelocityTracker.addMovement(event);
		} else {
			mSupportSlideFlag = false;
		}
		Log.d("lq_touch", "-------> [Down] y = " + y + "; mSupportSlideFlag = " + mSupportSlideFlag);
	}

	private void slidePointForMove(MotionEvent event, int y)
	{
		if (mSupportSlideFlag) {
			mSlideMoveY = y;
			int deltaY = mSlideMoveY - mSlideStartY;
            if (deltaY > 0 && getTranslationY() <= 0) {
                setTranslationY(0);
            } else {
                int disY = (int)getTranslationY() + mSlideMoveY - mLastY;
                setTranslationY(disY);
            }
            mLastY = mSlideMoveY;
			if (mVelocityTracker != null) {
				mVelocityTracker.addMovement(event);
			}
		}
	}

	private void slidePointForUp(MotionEvent event, int y)
	{
		Log.d("lq_touch", "-------> [Up] y = " + y + "; mSupportSlideFlag = " + mSupportSlideFlag);
		if (mSupportSlideFlag) {
			int velocityY = 0;
			if (mVelocityTracker != null) {
				mVelocityTracker.addMovement(event);
				mVelocityTracker.computeCurrentVelocity(1000);
				velocityY = (int) mVelocityTracker.getYVelocity();
				releaseVelocityTracker();
			}
			int y_diff = mSlideStartY - mSlideMoveY;
			Log.d("lq_touch", "-------> [Up] y_diff = " + y_diff + "; velocityY = " + velocityY);
			if ((velocityY > SNAP_VELOCITY && y_diff > 20) || (mSlideStartY - mSlideMoveY > SLIDE_MAX_Y)) {
				mSlideStartY = mSlideMoveY = 0;
				Log.d("lq_touch", "-------> [Up] pwr off!!");
				//powerOff();
				moveMoveView(-getHeight(), true);
			} else {
				moveMoveView(0, false);
			}
			mSupportSlideFlag = false;
		}
	}
	
    private void moveMoveView(float to, boolean exit){
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "translationY", to);
        ObjectAnimator animatorAlpha = ObjectAnimator.ofFloat(this, "Alpha", to);
        animator.setDuration(250).start();
        if (exit) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                	powerOff();
                }
            });
        }
    }
    
    private void releaseVelocityTracker() { 
        if(null != mVelocityTracker) {
        	mVelocityTracker.clear(); 
        	mVelocityTracker.recycle(); 
        	mVelocityTracker = null; 
        } 
    }
}
