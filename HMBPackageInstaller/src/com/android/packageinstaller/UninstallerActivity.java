/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/
package com.android.packageinstaller;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.app.admin.IDevicePolicyManager;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.packageinstaller.Activity.BaseActivity;

import java.util.List;

/*
 * This activity presents UI to uninstall an application. Usually launched with intent
 * Intent.ACTION_UNINSTALL_PKG_COMMAND and attribute 
 * com.android.packageinstaller.PackageName set to the application package name
 */
public class UninstallerActivity extends BaseActivity {
    private static final String TAG = "PackageInstaller";

    private static final int UNINSTALL_COMPLETE = 1;
    private int mResultCode;
    private IBinder mCallback;
    private ApplicationInfo mAppInfo;
    private boolean mAllUsers;
    private UserHandle mUser;


    /**
     *
     * add zhaolaichao  20170523 start
     */
    private boolean isProfileOfOrSame(UserManager userManager, int userId, int profileId) {
        if (userId == profileId) {
            return true;
        }
        UserInfo parentUser = userManager.getProfileParent(profileId);
        return parentUser != null && parentUser.id == userId;
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UNINSTALL_COMPLETE:
                    mResultCode = msg.arg1;
                    final String packageName = (String) msg.obj;

                    if (mCallback != null) {
                        final IPackageDeleteObserver2 observer = IPackageDeleteObserver2.Stub
                                .asInterface(mCallback);
                        try {
                            observer.onPackageDeleted(mAppInfo.packageName, mResultCode,
                                    packageName);
                        } catch (RemoteException ignored) {
                        }
                        finish();
                        return;
                    }

                    if (getIntent().getBooleanExtra(Intent.EXTRA_RETURN_RESULT, false)) {
                        Intent result = new Intent();
                        result.putExtra(Intent.EXTRA_INSTALL_RESULT, mResultCode);
                        setResult(mResultCode == PackageManager.DELETE_SUCCEEDED
                                        ? Activity.RESULT_OK : Activity.RESULT_FIRST_USER,
                                result);
                        finish();
                        return;
                    }

                    // Update the status text
                    final String statusText;
                    Context ctx = getBaseContext();
                    switch (msg.arg1) {
                        case PackageManager.DELETE_SUCCEEDED:
                            statusText = getString(R.string.uninstall_done);
                            // Show a Toast and finish the activity
                            Toast.makeText(ctx, statusText, Toast.LENGTH_LONG).show();
                            //setResultAndFinish(mResultCode);
                            return;
                        case PackageManager.DELETE_FAILED_DEVICE_POLICY_MANAGER: {
                            UserManager userManager =
                                    (UserManager) getSystemService(Context.USER_SERVICE);
                            IDevicePolicyManager dpm = IDevicePolicyManager.Stub.asInterface(
                                    ServiceManager.getService(Context.DEVICE_POLICY_SERVICE));
                            // Find out if the package is an active admin for some non-current user.
                            int myUserId = UserHandle.myUserId();
                            UserInfo otherBlockingUser = null;
                            for (UserInfo user : userManager.getUsers()) {
                                // We only catch the case when the user in question is neither the
                                // current user nor its profile.
                                if (isProfileOfOrSame(userManager, myUserId, user.id)) continue;

                                try {
                                    if (dpm.packageHasActiveAdmins(packageName, user.id)) {
                                        otherBlockingUser = user;
                                        break;
                                    }
                                } catch (RemoteException e) {
                                    Log.e(TAG, "Failed to talk to package manager", e);
                                }
                            }
                            if (otherBlockingUser == null) {
                                Log.d(TAG, "Uninstall failed because " + packageName
                                        + " is a device admin");
                                //mDeviceManagerButton.setVisibility(View.VISIBLE);
                                statusText = getString(
                                        R.string.uninstall_failed_device_policy_manager);
                            } else {
                                Log.d(TAG, "Uninstall failed because " + packageName
                                        + " is a device admin of user " + otherBlockingUser);
                                //mDeviceManagerButton.setVisibility(View.GONE);
                                statusText = String.format(
                                        getString(R.string.uninstall_failed_device_policy_manager_of_user),
                                        otherBlockingUser.name);
                            }
                            break;
                        }
                        case PackageManager.DELETE_FAILED_OWNER_BLOCKED: {
                            UserManager userManager =
                                    (UserManager) getSystemService(Context.USER_SERVICE);
                            IPackageManager packageManager = IPackageManager.Stub.asInterface(
                                    ServiceManager.getService("package"));
                            List<UserInfo> users = userManager.getUsers();
                            int blockingUserId = UserHandle.USER_NULL;
                            for (int i = 0; i < users.size(); ++i) {
                                final UserInfo user = users.get(i);
                                try {
                                    if (packageManager.getBlockUninstallForUser(packageName,
                                            user.id)) {
                                        blockingUserId = user.id;
                                        break;
                                    }
                                } catch (RemoteException e) {
                                    // Shouldn't happen.
                                    Log.e(TAG, "Failed to talk to package manager", e);
                                }
                            }
                            int myUserId = UserHandle.myUserId();
                            if (blockingUserId == UserHandle.USER_OWNER) {
                                statusText = getString(R.string.uninstall_blocked_device_owner);
                            } else if (blockingUserId == UserHandle.USER_NULL) {
                                Log.d(TAG, "Uninstall failed for " + packageName + " with code "
                                        + msg.arg1 + " no blocking user");
                                statusText = getString(R.string.uninstall_failed);
                            } else {
                                statusText = getString(R.string.uninstall_blocked_profile_owner);
                            }
                            break;
                        }
                        default:
                            Log.d(TAG, "Uninstall failed for " + packageName + " with code "
                                    + msg.arg1);
                            statusText = getString(R.string.uninstall_failed);
                            break;
                    }
                    Toast.makeText(ctx, statusText, Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }
    };
    public static class UninstallAlertDialogFragment extends DialogFragment implements
            DialogInterface.OnClickListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final PackageManager pm = getActivity().getPackageManager();
            final DialogInfo dialogInfo = ((UninstallerActivity) getActivity()).mDialogInfo;
            final CharSequence appLabel = dialogInfo.appInfo.loadLabel(pm);

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
            StringBuilder messageBuilder = new StringBuilder();

