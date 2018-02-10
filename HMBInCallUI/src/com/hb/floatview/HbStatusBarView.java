package com.hb.floatview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.incallui.Call;
import com.android.incallui.CallList;
import com.android.incallui.CallTimer;
import com.android.incallui.ContactInfoCache;
import com.android.incallui.InCallApp;
import com.android.incallui.InCallPresenter;
import com.android.incallui.ContactInfoCache.ContactCacheEntry;
import com.android.incallui.ContactInfoCache.ContactInfoCacheCallback;
import com.android.incallui.InCallPresenter.InCallState;
import com.android.incallui.InCallPresenter.InCallStateListener;
import com.android.incallui.R;
import com.hb.utils.CallStateUtil;
import com.hb.utils.SubUtils;

public class HbStatusBarView extends LinearLayout implements
		View.OnClickListener, InCallStateListener {

	private static final String LOG_TAG = "HbStatusBarView";

	private static final boolean DBG = true;
	
	/**
	 * 记录小悬浮窗的宽度
	 */
	public static int viewWidth;

	/**
	 * 记录小悬浮窗的高度
	 */
	public static int viewHeight;

	private TextView mName, mCallStateOrTime;
	
    private CallTimer mCallTimer;
    
    private CallList mCallList;
    
	private Context mContext;
	
    private ImageView mSimIcon;
    

	public HbStatusBarView(Context context) {
		super(context);
		mContext = context;
		LayoutInflater.from(context).inflate(R.layout.statusbar_layout, this);
		View view = findViewById(R.id.parent_container);
		view.setOnClickListener(this);
		viewWidth = view.getLayoutParams().width;
		viewHeight = view.getLayoutParams().height;
		mName = (TextView) findViewById(R.id.name);
		mCallStateOrTime = (TextView) findViewById(R.id.time);
        mSimIcon = (ImageView) findViewById(R.id.sim_icon);
		this.setOnKeyListener(null);
		mCallList = CallList.getInstance();
        mCallTimer = new CallTimer(new Runnable() {
            @Override
            public void run() {
                updateCallTime();
            }
        });

        mBackgroundAnimator = ObjectAnimator.ofInt(view, "backgroundColor", 0xff339d60,
                0xff06c012, 0xff339d60).setDuration(2000);
        mBackgroundAnimator.setEvaluator(new ArgbEvaluator());
        mBackgroundAnimator.setRepeatCount(ValueAnimator.INFINITE);
	}
	
	private ObjectAnimator mBackgroundAnimator; 

	public void onClick(View view) {
		int id = view.getId();
		log("onClick View ");

		switch (id) {
		case R.id.parent_container:
			InCallApp.getInstance().displayCallScreen();
			break;
		}
		FloatWindowManager.removeStatusBarWindow(InCallApp.getInstance());
	}
	
    private void updateCallTime() {
    	Call call = mCallList.getActiveCall();
       if (call == null) {
            mCallTimer.cancel();
    	    mCallStateOrTime.setVisibility(View.GONE);
        } else {
        	CharSequence state = CallStateUtil.getCallState();
        	if(!TextUtils.isEmpty(state)) {
                mCallStateOrTime.setText(state);
        	} else {
                final long callStart = call.getConnectTimeMillis();
                final long duration = System.currentTimeMillis() - callStart;
//                final long duration = call.getDuration();
                String callTimeElapsed = DateUtils.formatElapsedTime(duration / 1000);
                mCallStateOrTime.setText(callTimeElapsed);
        	}
            mCallStateOrTime.setVisibility(View.VISIBLE);
        }
    }
    
    @Override  
    protected void onAttachedToWindow() {  
        super.onAttachedToWindow();  
        InCallPresenter.getInstance().addListener(this);
        startCallTimerOrNot();
        queryInfo();
        updataSimIcon();
        mBackgroundAnimator.start();
    }
    
    @Override  
    protected void onDetachedFromWindow() {  
        super.onDetachedFromWindow();  
        InCallPresenter.getInstance().removeListener(this);
        mCallTimer.cancel();
        mBackgroundAnimator.cancel();
    }

	private void log(String msg) {
		Log.d(LOG_TAG, msg);
	}

	@Override
	public void onStateChange(InCallState oldState, InCallState newState,
			CallList callList) {
		// TODO Auto-generated method stub
	      // Start/stop timers.
		startCallTimerOrNot();		
	}
	
	private void startCallTimerOrNot() {
    	Call call = mCallList.getActiveCall();
        if (call != null) {
            mCallTimer.start(1000);
        } else {
            mCallTimer.cancel();
    	    mCallStateOrTime.setVisibility(View.GONE);
        }
	}
	
	private void queryInfo() {
		Call call = CallList.getInstance().getFirstCall();
		final ContactInfoCache cache = ContactInfoCache.getInstance(mContext);
		cache.findInfo(call, true, new ContactLookupCallback());
	}
	
	private ContactCacheEntry mContactCacheEntry;
	private class ContactLookupCallback implements ContactInfoCacheCallback {

		public ContactLookupCallback() {
		}

		@Override
		public void onContactInfoComplete(String callId, ContactCacheEntry entry) {
			mContactCacheEntry = entry;
			updateUiInternal();
		}

		@Override
		public void onImageLoadComplete(String callId, ContactCacheEntry entry) {
			mContactCacheEntry = entry;
			updateUiInternal();
		}

	}
	
	private void updateUiInternal() {
		if (mContactCacheEntry != null) {
			if (!TextUtils.isEmpty(mContactCacheEntry.name)) {
				mName.setText(mContactCacheEntry.name);
			} else {
				mName.setText(mContactCacheEntry.number);
			}
		}
	}

	private void updataSimIcon(){
    	Call call = mCallList.getActiveCall();
        if (call != null) {
    		int slot = SubUtils.getSlotBySubId(call.getSubId());
        	SubUtils.setSimSubId(mSimIcon, slot);
        }
	}
}