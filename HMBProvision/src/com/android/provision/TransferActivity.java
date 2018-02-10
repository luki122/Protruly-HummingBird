package com.android.provision;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by xiaobin on 17-6-29.
 */

public class TransferActivity extends Activity {

    public static final String TAG = "TransferActivity";

    public static final String FROM = "from";

    public static final String LANGUAGE_SETTING = "language_setting";
    public static final String INFO = "info";
    public static final String SETTING = "setting";
    public static final String ACCOUNT = "account";
    public static final String THEME = "theme";

    private String from;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        from = getIntent().getStringExtra(FROM);
        Log.i(TAG, "TransferActivity from:" + from);

        Intent next = new Intent();

        if (!TextUtils.isEmpty(from)) {
            if (from.equals(LANGUAGE_SETTING)) {
                next.setClass(this, InfoActivity.class);
                startActivity(next);
                finish();
            } else if (from.equals(INFO)) {
                next.setAction(Settings.ACTION_WIFI_SETTINGS);
                next.putExtra("isGuide", true);
                startActivity(next);
                finish();
            } else if (from.equals(SETTING)) {
                jumpLogin();
            } else if (from.equals(ACCOUNT)) {
                next.setAction("com.hb.thememanager.ACTION_SETUP_THEME");
                startActivity(next);
                finish();
            } else if (from.equals(THEME)) {
                complete();
            } else {
                next.setClass(this, FinishActivity.class);
                startActivity(next);
                finish();
            }
        } else {
            next.setClass(this, FinishActivity.class);
            startActivity(next);
            finish();
        }
    }


    private void jumpLogin() {
        int type = 2;   //type = 0,系统账号正常登录  type = 1, 三方应用调用登录   type = 2, 手机首次开机设置账号
        AccountManager accountManager = AccountManager.get(this);
        Bundle bundle = new Bundle();
        bundle.putInt("fromSettingType", type);
        accountManager.addAccount("com.protruly.AccountType", null, null, bundle, this, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> amfuture) {
                try {
                    Bundle bundle = amfuture.getResult();
                    String phone  = bundle.getString(AccountManager.KEY_ACCOUNT_NAME);
                    String psw = bundle.getString(AccountManager.KEY_PASSWORD);
                    Log.i(TAG, "AccountManagerCallback phone:" + phone);
                    Log.i(TAG, "AccountManagerCallback psw:" + psw);
                } catch (Exception e) {
                    Log.i(TAG, "AccountManagerCallback e:" + e.getMessage());
                }

            }
        }, null);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "TransferActivity onRestart() from:" + from);

        if (!TextUtils.isEmpty(from) && from.equals(SETTING)) {
            Intent intent = new Intent("com.android.settings.GUIDE_RETURN");
            intent.putExtra("isGuide", true);
            startActivity(intent);

            finish();
        }
    }

    private final long KILL_DELAY = 2000L;

    private void complete() {

        Utils.setHasFinish(this, true);

        // Add a persistent setting to allow other apps to know the device has been provisioned.
        Settings.Global.putInt(getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 1);
        Settings.Secure.putInt(getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 1);

        // remove this activity from the package manager.
        PackageManager pm = getPackageManager();
        ComponentName name = new ComponentName(this, LanguageSettingsWizard.class);
        pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        // 禁用关机广播
        ComponentName shutdown = new ComponentName(this, ShutdownReceiver.class);
        pm.setComponentEnabledSetting(shutdown, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);


        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage("com.android.dlauncher");
        startActivity(intent);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                am.forceStopPackage(getPackageName());
            }
        }, KILL_DELAY);

    }

}
