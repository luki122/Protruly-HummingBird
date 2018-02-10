package com.android.launcher3;

/**
 * Created by liuzuo on 17-2-28.
 */

public interface UninstallMode {
void showUninstallApp();
void hideUninstallApp();
public interface UninstallAnimation{
    void startShakeAnimation();
    void stopShakeAnimation();
}
}
