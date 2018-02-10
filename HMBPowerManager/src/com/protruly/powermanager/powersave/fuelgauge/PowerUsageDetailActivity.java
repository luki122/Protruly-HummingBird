package com.protruly.powermanager.powersave.fuelgauge;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.protruly.powermanager.R;
import com.protruly.powermanager.utils.LogUtils;
import com.protruly.powermanager.utils.Utils;

import hb.preference.Preference;
import hb.preference.PreferenceActivity;
import hb.preference.PreferenceCategory;

public class PowerUsageDetailActivity extends PreferenceActivity {
    private static final String TAG = "PowerUsageDetailActivity";

    public static void startBatteryDetailPage(Context context, BatteryStatsHelper helper,
                                              BatteryEntry entry) {
        // Initialize mStats if necessary.
        helper.getStats();

        Bundle args = new Bundle();
        args.putString(PowerUsageDetailActivity.EXTRA_TITLE, entry.name);
        args.putDouble(PowerUsageDetailActivity.EXTRA_PERCENT, entry.sipper.percent);
        args.putLong(PowerUsageDetailActivity.EXTRA_USAGE_DURATION, helper.getStatsPeriod());
        LogUtils.d(TAG, "startBatteryDetailPage() -> getStatsPeriod = "
                + helper.getStatsPeriod() + ", usageTimeMs = " + entry.sipper.usageTimeMs);
//        args.putLong(PowerUsageDetailActivity.EXTRA_USAGE_DURATION, entry.sipper.usageTimeMs);
        args.putString(PowerUsageDetailActivity.EXTRA_ICON_PACKAGE, entry.defaultPackageName);
        args.putInt(PowerUsageDetailActivity.EXTRA_ICON_ID, entry.iconId);
        args.putDouble(PowerUsageDetailActivity.EXTRA_NO_COVERAGE, entry.sipper.noCoveragePercent);
        if (entry.sipper.uidObj != null) {
            args.putInt(PowerUsageDetailActivity.EXTRA_UID, entry.sipper.uidObj.getUid());
        }
        args.putSerializable(PowerUsageDetailActivity.EXTRA_DRAIN_TYPE, entry.sipper.drainType);

        int[] types;
        double[] values;
        switch (entry.sipper.drainType) {
            case APP:
            case USER:
            {
                types = new int[] {
                        R.string.usage_type_cpu,
                        R.string.usage_type_cpu_foreground,
                        R.string.usage_type_wake_lock,
                        R.string.usage_type_gps,
                        R.string.usage_type_wifi_running,
                        R.string.usage_type_data_recv,
                        R.string.usage_type_data_send,
                        R.string.usage_type_radio_active,
                        R.string.usage_type_data_wifi_recv,
                        R.string.usage_type_data_wifi_send,
                        R.string.usage_type_audio,
                        R.string.usage_type_video,
                        R.string.usage_type_camera,
                        R.string.usage_type_flashlight,
                        R.string.usage_type_computed_power,
                };
                values = new double[] {
                        entry.sipper.cpuTimeMs,
                        entry.sipper.cpuFgTimeMs,
                        entry.sipper.wakeLockTimeMs,
                        entry.sipper.gpsTimeMs,
                        entry.sipper.wifiRunningTimeMs,
                        entry.sipper.mobileRxPackets,
                        entry.sipper.mobileTxPackets,
                        entry.sipper.mobileActive,
                        entry.sipper.wifiRxPackets,
                        entry.sipper.wifiTxPackets,
                        0,
                        0,
                        entry.sipper.cameraTimeMs,
                        entry.sipper.flashlightTimeMs,
                        entry.sipper.totalPowerMah,
                };
            }
            break;
            case CELL:
            {
                types = new int[] {
                        R.string.usage_type_on_time,
                        R.string.usage_type_no_coverage,
                        R.string.usage_type_radio_active,
                        R.string.usage_type_computed_power,
                };
                values = new double[] {
                        entry.sipper.usageTimeMs,
                        entry.sipper.noCoveragePercent,
                        entry.sipper.mobileActive,
                        entry.sipper.totalPowerMah,
                };
            }
            break;
            case WIFI:
            {
                types = new int[] {
                        R.string.usage_type_wifi_running,
                        R.string.usage_type_cpu,
                        R.string.usage_type_cpu_foreground,
                        R.string.usage_type_wake_lock,
                        R.string.usage_type_data_recv,
                        R.string.usage_type_data_send,
                        R.string.usage_type_data_wifi_recv,
                        R.string.usage_type_data_wifi_send,
                        R.string.usage_type_computed_power,
                };
                values = new double[] {
                        entry.sipper.wifiRunningTimeMs,
                        entry.sipper.cpuTimeMs,
                        entry.sipper.cpuFgTimeMs,
                        entry.sipper.wakeLockTimeMs,
                        entry.sipper.mobileRxPackets,
                        entry.sipper.mobileTxPackets,
                        entry.sipper.wifiRxPackets,
                        entry.sipper.wifiTxPackets,
                        entry.sipper.totalPowerMah,
                };
            } break;
            case BLUETOOTH:
            {
                types = new int[] {
                        R.string.usage_type_on_time,
                        R.string.usage_type_cpu,
                        R.string.usage_type_cpu_foreground,
                        R.string.usage_type_wake_lock,
                        R.string.usage_type_data_recv,
                        R.string.usage_type_data_send,
                        R.string.usage_type_data_wifi_recv,
                        R.string.usage_type_data_wifi_send,
                        R.string.usage_type_computed_power,
                };
                values = new double[] {
                        entry.sipper.usageTimeMs,
                        entry.sipper.cpuTimeMs,
                        entry.sipper.cpuFgTimeMs,
                        entry.sipper.wakeLockTimeMs,
                        entry.sipper.mobileRxPackets,
                        entry.sipper.mobileTxPackets,
                        entry.sipper.wifiRxPackets,
                        entry.sipper.wifiTxPackets,
                        entry.sipper.totalPowerMah,
                };
            } break;
            case UNACCOUNTED:
            {
                types = new int[] {
                        R.string.usage_type_total_battery_capacity,
                        R.string.usage_type_computed_power,
                        R.string.usage_type_actual_power,
                };
                values = new double[] {
                        helper.getPowerProfile().getBatteryCapacity(),
                        helper.getComputedPower(),
                        helper.getMinDrainedPower(),
                };
            } break;
            case OVERCOUNTED:
            {
                types = new int[] {
                        R.string.usage_type_total_battery_capacity,
                        R.string.usage_type_computed_power,
                        R.string.usage_type_actual_power,
                };
                values = new double[] {
                        helper.getPowerProfile().getBatteryCapacity(),
                        helper.getComputedPower(),
                        helper.getMaxDrainedPower(),
                };
            } break;
            default:
            {
                types = new int[] {
                        R.string.usage_type_on_time,
                        R.string.usage_type_computed_power,
                };
                values = new double[] {
                        entry.sipper.usageTimeMs,
                        entry.sipper.totalPowerMah,
                };
            }
        }
        args.putIntArray(PowerUsageDetailActivity.EXTRA_DETAIL_TYPES, types);
        args.putDoubleArray(PowerUsageDetailActivity.EXTRA_DETAIL_VALUES, values);

        Intent intent = new Intent(context, PowerUsageDetailActivity.class);
        intent.putExtras(args);
        context.startActivity(intent);
    }

