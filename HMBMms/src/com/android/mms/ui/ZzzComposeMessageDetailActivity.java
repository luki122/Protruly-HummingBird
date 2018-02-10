/*
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 *
 * Copyright (C) 2012 The Android Open Source Project.
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

package com.android.mms.ui;

import java.util.ArrayList;

import hb.app.dialog.AlertDialog;
import hb.app.dialog.ProgressDialog;
import hb.app.HbActivity;
import hb.widget.toolbar.Toolbar;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import android.widget.ScrollView;
import android.widget.TextView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.text.TextUtils;

import com.android.mms.R;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.ui.MessageUtils;
import java.lang.reflect.Method;

/**
 * hummingbird add by tangyisen for  hb style 2017.3.28
 * */
public class ZzzComposeMessageDetailActivity extends HbActivity{

    private static final String TAG = "MessageDetailActivity";
    private String mMsgBodyText;
    private ZzzDetailCheckOverSizeTextView mMsgBodyTextView;
    private ScrollView mScrollView;
    private float mDownX;
    private float mDownY;
    private int mTouchSlop;
    private int mScrollBarTop;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideBottomUIMenu();
        setHbContentView(R.layout.zzz_message_detail_content);
        final ViewConfiguration configuration = ViewConfiguration.get(this);
        mTouchSlop = configuration.getScaledTouchSlop();
        mScrollBarTop = (int)getResources().getDimension(R.dimen.mms_detail_scroll_padding_top);
        /*handleIntent();
        initUi();*/
    }

    /**
     * 隐藏虚拟按键，并且全屏
     */
    protected void hideBottomUIMenu() {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    /**获取虚拟功能键高度 */
    public int getVirtualBarHeigh() {
           int vh = 0;
           WindowManager windowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
           Display display = windowManager.getDefaultDisplay();
           DisplayMetrics dm = new DisplayMetrics();
           try {
               @SuppressWarnings("rawtypes")
               Class c = Class.forName("android.view.Display");
               @SuppressWarnings("unchecked")
               Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
               method.invoke(display, dm);
               vh = dm.heightPixels - windowManager.getDefaultDisplay().getHeight();
           } catch (Exception e) {
               e.printStackTrace();
           }
           return vh;
       }

    @Override
    protected void onResume() {
        super.onResume();
        hideBottomUIMenu();
        handleIntent();
        initUi();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initUi() {
        mScrollView = (ScrollView)findViewById(R.id.message_hb_detail);
        //mScrollView.setPadding( 0, mScrollBarTop, 0, getVirtualBarHeigh() );
        mScrollView.setOnTouchListener(new View.OnTouchListener() {
            private boolean mIsMove;
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                // TODO Auto-generated method stub
                //mGestureDetector.onTouchEvent(arg1);
                boolean isOverSize = mMsgBodyTextView != null ? mMsgBodyTextView.getIsEvenOverSize() : false;
                int action = arg1.getAction();
                if(action == MotionEvent.ACTION_DOWN) {
                    mDownX = arg1.getX();
                    mDownY = arg1.getY();
                }
                /*if(action == MotionEvent.ACTION_MOVE) {
                    mIsMove = true;
                }*/
                if(action == MotionEvent.ACTION_UP) {
                    float distanceX = Math.abs(arg1.getX() - mDownX);
                    float distanceY = Math.abs(arg1.getY() - mDownY);
                    if(!isOverSize || (isOverSize &&/* !mIsMove*/(distanceX < mTouchSlop && distanceY < mTouchSlop))) {
                        ZzzComposeMessageDetailActivity.this.finish();
                        return true;
                    }
                    mIsMove = false;
                }
                return false;
            }
        });
        mMsgBodyTextView = (ZzzDetailCheckOverSizeTextView)findViewById(R.id.message_hb_detail_body);
        mMsgBodyTextView.setTextIsSelectable( true );
        if(!TextUtils.isEmpty(mMsgBodyText)) {
            mMsgBodyTextView.setText(mMsgBodyText);
        }
        mMsgBodyTextView.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                ZzzComposeMessageDetailActivity.this.finish();
            }
        } );
        mMsgBodyTextView.setOnOverLineChangedListener(new ZzzDetailCheckOverSizeTextView.OnOverSizeChangedListener() {
            @Override
            public void onChanged(boolean isOverSize) {
                // TODO Auto-generated method stub
                if(isOverSize){
                    //mMsgBodyTextView.setLines(Integer.MAX_VALUE);
                    mMsgBodyTextView.displayAll();
                }
                //contentText.setIsCallChangedListener(false);
            }
        });
    }

    private void handleIntent() {
        Intent intent = getIntent();
        mMsgBodyText = intent.getStringExtra("msgBody");

        // Cancel failed notification. if need
        /*MessageUtils.cancelFailedToDeliverNotification(intent, this);
        MessageUtils.cancelFailedDownloadNotification(intent, this);*/

        if (TextUtils.isEmpty(mMsgBodyText)) {
            Log.e(TAG, "There's no sms uri!");
            finish();
        }
    }
}
