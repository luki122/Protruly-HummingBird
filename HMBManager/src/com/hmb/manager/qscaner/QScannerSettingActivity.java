package com.hmb.manager.qscaner;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hmb.manager.R;
import com.hmb.manager.utils.SPUtils;

import hb.app.HbActivity;
import hb.app.dialog.AlertDialog;
import hb.widget.Switch;


public class QScannerSettingActivity extends HbActivity implements View.OnClickListener {
    private static final String TAG = "QScannerSettingActivity";

    public static int QUICK_SCAN_MODE = 0;
    public static int FULL_SCAN_MODE = 1;

    public static final String SP_KEY_SCAN_MODE = "scan_mode";
    public static final String SP_KEY_URL_MONITOR = "url_monitor";

    private AlertDialog mDialog;
    private TextView mTvScanMode;
    private Switch mUriMonitorSwitch;
    private LinearLayout mScanModeLayout;

    private int mScanMode;
    private SPUtils SpInstance;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHbContentView(R.layout.activity_qscanner_settings);
        initView();
    }

    private void initView() {
        getToolbar().setTitle(R.string.safety_settings);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mTvScanMode = (TextView) findViewById(R.id.tv_scan_mode);
        mUriMonitorSwitch = (Switch) findViewById(R.id.switch_url_monitor);
        mScanModeLayout = (LinearLayout) findViewById(R.id.layout_scan_mode);

        mUriMonitorSwitch.setOnClickListener(this);
        mScanModeLayout.setOnClickListener(this);

        SpInstance = SPUtils.instance(this);
        mScanMode = SpInstance.getIntValue(SP_KEY_SCAN_MODE, QUICK_SCAN_MODE);
        if (mScanMode == QUICK_SCAN_MODE) {
            mTvScanMode.setText(getString(R.string.safety_quick_scan_mode));
        } else {
            mTvScanMode.setText(getString(R.string.safety_full_scan_mode));
        }
        mUriMonitorSwitch.setChecked(SpInstance.getBooleanValue(SP_KEY_URL_MONITOR, true));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.switch_url_monitor:
                SpInstance.setBooleanValue(SP_KEY_URL_MONITOR, mUriMonitorSwitch.isChecked());
                break;
            case R.id.layout_scan_mode:
                showScanModeDialog();
                break;
        }
    }

    private void showScanModeDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        mDialog = new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.safety_dialog_scan_mode_title))
                .setMessage(getResources().getString(R.string.safety_dialog_scan_mode_message))
                .setNegativeButton(getResources().getString(R.string.safety_full_scan_mode),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SpInstance.setIntValue(SP_KEY_SCAN_MODE, mScanMode, FULL_SCAN_MODE);
                                mScanMode = QUICK_SCAN_MODE;
                                mTvScanMode.setText(getString(R.string.safety_full_scan_mode));
                                Log.d(TAG, "setNegativeButton");
                            }
                        })
                .setPositiveButton(getResources().getString(R.string.safety_quick_scan_mode),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SpInstance.setIntValue(SP_KEY_SCAN_MODE, mScanMode, QUICK_SCAN_MODE);
                                mTvScanMode.setText(getString(R.string.safety_quick_scan_mode));
                                Log.d(TAG, "setPositiveButton");
                            }
                        }).setCancelable(true).create();
        mDialog.show();
        Button negativeBtn = mDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        if (negativeBtn != null) {
            negativeBtn.setBackgroundResource(com.hb.R.drawable.button_background_hb_positive);
        }
    }
}