    public static final int ACTION_DISPLAY_SETTINGS = 1;
    public static final int ACTION_WIFI_SETTINGS = 2;
    public static final int ACTION_BLUETOOTH_SETTINGS = 3;
    public static final int ACTION_WIRELESS_SETTINGS = 4;
    public static final int ACTION_APP_DETAILS = 5;
    public static final int ACTION_LOCATION_SETTINGS = 6;

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_PERCENT = "percent";
    public static final String EXTRA_UID = "uid";
    public static final String EXTRA_USAGE_DURATION = "duration";
    public static final String EXTRA_DETAIL_TYPES = "types"; // Array of usage types (cpu, gps, etc)
    public static final String EXTRA_DETAIL_VALUES = "values"; // Array of doubles
    public static final String EXTRA_DRAIN_TYPE = "drainType"; // DrainType
    public static final String EXTRA_ICON_PACKAGE = "iconPackage"; // String
    public static final String EXTRA_NO_COVERAGE = "noCoverage";
    public static final String EXTRA_ICON_ID = "iconId"; // Int
    public static final String EXTRA_SHOW_LOCATION_BUTTON = "showLocationButton";  // Boolean

    private static final String KEY_APP_PROGRESS = "app_progress";
    private static final String KEY_DETAILS_PARENT = "details_parent";
    private static final String KEY_CONTROLS_PARENT = "controls_parent";
    private static final String KEY_PACKAGES_PARENT = "packages_parent";

