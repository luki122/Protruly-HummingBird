package com.mediatek.settings.ext;

import hb.preference.PreferenceScreen;

public interface IAppsExt {

    /**
     * @param prefScreen
     *            customized prefScreen
     * @internal
     */
    void launchApp(PreferenceScreen prefScreen, String packageName);
}
