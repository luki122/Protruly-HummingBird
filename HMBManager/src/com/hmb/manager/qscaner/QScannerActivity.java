package com.hmb.manager.qscaner;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;

import com.hmb.manager.Constant;
import com.hmb.manager.R;
import com.hmb.manager.qscaner.bean.RiskEntity;
import com.hmb.manager.qscaner.perference.ScannerPanelPreference;
import com.hmb.manager.qscaner.provider.QScannerRiskProvider;
import com.hmb.manager.utils.SPUtils;

import java.util.ArrayList;
import java.util.List;

import hb.preference.Preference;
import hb.preference.PreferenceActivity;
import hb.preference.PreferenceScreen;
import hb.widget.toolbar.Toolbar.OnMenuItemClickListener;
import tmsdk.common.module.qscanner.QScanConfig;
import tmsdk.common.module.qscanner.QScanListener;
import tmsdk.common.module.qscanner.QScanResultEntity;
import tmsdk.common.module.qscanner.QScannerManagerV2;
import tmsdk.fg.creator.ManagerCreatorF;

public class QScannerActivity extends PreferenceActivity
        implements OnMenuItemClickListener, Preference.OnPreferenceClickListener {
    private static final String TAG = "QScannerActivity";

    private static final String KEY_SCANNER_PANEL = "preference_scanner_panel";
    private static final String KEY_NOT_OFFICIAL = "preference_not_official";
    private static final String KEY_VIRUSES = "preference_viruses";
    private static final String KEY_PAY_RISKS = "preference_pay_risks";
    private static final String KEY_STEALACCOUNT_RISKS = "preference_stealaccount_risks";
    private static final String KEY_OTHER_RISKS = "preference_other_risks";
//    private static final String KEY_PLUGINS = "preference_plugins";

    private ScannerPanelPreference mScannerPanelPreference;
    private PreferenceScreen mNotOfficialPreference;
    private PreferenceScreen mVirusesPreference;
    private PreferenceScreen mPayRisksPreference;
    private PreferenceScreen mStealAccountRisksPreference;
    private PreferenceScreen mOtherRisksPreference;
//    private PreferenceScreen mPluginsPreference;

    private int mNotOfficial = 0;
    private int mViruses = 0;
    private int mPayRisks = 0;
    private int mStealAccountRisks = 0;
    private int mOtherRisk = 0;
    private int mPlugin = 0;

    private ArrayList<String> mNotOfficialFileList = new ArrayList<>();
    private ArrayList<String> mNotOfficialAppList = new ArrayList<>();
    private ArrayList<String> mVirusesFileList = new ArrayList<>();
    private ArrayList<String> mVirusesAppList = new ArrayList<>();
    private ArrayList<String> mPayRisksFileList = new ArrayList<>();
    private ArrayList<String> mPayRisksAppList = new ArrayList<>();
    private ArrayList<String> mStealAccountRisksFileList = new ArrayList<>();
    private ArrayList<String> mStealAccountRisksAppList = new ArrayList<>();
    private ArrayList<String> mOtherRiskFileList = new ArrayList<>();
    private ArrayList<String> mOtherRiskAppList = new ArrayList<>();

    private int mScanMode;
    private String mScanDetailInfo;
    private Handler mMainJobHandler = null;
    private Handler mUpdateUIHandler = null;
    private HandlerThread mMainJobThread = null;
    private QScannerManagerV2 mQScannerManager;

    private static final int MSG_FREE = 0x100;
    private static final int MSG_START_SCAN_INSTALLED_PACKAGE = 0x101;
    private static final int MSG_START_SCAN_UNINSTALL_PACKAGE = 0x102;

    private static final int MSG_UI_START_SCAN = 0x201;
    private static final int MSG_UI_UPDATE_SCAN_DETAIL = 0x202;
    private static final int MSG_UI_UPDATE_SCAN_RESULT = 0x203;

    public static final int REQUEST_NOTOFFICIAL = 0x300;
    public static final int REQUEST_VIRUSES = 0x301;
    public static final int REQUEST_PAYRISKS = 0x303;
    public static final int REQUEST_STEALACCOUNTRISKS = 0x304;
    public static final int REQUEST_OTHERRISK = 0x305;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.activity_qscanner);

        initView();
        initData();
    }

    private void initView() {
        getToolbar().inflateMenu(R.menu.qs_toolbar_menu);
        getToolbar().setOnMenuItemClickListener(this);

        mScanMode = SPUtils.instance(this).getIntValue(QScannerSettingActivity.SP_KEY_SCAN_MODE,
                QScannerSettingActivity.QUICK_SCAN_MODE);

        mScannerPanelPreference = (ScannerPanelPreference) findPreference(KEY_SCANNER_PANEL);
        mNotOfficialPreference = (PreferenceScreen) findPreference(KEY_NOT_OFFICIAL);
        mVirusesPreference = (PreferenceScreen) findPreference(KEY_VIRUSES);
        mPayRisksPreference = (PreferenceScreen) findPreference(KEY_PAY_RISKS);
        mStealAccountRisksPreference = (PreferenceScreen) findPreference(KEY_STEALACCOUNT_RISKS);
        mOtherRisksPreference = (PreferenceScreen) findPreference(KEY_OTHER_RISKS);
//        mPluginsPreference = (PreferenceScreen) findPreference(KEY_PLUGINS);
        mNotOfficialPreference.setLayoutResource(com.hb.R.layout.preference_material_hb);
        mVirusesPreference.setLayoutResource(com.hb.R.layout.preference_material_hb);
        mPayRisksPreference.setLayoutResource(com.hb.R.layout.preference_material_hb);
        mStealAccountRisksPreference.setLayoutResource(com.hb.R.layout.preference_material_hb);
        mOtherRisksPreference.setLayoutResource(com.hb.R.layout.preference_material_hb);
        

        mScannerPanelPreference.setScanMode(mScanMode);
        mVirusesPreference.setOnPreferenceClickListener(this);
        mOtherRisksPreference.setOnPreferenceClickListener(this);
        mPayRisksPreference.setOnPreferenceClickListener(this);
        mStealAccountRisksPreference.setOnPreferenceClickListener(this);
        mNotOfficialPreference.setOnPreferenceClickListener(this);
//        mPluginsPreference.setOnPreferenceClickListener(this);
    }

    private void initData() {
        mQScannerManager = ManagerCreatorF.getManager(QScannerManagerV2.class);
        Log.v(TAG, "initData() -> VirusBaseVersion = " + mQScannerManager.getVirusBaseVersion());
        mMainJobThread = new HandlerThread("qscan");
        mMainJobThread.start();
        mMainJobHandler = new MainJobHandler(mMainJobThread.getLooper());
        mUpdateUIHandler = new UpdateUIHandler(this.getMainLooper());
        mMainJobHandler.sendEmptyMessage(MSG_START_SCAN_INSTALLED_PACKAGE);
    }
    
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(QScannerActivity.this, QScannerSettingActivity.class));
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        mMainJobHandler.sendEmptyMessage(MSG_FREE);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult() -> requestCode = " + requestCode + ", resultCode = " + resultCode);
        ArrayList<String> appList = data.getStringArrayListExtra("APP");
        ArrayList<String> fileList = data.getStringArrayListExtra("FILE");
        switch (requestCode) {
            case QScannerActivity.REQUEST_NOTOFFICIAL:
                mNotOfficialAppList = appList;
                mNotOfficialFileList = fileList;
                mNotOfficial = appList.size() + fileList.size();
                break;
            case QScannerActivity.REQUEST_VIRUSES:
                mVirusesAppList = appList;
                mVirusesFileList = fileList;
                mViruses = appList.size() + fileList.size();
                break;
            case QScannerActivity.REQUEST_PAYRISKS:
                mPayRisksAppList = appList;
                mPayRisksFileList = fileList;
                mPayRisks = appList.size() + fileList.size();
                break;
            case QScannerActivity.REQUEST_STEALACCOUNTRISKS:
                mStealAccountRisksAppList = appList;
                mStealAccountRisksFileList = fileList;
                mStealAccountRisks = appList.size() + fileList.size();
                break;
            case QScannerActivity.REQUEST_OTHERRISK:
                mOtherRiskAppList = appList;
                mOtherRiskFileList = fileList;
                mOtherRisk = appList.size() + fileList.size();
                break;
        }
        updateScanResult();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        Intent intent = new Intent(QScannerActivity.this, QScannerDetailActivity.class);
        switch (key) {
            case KEY_NOT_OFFICIAL:
                intent.putExtra("TYPE", QScanConfig.RET_NOT_OFFICIAL);
                intent.putStringArrayListExtra("APP", mNotOfficialAppList);
                intent.putStringArrayListExtra("FILE", mNotOfficialFileList);
                startActivityForResult(intent, REQUEST_NOTOFFICIAL);
                break;
            case KEY_OTHER_RISKS:
                intent.putExtra("TYPE", QScanConfig.RET_OTHER_RISKS);
                intent.putStringArrayListExtra("APP", mOtherRiskAppList);
                intent.putStringArrayListExtra("FILE", mOtherRiskFileList);
                startActivityForResult(intent, REQUEST_OTHERRISK);
                break;
            case KEY_PAY_RISKS:
                intent.putExtra("TYPE", QScanConfig.RET_PAY_RISKS);
                intent.putStringArrayListExtra("APP", mPayRisksAppList);
                intent.putStringArrayListExtra("FILE", mPayRisksFileList);
                startActivityForResult(intent, REQUEST_PAYRISKS);
                break;
            case KEY_STEALACCOUNT_RISKS:
                intent.putExtra("TYPE", QScanConfig.RET_STEALACCOUNT_RISKS);
                intent.putStringArrayListExtra("APP", mStealAccountRisksAppList);
                intent.putStringArrayListExtra("FILE", mStealAccountRisksFileList);
                startActivityForResult(intent, REQUEST_STEALACCOUNTRISKS);
                break;
            case KEY_VIRUSES:
                intent.putExtra("TYPE", QScanConfig.RET_VIRUSES);
                intent.putStringArrayListExtra("APP", mVirusesAppList);
                intent.putStringArrayListExtra("FILE", mVirusesFileList);
                startActivityForResult(intent, REQUEST_VIRUSES);
                break;
        }
        return false;
    }

    private class MainJobHandler extends Handler {

        public MainJobHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.e(TAG, "MainJobHandler() -> msg.what = " + msg.what);
            if (msg.what == MSG_START_SCAN_INSTALLED_PACKAGE) {
                int nRet = mQScannerManager.initScanner();
                if (nRet != QScanConfig.S_OK) {
                    Log.e(TAG, "initScanner() -> error = " + nRet);
                    finish();
                }
                Message msgUI = mUpdateUIHandler.obtainMessage(MSG_UI_START_SCAN, 0, 0);
                mUpdateUIHandler.sendMessage(msgUI);
                mQScannerManager.scanInstalledPackages(QScanConfig.SCAN_LOCAL
                                | QScanConfig.SCAN_CLOUD, null,
                        new QScanListenerUI(RiskEntity.RISK_TYPE_APP), QScanConfig.ERT_FAST, 0);
            } else if (msg.what == MSG_START_SCAN_UNINSTALL_PACKAGE) {
                if (mScanMode == QScannerSettingActivity.FULL_SCAN_MODE) {
                    mQScannerManager.scanUninstallApks(QScanConfig.SCAN_LOCAL
                                    | QScanConfig.SCAN_CLOUD, null,
                            new QScanListenerUI(RiskEntity.RISK_TYPE_APK), 0);
                } else {
                    // Fixme 快速扫描模式下传入扫描路径会报错
                    mQScannerManager.scanUninstallApks(QScanConfig.SCAN_LOCAL
                                    | QScanConfig.SCAN_CLOUD, null,
                            new QScanListenerUI(RiskEntity.RISK_TYPE_APK), 0);
                }
            } else if (msg.what == MSG_FREE) {
                mQScannerManager.cancelScan();
                mQScannerManager.freeScanner();
                mMainJobThread.quit();
            }
        }
    }

    private class UpdateUIHandler extends Handler {
        public UpdateUIHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_UI_START_SCAN) {
                updateStartScan();
            } else if (msg.what == MSG_UI_UPDATE_SCAN_DETAIL) {
                updateScanDetail();
            } else if (msg.what == MSG_UI_UPDATE_SCAN_RESULT) {
                updateScanResult();
            }
        }
    }

    private void updateStartScan() {
        mNotOfficialPreference.setStatus(getString(R.string.safety_scanning));
        mNotOfficialPreference.setEnabled(false);
        mNotOfficialPreference.showStatusArrow(false);

        mOtherRisksPreference.setStatus(getString(R.string.safety_scanning));
        mOtherRisksPreference.setEnabled(false);
        mOtherRisksPreference.showStatusArrow(false);

        mPayRisksPreference.setStatus(getString(R.string.safety_scanning));
        mPayRisksPreference.setEnabled(false);
        mPayRisksPreference.showStatusArrow(false);

        mVirusesPreference.setStatus(getString(R.string.safety_scanning));
        mVirusesPreference.setEnabled(false);
        mVirusesPreference.showStatusArrow(false);

        mStealAccountRisksPreference.setStatus(getString(R.string.safety_scanning));
        mStealAccountRisksPreference.setEnabled(false);
        mStealAccountRisksPreference.showStatusArrow(false);
    }

    private void updateScanDetail() {
        mScannerPanelPreference.updateScannerDetail(mScanDetailInfo);
    }

    private void updateScanResult() {
        int riskSum = mNotOfficial + mOtherRisk + mPayRisks + mViruses + mStealAccountRisks;
        Log.d(TAG, "updateScanResult() -> riskSum = " + riskSum + ", mNotOfficial = " + mNotOfficial
                + ", mOtherRisk = " + mOtherRisk + ", mPayRisks = " + mPayRisks
                + ", mViruses = " + mViruses + ", mStealAccountRisks = " + mStealAccountRisks);
        mScannerPanelPreference.updateScannerView(ScannerPanelPreference.DONE_STATUS, riskSum);
        if (mNotOfficial > 0) {
            mNotOfficialPreference.setEnabled(true);
            mNotOfficialPreference.showStatusArrow(true);
            mNotOfficialPreference.setStatusColor(Color.RED);
            mNotOfficialPreference.setStatus(mNotOfficial + "");
        } else {
            mNotOfficialPreference.setEnabled(false);
            mNotOfficialPreference.showStatusArrow(false);
            mNotOfficialPreference.setStatusColor(Color.parseColor("#61000000"));
            mNotOfficialPreference.setStatus(getString(R.string.safety_no_risk));
        }
        if (mOtherRisk > 0) {
            mOtherRisksPreference.setEnabled(true);
            mOtherRisksPreference.showStatusArrow(true);
            mOtherRisksPreference.setStatusColor(Color.RED);
            mOtherRisksPreference.setStatus(mOtherRisk + "");
        } else {
            mOtherRisksPreference.setEnabled(false);
            mOtherRisksPreference.showStatusArrow(false);
            mOtherRisksPreference.setStatusColor(Color.parseColor("#61000000"));
            mOtherRisksPreference.setStatus(getString(R.string.safety_no_risk));
        }
        if (mPayRisks > 0) {
            mPayRisksPreference.setEnabled(true);
            mPayRisksPreference.showStatusArrow(true);
            mPayRisksPreference.setStatusColor(Color.RED);
            mPayRisksPreference.setStatus(mPayRisks + "");
        } else {
            mPayRisksPreference.setEnabled(false);
            mPayRisksPreference.showStatusArrow(false);
            mPayRisksPreference.setStatusColor(Color.parseColor("#61000000"));
            mPayRisksPreference.setStatus(getString(R.string.safety_no_risk));
        }
        if (mViruses > 0) {
            mVirusesPreference.setEnabled(true);
            mVirusesPreference.showStatusArrow(true);
            mVirusesPreference.setStatusColor(Color.RED);
            mVirusesPreference.setStatus(mViruses + "");
        } else {
            mVirusesPreference.setEnabled(false);
            mVirusesPreference.showStatusArrow(false);
            mVirusesPreference.setStatusColor(Color.parseColor("#61000000"));
            mVirusesPreference.setStatus(getString(R.string.safety_no_risk));
        }
        if (mStealAccountRisks > 0) {
            mStealAccountRisksPreference.setEnabled(true);
            mStealAccountRisksPreference.showStatusArrow(true);
            mStealAccountRisksPreference.setStatusColor(Color.RED);
            mStealAccountRisksPreference.setStatus(mStealAccountRisks + "");
        } else {
            mStealAccountRisksPreference.setEnabled(false);
            mStealAccountRisksPreference.showStatusArrow(false);
            mStealAccountRisksPreference.setStatusColor(Color.parseColor("#61000000"));
            mStealAccountRisksPreference.setStatus(getString(R.string.safety_no_risk));
        }
//        if (mPlugin > 0) {
//            mPluginsPreference.setStatus(mPlugin + "");
//        } else {
//            mPluginsPreference.setStatus("");
//        }
        SPUtils.instance(this).setLongValue(Constant.SHARED_PREFERENCES_KEY_QSCAN_TIME, 0,
                System.currentTimeMillis());
    }

    private class QScanListenerUI extends QScanListener {
        private int mScanMode;

        public QScanListenerUI(int scanMode) {
            mScanMode = scanMode;
        }

        public void onScanStarted(int scanType) {
            Log.v(TAG, "onScanStarted() -> scanType = " + scanType);
        }

        public void onScanProgress(int scanType, int curr, int total, QScanResultEntity result) {
            Log.v(TAG, "onScanProgress() -> scanType = " + scanType + ", curr = " + curr
                    + ", total = " + total + ", packageName = " + result.packageName
                    + ", softName = " + result.softName);
            mScanDetailInfo = getString(R.string.safety_scanning)
                    + ":[" + result.packageName + "][" + result.softName + "]\n";
            mUpdateUIHandler.sendEmptyMessage(MSG_UI_UPDATE_SCAN_DETAIL);
        }

        public void onScanError(int scanType, int errCode) {
            Log.v(TAG, "onScanError() -> scanType = " + scanType + ", errCode = " + errCode);
            mScanDetailInfo = "扫描出错:[" + scanType + "]errCode:[" + errCode + "]\n";
            mUpdateUIHandler.sendEmptyMessage(MSG_UI_UPDATE_SCAN_DETAIL);
        }

        public void onScanPaused(int scanType) {
            Log.v(TAG, "onScanPaused() -> scanType = " + scanType);
            mScanDetailInfo = "暂停扫描:[" + scanType + "]\n";

            mUpdateUIHandler.sendEmptyMessage(MSG_UI_UPDATE_SCAN_DETAIL);
        }

        public void onScanContinue(int scanType) {
            Log.v(TAG, "onScanContinue, scanType:[" + scanType + "]");
            mScanDetailInfo = "继续扫描:[" + scanType + "]\n";

            mUpdateUIHandler.sendEmptyMessage(MSG_UI_UPDATE_SCAN_DETAIL);
        }

        public void onScanCanceled(int scanType) {
            Log.v(TAG, "onScanCanceled() -> scanType = " + scanType);
            mScanDetailInfo = "取消扫描:[" + scanType + "]\n";

            mUpdateUIHandler.sendEmptyMessage(MSG_UI_UPDATE_SCAN_DETAIL);
        }

        public void onScanFinished(int scanType, List<QScanResultEntity> results) {
            Log.v(TAG, "onScanFinished() -> scanType = " + scanType + ", scanMode = " + mScanMode);

            for (QScanResultEntity entity : results) {
                if (entity.scanResult == QScanConfig.RET_SAFE) {
                    continue;
                }
                Log.v(TAG, "onScanFinished() -> " +
                        ", packageName = " + entity.packageName +
                        ", softName = " + entity.softName +
                        ", version = " + entity.version +
                        ", versionCode = " + entity.versionCode +
                        ", path = " + entity.path +
                        ", scanResult = " + entity.scanResult +
                        ", description = " + entity.virusDiscription);
                if (entity.scanResult == QScanConfig.RET_VIRUSES) {
                    mViruses++;
                    if (mScanMode == RiskEntity.RISK_TYPE_APP) {
                        mVirusesAppList.add(entity.packageName);
                    } else {
                        mVirusesFileList.add(entity.path);
                    }
                } else if (entity.scanResult == QScanConfig.RET_PAY_RISKS) {
                    mPayRisks++;
                    if (mScanMode == RiskEntity.RISK_TYPE_APP) {
                        mPayRisksAppList.add(entity.packageName);
                    } else {
                        mPayRisksFileList.add(entity.path);
                    }
                } else if (entity.scanResult == QScanConfig.RET_STEALACCOUNT_RISKS) {
                    mStealAccountRisks++;
                    if (mScanMode == RiskEntity.RISK_TYPE_APP) {
                        mStealAccountRisksAppList.add(entity.packageName);
                    } else {
                        mStealAccountRisksFileList.add(entity.path);
                    }
                } else if (entity.scanResult == QScanConfig.RET_OTHER_RISKS) {
                    mOtherRisk++;
                    if (mScanMode == RiskEntity.RISK_TYPE_APP) {
                        mOtherRiskAppList.add(entity.packageName);
                    } else {
                        mOtherRiskFileList.add(entity.path);
                    }
                } else if (entity.scanResult == QScanConfig.RET_NOT_OFFICIAL) {
                    mNotOfficial++;
                    if (mScanMode == RiskEntity.RISK_TYPE_APP) {
                        mNotOfficialAppList.add(entity.packageName);
                    } else {
                        mNotOfficialFileList.add(entity.path);
                    }
                } else if (entity.plugins.size() > 0) {
                    mPlugin++;
                }

                RiskEntity riskEntity = new RiskEntity(mScanMode, entity);
                QScannerRiskProvider.saveRiskEntity(QScannerActivity.this, riskEntity);
            }

            if (mScanMode == RiskEntity.RISK_TYPE_APP) {
                mMainJobHandler.sendEmptyMessage(MSG_START_SCAN_UNINSTALL_PACKAGE);
            } else {
                mUpdateUIHandler.sendEmptyMessage(MSG_UI_UPDATE_SCAN_RESULT);
            }
        }
    }
}