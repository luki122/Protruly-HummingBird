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

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.android.systemui.R;
import com.android.systemui.statusbar.CustomStatusBarManager;

public class IconMerger extends LinearLayout /*implements StatusBarNotificationBoxStatusListener*/ {
    private static final String TAG = "IconMerger";
    private static final boolean DEBUG = true;

    private int mIconSize;
    
    //ShenQianfeng add begin
    private View mNotificationBoxIndicatorView;
    private CustomStatusBarManager mCustomStatusBarManager;
    //ShenQianfeng add end
    
    private View mMoreView;
    
    //ShenQianfeng add begin
    private boolean mMoreRequired;
    private boolean mNotificationBoxIndicatorRequired;
    //ShenQianfeng add end

    public IconMerger(Context context, AttributeSet attrs) {
        super(context, attrs);
        mIconSize = context.getResources().getDimensionPixelSize(R.dimen.status_bar_icon_size);
        /*
        if (DEBUG) {
            setBackgroundColor(0x800099FF);
        }
        */
    }
    
    //ShenQianfeng add begin
    public void setNotificationBoxIndicator(View v) {
        mNotificationBoxIndicatorView = v;
    }

    public void setCustomStatusBarManager(CustomStatusBarManager manager) {
        mCustomStatusBarManager = manager;
    }
    //ShenQianfeng add end

    public void setOverflowIndicator(View v) {
        mMoreView = v;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        //ShenQianfeng add begin
        if(mCustomStatusBarManager == null) {
            return;
        }
        //ShenQianfeng add end
        
        // we need to constrain this to an integral multiple of our children
        int width = getMeasuredWidth();
        
        //ShenQianfeng modify begin
        //Original:
        //Log.i(TAG, "onMeasure width:" + width + " getChildCount():" + getChildCount());
        //setMeasuredDimension(width - (width % mIconSize), getMeasuredHeight());
        //Modify to:
        int N = getChildCount();
        int timesOfWidth = width - (width % mIconSize);
        if(timesOfWidth <=0 ) {
            timesOfWidth = width;
        }
        // [-] indicates notification box indicator.
        boolean tooLong = false;
        int newWidth = getMeasuredWidth();
        int newHeight = getMeasuredHeight();
        
        //Log.i(TAG, "onMeasure width:" + width + " getChildCount():" + getChildCount()+" mNotificationBoxIndicatorRequired:"+mNotificationBoxIndicatorRequired);
        
        if(mNotificationBoxIndicatorRequired) {
            //Log.i(TAG, "onMeasure 11111111 ");
            // we have normal priority notification here, we need to show [-].
            // we don't show [...] here.
            // and if priority-max notifications are too long, 
            // we show parts of them, otherwise we show all of them.
            mMoreRequired = false;
            //no need to get all priority-max notifications here, just use the children
            newWidth = (N * mIconSize >= (timesOfWidth - mIconSize)) ? (timesOfWidth - mIconSize) : (N * mIconSize);
        } else {
            //Log.i(TAG, "onMeasure 2222222 ");
            // we don't show [-] here
            // 1) if notifications(include non-priority-max and priority-max) are too long show [...]
            if(N * mIconSize >= timesOfWidth) {
                mMoreRequired = true;
                newWidth = timesOfWidth - mIconSize;
            }
            // 2) if notifications not too long, show nothing.
            else {
                mMoreRequired = false;
                newWidth = timesOfWidth;
            }
        }
        setMeasuredDimension(newWidth, newHeight);

        /*
        if(mCustomStatusBarManager.isNotificationBoxOn()) {
            if(mCustomStatusBarManager.hasNotificationWithPriorityNormal()) {
                // we have normal priority notification here, we need to show [-].
                // we don't show [...] here.
                // and if priority-max notifications are too long, 
                // we show parts of them, otherwise we show all of them.
                mNotificationBoxIndicatorRequired = true;
                mMoreRequired = false;
                //no need to get all priority-max notifications here, just use the children
                newWidth = (N * mIconSize >= (timesOfWidth - mIconSize)) ? (timesOfWidth - mIconSize) : (N * mIconSize);
            } else {
                // we don't have normal priority notification here
                // so we don't show [-]
                mNotificationBoxIndicatorRequired = false;
                // 1) if notifications are too long, show [...]
                // TODO: check overflow
                if(N * mIconSize >= timesOfWidth) {
                    mMoreRequired = true;
                    newWidth = timesOfWidth - mIconSize;
                }
                // 2) if notifications not too long, show nothing.
                // TODO:
                // 
                else {
                    mMoreRequired = false;
                    newWidth = timesOfWidth;
                }
            }
        } else {
            // we don't show [-] here 
            mNotificationBoxIndicatorRequired = false;
            // 1) if notifications(include non-priority-max and priority-max) are too long show [...]
            if(N * mIconSize >= timesOfWidth) {
                mMoreRequired = true;
                newWidth = timesOfWidth - mIconSize;
            }
            // 2) if notifications not too long, show nothing.
            else {
                mMoreRequired = false;
                newWidth = timesOfWidth;
            }
        }
        */
        
        /*
        mMoreRequired = N * mIconSize >= timesOfWidth;
        if(mMoreRequired) {
            setMeasuredDimension(timesOfWidth - mIconSize, getMeasuredHeight());
            Log.i(TAG, "onMeasure width:" + (timesOfWidth - mIconSize) + " getChildCount():" + getChildCount() + " mMoreRequired:" + mMoreRequired);
        } else {
            Log.i(TAG, "onMeasure width:" + width + " getChildCount():" + getChildCount() + " mMoreRequired:" + mMoreRequired);
            setMeasuredDimension(N * mIconSize, getMeasuredHeight());
        }
        */
        
        /*
        int adjustedWidth = (width - (width % mIconSize) - mIconSize);
        if(adjustedWidth < 0) adjustedWidth = 0;
        setMeasuredDimension(adjustedWidth, getMeasuredHeight());
        Log.i(TAG, "onMeasure width:" + width + " adjustedWidth:" + adjustedWidth);
        */
        //ShenQianfeng modify end
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        checkOverflow(r - l);
        //Log.i(TAG, "onLayout ----- ");
    }

