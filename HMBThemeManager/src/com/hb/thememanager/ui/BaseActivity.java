package com.hb.thememanager.ui;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import hb.app.HbActivity;

/**
 * Created by caizhongting on 17-6-13.
 */

public class BaseActivity extends HbActivity {

    protected static final int INPUT_METHOD_SHOW = 10001;
    protected static final int INPUT_METHOD_HIDE = 10002;

    private Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what >= 10000) {
                handleSystemMessage(msg);
            }else {
                handleCompleteMessage(msg);
            }
        }
    };

    protected void handleCompleteMessage(Message msg){

    }

    protected void handleSystemMessage(Message msg){
        switch (msg.what){
            case INPUT_METHOD_SHOW: {
                InputMethodManager methodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                View view = getCurrentFocus();
                if(view == null){
                    view = getWindow().getDecorView();
                }
                methodManager.showSoftInput(view, 0);
                break;
            }
            case INPUT_METHOD_HIDE: {
                InputMethodManager methodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                View view = getCurrentFocus();
                if(view == null){
                    view = getWindow().getDecorView();
                }
                methodManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                break;
            }
        }
    }

    protected void sendMessage(int what, Object obj, int... args){
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        if(args != null) {
            if(args.length > 0){
                msg.arg1 = args[0];
                if(args.length > 1){
                    msg.arg2 = args[1];
                }
            }
        }
        mHandler.sendMessage(msg);
    }

    protected void sendMessageDelayed(int what, long delay, Object obj, int... args){
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        if(args != null) {
            if(args.length > 0){
                msg.arg1 = args[0];
                if(args.length > 1){
                    msg.arg2 = args[1];
                }
            }
        }
        mHandler.sendMessageDelayed(msg, delay);
    }

    protected void removeMessage(int what){
        mHandler.removeMessages(what);
    }

    public final void showInputMethod(){
        sendMessageDelayed(INPUT_METHOD_SHOW, 200, null);
    }

    public final void hideInputMethod(){
        sendMessageDelayed(INPUT_METHOD_HIDE, 200, null);
    }

}
