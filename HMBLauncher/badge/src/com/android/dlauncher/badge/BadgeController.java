package com.android.dlauncher.badge;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.NinePatchDrawable;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;

import com.android.launcher3.AppInfo;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.folder.FolderIcon;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by lijun on 17-8-3.
 */

public class BadgeController extends BroadcastReceiver {

    public static final boolean DEBUG = true;
    private static final String TAG = "BadgeController";

    public static final int MAX_UNREAD_COUNT = 99;

    static final HandlerThread sWorkerThread = new HandlerThread("launcher-badge-loader");

    static {
        sWorkerThread.start();
    }

    static final android.os.Handler sWorker = new android.os.Handler(sWorkerThread.getLooper());

    private static WeakReference<LauncherBadgeProvider> sLauncherBadgeProvider;

    public static void setLauncherBadgeProvider(LauncherBadgeProvider launcherBadgeProvider) {
        if (sLauncherBadgeProvider != null) {
            Log.w(TAG, "BadgeController setLauncherBadgeProvider called twice! old=" +
                    sLauncherBadgeProvider.get() + " new=" + launcherBadgeProvider);
        }
        sLauncherBadgeProvider = new WeakReference<>(launcherBadgeProvider);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "BadgeController onReceive action=" + action);
        if (Intent.ACTION_PACKAGE_DATA_CLEARED.equals(action) || Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
            String packageName = intent.getData().getSchemeSpecificPart();
            if (packageName != null) {
                clearBadge(new String[]{packageName});
            }
        } else if ("com.mediatek.intent.action.SETTINGS_PACKAGE_DATA_CLEARED".equals(action)) {
            String packageName = intent.getStringExtra("packageName");
            if (packageName != null) {
                clearBadge(new String[]{packageName});
            }
        }
    }

    public void initialize(Launcher launcher) {
        Log.d(TAG, "BadgeController initialize launcher=" + launcher);
        sLauncherBadgeProvider.get().initialize(launcher);
    }

    public void setBadge(final String packageName, final int count) {
        sWorker.post(new Runnable() {
            @Override
            public void run() {
                sLauncherBadgeProvider.get().setBadge(packageName, count);
            }
        });
    }

    public void clearBadge(final String[] packageName) {
        sWorker.post(new Runnable() {
            @Override
            public void run() {
                sLauncherBadgeProvider.get().removeBadge(packageName);
            }
        });
    }

    public void clearAllBadges() {
        sWorker.post(new Runnable() {
            @Override
            public void run() {
                sLauncherBadgeProvider.get().clearBadges();
            }
        });
    }

    public void reloadBadges() {
        sWorker.post(new Runnable() {
            @Override
            public void run() {
                sLauncherBadgeProvider.get().reloadBadges();
            }
        });
    }

    public ArrayList<BadgeInfo> getBadges() {
        return sLauncherBadgeProvider.get().getBadges();
    }

    public static void drawUnreadEventIfNeed(Canvas canvas, View icon, boolean isShowingUnread) {
        if (!FeatureFlags.UNREAD_ENABLE) return;
        ItemInfo info = (ItemInfo) icon.getTag();
        if (info != null && info.unreadNum > 0) {
            Resources res = icon.getContext().getResources();

            /// M: Meature sufficent width for unread text and background image
            Paint unreadTextNumberPaint = new Paint();
            unreadTextNumberPaint.setTextSize(res.getDimension(R.dimen.unread_text_number_size));
            unreadTextNumberPaint.setTypeface(Typeface.DEFAULT_BOLD);
            unreadTextNumberPaint.setColor(0xffffffff);
            unreadTextNumberPaint.setTextAlign(Paint.Align.CENTER);
            unreadTextNumberPaint.setAntiAlias(true);
            Paint unreadTextPlusPaint = new Paint(unreadTextNumberPaint);
            unreadTextPlusPaint.setTextSize(res.getDimension(R.dimen.unread_text_plus_size));

            Paint unreadBgPaint = new Paint(unreadTextNumberPaint);
            unreadBgPaint.setColor(0xfff03232);
            String unreadTextNumber;
            String unreadTextPlus = "+";
            Rect unreadTextNumberBounds = new Rect(0, 0, 0, 0);
            Rect unreadTextPlusBounds = new Rect(0, 0, 0, 0);
            if (info.unreadNum > MAX_UNREAD_COUNT) {
                unreadTextNumber = String.valueOf(MAX_UNREAD_COUNT);
                unreadTextPlusPaint.getTextBounds(unreadTextPlus, 0,
                        unreadTextPlus.length(), unreadTextPlusBounds);
            } else {
                unreadTextNumber = String.valueOf(info.unreadNum);
            }
            unreadTextNumberPaint.getTextBounds(unreadTextNumber, 0,
                    unreadTextNumber.length(), unreadTextNumberBounds);
            int textHeight = unreadTextNumberBounds.height();
            int textWidth = unreadTextNumberBounds.width() + unreadTextPlusBounds.width();

            /// M: Draw unread background image.
            NinePatchDrawable unreadBgNinePatchDrawable =
                    (NinePatchDrawable) res.getDrawable(R.drawable.ic_newevents_numberindication);
            int unreadBgWidth = unreadBgNinePatchDrawable.getIntrinsicWidth();
            int unreadBgHeight = unreadBgNinePatchDrawable.getIntrinsicHeight();

            int unreadMinWidth = (int) res.getDimension(R.dimen.unread_minWidth);
            if (unreadBgWidth < unreadMinWidth) {
                unreadBgWidth = unreadMinWidth;
            }
            int unreadTextMargin = (int) res.getDimension(R.dimen.unread_text_margin);
            if (unreadBgWidth < textWidth + unreadTextMargin) {
                unreadBgWidth = textWidth + unreadTextMargin;
            }
            if (unreadBgHeight < textHeight) {
                unreadBgHeight = textHeight;
            }
            Rect unreadBgBounds = new Rect(0, 0, unreadBgWidth, unreadBgHeight);
            unreadBgNinePatchDrawable.setBounds(unreadBgBounds);

            int unreadMarginTop = 0;
            int unreadMarginRight = 0;
            int unreadBgPosX = 0;
            int unreadBgPosY = 0;
            float scale = 95 / 100f;
            if (info instanceof ShortcutInfo) {
                float multiple = 4.5f;
                if (info.container == (long) LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                    unreadMarginTop = (int) res.getDimension(R.dimen.hotseat_unread_margin_top);
                    unreadMarginRight = (int) res.getDimension(R.dimen.hotseat_unread_margin_right);
                    multiple = 1.5f;
                } else if (info.container == (long) LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    unreadMarginTop = (int) res.getDimension(R.dimen.workspace_unread_margin_top);
                    unreadMarginRight = (int) res.getDimension(
                            R.dimen.workspace_unread_margin_right);
                } else {
                    unreadMarginTop = (int) res.getDimension(R.dimen.folder_unread_margin_top);
                    unreadMarginRight = (int) res.getDimension(R.dimen.folder_unread_margin_right);
                }
                BubbleTextView textView = (BubbleTextView) icon;
                int iconSize = textView.getIconSize();

                unreadBgPosX = icon.getScrollX() + iconSize + (icon.getWidth() - iconSize) / 2
                        - unreadBgWidth / 2 - 1;
                int offsetY = unreadBgHeight / 3 > icon.getPaddingTop() ? 2 : icon.getPaddingTop() - unreadBgHeight / 3;
                unreadBgPosY = (int) (icon.getScrollY() + offsetY/*( (icon.getHeight()-iconSize-textSize)/multiple*scale)*/);
            } else if (info instanceof FolderInfo) {
                if (info.container == (long) LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                    unreadMarginTop = (int) res.getDimension(R.dimen.hotseat_unread_margin_top);
                    unreadMarginRight = (int) res.getDimension(R.dimen.hotseat_unread_margin_right);
                } else if (info.container == (long) LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    unreadMarginTop = (int) res.getDimension(R.dimen.workspace_unread_margin_top);
                    unreadMarginRight = (int) res.getDimension(
                            R.dimen.workspace_unread_margin_right);
                }
                FolderIcon folderIcon = (FolderIcon) icon;
                int iconSize = folderIcon.getIconSize();
                int folderBgSize = res.getDimensionPixelSize(R.dimen.folder_bg_size);
                if(iconSize>0){
                    folderBgSize = folderBgSize > iconSize ? iconSize : folderBgSize;
                }
                int offsetY = unreadBgHeight / 3 > icon.getPaddingTop() ? 2 : icon.getPaddingTop() - unreadBgHeight / 3;
                unreadBgPosX = icon.getScrollX() + folderBgSize + (icon.getWidth() - folderBgSize) / 2
                        - unreadBgWidth / 2 - 1;
                unreadBgPosY = (int) (icon.getScrollY() + offsetY/*( (icon.getHeight()-iconSize-textSize)/multiple*scale)*/);
            } else if (info instanceof AppInfo) {
                unreadMarginTop = (int) res.getDimension(R.dimen.app_list_unread_margin_top);
                unreadMarginRight = (int) res.getDimension(R.dimen.app_list_unread_margin_right);
                unreadBgPosX = icon.getScrollX() + icon.getWidth()
                        - unreadBgWidth - unreadMarginRight;
                unreadBgPosY = icon.getScrollY() + unreadMarginTop;
            }

            canvas.save();
            canvas.translate(unreadBgPosX, unreadBgPosY);

            if (unreadBgNinePatchDrawable != null) {
                //unreadBgNinePatchDrawable.draw(canvas);
                if (unreadTextNumber != null && unreadTextNumber.length() > 1) {
                    RectF rectF = new RectF(unreadBgBounds);
                    canvas.drawRoundRect(rectF, unreadBgHeight / 2 + 1, unreadBgHeight / 2 + 1, unreadBgPaint);
                } else {
                    canvas.drawCircle(unreadBgWidth / 2, unreadBgHeight / 2, unreadBgWidth / 2, unreadBgPaint);
                }
            } else {
                Log.d(TAG, "drawUnreadEventIfNeed: "
                        + "unreadBgNinePatchDrawable is null pointer");
                return;
            }

            Log.d("lijun", "drawUnread : " + unreadBgHeight + ", " + info.getTargetComponent());
            /// M: Draw unread text.
            Paint.FontMetrics fontMetrics = unreadTextNumberPaint.getFontMetrics();
            if (info.unreadNum > MAX_UNREAD_COUNT) {
                canvas.drawText(unreadTextNumber,
                        (unreadBgWidth - unreadTextPlusBounds.width()) / 2,
                        (unreadBgHeight + textHeight) / 2,
                        unreadTextNumberPaint);
                canvas.drawText(unreadTextPlus,
                        (unreadBgWidth + unreadTextNumberBounds.width()) / 2,
                        (unreadBgHeight + textHeight) / 2 + fontMetrics.ascent / 2,
                        unreadTextPlusPaint);
            } else {
                canvas.drawText(unreadTextNumber,
                        unreadBgWidth / 2,
                        (unreadBgHeight + textHeight) / 2,
                        unreadTextNumberPaint);
            }

            canvas.restore();
            isShowingUnread = true;
        }
    }

    private static int getIconDragingOffsetY(Launcher launcher, ItemInfo info) {
        DeviceProfile profile = launcher.getDeviceProfile();
        if (info != null) {
            if (info.container == (long) LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                return profile.folderIconPreviewPadding + 16;
            } else if (info.container == (long) LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                return profile.folderIconPreviewPadding + 16;
            } else {
                return profile.folderIconPreviewPadding + 29;
            }
        } else {
            return profile.folderIconPreviewPadding + 16;
        }

    }

    public static void checkAndSetShortCutInfo(final ShortcutInfo info,final ArrayList<BadgeInfo> unreadApps) {
        final Intent intent = info.intent;
        final ComponentName componentName = intent.getComponent();
        BadgeInfo badgeInfo1 = null;
        for (BadgeInfo badgeInfo : unreadApps) {
            if (componentName != null && badgeInfo.pkgName != null && badgeInfo.pkgName.equals(componentName.getPackageName())
                    && badgeInfo.shortcutCustomId != null && badgeInfo.shortcutCustomId.equals(info.shortcutCustomId)) {
                badgeInfo1 = badgeInfo;
            }
        }
        if (badgeInfo1 != null) {
            info.unreadNum = badgeInfo1.badgeCount;
        } else {
            info.unreadNum = 0;
        }
    }
}
