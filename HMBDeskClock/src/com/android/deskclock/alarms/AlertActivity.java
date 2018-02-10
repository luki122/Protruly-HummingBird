package com.android.deskclock.alarms;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import com.hb.internal.app.AlertController;

import hb.app.HbActivity;

/**
 * Created by yubai on 17-4-24.
 */

public abstract class AlertActivity extends HbActivity implements DialogInterface {
    protected AlertController mAlert;
    protected AlertController.AlertParams mAlertParams;

    public AlertActivity() {
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mAlert = new AlertController(this, this, this.getWindow());
        this.mAlertParams = new AlertController.AlertParams(this);
    }

    public void cancel() {
        this.finish();
    }

    public void dismiss() {
        if(!this.isFinishing()) {
            this.finish();
        }

    }

    protected boolean isDefineCloseAnimation() {
        return true;
    }

    public void finish() {
        super.finish();
        if(this.isDefineCloseAnimation()) {
            this.overridePendingTransition(0, 0);
        }

    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        event.setClassName(Dialog.class.getName());
        event.setPackageName(this.getPackageName());
        WindowManager.LayoutParams params = this.getWindow().getAttributes();
        boolean isFullScreen = params.width == -1 && params.height == -1;
        event.setFullScreen(isFullScreen);
        return false;
    }

    protected void setupAlert() {
        this.mAlertParams.apply(this.mAlert);
        this.mAlert.installContent();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return this.mAlert.onKeyDown(keyCode, event)?true:super.onKeyDown(keyCode, event);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return this.mAlert.onKeyUp(keyCode, event)?true:super.onKeyUp(keyCode, event);
    }
}
