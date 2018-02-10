package com.android.settings.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.accessibility.AccessibilityEvent;
import android.view.WindowManager.LayoutParams;

import com.android.internal.app.AlertActivity;
import com.hb.internal.app.AlertController;

/**
 * Created by liuqin on 17-6-13.
 *
 * @date Liuqin on 2017-06-13
 */
public class AlertActivityCompat extends AlertActivity{
    /**
     * The model for the alert.
     *
     * @see #mAlertParams
     */
    protected AlertController mAlert;

    /**
     * The parameters for the alert.
     */
    protected AlertController.AlertParams mAlertParams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window winow = this.getWindow();
        LayoutParams params = winow.getAttributes();
        params.dimAmount = 0.6F;
        params.gravity = 80;
        winow.setAttributes(params);

        mAlert = new AlertController(this, this, getWindow(), true);
        mAlertParams = new AlertController.AlertParams(this);
        this.mAlertParams.setHbTheme(true);
        if (isSystemApp(this)) {
            this.mAlert.setHbButton(true);
        }
    }

    private boolean isSystemApp(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return ((pInfo.applicationInfo.flags & 1) == 0 && (pInfo.applicationInfo.flags & 128) == 0) ? false : true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void cancel() {
        finish();
    }

    public void dismiss() {
        // This is called after the click, since we finish when handling the
        // click, don't do that again here.
        if (!isFinishing()) {
            finish();
        }
    }

    protected boolean isDefineCloseAnimation(){
        return true;
    }

    @Override
    public void finish() {
        super.finish();
        if(isDefineCloseAnimation()) {
            overridePendingTransition(0, 0);
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        event.setClassName(Dialog.class.getName());
        event.setPackageName(getPackageName());

        ViewGroup.LayoutParams params = getWindow().getAttributes();
        boolean isFullScreen = (params.width == ViewGroup.LayoutParams.MATCH_PARENT) &&
                (params.height == ViewGroup.LayoutParams.MATCH_PARENT);
        event.setFullScreen(isFullScreen);

        return false;
    }

    /**
     * Sets up the alert, including applying the parameters to the alert model,
     * and installing the alert's content.
     *
     * @see #mAlert
     * @see #mAlertParams
     */
    protected void setupAlert() {
        mAlertParams.apply(mAlert);
        mAlert.installContent();
        getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() & -9);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mAlert.onKeyDown(keyCode, event)) return true;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mAlert.onKeyUp(keyCode, event)) return true;
        return super.onKeyUp(keyCode, event);
    }
}
