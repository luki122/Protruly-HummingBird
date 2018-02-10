/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mms.ui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.NumberKeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.transaction.TransactionService;
import com.android.mms.util.FeatureOption;
import com.mediatek.cb.cbsettings.CellBroadcastActivity;
import com.mediatek.mms.ext.IOpSubSelectActivityExt;
import com.mediatek.opmsg.util.OpMessageUtils;
import com.mediatek.setting.SimStateMonitor;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.List;

import hb.app.dialog.AlertDialog;
import hb.preference.Preference;
import hb.preference.PreferenceActivity;
import hb.preference.PreferenceCategory;
import hb.preference.PreferenceManager;
import hb.preference.PreferenceScreen;
import hb.preference.SwitchPreference;

//import android.app.AlertDialog;
//import android.preference.CheckBoxPreference;
//import android.preference.EditTextPreference;
//import android.preference.ListPreference;
//import android.preference.Preference;
//import android.preference.Preference.OnPreferenceChangeListener;
//import android.preference.Preference.OnPreferenceClickListener;
//import android.preference.PreferenceActivity;
//import android.preference.PreferenceCategory;
//import android.preference.PreferenceManager;
//import android.preference.PreferenceScreen;
//import android.preference.RingtonePreference;
//lichao add begin
//import android.telephony.TelephonyManager;
//import com.android.internal.telephony.Phone;
//import com.android.internal.telephony.PhoneFactory;
//lichao add end

/**
 * With this activity, users can set preferences for MMS and SMS and
 * can access and manipulate SMS messages stored on the SIM.
 */
