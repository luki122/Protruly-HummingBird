package com.android.settings;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.Selection;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.ImageView;

import com.android.internal.view.RotationPolicy;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.applications.ApplicationsState;
import com.hb.themeicon.theme.IconManager;

import java.text.Collator;
import java.util.Comparator;

import hb.preference.PreferenceScreen;

import static android.provider.Settings.Secure.SCREENSAVER_ENABLED;

/**
 * Created by liuqin on 17-3-22.
 */
public class LocalSettings {
    public static final int FLAG_ENABLED = 1;
    public static final int FLAG_DISABLED = 0;

    public static final class Secure {
        // 首次启动
        public static final String INIT_ON_FIRST_BOOT = "settings_init_on_first_boot";
        // 折叠非优先的通知图标
        public static final String NOTIFICATION_FOLD_NON_PRIORITY_NOTIFICATION = "notification_fold_non_priority_notification";
        // 显示实时网速
        public static final String NOTIFICATION_SHOW_NETWORK_SPEED = "notification_show_network_speed";
        // 电量百分比
        public static final String NOTIFICATION_SHOW_BATTERY_PERCENTAGE = "notification_show_battery_percentage";
        // 锁屏时可下拉
        public static final String NOTIFICATION_DRAG_DOWN_ON_LOCK_SCREEN = "notification_drag_down_on_lock_screen";
        // 使用消息盒子
        public static final String NOTIFICATION_USE_MESSAGE_BOX = "notification_use_message_box";
        // 三指截屏
        public static final String THREE_FINGER_CAPTURE = "three_finger_capture";
        // 虚拟键可隐藏
        public static final String HIDE_NAVIGATION_BAR = "hide_navigation_bar";
        // 虚拟键位置
        public static final String NAVIGATION_KEY_POSITION = "navigation_key_position";
    }


    /**
     * 获取系统设置键值
     *
     * @param context the context
     * @param key     the key
     * @param def     不存在该值时默认返回的值
     * @return true为开启，false为关闭
     * @date Liuqin on 2017-03-23
     */
    public static boolean isSettingsEnabled(Context context, String key, int def) {
        return Settings.Secure.getInt(context.getContentResolver(), key, def) != 0;
    }

    /**
     * Sets settings enable.
     *
     * @param context  the context
     * @param key      the key
     * @param isEnable the is enable
     * @date Liuqin on 2017-06-12
     */
    public static void setSettingsEnable(Context context, String key, boolean isEnable) {
        Settings.Secure.putInt(context.getContentResolver(), key, isEnable ? FLAG_ENABLED : FLAG_DISABLED);
    }

