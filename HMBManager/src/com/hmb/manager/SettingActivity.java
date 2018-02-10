package com.hmb.manager;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.hmb.manager.R;
import com.hmb.manager.qscaner.QScannerSettingActivity;
import com.hmb.manager.update.HMBUpdateManager;
import com.hmb.manager.utils.DateUtils;
import com.hmb.manager.utils.SPUtils;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;

import hb.app.dialog.AlertDialog;
import hb.app.dialog.ProgressDialog;
import hb.preference.Preference;
import hb.preference.PreferenceActivity;
import hb.preference.PreferenceScreen;
import hb.preference.SwitchPreference;

import static com.hmb.manager.update.HMBUpdateManager.RESULT_CODE_CHECK_MOBILE_DATA_CONNECTED;
import static com.hmb.manager.update.HMBUpdateManager.RESULT_CODE_GET_UPDATE;
import static com.hmb.manager.update.HMBUpdateManager.RESULT_CODE_NETWORK_ERROR;
import static com.hmb.manager.update.HMBUpdateManager.RESULT_CODE_NO_NETWORK_CONNECTED;
import static com.hmb.manager.update.HMBUpdateManager.RESULT_CODE_NO_UPDATE;
import static com.hmb.manager.update.HMBUpdateManager.RESULT_CODE_UPDATE_FINISHED;
import static com.hmb.manager.update.HMBUpdateManager.RESULT_CODE_UPDATE_MOBILE_DATA_CONNECTED;
import static com.hmb.manager.update.HMBUpdateManager.UPDATE_ACTION_CHECK;
import static com.hmb.manager.update.HMBUpdateManager.UPDATE_ACTION_UPDATE;


public class SettingActivity extends PreferenceActivity implements
        Preference.OnPreferenceClickListener{
    private static final String TAG = "SettingActivity";

    private static final String KEY_PRIVACY = "preference_privacy";
    private static final String KEY_SAFETY = "preference_safety";
    private static final String KEY_USAGE = "preference_usage";
    private static final String KEY_AUTO_UPDATE_SWITCH = "wlan_switch";
    private static final String KEY_UPDATE_DATABASE = "preference_update_database";

    private PreferenceScreen mPrivacyPreference;
    private PreferenceScreen mSafetyPreference;
    private PreferenceScreen mUsagePreference;
    private PreferenceScreen mUpdatePreference;
    private SwitchPreference mAutoUpdateSwitchPreference;

    private SPUtils SpInstance;
    private static MsgHandle mHandler;
    private AlertDialog mAlertDialog;
    private ProgressDialog mProgressDialog;
    private HMBUpdateManager hmbUpdateManager;
    private Context mContext;

    private static final int MSG_UPDATE_RESULT_CODE = 0x100;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.activity_settings);

        mContext = this.getApplicationContext();

        initView();
        initData();
    }

    private void initView () {
        SpInstance = SPUtils.instance(this);
        mPrivacyPreference = (PreferenceScreen) findPreference(KEY_PRIVACY);
        mSafetyPreference = (PreferenceScreen) findPreference(KEY_SAFETY);
        mUsagePreference = (PreferenceScreen) findPreference(KEY_USAGE);
        mAutoUpdateSwitchPreference = (SwitchPreference) findPreference(KEY_AUTO_UPDATE_SWITCH);
        mUpdatePreference = (PreferenceScreen) findPreference(KEY_UPDATE_DATABASE);
        mUpdatePreference.showStatusArrow(false);

        mAutoUpdateSwitchPreference.setChecked(SpInstance
                .getBooleanValue(Constant.SHARED_PREFERENCES_KEY_AUTO_UPDATE_DATABASE, true));

        mPrivacyPreference.setOnPreferenceClickListener(this);
        mSafetyPreference.setOnPreferenceClickListener(this);
        mUsagePreference.setOnPreferenceClickListener(this);
        mAutoUpdateSwitchPreference.setOnPreferenceClickListener(this);
        mUpdatePreference.setOnPreferenceClickListener(this);
    }

    private void initData() {
        updateTime();
        hmbUpdateManager = HMBUpdateManager.getInstance(this);
        mHandler = new MsgHandle(new WeakReference<SettingActivity>(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        hmbUpdateManager.attachActivity(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        hmbUpdateManager.attachActivity(null);
        mHandler.removeMessages(MSG_UPDATE_RESULT_CODE);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        switch (key) {
            case KEY_PRIVACY:
                Intent intentPrivacy = new Intent();
                intentPrivacy.setAction("com.hb.reject.setting");
                startActivity(intentPrivacy);
                break;
            case KEY_SAFETY:
                startActivity(new Intent(this, QScannerSettingActivity.class));
                break;
            case KEY_USAGE:
                Intent intentUsage = new Intent();
                intentUsage.setAction("com.hb.netmanage.simset.action");
                startActivity(intentUsage);
                break;
            case KEY_AUTO_UPDATE_SWITCH:
                SpInstance.setBooleanValue(Constant.SHARED_PREFERENCES_KEY_AUTO_UPDATE_DATABASE,
                        mAutoUpdateSwitchPreference.isChecked());
                break;
            case KEY_UPDATE_DATABASE:
                startCheck();
                break;
        }
        return false;
    }

    private void updateTime() {
        long updateTime = SpInstance.getLongValue(Constant.SHARED_PREFERENCES_LAST_UPDATE_TIME, 0);
        Log.d(TAG, "updateTime() -> updateTime = " + updateTime);
        if (updateTime > 0) {
            mUpdatePreference.setStatus(DateUtils.HMBManagerDate(mContext, updateTime));
        }
    }

    private void startCheck() {
        showProgressDialog(getString(R.string.app_setting_check_database));
        hmbUpdateManager.scheduleUpdate(UPDATE_ACTION_CHECK, false);
    }

    private void startUpdate() {
        showProgressDialog(getString(R.string.app_setting_updating_database));
        hmbUpdateManager.scheduleUpdate(UPDATE_ACTION_UPDATE, false);
    }

    public void showStatusProgressDialog(int status) {
        if (status == UPDATE_ACTION_CHECK) {
            showProgressDialog(getString(R.string.app_setting_check_database));
        } else if (status == UPDATE_ACTION_UPDATE) {
            showProgressDialog(getString(R.string.app_setting_updating_database));
        }
    }

    private void showProgressDialog(String message) {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mProgressDialog  = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setTitle(message);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private void showNoNetworkConnectedWarning() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
        mAlertDialog = new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.app_setting_update_no_network))
                .setMessage(getResources().getString(R.string.app_setting_update_no_network_tips))
                .setNegativeButton(getResources().getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mAlertDialog.dismiss();
                            }
                        })
                .setPositiveButton(getResources().getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        }).setCancelable(false).create();
        mAlertDialog.show();
    }

    public void showCheckMobileDataConnectedWarning() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
        mAlertDialog = new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.app_setting_dialog_tips))
                .setMessage(getResources().getString(R.string.app_setting_update_mobile_network_tips))
                .setNegativeButton(getResources().getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mAlertDialog.dismiss();
                            }
                        })
                .setPositiveButton(getResources().getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                hmbUpdateManager.scheduleUpdate(UPDATE_ACTION_CHECK, true);
                            }
                        }).setCancelable(false).create();
        mAlertDialog.show();
    }

    public void showUpdateMobileDataConnectedWarning() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
        mAlertDialog = new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.app_setting_dialog_tips))
                .setMessage(getResources().getString(R.string.app_setting_update_mobile_network_tips))
                .setNegativeButton(getResources().getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mAlertDialog.dismiss();
                            }
                        })
                .setPositiveButton(getResources().getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                hmbUpdateManager.scheduleUpdate(UPDATE_ACTION_UPDATE, true);
                            }
                        }).setCancelable(false).create();
        mAlertDialog.show();
    }

    public void showNewVersionWarning() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
        mAlertDialog = new AlertDialog.Builder(this)
