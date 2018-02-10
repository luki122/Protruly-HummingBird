/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.server.telecom;

import com.android.server.telecom.R;
import hb.app.dialog.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import hb.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Ultra-simple subclass of EditTextPreference that allows the "title" to wrap
 * onto multiple lines.
 *
 * (By default, the title of an EditTextPreference is singleLine="true"; see
 * preference_holo.xml under frameworks/base.  But in the "Respond via SMS"
 * settings UI we want titles to be multi-line, since the customized messages
 * might be fairly long, and should be able to wrap.)
 *
 * TODO: This is pretty cumbersome; it would be nicer for the framework to
 * either allow modifying the title's attributes in XML, or at least provide
 * some way from Java (given an EditTextPreference) to reach inside and get a
 * handle to the "title" TextView.
 *
 * TODO: Also, it would reduce clutter if this could be an inner class in
 * RespondViaSmsManager.java, but then there would be no way to reference the
 * class from XML.  That's because
 *    <com.android.server.telecom.MultiLineTitleEditTextPreference ... />
 * isn't valid XML syntax due to the "$" character.  And Preference
 * elements don't have a "class" attribute, so you can't do something like
 * <view class="com.android.server.telecom.Foo$Bar"> as you can with regular views.
 */
public class MultiLineTitleEditTextPreference extends EditTextPreference {
    public MultiLineTitleEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        //add by lgy
        getEditText().addTextChangedListener(prefWatcher);
    }

    public MultiLineTitleEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        //add by lgy
        getEditText().addTextChangedListener(prefWatcher);
    }

    public MultiLineTitleEditTextPreference(Context context) {
        super(context);
        //add by lgy
        getEditText().addTextChangedListener(prefWatcher);
    }

    // The "title" TextView inside an EditTextPreference defaults to
    // singleLine="true" (see preference_holo.xml under frameworks/base.)
    // We override onBindView() purely to look up that TextView and call
    // setSingleLine(false) on it.
    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        TextView textView = (TextView) view.findViewById(com.android.internal.R.id.title);
        if (textView != null) {
            textView.setSingleLine(false);
        }        

    }
    
    //zhangcj add in 20170426
    private String mOrigin;
    @Override
    protected void onBindDialogView(final View view) {
        super.onBindDialogView(view);
        mOrigin = getText();
    }
    //add by lgy
    private AlertDialog mSaveConfirmDialog;
    private TextWatcher prefWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {

        }

        @Override
        public void afterTextChanged(Editable s) {     
        	if(getDialog() != null) {
        	    Button button = (Button) getDialog().findViewById(android.R.id.button1);
                button.setEnabled(s.length() > 0);
        	}
        	if(s.length() >= 70) {
        	    Toast.makeText(
        	    		getContext(),
        	    		getContext().getResources().getString(R.string.max_chars),
                        Toast.LENGTH_LONG).show();
        	}
        }
    };
    
    protected void onDialogClosed(boolean positiveResult) {
    	final boolean positive = positiveResult;
    	String editText = getEditText().getText().toString();
    	boolean isTextChange = !mOrigin.equals(editText);
    	boolean isTextNull = TextUtils.isEmpty(editText);
    	if(isTextNull) {
    		getEditText().setText(mOrigin);
    	}
    	if(positive || !isTextChange || isTextNull) {
    		super.onDialogClosed(true);
    	} else {
            if (mSaveConfirmDialog != null) {
            	mSaveConfirmDialog.dismiss();
            	mSaveConfirmDialog = null;
            }
			mSaveConfirmDialog = new AlertDialog.Builder(
					getContext()).setMessage(R.string.need_save_content)
					.setTitle(android.R.string.dialog_alert_title)
					.setPositiveButton(android.R.string.ok,   new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        	MultiLineTitleEditTextPreference.this.onDialogClosed(true);
                        }})
                    .setNegativeButton(android.R.string.cancel, null)
					.setOnCancelListener(new DialogInterface.OnCancelListener() {
			            @Override
			            public void onCancel(DialogInterface dialog) {
			                if (mSaveConfirmDialog != null) {
			                	mSaveConfirmDialog.dismiss();
			                	mSaveConfirmDialog = null;
			                }
			            }
			        }).create();

			mSaveConfirmDialog.show();
    	}
    }
}
