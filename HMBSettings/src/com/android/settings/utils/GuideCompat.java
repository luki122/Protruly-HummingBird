package com.android.settings.utils;

import android.app.Activity;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ChooseLockGeneric;
import com.android.settings.ChooseLockPattern;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.FullActivityBase;
import com.android.settings.LocalSettings;
import com.android.settings.R;
import com.android.settings.fingerprint.FingerprintEnrollEnrolling;
import com.android.settings.fingerprint.RenameFingerprint;
import com.mediatek.settings.sim.TelephonyUtils;

import java.util.List;

import hb.app.HbActivity;
import hb.preference.PreferenceFragment;

/**
 * Created by liuqin.
 *
 * @date Liuqin on 2017-05-27
 */
public class GuideCompat {
    public static final int GUIDE_MENU_SKIP = Menu.FIRST + 100;
    private GuideMenuListener mGuideMenuListener;
    private HbActivity activity;
    private NetworkStateChangeReceiver mReceiver;
    public static boolean sIsOpenMobileData = false;

    public GuideCompat(Activity activity) {
        this.activity = (HbActivity) activity;
    }

    public static boolean checkGuide(Activity activity) {
//        if (LocalSettings.isSettingsEnabled(activity, Settings.Secure.USER_SETUP_COMPLETE, LocalSettings.FLAG_DISABLED)) {
//            return false;
//        }
        return activity.getIntent().getBooleanExtra(LocalSettings.KEY_IS_GUIDE, false);
    }

    public void guideInitToolbar(PreferenceFragment fragment, GuideMenuListener listener) {
        fragment.setHasOptionsMenu(true);
        FullActivityBase activityBase = (FullActivityBase) getActivity();
        activityBase.setDisplayHomeAsUpEnabled(true);
        activityBase.getActionBar().show();
        activityBase.setTitle(R.string.wizard_back);
        activityBase.initToolbar();
        activityBase.getToolbar().setTitle(R.string.wizard_back);

        if (listener != null) {
            guideSetMenuListener(listener);
        }
    }

