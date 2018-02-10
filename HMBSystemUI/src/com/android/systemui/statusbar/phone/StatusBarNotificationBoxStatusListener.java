package com.android.systemui.statusbar.phone;

/**
 * This class is used to listen to the status change of notification box in status bar.
 * @author ShenQianfeng
 *
 */

public interface StatusBarNotificationBoxStatusListener {
    /**
     * @param on true indicates notification box is on, false indicates notification box is off.
     */
    public void onStatusBarNotificationBoxStatusChange(boolean on);
}
