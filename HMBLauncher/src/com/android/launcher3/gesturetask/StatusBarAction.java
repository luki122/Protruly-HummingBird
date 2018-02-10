package com.android.launcher3.gesturetask;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Created by antino on 17-3-6.
 */
public class StatusBarAction {
    public static void collapsingNotification(Context context) {
        Object service = context.getSystemService("statusbar");
        if (null == service)
            return;
        try {
            Class<?> clazz = Class.forName("android.app.StatusBarManager");
            int sdkVersion = android.os.Build.VERSION.SDK_INT;
            Method collapse = null;
            if (sdkVersion <= 16) {
                collapse = clazz.getMethod("collapse");
            } else {
                collapse = clazz.getMethod("collapsePanels");
            }
            collapse.setAccessible(true);
            collapse.invoke(service);
        } catch (Exception e) {
            Log.e("StatusBarAction","Fail collapse statusbar.",e);
        }
    }
}
