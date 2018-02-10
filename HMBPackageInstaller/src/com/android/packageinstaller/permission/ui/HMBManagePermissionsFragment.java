package com.android.packageinstaller.permission.ui;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.ArraySet;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.packageinstaller.R;
import com.android.packageinstaller.permission.model.PermissionApps;
import com.android.packageinstaller.permission.model.PermissionGroup;
import com.android.packageinstaller.permission.model.PermissionGroups;
import com.android.packageinstaller.permission.utils.Utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import hb.preference.Preference;
import hb.preference.PreferenceScreen;

/**
 * Created by xiaobin on 17-7-3.
 */

public class HMBManagePermissionsFragment extends HMBPermissionsFrameFragment implements
        PermissionGroups.PermissionsGroupsChangeCallback, Preference.OnPreferenceClickListener {

    private static final String LOG_TAG = "HMBManagePermissionsFragment";

    private static final String OS_PKG = "android";

    private static final String EXTRA_PREFS_KEY = "extra_prefs_key";

    private ArraySet<String> mLauncherPkgs;

    private PermissionGroups mPermissions;

    private PreferenceScreen mExtraScreen;

    public static HMBManagePermissionsFragment newInstance() {
        return new HMBManagePermissionsFragment();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setLoading(true /* loading */, false /* animate */);
        setHasOptionsMenu(true);
        final ActionBar ab = getActivity().getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        mLauncherPkgs = Utils.getLauncherPackages(getContext());
        mPermissions = new PermissionGroups(getActivity(), getLoaderManager(), this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mPermissions.refresh();
        updatePermissionsUi();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        PermissionGroup group = mPermissions.getGroup(key);
        if (group == null) {
            return false;
        }

        Intent intent = new Intent("android.intent.action.HMB_MANAGE_PERMISSION_APPS")
                .putExtra(Intent.EXTRA_PERMISSION_NAME, key);
        try {
            getActivity().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(LOG_TAG, "No app to handle " + intent);
        }

        return true;
    }

    @Override
    public void onPermissionGroupsChanged() {
        updatePermissionsUi();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindPermissionUi(getActivity(), getView());
    }

    private static void bindPermissionUi(Context context, View rootView) {
        if (context == null || rootView == null) {
            return;
        }

        ImageView iconView = (ImageView) rootView.findViewById(R.id.lb_icon);
        if (iconView != null) {
            // Set the icon as the background instead of the image because ImageView
            // doesn't properly scale vector drawables beyond their intrinsic size
            Drawable icon = context.getDrawable(R.drawable.ic_lock);
            icon.setTint(context.getColor(R.color.off_white));
            iconView.setBackground(icon);
        }
        TextView titleView = (TextView) rootView.findViewById(R.id.lb_title);
        if (titleView != null) {
            titleView.setText(R.string.app_permissions);
        }
        TextView breadcrumbView = (TextView) rootView.findViewById(R.id.lb_breadcrumb);
        if (breadcrumbView != null) {
            breadcrumbView.setText(R.string.app_permissions_breadcrumb);
        }
    }

    private class PermissionGroupComparable implements Comparator<PermissionGroup> {

        @Override
        public int compare(PermissionGroup lhs, PermissionGroup rhs) {
            boolean lisSystemPermission = lhs.getDeclaringPackage().equals(OS_PKG);
            boolean rlsSystemPermission = rhs.getDeclaringPackage().equals(OS_PKG);
            if (lisSystemPermission != rlsSystemPermission) {
                if (lisSystemPermission) {
                    return -1;
                }
                return 1;
            }
            return 0;
        }

    }

    private void updatePermissionsUi() {
        Context context = getActivity().getApplicationContext();
        if (context == null) {
            return;
        }

        List<PermissionGroup> groups = mPermissions.getGroups();
        Collections.sort(groups, new PermissionGroupComparable());

        PreferenceScreen screen = getPreferenceScreen();

        // Use this to speed up getting the info for all of the PermissionApps below.
        // Create a new one for each refresh to make sure it has fresh data.
        PermissionApps.PmCache cache = new PermissionApps.PmCache(getContext().getPackageManager());
        for (PermissionGroup group : groups) {
            boolean isSystemPermission = group.getDeclaringPackage().equals(OS_PKG);

            Preference preference = findPreference(group.getName());
            if (preference == null && mExtraScreen != null) {
                preference = mExtraScreen.findPreference(group.getName());
            }
            if (preference == null) {
                preference = new Preference(getActivity());
                preference.setLayoutResource(com.hb.R.layout.preference_material_hb);
                preference.setOnPreferenceClickListener(this);
                preference.setKey(group.getName());
                preference.setIcon(Utils.getPermissionGroupsIcon(context, group.getName(), group.getIcon()));
                preference.setTitle(group.getLabel());
                // Set blank summary so that no resizing/jumping happens when the summary is loaded.
                preference.setSummary(" ");
                preference.setPersistent(false);
                if (isSystemPermission) {
                    screen.addPreference(preference);
                } else {
//                    if (mExtraScreen == null) {
//                        mExtraScreen = getPreferenceManager().createPreferenceScreen(context);
//                    }
//                    mExtraScreen.addPreference(preference);
                }
            }
            final Preference finalPref = preference;

            new PermissionApps(getContext(), group.getName(), new PermissionApps.Callback() {
                @Override
                public void onPermissionsLoaded(PermissionApps permissionApps) {
                    if (getActivity() == null) {
                        return;
                    }
                    int granted = permissionApps.getGrantedCount(mLauncherPkgs);
                    int total = permissionApps.getTotalCount(mLauncherPkgs);
                    finalPref.setSummary(getString(R.string.app_permissions_group_summary,
                            granted, total));
                }
            }, cache).refresh(false);
        }

        if (mExtraScreen != null && mExtraScreen.getPreferenceCount() > 0
                && screen.findPreference(EXTRA_PREFS_KEY) == null) {
            Preference extraScreenPreference = new Preference(getActivity());
            extraScreenPreference.setLayoutResource(com.hb.R.layout.preference_material_hb);
            extraScreenPreference.setKey(EXTRA_PREFS_KEY);
            extraScreenPreference.setIcon(Utils.applyTintByColor(context,
                    com.android.internal.R.drawable.ic_more_items,
                    R.color.color_range_title));
            extraScreenPreference.setTitle(R.string.additional_permissions);
            extraScreenPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    HMBManagePermissionsFragment.AdditionalPermissionsFragment frag =
                            new HMBManagePermissionsFragment.AdditionalPermissionsFragment();
                    frag.setTargetFragment(HMBManagePermissionsFragment.this, 0);
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.replace(android.R.id.content, frag);
                    ft.addToBackStack(null);
                    ft.commit();
                    return true;
                }
            });
            int count = mExtraScreen.getPreferenceCount();
            extraScreenPreference.setSummary(getResources().getQuantityString(
                    R.plurals.additional_permissions_more, count, count));
            screen.addPreference(extraScreenPreference);
        }
        if (screen.getPreferenceCount() != 0) {
            setLoading(false /* loading */, true /* animate */);
            showEmptyView(false);
        }
    }

    public static class AdditionalPermissionsFragment extends HMBPermissionsFrameFragment {
        @Override
        public void onCreate(Bundle icicle) {
            setLoading(true /* loading */, false /* animate */);
            super.onCreate(icicle);
            getActivity().setTitle(R.string.additional_permissions);
            setHasOptionsMenu(true);
        }

        @Override
        public void onDestroy() {
            getActivity().setTitle(R.string.app_permissions);
            super.onDestroy();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case android.R.id.home:
                    getFragmentManager().popBackStack();
                    return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            bindPermissionUi(getActivity(), getView());
        }

        @Override
        public void onCreatePreferences() {
            setPreferenceScreen(((HMBManagePermissionsFragment) getTargetFragment()).mExtraScreen);
            setLoading(false /* loading */, true /* animate */);
        }
    }






}