//                .setTitle(getResources().getString(R.string.app_setting_update_get_new_version))
                .setMessage(getResources().getString(R.string.app_setting_update_get_new_version))
                .setNegativeButton(getResources().getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mAlertDialog.dismiss();
                            }
                        })
                .setPositiveButton(getResources().getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startUpdate();
                            }
                        }).setCancelable(false).create();
        mAlertDialog.show();
    }

    private void showNetWorkError() {
        Toast.makeText(this, getResources().getString(R.string.app_setting_update_network_error),
                Toast.LENGTH_SHORT).show();
    }

    private void showNoNewVersion() {
        Toast.makeText(this, getResources().getString(R.string.app_setting_update_no_update),
                Toast.LENGTH_SHORT).show();
    }

    private void showUpdateFinished() {
        updateTime();
        Toast.makeText(this, getResources().getString(R.string.app_setting_update_finished),
                Toast.LENGTH_SHORT).show();
    }

    private static class MsgHandle extends Handler {
        WeakReference<SettingActivity> activityRef;
        MsgHandle(WeakReference<SettingActivity> ref){
            activityRef = ref;
        }
        @Override
        public void handleMessage(Message msg) {
            SettingActivity activity = activityRef.get();
            if (activity == null || msg.what != MSG_UPDATE_RESULT_CODE) {
                return;
            }
            activity.dismissProgressDialog();
            Log.d(TAG, "handleMessage()-> msg.arg1 = " + msg.arg1);
            switch (msg.arg1) {
                case RESULT_CODE_NO_NETWORK_CONNECTED:
                    activity.showNoNetworkConnectedWarning();
                    break;
                case RESULT_CODE_CHECK_MOBILE_DATA_CONNECTED:
                    activity.showCheckMobileDataConnectedWarning();
                    break;
                case RESULT_CODE_UPDATE_MOBILE_DATA_CONNECTED:
                    activity.showUpdateMobileDataConnectedWarning();
                    break;
                case RESULT_CODE_NETWORK_ERROR:
                    activity.showNetWorkError();
                    break;
                case RESULT_CODE_NO_UPDATE:
                    activity.showNoNewVersion();
                    break;
                case RESULT_CODE_GET_UPDATE:
                    activity.showNewVersionWarning();
                    break;
                case RESULT_CODE_UPDATE_FINISHED:
                    activity.showUpdateFinished();
                    break;
                default:
                    break;
            }
        }
    }

    public void updateResult(int resultCode) {
        Log.d(TAG, "updateResult() -> resultCode = " + resultCode);
        mHandler.removeMessages(MSG_UPDATE_RESULT_CODE);
        Message message = mHandler.obtainMessage(MSG_UPDATE_RESULT_CODE, resultCode, 0);
        mHandler.sendMessage(message);
    }
}