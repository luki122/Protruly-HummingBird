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

package com.android.settings.fingerprint;


import android.annotation.Nullable;
import android.app.Activity;
import hb.app.dialog.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.hardware.fingerprint.FingerprintManager.RemovalCallback;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;

import hb.preference.ListPreference;
import hb.preference.Preference;
import hb.preference.Preference.OnPreferenceChangeListener;
import hb.preference.PreferenceCategory;
import hb.preference.PreferenceGroup;
import hb.preference.PreferenceScreen;

import android.os.UserHandle;
import android.provider.Settings;
import android.security.Credentials;
import android.security.KeyStore;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ChooseLockGeneric;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.HelpUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SubSettings;

import java.util.ArrayList;
import java.util.List;

import static com.android.settings.fingerprint.FingerprintSettings.FingerprintSettingsFragment.REQUEST_TYPE_ADD_FINGERPRINT;
import static com.android.settings.fingerprint.FingerprintSettings.FingerprintSettingsFragment.REQUEST_TYPE_DISABLE_PASSWORD;
import static com.android.settings.fingerprint.FingerprintSettings.FingerprintSettingsFragment.REQUEST_TYPE_INIT;

/**
 * Settings screen for fingerprints
 */
public class FingerprintSettings extends SubSettings {
    /**
     * Used by the FP settings wizard to indicate the wizard is
     * finished, and each activity in the wizard should finish.
     * <p>
     * Previously, each activity in the wizard would finish itself after
     * starting the next activity. However, this leads to broken 'Back'
     * behavior. So, now an activity does not finish itself until it gets this
     * result.
     */
    static final int RESULT_FINISHED = RESULT_FIRST_USER;
    private static final long LOCKOUT_DURATION = 30000; // time we have to wait for fp to reset, ms

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, FingerprintSettingsFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (FingerprintSettingsFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.security_settings_fingerprint_preference_title);
        setTitle(msg);
    }

    public static class FingerprintSettingsFragment extends SettingsPreferenceFragment
            implements OnPreferenceChangeListener {
        private static final int MAX_RETRY_ATTEMPTS = 20;
        private static final int RESET_HIGHLIGHT_DELAY_MS = 500;

        private static final String TAG = "FingerprintSettings";
        private static final String KEY_FINGERPRINT_ITEM_PREFIX = "key_fingerprint_item";
        private static final String KEY_FINGERPRINT_ADD = "key_fingerprint_add";
        private static final String KEY_FINGERPRINT_ENABLE_KEYGUARD_TOGGLE =
                "fingerprint_enable_keyguard_toggle";
        private static final String KEY_LAUNCHED_CONFIRM = "launched_confirm";

        private static final int MSG_REFRESH_FINGERPRINT_TEMPLATES = 1000;
        private static final int MSG_FINGER_AUTH_SUCCESS = 1001;
        private static final int MSG_FINGER_AUTH_FAIL = 1002;
        private static final int MSG_FINGER_AUTH_ERROR = 1003;
        private static final int MSG_FINGER_AUTH_HELP = 1004;

        private static final int CONFIRM_REQUEST = 101;
        private static final int CHOOSE_LOCK_GENERIC_REQUEST = 102;

        private static final int ADD_FINGERPRINT_REQUEST = 10;

        protected static final boolean DEBUG = true;

        private FingerprintManager mFingerprintManager;
        private CancellationSignal mFingerprintCancel;
        private boolean mInFingerprintLockout;
        private byte[] mToken;
        private boolean mLaunchedConfirm;
        private Drawable mHighlightDrawable;

        private static final String KEY_LOCK_AFTER_TIMEOUT = "lock_after_timeout";
        private static final String KEY_FINGERPRINT_CATEGORY = "key_fingerprint_category";
        private static final String KEY_PASSWORD_SWITCH_TOGGLE = "key_password_switch_toggle";
        private static final String KEY_CHANGE_PASSWORD = "key_change_password";
        private static final String KEY_SENIOR_CONFIG_CATEGORY = "key_senior_config_category";
        private static final boolean IS_SHOW_FOOTER_TIP = false;
        private static final boolean IS_SHOW_ORIGINAL = false;
        private PreferenceCategory mFingerprintCategory;
        private LockPatternUtils mLockPatternUtils;
        private static final int MY_USER_ID = UserHandle.myUserId();
        private PreferenceScreen mPasswordTogglePreference;
        private PreferenceScreen mChangePasswordPreference;
        private PreferenceCategory mSeniorConfigCategory;
        private ListPreference mLockAfter;
        private DevicePolicyManager mDPM;
        public static final int REQUEST_TYPE_INIT = -1;
        public static final int REQUEST_TYPE_ENTRY = 0;
        public static final int REQUEST_TYPE_ENABLE_PASSWORD = 1;
        public static final int REQUEST_TYPE_DISABLE_PASSWORD = 2;
        public static final int REQUEST_TYPE_CHANGE_PASSWORD = 3;
        public static final int REQUEST_TYPE_ADD_FINGERPRINT = 4;
        private int mRequestType = REQUEST_TYPE_INIT;
        private static final String KEY_REQUEST_TYPE = "request_type";

        private boolean mFingerprintDetected = false;

        private AuthenticationCallback mAuthCallback = new AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(AuthenticationResult result) {
                int fingerId = result.getFingerprint().getFingerId();
                mHandler.obtainMessage(MSG_FINGER_AUTH_SUCCESS, fingerId, 0).sendToTarget();
            }

            @Override
            public void onAuthenticationFailed() {
                mHandler.obtainMessage(MSG_FINGER_AUTH_FAIL).sendToTarget();
            };

            @Override
            public void onAuthenticationError(int errMsgId, CharSequence errString) {
                mHandler.obtainMessage(MSG_FINGER_AUTH_ERROR, errMsgId, 0, errString)
                        .sendToTarget();
            }

            @Override
            public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                mHandler.obtainMessage(MSG_FINGER_AUTH_HELP, helpMsgId, 0, helpString)
                        .sendToTarget();
            }
        };
        private RemovalCallback mRemoveCallback = new RemovalCallback() {

            @Override
            public void onRemovalSucceeded(Fingerprint fingerprint) {
                mHandler.obtainMessage(MSG_REFRESH_FINGERPRINT_TEMPLATES,
                        fingerprint.getFingerId(), 0).sendToTarget();
            }

            @Override
            public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
                final Activity activity = getActivity();
                if (activity != null) {
                    Toast.makeText(activity, errString, Toast.LENGTH_SHORT);
                }
            }
        };
        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case MSG_REFRESH_FINGERPRINT_TEMPLATES:
                        removeFingerprintPreference(msg.arg1);
                        updateAddPreference();
                        // shangrui added for byd_fps to authentication again
                        retryFingerprint();
                        break;
                    case MSG_FINGER_AUTH_SUCCESS:
                        mFingerprintCancel = null;
                        highlightFingerprintItem(msg.arg1);
                        // shangrui added for byd_fps to avoid fingerprintservice dispatchAuthenticated remove the new client
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                retryFingerprint();
                            }
                        }, RESET_HIGHLIGHT_DELAY_MS);
                        break;
                    case MSG_FINGER_AUTH_FAIL:
                        // No action required... fingerprint will allow up to 5 of these
                        break;
                    case MSG_FINGER_AUTH_ERROR:
                        handleError(msg.arg1 /* errMsgId */, (CharSequence) msg.obj /* errStr */);
                        break;
                    case MSG_FINGER_AUTH_HELP: {
                        // Not used
                    }
                    break;
                }
            };
        };

        private void stopFingerprint() {
            if (!mFingerprintDetected) {
                return;
            }
            if (mFingerprintCancel != null && !mFingerprintCancel.isCanceled()) {
                mFingerprintCancel.cancel();
            }
            mFingerprintCancel = null;

            if (mHandler.hasCallbacks(mFingerprintLockoutReset)) {
                mHandler.removeCallbacks(mFingerprintLockoutReset);
            }
            mInFingerprintLockout = false;
        }

        /**
         * @param errMsgId
         */
        protected void handleError(int errMsgId, CharSequence msg) {
            mFingerprintCancel = null;
            switch (errMsgId) {
                case FingerprintManager.FINGERPRINT_ERROR_CANCELED:
                    return; // Only happens if we get preempted by another activity. Ignored.
                case FingerprintManager.FINGERPRINT_ERROR_LOCKOUT:
                    mInFingerprintLockout = true;
                    // We've been locked out.  Reset after 30s.
                    if (!mHandler.hasCallbacks(mFingerprintLockoutReset)) {
                        mHandler.postDelayed(mFingerprintLockoutReset,
                                LOCKOUT_DURATION);
                    }
                    // Fall through to show message
                default:
                    // Activity can be null on a screen rotation.
                    final Activity activity = getActivity();
                    if (activity != null) {
                        Toast.makeText(activity, msg, Toast.LENGTH_SHORT);
                    }
                    break;
            }
            retryFingerprint(); // start again
        }

        private void retryFingerprint() {
            if (!mInFingerprintLockout
                    && mFingerprintManager.getEnrolledFingerprints().size() > 0) {
                mFingerprintCancel = new CancellationSignal();
                mFingerprintManager.authenticate(null, mFingerprintCancel, 0 /* flags */,
                        mAuthCallback, null);
            }
        }

        @Override
        protected int getMetricsCategory() {
            return MetricsLogger.FINGERPRINT;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                mToken = savedInstanceState.getByteArray(
                        ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                mLaunchedConfirm = savedInstanceState.getBoolean(
                        KEY_LAUNCHED_CONFIRM, false);
                mRequestType = savedInstanceState.getInt(
                        KEY_REQUEST_TYPE, REQUEST_TYPE_INIT);
            }

            Activity activity = getActivity();
            mFingerprintManager = (FingerprintManager) activity.getSystemService(
                    Context.FINGERPRINT_SERVICE);

            initPreference();

            // Need to authenticate a session token if none
            if (mToken == null && mLaunchedConfirm == false
                    && hasScreenLock()) {
                mRequestType = REQUEST_TYPE_ENTRY;
                mLaunchedConfirm = true;
                launchChooseOrConfirmLock(false);
//                launchChooseOrConfirmLock();
            }
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            TextView v = (TextView) LayoutInflater.from(view.getContext()).inflate(
                    R.layout.fingerprint_settings_footer, null);
            v.setText(LearnMoreSpan.linkify(getText(isFingerprintDisabled()
                            ? R.string.security_settings_fingerprint_enroll_disclaimer_lockscreen_disabled
                            : R.string.security_settings_fingerprint_enroll_disclaimer),
                    getString(getHelpResource())));
            v.setMovementMethod(new LinkMovementMethod());
            if (IS_SHOW_FOOTER_TIP) {
                getListView().addFooterView(v);
                getListView().setFooterDividersEnabled(false);
            }
        }

        private boolean isFingerprintDisabled() {
            final DevicePolicyManager dpm =
                    (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            return dpm != null && (dpm.getKeyguardDisabledFeatures(null)
                    & DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT) != 0;
        }

        protected void removeFingerprintPreference(int fingerprintId) {
            String name = genKey(fingerprintId);
            Preference prefToRemove = mFingerprintCategory.findPreference(name);
            if (prefToRemove != null) {
                if (!mFingerprintCategory.removePreference(prefToRemove)) {
                    Log.w(TAG, "Failed to remove preference with key " + name);
                }
            } else {
                Log.w(TAG, "Can't find preference to remove: " + name);
            }
        }

        /**
         * Important!
         * <p>
         * Don't forget to update the SecuritySearchIndexProvider if you are doing any change in the
         * logic or adding/removing preferences here.
         */
        private Preference createPreferenceHierarchy() {
//            PreferenceScreen root = getPreferenceScreen();
//            if (root != null) {
//                root.removeAll();
//            }
//            addPreferencesFromResource(R.xml.security_settings_fingerprint);
//            root = getPreferenceScreen();
            mFingerprintCategory.removeAll();
            addFingerprintItemPreferences(mFingerprintCategory);
            return mFingerprintCategory;
        }

        private void addFingerprintItemPreferences(PreferenceGroup root) {
            root.removeAll();

            Preference addPreference = new Preference(root.getContext());
            addPreference.setKey(KEY_FINGERPRINT_ADD);
            addPreference.setTitle(R.string.fingerprint_add_title);
            addPreference.setIcon(R.drawable.ic_add_24dp);
            root.addPreference(addPreference);
            addPreference.setOnPreferenceChangeListener(this);

            final List<Fingerprint> items = mFingerprintManager.getEnrolledFingerprints();
            final int fingerprintCount = items.size();
            for (int i = 0; i < fingerprintCount; i++) {
                final Fingerprint item = items.get(i);
                FingerprintPreference pref = new FingerprintPreference(root.getContext());
                pref.setKey(genKey(item.getFingerId()));
                pref.setTitle(item.getName());
                pref.setFingerprint(item);
                pref.setPersistent(false);
                root.addPreference(pref);
                pref.setOnPreferenceChangeListener(this);
            }
            updateAddPreference();
        }

        private void updateAddPreference() {
            /* Disable preference if too many fingerprints added */
            final int max = getContext().getResources().getInteger(
                    com.android.internal.R.integer.config_fingerprintMaxTemplatesPerUser);
            boolean tooMany = mFingerprintManager.getEnrolledFingerprints().size() >= max;
            CharSequence maxSummary = tooMany ?
                    getContext().getString(R.string.fingerprint_add_max, max) : "";
            Preference addPreference = findPreference(KEY_FINGERPRINT_ADD);
            addPreference.setSummary(maxSummary);
            addPreference.setEnabled(!tooMany);
        }

        private static String genKey(int id) {
            return KEY_FINGERPRINT_ITEM_PREFIX + "_" + id;
        }

        @Override
        public void onResume() {
            super.onResume();
            // Make sure we reload the preference hierarchy since fingerprints may be added,
            // deleted or renamed.
            updatePreferences();
        }

        private void updatePreferences() {
            if (mFingerprintDetected) {
                createPreferenceHierarchy();
                retryFingerprint();
            }

            updatePreferenceEx();
        }

        @Override
        public void onPause() {
            super.onPause();
            stopFingerprint();
        }

        @Override
        public void onSaveInstanceState(final Bundle outState) {
            outState.putByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN,
                    mToken);
            outState.putBoolean(KEY_LAUNCHED_CONFIRM, mLaunchedConfirm);
            outState.putInt(KEY_REQUEST_TYPE, mRequestType);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference pref) {
            final String key = pref.getKey();
            if (KEY_FINGERPRINT_ADD.equals(key)) {
//                startAddFingerprintActivity();
                mRequestType = REQUEST_TYPE_ADD_FINGERPRINT;
                launchChooseOrConfirmLock(!hasScreenLock());
            } else if (pref instanceof FingerprintPreference) {
                FingerprintPreference fpref = (FingerprintPreference) pref;
                final Fingerprint fp =fpref.getFingerprint();
//                showRenameDeleteDialog(fp);

                Intent intent = new Intent(getActivity(), RenameFingerprint.class);
                intent.putExtra("fingerprint", fp);
                startActivity(intent);
                return super.onPreferenceTreeClick(preferenceScreen, pref);
            } else if (KEY_PASSWORD_SWITCH_TOGGLE.equals(key)) {
                if (hasScreenLock()) {
                    handlePasswordChangeAction(getString(R.string.password_switch_disable_title),
                            new WarningToDeleteVpnProfilesDialog.OnComfirmToDeleteVpnProfileListener() {
                        @Override
                        public void onComfirmToDeleteVpnProfile() {
                            mRequestType = REQUEST_TYPE_DISABLE_PASSWORD;
                            launchChooseOrConfirmLock(false);
                        }
                    });
                } else {
                    mRequestType = REQUEST_TYPE_ENABLE_PASSWORD;
                    launchChooseLock(false);
                }
            } else if (KEY_CHANGE_PASSWORD.equals(key)) {
                if (hasScreenLock()) {
                    handlePasswordChangeAction(getString(R.string.change_password_title),
                            new WarningToDeleteVpnProfilesDialog.OnComfirmToDeleteVpnProfileListener() {
                                @Override
                                public void onComfirmToDeleteVpnProfile() {
                                    mRequestType = REQUEST_TYPE_CHANGE_PASSWORD;
//                                    launchChooseOrConfirmLock(false);
                                    launchChooseLock(false);
                                }
                            });
                }
            }
            return true;
        }

        private void showRenameDeleteDialog(final Fingerprint fp) {
            RenameDeleteDialog renameDeleteDialog = new RenameDeleteDialog();
            Bundle args = new Bundle();
            args.putParcelable("fingerprint", fp);
            renameDeleteDialog.setArguments(args);
            renameDeleteDialog.setTargetFragment(this, 0);
            renameDeleteDialog.show(getFragmentManager(), RenameDeleteDialog.class.getName());
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            boolean result = true;
            final String key = preference.getKey();
            if (KEY_FINGERPRINT_ENABLE_KEYGUARD_TOGGLE.equals(key)) {
                // TODO
            } else {
                Log.v(TAG, "Unknown key:" + key);
            }

            if (KEY_LOCK_AFTER_TIMEOUT.equals(key)) {
                int timeout = Integer.parseInt((String) value);
                try {
                    Settings.Secure.putInt(getContentResolver(),
                            Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, timeout);
                } catch (NumberFormatException e) {
                    Log.e("SecuritySettings", "could not persist lockAfter timeout setting", e);
                }
                updateLockAfterPreferenceSummary();
            }
            return result;
        }

        @Override
        protected int getHelpResource() {
            return R.string.help_url_fingerprint;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            byte[] token = null;
            if (requestCode == CHOOSE_LOCK_GENERIC_REQUEST
                    || requestCode == CONFIRM_REQUEST) {
                if (resultCode == RESULT_FINISHED || resultCode == RESULT_OK) {
                    // The lock pin/pattern/password was set. Start enrolling!
                    if (data != null) {
                        token = data.getByteArrayExtra(
                                ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                    }
                }
            }

            if (token != null) {
                mToken = token;
                if (mRequestType != REQUEST_TYPE_ENTRY) {
                    handleRequest();
                }
            }

            if (mToken == null && mRequestType == REQUEST_TYPE_ENTRY) {
                // Didn't get an authentication, finishing
                getActivity().finish();
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (getActivity().isFinishing()) {
                int result = mFingerprintManager.postEnroll();
                if (result < 0) {
                    Log.w(TAG, "postEnroll failed: result = " + result);
                }
            }
        }

        private Drawable getHighlightDrawable() {
            if (mHighlightDrawable == null) {
                final Activity activity = getActivity();
                if (activity != null) {
                    mHighlightDrawable = activity.getDrawable(R.drawable.preference_highlight);
                }
            }
            return mHighlightDrawable;
        }

        private void highlightFingerprintItem(int fpId) {
            String prefName = genKey(fpId);
            FingerprintPreference fpref = (FingerprintPreference) findPreference(prefName);
            final Drawable highlight = getHighlightDrawable();
            if (highlight != null && fpref != null && fpref.getView() != null) {
                try {
                    final View view = fpref.getView();
                    final int centerX = view.getWidth() / 2;
                    final int centerY = view.getHeight() / 2;
                    highlight.setHotspot(centerX, centerY);
                    view.setBackground(highlight);
                    view.setPressed(true);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            view.setPressed(false);
                            view.setBackground(null);
                        }
                    }, RESET_HIGHLIGHT_DELAY_MS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private boolean launchChooseOrConfirmLock() {
            return launchChooseOrConfirmLock(true);
        }

        private boolean launchChooseOrConfirmLock(boolean enableChooseLock) {
            boolean result = false;
            long challenge = mFingerprintManager.preEnroll();
            ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);
            if (!helper.launchConfirmationActivity(CONFIRM_REQUEST,
                    getString(R.string.security_settings_fingerprint_preference_title),
                    null, null, challenge)) {
                if (enableChooseLock) {
                    launchChooseLock(true);
                }
                result = true;
            }
            return result;
        }

        private void launchChooseLock(boolean hasChallenge) {
            long challenge = mFingerprintManager.preEnroll();
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", ChooseLockGeneric.class.getName());
            intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                    DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
            intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_DISABLED_PREFS,
                    true);
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, hasChallenge);
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, challenge);
            startActivityForResult(intent, CHOOSE_LOCK_GENERIC_REQUEST);
        }

        private void deleteFingerPrint(Fingerprint fingerPrint) {
            mFingerprintManager.remove(fingerPrint, mRemoveCallback);
        }

        private void renameFingerPrint(int fingerId, String newName) {
            mFingerprintManager.rename(fingerId, newName);
            updatePreferences();
        }

        private final Runnable mFingerprintLockoutReset = new Runnable() {
            @Override
            public void run() {
                mInFingerprintLockout = false;
                Activity activity = getActivity();
                if (activity != null
                        && !activity.isFinishing() && !activity.isDestroyed()) {
                    retryFingerprint();
                }
            }
        };

        public static class RenameDeleteDialog extends DialogFragment {

            private Fingerprint mFp;
            private EditText mDialogTextField;
            private String mFingerName;
            private Boolean mTextHadFocus;
            private int mTextSelectionStart;
            private int mTextSelectionEnd;

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                mFp = getArguments().getParcelable("fingerprint");
                if (savedInstanceState != null) {
                    mFingerName = savedInstanceState.getString("fingerName");
                    mTextHadFocus = savedInstanceState.getBoolean("textHadFocus");
                    mTextSelectionStart = savedInstanceState.getInt("startSelection");
                    mTextSelectionEnd = savedInstanceState.getInt("endSelection");
                }
                final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                        .setView(R.layout.fingerprint_rename_dialog)
                        .setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        final String newName =
                                                mDialogTextField.getText().toString();
                                        final CharSequence name = mFp.getName();
                                        if (!newName.equals(name)) {
                                            if (DEBUG) {
                                                Log.v(TAG, "rename " + name + " to " + newName);
                                            }
                                            FingerprintSettingsFragment parent
                                                    = (FingerprintSettingsFragment)
                                                    getTargetFragment();
                                            parent.renameFingerPrint(mFp.getFingerId(),
                                                    newName);
                                        }
                                        dialog.dismiss();
                                    }
                                })
                        .setNegativeButton(
                                R.string.security_settings_fingerprint_enroll_dialog_delete,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        onDeleteClick(dialog);
                                    }
                                })
                        .create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        mDialogTextField = (EditText) alertDialog.findViewById(
                                R.id.fingerprint_rename_field);
                        CharSequence name = mFingerName == null ? mFp.getName() : mFingerName;
                        mDialogTextField.setText(name);
                        if (mTextHadFocus == null) {
                            mDialogTextField.selectAll();
                        } else {
                            mDialogTextField.setSelection(mTextSelectionStart, mTextSelectionEnd);
                        }
                    }
                });
                if (mTextHadFocus == null || mTextHadFocus) {
                    // Request the IME
                    alertDialog.getWindow().setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
                alertDialog.setTitle(R.string.security_settings_fingerprint_enroll_dialog_name_label);
                return alertDialog;
            }

            private void onDeleteClick(DialogInterface dialog) {
                if (DEBUG) Log.v(TAG, "Removing fpId=" + mFp.getFingerId());
                FingerprintSettingsFragment parent
                        = (FingerprintSettingsFragment) getTargetFragment();
                if (parent.mFingerprintManager.getEnrolledFingerprints().size() > 1) {
                    parent.deleteFingerPrint(mFp);
                } else {
                    ConfirmLastDeleteDialog lastDeleteDialog = new ConfirmLastDeleteDialog();
                    Bundle args = new Bundle();
                    args.putParcelable("fingerprint", mFp);
                    lastDeleteDialog.setArguments(args);
                    lastDeleteDialog.setTargetFragment(getTargetFragment(), 0);
                    lastDeleteDialog.show(getFragmentManager(),
                            ConfirmLastDeleteDialog.class.getName());
                }
                dialog.dismiss();
            }

            @Override
            public void onSaveInstanceState(Bundle outState) {
                super.onSaveInstanceState(outState);
                if (mDialogTextField != null) {
                    outState.putString("fingerName", mDialogTextField.getText().toString());
                    outState.putBoolean("textHadFocus", mDialogTextField.hasFocus());
                    outState.putInt("startSelection", mDialogTextField.getSelectionStart());
                    outState.putInt("endSelection", mDialogTextField.getSelectionEnd());
                }
            }
        }

        public static class ConfirmLastDeleteDialog extends DialogFragment {

            private Fingerprint mFp;

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                mFp = getArguments().getParcelable("fingerprint");
                final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.fingerprint_last_delete_title)
                        .setMessage(R.string.fingerprint_last_delete_message)
                        .setPositiveButton(R.string.fingerprint_last_delete_confirm,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        FingerprintSettingsFragment parent
                                                = (FingerprintSettingsFragment) getTargetFragment();
                                        parent.deleteFingerPrint(mFp);
                                        dialog.dismiss();
                                    }
                                })
                        .setNegativeButton(
                                R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                        .create();
                return alertDialog;
            }
        }

        /**
         * Init preference
         *
         * @date Liuqin on 2017-04-01
         */
        private void initPreference() {
            addPreferencesFromResource(R.xml.security_settings_fingerprint);
            PreferenceScreen root = getPreferenceScreen();
            mLockPatternUtils = new LockPatternUtils(getActivity());
            mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);

            mFingerprintCategory = (PreferenceCategory) findPreference(KEY_FINGERPRINT_CATEGORY);
            mPasswordTogglePreference = (PreferenceScreen) findPreference(KEY_PASSWORD_SWITCH_TOGGLE);
            mChangePasswordPreference = (PreferenceScreen) findPreference(KEY_CHANGE_PASSWORD);
            mSeniorConfigCategory = (PreferenceCategory) findPreference(KEY_SENIOR_CONFIG_CATEGORY);

            // lock after preference
            mLockAfter = (ListPreference) root.findPreference(KEY_LOCK_AFTER_TIMEOUT);
            if (mLockAfter != null) {
                setupLockAfterPreference();
                updateLockAfterPreferenceSummary();
            }

            if (IS_SHOW_ORIGINAL) {
                root.removePreference(mPasswordTogglePreference);
                root.removePreference(mChangePasswordPreference);
                root.removePreference(mSeniorConfigCategory);
            }

            FingerprintManager fpm = (FingerprintManager) getActivity().getSystemService(
                    Context.FINGERPRINT_SERVICE);
            mFingerprintDetected = fpm.isHardwareDetected();
            if (!mFingerprintDetected) {
                Log.v(TAG, "No fingerprint hardware detected!!");
                root.removePreference(mFingerprintCategory);
            }
        }

        /**
         * Update preference ex
         *
         * @date Liuqin on 2017-04-01
         */
        private void updatePreferenceEx() {
            if (IS_SHOW_ORIGINAL) {
                return;
            }
            PreferenceScreen root = getPreferenceScreen();
            if (!mLockPatternUtils.isSecure(MY_USER_ID)) {
//                if (mLockPatternUtils.isLockScreenDisabled(MY_USER_ID)) {
                mPasswordTogglePreference.setTitle(R.string.password_switch_enable_title);
                mChangePasswordPreference.setEnabled(false);
                root.removePreference(mSeniorConfigCategory);
//                }
            } else {
                mPasswordTogglePreference.setTitle(R.string.password_switch_disable_title);
                mChangePasswordPreference.setEnabled(true);
                if (root.findPreference(KEY_SENIOR_CONFIG_CATEGORY) == null) {
                    root.addPreference(mSeniorConfigCategory);
                }
            }

            if (mLockAfter != null) {
                setupLockAfterPreference();
                updateLockAfterPreferenceSummary();
            }
        }

        private void setupLockAfterPreference() {
            // Compatible with pre-Froyo
            long currentTimeout = Settings.Secure.getLong(getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
            mLockAfter.setValue(String.valueOf(currentTimeout));
            mLockAfter.setOnPreferenceChangeListener(this);
            final long adminTimeout = (mDPM != null ? mDPM.getMaximumTimeToLock(null) : 0);
            final long displayTimeout = Math.max(0,
                    Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 0));
            if (adminTimeout > 0) {
                // This setting is a slave to display timeout when a device policy is enforced.
                // As such, maxLockTimeout = adminTimeout - displayTimeout.
                // If there isn't enough time, shows "immediately" setting.
                disableUnusableTimeouts(Math.max(0, adminTimeout - displayTimeout));
            }
        }

        private void disableUnusableTimeouts(long maxTimeout) {
            final CharSequence[] entries = mLockAfter.getEntries();
            final CharSequence[] values = mLockAfter.getEntryValues();
            ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
            ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
            for (int i = 0; i < values.length; i++) {
                long timeout = Long.valueOf(values[i].toString());
                if (timeout <= maxTimeout) {
                    revisedEntries.add(entries[i]);
                    revisedValues.add(values[i]);
                }
            }
            if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
                mLockAfter.setEntries(
                        revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
                mLockAfter.setEntryValues(
                        revisedValues.toArray(new CharSequence[revisedValues.size()]));
                final int userPreference = Integer.valueOf(mLockAfter.getValue());
                if (userPreference <= maxTimeout) {
                    mLockAfter.setValue(String.valueOf(userPreference));
                } else {
                    // There will be no highlighted selection since nothing in the list matches
                    // maxTimeout. The user can still select anything less than maxTimeout.
                    // TODO: maybe append maxTimeout to the list and mark selected.
                }
            }
            mLockAfter.setEnabled(revisedEntries.size() > 0);
        }

        private void updateLockAfterPreferenceSummary() {
            // Update summary message with current value
            long currentTimeout = Settings.Secure.getLong(getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
            final CharSequence[] entries = mLockAfter.getEntries();
            final CharSequence[] values = mLockAfter.getEntryValues();
            int best = 0;
            for (int i = 0; i < values.length; i++) {
                long timeout = Long.valueOf(values[i].toString());
                if (currentTimeout >= timeout) {
                    best = i;
                }
            }

//            Preference preference = getPreferenceScreen().findPreference(KEY_TRUST_AGENT);
//            if (preference != null && preference.getTitle().length() > 0) {
//                if (Long.valueOf(values[best].toString()) == 0) {
//                    mLockAfter.setSummary(getString(R.string.lock_immediately_summary_with_exception,
//                            preference.getTitle()));
//                } else {
//                    mLockAfter.setSummary(getString(R.string.lock_after_timeout_summary_with_exception,
//                            entries[best], preference.getTitle()));
//                }
//            } else {
            mLockAfter.setSummary(getString(R.string.lock_after_timeout_summary, entries[best]));
//            }
            /// M: Reset value for preference, because onResume will renew preference
            mLockAfter.setValue(String.valueOf(currentTimeout));
        }

        private boolean hasScreenLock() {
            boolean result = true;
            if (!mLockPatternUtils.isSecure(MY_USER_ID)) {
//                if (mLockPatternUtils.isLockScreenDisabled(MY_USER_ID)) {
                result = false;
//                }
            }

            return result;
        }

        private void handleRequest() {
            switch (mRequestType) {
                case REQUEST_TYPE_DISABLE_PASSWORD:
                    clearPassword();
                    break;
                case REQUEST_TYPE_ADD_FINGERPRINT:
                    startAddFingerprintActivity();
                    break;
            }
            mRequestType = REQUEST_TYPE_INIT;
        }

        private void startAddFingerprintActivity() {
            Intent intent = new Intent();
            intent.setClassName("com.android.settings",
                    FingerprintEnrollEnrolling.class.getName());
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
            startActivityForResult(intent, ADD_FINGERPRINT_REQUEST);
        }

        private void clearPassword() {
            mLockPatternUtils.clearLock(UserHandle.myUserId());
            mLockPatternUtils.setLockScreenDisabled(false, MY_USER_ID);
        }

        private void handlePasswordChangeAction(String opName,
                                                WarningToDeleteVpnProfilesDialog.OnComfirmToDeleteVpnProfileListener listener) {
            KeyStore keyStore = KeyStore.getInstance();
            String[] profileList = keyStore.list(Credentials.VPN);
            if (profileList != null && profileList.length > 0) {
                WarningToDeleteVpnProfilesDialog dialog = new WarningToDeleteVpnProfilesDialog();
                dialog.setOpName(opName);
                dialog.setListener(listener);
                dialog.show(getFragmentManager(), null);
            } else {
                if (listener != null) {
                    listener.onComfirmToDeleteVpnProfile();
                }
            }
        }
    }

    public static class FingerprintPreference extends Preference {
        private Fingerprint mFingerprint;
        private View mView;

        public FingerprintPreference(Context context, AttributeSet attrs, int defStyleAttr,
                int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }
        public FingerprintPreference(Context context, AttributeSet attrs, int defStyleAttr) {
            this(context, attrs, defStyleAttr, 0);
        }

        public FingerprintPreference(Context context, AttributeSet attrs) {
            this(context, attrs, com.android.internal.R.attr.preferenceStyle);
        }

        public FingerprintPreference(Context context) {
            this(context, null);
        }

        public View getView() { return mView; }

        public void setFingerprint(Fingerprint item) {
            mFingerprint = item;
        }

        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);
            mView = view;
        }
    };

    private static class LearnMoreSpan extends URLSpan {

        private static final Typeface TYPEFACE_MEDIUM =
                Typeface.create("sans-serif-medium", Typeface.NORMAL);

        private LearnMoreSpan(String url) {
            super(url);
        }

        @Override
        public void onClick(View widget) {
            Context ctx = widget.getContext();
            Intent intent = HelpUtils.getHelpIntent(ctx, getURL(), ctx.getClass().getName());
            try {
                ((Activity) ctx).startActivityForResult(intent, 0);
            } catch (ActivityNotFoundException e) {
                Log.w(FingerprintSettingsFragment.TAG,
                        "Actvity was not found for intent, " + intent.toString());
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
            ds.setTypeface(TYPEFACE_MEDIUM);
        }

        public static CharSequence linkify(CharSequence rawText, String uri) {
            SpannableString msg = new SpannableString(rawText);
            Annotation[] spans = msg.getSpans(0, msg.length(), Annotation.class);
            SpannableStringBuilder builder = new SpannableStringBuilder(msg);
            for (Annotation annotation : spans) {
                int start = msg.getSpanStart(annotation);
                int end = msg.getSpanEnd(annotation);
                LearnMoreSpan link = new LearnMoreSpan(uri);
                builder.setSpan(link, start, end, msg.getSpanFlags(link));
            }
            return builder;
        }
    }

    public static class WarningToDeleteVpnProfilesDialog extends DialogFragment {
        private OnComfirmToDeleteVpnProfileListener mListener;
        private String mOpName;

        public void setListener(OnComfirmToDeleteVpnProfileListener mListener) {
            this.mListener = mListener;
        }

        public void setOpName(String mOpName) {
            this.mOpName = mOpName;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(mOpName + getContext().getString(R.string.password_delete_vpn_profile_warning))
                    .setPositiveButton(R.string.okay,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    if (mListener != null) {
                                        mListener.onComfirmToDeleteVpnProfile();
                                    }
                                }
                            })
                    .setNegativeButton(R.string.cancel, null);

            Dialog dialog = builder.create();
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    Button summitBtn = ((AlertDialog)dialogInterface).getButton(DialogInterface.BUTTON_POSITIVE);
                    summitBtn.setBackgroundResource(R.drawable.hb_button_warn);
                }
            });
            return dialog;
        }

        public interface OnComfirmToDeleteVpnProfileListener {
            void onComfirmToDeleteVpnProfile();
        }
    }
}