public class MyMessagingPreferenceActivity extends PreferenceActivity
            /*implements OnPreferenceChangeListener*/ {
    private static final String TAG = "Mms/MyMessagingPref";
    private static final boolean DEBUG = false;
    // Symbolic names for the keys used for preference lookup
    //see: xml/zzz_preferences.xml
    //public static final String MMS_DELIVERY_REPORT_MODE = "pref_key_mms_delivery_reports";
    public static final String EXPIRY_TIME              = "pref_key_mms_expiry";
    //public static final String EXPIRY_TIME_SLOT1        = "pref_key_mms_expiry_slot1";
    //public static final String EXPIRY_TIME_SLOT2        = "pref_key_mms_expiry_slot2";
    //public static final String PRIORITY                 = "pref_key_mms_priority";
    //public static final String READ_REPORT_MODE         = "pref_key_mms_read_reports";
    public static final String SMS_DELIVERY_REPORT_MODE = "pref_key_sms_delivery_reports";
    //public static final String SMS_DELIVERY_REPORT_SUB1 = "pref_key_sms_delivery_reports_slot1";
    //public static final String SMS_DELIVERY_REPORT_SUB2 = "pref_key_sms_delivery_reports_slot2";
    //public static final String NOTIFICATION_ENABLED     = "pref_key_enable_notifications";
    //public static final String NOTIFICATION_VIBRATE     = "pref_key_vibrate";
    //public static final String NOTIFICATION_VIBRATE_WHEN= "pref_key_vibrateWhen";
    //public static final String NOTIFICATION_RINGTONE    = "pref_key_ringtone";
    //HB. Comments :  , Engerineer : lichao , Date : 17-6-23 , begin
    public static final String NOTIFICATION_WAKE_UP_SCREEN = "pref_key_wake_up_screen";
    //HB. end
    public static final String AUTO_RETRIEVAL           = "pref_key_mms_auto_retrieval";
    public static final String RETRIEVAL_DURING_ROAMING = "pref_key_mms_retrieval_during_roaming";
    //public static final String AUTO_DELETE              = "pref_key_auto_delete";
    //public static final String GROUP_MMS_MODE           = "pref_key_mms_group_mms";
    //public static final String SMS_CDMA_PRIORITY        = "pref_key_sms_cdma_priority";
    public static final String SMSC_DEFAULT             = "pref_key_default_smsc";

    // ConfigurationClient
    //public static final String OMACP_CONFIGURATION_CATEGORY =
    //        "pref_key_sms_omacp_configuration";
    //public static final String CONFIGURATION_MESSAGE    = "pref_key_configuration_message";

    public static final String CHAT_WALLPAPER_SETTING   = "pref_key_chat_wallpaper";

    // Expiry of MMS
    //private final static String EXPIRY_ONE_WEEK = "604800"; // 7 * 24 * 60 * 60
    //private final static String EXPIRY_TWO_DAYS = "172800"; // 2 * 24 * 60 * 60

    //Chat wallpaper
    public static final String CHAT_WALLPAPER            = "chat_wallpaper";

    //private static final int  PICK_FROM_CAMERA        = 0;
    //private static final int  PICK_FROM_GALLERY       = 1;

    public static final String CELL_BROADCAST            = "pref_key_cell_broadcast";
    //Fontsize
    public static final String FONT_SIZE_SETTING         = "pref_key_message_font_size";

    // Menu entries
    private static final int MENU_RESTORE_DEFAULTS    = 1;

    // Preferences for enabling and disabling SMS
    private Preference mSmsDisabledPref;
    //private Preference mSmsEnabledPref;

    //private PreferenceCategory mStoragePrefCategory;
    private PreferenceCategory mSmsPrefCategory;
    //lichao add begin
    private PreferenceCategory mSmsMmsPrefCategory;
    //private PreferenceCategory mSimMessagesPrefCategory;
    private PreferenceCategory mCellBroadcastPrefCategory;//pref_key_cb_category
    //lichao add end
    private PreferenceCategory mMmsPrefCategory;
    //private PreferenceCategory mNotificationPrefCategory;
    //private PreferenceCategory mSmscPrefCate;//pref_key_smsc
    private Preference mSmsServiceCenterPref;
    private Preference mSmsServiceCenterSim1Pref;
    private Preference mSmsServiceCenterSim2Pref;

    //private Preference mChatWallpaperPref;
    //private Preference mSmsLimitPref;
    private SwitchPreference mSmsDeliveryReportPref;
    //private SwitchPreference mSmsDeliveryReportPrefSub1;
    //private SwitchPreference mSmsDeliveryReportPrefSub2;
    //private Preference mMmsLimitPref;
    //private SwitchPreference mMmsDeliveryReportPref;
    //private Preference mMmsGroupMmsPref;
    //private SwitchPreference mMmsReadReportPref;
    private Preference mManageSimPref;
    private Preference mManageSim1Pref;
    private Preference mManageSim2Pref;
    private Preference mCBsettingPref;
    private Preference mCBsettingSim1Pref;
    private Preference mCBsettingSim2Pref;
    //private Preference mClearHistoryPref;
    //private Preference mConfigurationmessage;
    //private Preference mMmsSizeLimit;
    //private SwitchPreference mVibratePref;
    //private SwitchPreference mEnableNotificationsPref;
    private SwitchPreference mMmsAutoRetrievialPref;
    //private ListPreference mFontSizePref;
    //private ListPreference mMmsCreationModePref;
    //private ListPreference mMmsExpiryPref;
    //private ListPreference mMmsExpiryCard1Pref;
    //private ListPreference mMmsExpiryCard2Pref;
    //private RingtonePreference mRingtonePref;
    //private ListPreference mSmsStorePref;
    //private ListPreference mSmsStoreCard1Pref;
    //private ListPreference mSmsStoreCard2Pref;
    //private ListPreference mSmsValidityPref;
    //private ListPreference mSmsValidityCard1Pref;
    //private ListPreference mSmsValidityCard2Pref;
    //private Recycler mSmsRecycler;
    //private Recycler mMmsRecycler;
    //private Preference mSmsTemplate;
    //private SwitchPreference mSmsSignaturePref;
    //private EditTextPreference mSmsSignatureEditPref;
    //private ArrayList<Preference> mSmscPrefList = new ArrayList<Preference>();
    //private static final int CONFIRM_CLEAR_SEARCH_HISTORY_DIALOG = 3;
    private SwitchPreference mWakeUpPref;

    private AsyncDialog mAsyncDialog;

    // Whether or not we are currently enabled for SMS. This field is updated in onResume to make
    // sure we notice if the user has changed the default SMS app.
    private boolean mIsSmsEnabled;
    private static final String SMSC_DIALOG_TITLE = "title";
    private static final String SMSC_DIALOG_NUMBER = "smsc";
    private static final String SMSC_DIALOG_SUB = "sub";
    private static final int EVENT_SET_SMSC_DONE = 0;
    private static final int EVENT_GET_SMSC_DONE = 1;
    private static final int EVENT_SET_SMSC_PREF_DONE = 2;
    private static final String EXTRA_EXCEPTION = "exception";
    //private static SmscHandler mHandler = null;

    public static final String MESSAGE_FONT_SIZE = "message_font_size";

    public static final String MMS_CREATION_MODE = "pref_key_creation_mode";
    public static final int CREATION_MODE_RESTRICTED = 1;
    public static final int CREATION_MODE_WARNING = 2;
    public static final int CREATION_MODE_FREE = 3;

    //private TelephonyManager mTelephonyManager;
    //private Phone mPhone = null;

    // ConfigurationClient
    //private static final String ACTION_CONFIGURE_MESSAGE =
    //        "org.codeaurora.CONFIGURE_MESSAGE";

    //这个广播接收器只过滤了2个广播，而SimStateMonitor类过滤了5个广播更加保险
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)
                    || Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                updateAllPref();
            }
        }
    };

    //lichao merge from com/mediatek/setting/SubSelectActivity.java
    private IOpSubSelectActivityExt mOpSubSelectActivityExt;
    private List<SubscriptionInfo> mSubInfoList;
    private int mSubCount;

    //lichao merge from com/mediatek/setting/GeneralPreferenceActivity.java
    public String SUB_TITLE_NAME = "sub_title_name";

    private Handler mHandler;

    private int mShowDialogSubId;
    MyEditDialogFragment mServiceCenterDialog;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.d(TAG, "==onCreate()==");
        //lichao add in 2017-04-11 begin
        // add Plugin,set title
        mOpSubSelectActivityExt = OpMessageUtils.getOpMessagePlugin()
                .getOpSubSelectActivityExt();
        mOpSubSelectActivityExt.onCreate(this);

        /*mSubInfoList = SubscriptionManager.from(this).getActiveSubscriptionInfoList();
        //参考SimStateMonitor类，增加这个null判断更加保险
        if (mSubInfoList == null) {
            mSubInfoList = new CopyOnWriteArrayList();
        }*/
        //或者直接干脆使用SimStateMonitor类的实例来获取：
        mSubInfoList = SimStateMonitor.getInstance().getSubInfoList();

        //lichao add in 2017-04-11 end
        //mHandler = new SmscHandler(this);
        loadPrefs();

        //ActionBar actionBar = getActionBar();
        //actionBar.setDisplayHomeAsUpEnabled(true);

        getToolbar().setTitle(getResources().getString(R.string.preferences_title));

        mHandler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "==onCreate()==");
        boolean isSmsEnabled = MmsConfig.isSmsEnabled(this);
        if (isSmsEnabled != mIsSmsEnabled) {
            mIsSmsEnabled = isSmsEnabled;
            invalidateOptionsMenu();
        }
        updateSmsEnabledState();

        // Since the enabled notifications pref can be changed outside of this activity,
        // we have to reload it whenever we resume.
        //setEnabledNotificationsPref();
        // Initialize the sms signature
        //updateSignatureStatus();
        registerListeners();
        updateAllPref();
    }

    private void updateSmsEnabledState() {
        // Show the right pref (SMS Disabled or SMS Enabled)
        PreferenceScreen prefRoot = (PreferenceScreen)findPreference("pref_key_root");
        // Enable or Disable the settings as appropriate
        //mStoragePrefCategory.setEnabled(mIsSmsEnabled);
        //mSmsPrefCategory.setEnabled(mIsSmsEnabled);
        mSmsMmsPrefCategory.setEnabled(mIsSmsEnabled);
        //mSimMessagesPrefCategory.setEnabled(mIsSmsEnabled);
        //mSmscPrefCate.setEnabled(mIsSmsEnabled);
        mMmsPrefCategory.setEnabled(mIsSmsEnabled);
        //mNotificationPrefCategory.setEnabled(mIsSmsEnabled);
        if (!mIsSmsEnabled) {
            prefRoot.addPreference(mSmsDisabledPref);
        } else {
            prefRoot.removePreference(mSmsDisabledPref);
        }
        if (null != mCellBroadcastPrefCategory) {
            // Enable link to Cell broadcast activity depending on the value in config.xml.
            boolean isCellBroadcastAppLinkEnabled = this.getResources().getBoolean(
                    com.android.internal.R.bool.config_cellBroadcastAppLinks);
            Log.d(TAG, "updateSmsEnabledState(), config_cellBroadcastAppLinks = " + isCellBroadcastAppLinkEnabled);
            if (isCellBroadcastAppLinkEnabled) {
                prefRoot.addPreference(mCellBroadcastPrefCategory);
                mCellBroadcastPrefCategory.setEnabled(mIsSmsEnabled);
            } else {
                prefRoot.removePreference(mCellBroadcastPrefCategory);
                mCellBroadcastPrefCategory = null;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(DEBUG) Log.d(TAG, "==onPause()==");
        unregisterReceiver(mReceiver);

        if (mAsyncDialog != null) {
            mAsyncDialog.clearPendingProgressDialog();
        }
    }

    private void loadPrefs() {
        //if (getResources().getBoolean(R.bool.def_custom_preferences_settings)) {
        //    addPreferencesFromResource(R.xml.custom_preferences);
        //} else {
            addPreferencesFromResource(R.xml.zzz_preferences);
        //}

        mSmsMmsPrefCategory = (PreferenceCategory)findPreference("pref_key_sms_mms_settings");
        //mSimMessagesPrefCategory = (PreferenceCategory)findPreference("pref_key_sim_messages");
        mCellBroadcastPrefCategory = (PreferenceCategory)findPreference("pref_key_cb_category");

        mSmsDisabledPref = findPreference("pref_key_sms_disabled");
        //mSmsEnabledPref = findPreference("pref_key_sms_enabled");

        //mStoragePrefCategory = (PreferenceCategory)findPreference("pref_key_storage_settings");
        //mSmsPrefCategory = (PreferenceCategory)findPreference("pref_key_sms_settings");
        mMmsPrefCategory = (PreferenceCategory)findPreference("pref_key_mms_settings");
        //mNotificationPrefCategory =
        //        (PreferenceCategory)findPreference("pref_key_notification_settings");

        mManageSimPref = findPreference("pref_key_manage_sim_messages");
        mManageSim1Pref = findPreference("pref_key_manage_sim_messages_slot1");
        mManageSim2Pref = findPreference("pref_key_manage_sim_messages_slot2");
        //mSmsLimitPref = findPreference("pref_key_sms_delete_limit");
        mSmsDeliveryReportPref = (SwitchPreference)findPreference("pref_key_sms_delivery_reports");
        //mSmsDeliveryReportPrefSub1 = (SwitchPreference)findPreference("pref_key_sms_delivery_reports_slot1");
        //mSmsDeliveryReportPrefSub2 = (SwitchPreference)findPreference("pref_key_sms_delivery_reports_slot2");
        //mMmsDeliveryReportPref = (SwitchPreference)findPreference("pref_key_mms_delivery_reports");
        //mMmsGroupMmsPref = findPreference("pref_key_mms_group_mms");
        //mMmsReadReportPref = (SwitchPreference)findPreference("pref_key_mms_read_reports");
        //mMmsLimitPref = findPreference("pref_key_mms_delete_limit");
        //mClearHistoryPref = findPreference("pref_key_mms_clear_history");
        //mEnableNotificationsPref = (SwitchPreference) findPreference(NOTIFICATION_ENABLED);
        mMmsAutoRetrievialPref = (SwitchPreference) findPreference(AUTO_RETRIEVAL);
        /*
		mMmsExpiryPref = (ListPreference) findPreference("pref_key_mms_expiry");
        mMmsExpiryCard1Pref = (ListPreference) findPreference("pref_key_mms_expiry_slot1");
        mMmsExpiryCard2Pref = (ListPreference) findPreference("pref_key_mms_expiry_slot2");
        mMmsCreationModePref = (ListPreference) findPreference("pref_key_creation_mode");
        mSmsSignaturePref = (SwitchPreference) findPreference("pref_key_enable_signature");
        mSmsSignatureEditPref = (EditTextPreference) findPreference("pref_key_edit_signature");
        */
        /*
        mVibratePref = (SwitchPreference) findPreference(NOTIFICATION_VIBRATE);
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibratePref != null && (vibrator == null || !vibrator.hasVibrator())) {
            mNotificationPrefCategory.removePreference(mVibratePref);
            mVibratePref = null;
        }
        mRingtonePref = (RingtonePreference) findPreference(NOTIFICATION_RINGTONE);
        */
        mWakeUpPref = (SwitchPreference) findPreference(NOTIFICATION_WAKE_UP_SCREEN);
		/*
        mSmsTemplate = findPreference("pref_key_message_template");
        mSmsStorePref = (ListPreference) findPreference("pref_key_sms_store");
        mSmsStoreCard1Pref = (ListPreference) findPreference("pref_key_sms_store_card1");
        mSmsStoreCard2Pref = (ListPreference) findPreference("pref_key_sms_store_card2");
        mSmsValidityPref = (ListPreference) findPreference("pref_key_sms_validity_period");
        mSmsValidityCard1Pref
            = (ListPreference) findPreference("pref_key_sms_validity_period_slot1");
        mSmsValidityCard2Pref
            = (ListPreference) findPreference("pref_key_sms_validity_period_slot2");
        // ConfigurationClient
        if((MmsConfig.isOMACPEnabled())){
            mConfigurationmessage = findPreference(CONFIGURATION_MESSAGE);
        }else {
            PreferenceScreen prefRoot = (PreferenceScreen) findPreference("pref_key_root");
            PreferenceCategory OMACPConCategory =
                    (PreferenceCategory) findPreference(OMACP_CONFIGURATION_CATEGORY);
            prefRoot.removePreference(OMACPConCategory);
        }

        mMmsSizeLimit = (Preference) findPreference("pref_key_mms_size_limit");

        if (getResources().getBoolean(R.bool.def_custom_preferences_settings)) {
            mCBsettingPref = findPreference(CELL_BROADCAST);
            mFontSizePref = (ListPreference) findPreference(FONT_SIZE_SETTING);
        }
        //Chat wallpaper
        if (getResources().getBoolean(R.bool.def_custom_preferences_settings)) {
            mChatWallpaperPref = findPreference(CHAT_WALLPAPER_SETTING);
        }
		*/
        mCBsettingPref = findPreference("pref_key_cell_broadcast");
        mCBsettingSim1Pref = findPreference("pref_key_cell_broadcast_slot1");
        mCBsettingSim2Pref = findPreference("pref_key_cell_broadcast_slot2");
        /*
        if (SimStateMonitor.getInstance().getSubCount() < 1) {
            mCBsettingPref.setEnabled(false);
        }
        // Change the key to the SIM-related key, if has one SIM card, else set default value.
        if (SimStateMonitor.getInstance().getSubCount() > 1) {
            // MTK_OP02_PROTECT_END
            mCellBroadcastMultiSub = findPreference(CELL_BROADCAST);
        }
        */

        setMessagePreferences();
    }

    private void restoreDefaultPreferences() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().clear().apply();
        setPreferenceScreen(null);
        // Reset the SMSC preference.
        //mSmscPrefList.clear();
        //mSmscPrefCate.removeAll();
        loadPrefs();
        mIsSmsEnabled = MmsConfig.isSmsEnabled(this);
        updateSmsEnabledState();

        // NOTE: After restoring preferences, the auto delete function (i.e. message recycler)
        // will be turned off by default. However, we really want the default to be turned on.
        // Because all the prefs are cleared, that'll cause:
        // ConversationList.runOneTimeStorageLimitCheckForLegacyMessages to get executed the
        // next time the user runs the Messaging app and it will either turn on the setting
        // by default, or if the user is over the limits, encourage them to turn on the setting
        // manually.
    }

    private void setMessagePreferences() {
        //updateSignatureStatus();

        //mSmscPrefCate = (PreferenceCategory) findPreference("pref_key_smsc");
        mSmsServiceCenterPref = findPreference("pref_key_sms_service_center");
        mSmsServiceCenterSim1Pref = findPreference("pref_key_sms_service_center_slot1");
        mSmsServiceCenterSim2Pref = findPreference("pref_key_sms_service_center_slot2");

        //lichao move it to updateSMSCPref()
        //showSmscPref();

        //lichao move it to updateAllPref()
        //updateSMSCPref();

        //setMessagePriorityPref();

        //lichao move it to updateAllPref()
        //updateDeliveryReportPref();

        //lichao move it to updateAllPref()
        //updateAutoRetrievialPref();

        /*if (!MmsConfig.getSMSDeliveryReportsEnabled()) {
            //mSmsPrefCategory.removePreference(mSmsDeliveryReportPref);
            getPreferenceScreen().removePreference(mSmsMmsPrefCategory);
            //mSmsPrefCategory.removePreference(mSmsDeliveryReportPrefSub1);
            //mSmsPrefCategory.removePreference(mSmsDeliveryReportPrefSub2);
            *//*if (!MmsApp.getApplication().getTelephonyManager().hasIccCard()) {
                getPreferenceScreen().removePreference(mSmsPrefCategory);
            }*//*
        }*//* else {
            if (MessageUtils.isMultiSimEnabledMms()) {
                mSmsPrefCategory.removePreference(mSmsDeliveryReportPref);
                if (!MessageUtils.isIccCardEnabled(MessageUtils.SUB1)) {
                    mSmsDeliveryReportPrefSub1.setEnabled(false);
                }
                if (!MessageUtils.isIccCardEnabled(MessageUtils.SUB2)) {
                    mSmsDeliveryReportPrefSub2.setEnabled(false);
                }
            } else {
                mSmsPrefCategory.removePreference(mSmsDeliveryReportPrefSub1);
                mSmsPrefCategory.removePreference(mSmsDeliveryReportPrefSub2);
            }
        }*/

        //setMmsRelatedPref();

        //setEnabledNotificationsPref();

        /*
        if (getResources().getBoolean(R.bool.config_savelocation)) {
            if (MessageUtils.isMultiSimEnabledMms()) {
                PreferenceCategory storageOptions =
                    (PreferenceCategory)findPreference("pref_key_storage_settings");
                storageOptions.removePreference(mSmsStorePref);

                if (!MessageUtils.isIccCardActivated(MessageUtils.SUB1)) {
                    mSmsStoreCard1Pref.setEnabled(false);
                } else {
                    setSmsPreferStoreSummary(MessageUtils.SUB1);
                }
                if (!MessageUtils.isIccCardActivated(MessageUtils.SUB2)) {
                    mSmsStoreCard2Pref.setEnabled(false);
                } else {
                    setSmsPreferStoreSummary(MessageUtils.SUB2);
                }
            } else {
                PreferenceCategory storageOptions =
                    (PreferenceCategory)findPreference("pref_key_storage_settings");
                storageOptions.removePreference(mSmsStoreCard1Pref);
                storageOptions.removePreference(mSmsStoreCard2Pref);

                if (!MessageUtils.hasIccCard()) {
                    mSmsStorePref.setEnabled(false);
                } else {
                    setSmsPreferStoreSummary();
                }
            }
        } else {
            PreferenceCategory storageOptions =
                    (PreferenceCategory)findPreference("pref_key_storage_settings");
            storageOptions.removePreference(mSmsStorePref);
            storageOptions.removePreference(mSmsStoreCard1Pref);
            storageOptions.removePreference(mSmsStoreCard2Pref);
        }
        setSmsValidityPeriodPref();
		*/

        // If needed, migrate vibration setting from the previous tri-state setting stored in
        // NOTIFICATION_VIBRATE_WHEN to the boolean setting stored in NOTIFICATION_VIBRATE.
        /*SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.contains(NOTIFICATION_VIBRATE_WHEN)) {
            String vibrateWhen = sharedPreferences.
                    getString(MyMessagingPreferenceActivity.NOTIFICATION_VIBRATE_WHEN, null);
            boolean vibrate = "always".equals(vibrateWhen);
            SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
            prefsEditor.putBoolean(NOTIFICATION_VIBRATE, vibrate);
            prefsEditor.remove(NOTIFICATION_VIBRATE_WHEN);  // remove obsolete setting
            prefsEditor.apply();
            mVibratePref.setChecked(vibrate);
        }*/

        //mSmsRecycler = Recycler.getSmsRecycler();
        //mMmsRecycler = Recycler.getMmsRecycler();

        // Fix up the recycler's summary with the correct values
        //setSmsDisplayLimit();
        //setMmsDisplayLimit();
        //setMmsExpiryPref();

        //String soundValue = sharedPreferences.getString(NOTIFICATION_RINGTONE, null);
        //setRingtoneSummary(soundValue);
    }

    private void setMmsRelatedPref() {
        if (!MmsConfig.getMmsEnabled()) {
            // No Mms, remove all the mms-related preferences
            getPreferenceScreen().removePreference(mMmsPrefCategory);

            //mStoragePrefCategory.removePreference(findPreference("pref_key_mms_delete_limit"));
        } else {
            /*if (!MmsConfig.getMMSDeliveryReportsEnabled()) {
                mMmsPrefCategory.removePreference(mMmsDeliveryReportPref);
            }*/
            /*if (!MmsConfig.getMMSReadReportsEnabled()) {
                mMmsPrefCategory.removePreference(mMmsReadReportPref);
            }*/
			/*
            // If the phone's SIM doesn't know it's own number, disable group mms.
            if (!MmsConfig.getGroupMmsEnabled() ||
                    TextUtils.isEmpty(MessageUtils.getLocalNumber())) {
                mMmsPrefCategory.removePreference(mMmsGroupMmsPref);
            }
            if (!MmsConfig.isCreationModeEnabled()) {
                mMmsPrefCategory.removePreference(mMmsCreationModePref);
            }
            if (!getResources().getBoolean(
                    R.bool.def_custom_preferences_settings)
                    && !(getResources().getBoolean(R.bool.def_show_mms_size))) {
                mMmsPrefCategory.removePreference(mMmsSizeLimit);
            } else {
                setMmsSizeSummary();
            }
			*/
        }

        if (MessageUtils.isMultiSimEnabledMms()) {
            /*if(MessageUtils.getActivatedIccCardCount() < PhoneConstants.MAX_PHONE_COUNT_DUAL_SIM) {
                int subId = SmsManager.getDefault().getDefaultSmsSubscriptionId();
                int phoneId = SubscriptionManager.getPhoneId(subId);
                mManageSimPref.setSummary(
                        getString(R.string.pref_summary_manage_sim_messages_slot, phoneId + 1));
            } else {
                mManageSimPref.setSummary(
                        getString(R.string.pref_summary_manage_sim_messages));
            }*/
            //mMmsPrefCategory.removePreference(mMmsExpiryPref);
        } /*else {
            //mMmsPrefCategory.removePreference(mMmsExpiryCard1Pref);
            //mMmsPrefCategory.removePreference(mMmsExpiryCard2Pref);
        }*/
    }

    /*
    private void setMessagePriorityPref() {
        if (!getResources().getBoolean(R.bool.support_sms_priority)) {
            Preference priorotySettings = findPreference(SMS_CDMA_PRIORITY);
            PreferenceScreen prefSet = getPreferenceScreen();
            prefSet.removePreference(priorotySettings);
        }
    }

    private void setSmsValidityPeriodPref() {
        PreferenceCategory storageOptions =
                (PreferenceCategory)findPreference("pref_key_sms_settings");
        if (getResources().getBoolean(R.bool.config_sms_validity)) {
            if (MessageUtils.isMultiSimEnabledMms()) {
                storageOptions.removePreference(mSmsValidityPref);
                if (!MessageUtils.isIccCardActivated(MessageUtils.SUB1)) {
                    mSmsValidityCard1Pref.setEnabled(false);
                } else {
                    setSmsPreferValiditySummary(MessageUtils.SUB1);
                }
                if (!MessageUtils.isIccCardActivated(MessageUtils.SUB2)) {
                    mSmsValidityCard2Pref.setEnabled(false);
                } else {
                    setSmsPreferValiditySummary(MessageUtils.SUB2);
                }
            } else {
                storageOptions.removePreference(mSmsValidityCard1Pref);
                storageOptions.removePreference(mSmsValidityCard2Pref);
                setSmsPreferValiditySummary(MessageUtils.SUB_INVALID);
            }
        } else {
            storageOptions.removePreference(mSmsValidityPref);
            storageOptions.removePreference(mSmsValidityCard1Pref);
            storageOptions.removePreference(mSmsValidityCard2Pref);
        }
    }
	*/

    /*private void setRingtoneSummary(String soundValue) {
        Uri soundUri = TextUtils.isEmpty(soundValue) ? null : Uri.parse(soundValue);
        Ringtone tone = soundUri != null ? RingtoneManager.getRingtone(this, soundUri) : null;
        mRingtonePref.setSummary(tone != null ? tone.getTitle(this)
                : getResources().getString(R.string.silent_ringtone));
    }*/

    private void showSmscPref() {
        /*int count = TelephonyManager.getDefault().getPhoneCount();
        for (int i = 0; i < count; i++) {
            final Preference pref = new Preference(this);
            pref.setKey(String.valueOf(i));
            pref.setTitle(getSMSCDialogTitle(count, i));
            //lichao modify
            //if (getResources().getBoolean(R.bool.def_enable_reset_smsc)
            //    || getResources().getBoolean(com.android.internal.R.bool.config_regional_smsc_editable)) {
            //    pref.setOnPreferenceClickListener(null);
            //} else {
            //}
            mSmscPrefCate.addPreference(pref);
            mSmscPrefList.add(pref);
        }*/
		//lichao move it to updateSMSCPref()
        /*if (MessageUtils.isMultiSimEnabledMms()) {
            mSmsMmsPrefCategory.removePreference(mSmsServiceCenterPref);
            mSmsMmsPrefCategory.addPreference(mSmsServiceCenterSim1Pref);
            mSmsMmsPrefCategory.addPreference(mSmsServiceCenterSim2Pref);
        }else{
            mSmsMmsPrefCategory.addPreference(mSmsServiceCenterPref);
            mSmsMmsPrefCategory.removePreference(mSmsServiceCenterSim1Pref);
            mSmsMmsPrefCategory.removePreference(mSmsServiceCenterSim2Pref);
        }*/
    }

    //update Sim Messages Preference
    private void updateSIMSMSPref() {
        /*if (null == mSimMessagesPrefCategory) {
            return;
        }*/
        boolean isNotAirPlaneMode = !isAirPlaneModeOn();
        if (MessageUtils.isMultiSimEnabledMms()) {
            /*if (!MessageUtils.isIccCardActivated(MessageUtils.SUB1)) {
                mSimMessagesPrefCategory.removePreference(mManageSim1Pref);
            } else {
                mSimMessagesPrefCategory.addPreference(mManageSim1Pref);
            }
            if (!MessageUtils.isIccCardActivated(MessageUtils.SUB2)) {
                mSimMessagesPrefCategory.removePreference(mManageSim2Pref);
            } else {
                mSimMessagesPrefCategory.addPreference(mManageSim2Pref);
            }*/
            //lichao modify in 2016-11-30
            mSmsMmsPrefCategory.addPreference(mManageSim1Pref);
            mSmsMmsPrefCategory.addPreference(mManageSim2Pref);
            mSmsMmsPrefCategory.removePreference(mManageSimPref);

            if(mSubCount <= 0){
                mManageSim1Pref.setEnabled(false);
                mManageSim2Pref.setEnabled(false);
                return;
            }
            boolean isIccCardEnabledSim1 = MessageUtils.isIccCardEnabled(MessageUtils.SUB1);
            mManageSim1Pref.setEnabled(isNotAirPlaneMode && isIccCardEnabledSim1);

            boolean isIccCardEnabledSim2 = MessageUtils.isIccCardEnabled(MessageUtils.SUB2);
            mManageSim2Pref.setEnabled(isNotAirPlaneMode && isIccCardEnabledSim2);

            if (mSubCount == 1) {
                int subId = mSubInfoList.get(0).getSubscriptionId();
                int slotId = mSubInfoList.get(0).getSimSlotIndex();
                Log.d(TAG, "updateSIMSMSPref(), when SubCount == 1, subId = "+subId);
                Log.d(TAG, "updateSIMSMSPref(), when SubCount == 1, slotId = "+slotId);
                boolean isRadioOnSim = MessageUtils.isRadioOn(subId, this);
                if(false == isRadioOnSim){
                    if(slotId == MessageUtils.SUB1){
                        mManageSim1Pref.setEnabled(false);
                    }else if(slotId == MessageUtils.SUB2){
                        mManageSim2Pref.setEnabled(false);
                    }
                }
            } else if (mSubCount >= 2) {
                int subId1 = mSubInfoList.get(0).getSubscriptionId();
                int subId2 = mSubInfoList.get(1).getSubscriptionId();
                //int slotId1 = mSubInfoList.get(0).getSimSlotIndex();
                //int slotId2 = mSubInfoList.get(1).getSimSlotIndex();
                //Log.d(TAG, "updateSIMSMSPref(), when SubCount >= 2, subId1 = "+subId1);
                //Log.d(TAG, "updateSIMSMSPref(), when SubCount >= 2, subId2 = "+subId2);
                //Log.d(TAG, "updateSIMSMSPref(), when SubCount >= 2, slotId1 = "+slotId1);
                //Log.d(TAG, "updateSIMSMSPref(), when SubCount >= 2, slotId2 = "+slotId2);
                boolean isRadioOnSim1 = MessageUtils.isRadioOn(subId1, this);
                boolean isRadioOnSim2 = MessageUtils.isRadioOn(subId2, this);
                if (false == isRadioOnSim1 && true == isIccCardEnabledSim1) {
                    mManageSim1Pref.setEnabled(false);
                }
                if (false == isRadioOnSim2 && true == isIccCardEnabledSim2) {
                    mManageSim2Pref.setEnabled(false);
                }
            }



            /*if(MessageUtils.getActivatedIccCardCount() < PhoneConstants.MAX_PHONE_COUNT_DUAL_SIM) {
                int subId = SmsManager.getDefault().getDefaultSmsSubscriptionId();
                int phoneId = SubscriptionManager.getPhoneId(subId);
                mManageSimPref.setSummary(
                        getString(R.string.zzz_pref_summary_manage_sim_messages_slot, phoneId + 1));
            } else {
                mManageSimPref.setSummary(
                        getString(R.string.zzz_pref_summary_manage_sim_messages));
            }*/
        } else {
            /*if (!MessageUtils.hasIccCard()) {
                mSimMessagesPrefCategory.removePreference(mManageSimPref);
            } else {
                mSimMessagesPrefCategory.addPreference(mManageSimPref);
            }*/
            mSmsMmsPrefCategory.addPreference(mManageSimPref);
            mManageSimPref.setEnabled(isNotAirPlaneMode && MessageUtils.hasIccCard());
            mSmsMmsPrefCategory.removePreference(mManageSim1Pref);
            mSmsMmsPrefCategory.removePreference(mManageSim2Pref);
        }
    }

    private boolean isAirPlaneModeOn() {
        //return Settings.System.getInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        return Settings.System.getInt(MmsApp.getApplication().getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 1;
    }

    private String getSMSCDialogTitle(int count, int index) {
        String title = MessageUtils.isMultiSimEnabledMms()
                ? getString(R.string.pref_more_smcs, index + 1)
                : getString(R.string.pref_one_smcs);
        return title;
    }

    /*
    private void setSmsPreferStoreSummary() {
    }

    private void setSmsPreferStoreSummary(int subscription) {
    }

    private void setSmsPreferValiditySummary(int subscription) {
    }
	*/

    /*
    private void setEnabledNotificationsPref() {
        // The "enable notifications" setting is really stored in our own prefs. Read the
        // current value and set the checkbox to match.
        mEnableNotificationsPref.setChecked(getNotificationEnabled(this));
    }
    */

    /*
    private void setSmsDisplayLimit() {
        mSmsLimitPref.setSummary(
                getString(R.string.pref_summary_delete_limit,
                        mSmsRecycler.getMessageLimit(this)));
    }

    private void setMmsDisplayLimit() {
        mMmsLimitPref.setSummary(
                getString(R.string.pref_summary_delete_limit,
                        mMmsRecycler.getMessageLimit(this)));
    }

    private void setMmsExpiryPref() {
    }

    private void setMmsExpirySummary(int subscription) {
    }

    private void updateSignatureStatus() {
        // If the signature CheckBox is checked, we should set the signature EditText
        // enable, and disable when it's not checked.
        boolean isChecked = mSmsSignaturePref.isChecked();
        mSmsSignatureEditPref.setEnabled(isChecked);
    }
	*/

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.clear();
        if (mIsSmsEnabled) {
            menu.add(0, MENU_RESTORE_DEFAULTS, 0, R.string.restore_default);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESTORE_DEFAULTS:
                restoreDefaultPreferences();
                return true;

            case android.R.id.home:
                // The user clicked on the Messaging icon in the action bar. Take them back from
                // wherever they came from
                finish();
                return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
        mSubInfoList = SimStateMonitor.getInstance().getSubInfoList();
        //int subCount = SimStateMonitor.getInstance().getSubCount();
        int subCount = 0;
        if (null != mSubInfoList) {
            subCount = mSubInfoList.size();
        }
        Log.d(TAG, "onPreferenceTreeClick, subCount = " + subCount);
        if (subCount < 1) {
            return true;
        }
        if (preference == mManageSimPref) {
            startActivity(new Intent(this, MySimMessageList.class));
        } else if (preference == mManageSim1Pref) {
            Intent intent = new Intent(this, MySimMessageList.class);
            intent.putExtra(PhoneConstants.SLOT_KEY, PhoneConstants.SUB1);
            startActivity(intent);
        } else if (preference == mManageSim2Pref) {
            Intent intent = new Intent(this, MySimMessageList.class);
            intent.putExtra(PhoneConstants.SLOT_KEY, PhoneConstants.SUB2);
            startActivity(intent);
        } else if (preference == mMmsAutoRetrievialPref) {
            if (mMmsAutoRetrievialPref.isChecked()) {
                startMmsDownload();
            }
        } else if (preference == mCBsettingPref) {
            //qc
            /*try {
                startActivity(MessageUtils.getCellBroadcastIntent());
            } catch (ActivityNotFoundException e) {
                Log.e(TAG,
                    "ActivityNotFoundException for CellBroadcastListActivity");
            }*/
            //mtk:
            onCBsettingPrefClicked(mSubInfoList.get(0));
        } else if (preference == mCBsettingSim1Pref) {
            onCBsettingPrefClicked(mSubInfoList.get(0));
        } else if (preference == mCBsettingSim2Pref) {
            //this for only insert SIM2
            if (subCount == 1) {
                onCBsettingPrefClicked(mSubInfoList.get(0));
                return true;
            }
            if (subCount >= 2) {
                onCBsettingPrefClicked(mSubInfoList.get(1));
            }
        } else if (preference == mSmsServiceCenterPref) {
            int subId = mSubInfoList.get(0).getSubscriptionId();
            Log.d(TAG, "mSmsServiceCenterPref click, subId =" + subId);
            showEditServiceCenterDialog(subId, preference.getTitle());
        } else if (preference == mSmsServiceCenterSim1Pref) {
            //this for only insert SIM1
            int subId = mSubInfoList.get(0).getSubscriptionId();
            Log.d(TAG, "mSmsServiceCenterSim1Pref click, subId =" + subId);
            showEditServiceCenterDialog(subId, preference.getTitle());
        } else if (preference == mSmsServiceCenterSim2Pref) {
            int subId = -1;
            if (subCount == 1) {
                //this for only insert SIM2
                subId = mSubInfoList.get(0).getSubscriptionId();
            } else if (subCount >= 2) {
                subId = mSubInfoList.get(1).getSubscriptionId();
            }
            Log.d(TAG, "mSmsServiceCenterSim2Pref click, subId =" + subId);
            if (subId > 0) {
                showEditServiceCenterDialog(subId, preference.getTitle());
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    /**
     * Trigger the TransactionService to download any outstanding messages.
     */
    private void startMmsDownload() {
        startService(new Intent(TransactionService.ACTION_ENABLE_AUTO_RETRIEVE, null, this,
                TransactionService.class));
    }

    private void registerListeners() {
        //mRingtonePref.setOnPreferenceChangeListener(this);
        final IntentFilter intentFilter =
                new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
    }

    /*public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = false;
        if (preference == mRingtonePref) {
            setRingtoneSummary((String)newValue);
            result = true;
        }
        return result;
    }*/


    private void showToast(int id) {
        Toast.makeText(this, id, Toast.LENGTH_SHORT).show();
    }

    /**
     * Set the SMSC preference enable or disable.
     *
     * @param id  the subscription of the slot, if the value is ALL_SUB, update all the SMSC
     *            preference
     * @param prefEnabled  the state of the airplane mode
     */
    /*private void setSMSCPrefState(int id, boolean prefEnabled) {
        // We need update the preference summary.
        if (prefEnabled) {
            *//*
            if(DEBUG) Log.d(TAG, "get SMSC from sub= " + id);
            if (getResources().getBoolean(R.bool.def_enable_reset_smsc)) {
                updateSmscFromPreference(id);
            } else {
                final Message callback = mHandler.obtainMessage(EVENT_GET_SMSC_DONE);
                Bundle userParams = new Bundle();
                userParams.putInt(PhoneConstants.SLOT_KEY, id);
                callback.obj = userParams;
                MessageUtils.getSmscFromSub(this, id, callback);
            }
            *//*
        } else {
            mSmscPrefList.get(id).setSummary(null);
        }
        mSmscPrefList.get(id).setEnabled(prefEnabled);
    }*/

    private void updateSMSCPref() {
        if (DEBUG) Log.d(TAG, "==updateSMSCPref()== ");
        /*if (mSmscPrefList == null || mSmscPrefList.size() == 0) {
            return;
        }*/
        /*int count = TelephonyManager.getDefault().getPhoneCount();
        for (int i = 0; i < count; i++) {
            boolean isIccCardEnabled = MessageUtils.isMultiSimEnabledMms()
                    ? MessageUtils.isIccCardEnabled(i)
                    : TelephonyManager.getDefault().hasIccCard();
            boolean prefEnabled = !isAirPlaneModeOn() && isIccCardEnabled;
            setSMSCPrefState(i, prefEnabled);
        }*/
        boolean isNotAirPlaneMode = !isAirPlaneModeOn();
        if (MessageUtils.isMultiSimEnabledMms()) {
            mSmsMmsPrefCategory.removePreference(mSmsServiceCenterPref);
            mSmsMmsPrefCategory.addPreference(mSmsServiceCenterSim1Pref);
            mSmsMmsPrefCategory.addPreference(mSmsServiceCenterSim2Pref);
            mSmsServiceCenterSim1Pref.setSummary("");
            mSmsServiceCenterSim2Pref.setSummary("");
            if(mSubCount <= 0){
                mSmsServiceCenterSim1Pref.setEnabled(false);
                mSmsServiceCenterSim2Pref.setEnabled(false);
                return;
            }

            boolean isIccCardEnabledSim1 = MessageUtils.isIccCardEnabled(MessageUtils.SUB1);
            boolean isIccCardEnabledSim2 = MessageUtils.isIccCardEnabled(MessageUtils.SUB2);
            mSmsServiceCenterSim1Pref.setEnabled(isNotAirPlaneMode && isIccCardEnabledSim1);
            mSmsServiceCenterSim2Pref.setEnabled(isNotAirPlaneMode && isIccCardEnabledSim2);

            //当只插入1张卡时候，可能是在卡槽1，也可能是卡槽2
            if(mSubCount == 1){
                int subId = mSubInfoList.get(0).getSubscriptionId();
                boolean isRadioOnSim = MessageUtils.isRadioOn(subId, this);
                boolean isSimSupported = isSimSupported(subId);
                String summary = getSMSC_SummaryString(subId, isSimSupported);
                if(isIccCardEnabledSim1){
                    mSmsServiceCenterSim1Pref.setSummary(summary);
                    if(!isSimSupported || !isRadioOnSim){
                        mSmsServiceCenterSim1Pref.setEnabled(false);
                    }
                } else if(isIccCardEnabledSim2){
                    mSmsServiceCenterSim2Pref.setSummary(summary);
                    if(!isSimSupported || !isRadioOnSim){
                        mSmsServiceCenterSim2Pref.setEnabled(false);
                    }
                }
                return;
            }
            //当插入2张卡时候
            if(mSubCount >= 2){
                int subId1 = mSubInfoList.get(0).getSubscriptionId();
                boolean isRadioOnSim1 = MessageUtils.isRadioOn(subId1, this);
                boolean isSimSupported1 = isSimSupported(subId1);
                String summary1 = getSMSC_SummaryString(subId1, isSimSupported1);
                if(isIccCardEnabledSim1){
                    mSmsServiceCenterSim1Pref.setSummary(summary1);
                    if(!isSimSupported1 || !isRadioOnSim1){
                        mSmsServiceCenterSim1Pref.setEnabled(false);
                    }
                }

                int subId2 = mSubInfoList.get(1).getSubscriptionId();
                boolean isRadioOnSim2 = MessageUtils.isRadioOn(subId2, this);
                boolean isSimSupported2 = isSimSupported(subId2);
                String summary2 = getSMSC_SummaryString(subId2, isSimSupported2);
                if(isIccCardEnabledSim2){
                    mSmsServiceCenterSim2Pref.setSummary(summary2);
                    if(!isSimSupported2 || !isRadioOnSim2){
                        mSmsServiceCenterSim2Pref.setEnabled(false);
                    }
                }
            }
        } else {
            mSmsMmsPrefCategory.addPreference(mSmsServiceCenterPref);
            mSmsMmsPrefCategory.removePreference(mSmsServiceCenterSim1Pref);
            mSmsMmsPrefCategory.removePreference(mSmsServiceCenterSim2Pref);
            String summary = "";
            boolean isSimSupported = false;
            if (SimStateMonitor.getInstance().getSubCount() > 0){
                int subId = SimStateMonitor.getInstance().getSubInfoList().get(0).getSubscriptionId();
                isSimSupported = isSimSupported(subId);
                summary = getSMSC_SummaryString(subId, isSimSupported);
            }
            mSmsServiceCenterPref.setSummary(summary);
            boolean isIccCardEnabled = MessageUtils.hasIccCard();
            mSmsServiceCenterPref.setEnabled(isSimSupported && isNotAirPlaneMode && isIccCardEnabled);
        }

        /*for (int i = 0; i < mSubInfoList.size(); i++) {
            final int subId = mSubInfoList.get(i).getSubscriptionId();
            String summary = getSMSC_Number_MTK(subId);
            if (DEBUG) Log.d(TAG, "updateSMSCPref, subId = "+subId+", setSummary: "+summary);
            mSmscPrefList.get(i).setSummary(summary);
            mSmscPrefList.get(i).setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //lichao add in 2017-04-11 begin
                    if (!mOpSubSelectActivityExt.isSimSupported(subId)) {
                        Toast.makeText(getApplicationContext(), getString(R.string.cdma_not_support),
                                Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    //lichao add in 2017-04-11 end
                    //lichao modify for getKey()=0 while subId=1
                    if (DEBUG) Log.d(TAG, "updateSMSCPref, getKey() = " + preference.getKey());
                    MyEditDialogFragment dialog = MyEditDialogFragment.newInstance(
                            MyMessagingPreferenceActivity.this,
                            preference.getTitle(),
                            preference.getSummary(),
                            subId*//*Integer.valueOf(preference.getKey())*//*);
                    dialog.show(getFragmentManager(), "dialog");
                    return true;
                }
            });
        }*/
    }

    /*
    private void updateSmscFromBundle(Bundle bundle) {
        if (bundle != null) {
            int sub = bundle.getInt(PhoneConstants.SLOT_KEY, -1);
            if (sub != -1) {
                String summary = bundle.getString(EXTRA_SMSC, null);
                if (summary == null) {
                    return;
                }
                if(DEBUG) Log.d(TAG, "Update SMSC: sub= " + sub + " SMSC= " + summary);
                int end = summary.lastIndexOf("\"");
                mSmscPrefList.get(sub).setSummary(summary.substring(1, end));
            }
        }
    }
    */

    /*
    private static final class SmscHandler extends Handler {
        MyMessagingPreferenceActivity mOwner;
        public SmscHandler(MyMessagingPreferenceActivity owner) {
            super(Looper.getMainLooper());
            mOwner = owner;
        }
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = (Bundle) msg.obj;
            if (bundle == null) {
                return;
            }
            Throwable exception = (Throwable)bundle.getSerializable(EXTRA_EXCEPTION);
            if (exception != null) {
                if(DEBUG) Log.d(TAG, "Error: " + exception);
                mOwner.showToast(R.string.set_smsc_error);
                return;
            }

            Bundle userParams = (Bundle)bundle.getParcelable("userobj");
            if (userParams == null) {
                if(DEBUG) Log.d(TAG, "userParams = null");
                return;
            }
            switch (msg.what) {
                case EVENT_SET_SMSC_DONE:
                    if(DEBUG) Log.d(TAG, "Set SMSC successfully");
                    mOwner.showToast(R.string.set_smsc_success);
                    mOwner.updateSmscFromBundle(userParams);
                    break;
                case EVENT_GET_SMSC_DONE:
                    if(DEBUG) Log.d(TAG, "Get SMSC successfully");
                    int sub = userParams.getInt(PhoneConstants.SLOT_KEY, -1);
                    if (sub != -1) {
                        bundle.putInt(PhoneConstants.SLOT_KEY, sub);
                        mOwner.updateSmscFromBundle(bundle);
                    }
                    break;
                case EVENT_SET_SMSC_PREF_DONE:
                    int key = userParams.getInt(PhoneConstants.SLOT_KEY, -1);
                    if (key != -1) {
                        mOwner.updateSmscFromPreference(key);
                    }
                    break;
            }
        }
    }
    */

    public static class MyEditDialogFragment extends DialogFragment {
        private MyMessagingPreferenceActivity mActivity;

        public static MyEditDialogFragment newInstance(MyMessagingPreferenceActivity activity,
                CharSequence title, CharSequence smsc, int subId) {
            MyEditDialogFragment dialog = new MyEditDialogFragment();
            dialog.mActivity = activity;

            Bundle args = new Bundle();
            args.putCharSequence(SMSC_DIALOG_TITLE, title);
            args.putCharSequence(SMSC_DIALOG_NUMBER, smsc);
            args.putInt(SMSC_DIALOG_SUB, subId);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final int subId = getArguments().getInt(SMSC_DIALOG_SUB);
            if (null == mActivity) {
                mActivity = (MyMessagingPreferenceActivity) getActivity();
                dismiss();
            }
            final EditText edit = new EditText(mActivity);
            edit.setPadding(15, 15, 15, 15);
            CharSequence smsc = getArguments().getCharSequence(SMSC_DIALOG_NUMBER);
            edit.setText(smsc);
            edit.setSelection(smsc.length());
            
            //add by lgy start
            edit.setKeyListener(new NumberKeyListener() {
				protected char[] getAcceptedChars() {
					char[] numberChars = { '1', '2', '3', '4', '5', '6', '7',
							'8', '9', '0', '+', '*', '#' };
					return numberChars;
				}
				@Override
				public int getInputType() {
					// TODO Auto-generated method stub
					return InputType.TYPE_CLASS_PHONE;
				}
			});
            edit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});  
            //add by lgy end

            Dialog alert = new AlertDialog.Builder(mActivity)
                    .setTitle(getArguments().getCharSequence(SMSC_DIALOG_TITLE))
                    .setView(edit)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //lichao modify in 2016-08-20 begin
                            if(edit.getText().length() < 4){
                                Toast.makeText(mActivity, R.string.set_smsc_fewer_then_4, Toast.LENGTH_SHORT).show();
                            }else{
                                MyAlertDialogFragment newFragment = MyAlertDialogFragment.newInstance(
                                        mActivity, subId, edit.getText().toString());
                                newFragment.show(getFragmentManager(), "dialog");
                                dismiss();
                            }
                            //lichao modify in 2016-08-20 end
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setCancelable(true)
                    .create();
            alert.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            return alert;
        }
    }

    /**
     * All subclasses of Fragment must include a public empty constructor. The
     * framework will often re-instantiate a fragment class when needed, in
     * particular during state restore, and needs to be able to find this
     * constructor to instantiate it. If the empty constructor is not available,
     * a runtime exception will occur in some cases during state restore.
     */
    public static class MyAlertDialogFragment extends DialogFragment {
        private MyMessagingPreferenceActivity mActivity;

        public static MyAlertDialogFragment newInstance(MyMessagingPreferenceActivity activity,
                                                        int subId, String smsc) {
            MyAlertDialogFragment dialog = new MyAlertDialogFragment();
            dialog.mActivity = activity;

            Bundle args = new Bundle();
            args.putInt(SMSC_DIALOG_SUB, subId);
            args.putString(SMSC_DIALOG_NUMBER, smsc);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final int subId = getArguments().getInt(SMSC_DIALOG_SUB);
            final String displayedSMSC = getArguments().getString(SMSC_DIALOG_NUMBER);
            if(DEBUG) Log.d(TAG, "MyAlertDialogFragment, displayedSMSC = "+displayedSMSC);

            // When framework re-instantiate this fragment by public empty
            // constructor and call onCreateDialog(Bundle savedInstanceState) ,
            // we should make sure mActivity not null.
            if (null == mActivity) {
                mActivity = (MyMessagingPreferenceActivity) getActivity();
            }

            final String actualSMSC = mActivity.adjustSMSC(displayedSMSC);
            if(DEBUG) Log.d(TAG, "MyAlertDialogFragment, actualSMSC = "+actualSMSC);

            return new AlertDialog.Builder(mActivity)
                    .setIcon(android.R.drawable.ic_dialog_alert).setMessage(
                            R.string.set_smsc_confirm_message)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            /*
                           Bundle userParams = new Bundle();
                           userParams.putInt(PhoneConstants.SLOT_KEY, subId);
                           if (getResources().getBoolean(R.bool.def_enable_reset_smsc)) {
                                final Message callbackMessage = mHandler
                                        .obtainMessage(EVENT_SET_SMSC_PREF_DONE);
                               callbackMessage.obj = userParams;
                               putSmscIntoPref(mActivity,subId,displayedSMSC,callbackMessage);
                           } else {
                               *//*final Message callback = mHandler.obtainMessage(EVENT_SET_SMSC_DONE);
                               userParams.putString(EXTRA_SMSC,actualSMSC);
                               callback.obj = userParams;
                               MessageUtils.setSmscForSub(mActivity, subId, actualSMSC, callback);*//*
                           }
                           */
                            if(DEBUG) Log.d(TAG, "set SMSC from subId= " +subId + " SMSC= " + displayedSMSC);
                            mActivity.setSMSC_Number_MTK(subId, displayedSMSC);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setCancelable(true)
                    .create();
        }
    }

    private String adjustSMSC(String smsc) {
        String actualSMSC = "\"" + smsc + "\"";
        return actualSMSC;
    }

    // For the group mms feature to be enabled, the following must be true:
    //  1. the feature is enabled in mms_config.xml (currently on by default)
    //  2. the feature is enabled in the mms settings page
    //  3. the SIM knows its own phone number
    public static boolean getIsGroupMmsEnabled(Context context) {
        /*SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean groupMmsPrefOn = prefs.getBoolean(
                MyMessagingPreferenceActivity.GROUP_MMS_MODE, true);
        return MmsConfig.getGroupMmsEnabled() &&
                groupMmsPrefOn &&
                !TextUtils.isEmpty(MessageUtils.getLocalNumber());*/
        //tangyisen modify
        return false;
    }

    /*
    private void updateSmscFromPreference(int sub) {
        if (sub == -1) {
            return;
        }
        String smsc = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SMSC_DEFAULT, ""*//*SmsManager.getDefault().getSmscAddressFromIcc()*//*);
        mSmscPrefList.get(sub).setSummary(smsc);
    }
    */

    private void updateCBPref(){
        if (null == mCellBroadcastPrefCategory) {
            return;
        }
        boolean isNotAirPlaneMode = !isAirPlaneModeOn();

        if (MessageUtils.isMultiSimEnabledMms()) {
            mCellBroadcastPrefCategory.removePreference(mCBsettingPref);
            mCellBroadcastPrefCategory.addPreference(mCBsettingSim1Pref);
            mCellBroadcastPrefCategory.addPreference(mCBsettingSim2Pref);
            mCBsettingSim1Pref.setSummary("");
            mCBsettingSim2Pref.setSummary("");
            if(mSubCount <= 0){
                mCBsettingSim1Pref.setEnabled(false);
                mCBsettingSim2Pref.setEnabled(false);
                return;
            }

            boolean isIccCardEnabledSim1 = MessageUtils.isIccCardEnabled(MessageUtils.SUB1);
            mCBsettingSim1Pref.setEnabled(isNotAirPlaneMode && isIccCardEnabledSim1);

            boolean isIccCardEnabledSim2 = MessageUtils.isIccCardEnabled(MessageUtils.SUB2);
            mCBsettingSim2Pref.setEnabled(isNotAirPlaneMode && isIccCardEnabledSim2);

            if(mSubCount == 1){
                int subId = mSubInfoList.get(0).getSubscriptionId();
                boolean isRadioOnSim = MessageUtils.isRadioOn(subId, this);
                if(false == isRadioOnSim){
                    if(true == isIccCardEnabledSim1){
                        mCBsettingSim1Pref.setEnabled(false);
                    }else if(true == isIccCardEnabledSim2){
                        mCBsettingSim2Pref.setEnabled(false);
                    }
                }
                if (FeatureOption.MTK_C2K_SUPPORT && MessageUtils.isUSimType(subId)) {
                    if(isIccCardEnabledSim1){
                        mCBsettingSim1Pref.setSummary(R.string.cdma_not_support);
                        mCBsettingSim1Pref.setEnabled(false);
                        return;
                    }
                    if(isIccCardEnabledSim2){
                        mCBsettingSim2Pref.setSummary(R.string.cdma_not_support);
                        mCBsettingSim2Pref.setEnabled(false);
                    }
                }
                return;
            }
            if(mSubCount >= 2){
                int subId1 = mSubInfoList.get(0).getSubscriptionId();
                if (FeatureOption.MTK_C2K_SUPPORT && MessageUtils.isUSimType(subId1)) {
                    mCBsettingSim1Pref.setSummary(R.string.cdma_not_support);
                    mCBsettingSim1Pref.setEnabled(false);
                }
                boolean isRadioOnSim1 = MessageUtils.isRadioOn(subId1, this);
                if(!isRadioOnSim1){
                    mCBsettingSim1Pref.setEnabled(false);
                }
                int subId2 = mSubInfoList.get(1).getSubscriptionId();
                if (FeatureOption.MTK_C2K_SUPPORT && MessageUtils.isUSimType(subId2)) {
                    mCBsettingSim2Pref.setSummary(R.string.cdma_not_support);
                    mCBsettingSim2Pref.setEnabled(false);
                }
                boolean isRadioOnSim2 = MessageUtils.isRadioOn(subId2, this);
                if(!isRadioOnSim2){
                    mCBsettingSim2Pref.setEnabled(false);
                }
            }
            /*if(MessageUtils.getActivatedIccCardCount() < PhoneConstants.MAX_PHONE_COUNT_DUAL_SIM) {
                int subId = SmsManager.getDefault().getDefaultSmsSubscriptionId();
                int phoneId = SubscriptionManager.getPhoneId(subId);
                mCBsettingPref.setSummary(
                        getString(R.string.pref_summary_cell_broadcast_slot, phoneId + 1));
            } else {
                mCBsettingPref.setSummary(
                        getString(R.string.pref_summary_manage_sim_messages));
            }*/
        } else {
            mCellBroadcastPrefCategory.addPreference(mCBsettingPref);
            mCellBroadcastPrefCategory.removePreference(mCBsettingSim1Pref);
            mCellBroadcastPrefCategory.removePreference(mCBsettingSim2Pref);
            mCBsettingPref.setSummary("");

            boolean isIccCardEnabled =  MessageUtils.hasIccCard();
            mCBsettingPref.setEnabled(isNotAirPlaneMode && isIccCardEnabled);

            if(SimStateMonitor.getInstance().getSubCount()>0){
                int subId = SimStateMonitor.getInstance().getSubInfoList().get(0).getSubscriptionId();
                if (FeatureOption.MTK_C2K_SUPPORT && MessageUtils.isUSimType(subId)) {
                    mCBsettingPref.setSummary(R.string.cdma_not_support);
                    mCBsettingPref.setEnabled(false);
                }/*else{
                    mCBsettingPref.setSummary(R.string.cell_broadcast_settings);
                }*/
            }
        }

    }

    //lichao add in 2017-04-11 begin
    //SMSC: Short Message Service Center
    private String getSMSC_Number_MTK(int subId){
        Bundle result = TelephonyManagerEx.getDefault().getScAddressWithErroCode(subId);
        if (result != null
                && result.getByte(TelephonyManagerEx.GET_SC_ADDRESS_KEY_RESULT)
                == TelephonyManagerEx.ERROR_CODE_NO_ERROR) {
            String scNumber = (String) result
                    .getCharSequence(TelephonyManagerEx.GET_SC_ADDRESS_KEY_ADDRESS);
            if(DEBUG) Log.d(TAG, "getServiceCenterAddress is: " + scNumber);
            return scNumber;
        }
        return "";
    }

    private void setSMSC_Number_MTK(final int subId, final String scNumber){
        //final String scNumber = mNumberText.getText().toString();
        if(DEBUG) Log.d(TAG, "setSMSC_Number_MTK(), subId("+subId+") setScAddress: " + scNumber);
        new Thread(new Runnable() {
            public void run() {
                TelephonyManagerEx.getDefault().setScAddress(subId, scNumber);
                //HB. Comments :  , Engerineer : lichao , Date : 17-6-5 , begin
                mHandler.postDelayed(mUpdateSMSCPrefRunnable, 200);
                //HB. end
            }
        }).start();
    }
    //lichao add in 2017-04-11 end

    //lichao add in 2017-04-14
    private String getSMSC_SummaryString(int subId, boolean isSimSupported) {
        String summary;
        if(isSimSupported){
            summary = getSMSC_Number_MTK(subId);
        }else{
            summary = getString(R.string.cdma_not_support);
        }
        return summary;
    }

    //lichao add in 2017-04-14
    private boolean isSimSupported(int subId) {
        boolean isSimSupported = true;
        if(null == mOpSubSelectActivityExt){
            return isSimSupported;
        }
        try {
            isSimSupported = mOpSubSelectActivityExt.isSimSupported(subId);
        } catch (Exception e) {
            Log.e(TAG, "isSimSupported() Exception: " + e);
        }
        return isSimSupported;
    }

    //lichao add in 2017-04-14
    private void onCBsettingPrefClicked(SubscriptionInfo subInfo){
        int subId = subInfo.getSubscriptionId();
        String displayName = subInfo.getDisplayName().toString();
        if (FeatureOption.MTK_C2K_SUPPORT && MessageUtils.isUSimType(subId)) {
            showToast(R.string.cdma_not_support);
        } else {
            Intent it = new Intent();
            it.setClass(this, CellBroadcastActivity.class);
            it.putExtra(PhoneConstants.SUBSCRIPTION_KEY, subId);
            it.putExtra(SUB_TITLE_NAME, displayName);
            startActivity(it);
        }
    }

    private void updateAllPref(){
        mSubInfoList = SimStateMonitor.getInstance().getSubInfoList();
        mSubCount = 0;
        if (null != mSubInfoList) {
            mSubCount = mSubInfoList.size();
        }
        Log.d(TAG, "updateSIMSMSPref, mSubCount = " + mSubCount);
        updateSIMSMSPref();
        updateSMSCPref();
        updateCBPref();
        updateDeliveryReportPref();
        updateAutoRetrievialPref();
        updateWakeUpPref();
        updateServiceCenterDialog();
    }

    //HB. Comments :  , Engerineer : lichao , Date : 17-5-22 , begin
    private void updateDeliveryReportPref(){
        mSmsDeliveryReportPref.setEnabled(!isAirPlaneModeOn() && MessageUtils.hasIccCard());
    }

    private void updateAutoRetrievialPref(){
        mMmsAutoRetrievialPref.setEnabled(!isAirPlaneModeOn() && MessageUtils.hasIccCard());
    }

    private void updateWakeUpPref(){
        mWakeUpPref.setEnabled(!isAirPlaneModeOn() && MessageUtils.hasIccCard());
    }
    //HB. end

    //HB. Comments :  , Engerineer : lichao , Date : 17-6-5 , begin
    private Runnable mUpdateSMSCPrefRunnable = new Runnable() {
        @Override
        public void run() {
            updateSMSCPref();
        }
    };
    //HB. end

    //HB. Comments :  , Engerineer : lichao , Date : 17-7-4 , begin
    private void showEditServiceCenterDialog(int subId, CharSequence title) {
        mShowDialogSubId = subId;
        mServiceCenterDialog = MyEditDialogFragment.newInstance(
                MyMessagingPreferenceActivity.this,
                title,
                getSMSC_Number_MTK(subId),
                subId);
        mServiceCenterDialog.show(getFragmentManager(), "dialog");
    }
    //HB. end

    //HB. Comments :  , Engerineer : lichao , Date : 17-7-20 , begin
    private void updateServiceCenterDialog(){
        if(null == mServiceCenterDialog || false == mServiceCenterDialog.getShowsDialog()){
            return;
        }
        int slotIdx = SubscriptionManager.getSlotId(mShowDialogSubId);
        boolean isIccCardEnabledSimX = MessageUtils.isIccCardEnabled(slotIdx);
        if(!isIccCardEnabledSimX || isAirPlaneModeOn() || !MessageUtils.hasIccCard()){
            mServiceCenterDialog.dismiss();
        }
    }
    //HB. end

}
