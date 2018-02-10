package com.android.packageinstaller.permission.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.ArraySet;

import com.android.packageinstaller.R;
import com.android.packageinstaller.permission.model.AppPermissions;
import com.android.packageinstaller.permission.utils.Utils;
import com.hb.themeicon.theme.IconManager;

import java.util.ArrayList;
import java.util.List;

import hb.preference.Preference;
import hb.preference.PreferenceScreen;

/**
 * Created by xiaobin on 17-7-4.
 */

public class HMBManageAppsFragment extends HMBPermissionsFrameFragment implements Preference.OnPreferenceClickListener {

    private ArraySet<String> mLauncherPkgs;
    private List<App> appList;
    private AppsInfoLoader appsInfoLoader;

    public static HMBManageAppsFragment newInstance() {
        return new HMBManageAppsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLoading(true /* loading */, false /* animate */);

        mLauncherPkgs = Utils.getLauncherPackages(getContext());
        appsInfoLoader = new AppsInfoLoader();
        appList = new ArrayList<>();

        appsInfoLoader = new AppsInfoLoader();
        appsInfoLoader.execute();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAppsUi();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (appsInfoLoader != null) {
            appsInfoLoader.cancel(true);
            appsInfoLoader = null;
        }
    }

    private void updateAppsUi() {
        Context context = getActivity().getApplicationContext();
        if (context == null) {
            return;
        }

        PreferenceScreen screen = getPreferenceScreen();

        for (App app : appList) {
            Preference preference = findPreference(app.packageName);
            if (preference == null) {
                preference = new Preference(getActivity());
                preference.setLayoutResource(com.hb.R.layout.preference_material_hb);
                preference.setOnPreferenceClickListener(this);
                preference.setKey(app.packageName);
                preference.setIcon(app.icon);
                preference.setTitle(app.label);
                // Set blank summary so that no resizing/jumping happens when the summary is loaded.
                preference.setSummary(app.summary);
                preference.setPersistent(false);

                screen.addPreference(preference);
            }
        }

        setLoading(false /* loading */, true /* animate */);
        if (screen.getPreferenceCount() != 0) {
            showEmptyView(false);
        } else {
            showEmptyView(true);
            setEmptyText(getString(R.string.no_apps));
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        Intent intent = new Intent("android.intent.action.HMB_MANAGE_APP_PERMISSIONS")
                .putExtra(Intent.EXTRA_PACKAGE_NAME, key);
        try {
            getActivity().startActivity(intent);
        } catch (ActivityNotFoundException e) {

        }

        return true;
    }

    private class App {
        String packageName;
        String label;
        Drawable icon;
        String summary;
    }


    private class AppsInfoLoader extends AsyncTask<Void, Void, Void> {

        private Context mContext;
        private PackageManager mPm;

        public AppsInfoLoader() {
            if (getActivity() != null && !getActivity().isFinishing()) {
                mContext = getActivity().getApplicationContext();
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            appList.clear();
            if (mContext == null) {
                return null;
            }

            IconManager iconManager = IconManager.getInstance(mContext, true, false);

            App app;
            mPm = getActivity().getPackageManager();

            List<PackageInfo> ret = mPm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
            for (PackageInfo packageInfo : ret) {

                ApplicationInfo info = packageInfo.applicationInfo;
                if (info.isSystemApp() && (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                    continue;
                }

                AppPermissions mAppPermissions = new AppPermissions(getActivity(), packageInfo, null, true, null);
                mAppPermissions.refresh();

                app = new App();
                app.packageName = packageInfo.packageName;
                app.label = mAppPermissions.getAppLabel().toString();
                app.icon = iconManager.getIconDrawable(app.packageName, null);
                int count = mAppPermissions.getPermissionGroups().size();
                String summary = count > 1 ? count + mContext.getString(R.string.perm_count_unit_plural) :
                        count + getString(R.string.perm_count_unit_single);
                app.summary = summary;
                appList.add(app);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            updateAppsUi();
        }
    }

}