    private void checkOverflow(int width) {
        //ShenQianfeng modify begin
        //Original:
        /*
        final int N = getChildCount();
        
        int visibleChildren = 0;
        for (int i=0; i<N; i++) {
            if (getChildAt(i).getVisibility() != GONE) visibleChildren++;
        }
        final boolean overflowShown = (mMoreView.getVisibility() == View.VISIBLE);
        // let's assume we have one more slot if the more icon is already showing
        
        Log.i(TAG, "111 checkOverflow: width:" + width + " child count:" + N + 
                " visibleChildren:" + visibleChildren + 
                " overflowShown:" + overflowShown + 
                " mIconSize:" + mIconSize);
        
        if (overflowShown) visibleChildren --;
        
        Log.i(TAG, "222 visibleChildren: " + visibleChildren);
        
        final boolean moreRequired = visibleChildren * mIconSize > width;
        if (moreRequired != overflowShown) {
            post(new Runnable() {
                @Override
                public void run() {
                    mMoreView.setVisibility(moreRequired ? View.VISIBLE : View.GONE);
                }
            });
        }
        */
        //Modify to:
        if(mMoreView == null || mNotificationBoxIndicatorView == null) return;
        final boolean overflowShown = (mMoreView.getVisibility() == View.VISIBLE);
        final boolean notificationBoxIndicatorShown = mNotificationBoxIndicatorView.getVisibility() == View.VISIBLE;
        final boolean changeMore = mMoreRequired != overflowShown;
        final boolean changeIndicator = mNotificationBoxIndicatorRequired != notificationBoxIndicatorShown;
        if (changeMore || changeIndicator) {
            post(new Runnable() {
                @Override
                public void run() {
                    if(changeMore && mMoreView != null) {
                        mMoreView.setVisibility(mMoreRequired ? View.VISIBLE : View.GONE);
                    }
                    if(changeIndicator && mNotificationBoxIndicatorView != null) {
                        mNotificationBoxIndicatorView.setVisibility(mNotificationBoxIndicatorRequired ? 
                                View.VISIBLE : View.GONE);
                    }
                }
            });
        }
        //ShenQianfeng modify end
    }


    //ShenQianfeng add begin
    /*
    @Override
    public void onStatusBarNotificationBoxStatusChange(boolean on) {
        if(on) {
            post(new Runnable() {
                @Override
                public void run() {
                    mMoreView.setVisibility(View.GONE);
                }
            });
        } else {
            
        }
    }
    */
    //ShenQianfeng add end

    //add by chenhl start
    public void setBoxIndicatorRequired(boolean is){
        if(mNotificationBoxIndicatorRequired!=is) {
            mNotificationBoxIndicatorRequired =is;
            if(!mNotificationBoxIndicatorRequired&&mNotificationBoxIndicatorView!=null){
                mNotificationBoxIndicatorView.setVisibility(View.GONE);
            }
            requestLayout();
        }
    }
    //add by chenhl end
}