    public static boolean getLockscreenNotificationsEnabled(Context context) {
        return !isSecureNotificationsDisabled(context) && !isUnredactedNotificationsDisabled(context)
                && Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, FLAG_DISABLED) != FLAG_DISABLED;
    }

    public static boolean isSecureNotificationsDisabled(Context context) {
        final DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && (dpm.getKeyguardDisabledFeatures(null)
                & DevicePolicyManager.KEYGUARD_DISABLE_SECURE_NOTIFICATIONS) != 0;
    }

    public static boolean isUnredactedNotificationsDisabled(Context context) {
        final DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && (dpm.getKeyguardDisabledFeatures(null)
                & DevicePolicyManager.KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS) != 0;
    }

    public static class NotificationFilter implements ApplicationsState.AppFilter {
        public int banned;
        public int nonBanned;

        public void init() {
            banned = 0;
            nonBanned = 0;
        }

        public boolean filterApp(ApplicationsState.AppEntry entry) {
            boolean result = false;
            if ((entry.info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                result = true;
            } else if ((entry.info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                result = true;
            } else if (entry.hasLauncherEntry) {
                result = true;
            }
            if(result && (entry.extraInfo instanceof NotificationBackend.AppRow)) {
                countBanned(entry);
            }
            return result;
        }

        private void countBanned(ApplicationsState.AppEntry entry) {
            if (entry.extraInfo != null
                    && ((NotificationBackend.AppRow)entry.extraInfo).banned) {
                banned++;
            } else {
                nonBanned++;
            }
        }

    }

    /**
     * The constant NOTIFICATION_FILTER.
     *
     * @date Liuqin on 2017-03-23
     */
    public static final NotificationFilter NOTIFICATION_FILTER = new NotificationFilter();

    /**
     * app通知管理列表按禁止与否排序
     *
     * @date Liuqin on 2017-03-23
     */
    public static final Comparator<ApplicationsState.AppEntry> APP_NOTIFICATION_TYPE_COMPARATOR =
            new Comparator<ApplicationsState.AppEntry>() {
                private final Collator sCollator = Collator.getInstance();

                @Override
                public int compare(ApplicationsState.AppEntry appEntryLeft, ApplicationsState.AppEntry appEntryRight) {
                    if(appEntryLeft.extraInfo == null || appEntryRight.extraInfo == null
                            || !(appEntryLeft.extraInfo instanceof NotificationBackend.AppRow)
                            || !(appEntryRight.extraInfo instanceof NotificationBackend.AppRow)) {
                        return 0;
                    }
                    int leftValue = ((NotificationBackend.AppRow)(appEntryLeft.extraInfo)).banned ? 0 : 1;
                    int rightValue = ((NotificationBackend.AppRow)(appEntryRight.extraInfo)).banned ? 0 : 1;

                    if (leftValue == rightValue) {
                        return sCollator.compare(appEntryLeft.label, appEntryRight.label);
                    }

                    return leftValue - rightValue;
                }
    };

    public static final class Enabler {
        public static final boolean ENABLE_NOTIFCATION_SETUP_AFTER_PASSWORD_ADDED = false;
        public static final boolean ENABLE_INPUTMETHOD_VOICE = false;
        public static final boolean ENABLE_SCREEN_ROTATE = false;
        public static final boolean ENABLE_CUSTOM_NUM_KEYBOARD = true;
        public static final boolean ENABLE_TIP_IN_CHOOSE_LOCKTYPE_ACCESSIBILITY_ON = false;
        public static final boolean ENABLER_FORCE_SUBSETTINGS = true;
    }

    public static final int EDITTEXT_GENERAL_MIN_LENGTH = 1;
    public static final int EDITTEXT_GENERAL_MAX_LENGTH = 32;
    public static final int DIGITAL_PASSWORD_MAX_LENGTH = 6;
    public static final int PASSWORD_MAX_LENGTH = 16;

    public static final String KEY_IS_GUIDE = "isGuide";
    public static final String KEY_GUIDE_FROM = "guideFrom";
    public static final String GUIDE_FROM_WIFI = "WIFI";

    public static final class Layout {
        public static final int PREFERENCE_CATEGORY_MATERIAL = R.layout.preference_category_material_hb;
    }

    public static final class Drawable {
        public static final int IC_MENU_ADD = com.hb.R.drawable.ic_menu_add;
        public static final int IC_MENU_REFRESH = com.hb.R.drawable.ic_refresh;
    }

    public static android.graphics.drawable.Drawable getIconDrawable(Context context, String packageName) {
        return IconManager.getInstance(context.getApplicationContext(), true, false)
                .getIconDrawable(packageName, UserHandle.CURRENT);
    }

    public static void startRotateAnimate(View rotateView) {
        startRotateAnimate(rotateView, 1000);
    }

    public static void startRotateAnimate(View rotateView, int repeatCount) {
        startRotateAnimate(rotateView, repeatCount, 2500);
    }

    public static void startRotateAnimate(View rotateView, int repeatCount, int duration) {
        int times = repeatCount;
        long totalDuration = duration * times;

        Interpolator lin = new LinearInterpolator();
        Animation am = new RotateAnimation(0, - (360 * times), Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        am.setDuration(totalDuration);
        am.setRepeatCount(0);
        am.setInterpolator(lin);
        am.setFillAfter(false);
//        am.setAnimationListener(new Animation.AnimationListener() {
//            @Override
//            public void onAnimationStart(Animation animation) {
//            }
//
//            @Override
//            public void onAnimationRepeat(Animation animation) {
//            }
//
//            @Override
//            public void onAnimationEnd(Animation animation) {
//            }
//        });
        stopAnimation(rotateView);
        rotateView.startAnimation(am);
    }

    public static void stopAnimation(View rotateView) {
        if (rotateView != null) {
            Animation animation = rotateView.getAnimation();
            if (animation != null) {
                animation.cancel();
                rotateView.setAnimation(null);
            }
        }
    }

    public static boolean isAnimating(View rotateView) {
        Animation animation = rotateView.getAnimation();
        return animation != null && !animation.hasEnded();
    }

    public static void disableScreenRotate(Activity activity) {
        if (!LocalSettings.Enabler.ENABLE_SCREEN_ROTATE) {
            if (activity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
    }

    public static void limitEdittextMaxLen(EditText editText) {
        limitEdittextMaxLen(editText, LocalSettings.EDITTEXT_GENERAL_MAX_LENGTH);
    }

    public static void limitEdittextMaxLen(EditText editText, int maxLen) {
        if (editText == null) {
            return;
        }
        int maxLength = LocalSettings.EDITTEXT_GENERAL_MAX_LENGTH;
        int length = editText.length();
        if(length > maxLength) {
            Editable editable = (Editable) editText.getText();
            String str = editable.toString();
            String newStr = str.substring(0, maxLength);
            editText.setText(newStr);
            editable = (Editable) editText.getText();
            Selection.setSelection(editable, maxLength);
        }
    }

    public static boolean isRestricted(Context context) {
        final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        return um.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION);
    }

    /**
     * Init on first boot
     *
     * @param context the context
     * @date Liuqin on 2017-06-12
     */
    public static void initOnFirstBoot(Context context) {
        if (!isSettingsEnabled(context, Secure.INIT_ON_FIRST_BOOT, FLAG_DISABLED)) {
            // 关闭互动屏保
//            setSettingsEnable(context, SCREENSAVER_ENABLED, false);
            // 自动旋转屏幕
//            RotationPolicy.setRotationLock(context, false);
            // 自动调节亮度
//            Settings.System.putInt(context.getContentResolver(),
//                    android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
//                    android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
            if (!isRestricted(context)) {
                // 开启gps
                Settings.Secure.putInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE,
                        android.provider.Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);
            }
            // 关闭锁屏通知
//            setSettingsEnable(context, Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, false);

            setSettingsEnable(context, Secure.INIT_ON_FIRST_BOOT, true);
        }
    }

    private static int sPreferenceScreenLayout = 0;

    private static int sPreferenceScreenWidgetLayout = 0;

    public static int getPreferenceScreenLayout(Context context) {
        if (sPreferenceScreenLayout == 0) {
            sPreferenceScreenLayout = new PreferenceScreen(context, null).getLayoutResource();
        }
        return sPreferenceScreenLayout;
    }

    public static int getPreferenceScreenWidgetLayout(Context context) {
        if (sPreferenceScreenWidgetLayout == 0) {
            sPreferenceScreenWidgetLayout = new PreferenceScreen(context, null).getWidgetLayoutResource();
        }
        return sPreferenceScreenWidgetLayout;
    }

    public static int getPreferenceScreenLayout2(Context context) {
        return com.hb.R.layout.preference_material_hb;
    }

    public static int getTingColor(Context context) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorAccent, outValue, true);
        return outValue.data;
    }
}
