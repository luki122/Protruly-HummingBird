/*
* Copyright (C) 2015 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.packageinstaller.permission.ui;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import hb.app.HbActivity;

public final class HMBManageActivity extends HMBOverlayTouchActivity {

    private static final String LOG_TAG = "HMBManageActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            return;
        }

        Fragment fragment;
        String action = getIntent().getAction();

        switch (action) {
            case "android.intent.action.MANAGE_APP_PERMISSIONS":
            case "android.intent.action.HMB_MANAGE_APP_PERMISSIONS": {
                String packageName = getIntent().getStringExtra(Intent.EXTRA_PACKAGE_NAME);
                if (packageName == null) {
                    Log.i(LOG_TAG, "Missing mandatory argument EXTRA_PACKAGE_NAME");
                    finish();
                    return;
                }
                fragment = HMBAppPermissionsFragment.newInstance(packageName);
            } break;

            case "android.intent.action.MANAGE_PERMISSION_APPS":
            case "android.intent.action.HMB_MANAGE_PERMISSION_APPS": {
                String permissionName = getIntent().getStringExtra(Intent.EXTRA_PERMISSION_NAME);
                if (permissionName == null) {
                    Log.i(LOG_TAG, "Missing mandatory argument EXTRA_PERMISSION_NAME");
                    finish();
                    return;
                }
                fragment = HMBPermissionAppsFragment.newInstance(permissionName);
            } break;

            case "android.intent.action.HMB_MANAGE_ALL_APP_PERMISSIONS": {
                String packageName = getIntent().getStringExtra(Intent.EXTRA_PACKAGE_NAME);
                if (packageName == null) {
                    Log.i(LOG_TAG, "Missing mandatory argument EXTRA_PACKAGE_NAME");
                    finish();
                    return;
                }
                fragment = HMBAllAppPermissionsFragment.newInstance(packageName);
            } break;

            default: {
                Log.w(LOG_TAG, "Unrecognized action " + action);
                finish();
                return;
            }
        }

        getFragmentManager().beginTransaction().replace(com.hb.R.id.content, fragment).commit();
    }
}