    /**
     * 初始化其它解锁方式
     *
     * @date Liuqin on 2017-05-27
     */
    public void guideInitSwitchLockTypeBtn() {
        TextView switchLockTypeView = guideGetSwitchLockTypeBtn();
        switchLockTypeView.setVisibility(View.VISIBLE);
        switchLockTypeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                guideOnSwitchLockTypeClick();
            }
        });
    }

    public void guideOnSwitchLockTypeClick() {
        FingerprintManager mFingerprintManager = (FingerprintManager) activity.getSystemService(
                Context.FINGERPRINT_SERVICE);
        long challenge = mFingerprintManager.preEnroll();
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", ChooseLockGeneric.class.getName());
        intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
        intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_DISABLED_PREFS,
                true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, challenge);

        intent.putExtra(LocalSettings.KEY_IS_GUIDE, true);
        activity.startActivity(intent);

        getActivity().finish();
    }

    public void guideLockscreenInit(PreferenceFragment fragment) {
        guideLockscreenInit(fragment, new GuideMenuListener() {
            @Override
            public void onSkipMenuClick() {
                guideReturn();
            }

            @Override
            public void onNavigationBackClick() {
                if (guideIsFromWifiSettings()) {
                    guideLaunchWifiSettings();
                } else {
                    guideOnSwitchLockTypeClick();
                }
            }
        });
    }

    public void guideLockscreenInit(PreferenceFragment fragment, GuideMenuListener listener) {
        guideInitToolbar(fragment, listener);
        guideInitSwitchLockTypeBtn();

        View container = getActivity().findViewById(R.id.guide_button_container);
        container.setVisibility(View.VISIBLE);
    }

    /**
     * 获取继续按钮
     *
     * @return the button
     * @date Liuqin on 2017-05-27
     */
    public Button guideGetPositiveBtn() {
        return (Button)getActivity().findViewById(R.id.guide_positive_btn);
    }

    public TextView guideGetSwitchLockTypeBtn() {
        return (TextView)getActivity().findViewById(R.id.guide_switch_lock_type);
    }

    public void guideAddOptionMenu(Menu menu, MenuInflater inflater) {
        MenuItem skipMenuItem = menu.add(0, GUIDE_MENU_SKIP, 0, R.string.skip_label);
        skipMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (mGuideMenuListener != null) {
                    mGuideMenuListener.onSkipMenuClick();
                } else {
                    getActivity().finish();
                }
                return true;
            }
        });
        skipMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        skipMenuItem.setVisible(true);
        TextView skipView = (TextView) getActivity().findViewById(GUIDE_MENU_SKIP);
        skipView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
    }

    public void guideSetMenuListener(GuideMenuListener listener) {
        this.mGuideMenuListener = listener;
        FullActivityBase activityBase = (FullActivityBase) getActivity();
        activityBase.setOnToolbarListener(new FullActivityBase.OnToolbarListener() {
            @Override
            public void onNavigationBackClick() {
                if (mGuideMenuListener != null) {
                    mGuideMenuListener.onNavigationBackClick();
                }
            }
        });
    }

    public void guideSetIntent(Intent intent) {
        if (intent != null) {
            intent.putExtra(LocalSettings.KEY_IS_GUIDE, true);
            activity.getWindow().getDecorView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    getActivity().finish();
                }
            }, 1000);
        }
    }

    /**
     * 设置完密码后的处理
     *
     * @param token the token
     * @date Liuqin on 2017-05-27
     */
    public void guideOnSetPasswordFinish(byte[] token) {
        FingerprintManager fpm = (FingerprintManager) getActivity().getSystemService(
                Context.FINGERPRINT_SERVICE);
        if (fpm.isHardwareDetected() && guideHasScreenLock() && token != null) {
            guideStartAddFingerprintActivity(token);
        } else {
            guideReturn();
        }
    }

    private void guideStartAddFingerprintActivity(byte[] token) {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings",
                FingerprintEnrollEnrolling.class.getName());
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, token);
        intent.putExtra(LocalSettings.KEY_IS_GUIDE, true);
        activity.startActivity(intent);
    }

    public boolean guideHasScreenLock() {
        boolean result = true;
        LockPatternUtils mLockPatternUtils = new LockPatternUtils(getActivity());
        if (!mLockPatternUtils.isSecure(UserHandle.myUserId())) {
            result = false;
        }
        return result;
    }

    /**
     * 返回到开机引导
     *
     * @date Liuqin on 2017-05-27
     */
    public void guideReturn() {
        Intent next = new Intent("com.android.provision.TRANSFER");
        next.putExtra("from", "setting");
        activity.startActivity(next);
        getActivity().finish();

        postFingerprintEnroll();
    }

    private void doFinish() {
        activity.finish();
    }

    private Activity getActivity() {
        return activity;
    }

    public void guideEnsureWifiOn() {
        WifiManager mWifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        int wifiState = mWifiManager.getWifiState();
        if (wifiState != WifiManager.WIFI_STATE_ENABLED && wifiState != WifiManager.WIFI_STATE_ENABLING) {
            mWifiManager.setWifiEnabled(true);
        }
    }

    public static boolean guideIsSimCardReady(Context context) {
        boolean mIsAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(context.getApplicationContext());
        TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Service.TELEPHONY_SERVICE);
        return tm.getSimState() == TelephonyManager.SIM_STATE_READY
                 && !mIsAirplaneModeOn;
    }