            // If the Activity label differs from the App label, then make sure the user
            // knows the Activity belongs to the App being uninstalled.
            if (dialogInfo.activityInfo != null) {
                final CharSequence activityLabel = dialogInfo.activityInfo.loadLabel(pm);
                if (!activityLabel.equals(appLabel)) {
                    messageBuilder.append(
                            getString(R.string.uninstall_activity_text, activityLabel));
                    messageBuilder.append(" ").append(appLabel).append(".\n\n");
                }
            }

            final boolean isUpdate =
                    ((dialogInfo.appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
            if (isUpdate) {
                messageBuilder.append(getString(R.string.uninstall_update_text));
            } else {
                UserManager userManager = UserManager.get(getActivity());
                if (dialogInfo.allUsers && userManager.getUserCount() >= 2) {
                    messageBuilder.append(getString(R.string.uninstall_application_text_all_users));
                } else if (!dialogInfo.user.equals(android.os.Process.myUserHandle())) {
                    UserInfo userInfo = userManager.getUserInfo(dialogInfo.user.getIdentifier());
                    messageBuilder.append(
                            getString(R.string.uninstall_application_text_user, userInfo.name));
                } else {
                    messageBuilder.append(getString(R.string.uninstall_application_text));
                }
            }

            dialogBuilder.setTitle(appLabel);
            dialogBuilder.setIcon(dialogInfo.appInfo.loadIcon(pm));
            dialogBuilder.setPositiveButton(android.R.string.ok, this);
            dialogBuilder.setNegativeButton(android.R.string.cancel, this);
            dialogBuilder.setMessage(messageBuilder.toString());
            return dialogBuilder.create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == Dialog.BUTTON_POSITIVE) {
                //((UninstallerActivity) getActivity()).startUninstallProgress();
                ((UninstallerActivity) getActivity()).initUnInstaller();
            } else {
                ((UninstallerActivity) getActivity()).dispatchAborted();
            }
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);

