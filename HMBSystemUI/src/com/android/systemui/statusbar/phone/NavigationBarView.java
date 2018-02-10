/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.statusbar.phone;

import android.animation.LayoutTransition;
import android.animation.LayoutTransition.TransitionListener;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.ActivityManagerNative;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewRootImpl;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.statusbar.StatusBarState;
import com.android.systemui.statusbar.policy.DeadZone;
import com.android.systemui.statusbar.policy.KeyButtonRipple;
import com.android.systemui.statusbar.policy.KeyButtonView;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.mediatek.common.MPlugin;
import com.mediatek.systemui.ext.DefaultNavigationBarPlugin;
import com.mediatek.systemui.ext.INavigationBarPlugin;

public class NavigationBarView extends LinearLayout {
    final static boolean DEBUG = false;
    final static String TAG = "PhoneStatusBar/NavigationBarView";

    // slippery nav bar when everything is disabled, e.g. during setup
    final static boolean SLIPPERY_WHEN_DISABLED = true;

    final Display mDisplay;
    View mCurrentView = null;
    /**hb tangjun mod begin*/
    //View[] mRotatedViews = new View[4];
    View[] mRotatedViews = new View[8];
    /**hb tangjun mod end*/

    int mBarSize;
    boolean mVertical;
    boolean mScreenOn;

    boolean mShowMenu;
    int mDisabledFlags = 0;
    int mNavigationIconHints = 0;

    private Drawable mBackIcon, mBackLandIcon, mBackAltIcon, mBackAltLandIcon;
    private Drawable mRecentIcon;
    private Drawable mRecentLandIcon;

    private NavigationBarViewTaskSwitchHelper mTaskSwitchHelper;
    private DeadZone mDeadZone;
    private final NavigationBarTransitions mBarTransitions;

    // workaround for LayoutTransitions leaving the nav buttons in a weird state (bug 5549288)
    final static boolean WORKAROUND_INVALID_LAYOUT = true;
    final static int MSG_CHECK_INVALID_LAYOUT = 8686;

    // performs manual animation in sync with layout transitions
    private final NavTransitionListener mTransitionListener = new NavTransitionListener();

    private OnVerticalChangedListener mOnVerticalChangedListener;
    private boolean mIsLayoutRtl;
    private boolean mLayoutTransitionsEnabled;

    // MPlugin for Navigation Bar
    private INavigationBarPlugin mNavBarPlugin;
    
    /**hb add by tangjun begin*/
    private View mDelegateView;
    private MotionEvent mDownEvent;
    private PhoneStatusBar mPhoneStatusBar;
    private boolean mToggle;
    // 虚拟键位置
    public static final String NAVIGATION_KEY_POSITION = "navigation_key_position";
    /*
    public static final String DISABLE_PULLUP_QSPANEL = "disable_pullup_qspanel";
    public static final String ENABLE_PULLUP_QSPANEL = "enable_pullup_qspanel";
    private boolean mDisablePullUpQSpanel;
    */
    /**hb add by tangjun end*/

    private class NavTransitionListener implements TransitionListener {
        private boolean mBackTransitioning;
        private boolean mHomeAppearing;
        private long mStartDelay;
        private long mDuration;
        private TimeInterpolator mInterpolator;

        @Override
        public void startTransition(LayoutTransition transition, ViewGroup container,
                View view, int transitionType) {
            if (view.getId() == R.id.back) {
                mBackTransitioning = true;
            } else if (view.getId() == R.id.home && transitionType == LayoutTransition.APPEARING) {
                mHomeAppearing = true;
                mStartDelay = transition.getStartDelay(transitionType);
                mDuration = transition.getDuration(transitionType);
                mInterpolator = transition.getInterpolator(transitionType);
            }
        }

        @Override
        public void endTransition(LayoutTransition transition, ViewGroup container,
                View view, int transitionType) {
            if (view.getId() == R.id.back) {
                mBackTransitioning = false;
            } else if (view.getId() == R.id.home && transitionType == LayoutTransition.APPEARING) {
                mHomeAppearing = false;
            }
        }

        public void onBackAltCleared() {
            // When dismissing ime during unlock, force the back button to run the same appearance
            // animation as home (if we catch this condition early enough).
            if (!mBackTransitioning && getBackButton().getVisibility() == VISIBLE
                    && mHomeAppearing && getHomeButton().getAlpha() == 0) {
                getBackButton().setAlpha(0);
                ValueAnimator a = ObjectAnimator.ofFloat(getBackButton(), "alpha", 0, 1);
                a.setStartDelay(mStartDelay);
                a.setDuration(mDuration);
                a.setInterpolator(mInterpolator);
                a.start();
            }
        }
    }