//    public static boolean guideIsMobileDataSwitchClickable(Context context) {
//        final boolean ecbMode = SystemProperties.getBoolean(
//                TelephonyProperties.PROPERTY_INECM_MODE, false);
//        boolean mIsAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(context.getApplicationContext());
//        SubscriptionInfo sir = guideGetFirstActiveSimCard(context);
//        return sir != null
//                && (!TelephonyUtils.isCapabilitySwitching())
//                && (!mIsAirplaneModeOn)
//                && !TelecomManager.from(context).isInCall()
//                && !ecbMode;
//    }

    public static SubscriptionInfo guideGetFirstActiveSimCard(Context context) {
        SubscriptionInfo sir = null;
        SubscriptionManager mSubscriptionManager = SubscriptionManager.from(context);
        List<SubscriptionInfo> infoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (infoList != null &&  !infoList.isEmpty()) {
            sir = infoList.get(0);
        }
        return sir;
    }

    public static SubscriptionInfo guideGetDefaultSimCard(Context context) {
        SubscriptionManager mSubscriptionManager = SubscriptionManager.from(context);
        return mSubscriptionManager.getDefaultDataSubscriptionInfo();
    }

    public static void guideSelectFirstSimCardAsDefault(Context context) {
        if (guideGetDefaultSimCard(context) != null) {
            return;
        }
        SubscriptionInfo sir = guideGetFirstActiveSimCard(context);
        if (sir != null) {
            guideSetSimCardAsDefault(context, sir);
        }
    }

    public static void guideSetSimCardAsDefault(Context context, SubscriptionInfo sir) {
        if (sir != null) {
            SubscriptionManager mSubscriptionManager = SubscriptionManager.from(context);
            mSubscriptionManager.setDefaultDataSubId(sir.getSubscriptionId());
        }
    }

    public static boolean guideIsMobileNetworkEnabled(Context context) {
        SubscriptionManager mSubscriptionManager = SubscriptionManager.from(context);
        SubscriptionInfo sir = mSubscriptionManager.getDefaultDataSubscriptionInfo();
        if (sir == null) {
            sir = guideGetFirstActiveSimCard(context);
        }
        if (sir != null) {
            TelephonyManager tm =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            return tm.getDataEnabled(sir.getSubscriptionId());
        }
        return false;
    }

    public static boolean guideSetMobileDataEnabled(Context context, boolean enabled) {
        boolean result = false;
        SubscriptionManager mSubscriptionManager = SubscriptionManager.from(context);
        SubscriptionInfo sir = mSubscriptionManager.getDefaultDataSubscriptionInfo();
        if (sir == null) {
            sir = guideGetFirstActiveSimCard(context);
            if (sir != null) {
                guideSetSimCardAsDefault(context, sir);
            }
        }

        if (sir != null) {
            TelephonyManager tm =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            tm.setDataEnabled(sir.getSubscriptionId(), enabled);
            result = true;
        }
        return result;
    }

    public void guideLaunchLockPattern() {
        if (!guideHasScreenLock()) {
            FingerprintManager fpm = (FingerprintManager) getActivity().getSystemService(
                    Context.FINGERPRINT_SERVICE);
            long challenge = fpm.isHardwareDetected() ? fpm.preEnroll() : 0;
            Intent intent = ChooseLockPattern.createIntent(getActivity(), false, challenge);
            intent.putExtra(LocalSettings.KEY_IS_GUIDE, true);
            intent.putExtra(LocalSettings.KEY_GUIDE_FROM, LocalSettings.GUIDE_FROM_WIFI);
            getActivity().startActivity(intent);
            doFinish();
        } else {
            guideReturn();
        }
    }

    public void guideLaunchWifiSettings() {
        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        intent.putExtra(LocalSettings.KEY_IS_GUIDE, true);
        getActivity().startActivity(intent);
        doFinish();
    }

    public void guideSetSkipBtnVisible(boolean visible) {
        TextView skipView = (TextView) getActivity().findViewById(GUIDE_MENU_SKIP);
        if (skipView != null) {
            int visibility = visible ? View.VISIBLE : View.INVISIBLE;
            skipView.setVisibility(visibility);
        }
    }

    public void guideSetSwitchOtherLockTypeBtnVisible(boolean visible) {
        View view = guideGetSwitchLockTypeBtn();
        if (view != null) {
            int visibility = visible ? View.VISIBLE : View.INVISIBLE;
            view.setVisibility(visibility);
        }
    }

    public interface GuideMenuListener {
        void onSkipMenuClick();
        void onNavigationBackClick();
    }

    public interface GuideNetworkStateChangeListener {
        void onNetworkConnectedChange(boolean isConnected);
        void onSimCardStateChange(boolean isReady);
    }

    public void guideRegisterNetworkStateListener(Context context, boolean register,
                                                  GuideNetworkStateChangeListener listener) {
        try {
            if (register) {
                guideRegisterNetworkStateListener(context, false, listener);

                mReceiver = new NetworkStateChangeReceiver();
                mReceiver.setGuideNetowkStateChangeListener(listener);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
                context.registerReceiver(mReceiver, filter);

                mReceiver.registerAirplaneModeListener(context);
            } else if (mReceiver != null) {
                context.unregisterReceiver(mReceiver);

                mReceiver.unRegisterAirplaneModeListener(context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean guideIsNetworkConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

//    public static boolean guideIsMobileConnected(Context context) {
//        ConnectivityManager manager = (ConnectivityManager) context
//                .getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
//        return activeNetwork != null && activeNetwork.isConnected()
//                && activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;
//    }

    public static class NetworkStateChangeReceiver extends BroadcastReceiver {
        private GuideNetworkStateChangeListener mGuideNetowkStateChangeListener;
        private Context mContext;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mGuideNetowkStateChangeListener == null) {
                return;
            }
            String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                mGuideNetowkStateChangeListener
                        .onNetworkConnectedChange(guideIsNetworkConnected(context));
            } else if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
                mGuideNetowkStateChangeListener.onSimCardStateChange(guideIsSimCardReady(context));
            }
        }

        public void setGuideNetowkStateChangeListener(
                GuideNetworkStateChangeListener mGuideNetowkStateChangeListener) {
            this.mGuideNetowkStateChangeListener = mGuideNetowkStateChangeListener;
        }

        private ContentObserver mAirplaneModeObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                Context context = mContext;
                if (context != null) {
                    mGuideNetowkStateChangeListener.onSimCardStateChange(guideIsSimCardReady(context));
                }
            }
        };

        private void registerAirplaneModeListener(Context context) {
            context.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON), true,
                    mAirplaneModeObserver);
            this.mContext = context.getApplicationContext();
        }

        private void unRegisterAirplaneModeListener(Context context) {
            context.getContentResolver().unregisterContentObserver(mAirplaneModeObserver);
            this.mContext = null;
        }
    }

    private void postFingerprintEnroll() {
        FingerprintManager fpm = (FingerprintManager) getActivity().getSystemService(
                Context.FINGERPRINT_SERVICE);
        if (fpm.isHardwareDetected()) {
            fpm.postEnroll();
        }
    }

    public void cancelBackKey() {
        FullActivityBase activityBase = (FullActivityBase) getActivity();
        activityBase.setOnToolbarListener(new FullActivityBase.OnToolbarListener() {
            @Override
            public void onNavigationBackClick() {
            }
        });
    }

    public static boolean isOpenMobileData() {
        return sIsOpenMobileData;
    }

    public static void setOpenMobileData() {
        sIsOpenMobileData = true;
    }

    public void guideOpenSimCard() {
        if (GuideCompat.isOpenMobileData()) {
            // 初始化选择一张默认卡
            GuideCompat.guideSelectFirstSimCardAsDefault(activity.getApplicationContext());
        } else {
            // 初始化选择一张默认卡并打开网络
            if (!GuideCompat.guideIsMobileNetworkEnabled(activity.getApplicationContext())) {
                GuideCompat.guideSetMobileDataEnabled(activity.getApplicationContext(), true);
            }
        }
    }

    public boolean guideIsFromWifiSettings() {
        String from = getActivity().getIntent().getStringExtra(LocalSettings.KEY_GUIDE_FROM);
        return !TextUtils.isEmpty(from) && from.equals(LocalSettings.GUIDE_FROM_WIFI);
    }

    public void clearLockscreen() {
        LockPatternUtils mLockPatternUtils = new LockPatternUtils(getActivity());
        mLockPatternUtils.clearLock(UserHandle.myUserId());
        mLockPatternUtils.setLockScreenDisabled(false, UserHandle.myUserId());
    }

    public void removeAllFp() {
        FingerprintManager mFingerprintManager = (FingerprintManager) activity.getSystemService(
                Context.FINGERPRINT_SERVICE);
        final List<Fingerprint> items = mFingerprintManager.getEnrolledFingerprints();
        final int fingerprintCount = items.size();
        for (int i = 0; i < fingerprintCount; i++) {
            mFingerprintManager.remove(items.get(i), null);
        }
    }

    public int getFpCount() {
        FingerprintManager mFingerprintManager = (FingerprintManager) activity.getSystemService(
                Context.FINGERPRINT_SERVICE);
        final List<Fingerprint> items = mFingerprintManager.getEnrolledFingerprints();
        return items != null ? items.size() : 0;
    }
}
