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
 * limitations under the License
 */

package com.android.settings.fingerprint;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.settings.FullActivityBase;
import com.android.settings.LocalSettings;
import com.android.settings.R;


import hb.app.dialog.AlertDialog;

/**
 * Activity which concludes fingerprint enrollment.
 */
public class RenameFingerprint extends FullActivityBase implements View.OnClickListener, TextWatcher{
    private EditText mEditText;
    private Button mRenameBtn;
    private Dialog mDialog;
    private FingerprintManager mFingerprintManager;
    private Fingerprint mFp;
    private boolean mIsRemoving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.fingerprint_rename_activity);
        setTitle(getString(R.string.security_settings_fingerprint_preference_title));

        mFingerprintManager = (FingerprintManager) getSystemService(
                Context.FINGERPRINT_SERVICE);
        mFp = getIntent().getParcelableExtra("fingerprint");
        if (mFingerprintManager == null || mFp == null) {
            finish();
            return;
        }

        mRenameBtn = (Button) findViewById(R.id.fingerprint_rename_btn);
        mRenameBtn.setOnClickListener(this);

        mEditText = (EditText) findViewById(R.id.fingerprint_rename_field);
        mEditText.setText(mFp.getName());
        mEditText.setSelection(mEditText.length());
        mEditText.addTextChangedListener(this);

    }

    @Override
    public void onClick(View view) {
        comfirmDeleteDialog();
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        int maxLength = LocalSettings.EDITTEXT_GENERAL_MAX_LENGTH;
        int length = mEditText.length();
        if(length > maxLength) {
            Editable editable = (Editable) mEditText.getText();
            String str = editable.toString();
            String newStr = str.substring(0, maxLength);
            mEditText.setText(newStr);
            editable = (Editable) mEditText.getText();
            Selection.setSelection(editable, maxLength);
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {
        String name = mEditText.getText().toString();
        int length = name.trim().length();
        if (length < LocalSettings.EDITTEXT_GENERAL_MIN_LENGTH) {
            mRenameBtn.setEnabled(false);
        } else {
            mRenameBtn.setEnabled(true);
        }
    }

    private void comfirmDeleteDialog() {
        closeDialog();
        mDialog = new AlertDialog.Builder(RenameFingerprint.this)
                .setMessage(R.string.fingerprint_delete_message)
                .setPositiveButton(R.string.okay,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mIsRemoving = true;
                                deleteFingerPrint(mFp);
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
        mDialog.show();
    }

    private void closeDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeDialog();
    }

    @Override
    protected void onPause() {
        super.onPause();
        String name = getInputText();
        if (!TextUtils.isEmpty(name.trim()) && mFp !=null && !name.equals(mFp.getName())) {
            renameFingerPrint(name);
        }
    }

    private String getInputText() {
        return mEditText != null ? mEditText.getText().toString() : "";
    }

    private void renameFingerPrint(String newName) {
        mFingerprintManager.rename(mFp.getFingerId(), newName);
    }

    private void deleteFingerPrint(Fingerprint fingerPrint) {
        mFingerprintManager.remove(fingerPrint, mRemoveCallback);
    }

    private FingerprintManager.RemovalCallback mRemoveCallback = new FingerprintManager.RemovalCallback() {

        @Override
        public void onRemovalSucceeded(Fingerprint fingerprint) {
            finish();
        }

        @Override
        public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
            Toast.makeText(RenameFingerprint.this, errString, Toast.LENGTH_SHORT).show();
            finish();
        }
    };

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mIsRemoving) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mIsRemoving) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
