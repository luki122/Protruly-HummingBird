package com.hb.thememanager.receiver;

import com.hb.thememanager.model.User;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class LoginOutReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
    	User.getInstance(context).LogOut();
    }
}