    private int[] mTypes;
    private int mUid;
    private double[] mValues;
    private BatterySipper.DrainType mDrainType;
    private double mNoCoverage; // Percentage of time that there was no coverage

    private AppProgressPreference mAppProgress;
    private PreferenceCategory mDetailsParent;
    private PreferenceCategory mControlsParent;
    private PreferenceCategory mPackagesParent;

    private boolean mUsesGps;
    private boolean mShowLocationButton;

    private String[] mPackages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.activity_power_usage_detail);
        initView();
        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupHeader();
    }

    private void initView() {
        getToolbar().setTitle(getIntent().getExtras().getString(EXTRA_TITLE));
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mAppProgress = (AppProgressPreference) findPreference(KEY_APP_PROGRESS);
        mDetailsParent = (PreferenceCategory) findPreference(KEY_DETAILS_PARENT);
        mControlsParent = (PreferenceCategory) findPreference(KEY_CONTROLS_PARENT);
        mPackagesParent = (PreferenceCategory) findPreference(KEY_PACKAGES_PARENT);
    }

    private void initData() {
        final Bundle args = getIntent().getExtras();
        mUid = args.getInt(EXTRA_UID, 0);
        mPackages = getPackageManager().getPackagesForUid(mUid);
        mDrainType = (BatterySipper.DrainType) args.getSerializable(EXTRA_DRAIN_TYPE);
        mNoCoverage = args.getDouble(EXTRA_NO_COVERAGE, 0);
        mShowLocationButton = args.getBoolean(EXTRA_SHOW_LOCATION_BUTTON);

        mTypes = args.getIntArray(EXTRA_DETAIL_TYPES);
        mValues = args.getDoubleArray(EXTRA_DETAIL_VALUES);

        fillDetailsSection();
        fillPackagesSection(mUid);
        fillControlsSection(mUid);
    }

    private void setupHeader() {
        final Bundle args = getIntent().getExtras();
        String title = args.getString(EXTRA_TITLE);
        double percent = args.getDouble(EXTRA_PERCENT);
        String pkg = args.getString(EXTRA_ICON_PACKAGE);
        int iconId = args.getInt(EXTRA_ICON_ID, 0);
        Drawable appIcon = null;

        if (!TextUtils.isEmpty(pkg)) {
            try {
                final PackageManager pm = getPackageManager();
                ApplicationInfo ai = pm.getPackageInfo(pkg, 0).applicationInfo;
                if (ai != null) {
                    appIcon = ai.loadIcon(pm);
                }
            } catch (PackageManager.NameNotFoundException nnfe) {
                // Use default icon
            }
        } else if (iconId != 0) {
            appIcon = getDrawable(iconId);
        }
        if (appIcon == null) {
            appIcon = getPackageManager().getDefaultActivityIcon();
        }

        mAppProgress.setAppProgress(appIcon, title,
                getResources().getString(R.string.percentage, percent));
    }

    private void fillDetailsSection() {
        if (mTypes != null && mValues != null) {
            for (int i = 0; i < mTypes.length; i++) {
                // Only add an item if the time is greater than zero
                if (mValues[i] <= 0) continue;
                final String label = getString(mTypes[i]);
                String value = null;
                switch (mTypes[i]) {
                    case R.string.usage_type_data_recv:
                    case R.string.usage_type_data_send:
                    case R.string.usage_type_data_wifi_recv:
                    case R.string.usage_type_data_wifi_send:
                        final long packets = (long) (mValues[i]);
                        value = Long.toString(packets);
                        break;
                    case R.string.usage_type_no_coverage:
                        final int percentage = (int) Math.floor(mValues[i]);
                        value = Utils.formatPercentage(percentage);
                        break;
                    case R.string.usage_type_total_battery_capacity:
                    case R.string.usage_type_computed_power:
                    case R.string.usage_type_actual_power:
                        value = getString(R.string.mah, mValues[i]);
                        break;
                    case R.string.usage_type_gps:
                        mUsesGps = true;
                        // Fall through
                    default:
                        value = Utils.formatElapsedTime(this, mValues[i], true);
                }
                LogUtils.d(TAG, "fillDetailsSection() -> label = " + label + ", value = " + value);
                addHorizontalPreference(mDetailsParent, label, value);
            }
        }
    }

    private void addHorizontalPreference(PreferenceCategory parent, CharSequence title,
                                         CharSequence summary) {
        Preference pref = new Preference(this);
        pref.setLayoutResource(R.layout.horizontal_preference);
        pref.setTitle(title);
        pref.setSummary(summary);
//        pref.setSelectable(false);
        parent.addPreference(pref);
    }

    private void fillPackagesSection(int uid) {
        if (uid < 1) {
            getPreferenceScreen().removePreference(mPackagesParent);
            return;
        }
        if (mPackages == null || mPackages.length < 2) {
            getPreferenceScreen().removePreference(mPackagesParent);
            return;
        }

        PackageManager pm = getPackageManager();
        // Convert package names to user-facing labels where possible
        for (int i = 0; i < mPackages.length; i++) {
            try {
                ApplicationInfo ai = pm.getApplicationInfo(mPackages[i], 0);
                CharSequence label = ai.loadLabel(pm);
                if (label != null) {
                    mPackages[i] = label.toString();
                }
                addHorizontalPreference(mPackagesParent, mPackages[i], null);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
    }

    private void fillControlsSection(int uid) {
        PackageManager pm = getPackageManager();
        String[] packages = pm.getPackagesForUid(uid);
        PackageInfo pi = null;
        try {
            pi = packages != null ? pm.getPackageInfo(packages[0], 0) : null;
        } catch (PackageManager.NameNotFoundException nnfe) { /* Nothing */ }
        ApplicationInfo ai = pi != null? pi.applicationInfo : null;

        boolean removeHeader = true;
        switch (mDrainType) {
            case APP:
                // If it is a Java application and only one package is associated with the Uid
                if (packages != null && packages.length == 1) {
//                    addControl(R.string.battery_action_app_details,
//                            R.string.battery_sugg_apps_info, ACTION_APP_DETAILS);
//                    removeHeader = false;
                    // If the application has a settings screen, jump to  that
                    // TODO:
                }
                // If power usage detail page is launched from location page, suppress "Location"
                // button to prevent circular loops.
                if (mUsesGps && mShowLocationButton) {
                    addControl(R.string.location_settings_title,
                            R.string.battery_sugg_apps_gps, ACTION_LOCATION_SETTINGS);
                    removeHeader = false;
                }
                break;
            case SCREEN:
                addControl(R.string.display_settings,
                        R.string.battery_sugg_display,
                        ACTION_DISPLAY_SETTINGS);
                removeHeader = false;
                break;
            case WIFI:
                addControl(R.string.wifi_settings,
                        R.string.battery_sugg_wifi,
                        ACTION_WIFI_SETTINGS);
                removeHeader = false;
                break;
            case BLUETOOTH:
                addControl(R.string.bluetooth_settings,
                        R.string.battery_sugg_bluetooth_basic,
                        ACTION_BLUETOOTH_SETTINGS);
                removeHeader = false;
                break;
            case CELL:
                if (mNoCoverage > 10) {
                    addControl(R.string.radio_controls_title,
                            R.string.battery_sugg_radio,
                            ACTION_WIRELESS_SETTINGS);
                    removeHeader = false;
                }
                break;
        }
        if (removeHeader) {
            getPreferenceScreen().removePreference(mControlsParent);
        }
    }

    private void addControl(int pageSummary, int actionTitle, final int action) {
        Preference pref = new Preference(this);
        pref.setTitle(actionTitle);
        pref.setLayoutResource(R.layout.horizontal_preference);
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                doAction(action);
                return false;
            }
        });
        mControlsParent.addPreference(pref);
    }

    private void doAction(int action) {

        switch (action) {
            case ACTION_DISPLAY_SETTINGS:
                Intent display = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
                startActivity(display);
                break;
            case ACTION_WIFI_SETTINGS:
                Intent wifi = new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivity(wifi);
                break;
            case ACTION_BLUETOOTH_SETTINGS:
                Intent bluetooth = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(bluetooth);
                break;
            case ACTION_WIRELESS_SETTINGS:
                Intent wireless = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(wireless);
                break;
            case ACTION_APP_DETAILS:
                break;
            case ACTION_LOCATION_SETTINGS:
                Intent location = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                startActivity(location);
                break;
        }
    }
}