    private final OnClickListener mImeSwitcherClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            ((InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE))
                    .showInputMethodPicker(true /* showAuxiliarySubtypes */);
        }
    };

    private class H extends Handler {
        public void handleMessage(Message m) {
            switch (m.what) {
                case MSG_CHECK_INVALID_LAYOUT:
                    final String how = "" + m.obj;
                    final int w = getWidth();
                    final int h = getHeight();
                    final int vw = mCurrentView.getWidth();
                    final int vh = mCurrentView.getHeight();

                    if (h != vh || w != vw) {
                        Log.w(TAG, String.format(
                            "*** Invalid layout in navigation bar (%s this=%dx%d cur=%dx%d)",
                            how, w, h, vw, vh));
                        if (WORKAROUND_INVALID_LAYOUT) {
                            requestLayout();
                        }
                    }
                    break;
            }
        }
    }

    public NavigationBarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mDisplay = ((WindowManager)context.getSystemService(
                Context.WINDOW_SERVICE)).getDefaultDisplay();

        final Resources res = getContext().getResources();
        mBarSize = res.getDimensionPixelSize(R.dimen.navigation_bar_size);
        mVertical = false;
        mShowMenu = false;
        mTaskSwitchHelper = new NavigationBarViewTaskSwitchHelper(context);

        // MPlugin Navigation Bar creation and initialization
        try {
            mNavBarPlugin = (INavigationBarPlugin) MPlugin.createInstance(
            INavigationBarPlugin.class.getName(), context);
        } catch (Exception e) {
            Log.e(TAG, "Catch INavigationBarPlugin exception: ", e);
        }
        if (mNavBarPlugin == null) {
            Log.d(TAG, "DefaultNavigationBarPlugin");
            mNavBarPlugin = new DefaultNavigationBarPlugin(context);
        }

        getIcons(res);

        mBarTransitions = new NavigationBarTransitions(this);
        
		//add by chenhl start
        initLightDarkModeColor(context);
        //add by chenhl end
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ViewRootImpl root = getViewRootImpl();
        if (root != null) {
            root.setDrawDuringWindowsAnimating(true);
        }
        /**hb tangjun add begin*/
        mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(NAVIGATION_KEY_POSITION), false, new ContentObserver(mHandler) {
        	@Override
        	public void onChange(boolean selfChange,Uri uri) {
        		int toggle = Settings.Secure.getInt(mContext.getContentResolver(), NAVIGATION_KEY_POSITION, 0);
        		setNavigationButtonToggle(toggle == 0 ? false :true);
        	}
		});
        /*
		IntentFilter filter = new IntentFilter();
		filter.addAction(DISABLE_PULLUP_QSPANEL);
		filter.addAction(ENABLE_PULLUP_QSPANEL);
		mContext.registerReceiver(new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				if (intent.getAction().equals(DISABLE_PULLUP_QSPANEL)) {
					mDisablePullUpQSpanel = true;
				} else {
					mDisablePullUpQSpanel = false;
				}
			}
		}, filter);*/
        /**hb tangjun add end*/
    }

    public BarTransitions getBarTransitions() {
        return mBarTransitions;
    }

    public void setBar(PhoneStatusBar phoneStatusBar) {
        mTaskSwitchHelper.setBar(phoneStatusBar);
        /**hb tangjun add begin*/
        mPhoneStatusBar = phoneStatusBar;
        /**hb tangjun add end*/
    }

    public void setOnVerticalChangedListener(OnVerticalChangedListener onVerticalChangedListener) {
        mOnVerticalChangedListener = onVerticalChangedListener;
        notifyVerticalChangedListener(mVertical);
    }
    
    /**hb add by tangjun begin*/
    public void setDelegateView(View view) {
        mDelegateView = view;
    }
    /**hb add by tangjun end*/

    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	//Log.d("111111", "----NavigationBarView onTouchEvent getAction = ----" + event.getAction());
    	/**hb tangjun add begin*/
    	if(mPhoneStatusBar.isPanelFullyCollapsed() /*&& !mDisablePullUpQSpanel*/) {
        	mDelegateView.dispatchTouchEvent(event);
    	}
    	/**hb tangjun add end*/
        if (mTaskSwitchHelper.onTouchEvent(event)) {
            return true;
        }
        if (mDeadZone != null && event.getAction() == MotionEvent.ACTION_OUTSIDE) {
            mDeadZone.poke(event);
        }
        return super.onTouchEvent(event);
    }
    private boolean touchFlag = false;
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
    	//Log.d("111111", "----NavigationBarView onInterceptTouchEvent getAction = ----" + event.getAction());
    	/**hb tangjun add begin*/
    	if(!mPhoneStatusBar.isPanelFullyCollapsed() /*|| mDisablePullUpQSpanel*/) {
        	//Log.d("tangjun222", "---NavigationBarView onInterceptTouchEvent mDisablePullUpQSpanel = " + mDisablePullUpQSpanel);
    		return mTaskSwitchHelper.onInterceptTouchEvent(event);
    	}
    	switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			beginTracking(event);
			break;
		case MotionEvent.ACTION_MOVE:
			touchFlag = detectTracking(event);
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			cancelTracking(event);
			break;

		default:
			break;
		}
    	//Log.d("111111", "----NavigationBarView touchFlag = ----" + touchFlag);
    	if(touchFlag) {
    		return true;
    	} else {
    		return mTaskSwitchHelper.onInterceptTouchEvent(event);
    	}
    	//return mTaskSwitchHelper.onInterceptTouchEvent(event);
    	/**hb tangjun add end*/
    }
    
    /**hb tangjun add begin*/
    private void beginTracking(MotionEvent event) {
    	touchFlag = false;
    	mDownEvent = MotionEvent.obtain(event);
    	mDelegateView.dispatchTouchEvent(event);
    }
    
    private boolean detectTracking(MotionEvent event) {
        if (mDownEvent == null) {
            return false;
        }
        double currentX = event.getX();
        double currentY = event.getY();
        double downX = mDownEvent.getX();
        double downY = mDownEvent.getY();
        double currentZ = Utilities.isOrientationPortrait(getContext()) ? currentY : currentX;
        double downZ = Utilities.isOrientationPortrait(getContext()) ? downY : downX;
        double absX = Math.abs(currentX - downX);
        double absY = Math.abs(currentY - downY);
        double distance = Math.hypot(absX, absY);
        
        if(distance > 30 && currentZ < downZ) {
        	return true;
        }
        
        return false;
    }
    
    private void cancelTracking(MotionEvent event) {
    	mDownEvent = null;
    }
    /**hb tangjun add end*/

    public void abortCurrentGesture() {
        getHomeButton().abortCurrentGesture();
    }

    private H mHandler = new H();

    public View getCurrentView() {
        return mCurrentView;
    }

    public View getRecentsButton() {
        ImageView view = (ImageView) mCurrentView.findViewById(R.id.recent_apps);
        view.setImageDrawable(mNavBarPlugin.getRecentImage(view.getDrawable()));
        return view;
    }

    public View getMenuButton() {
        return mCurrentView.findViewById(R.id.menu);
    }

    public View getBackButton() {
        ImageView view = (ImageView) mCurrentView.findViewById(R.id.back);
        view.setImageDrawable(mNavBarPlugin.getBackImage(view.getDrawable()));
        return view;
    }

    public KeyButtonView getHomeButton() {
        KeyButtonView view = (KeyButtonView) mCurrentView.findViewById(R.id.home);
        view.setImageDrawable(mNavBarPlugin.getHomeImage(view.getDrawable()));
        return (KeyButtonView) view;
    }

    public View getImeSwitchButton() {
        return mCurrentView.findViewById(R.id.ime_switcher);
    }

    private void getIcons(Resources res) {
        mBackIcon = mNavBarPlugin.getBackImage(res.getDrawable(R.drawable.ic_sysbar_back));

        mBackLandIcon =
        mNavBarPlugin.getBackLandImage(res.getDrawable(R.drawable.ic_sysbar_back_land));

        mBackAltIcon =
        mNavBarPlugin.getBackImeImage(res.getDrawable(R.drawable.ic_sysbar_back_ime));

        mBackAltLandIcon =
        mNavBarPlugin.getBackImeImage(res.getDrawable(R.drawable.ic_sysbar_back_ime_land));

        mRecentIcon =
        mNavBarPlugin.getRecentImage(res.getDrawable(R.drawable.ic_sysbar_recent));

        mRecentLandIcon =
        mNavBarPlugin.getRecentLandImage(res.getDrawable(R.drawable.ic_sysbar_recent_land));
    }

    @Override
    public void setLayoutDirection(int layoutDirection) {
        getIcons(getContext().getResources());

        super.setLayoutDirection(layoutDirection);
    }

    public void notifyScreenOn(boolean screenOn) {
        mScreenOn = screenOn;
        setDisabledFlags(mDisabledFlags, true);
    }

    public void setNavigationIconHints(int hints) {
        setNavigationIconHints(hints, false);
    }

    public void setNavigationIconHints(int hints, boolean force) {
        if (!force && hints == mNavigationIconHints) return;
        final boolean backAlt = (hints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) != 0;
        if ((mNavigationIconHints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) != 0 && !backAlt) {
            mTransitionListener.onBackAltCleared();
        }
        if (DEBUG) {
            android.widget.Toast.makeText(getContext(),
                "Navigation icon hints = " + hints,
                500).show();
        }

        mNavigationIconHints = hints;

        ((ImageView)getBackButton()).setImageDrawable(backAlt
                ? (mVertical ? mBackAltLandIcon : mBackAltIcon)
                : (mVertical ? mBackLandIcon : mBackIcon));

        ((ImageView)getRecentsButton()).setImageDrawable(mVertical ? mRecentLandIcon : mRecentIcon);

        final boolean showImeButton = ((hints & StatusBarManager.NAVIGATION_HINT_IME_SHOWN) != 0);
        getImeSwitchButton().setVisibility(showImeButton ? View.VISIBLE : View.INVISIBLE);
        // Update menu button in case the IME state has changed.
        setMenuVisibility(mShowMenu, true);


        setDisabledFlags(mDisabledFlags, true);
        
        setIconsDark(mDark); //add by chenhl
    }

    public void setDisabledFlags(int disabledFlags) {
        setDisabledFlags(disabledFlags, false);
    }

    public void setDisabledFlags(int disabledFlags, boolean force) {
        if (!force && mDisabledFlags == disabledFlags) return;

        mDisabledFlags = disabledFlags;

        final boolean disableHome = ((disabledFlags & View.STATUS_BAR_DISABLE_HOME) != 0);
        boolean disableRecent = ((disabledFlags & View.STATUS_BAR_DISABLE_RECENT) != 0);
        final boolean disableBack = ((disabledFlags & View.STATUS_BAR_DISABLE_BACK) != 0)
                && ((mNavigationIconHints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) == 0);
        final boolean disableSearch = ((disabledFlags & View.STATUS_BAR_DISABLE_SEARCH) != 0);

        if (SLIPPERY_WHEN_DISABLED) {
            setSlippery(disableHome && disableRecent && disableBack && disableSearch);
        }

        ViewGroup navButtons = (ViewGroup) mCurrentView.findViewById(R.id.nav_buttons);
        if (navButtons != null) {
            LayoutTransition lt = navButtons.getLayoutTransition();
            if (lt != null) {
                if (!lt.getTransitionListeners().contains(mTransitionListener)) {
                    lt.addTransitionListener(mTransitionListener);
                }
            }
        }
        if (inLockTask() && disableRecent && !disableHome) {
            // Don't hide recents when in lock task, it is used for exiting.
            // Unless home is hidden, then in DPM locked mode and no exit available.
            disableRecent = false;
        }

        getBackButton()   .setVisibility(disableBack       ? View.INVISIBLE : View.VISIBLE);
        getHomeButton()   .setVisibility(disableHome       ? View.INVISIBLE : View.VISIBLE);
        getRecentsButton().setVisibility(disableRecent     ? View.INVISIBLE : View.VISIBLE);
    }

    private boolean inLockTask() {
        try {
            return ActivityManagerNative.getDefault().isInLockTaskMode();
        } catch (RemoteException e) {
            return false;
        }
    }

    private void setVisibleOrGone(View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? VISIBLE : GONE);
        }
    }

    public void setWakeAndUnlocking(boolean wakeAndUnlocking) {
        setUseFadingAnimations(wakeAndUnlocking);
        setLayoutTransitionsEnabled(!wakeAndUnlocking);
    }

    private void setLayoutTransitionsEnabled(boolean enabled) {
        mLayoutTransitionsEnabled = enabled;
        ViewGroup navButtons = (ViewGroup) mCurrentView.findViewById(R.id.nav_buttons);
        LayoutTransition lt = navButtons.getLayoutTransition();
        if (lt != null) {
            if (enabled) {
                lt.enableTransitionType(LayoutTransition.APPEARING);
                lt.enableTransitionType(LayoutTransition.DISAPPEARING);
                lt.enableTransitionType(LayoutTransition.CHANGE_APPEARING);
                lt.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
            } else {
                lt.disableTransitionType(LayoutTransition.APPEARING);
                lt.disableTransitionType(LayoutTransition.DISAPPEARING);
                lt.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
                lt.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
            }
        }
    }

    private void setUseFadingAnimations(boolean useFadingAnimations) {
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
        if (lp != null) {
            boolean old = lp.windowAnimations != 0;
            if (!old && useFadingAnimations) {
                lp.windowAnimations = R.style.Animation_NavigationBarFadeIn;
            } else if (old && !useFadingAnimations) {
                lp.windowAnimations = 0;
            } else {
                return;
            }
            //modify by chenhl start
            if(isAttachedToWindow()) {
                WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                wm.updateViewLayout(this, lp);
            }
            //modify by chenhl end
        }
    }

    public void setSlippery(boolean newSlippery) {
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
        if (lp != null) {
            boolean oldSlippery = (lp.flags & WindowManager.LayoutParams.FLAG_SLIPPERY) != 0;
            if (!oldSlippery && newSlippery) {
                lp.flags |= WindowManager.LayoutParams.FLAG_SLIPPERY;
            } else if (oldSlippery && !newSlippery) {
                lp.flags &= ~WindowManager.LayoutParams.FLAG_SLIPPERY;
            } else {
                return;
            }
            //modify by chnehl start
            if(isAttachedToWindow()) {
                WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                wm.updateViewLayout(this, lp);
            }
            //modify by chenhl end
        }
    }

    public void setMenuVisibility(final boolean show) {
        setMenuVisibility(show, false);
    }

    public void setMenuVisibility(final boolean show, final boolean force) {
        if (!force && mShowMenu == show) return;

        mShowMenu = show;

        // Only show Menu if IME switcher not shown.
        final boolean shouldShow = mShowMenu &&
                ((mNavigationIconHints & StatusBarManager.NAVIGATION_HINT_IME_SHOWN) == 0);
        getMenuButton().setVisibility(shouldShow ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onFinishInflate() {
        mRotatedViews[Surface.ROTATION_0] =
        mRotatedViews[Surface.ROTATION_180] = findViewById(R.id.rot0);

        mRotatedViews[Surface.ROTATION_90] = findViewById(R.id.rot90);

        mRotatedViews[Surface.ROTATION_270] = mRotatedViews[Surface.ROTATION_90];
    	/**hb tangjun add begin*/
        mRotatedViews[Surface.ROTATION_0 + 4] =
        mRotatedViews[Surface.ROTATION_180 + 4] = findViewById(R.id.rot0_toggle);

        mRotatedViews[Surface.ROTATION_90 + 4] = findViewById(R.id.rot90_toggle);

        mRotatedViews[Surface.ROTATION_270 + 4] = mRotatedViews[Surface.ROTATION_90 + 4];
    	/**hb tangjun add end*/

        /**hb tangjun mod begin*/
        //改为默认返回键在右边
        //mCurrentView = mRotatedViews[Surface.ROTATION_0 ];
        mCurrentView = mRotatedViews[Surface.ROTATION_0 + 4];
        /**hb tangjun mod end*/
        
        /**hb tangjun add begin*/
        int toggle = Settings.Secure.getInt(mContext.getContentResolver(), NAVIGATION_KEY_POSITION, 0);
        mToggle = toggle == 0 ? false :true;
        /**hb tangjun add end*/

        getImeSwitchButton().setOnClickListener(mImeSwitcherClickListener);
        getHideButton().setOnClickListener(mHideClickListener);//add by chenhl
        updateRTLOrder();
        updateHideView();//add by chenhl
    }

    public boolean isVertical() {
        return mVertical;
    }

    public void reorient() {
        final int rot = mDisplay.getRotation();
        //hb tangjun mod begin
        //for (int i=0; i<4; i++) {
        for (int i=0; i<8; i++) {
        //hb tangjun mod end	
        	mRotatedViews[i].setVisibility(View.GONE);
        }
        
        //hb tangjun mod begin
        if(mToggle) {
        	mCurrentView = mRotatedViews[rot];
        } else {
        	mCurrentView = mRotatedViews[rot + 4];
        }
        //mCurrentView = mRotatedViews[rot];
        //hb tangjun mod end
        mCurrentView.setVisibility(View.VISIBLE);
        setLayoutTransitionsEnabled(mLayoutTransitionsEnabled);

        getImeSwitchButton().setOnClickListener(mImeSwitcherClickListener);
        getHideButton().setOnClickListener(mHideClickListener);//add by chenhl
        /**hb tangjun add begin*/
        setLightNavigationBarLineShow(mShowLine);
        /**hb tangjun add end*/

        mDeadZone = (DeadZone) mCurrentView.findViewById(R.id.deadzone);

        // force the low profile & disabled states into compliance
        mBarTransitions.init();
        setDisabledFlags(mDisabledFlags, true /* force */);
        setMenuVisibility(mShowMenu, true /* force */);

        if (DEBUG) {
            Log.d(TAG, "reorient(): rot=" + mDisplay.getRotation());
        }

        updateTaskSwitchHelper();

        setNavigationIconHints(mNavigationIconHints, true);
        updateHideView();//add by chenhl
    }

    private void updateTaskSwitchHelper() {
        boolean isRtl = (getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
        mTaskSwitchHelper.setBarState(mVertical, isRtl);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (DEBUG) Log.d(TAG, String.format(
                    "onSizeChanged: (%dx%d) old: (%dx%d)", w, h, oldw, oldh));

        final boolean newVertical = w > 0 && h > w;
        if (newVertical != mVertical) {
            mVertical = newVertical;
            //Log.v(TAG, String.format("onSizeChanged: h=%d, w=%d, vert=%s", h, w, mVertical?"y":"n"));
            reorient();
            notifyVerticalChangedListener(newVertical);
        }

        postCheckForInvalidLayout("sizeChanged");
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private void notifyVerticalChangedListener(boolean newVertical) {
        if (mOnVerticalChangedListener != null) {
            mOnVerticalChangedListener.onVerticalChanged(newVertical);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateRTLOrder();
        updateTaskSwitchHelper();

        /// M: [ALPS01868023] Set ContentDescription when language changed. @{
        getBackButton().setContentDescription(
            getResources().getString(R.string.accessibility_back));
        getHomeButton().setContentDescription(
            getResources().getString(R.string.accessibility_home));
        getRecentsButton().setContentDescription(
            getResources().getString(R.string.accessibility_recent));
        getMenuButton().setContentDescription(
            getResources().getString(R.string.accessibility_menu));
        getImeSwitchButton().setContentDescription(
            getResources().getString(R.string.accessibility_ime_switch_button));
        /// M: [ALPS01868023] Set ContentDescription when language changed. @}
    }

    /**
     * In landscape, the LinearLayout is not auto mirrored since it is vertical. Therefore we
     * have to do it manually
     */
    private void updateRTLOrder() {
        boolean isLayoutRtl = getResources().getConfiguration()
                .getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        if (mIsLayoutRtl != isLayoutRtl) {

            // We swap all children of the 90 and 270 degree layouts, since they are vertical
            View rotation90 = mRotatedViews[Surface.ROTATION_90];
            swapChildrenOrderIfVertical(rotation90.findViewById(R.id.nav_buttons));
            adjustExtraKeyGravity(rotation90, isLayoutRtl);

            View rotation270 = mRotatedViews[Surface.ROTATION_270];
            if (rotation90 != rotation270) {
                swapChildrenOrderIfVertical(rotation270.findViewById(R.id.nav_buttons));
                adjustExtraKeyGravity(rotation270, isLayoutRtl);
            }
            mIsLayoutRtl = isLayoutRtl;
        }
    }

    private void adjustExtraKeyGravity(View navBar, boolean isLayoutRtl) {
        View menu = navBar.findViewById(R.id.menu);
        View imeSwitcher = navBar.findViewById(R.id.ime_switcher);
        if (menu != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) menu.getLayoutParams();
            lp.gravity = isLayoutRtl ? Gravity.BOTTOM : Gravity.TOP;
            menu.setLayoutParams(lp);
        }
        if (imeSwitcher != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) imeSwitcher.getLayoutParams();
            lp.gravity = isLayoutRtl ? Gravity.BOTTOM : Gravity.TOP;
            imeSwitcher.setLayoutParams(lp);
        }
    }

    /**
     * Swaps the children order of a LinearLayout if it's orientation is Vertical
     *
     * @param group The LinearLayout to swap the children from.
     */
    private void swapChildrenOrderIfVertical(View group) {
        if (group instanceof LinearLayout) {
            LinearLayout linearLayout = (LinearLayout) group;
            if (linearLayout.getOrientation() == VERTICAL) {
                int childCount = linearLayout.getChildCount();
                ArrayList<View> childList = new ArrayList<>(childCount);
                for (int i = 0; i < childCount; i++) {
                    childList.add(linearLayout.getChildAt(i));
                }
                linearLayout.removeAllViews();
                for (int i = childCount - 1; i >= 0; i--) {
                    linearLayout.addView(childList.get(i));
                }
            }
        }
    }

    /*
    @Override
    protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
        if (DEBUG) Log.d(TAG, String.format(
                    "onLayout: %s (%d,%d,%d,%d)",
                    changed?"changed":"notchanged", left, top, right, bottom));
        super.onLayout(changed, left, top, right, bottom);
    }

    // uncomment this for extra defensiveness in WORKAROUND_INVALID_LAYOUT situations: if all else
    // fails, any touch on the display will fix the layout.
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (DEBUG) Log.d(TAG, "onInterceptTouchEvent: " + ev.toString());
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            postCheckForInvalidLayout("touch");
        }
        return super.onInterceptTouchEvent(ev);
    }
    */


    private String getResourceName(int resId) {
        if (resId != 0) {
            final android.content.res.Resources res = getContext().getResources();
            try {
                return res.getResourceName(resId);
            } catch (android.content.res.Resources.NotFoundException ex) {
                return "(unknown)";
            }
        } else {
            return "(null)";
        }
    }

    private void postCheckForInvalidLayout(final String how) {
        mHandler.obtainMessage(MSG_CHECK_INVALID_LAYOUT, 0, 0, how).sendToTarget();
    }

    private static String visibilityToString(int vis) {
        switch (vis) {
            case View.INVISIBLE:
                return "INVISIBLE";
            case View.GONE:
                return "GONE";
            default:
                return "VISIBLE";
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("NavigationBarView {");
        final Rect r = new Rect();
        final Point size = new Point();
        mDisplay.getRealSize(size);

        pw.println(String.format("      this: " + PhoneStatusBar.viewInfo(this)
                        + " " + visibilityToString(getVisibility())));

        getWindowVisibleDisplayFrame(r);
        final boolean offscreen = r.right > size.x || r.bottom > size.y;
        pw.println("      window: "
                + r.toShortString()
                + " " + visibilityToString(getWindowVisibility())
                + (offscreen ? " OFFSCREEN!" : ""));

        pw.println(String.format("      mCurrentView: id=%s (%dx%d) %s",
                        getResourceName(mCurrentView.getId()),
                        mCurrentView.getWidth(), mCurrentView.getHeight(),
                        visibilityToString(mCurrentView.getVisibility())));

        pw.println(String.format("      disabled=0x%08x vertical=%s menu=%s",
                        mDisabledFlags,
                        mVertical ? "true" : "false",
                        mShowMenu ? "true" : "false"));

        dumpButton(pw, "back", getBackButton());
        dumpButton(pw, "home", getHomeButton());
        dumpButton(pw, "rcnt", getRecentsButton());
        dumpButton(pw, "menu", getMenuButton());

        pw.println("    }");
    }

    private static void dumpButton(PrintWriter pw, String caption, View button) {
        pw.print("      " + caption + ": ");
        if (button == null) {
            pw.print("null");
        } else {
            pw.print(PhoneStatusBar.viewInfo(button)
                    + " " + visibilityToString(button.getVisibility())
                    + " alpha=" + button.getAlpha()
                    );
        }
        pw.println();
    }

    public interface OnVerticalChangedListener {
        void onVerticalChanged(boolean isVertical);
    }
    //add by chenhl start
    private boolean mHideView=false;
    public void updateBarTranslation(boolean is){
        mHideView = is;
        if(isAttachedToWindow()) {
            setVisibility(is ? View.GONE : View.VISIBLE);
        }
    }

    public boolean isHideBar(){
        return mHideView;
    }
    //add by chenhl end
    
    //add by chenhl start
    public static final String HIDE_NAVIGATION_BAR = "hide_navigation_bar";
    private int mDarkModeIconColorSingleTone;
    private int mLightModeIconColorSingleTone;
    private boolean mDark=false;

    public boolean isDark(){
        return mDark;
    }
    public void setIconsDark2(boolean dark){
        mDark = dark;
        setNavigationIconHints(mNavigationIconHints, true);
    }
    public void setIconsDark(boolean dark) {
        if(getHomeButton()==null){
            return;
        }
        //add for inputmethed show
        final boolean backAlt = (mNavigationIconHints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) != 0;
        /*if((getImeSwitchButton().getVisibility()==View.VISIBLE||backAlt)&&!mVertical){
            dark = false;
        }*/
        int tint = (int) ArgbEvaluator.getInstance().evaluate(dark?1f:0f,
                mLightModeIconColorSingleTone, mDarkModeIconColorSingleTone);
        KeyButtonView backButton = (KeyButtonView)getBackButton();
        KeyButtonView homeButton = (KeyButtonView)getHomeButton();
        KeyButtonView recentButton = (KeyButtonView)getRecentsButton();
        KeyButtonView imeButton = (KeyButtonView)getImeSwitchButton();
        KeyButtonView menuButton = (KeyButtonView)getMenuButton();
        KeyButtonView hideButton = (KeyButtonView) getHideButton();

        //getBackButton().setImageDrawable(dark?mBackAltIcon:mBackIcon);
        setImageTint((ImageView)getBackButton(), tint);
        setImageTint((ImageView)getHomeButton(), tint);
        setImageTint((ImageView)getRecentsButton(), tint);
        setImageTint((ImageView)getImeSwitchButton(), tint);
        setImageTint((ImageView)getMenuButton(), tint);
        hideButton.getDrawable().setTint(tint);
        //modify ripple color
        backButton.setBackground(new KeyButtonRipple(backButton.getContext(),backButton,tint));
        homeButton.setBackground(new KeyButtonRipple(homeButton.getContext(),homeButton,tint));
        recentButton.setBackground(new KeyButtonRipple(recentButton.getContext(),recentButton,tint));
        imeButton.setBackground(new KeyButtonRipple(imeButton.getContext(),imeButton,tint));
        menuButton.setBackground(new KeyButtonRipple(menuButton.getContext(),menuButton,tint));
        hideButton.setBackground(new KeyButtonRipple(hideButton.getContext(),hideButton,tint));
    }

    private void initLightDarkModeColor(Context context){
        mDarkModeIconColorSingleTone = context.getColor(R.color.dark_mode_navigationbar_tone);
        mLightModeIconColorSingleTone = context.getColor(R.color.light_mode_icon_color_single_tone);
    }
    
    public void setImageTint(ImageView view, int tint){
    	Drawable drawable = view.getDrawable();
    	drawable.mutate();
    	drawable.setColorFilter(tint, PorterDuff.Mode.SRC_ATOP);
    	view.setImageDrawable(drawable);
    }
    public View getHideButton() {
        return mCurrentView.findViewById(R.id.hide);
    }

    private OnClickListener mHideClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {

            Intent intent = new Intent("com_hb_hide_navagationbar_action");
            intent.putExtra("state",0);
            getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
//        	if(mToggle) {
//            	setNavigationButtonToggle(false);
//        	} else {
//            	setNavigationButtonToggle(true);
//        	}
//            Intent intent = new Intent();
//            if(mDisablePullUpQSpanel) {
//            	intent.setAction(ENABLE_PULLUP_QSPANEL);
//            } else {
//            	intent.setAction(DISABLE_PULLUP_QSPANEL);
//            }
//            getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    };



    public void updateHideView(){
        if(getHideButton()==null){
            return;
        }
        int hide = Settings.Secure.getInt(getContext().getContentResolver(), HIDE_NAVIGATION_BAR, 0);
        getHideButton().setVisibility(hide==1?View.VISIBLE:View.INVISIBLE);
        /**hb tangjun add for sync for toggle navigation bar begin*/
    	setButtonVisibility();
    	/**hb tangjun add for sync for toggle navigation bar end*/
    }

    //add by chenhl end
    
    /**hb tangjun add begin*/
    private boolean mShowLine = false;
    public void setLightNavigationBarLineShow(boolean show){
    	mShowLine = show;
        if(show) {
        	getLightNavigationBarLine().setVisibility(View.VISIBLE);
        } else {
        	getLightNavigationBarLine().setVisibility(View.GONE);
        }
    }
    
    public View getLightNavigationBarLine() {
        View view = mCurrentView.findViewById(R.id.line);
        return view;
    }
    
    private void setNavigationButtonToggle(boolean toggle) {
    	mToggle = toggle;
    	reorient();
    }
    
    private void setButtonVisibility() {
        for (int i=0; i<8; i++) {
        	mRotatedViews[i].findViewById(R.id.hide).setVisibility(getHideButton().getVisibility());
        }
    }
    /**hb tangjun add end*/
}
