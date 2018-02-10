package com.android.settings;


import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.hb.themeicon.theme.IconManager;


/**
 * Created by liuqin on 17-6-30.
 *
 * @date Liuqin on 2017-06-30
 */
public class SettingsApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        new ClearIconCacheReceiver().registerReceiver(getApplicationContext());
    }

    private static class ClearIconCacheReceiver extends BroadcastReceiver {
        private final String ACTION_CLEAR_ICON_CACHE = "com.hb.theme.ACTION_THEME_CHANGE";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_CLEAR_ICON_CACHE.equals(action)) {
                IconManager.clearCaches();
            }
        }

        private void registerReceiver(Context context) {
            IntentFilter filter = new IntentFilter(ACTION_CLEAR_ICON_CACHE);
            context.registerReceiver(this, filter);
        }

        private void unRegisterReceiver(Context context) {
            context.unregisterReceiver(this);
        }
    }
}
