/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import android.app.Dialog;
import android.content.Context;

import hb.app.dialog.AlertDialog;
import hb.preference.EditTextPreference;

import android.content.DialogInterface;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * TODO: Add a soft dialpad for PIN entry.
 */
class EditPinPreference extends EditTextPreference implements TextWatcher{
    private static int MIN_LENGTH = 4;
    private static int MAX_LENGTH = 8;
    private EditText mPasswordEntry;

    interface OnPinEnteredListener {
        void onPinEntered(EditPinPreference preference, boolean positiveResult);
    }
    
    private OnPinEnteredListener mPinListener;
    
    public EditPinPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditPinPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public void setOnPinEnteredListener(OnPinEnteredListener listener) {
        mPinListener = listener;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        final EditText editText = getEditText();

        if (editText != null) {
            editText.setInputType(InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            editText.addTextChangedListener(this);
            mPasswordEntry = editText;
        }
    }

    public boolean isDialogOpen() {
        Dialog dialog = getDialog();
        return dialog != null && dialog.isShowing();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (mPinListener != null) {
            mPinListener.onPinEntered(this, positiveResult);
        }
    }

    public void showPinDialog() {
        Dialog dialog = getDialog();
        if (dialog == null || !dialog.isShowing()) {
            showDialog(null);
            if (mPasswordEntry.length() < MIN_LENGTH) {
                setSummitBtnEnable(false);
            }
            Selection.setSelection(mPasswordEntry.getText(), mPasswordEntry.length());
        }
    }

    public void afterTextChanged(Editable s) {
        setSummitBtnEnable(mPasswordEntry.length() >= MIN_LENGTH);
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if(mPasswordEntry.length() > MAX_LENGTH) {
            Editable editable = (Editable) mPasswordEntry.getText();
            String str = editable.toString();
            String newStr = str.substring(0, MAX_LENGTH);
            mPasswordEntry.setText(newStr);
            editable = (Editable) mPasswordEntry.getText();
            Selection.setSelection(editable, MAX_LENGTH);
        }
    }

    private void setSummitBtnEnable(boolean enable) {
        AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null) {
            Button summitBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            summitBtn.setEnabled(enable);
        }
    }
}
