/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2006 The Android Open Source Project
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
package com.android.settings.sim;

import com.android.ims.ImsManager;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;

import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.phone.PhoneFeatureConstants.FeatureOption;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.phone.ext.IMobileNetworkSettingsExt;
import com.mediatek.settings.Enhanced4GLteSwitchPreference;
import com.mediatek.settings.MobileNetworkSettingsOmEx;
import com.mediatek.settings.TelephonyUtils;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.cdma.CdmaNetworkSettings;
import com.mediatek.settings.cdma.CdmaUtils;
import com.mediatek.settings.cdma.TelephonyUtilsEx;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.ext.ISimManagementExt;
import com.mediatek.settings.sim.PhoneServiceStateHandler;
import com.mediatek.settings.sim.SimHotSwapHandler;
import com.mediatek.settings.sim.SimHotSwapHandler.OnSimHotSwapListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.ActionBar;
import hb.app.dialog.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import hb.preference.ListPreference;
import hb.preference.Preference;
import hb.preference.PreferenceActivity;
import hb.preference.PreferenceCategory;
import hb.preference.PreferenceGroup;
import hb.preference.PreferenceScreen;
import hb.preference.SwitchPreference;
import android.telephony.CarrierConfigManager;
import android.provider.Settings;
import android.telephony.RadioAccessFamily;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.android.phone.*;

import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import com.mediatek.telecom.TelecomManagerEx;

/**
 * "Mobile network settings" screen.  This preference screen lets you
 * enable/disable mobile data, and control data roaming and other
 * network-specific mobile data features.  It's used on non-voice-capable
 * tablets as well as regular phone devices.
 *
 * Note that this PreferenceActivity is part of the phone app, even though
 * you reach it from the "Wireless & Networks" section of the main
 * Settings app.  It's not part of the "Call settings" hierarchy that's
 * available from the Phone app (see CallFeaturesSetting for that.)
 */
