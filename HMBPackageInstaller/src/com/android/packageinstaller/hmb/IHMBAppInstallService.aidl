package com.android.packageinstaller.hmb;

import android.content.pm.IPackageInstallObserver;

interface IHMBAppInstallService {

    int installApp(String packageName, String apkFilePath, IPackageInstallObserver observer);

}
