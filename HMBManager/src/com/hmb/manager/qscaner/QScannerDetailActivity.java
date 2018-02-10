package com.hmb.manager.qscaner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.hmb.manager.R;
import com.hmb.manager.bean.AppInfo;
import com.hmb.manager.qscaner.perference.AppHorizontalPreference;
import com.hmb.manager.qscaner.perference.FileHorizontalPreference;
import com.hmb.manager.qscaner.bean.RiskEntity;
import com.hmb.manager.qscaner.provider.QScannerRiskProvider;
import com.hmb.manager.utils.FileUtils;
import com.hmb.manager.utils.ManagerUtils;

import java.util.ArrayList;

import hb.preference.PreferenceActivity;
import hb.preference.PreferenceCategory;
import tmsdk.common.module.qscanner.QScanConfig;


public class QScannerDetailActivity extends PreferenceActivity {
    private static final String TAG = "QScannerDetailActivity";

    private ArrayList<String> mRiskAppList;
    private ArrayList<String> mRiskFileList;

    public PreferenceCategory mAppParent;
    public PreferenceCategory mFileParent;

    private int mRiskType;
    private int mHandleRisk = 0;

    private static final String KEY_APP_PARENT = "app_parent";
    private static final String KEY_FILE_PARENT = "file_parent";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.activity_qscanner_detail);

        initView();
        initData();
    }

    private void initView() {
        Intent intent = getIntent();
        mRiskType = intent.getIntExtra("TYPE", QScanConfig.RET_SAFE);
        mRiskAppList = intent.getStringArrayListExtra("APP");
        mRiskFileList = intent.getStringArrayListExtra("FILE");
        mAppParent = (PreferenceCategory) findPreference(KEY_APP_PARENT);
        mFileParent = (PreferenceCategory) findPreference(KEY_FILE_PARENT);
        String title = "";
        switch (mRiskType) {
            case QScanConfig.RET_NOT_OFFICIAL:
                title = getString(R.string.safety_not_official);
                mAppParent.setTitle(getString(R.string.safety_not_official_app_summary));
                mFileParent.setTitle(getString(R.string.safety_not_official_file_summary));
                break;
            case QScanConfig.RET_PAY_RISKS:
                title = getString(R.string.safety_pay_risks);
                mAppParent.setTitle(getString(R.string.safety_pay_risks_app_summary));
                mFileParent.setTitle(getString(R.string.safety_pay_risks_file_summary));
                break;
            case QScanConfig.RET_VIRUSES:
                title = getString(R.string.safety_viruses);
                mAppParent.setTitle(getString(R.string.safety_viruses_app_summary));
                mFileParent.setTitle(getString(R.string.safety_viruses_file_summary));
                break;
            case QScanConfig.RET_STEALACCOUNT_RISKS:
                title = getString(R.string.safety_stealaccount_risks);
                mAppParent.setTitle(getString(R.string.safety_tealaccount_risks_app_summary));
                mFileParent.setTitle(getString(R.string.safety_tealaccount_risks_file_summary));
                break;
            case QScanConfig.RET_OTHER_RISKS:
                title = getString(R.string.safety_other_risks);
                mAppParent.setTitle(getString(R.string.safety_other_risks_app_summary));
                mFileParent.setTitle(getString(R.string.safety_other_risks_file_summary));
                break;
            default:
                break;
        }
        getToolbar().setTitle(title);
    }

    private void initData() {
        fillAppSection();
        fillFileSection();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");
        registerReceiver(mUninstallAppReceiver, intentFilter);
    }

    private void fillAppSection() {
        if (mRiskAppList.size() == 0) {
            getPreferenceScreen().removePreference(mAppParent);
        } else {
            for (int i = 0; i < mRiskAppList.size(); i++) {
                addAppHorizontalPreference(mAppParent, mRiskAppList.get(i));
            }
        }
    }

    private void fillFileSection() {
        if (mRiskFileList.size() == 0) {
            getPreferenceScreen().removePreference(mFileParent);
        } else {
            for (int i = 0; i < mRiskFileList.size(); i++) {
                addFileHorizontalPreference(mFileParent, mRiskFileList.get(i));
            }
        }
    }

    private void addAppHorizontalPreference(PreferenceCategory parent, String packageName) {
        RiskEntity riskEntity = QScannerRiskProvider
                .getRiskEntity(this, RiskEntity.RISK_TYPE_APP, packageName);
        if (riskEntity != null) {
            AppInfo appInfo = ManagerUtils.getAppInfoByPackageName(this, packageName);
            if (appInfo != null) {
                AppHorizontalPreference pref = new AppHorizontalPreference(this, appInfo.getAppIcon(),
                        appInfo.getPkgName(), appInfo.getAppLabel());
                parent.addPreference(pref);
            }
        }
    }

    private void addFileHorizontalPreference(PreferenceCategory parent, String path) {
        RiskEntity riskEntity = QScannerRiskProvider
                .getRiskEntity(this, RiskEntity.RISK_TYPE_APK, path);
        if (riskEntity != null) {
            AppInfo appInfo = ManagerUtils.getApkInfoByAbsPath(this, riskEntity.path);
            FileHorizontalPreference pref = new FileHorizontalPreference(this, appInfo.getAppIcon(),
                    appInfo.getAppLabel(), appInfo.getPkgName(), riskEntity.path,
                    FileUtils.getFileOrFilesSize(riskEntity.path));
            parent.addPreference(pref);
        }
    }

    public void removeFileHorizontalPreference(FileHorizontalPreference preference) {
        mHandleRisk++;
        if (preference != null) {
            mFileParent.removePreference(preference);
            mRiskFileList.remove(preference.mFilePath);
            QScannerRiskProvider.removeRiskEntity(this, RiskEntity.RISK_TYPE_APK,
                    preference.mFilePath);
        }
        if (mFileParent.getPreferenceCount() == 0) {
            getPreferenceScreen().removePreference(mFileParent);
            if (mAppParent.getPreferenceCount() == 0) {
                onBackPressed();
            }
        }
    }

    public void removeAppHorizontalPreference(AppHorizontalPreference preference) {
        mHandleRisk++;
        if (preference != null) {
            mAppParent.removePreference(preference);
            mRiskAppList.remove(preference.mPackageName);
            QScannerRiskProvider.removeRiskEntity(this, RiskEntity.RISK_TYPE_APP,
                    preference.mPackageName);
        }
        if (mAppParent.getPreferenceCount() == 0) {
            getPreferenceScreen().removePreference(mAppParent);
            if (mFileParent.getPreferenceCount() == 0) {
                onBackPressed();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUninstallAppReceiver);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed() -> mHandleRisk = " + mHandleRisk);
        Intent intent = new Intent();
        intent.putStringArrayListExtra("APP", mRiskAppList);
        intent.putStringArrayListExtra("FILE", mRiskFileList);
        setResult(mHandleRisk, intent);
        finish();
    }

    private BroadcastReceiver mUninstallAppReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getData() == null) {
            return;
        }

        if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
            String packageName = intent.getData().getSchemeSpecificPart();
            Log.d(TAG, "onReceive() -> packageName = " + packageName);
            for (int i = 0; i < mAppParent.getPreferenceCount(); i++) {
                AppHorizontalPreference preference =
                        (AppHorizontalPreference) mAppParent.getPreference(i);
                if (preference.mPackageName.equals(packageName)) {
                    removeAppHorizontalPreference(preference);
                }
            }
        }
        }
    };
}