public class SimSettings extends PreferenceActivity implements
        DialogInterface.OnClickListener, DialogInterface.OnDismissListener,
        Preference.OnPreferenceChangeListener,  PhoneServiceStateHandler.Listener {

    // debug data
    private static final String LOG_TAG = "NetworkSettings";
    private static final boolean DBG = true;
    public static final int REQUEST_CODE_EXIT_ECM = 17;

    //String keys for preference lookup
    public static final String BUTTON_PREFERED_NETWORK_MODE = "preferred_network_mode_key";
    private static final String BUTTON_ROAMING_KEY = "button_roaming_key";
    private static final String BUTTON_CDMA_LTE_DATA_SERVICE_KEY = "cdma_lte_data_service_key";
    public static final String BUTTON_ENABLED_NETWORKS_KEY = "enabled_networks_key";
    private static final String BUTTON_4G_LTE_KEY = "enhanced_4g_lte";
    private static final String BUTTON_CELL_BROADCAST_SETTINGS = "cell_broadcast_settings";
    private static final String BUTTON_APN_EXPAND_KEY = "button_apn_key";
    private static final String BUTTON_OPERATOR_SELECTION_EXPAND_KEY = "button_carrier_sel_key";
    private static final String BUTTON_CARRIER_SETTINGS_KEY = "carrier_settings_key";
    private static final String BUTTON_CDMA_SYSTEM_SELECT_KEY = "cdma_system_select_key";

    static final int preferredNetworkMode = Phone.PREFERRED_NT_MODE;

    //Information about logical "up" Activity
    private static final String UP_ACTIVITY_PACKAGE = "com.android.settings";
    private static final String UP_ACTIVITY_CLASS =
            "com.android.settings.Settings$WirelessSettingsActivity";

    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;

    //UI objects
    private ListPreference mButtonPreferredNetworkMode;
    private ListPreference mButtonEnabledNetworks;
    private SwitchPreference mButtonDataRoam;
    private SwitchPreference mButton4glte;
    private Preference mLteDataServicePref;

    private static final String iface = "rmnet0"; //TODO: this will go away
    private List<SubscriptionInfo> mActiveSubInfos;

    private UserManager mUm;
    private Phone mPhone;
    private MyHandler mHandler;
    private boolean mOkClicked;

    //GsmUmts options and Cdma options
    GsmUmtsOptions mGsmUmtsOptions;
    CdmaOptions mCdmaOptions;

    private Preference mClickedPreference;
    private boolean mShow4GForLTE;
    private boolean mIsGlobalCdma;
    private boolean mUnavailable;

    /// Add for C2K OM features
    private CdmaNetworkSettings mCdmaNetworkSettings;

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        /*
         * Enable/disable the 'Enhanced 4G LTE Mode' when in/out of a call
         * and depending on TTY mode and TTY support over VoLTE.
         * @see android.telephony.PhoneStateListener#onCallStateChanged(int,
         * java.lang.String)
         */
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (DBG) log("PhoneStateListener.onCallStateChanged: state=" + state);

            updateScreenStatus();

            boolean enabled = (state == TelephonyManager.CALL_STATE_IDLE) &&
                    ImsManager.isNonTtyOrTtyOnVolteEnabled(getApplicationContext());
            Preference pref = getPreferenceScreen().findPreference(BUTTON_4G_LTE_KEY);
            if (pref != null) pref.setEnabled(enabled && hasActiveSubscriptions());
        }
    };

    /// M: Replaced with mReceiver
    /*private final BroadcastReceiver mPhoneChangeReceiver = new PhoneChangeReceiver();

    private class PhoneChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DBG) log("onReceive:");
            // When the radio changes (ex: CDMA->GSM), refresh all options.
            mGsmUmtsOptions = null;
            mCdmaOptions = null;
            updateBody();
        }
    }*/

    //This is a method implemented for DialogInterface.OnClickListener.
    //  Used to dismiss the dialogs when they come up.
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            mPhone.setDataRoamingEnabled(true);
            mOkClicked = true;
        } else {
            // Reset the toggle
            mButtonDataRoam.setChecked(false);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        // Assuming that onClick gets called first
        mButtonDataRoam.setChecked(mOkClicked);
    }

    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceActivity's implementation.  Used to make sure we track the
     * preference click events.
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        /** TODO: Refactor and get rid of the if's using subclasses */
        final int phoneSubId = mPhone.getSubId();
        final Context context = this;
        Intent simIntent = new Intent(context, SimDialogActivity.class);
        simIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        if (mCdmaNetworkSettings != null &&
            mCdmaNetworkSettings.onPreferenceTreeClick(preferenceScreen, preference)) {
            return true;
        }
        /// M: Add for Plug-in @{
        if (mExt.onPreferenceTreeClick(preferenceScreen, preference)) {
            return true;
        } else
        /// @}
        if (preference.getKey().equals(BUTTON_4G_LTE_KEY)) {
            return true;
        } else if (mGsmUmtsOptions != null &&
                mGsmUmtsOptions.preferenceTreeClick(preference) == true) {
            return true;
        } else if (mCdmaOptions != null &&
                   mCdmaOptions.preferenceTreeClick(preference) == true) {
            if (Boolean.parseBoolean(
                    SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {

                mClickedPreference = preference;

                // In ECM mode launch ECM app dialog
                startActivityForResult(
                    new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                    REQUEST_CODE_EXIT_ECM);
            }
            return true;
        } else if (preference == mButtonPreferredNetworkMode) {
            //displays the value taken from the Settings.System
            int settingsNetworkMode = android.provider.Settings.Global.getInt(mPhone.getContext().
                    getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                    preferredNetworkMode);
            mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
            return true;
        } else if (preference == mLteDataServicePref) {
            String tmpl = android.provider.Settings.Global.getString(getContentResolver(),
                        android.provider.Settings.Global.SETUP_PREPAID_DATA_SERVICE_URL);
            if (!TextUtils.isEmpty(tmpl)) {
                TelephonyManager tm = (TelephonyManager) getSystemService(
                        Context.TELEPHONY_SERVICE);
                String imsi = tm.getSubscriberId();
                if (imsi == null) {
                    imsi = "";
                }
                final String url = TextUtils.isEmpty(tmpl) ? null
                        : TextUtils.expandTemplate(tmpl, imsi).toString();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } else {
                android.util.Log.e(LOG_TAG, "Missing SETUP_PREPAID_DATA_SERVICE_URL");
            }
            return true;
        }  else if (preference == mButtonEnabledNetworks) {
            int settingsNetworkMode = android.provider.Settings.Global.getInt(mPhone.getContext().
                            getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                    preferredNetworkMode);
            /** M: Remove this for LW project, for we need set a temple value.
            mButtonEnabledNetworks.setValue(Integer.toString(settingsNetworkMode));
             */
            return true;
        } else if (preference == mButtonDataRoam) {
            // Do not disable the preference screen if the user clicks Data roaming.
            return true;
        } else if (findPreference(KEY_CELLULAR_DATA) == preference) {
        	simIntent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.DATA_PICK);
            context.startActivity(simIntent);
            return true;
        } else if (findPreference(KEY_CALLS) == preference) {
        	simIntent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.CALLS_PICK);
            context.startActivity(simIntent);
            return true;
        } else if (findPreference(KEY_SMS) == preference) {
        	simIntent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.SMS_PICK);
            context.startActivity(simIntent);
            return true;
        } else if (mSim1 == preference || mSim2 == preference || mData == preference) {
        	 return false;
        } else {
            // if the button is anything but the simple toggle preference,
            // we'll need to disable all preferences to reject all click
            // events until the sub-activity's UI comes up.
            preferenceScreen.setEnabled(false);
            // Let the intents be launched by the Preference manager
            return false;
        }
    }

    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener
            = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            if (DBG) log("onSubscriptionsChanged: start");
            updateSubscriptions();
            /// M: add for hot swap @{
            if (TelephonyUtils.isHotSwapHanppened(
                    mActiveSubInfos, PhoneUtils.getActiveSubInfoList())) {
                log("onSubscriptionsChanged:hot swap hanppened");
                dissmissDialog(mButtonPreferredNetworkMode);
                dissmissDialog(mButtonEnabledNetworks);
                finish();
                return;
            }
            /// @}
            log("onSubscriptionsChanged: end");
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        if (DBG) log("onCreate:+");
        //zhangcj deleete for UI
       // setTheme(R.style.Theme_Material_Settings);
        super.onCreate(icicle);
        mContext = this;
        /// Add for cmcc open market @{
        mOmEx = new MobileNetworkSettingsOmEx(this);
        /// @}

        mHandler = new MyHandler();
        mUm = (UserManager) getSystemService(Context.USER_SERVICE);
        mSubscriptionManager = SubscriptionManager.from(this);
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);       
       
        mPhone =  PhoneFactory.getPhone(mSubscriptionManager.getDefaultDataPhoneId());
        if(mPhone == null) {
        	mPhone = PhoneFactory.getDefaultPhone();
        }
        mSlotId = mPhone.getPhoneId();

        if (mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)) {
            mUnavailable = true;
            setContentView(R.layout.telephony_disallowed_preference_screen);
            return;
        }

        addPreferencesFromResource(R.xml.sim_settings);

        mButton4glte = (SwitchPreference)findPreference(BUTTON_4G_LTE_KEY);
        mButton4glte.setOnPreferenceChangeListener(this);

        try {
            Context con = createPackageContext("com.android.systemui", 0);
            int id = con.getResources().getIdentifier("config_show4GForLTE",
                    "bool", "com.android.systemui");
            mShow4GForLTE = con.getResources().getBoolean(id);
        } catch (NameNotFoundException e) {
            loge("NameNotFoundException for show4GFotLTE");
            mShow4GForLTE = false;
        }

        //get UI object references
        PreferenceScreen prefSet = getPreferenceScreen();

        mButtonDataRoam = (SwitchPreference) prefSet.findPreference(BUTTON_ROAMING_KEY);
        mButtonPreferredNetworkMode = (ListPreference) prefSet.findPreference(
                BUTTON_PREFERED_NETWORK_MODE);
        mButtonEnabledNetworks = (ListPreference) prefSet.findPreference(
                BUTTON_ENABLED_NETWORKS_KEY);
        mButtonDataRoam.setOnPreferenceChangeListener(this);

        mLteDataServicePref = prefSet.findPreference(BUTTON_CDMA_LTE_DATA_SERVICE_KEY);

        // Initialize mActiveSubInfo
        int max = mSubscriptionManager.getActiveSubscriptionInfoCountMax();
        mActiveSubInfos = new ArrayList<SubscriptionInfo>(max);
        
        List<SubscriptionInfo> sil = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (sil != null) {
            mActiveSubInfos.addAll(sil);
        }

        initIntentFilter();
        registerReceiver(mReceiver, mIntentFilter);
        mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        mTelephonyManager.listen(
                mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        if (DBG) log("onCreate:-");
        
        initPreference();
        updateBody();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // M: replace with mReceiver
        //unregisterReceiver(mPhoneChangeReceiver);
        unregisterReceiver(mReceiver);

        log("onDestroy " + this);
        if (mCdmaNetworkSettings != null) {
            mCdmaNetworkSettings.onDestroy();
            mCdmaNetworkSettings = null;
        }
        mSubscriptionManager.removeOnSubscriptionsChangedListener(
                mOnSubscriptionsChangeListener);
        mTelephonyManager.listen(
                mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        /// M: For plugin to unregister listener
        mExt.unRegister();
        /// Add for cmcc open market @{
        mOmEx.unRegister();
        /// @}
        
        this.unregisterReceiver(mSimReceiver);
        mSimHotSwapHandler.unregisterOnSimHotSwap();
        mStateHandler.unregisterOnPhoneServiceStateChange();
        mTelephonyManager.listen(
                mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        getContentResolver().unregisterContentObserver(mSettingObserver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mCdmaNetworkSettings != null) {
            mCdmaNetworkSettings.onResume();
        }

        if (DBG) log("onResume:+");

        if (mUnavailable) {
            if (DBG) log("onResume:- ignore mUnavailable == false");
            return;
        }

        // upon resumption from the sub-activity, make sure we re-enable the
        // preferences.
        // getPreferenceScreen().setEnabled(true);

        // Set UI state in onResume because a user could go home, launch some
        // app to change this setting's backend, and re-launch this settings app
        // and the UI state would be inconsistent with actual state
        mButtonDataRoam.setChecked(mPhone.getDataRoamingEnabled());

        if (getPreferenceScreen().findPreference(BUTTON_PREFERED_NETWORK_MODE) != null
                || getPreferenceScreen().findPreference(BUTTON_ENABLED_NETWORKS_KEY) != null)  {
            updatePreferredNetworkUIFromDb();
        }

        /** M: Add For [MTK_Enhanced4GLTE]
        if (ImsManager.isVolteEnabledByPlatform(this)
                && ImsManager.isVolteProvisionedOnDevice(this)) {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }

        // NOTE: Buttons will be enabled/disabled in mPhoneStateListener
        boolean enh4glteMode = ImsManager.isEnhanced4gLteModeSettingEnabledByUser(this)
                && ImsManager.isNonTtyOrTtyOnVolteEnabled(this);
        mButton4glte.setChecked(enh4glteMode);

        mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        @} */
        /// M: For screen update
        updateScreenStatus();
        /// M: For plugin to update UI
        mExt.onResume();
        
        /// M: for [Tablet]
        removeItemsForTablet();

        //modify by lgy
        mHandler.post(new Runnable(){
        	public void run(){
                updateSubscriptions();
        	}
        });

        /// M: for Plug-in @{
        //delete by lgy
//        customizeSimDisplay();
        mSimManagementExt.onResume(this);
        /// @}

        if (DBG) log("onResume:-");

    }

    private boolean hasActiveSubscriptions() {
        return mActiveSubInfos.size() > 0;
    }

    private void updateBody() {
        mExt = ExtensionManager.getMobileNetworkSettingsExt();
        final Context context = getApplicationContext();
        PreferenceScreen prefSet = getPreferenceScreen();
        boolean isLteOnCdma = mPhone.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE;
        final int phoneSubId = mPhone.getSubId();

        if (DBG) {
            log("updateBody: isLteOnCdma=" + isLteOnCdma + " phoneSubId=" + phoneSubId);
        }

        if (prefSet != null) {
            prefSet.removeAll();
            prefSet.addPreference(mBasicSettingCategory);
            prefSet.addPreference(mNetSettingCategory);
//            prefSet.addPreference(mData);            
            prefSet.addPreference(mButtonDataRoam);
            prefSet.addPreference(mButtonPreferredNetworkMode);
            prefSet.addPreference(mButtonEnabledNetworks);
            prefSet.addPreference(mButton4glte);
            prefSet.addPreference(mSelectData);
            prefSet.addPreference(mSelectCall);
        }

        int settingsNetworkMode = android.provider.Settings.Global.getInt(
                mPhone.getContext().getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                preferredNetworkMode);

        PersistableBundle carrierConfig =
                PhoneGlobals.getInstance().getCarrierConfigForSubId(mPhone.getSubId());
        mIsGlobalCdma = isLteOnCdma
                && carrierConfig.getBoolean(CarrierConfigManager.KEY_SHOW_CDMA_CHOICES_BOOL);
        if (carrierConfig.getBoolean(CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL)) {
            prefSet.removePreference(mButtonPreferredNetworkMode);
            prefSet.removePreference(mButtonEnabledNetworks);
            prefSet.removePreference(mLteDataServicePref);
        } else if (carrierConfig.getBoolean(CarrierConfigManager.KEY_WORLD_PHONE_BOOL) == true) {
            prefSet.removePreference(mButtonEnabledNetworks);
            // set the listener for the mButtonPreferredNetworkMode list preference so we can issue
            // change Preferred Network Mode.
            mButtonPreferredNetworkMode.setOnPreferenceChangeListener(this);

            mCdmaOptions = new CdmaOptions(this, prefSet, mPhone);
            mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet, phoneSubId);
        } else {
            log("updatebody is not world phone");
            prefSet.removePreference(mButtonPreferredNetworkMode);
            final int phoneType = mPhone.getPhoneType();

            if (TelephonyUtilsEx.isCDMAPhone(mPhone)) {
                log("phoneType == PhoneConstants.PHONE_TYPE_CDMA");

                int lteForced = android.provider.Settings.Global.getInt(
                        mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.LTE_SERVICE_FORCED + mPhone.getSubId(),
                        0);

                if (isLteOnCdma) {
                    if (lteForced == 0) {
                        mButtonEnabledNetworks.setEntries(
                                R.array.enabled_networks_cdma_choices);
                        mButtonEnabledNetworks.setEntryValues(
                                R.array.enabled_networks_cdma_values);
                    } else {
                        switch (settingsNetworkMode) {
                            case Phone.NT_MODE_CDMA:
                            case Phone.NT_MODE_CDMA_NO_EVDO:
                            case Phone.NT_MODE_EVDO_NO_CDMA:
                                mButtonEnabledNetworks.setEntries(
                                        R.array.enabled_networks_cdma_no_lte_choices);
                                mButtonEnabledNetworks.setEntryValues(
                                        R.array.enabled_networks_cdma_no_lte_values);
                                break;
                            case Phone.NT_MODE_GLOBAL:
                            case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                            case Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                            case Phone.NT_MODE_LTE_ONLY:
                                mButtonEnabledNetworks.setEntries(
                                        R.array.enabled_networks_cdma_only_lte_choices);
                                mButtonEnabledNetworks.setEntryValues(
                                        R.array.enabled_networks_cdma_only_lte_values);
                                break;
                            default:
                                mButtonEnabledNetworks.setEntries(
                                        R.array.enabled_networks_cdma_choices);
                                mButtonEnabledNetworks.setEntryValues(
                                        R.array.enabled_networks_cdma_values);
                                break;
                        }
                    }
                }
                mCdmaOptions = new CdmaOptions(this, prefSet, mPhone);

                // In World mode force a refresh of GSM Options.
                if (isWorldMode()) {
                    mGsmUmtsOptions = null;
                }
                /// M: support for cdma @{
                if (FeatureOption.isMtk3gDongleSupport()) {
                    PreferenceScreen activateDevice = (PreferenceScreen)
                            prefSet.findPreference(BUTTON_CDMA_ACTIVATE_DEVICE_KEY);
                    if (activateDevice != null) {
                        prefSet.removePreference(activateDevice);
                    }
                }
                /// @}
            } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                if (!carrierConfig.getBoolean(CarrierConfigManager.KEY_PREFER_2G_BOOL)
                        && !getResources().getBoolean(R.bool.config_enabled_lte)) {
                    mButtonEnabledNetworks.setEntries(
                            R.array.enabled_networks_except_gsm_lte_choices);
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_except_gsm_lte_values);
                } else if (!carrierConfig.getBoolean(CarrierConfigManager.KEY_PREFER_2G_BOOL)) {
                    int select = (mShow4GForLTE == true) ?
                            R.array.enabled_networks_except_gsm_4g_choices
                            : R.array.enabled_networks_except_gsm_choices;
                    mButtonEnabledNetworks.setEntries(select);
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_except_gsm_values);
                } else if (!FeatureOption.isMtkLteSupport()) {
                    mButtonEnabledNetworks.setEntries(
                            R.array.enabled_networks_except_lte_choices);
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_except_lte_values);
                } else if (mIsGlobalCdma) {
                    mButtonEnabledNetworks.setEntries(
                            R.array.enabled_networks_cdma_choices);
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_cdma_values);
                } else {
                    int select = (mShow4GForLTE == true) ? R.array.enabled_networks_4g_choices
                            : R.array.enabled_networks_choices;
                    mButtonEnabledNetworks.setEntries(select);
                    mExt.changeEntries(mButtonEnabledNetworks);
                    /// Add for C2K @{
                    if (isC2kLteSupport()) {
                        log("Change to C2K values");
                        mButtonEnabledNetworks.setEntryValues(
                                R.array.enabled_networks_values_c2k);
                    } else {
                        mButtonEnabledNetworks.setEntryValues(
                                R.array.enabled_networks_values);
                    }
                    /// @}
                }
                mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet, phoneSubId);
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }
            if (isWorldMode()) {
                mButtonEnabledNetworks.setEntries(
                        R.array.preferred_network_mode_choices_world_mode);
                mButtonEnabledNetworks.setEntryValues(
                        R.array.preferred_network_mode_values_world_mode);
            }
            mButtonEnabledNetworks.setOnPreferenceChangeListener(this);
            if (DBG) log("settingsNetworkMode: " + settingsNetworkMode);
        }

        final boolean missingDataServiceUrl = TextUtils.isEmpty(
                android.provider.Settings.Global.getString(getContentResolver(),
                        android.provider.Settings.Global.SETUP_PREPAID_DATA_SERVICE_URL));
        if (!isLteOnCdma || missingDataServiceUrl) {
            prefSet.removePreference(mLteDataServicePref);
        } else {
            android.util.Log.d(LOG_TAG, "keep ltePref");
        }

        /// M: add mtk feature.
        onCreateMTK(prefSet);

        // Enable enhanced 4G LTE mode settings depending on whether exists on platform
        /** M: Add For [MTK_Enhanced4GLTE] @{
        if (!(ImsManager.isVolteEnabledByPlatform(this)
                && ImsManager.isVolteProvisionedOnDevice(this))) {
            Preference pref = prefSet.findPreference(BUTTON_4G_LTE_KEY);
            if (pref != null) {
                prefSet.removePreference(pref);
            }
        }
        @} */

        final boolean isSecondaryUser = UserHandle.myUserId() != UserHandle.USER_OWNER;
        // Enable link to CMAS app settings depending on the value in config.xml.
        final boolean isCellBroadcastAppLinkEnabled = this.getResources().getBoolean(
                com.android.internal.R.bool.config_cellBroadcastAppLinks);
        if (isSecondaryUser || !isCellBroadcastAppLinkEnabled
                || mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_CELL_BROADCASTS)) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = findPreference(BUTTON_CELL_BROADCAST_SETTINGS);
            if (ps != null) {
                root.removePreference(ps);
            }
        }

        // Get the networkMode from Settings.System and displays it
        mButtonDataRoam.setChecked(mPhone.getDataRoamingEnabled());

        mButtonEnabledNetworks.setValue(Integer.toString(settingsNetworkMode));
        mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
        UpdatePreferredNetworkModeSummary(settingsNetworkMode);
        UpdateEnabledNetworksValueAndSummary(settingsNetworkMode);
        // Display preferred network type based on what modem returns b/18676277
        /// M: no need set mode here
        //mPhone.setPreferredNetworkType(settingsNetworkMode, mHandler
        //        .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));

        /**
         * Enable/disable depending upon if there are any active subscriptions.
         *
         * I've decided to put this enable/disable code at the bottom as the
         * code above works even when there are no active subscriptions, thus
         * putting it afterwards is a smaller change. This can be refined later,
         * but you do need to remember that this all needs to work when subscriptions
         * change dynamically such as when hot swapping sims.

        boolean hasActiveSubscriptions = hasActiveSubscriptions();
        TelephonyManager tm = (TelephonyManager) getSystemService(
                Context.TELEPHONY_SERVICE);
        boolean canChange4glte = (tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) &&
                ImsManager.isNonTtyOrTtyOnVolteEnabled(getApplicationContext());
        mButtonDataRoam.setEnabled(hasActiveSubscriptions);
        mButtonPreferredNetworkMode.setEnabled(hasActiveSubscriptions);
        mButtonEnabledNetworks.setEnabled(hasActiveSubscriptions);
        mButton4glte.setEnabled(hasActiveSubscriptions && canChange4glte);
        mLteDataServicePref.setEnabled(hasActiveSubscriptions);
        Preference ps;
        PreferenceScreen root = getPreferenceScreen();
        ps = findPreference(BUTTON_CELL_BROADCAST_SETTINGS);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }
        ps = findPreference(BUTTON_APN_EXPAND_KEY);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }
        ps = findPreference(BUTTON_OPERATOR_SELECTION_EXPAND_KEY);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }
        ps = findPreference(BUTTON_CARRIER_SETTINGS_KEY);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }
        ps = findPreference(BUTTON_CDMA_SYSTEM_SELECT_KEY);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }*/
        /// M: For C2K solution 1.5 @{
        if (!FeatureOption.isMtkSvlteSolutionSupport() && TelephonyUtilsEx.isCGCardInserted()) {
            if (TelephonyUtilsEx.isCapabilityOnGCard()
                    && !TelephonyUtilsEx.isGCardInserted(mPhone.getPhoneId())) {
                PreferenceScreen prefScreen = getPreferenceScreen();
                for (int i = 0; i < prefScreen.getPreferenceCount(); i++) {
                    Preference pref = prefScreen.getPreference(i);
                    pref.setEnabled(false);
                }
            } else {
                if(mButtonDataRoam != null) {
                    mButtonDataRoam.setEnabled(true);
                }
            }
        }
        /// @}
        /// M: Add for L+W DSDS.
        updateNetworkModeForLwDsds();
        /// M: Add for Plug-in @{
        if (mButtonEnabledNetworks != null) {
            log("Enter plug-in update updateNetworkTypeSummary - Enabled again!");
            mExt.updateNetworkTypeSummary(mButtonEnabledNetworks);
            /// Add for cmcc open market @{
            mOmEx.updateNetworkTypeSummary(mButtonEnabledNetworks);
            /// @}
        }
        /// @}
    }

    /* M: move unregister Subscriptions Change Listener to onDestory
    @Override
    protected void onPause() {
        super.onPause();
        if (DBG) log("onPause:+");

        if (ImsManager.isVolteEnabledByPlatform(this)
                && ImsManager.isVolteProvisionedOnDevice(this)) {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        mSubscriptionManager
            .removeOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        if (DBG) log("onPause:-");
    }*/

    /**
     * Implemented to support onPreferenceChangeListener to look for preference
     * changes specifically on CLIR.
     *
     * @param preference is the preference to be changed, should be mButtonCLIR.
     * @param objValue should be the value of the selection, NOT its localized
     * display value.
     */
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final int phoneSubId = mPhone.getSubId();
        if (onPreferenceChangeMTK(preference, objValue)) {
            return true;
        }
        if (preference == mButtonPreferredNetworkMode) {
            //NOTE onPreferenceChange seems to be called even if there is no change
            //Check if the button value is changed from the System.Setting
            mButtonPreferredNetworkMode.setValue((String) objValue);
            int buttonNetworkMode;
            buttonNetworkMode = Integer.valueOf((String) objValue).intValue();
            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                    preferredNetworkMode);

            log("onPreferenceChange buttonNetworkMode:"
                    + buttonNetworkMode + " settingsNetworkMode:" + settingsNetworkMode);

            if (buttonNetworkMode != settingsNetworkMode) {
                int modemNetworkMode;
                // if new mode is invalid ignore it
                switch (buttonNetworkMode) {
                    case Phone.NT_MODE_WCDMA_PREF:
                    case Phone.NT_MODE_GSM_ONLY:
                    case Phone.NT_MODE_WCDMA_ONLY:
                    case Phone.NT_MODE_GSM_UMTS:
                    case Phone.NT_MODE_CDMA:
                    case Phone.NT_MODE_CDMA_NO_EVDO:
                    case Phone.NT_MODE_EVDO_NO_CDMA:
                    case Phone.NT_MODE_GLOBAL:
                    case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                    case Phone.NT_MODE_LTE_GSM_WCDMA:
                    case Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                    case Phone.NT_MODE_LTE_ONLY:
                    case Phone.NT_MODE_LTE_WCDMA:
                        // This is one of the modes we recognize
                        modemNetworkMode = buttonNetworkMode;
                        break;
                    default:
                        loge("Invalid Network Mode (" + buttonNetworkMode + ") chosen. Ignore.");
                        return true;
                }

                mButtonPreferredNetworkMode.setValue(Integer.toString(modemNetworkMode));
                mButtonPreferredNetworkMode.setSummary(mButtonPreferredNetworkMode.getEntry());
                log(":::onPreferenceChange: summary: " + mButtonPreferredNetworkMode.getEntry());

                android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                        buttonNetworkMode );

                if (DBG) log("setPreferredNetworkType, networkType: " + modemNetworkMode);

                //Set the modem network mode
                mPhone.setPreferredNetworkType(modemNetworkMode, mHandler
                        .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
            }
        } else if (preference == mButtonEnabledNetworks) {
            mButtonEnabledNetworks.setValue((String) objValue);
            int buttonNetworkMode = Integer.valueOf((String) objValue).intValue();
            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                    preferredNetworkMode);

            if (DBG) log("buttonNetworkMode: " + buttonNetworkMode +
                    "settingsNetworkMode: " + settingsNetworkMode);

            if (buttonNetworkMode != settingsNetworkMode) {
                int modemNetworkMode;
                // if new mode is invalid ignore it
                switch (buttonNetworkMode) {
                    case Phone.NT_MODE_WCDMA_PREF:
                    case Phone.NT_MODE_GSM_ONLY:
                    case Phone.NT_MODE_LTE_GSM_WCDMA:
                    case Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                    case Phone.NT_MODE_CDMA:
                    case Phone.NT_MODE_CDMA_NO_EVDO:
                    case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                    /// M: Add for C2K
                    case Phone.NT_MODE_GLOBAL:
                        // This is one of the modes we recognize
                        modemNetworkMode = buttonNetworkMode;
                        break;
                    default:
                        loge("Invalid Network Mode (" + buttonNetworkMode + ") chosen. Ignore.");
                        return true;
                }

                UpdateEnabledNetworksValueAndSummary(buttonNetworkMode);
                android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                        buttonNetworkMode );

                if (DBG) log("setPreferredNetworkType, networkType: " + modemNetworkMode);

                //Set the modem network mode
                mPhone.setPreferredNetworkType(modemNetworkMode, mHandler
                        .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
            }
        } else if (preference == mButton4glte) {
            SwitchPreference enhanced4gModePref = (SwitchPreference) preference;
            boolean enhanced4gMode = !enhanced4gModePref.isChecked();
            enhanced4gModePref.setChecked(enhanced4gMode);
            ImsManager.setEnhanced4gLteModeSetting(this, enhanced4gModePref.isChecked());
        } else if (preference == mButtonDataRoam) {
            if (DBG) log("onPreferenceTreeClick: preference == mButtonDataRoam.");

            //normally called on the toggle click
            if (!mButtonDataRoam.isChecked()) {
                // First confirm with a warning dialog about charges
                mOkClicked = false;
                /// M:Add for plug-in @{
                /* Google Code, delete by MTK
                new AlertDialog.Builder(this).setMessage(
                        getResources().getString(R.string.roaming_warning))
                        .setTitle(android.R.string.dialog_alert_title)
                */
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getResources().getString(R.string.roaming_warning))
                        .setTitle(android.R.string.dialog_alert_title);
                mExt.customizeAlertDialog(mButtonDataRoam, builder);
                builder.setIconAttribute(android.R.attr.alertDialogIcon)
                        .setPositiveButton(android.R.string.yes, this)
                        .setNegativeButton(android.R.string.no, this)
                        .show()
                        .setOnDismissListener(this);
                /// @}
            } else {
                mPhone.setDataRoamingEnabled(false);
            }
            return true;
        }

        /// Add for Plug-in @{
        mExt.onPreferenceChange(preference, objValue);
        /// @}
        /// M: no need updateBody here
        //updateBody();
        // always let the preference setting proceed.
        return true;
    }

    private class MyHandler extends Handler {

        static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 0;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SET_PREFERRED_NETWORK_TYPE:
                    handleSetPreferredNetworkTypeResponse(msg);
                    break;
            }
        }

        private void handleSetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            final int phoneSubId = mPhone.getSubId();

            log("handleSetPreferredNetworkTypeResponse: " + (ar.exception == null));

            if (ar.exception == null) {
                int networkMode;
                if (getPreferenceScreen().findPreference(BUTTON_PREFERED_NETWORK_MODE) != null)  {
                    networkMode =  Integer.valueOf(
                            mButtonPreferredNetworkMode.getValue()).intValue();

                    log("handleSetPreferredNetwrokTypeResponse1: networkMode:" + networkMode );

                    android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                            android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                            networkMode );
                }
                if (getPreferenceScreen().findPreference(BUTTON_ENABLED_NETWORKS_KEY) != null)  {
                    networkMode = Integer.valueOf(
                            mButtonEnabledNetworks.getValue()).intValue();

                    log("handleSetPreferredNetwrokTypeResponse2: networkMode:" + networkMode );

                    android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                            android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                            networkMode );
                }
            } else {
                if (DBG) {
                    log("handleSetPreferredNetworkTypeResponse: exception in setting network mode.");
                }
                updatePreferredNetworkUIFromDb();
            }
        }
    }

    private void updatePreferredNetworkUIFromDb() {
        final int phoneSubId = mPhone.getSubId();

        int settingsNetworkMode = android.provider.Settings.Global.getInt(
                mPhone.getContext().getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                preferredNetworkMode);

        if (DBG) {
            log("updatePreferredNetworkUIFromDb: settingsNetworkMode = " +
                    settingsNetworkMode);
        }

        UpdatePreferredNetworkModeSummary(settingsNetworkMode);
        UpdateEnabledNetworksValueAndSummary(settingsNetworkMode);
        // changes the mButtonPreferredNetworkMode accordingly to settingsNetworkMode
        mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
    }

    private void UpdatePreferredNetworkModeSummary(int NetworkMode) {
        // M: if is not 3/4G phone, init the preference with gsm only type @{
        if (!isCapabilityPhone(mPhone)) {
            NetworkMode = Phone.NT_MODE_GSM_ONLY;
            log("init PreferredNetworkMode with gsm only");
        }
        // @}
        switch(NetworkMode) {
            case Phone.NT_MODE_WCDMA_PREF:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_wcdma_perf_summary);
                break;
            case Phone.NT_MODE_GSM_ONLY:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_gsm_only_summary);
                break;
            case Phone.NT_MODE_WCDMA_ONLY:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_wcdma_only_summary);
                break;
            case Phone.NT_MODE_GSM_UMTS:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_gsm_wcdma_summary);
                break;
            case Phone.NT_MODE_CDMA:
                switch (mPhone.getLteOnCdmaMode()) {
                    case PhoneConstants.LTE_ON_CDMA_TRUE:
                        mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_cdma_summary);
                    break;
                    case PhoneConstants.LTE_ON_CDMA_FALSE:
                    default:
                        mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_cdma_evdo_summary);
                        break;
                }
                break;
            case Phone.NT_MODE_CDMA_NO_EVDO:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_cdma_only_summary);
                break;
            case Phone.NT_MODE_EVDO_NO_CDMA:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_evdo_only_summary);
                break;
            case Phone.NT_MODE_LTE_ONLY:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_lte_summary);
                break;
            case Phone.NT_MODE_LTE_GSM_WCDMA:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_lte_gsm_wcdma_summary);
                break;
            case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_lte_cdma_evdo_summary);
                break;
            case Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                if (mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA ||
                        mIsGlobalCdma ||
                        isWorldMode()) {
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_global_summary);
                } else {
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_lte_summary);
                }
                break;
            case Phone.NT_MODE_GLOBAL:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_cdma_evdo_gsm_wcdma_summary);
                break;
            case Phone.NT_MODE_LTE_WCDMA:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_lte_wcdma_summary);
                break;
            default:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_global_summary);
        }
        /// Add for Plug-in @{
        log("Enter plug-in update updateNetworkTypeSummary - Preferred.");
        mExt.updateNetworkTypeSummary(mButtonPreferredNetworkMode);
        mExt.customizePreferredNetworkMode(mButtonPreferredNetworkMode, mPhone.getSubId());
        /// @}
        /// Add for cmcc open market @{
        mOmEx.updateNetworkTypeSummary(mButtonPreferredNetworkMode);
        /// @}
    }

    private void UpdateEnabledNetworksValueAndSummary(int NetworkMode) {
        Log.d(LOG_TAG, "NetworkMode: " + NetworkMode);
        // M: if is not 3/4G phone, init the preference with gsm only type @{
        if (!isCapabilityPhone(mPhone)) {
            NetworkMode = Phone.NT_MODE_GSM_ONLY;
            log("init EnabledNetworks with gsm only");
        }
        // @}
        switch (NetworkMode) {
            case Phone.NT_MODE_WCDMA_ONLY:
            case Phone.NT_MODE_GSM_UMTS:
            case Phone.NT_MODE_WCDMA_PREF:
                if (!mIsGlobalCdma) {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_WCDMA_PREF));
                    mButtonEnabledNetworks.setSummary(R.string.network_3G);
                    mExt.changeString(mButtonEnabledNetworks, NetworkMode);
                } else {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary(R.string.network_global);
                }
                break;
            case Phone.NT_MODE_GSM_ONLY:
                if (!mIsGlobalCdma) {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_GSM_ONLY));
                    mButtonEnabledNetworks.setSummary(R.string.network_2G);
                    mExt.changeString(mButtonEnabledNetworks, NetworkMode);
                } else {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary(R.string.network_global);
                }
                break;
            case Phone.NT_MODE_LTE_GSM_WCDMA:
                if (isWorldMode()) {
                    mButtonEnabledNetworks.setSummary(
                            R.string.preferred_network_mode_lte_gsm_umts_summary);
                    controlCdmaOptions(false);
                    controlGsmOptions(true);
                    break;
                }
            case Phone.NT_MODE_LTE_ONLY:
            case Phone.NT_MODE_LTE_WCDMA:
                if (!mIsGlobalCdma) {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_LTE_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary((mShow4GForLTE == true)
                            ? R.string.network_4G : R.string.network_lte);
                    mExt.changeString(mButtonEnabledNetworks, NetworkMode);
                } else {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary(R.string.network_global);
                }
                break;
            case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                if (isWorldMode()) {
                    mButtonEnabledNetworks.setSummary(
                            R.string.preferred_network_mode_lte_cdma_summary);
                    controlCdmaOptions(true);
                    controlGsmOptions(false);
                } else {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_LTE_CDMA_AND_EVDO));
                    mButtonEnabledNetworks.setSummary(R.string.network_lte);
                }
                break;
            case Phone.NT_MODE_CDMA:
            case Phone.NT_MODE_EVDO_NO_CDMA:
            case Phone.NT_MODE_GLOBAL:
                /// M: For C2K @{
                if (isC2kLteSupport()) {
                    log("Update value to Global for c2k project");
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_GLOBAL));
                } else {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_CDMA));
                }
                /// @}

                mButtonEnabledNetworks.setSummary(R.string.network_3G);
                mExt.changeString(mButtonEnabledNetworks, NetworkMode);
                break;
            case Phone.NT_MODE_CDMA_NO_EVDO:
                mButtonEnabledNetworks.setValue(
                        Integer.toString(Phone.NT_MODE_CDMA_NO_EVDO));
                mButtonEnabledNetworks.setSummary(R.string.network_1x);
                break;
            case Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                if (isWorldMode()) {
                    controlCdmaOptions(true);
                    controlGsmOptions(false);
                }
                mButtonEnabledNetworks.setValue(
                        Integer.toString(Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                if (mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA ||
                        mIsGlobalCdma ||
                        isWorldMode()) {
                    mButtonEnabledNetworks.setSummary(R.string.network_global);
                } else {
                    mButtonEnabledNetworks.setSummary((mShow4GForLTE == true)
                            ? R.string.network_4G : R.string.network_lte);
                    mExt.changeString(mButtonEnabledNetworks, NetworkMode);
                }
                break;
            default:
                String errMsg = "Invalid Network Mode (" + NetworkMode + "). Ignore.";
                loge(errMsg);
                mButtonEnabledNetworks.setSummary(errMsg);
        }

        /// M: ALPS02217238, for c2k 5m, any way make sure right summary of network type @{
        handleC2k5MCapalibity();
        /// @}

        /// Add for Plug-in @{
        if (mButtonEnabledNetworks != null) {
            log("Enter plug-in update updateNetworkTypeSummary - Enabled.");
            mExt.updateNetworkTypeSummary(mButtonEnabledNetworks);
            log("customizePreferredNetworkMode mButtonEnabledNetworks.");
            mExt.customizePreferredNetworkMode(mButtonEnabledNetworks, mPhone.getSubId());
            /// Add for cmcc open market @{
            mOmEx.updateNetworkTypeSummary(mButtonEnabledNetworks);
            /// @}
        }
        /// @}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
        case REQUEST_CODE_EXIT_ECM:
            Boolean isChoiceYes =
                data.getBooleanExtra(EmergencyCallbackModeExitDialog.EXTRA_EXIT_ECM_RESULT, false);
            if (isChoiceYes) {
                // If the phone exits from ECM mode, show the CDMA Options
                mCdmaOptions.showDialog(mClickedPreference);
            } else {
                // do nothing
            }
            break;

        default:
            break;
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private static void loge(String msg) {
        Log.e(LOG_TAG, msg);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {  // See ActionBar#setDisplayHomeAsUpEnabled()
            // Commenting out "logical up" capability. This is a workaround for issue 5278083.
            //
            // Settings app may not launch this activity via UP_ACTIVITY_CLASS but the other
            // Activity that looks exactly same as UP_ACTIVITY_CLASS ("SubSettings" Activity).
            // At that moment, this Activity launches UP_ACTIVITY_CLASS on top of the Activity.
            // which confuses users.
            // TODO: introduce better mechanism for "up" capability here.
            /*Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(UP_ACTIVITY_PACKAGE, UP_ACTIVITY_CLASS);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);*/
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isWorldMode() {
        boolean worldModeOn = false;
        final TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        final String configString = getResources().getString(R.string.config_world_mode);

        if (!TextUtils.isEmpty(configString)) {
            String[] configArray = configString.split(";");
            // Check if we have World mode configuration set to True only or config is set to True
            // and SIM GID value is also set and matches to the current SIM GID.
            if (configArray != null &&
                   ((configArray.length == 1 && configArray[0].equalsIgnoreCase("true")) ||
                       (configArray.length == 2 && !TextUtils.isEmpty(configArray[1]) &&
                           tm != null && configArray[1].equalsIgnoreCase(tm.getGroupIdLevel1())))) {
                               worldModeOn = true;
            }
        }

        if (DBG) {
            log("isWorldMode=" + worldModeOn);
        }

        return worldModeOn;
    }

    private void controlGsmOptions(boolean enable) {
        PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet == null) {
            return;
        }

        if (mGsmUmtsOptions == null) {
            mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet, mPhone.getSubId());
        }
        PreferenceScreen apnExpand =
                (PreferenceScreen) prefSet.findPreference(BUTTON_APN_EXPAND_KEY);
        PreferenceScreen operatorSelectionExpand =
                (PreferenceScreen) prefSet.findPreference(BUTTON_OPERATOR_SELECTION_EXPAND_KEY);
        PreferenceScreen carrierSettings =
                (PreferenceScreen) prefSet.findPreference(BUTTON_CARRIER_SETTINGS_KEY);
        if (apnExpand != null) {
            apnExpand.setEnabled(isWorldMode() || enable);
        }
        if (operatorSelectionExpand != null) {
            operatorSelectionExpand.setEnabled(enable);
        }
        if (carrierSettings != null) {
            prefSet.removePreference(carrierSettings);
        }
    }

    private void controlCdmaOptions(boolean enable) {
        PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet == null) {
            return;
        }
        if (enable && mCdmaOptions == null) {
            mCdmaOptions = new CdmaOptions(this, prefSet, mPhone);
        }
        CdmaSystemSelectListPreference systemSelect =
                (CdmaSystemSelectListPreference)prefSet.findPreference
                        (BUTTON_CDMA_SYSTEM_SELECT_KEY);
        if (systemSelect != null) {
            systemSelect.setEnabled(enable);
        }
    }

    private void dissmissDialog(ListPreference preference) {
        Dialog dialog = null;
        if (preference != null) {
            dialog = preference.getDialog();
            if (dialog != null) {
                dialog.dismiss();
            }
        }
    }

    // -------------------- Mediatek ---------------------
    // M: Add for plug-in
    private IMobileNetworkSettingsExt mExt;
    // M: Add for cmcc open market
    private MobileNetworkSettingsOmEx mOmEx;
    /// M: add for plmn list
    public static final String BUTTON_PLMN_LIST = "button_plmn_key";
    private static final String BUTTON_CDMA_ACTIVATE_DEVICE_KEY = "cdma_activate_device_key";
    /// M: c2k 4g data only
    private static final String SINGLE_LTE_DATA = "single_lte_data";
    private Preference mPLMNPreference;
    private IntentFilter mIntentFilter;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            log("action: " + action);
            /// When receive aiplane mode, we would like to finish the activity, for
            //  we can't get the modem capability, and will show the user selected network
            //  mode as summary, this will make user misunderstand.(ALPS01971666)
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                finish();
            } else if (action.equals(Intent.ACTION_MSIM_MODE_CHANGED)
                    || action.equals(TelephonyIntents.ACTION_MD_TYPE_CHANGE)
                    || action.equals(TelephonyIntents.ACTION_LOCATED_PLMN_CHANGED)) {
                updateScreenStatus();
            }
            /// Add for Sim Switch @{
            else if (action.equals(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_DONE)) {
                log("Siwthc done Action ACTION_SET_PHONE_RAT_FAMILY_DONE received ");
                mPhone = PhoneUtils.getPhoneUsingSubId(mPhone.getSubId());
                updateScreenStatus();
            } else if (action.equals(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED)) {
                // When the radio changes (ex: CDMA->GSM), refresh all options.
                mGsmUmtsOptions = null;
                mCdmaOptions = null;
                updateBody();
            } else if (action.equals(TelephonyIntents.ACTION_RAT_CHANGED)) {
                handleRatChanged(intent);
            /// M: for 5M, add for sim loaded @{
            } else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                if (FeatureOption.isMtkC2k5MSupport()) {
                    String simStatus = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                    if (simStatus.equals(IccCardConstants.INTENT_VALUE_ICC_LOADED)) {
                        log("--- sim card loaded ---");

                        if (!mButtonEnabledNetworks.isEnabled()) {
                            mButtonEnabledNetworks.setEnabled(true);
                        }
                    }
                    updateBody();
                }
            }
            /// @}
            /// @}
        }
    };

    private void onCreateMTK(PreferenceScreen prefSet) {

        /// M: Add For [MTK_Enhanced4GLTE] @{
        addEnhanced4GLteSwitchPreference(prefSet);
        /// @}

        /// M: For 2G only project remove select network mode item @{
        if (TelephonyUtils.is2GOnlyProject()) {
            log("[initPreferenceForMobileNetwork]only 2G");
            if (findPreference(BUTTON_PREFERED_NETWORK_MODE) != null) {
                prefSet.removePreference(mButtonPreferredNetworkMode);
            }
            if (findPreference(BUTTON_ENABLED_NETWORKS_KEY) != null) {
                prefSet.removePreference(mButtonEnabledNetworks);
            }
        }
        /// @}

        /// M: Add for plmn list @{
        if (!FeatureOption.isMtk3gDongleSupport() && FeatureOption.isMtkCtaSet()
                && !TelephonyUtilsEx.isCDMAPhone(mPhone)) {
            log("---addPLMNList---");

            //delete by lgy
//            addPLMNList(prefSet);
        }
        /// @}

        mExt = ExtensionManager.getMobileNetworkSettingsExt();
        /// M: Add For C2K OM, OP09 will implement its own cdma network setting @{
        if (FeatureOption.isMtkLteSupport()
                && (isC2kLteSupport())
                && (TelephonyUtilsEx.isCDMAPhone(mPhone)
                        || TelephonyUtilsEx.isRoaming(mPhone)
                        || TelephonyUtilsEx.isCdma4gCard(mPhone.getSubId()))
                && !mExt.isCtPlugin()) {
            if ((FeatureOption.isMtkC2k5MSupport() && FeatureOption.isLoadForHome())
                    || !FeatureOption.isMtkC2k5MSupport()) {
                if (mCdmaNetworkSettings != null) {
                    log("CdmaNetworkSettings destroy " + this);
                    mCdmaNetworkSettings.onDestroy();
                    mCdmaNetworkSettings = null;
                }
                mCdmaNetworkSettings = new CdmaNetworkSettings(this, prefSet, mPhone);
                mCdmaNetworkSettings.onResume();
            }
        }
        /// @}

        /// Add for plug-in @{
        if (mPhone != null) {
            mExt.initOtherMobileNetworkSettings(this, mPhone.getSubId());
        }
        mExt.initMobileNetworkSettings(this, mSlotId);
        /// @}
        /// Add for cmcc open market @{
        mOmEx.initMobileNetworkSettings(this, mSlotId);
        /// @}

        updateScreenStatus();
        mExt.onResume();

        /// M: for mtk 3m
        handleC2k3MScreen(prefSet);
        /// M: for mtk 4m
        handleC2k4MScreen(prefSet);
        /// M: for mtk 5m
        handleC2k5MScreen(prefSet);
    }

    /**
     * Update the preferred network mode item Entries & Values
     */
    private void updateNetworkModeForLwDsds() {
        /// Get main phone Id;
        ITelephonyEx iTelEx = ITelephonyEx.Stub.asInterface(
                ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
        int mainPhoneId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        if (iTelEx != null) {
            try{
                mainPhoneId = iTelEx.getMainCapabilityPhoneId();
            } catch (RemoteException e) {
                loge("handleLwDsdsNetworkMode get iTelEx error" + e.getMessage());
            }
        }
        /// If the phone main phone we should do nothing special;
        log("handleLwDsdsNetworkMode mainPhoneId = " + mainPhoneId);
        if (mainPhoneId != mPhone.getPhoneId()) {
            /// We should compare the user's setting value & modem support info;
            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE
                    + mPhone.getSubId(), Phone.NT_MODE_GSM_ONLY);
            int currRat = mPhone.getRadioAccessFamily();
            log("updateNetworkModeForLwDsds settingsNetworkMode = "
                    + settingsNetworkMode + "; currRat = " + currRat);
            if ((currRat & RadioAccessFamily.RAF_UMTS)== RadioAccessFamily.RAF_UMTS) {
                // Support 3/2G for WorldMode is uLWG
                mButtonEnabledNetworks.setEntries(
                        R.array.enabled_networks_except_lte_choices);
                if (isC2kLteSupport()) {
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_except_lte_values_c2k);
                } else {
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_except_lte_values);
                }
                // If user select contain LTE, should set UI to 3G;
                // NT_MODE_LTE_CDMA_AND_EVDO = 8 is the smallest value supporting LTE.
                if (settingsNetworkMode > Phone.NT_MODE_LTE_CDMA_AND_EVDO) {
                    log("updateNetworkModeForLwDsds set network mode to 3G");
                    if (isC2kLteSupport()) {
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_GLOBAL));
                    } else {
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_WCDMA_PREF));
                    }
                    mButtonEnabledNetworks.setSummary(R.string.network_3G);
                } else {
                    log("updateNetworkModeForLwDsds set to what user select. ");
                    UpdateEnabledNetworksValueAndSummary(settingsNetworkMode);
                }
            } else {
                // Only support 2G for WorldMode is uLtTG
                log("updateNetworkModeForLwDsds set to 2G only.");
                mButtonEnabledNetworks.setSummary(R.string.network_2G);
                mButtonEnabledNetworks.setEnabled(false);
            }
        } else {
            log("handleLwDsdsNetworkMode main phone do nothing");
        }
    }

    private void initIntentFilter() {
        /// M: for receivers sim lock gemini phone @{
        mIntentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_EF_CSP_CONTENT_NOTIFY);
        mIntentFilter.addAction(Intent.ACTION_MSIM_MODE_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_MD_TYPE_CHANGE);
        mIntentFilter.addAction(TelephonyIntents.ACTION_LOCATED_PLMN_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
        ///@}
        /// M: Add for Sim Switch @{
        mIntentFilter.addAction(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_DONE);
        mIntentFilter.addAction(TelephonyIntents.ACTION_RAT_CHANGED);
        /// @}

        /// M: add for sim card loaded @{
        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        /// @}
    }

    private void addPLMNList(PreferenceScreen prefSet) {
        // add PLMNList, if c2k project the order should under the 4g data only
        int order = prefSet.findPreference(SINGLE_LTE_DATA) != null ?
                prefSet.findPreference(SINGLE_LTE_DATA).getOrder() : mButtonDataRoam.getOrder();
        mPLMNPreference = new Preference(this);
        mPLMNPreference.setKey(BUTTON_PLMN_LIST);
        mPLMNPreference.setTitle(R.string.plmn_list_setting_title);
        Intent intentPlmn = new Intent();
        intentPlmn.setClassName("com.android.phone", "com.mediatek.settings.PLMNListPreference");
        intentPlmn.putExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, mPhone.getSubId());
        mPLMNPreference.setIntent(intentPlmn);
        mPLMNPreference.setOrder(order + 1);
        prefSet.addPreference(mPLMNPreference);
    }

    private void updateScreenStatus() {
        boolean isIdle = (TelephonyManager.getDefault().getCallState()
                == TelephonyManager.CALL_STATE_IDLE);
        boolean isShouldEnabled = isIdle && TelephonyUtils.isRadioOn(mPhone.getSubId(), this);
        log("updateNetworkModePreference:isShouldEnabled = "
                + isShouldEnabled + ", isIdle = " + isIdle);
        //modify by lgy
//        getPreferenceScreen().setEnabled(isShouldEnabled || mExt.useCTTestcard());
        PreferenceGroup screen = (PreferenceGroup) getPreferenceScreen();      
        for (int i = 0, n = screen.getPreferenceCount(); i < n; i++) {
        	if(screen.getPreference(i) != mBasicSettingCategory) {
        		screen.getPreference(i).setEnabled(isShouldEnabled || mExt.useCTTestcard());
        	}
        }
        updateCapabilityRelatedPreference(isShouldEnabled);
    }

    /**
     * Add for update the display of network mode preference.
     * @param enable is the preference or not
     */
    private void updateCapabilityRelatedPreference(boolean enable) {
        // if airplane mode is on or all SIMs closed, should also dismiss dialog
        boolean isNWModeEnabled = enable && isCapabilityPhone(mPhone);
        log("updateNetworkModePreference:isNWModeEnabled = " + isNWModeEnabled);

        updateNetworkModePreference(mButtonPreferredNetworkMode, isNWModeEnabled);
        updateNetworkModePreference(mButtonEnabledNetworks, isNWModeEnabled);
        /// M: Add for L+W DSDS.
        updateNetworkModeForLwDsds();
        /// Add for [MTK_Enhanced4GLTE]
        updateEnhanced4GLteSwitchPreference(enable);

        /// M: for mtk c2k 5m
        handleC2k5MCapalibity();
        /// Update CDMA network settings
        if (TelephonyUtilsEx.isCDMAPhone(mPhone) && mCdmaNetworkSettings != null) {
            mCdmaNetworkSettings.onResume();
        } else {
            log("updateCapabilityRelatedPreference don't update cdma settings");
        }
    }

    /**
     * Add for update the display of network mode preference.
     * @param enable is the preference or not
     */
    private void updateNetworkModePreference(ListPreference preference, boolean enable) {
        // if airplane mode is on or all SIMs closed, should also dismiss dialog
        if (preference != null) {
            preference.setEnabled(enable);
            if (!enable) {
                dissmissDialog(preference);
            }
            if (getPreferenceScreen().findPreference(preference.getKey()) != null) {
                updatePreferredNetworkUIFromDb();
            }
            /// Add for Plug-in @{
            mExt.customizePreferredNetworkMode(preference, mPhone.getSubId());
            log("Enter plug-in update updateLTEModeStatus.");
            mExt.updateLTEModeStatus(preference);
            /// @}
            /// Add for cmcc open market @{
            mOmEx.updateLTEModeStatus(preference);
            /// @}
        }
    }

    /**
     * handle network mode change result by framework world phone sim switch logical.
     * @param intent which contains the info of network mode
     */
    private void handleRatChanged(Intent intent) {
        int phoneId = intent.getIntExtra(PhoneConstants.PHONE_KEY, 0);
        int modemMode = intent.getIntExtra(TelephonyIntents.EXTRA_RAT, -1);
        log("handleRatChanged phoneId: " + phoneId + " modemMode: " + modemMode);
        Phone phone = PhoneFactory.getPhone(phoneId);
        log("ACTION_RAT_CHANGED --- don't update DB ---");
        /* For ALPS02337896, don't update the DB value when rat change.
         * This may lead to ALPS01923338 happens again.
        if (modemMode != -1 && isCapabilityPhone(phone)) {
            android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE +
                    phone.getSubId(),
                    modemMode);
        }
        */
        if (phoneId == mPhone.getPhoneId() && isCapabilityPhone(phone)) {
            log("handleRatChanged: updateBody");
            updateBody();
        }
    }

    /**
     * Is the phone has 3/4G capability or not.
     * @return true if phone has 3/4G capability
     */
    private boolean isCapabilityPhone(Phone phone) {
        boolean result = phone != null ? ((phone.getRadioAccessFamily()
                & (RadioAccessFamily.RAF_UMTS | RadioAccessFamily.RAF_LTE)) > 0) : false;
        log("isCapabilityPhone: " + result);
        return result;
    }

    // M: Add for [MTK_Enhanced4GLTE] @{
    // Use our own button instand of Google default one mButton4glte
    private Enhanced4GLteSwitchPreference mEnhancedButton4glte;

    /**
     * Add our switchPreference & Remove google default one.
     * @param preferenceScreen
     */
    private void addEnhanced4GLteSwitchPreference(PreferenceScreen preferenceScreen) {
        log("[addEnhanced4GLteSwitchPreference] ImsEnabled :"
                + ImsManager.isVolteEnabledByPlatform(this));
        if (mButton4glte != null) {
            log("[addEnhanced4GLteSwitchPreference] Remove mButton4glte!");
            preferenceScreen.removePreference(mButton4glte);
        }
        if (ImsManager.isVolteEnabledByPlatform(this)) {
            int order = mButtonDataRoam.getOrder() + 1;
            mEnhancedButton4glte = new Enhanced4GLteSwitchPreference(this, mPhone.getSubId());
            /// Still use Google's key, title, and summary.
            mEnhancedButton4glte.setKey(BUTTON_4G_LTE_KEY);
            if (ImsManager.isWfcEnabledByPlatform(this)) {
            	mEnhancedButton4glte.setTitle(R.string.wfc_volte_switch_title);
            } else {
                mEnhancedButton4glte.setTitle(R.string.hb_enhanced_4g_lte_mode_title);
            }
            mEnhancedButton4glte.setSummary(R.string.hb_enhanced_4g_lte_mode_summary);
            mEnhancedButton4glte.setOnPreferenceChangeListener(this);
            mEnhancedButton4glte.setOrder(order);
            //preferenceScreen.addPreference(mEnhancedButton4glte);
        }
    }

    /**
     * Update the subId in mEnhancedButton4glte.
     */
    private void updateEnhanced4GLteSwitchPreference(final boolean enable) {
        if (mEnhancedButton4glte != null) {
            if (ImsManager.isVolteEnabledByPlatform(this) &&
                    TelephonyUtilsEx.getMainPhoneId() == mPhone.getPhoneId()) {
                if (findPreference(BUTTON_4G_LTE_KEY) == null) {
                    log("updateEnhanced4GLteSwitchPreference add switcher");
                    getPreferenceScreen().addPreference(mEnhancedButton4glte);
                }
            } else {
                if (findPreference(BUTTON_4G_LTE_KEY) != null) {
                    getPreferenceScreen().removePreference(mEnhancedButton4glte);
                }
            }
            if (findPreference(BUTTON_4G_LTE_KEY) != null) {
                log("[updateEnhanced4GLteSwitchPreference] SubId = " + mPhone.getSubId());
                mEnhancedButton4glte.setSubId(mPhone.getSubId());
                int isChecked = Settings.Global.getInt(getContentResolver(),
                        Settings.Global.ENHANCED_4G_MODE_ENABLED, 0);
                mEnhancedButton4glte.setChecked(isChecked == 1);
                //add by lgy
                mHandler.post(new Runnable(){
                	public void run(){
                        mEnhancedButton4glte.setEnabled(enable);
                	}
                });
            }
        }
    }

    /**
     * For [MTK_Enhanced4GLTE]
     * We add our own SwitchPreference, and its own onPreferenceChange call backs.
     * @param preference
     * @param objValue
     * @return
     */
    private boolean onPreferenceChangeMTK(Preference preference, Object objValue) {
        log("[onPreferenceChangeMTK] preference = " + preference.getTitle());
        if (mEnhancedButton4glte == preference) {
            log("[onPreferenceChangeMTK] IsChecked = " + mEnhancedButton4glte.isChecked());
            Enhanced4GLteSwitchPreference ltePref = (Enhanced4GLteSwitchPreference) preference;
            ltePref.setChecked(!ltePref.isChecked());
            ImsManager.setEnhanced4gLteModeSetting(this, ltePref.isChecked());
            return true;
        }
        return false;
    }
    /// @}

    /**
     * For C2k 5M
     */
    private void handleC2k5MCapalibity() {
        if (FeatureOption.isMtkLteSupport() && FeatureOption.isMtkC2k5MSupport()) {
            if (TelephonyUtils.isInvalidSimCard(mPhone.getSubId())
                    && mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM) {
                log("--- go to C2k 5M capability ---");

                mButtonEnabledNetworks.setEnabled(false);
            }
        }
    }

    /**
     * For C2k 5M
     * Under 5M(CLLWG),CMCC card in home network is no need show 3G item.
     * @param prefSet
     */
    private void handleC2k5MScreen(PreferenceScreen prefSet) {
        if (FeatureOption.isMtkLteSupport() && FeatureOption.isMtkC2k5MSupport()) {

            handleC2kCommonScreen(prefSet);
            log("--- go to c2k 5M ---");

            if (!TelephonyUtilsEx.isCDMAPhone(mPhone)) {
                mButtonEnabledNetworks.setEntries(R.array.enabled_networks_4g_choices);
                mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_values_c2k);
            }
        }
    }

    /**
     * For C2k OM 4M
     * @param preset
     */
    private void handleC2k4MScreen(PreferenceScreen prefSet) {
        if (FeatureOption.isMtkLteSupport() && FeatureOption.isMtkC2k4MSupport()) {
            log( "--- go to C2k 4M ---");

            if (PhoneConstants.PHONE_TYPE_GSM == mPhone.getPhoneType()) {
                mButtonEnabledNetworks.setEntries(
                        R.array.enabled_networks_except_td_cdma_3g_choices);
                mButtonEnabledNetworks.setEntryValues(
                        R.array.enabled_networks_except_td_cdma_3g_values);
            }
        }
    }

    /**
     * For C2k 3M
     * @param preset
     */
    private void handleC2k3MScreen(PreferenceScreen prefSet) {
        if (!FeatureOption.isMtkLteSupport() && FeatureOption.isMtkC2k3MSupport()) {

            handleC2kCommonScreen(prefSet);
            log( "--- go to C2k 3M ---");

            if (!TelephonyUtilsEx.isCDMAPhone(mPhone)) {
                mButtonEnabledNetworks.setEntries(R.array.enabled_networks_except_lte_choices);
                mButtonEnabledNetworks.setEntryValues(
                        R.array.enabled_networks_except_lte_values_c2k);
            }
        }
    }

    /**
     * For C2k Common screen, (3M, 5M)
     * @param preset
     */
    private void handleC2kCommonScreen(PreferenceScreen prefSet) {
        log("--- go to C2k Common (3M, 5M) screen ---");

        if (!FeatureOption.isLoadForHome()) {
            if (prefSet.findPreference(BUTTON_PLMN_LIST) != null) {
                prefSet.removePreference(prefSet.findPreference(BUTTON_PLMN_LIST));
            }
        }
        if (prefSet.findPreference(BUTTON_PREFERED_NETWORK_MODE) != null) {
            prefSet.removePreference(prefSet.findPreference(BUTTON_PREFERED_NETWORK_MODE));
        }
        if (TelephonyUtilsEx.isCDMAPhone(mPhone)) {
            if (prefSet.findPreference(BUTTON_ENABLED_NETWORKS_KEY) != null) {
                prefSet.removePreference(prefSet.findPreference(BUTTON_ENABLED_NETWORKS_KEY));
            }
        }
    }

    /**
     * Whether support c2k LTE or not
     * @return true if support else false.
     */
    private boolean isC2kLteSupport() {
        return FeatureOption.isMtkSrlteSupport()
                || FeatureOption.isMtkSvlteSupport();
    }
    
    //add by lgy for SimSetting
    private static final String TAG = "SimSettings";
    private int mSlotId = -1;
    
    private Preference mBasicSettingCategory;
    private Preference mNetSettingCategory;
    
    private static final String DISALLOW_CONFIG_SIM = "no_config_sim";
    private static final String SIM_CARD_CATEGORY = "sim_cards";
    private static final String KEY_CELLULAR_DATA = "sim_cellular_data";
    private static final String KEY_CALLS = "sim_calls";
    private static final String KEY_SMS = "sim_sms";
    public static final String EXTRA_SLOT_ID = "slot_id";

    /**
     * By UX design we use only one Subscription Information(SubInfo) record per SIM slot.
     * mAvalableSubInfos is the list of SubInfos we present to the user.
     * mSubInfoList is the list of all SubInfos.
     * mSelectableSubInfos is the list of SubInfos that a user can select for data, calls, and SMS.
     */
    private List<SubscriptionInfo> mAvailableSubInfos = null;
    private List<SubscriptionInfo> mSubInfoList = null;
    private List<SubscriptionInfo> mSelectableSubInfos = null;
    private PreferenceScreen mSimCards = null;
    private int mNumSlots;
    private Context mContext;
    private Preference mSim1, mSim2;
    private SwitchPreference mData;
    private Preference mSelectCall, mSelectData;

    private void updateSubscriptions() {
        mSubInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        mAvailableSubInfos.clear();
        mSelectableSubInfos.clear();

        for (int i = 0; i < mNumSlots; ++i) {
            final SubscriptionInfo sir = mSubscriptionManager
                    .getActiveSubscriptionInfoForSimSlotIndex(i);
            mAvailableSubInfos.add(sir);         
            if (sir != null) {
                mSelectableSubInfos.add(sir);
            }
        }

        updateSimInfo();
        updateAllOptions();
    }
    
    private void updateAllOptions() {
        updateActivitesCategory();
    }

    private void updateActivitesCategory() {
        updateCellularDataValues();
        updateCallValues();
        updateSmsValues();
        updateSimPref();
        //add by lgy 
        boolean enable = isDataPrefEnable();
		mData.setEnabled(enable);
    }

    private void updateSmsValues() {
        final Preference simPref = findPreference(KEY_SMS);
        if (simPref != null) {
            SubscriptionInfo sir = mSubscriptionManager.getDefaultSmsSubscriptionInfo();
            simPref.setTitle(R.string.sms_messages_title);
            if (DBG) log("[updateSmsValues] mSubInfoList=" + mSubInfoList);

            /// M: for plug-in
            sir = mSimManagementExt.setDefaultSubId(this, sir, KEY_SMS);

            if (sir != null) {
                simPref.setSummary(sir.getDisplayName());
            } else if (sir == null) {
                /// M: for [Always Ask]
                // simPref.setSummary(R.string.sim_selection_required_pref);
                simPref.setSummary(R.string.sim_calls_ask_first_prefs_title);
                /// M: for plug-in
                mSimManagementExt.updateDefaultSmsSummary(simPref);
            }

            /// M: [C2K solution 2 enhancement] [C2K solution 1.5] @{
            if (!shouldDisableActivitesCategory(this)) {
            /// @}
                simPref.setEnabled(mSelectableSubInfos.size() >= 1);
                /// M: for plug-in
               mSimManagementExt.configSimPreferenceScreen(simPref, KEY_SMS,
                       mSelectableSubInfos.size());
            }
        }
    }

    private void updateCellularDataValues() {
        final Preference simPref = findPreference(KEY_CELLULAR_DATA);
        if (simPref != null) {
            SubscriptionInfo sir = mSubscriptionManager.getDefaultDataSubscriptionInfo();
            simPref.setTitle(R.string.hb_cellular_data_title);
            if (DBG) log("[updateCellularDataValues] mSubInfoList=" + mSubInfoList);

            /// M: for plug-in
            sir = mSimManagementExt.setDefaultSubId(this, sir, KEY_CELLULAR_DATA);

            if (sir != null) {
                simPref.setSummary(sir.getDisplayName().toString() + (sir.getSimSlotIndex() + 1));
            } else if (sir == null) {
                simPref.setSummary(R.string.sim_selection_required_pref);
            }
            /// M: [SIM Capability Switch] to prevent continuous click when SIM switch @{
            // simPref.setEnabled(mSelectableSubInfos.size() >= 1);            
//            simPref.setEnabled(isDataPrefEnable());
            simPref.setEnabled(isDataPrefEnable() && !isAllRadioOff());
            mSimManagementExt.configSimPreferenceScreen(simPref, KEY_CELLULAR_DATA, -1);
            /// @}
        }
    }

    private void updateCallValues() {
        final Preference simPref = findPreference(KEY_CALLS);
        if (simPref != null) {
            final TelecomManager telecomManager = TelecomManager.from(mContext);
            PhoneAccountHandle phoneAccount =
                telecomManager.getUserSelectedOutgoingPhoneAccount();
            final List<PhoneAccountHandle> allPhoneAccounts =
                telecomManager.getCallCapablePhoneAccounts();

            phoneAccount = mSimManagementExt.setDefaultCallValue(phoneAccount);

            simPref.setTitle(R.string.hb_calls_title);
            /// M: for ALPS02320747 @{
            // phoneaccount may got unregistered, need to check null here
            /*
            simPref.setSummary(phoneAccount == null
                    ? mContext.getResources().getString(R.string.sim_calls_ask_first_prefs_title)
                    : (String)telecomManager.getPhoneAccount(phoneAccount).getLabel());
             */
            PhoneAccount defaultAccount = phoneAccount == null ? null : telecomManager
                    .getPhoneAccount(phoneAccount);
            simPref.setSummary(defaultAccount == null
                    ? mContext.getResources().getString(R.string.sim_calls_ask_first_prefs_title)
                    : (String)defaultAccount.getLabel() + (PhoneUtils.getSlotdForPhoneAccountHandle(phoneAccount) + 1));
            /// @}
            /// M: [C2K solution 2 enhancement] [C2K solution 1.5] @{
            if (!shouldDisableActivitesCategory(this)) {
                simPref.setEnabled(!isAllRadioOff());
                mSimManagementExt.configSimPreferenceScreen(simPref, KEY_CALLS,
                        allPhoneAccounts.size());
            }
            /// @}
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        mSubscriptionManager.removeOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        /// M: Google bug: only listen to default sub, listen to Phone state change instead @{
        /*
        final TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        */
        ///@}

        /// M: for Plug-in
        mSimManagementExt.onPause();
    }
    
    ///----------------------------------------MTK-----------------------------------------------

    private static final String KEY_SIM_ACTIVITIES = "sim_activities";

    // / M: for Plug in @{
    private ISettingsMiscExt mMiscExt;
    private ISimManagementExt mSimManagementExt;
    // / @}

    private ITelephonyEx mTelephonyEx;
    private SimHotSwapHandler mSimHotSwapHandler;
    private boolean mIsAirplaneModeOn = false;

    private static final int MODE_PHONE1_ONLY = 1;
    private PhoneServiceStateHandler mStateHandler;

            // Receiver to handle different actions
    private BroadcastReceiver mSimReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "mSimReceiver action = " + action);
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                handleAirplaneModeChange(intent);
            } else if (action.equals(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
                updateCellularDataValues();
            } else if (action.equals(TelecomManagerEx.ACTION_DEFAULT_ACCOUNT_CHANGED)
                    || action.equals(TelecomManagerEx.ACTION_PHONE_ACCOUNT_CHANGED)) {
                updateCallValues();
            } else if (action.equals(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_DONE)
                    || action.equals(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_FAILED)) {
                updateCellularDataValues();
            } else if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
                updateCellularDataValues();
            } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            	//zhangcj modfiy
            	mData.setChecked(mTelephonyManager.getDataEnabled());
            }
        }
    };

    @Override
    public void onServiceStateChanged(ServiceState state, int subId) {
        Log.d(TAG, "PhoneStateListener:onServiceStateChanged: subId: " + subId
                + ", state: " + state);
        updateSimPref();
    }

    /**
     * init for sim state change, including SIM hot swap, airplane mode, etc.
     */
    private void initForSimStateChange() {
        mTelephonyEx = ITelephonyEx.Stub.asInterface(
                ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
        /// M: for [SIM Hot Swap] @{
        mSimHotSwapHandler = new SimHotSwapHandler(this.getApplicationContext());
        mSimHotSwapHandler.registerOnSimHotSwap(new OnSimHotSwapListener() {
            @Override
            public void onSimHotSwap() {
                if (this != null) {
                    log("onSimHotSwap, finish Activity~~");
                    finish();
                }
            }
        });
        /// @}

        mIsAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(this.getApplicationContext());
        Log.d(TAG, "init()... air plane mode is: " + mIsAirplaneModeOn);

        mStateHandler = new PhoneServiceStateHandler(this.getApplicationContext());
        mStateHandler.registerOnPhoneServiceStateChange(this);

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        // For PhoneAccount
        intentFilter.addAction(TelecomManagerEx.ACTION_DEFAULT_ACCOUNT_CHANGED);
        intentFilter.addAction(TelecomManagerEx.ACTION_PHONE_ACCOUNT_CHANGED);
        // For radio on/off
        intentFilter.addAction(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_DONE);
        intentFilter.addAction(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_FAILED);

        // listen to PHONE_STATE_CHANGE
        intentFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        this.registerReceiver(mSimReceiver, intentFilter);
        int id =  mPhone.getSubId();
        getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.MOBILE_DATA+id),
                true, mSettingObserver);
    }
    
	private final ContentObserver mSettingObserver = new ContentObserver(
			new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			int id =  mPhone.getSubId();
			boolean retVal = Settings.Global.getInt(getContentResolver(),
					Settings.Global.MOBILE_DATA+id, 0) != 0;
			mData.setChecked(retVal);
		}
	};

    /**
     * When airplane mode is on, some parts need to be disabled for prevent some telephony issues
     * when airplane on.
     * Default data is not able to switch as may cause modem switch
     * SIM radio power switch need to disable, also this action need operate modem
     * @param airplaneOn airplane mode state true on, false off
     */
    private void handleAirplaneModeChange(Intent intent) {
        mIsAirplaneModeOn = intent.getBooleanExtra("state", false);
        Log.d(TAG, "air plane mode is = " + mIsAirplaneModeOn);
        updateCellularDataValues();
        updateSimPref();
        removeItemsForTablet();
    }

    /**
     * remove unnecessary items for tablet
     */
    private void removeItemsForTablet() {
        // remove some item when in 4gds wifi-only
        if ( com.mediatek.settings.FeatureOption.MTK_PRODUCT_IS_TABLET) {
            Preference sim_call_Pref = findPreference(KEY_CALLS);
            Preference sim_sms_Pref = findPreference(KEY_SMS);
            Preference sim_data_Pref = findPreference(KEY_CELLULAR_DATA);
            PreferenceCategory mPreferenceCategoryActivities =
                (PreferenceCategory) findPreference(KEY_SIM_ACTIVITIES);
            TelephonyManager tm = TelephonyManager.from(this);
            if (!tm.isSmsCapable() && sim_sms_Pref != null) {
                mPreferenceCategoryActivities.removePreference(sim_sms_Pref);
            }
            if (!tm.isMultiSimEnabled() && sim_data_Pref != null && sim_sms_Pref != null) {
                mPreferenceCategoryActivities.removePreference(sim_data_Pref);
                mPreferenceCategoryActivities.removePreference(sim_sms_Pref);
            }
            if (!tm.isVoiceCapable() && sim_call_Pref != null) {
                mPreferenceCategoryActivities.removePreference(sim_call_Pref);
            }
        }
    }


    /**
     * only for plug-in, change "SIM" to "UIM/SIM".
     */
    private void customizeSimDisplay() {
        if (mSimCards != null) {
            mSimCards.setTitle(mMiscExt.customizeSimDisplayString(
                    getString(R.string.sim_settings_title),
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID));
        }
        this.setTitle(
                mMiscExt.customizeSimDisplayString(getString(R.string.sim_settings_title),
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID));
    }


    private void updateSimPref() {
        if (shouldDisableActivitesCategory(this)) {
            final Preference simCallsPref = findPreference(KEY_CALLS);
            if (simCallsPref != null) {
                final TelecomManager telecomManager = TelecomManager.from(this);
                int accoutSum = telecomManager.getCallCapablePhoneAccounts().size();
                Log.d(TAG, "accountSum: " + accoutSum);
                simCallsPref.setEnabled(accoutSum > 1
                        && (!com.mediatek.settings.sim.TelephonyUtils.isCapabilitySwitching())
                        && (!mIsAirplaneModeOn));
            }
            final Preference simSmsPref = findPreference(KEY_SMS);
            if (simSmsPref != null) {
                simSmsPref.setEnabled(mSelectableSubInfos.size() > 1
                        && (!com.mediatek.settings.sim.TelephonyUtils.isCapabilitySwitching())
                        && (!mIsAirplaneModeOn));
            }
        }
    }

    private boolean isDataPrefEnable() {
        final boolean ecbMode = SystemProperties.getBoolean(
                TelephonyProperties.PROPERTY_INECM_MODE, false);
        log("isEcbMode()... isEcbMode: " + ecbMode);
        return mSelectableSubInfos.size() >= 1
                && (!com.mediatek.settings.sim.TelephonyUtils.isCapabilitySwitching())
                && (!mIsAirplaneModeOn)
                && !TelecomManager.from(mContext).isInCall()
                && !ecbMode;
    }

    private static boolean shouldDisableActivitesCategory(Context context) {
        boolean shouldDisable = false;
        shouldDisable = CdmaUtils.isCdmaCardCompetion(context)
                || CdmaUtils.isCdamCardAndGsmCard(context);
        Log.d(TAG, "shouldDisableActivitesCategory() .. shouldDisable :" + shouldDisable);
        return shouldDisable;
    }
    
    
    private void initPreference() {
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        
    	mBasicSettingCategory = findPreference("basic_category");
    	mNetSettingCategory = findPreference("networks_category");
    	
        mSim1 = findPreference("sim_1");
        mSim1.getIntent().putExtra(SimSettings.EXTRA_SLOT_ID, 0);
        mSim2 = findPreference("sim_2");        
        mSim2.getIntent().putExtra(SimSettings.EXTRA_SLOT_ID, 1);
        mData = (SwitchPreference)findPreference("data_switch");  
        mData.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference,
					Object objValue) {
				if (preference == mData) {
					boolean isChecked =  (Boolean)objValue;
				   	mTelephonyManager.setDataEnabled(isChecked);
					return true;
				}
				return false;
			}
		});
        
        
        mNumSlots = mTelephonyManager.getSimCount();
        mSimCards = (PreferenceScreen)findPreference(SIM_CARD_CATEGORY);
        mAvailableSubInfos = new ArrayList<SubscriptionInfo>(mNumSlots);
        mSelectableSubInfos = new ArrayList<SubscriptionInfo>();
        SimSelectNotification.cancelNotification(this);

        /// M: for [SIM Hot Swap], [SIM Radio On/Off] etc.
        initForSimStateChange();

        /// M: for Plug-in @{
        mSimManagementExt = UtilsExt.getSimManagmentExtPlugin(this);
        mMiscExt = UtilsExt.getMiscPlugin(this);
      
        
        mTelephonyManager.listen(
                mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        mSelectData = findPreference(KEY_CELLULAR_DATA);
        mSelectCall = findPreference(KEY_CALLS);
    }
    
     
    private void updateSimInfo() {
    	
		mData.setChecked(mTelephonyManager.getDataEnabled());

		final SubscriptionInfo sir1 = mSubscriptionManager
				.getActiveSubscriptionInfoForSimSlotIndex(0);
		if (sir1 != null) {
			if (!TextUtils.isEmpty(sir1.getDisplayName())) {
				mSim1.setTitle(sir1.getDisplayName().toString() + 1);
			} else {
				String simCarrierName = mTelephonyManager
						.getSimOperatorNameForSubscription(sir1
								.getSubscriptionId());
				if (!TextUtils.isEmpty(simCarrierName)) {
					mSim1.setTitle(simCarrierName + 1);
				} else {
					mSim1.setTitle(getString(R.string.sub_1));
				}
			}
		} else {
			mSim1.setTitle(getString(R.string.sub_1));
		}

		final SubscriptionInfo sir2 = mSubscriptionManager
				.getActiveSubscriptionInfoForSimSlotIndex(1);
		if (sir2 != null) {
			if (!TextUtils.isEmpty(sir2.getDisplayName())) {
				mSim2.setTitle(sir2.getDisplayName().toString() + 2);
			} else {
				String simCarrierName = mTelephonyManager
						.getSimOperatorNameForSubscription(sir2
								.getSubscriptionId());
				if (!TextUtils.isEmpty(simCarrierName)) {
					mSim2.setTitle(simCarrierName + 2);
				} else {
					mSim2.setTitle(getString(R.string.sub_2));
				}
			}
		} else {
			mSim2.setTitle(getString(R.string.sub_2));
		}

		mSim1.setEnabled(sir1 != null);
		mSim2.setEnabled(sir2 != null);
		
        PreferenceScreen prefSet = getPreferenceScreen();
        boolean isShowSelect = sir1 != null && sir2 != null 
        		&& TelephonyUtils.isRadioOn(sir1.getSubscriptionId(), mContext) 
        		&& TelephonyUtils.isRadioOn(sir2.getSubscriptionId(), mContext) ;

		if(isShowSelect) {
		    prefSet.addPreference(mSelectData);
		    prefSet.addPreference(mSelectCall);
		} else {
	        prefSet.removePreference(mSelectData);
	        prefSet.removePreference(mSelectCall);
		}
		
		saveSimNameToDb(0);
		saveSimNameToDb(1);
  }
    
    private boolean isAllRadioOff(){
    	for (SubscriptionInfo subinfo :mSelectableSubInfos) {
    		if(TelephonyUtils.isRadioOn(subinfo.getSubscriptionId(), this)){
    			return false;
    		}
    	}
    	return true;
    }
    
    //add by lgy for 3059
    private void saveSimNameToDb(int slot) {
		final SubscriptionInfo sir = mSubscriptionManager
				.getActiveSubscriptionInfoForSimSlotIndex(slot);
		if (sir != null) {
			if(sir.getNameSource() != SubscriptionManager.NAME_SOURCE_USER_INPUT) {
				String name = sir.getDisplayName().toString();
				if (!TextUtils.isEmpty(name) && !name.startsWith("CARD")) {		
						mSubscriptionManager.setDisplayName(name, sir.getSubscriptionId(),
								SubscriptionManager.NAME_SOURCE_USER_INPUT);
					
				}
			}
		}
    }
}
