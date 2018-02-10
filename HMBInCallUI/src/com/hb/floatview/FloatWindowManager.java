package com.hb.floatview;


import com.android.incallui.Call;
import com.android.incallui.CallList;
import com.android.incallui.InCallApp;

import android.os.Handler;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.*;
import android.content.Context;
import android.content.Intent;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;
import android.graphics.*;
import com.android.incallui.R;

public class FloatWindowManager {
	private final static String TAG = "FloatWindowManager";

	private static HbFloatView mRingingWindow;
	
	private static HbStatusBarView mStatusBarWindow;

	private static LayoutParams WindowParams, mStatusWindowParams;

	private static WindowManager mWindowManager;
		
	public static boolean  sIsShowAfterAnswer = false;	
	public static boolean  sIsAnsweringCallWaiting = false;	
	
	private final static int sWindowType = LayoutParams.TYPE_KEYGUARD_DIALOG;
	
	/**
	 * 创建一个大悬浮窗。位置为屏幕正中间。
	 * 
	 * @param context
	 *            必须为应用程序的Context.
	 */
	public static void createWindow(Context context) {
		WindowManager windowManager = getWindowManager(context);
		int screenWidth = windowManager.getDefaultDisplay().getWidth();
		int screenHeight = windowManager.getDefaultDisplay().getHeight();
		if (mRingingWindow == null) {
			mRingingWindow = new HbFloatView(context);
			if (WindowParams == null) {
//				int marginTop = context.getResources().getDimensionPixelOffset(R.dimen.float_margin_top);  
				WindowParams = new LayoutParams();
				WindowParams.x = 0;
				WindowParams.y = 0;
				WindowParams.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE 
						|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
						|WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
						|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
//				WindowParams.type = LayoutParams.TYPE_TOP_MOST;
				WindowParams.type = sWindowType;
//				WindowParams.type = 2033;
				WindowParams.format = PixelFormat.RGBA_8888;
				WindowParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
				WindowParams.width = HbFloatView.viewWidth;
				WindowParams.height = HbFloatView.viewHeight;
				WindowParams.windowAnimations=R.style.incoming_pop_anim_style;
			}
	        Log.i(TAG, "createWindow addView ");
			windowManager.addView(mRingingWindow, WindowParams);	
			updateRingingViewYPos();
		} else {
			updateUI();
		}
	}

	/**
	 * 将大悬浮窗从屏幕上移除。
	 * 
	 * @param context
	 *            必须为应用程序的Context.
	 */
	
	public static void removeWindow(Context context) {
		if (mRingingWindow != null) {
		       Log.i(TAG, "removeWindow ");
			final WindowManager windowManager = getWindowManager(context);
			windowManager.removeView(mRingingWindow);
			mRingingWindow = null;
		}
	}
	
	public static void createStatusBarWindow(Context context) {
		WindowManager windowManager = getWindowManager(context);
		if (mStatusBarWindow == null) {
			mStatusBarWindow = new HbStatusBarView(context);
			if (mStatusWindowParams == null) {
				mStatusWindowParams = new LayoutParams();
				mStatusWindowParams.x = 0;
				mStatusWindowParams.y = 0;
				mStatusWindowParams.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE 
						|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
						|WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
				mStatusWindowParams.type = sWindowType;
				mStatusWindowParams.format = PixelFormat.RGBA_8888;
				mStatusWindowParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
				mStatusWindowParams.width = HbStatusBarView.viewWidth;
				mStatusWindowParams.height = HbStatusBarView.viewHeight;
			}
	        Log.i(TAG, "createStatusBarWindow addView ");
			windowManager.addView(mStatusBarWindow, mStatusWindowParams);
			updateRingingViewYPos();
		} 
		//显示
//		Intent intent = new Intent("hb.intent.action.show.callstatus");
//		intent.putExtra("show",1);
//		intent.putExtra("callstate", getCallState());
//	    intent.putExtra("showTime", isShowTime());
//	    intent.putExtra("time",getCallTime());  //当前显示时间精确到毫秒
//		context.sendBroadcast(intent);
//		isStatusShowing = true;
	}	
	
   public static void updateStatusBarWindow(Context context) {
        Intent intent = new Intent("hb.intent.action.show.callstatus");
        intent.putExtra("show",1);
        intent.putExtra("callstate", getCallState());
        intent.putExtra("showTime", isShowTime());
        intent.putExtra("time",getCallTime()); 
        context.sendBroadcast(intent);
    }   
	