            /// M: ALPS01790404. An NullPointer happens at the function "getActivity()" @{
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
            /// Google Original Code
            ///getActivity().finish();
            /// @}
        }
    }

    public static class AppNotFoundDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.app_not_found_dlg_title)
                    .setMessage(R.string.app_not_found_dlg_text)
                    .setNeutralButton(android.R.string.ok, null)
                    .create();
        }
        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            ((UninstallerActivity) getActivity()).dispatchAborted();
            getActivity().setResult(Activity.RESULT_FIRST_USER);
            getActivity().finish();
        }
    }

    static class DialogInfo {
        ApplicationInfo appInfo;
        ActivityInfo activityInfo;
        boolean allUsers;
        UserHandle user;
        IBinder callback;
    }

    private String mPackageName;
    private DialogInfo mDialogInfo;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // Get intent information.
        // We expect an intent with URI of the form package://<packageName>#<className>
        // className is optional; if specified, it is the activity the user chose to uninstall
        final Intent intent = getIntent();
        final Uri packageUri = intent.getData();
        if (packageUri == null) {
            Log.e(TAG, "No package URI in intent");
            showAppNotFound();
            return;
        }
        mPackageName = packageUri.getEncodedSchemeSpecificPart();
        if (mPackageName == null) {
            Log.e(TAG, "Invalid package name in URI: " + packageUri);
            showAppNotFound();
            return;
        }

        final IPackageManager pm = IPackageManager.Stub.asInterface(
                ServiceManager.getService("package"));

        mDialogInfo = new DialogInfo();

        mDialogInfo.user = intent.getParcelableExtra(Intent.EXTRA_USER);
        if (mDialogInfo.user == null) {
            mDialogInfo.user = android.os.Process.myUserHandle();
        }

        mDialogInfo.allUsers = intent.getBooleanExtra(Intent.EXTRA_UNINSTALL_ALL_USERS, false);
        mDialogInfo.callback = intent.getIBinderExtra(PackageInstaller.EXTRA_CALLBACK);

        try {
            mDialogInfo.appInfo = pm.getApplicationInfo(mPackageName,
                    PackageManager.GET_UNINSTALLED_PACKAGES, mDialogInfo.user.getIdentifier());
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get packageName. Package manager is dead?");
        }

        if (mDialogInfo.appInfo == null) {
            Log.e(TAG, "Invalid packageName: " + mPackageName);
            showAppNotFound();
            return;
        }

        // The class name may have been specified (e.g. when deleting an app from all apps)
        final String className = packageUri.getFragment();
        if (className != null) {
            try {
                mDialogInfo.activityInfo = pm.getActivityInfo(
                        new ComponentName(mPackageName, className), 0,
                        mDialogInfo.user.getIdentifier());
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to get className. Package manager is dead?");
                // Continue as the ActivityInfo isn't critical.
            }
        }

        showConfirmationDialog();
    }

    private void showConfirmationDialog() {
        showDialogFragment(new UninstallAlertDialogFragment());
    }

    private void showAppNotFound() {
        showDialogFragment(new AppNotFoundDialogFragment());
    }

    private void showDialogFragment(DialogFragment fragment) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        fragment.show(ft, "dialog");
    }

    void startUninstallProgress() {
        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        newIntent.putExtra(Intent.EXTRA_USER, mDialogInfo.user);
        newIntent.putExtra(Intent.EXTRA_UNINSTALL_ALL_USERS, mDialogInfo.allUsers);
        newIntent.putExtra(PackageInstaller.EXTRA_CALLBACK, mDialogInfo.callback);
        newIntent.putExtra(PackageUtil.INTENT_ATTR_APPLICATION_INFO, mDialogInfo.appInfo);
        if (getIntent().getBooleanExtra(Intent.EXTRA_RETURN_RESULT, false)) {
            newIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
            newIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        }
        newIntent.setClass(this, UninstallAppProgress.class);
        startActivity(newIntent);
    }

    void dispatchAborted() {
        if (mDialogInfo != null && mDialogInfo.callback != null) {
            final IPackageDeleteObserver2 observer = IPackageDeleteObserver2.Stub.asInterface(
                    mDialogInfo.callback);
            try {
                observer.onPackageDeleted(mPackageName,
                        PackageManager.DELETE_FAILED_ABORTED, "Cancelled by user");
            } catch (RemoteException ignored) {
            }
        }
    }

    /**
     *
     * add zhaolaichao  20170523 start
     */
    private void initUnInstaller() {
        mCallback = mDialogInfo.callback;
        mAppInfo = mDialogInfo.appInfo;
        mAllUsers = mDialogInfo.allUsers;
        mUser = mDialogInfo.user;
        IPackageManager packageManager =
                IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        PackageDeleteObserver observer = new PackageDeleteObserver();
        try {
            packageManager.deletePackageAsUser(mAppInfo.packageName, observer,
                    mUser.getIdentifier(),
                    mAllUsers ? PackageManager.DELETE_ALL_USERS : 0);
        } catch (RemoteException e) {
            // Shouldn't happen.
            Log.e(TAG, "Failed to talk to package manager", e);
        }
    }

    class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        public void packageDeleted(String packageName, int returnCode) {
            Message msg = mHandler.obtainMessage(UNINSTALL_COMPLETE);
            msg.arg1 = returnCode;
            msg.obj = packageName;
            mHandler.sendMessage(msg);
        }
    }
    /**
     *
     * add zhaolaichao  20170523 end
     */
}
