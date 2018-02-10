package com.hb.interception.activity;

import java.util.ArrayList;
import java.util.List;

import hb.app.AlertActivity;
import hb.app.HbActivity;
import hb.view.menu.BottomWidePopupMenu;
import hb.widget.toolbar.Toolbar;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.Contacts.Entity;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import hb.widget.ActionMode;
import hb.widget.ActionModeListener;
import hb.widget.ActionMode.Item;

import com.hb.interception.util.BlackUtils;
import com.hb.interception.util.ContactUtils;
import com.hb.interception.util.FormatUtils;
import com.hb.interception.util.InterceptionUtils;
import com.hb.interception.InterceptionApplication;
import com.hb.interception.util.YuloreUtil;
import com.hb.interception.R;

public class AddBlackManually extends HbActivity {
	private static final String TAG = "AddBlackManually";

	private EditText mNumberEdit, mNameEdit;
	private String mNumber, mName;
	private Context mContext;
	private boolean mIsWhite = false;	
	private Toolbar myToolbar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHbContentView(R.layout.manually_add);

		mContext = this;

		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		
		mIsWhite = getIntent().getBooleanExtra("white", false);

		mNumberEdit = (EditText) findViewById(R.id.number);
		//begin tangyisen
		mNumberEdit.setHintTextColor(R.color.hint_text_color);
		//end tangyisen
		mNameEdit = (EditText) findViewById(R.id.name);
		mNameEdit.setHintTextColor(R.color.hint_text_color);
		mNumberEdit
				.setFilters(new InputFilter[] { new InputFilter.LengthFilter(15) });		
		mNameEdit.setFilters(new InputFilter[] { new InputFilter.LengthFilter(
				15) });
		
		mNumberEdit.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub
				mNumber = mNumberEdit.getText().toString().replace("-", "")
						.replace(" ", "");
				if (mNumber != null || !"".equals(mNumber)) {
					String contactName = ContactUtils
							.getContactNameByPhoneNumber(mContext, mNumber);
					if (!TextUtils.isEmpty(contactName)) {
						mNameEdit.setText(contactName);
					} 
//					else {
//						mNameEdit.setText("");
//					}
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub

			}
		});
		

		initToolBar();

	}
	
	private TextView mCancel;
	private TextView mAdd;
	private void initToolBar() {
	    myToolbar = getToolbar();
		myToolbar.setTitle(R.string.manaully_add_title);
		myToolbar.setOnMenuItemClickListener(this);
		myToolbar.inflateMenu(R.menu.toolbar_menu_add);
	}	
	
	@Override
	public boolean onMenuItemClick(MenuItem item) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onMenuItemClick--->" + item.getTitle());
		if (item.getItemId() == R.id.add) {
			 doSaveAction();
			return true;
		}
		return false;
	}

	protected void doCanelAction() {
		finish();
	}

	protected void doSaveAction() {
		// TODO Auto-generated method stub
		mNumber = mNumberEdit.getText().toString().replace("-", "")
				.replace(" ", "");
		mName = mNameEdit.getText().toString().replace("-", "")
				.replace(" ", "");
		if (!TextUtils.isEmpty(mNumber)) {
			if (InterceptionUtils.isNoneDigit(mNumber)) {
		        showToast(R.string.format);
				return;
			}
		
		    if(mIsWhite ? BlackUtils.isWhiteNumberAlreadyExisted(mContext, mNumber) :BlackUtils.isNumberAlreadyExisted(mContext, mNumber)) {
		        showToast(R.string.exist_number);
		        return;
		    }
		    
	        saveInternal();			
		} else {
	        showToast(R.string.no_number);
			return;
		}

	}

	private void saveInternal() {
		
		if(mIsWhite) {
			BlackUtils.saveWhiteToDb(mContext, mNumber, mName);
	        showToast(R.string.add_to_white_over);
		} else {
			BlackUtils.saveBlackToDb(mContext, mNumber, mName);
	        showToast(R.string.add_to_black_over);
		}	
		
		finish();
	}
	
	private void showToast(int resId) {
        Toast.makeText(
                mContext,
                mContext.getResources().getString(resId),
                Toast.LENGTH_LONG).show();
	}
	
	@Override
    public void onNavigationClicked(View view) {
        finish();
    }


}