	private static String getCallState() {
        CallList mCallList = CallList.getInstance();
        Call call = mCallList.getFirstCall();
        if(call != null) {
            int resId;
            switch  (call.getState()) {
                case Call.State.IDLE:
                    resId = R.string.card_title_call_ended;
                    break;
                case Call.State.ACTIVE:
                    resId = R.string.card_title_in_call;
                    break;
                case Call.State.ONHOLD:
                    resId = R.string.card_title_on_hold;
                    break;
                case Call.State.CONNECTING:
                case Call.State.DIALING:
                    resId = R.string.card_title_dialing;    
                    break;
                case Call.State.REDIALING:
                    resId = R.string.card_title_redialing;    
                    break;
                case Call.State.INCOMING:
                case Call.State.CALL_WAITING:
                    resId = R.string.card_title_incoming_call;    
                    break;
                case Call.State.DISCONNECTING:
                    resId = R.string.card_title_hanging_up;  
                    break;
                case Call.State.DISCONNECTED:
                    resId = R.string.card_title_call_ended;             
                    break;
                case Call.State.CONFERENCED:
                    resId = R.string.card_title_conf_call;   
                    break;
                default:
                    resId = 0;                   
            }
            if(resId > 0) {
                return InCallApp.getInstance().getString(resId);
            }        
        } 
        return "";
	}
		
   private static boolean isShowTime() {
        CallList mCallList = CallList.getInstance();
        Call call = mCallList.getFirstCall();
        if(call != null) {
            return call.getState() == Call.State.ACTIVE;
        }
        return false;
    }	
	
	private static long getCallTime() {
	       CallList mCallList = CallList.getInstance();
	       Call call = mCallList.getActiveCall();
	       if (call == null) {
	            return -1;
	        } else {
	            final long callStart = call.getConnectTimeMillis();
	            final long duration = System.currentTimeMillis() - callStart;
	            return duration;
//	            return call.getDuration();
	        }
	}

	/**
	 * 将大悬浮窗从屏幕上移除。
	 * 
	 * @param context
	 *            必须为应用程序的Context.
	 */
	
	public static void removeStatusBarWindow(Context context) {
		if (mStatusBarWindow != null) {
		       Log.i(TAG, "removeStatusBarWindow ");
			final WindowManager windowManager = getWindowManager(context);
			windowManager.removeView(mStatusBarWindow);
			mStatusBarWindow = null;
			updateRingingViewYPos();
		}
	    //隐藏
//	    Intent intent = new Intent("hb.intent.action.show.callstatus");
//	    intent.putExtra("show",0);
//	    context.sendBroadcast(intent);
//	    isStatusShowing = false;
	}
	
	private static boolean isStatusShowing;
	   public static boolean isStatusWindowShowing() {
	        return isStatusShowing;
	    }
	
	
	private static Handler mHandler = new Handler();


	/**
	 * 是否有悬浮窗(包括小悬浮窗和大悬浮窗)显示在屏幕上。
	 * 
	 * @return 有悬浮窗显示在桌面上返回true，没有的话返回false。
	 */
	public static boolean isWindowShowing() {
		return mRingingWindow != null;
	}

	/**
	 * 如果WindowManager还未创建，则创建一个新的WindowManager返回。否则返回当前已创建的WindowManager。
	 * 
	 * @param context
	 *            必须为应用程序的Context.
	 * @return WindowManager的实例，用于控制在屏幕上添加或移除悬浮窗。
	 */
	private static WindowManager getWindowManager(Context context) {
		if (mWindowManager == null) {
			mWindowManager = (WindowManager) context
					.getSystemService(Context.WINDOW_SERVICE);
		}
		return mWindowManager;
	}
	
	public static void updateUI() {
		if(isWindowShowing()) {
			mRingingWindow.updateUI();
		}
	} 

	private static void updateRingingViewYPos() {
		if(mRingingWindow == null)  return;
		int statusbarHeight = InCallApp.getInstance().getResources().getDimensionPixelSize(
				R.dimen.hb_statusbar_height);
//		int y = mStatusBarWindow != null ? statusbarHeight : 0;
//		mRingingWindow.setTranslationY(y);
		mRingingWindow.updateMarginTop(mStatusBarWindow != null);
	}


}