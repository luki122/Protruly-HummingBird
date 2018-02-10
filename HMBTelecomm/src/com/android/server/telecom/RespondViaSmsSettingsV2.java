package com.android.server.telecom;

import hb.app.HbActivity;
import hb.app.dialog.AlertDialog;
import hb.preference.PreferenceManager;
import hb.widget.toolbar.Toolbar;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import hb.widget.ActionMode.Item;
import hb.widget.ActionModeListener;
import hb.widget.ActionMode;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;


public class RespondViaSmsSettingsV2 extends HbActivity  {

	private static final String TAG = "RespondViaSmsSettingsV2";
	
	private Context mContext;
	private EditText[] mEditText;
	public ActionMode mActionMode;
    private AlertDialog mSaveConfirmDialog;
    private boolean mIsNeedSave;
    private InputMethodManager mInputManager; 
	
    private SharedPreferences mPrefs;
    private Handler mHandler = new Handler();
    
//    private View.OnFocusChangeListener focusListener = new View.OnFocusChangeListener () {
//        public void onFocusChange(View v, boolean hasFocus) {
//			showActionMode(true);
//        }
//    };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this;
		setHbContentView(R.layout.respond_via_sms_settings_v2);
		
        // This function guarantees that QuickResponses will be in our
        // SharedPreferences with the proper values considering there may be
        // old QuickResponses in Telephony pre L.
        QuickResponseUtils.maybeMigrateLegacyQuickResponses(this);
		
        PreferenceManager pm = new PreferenceManager(this, 100);
        pm.setSharedPreferencesName(QuickResponseUtils.SHARED_PREFERENCES_NAME);
        mPrefs = pm.getSharedPreferences();
        
		mEditText = new EditText[3];
        for (int i = 0; i<3; i++) {
        	String idname = "canned_response_pref_" + (i + 1);
            int resId =getResources().getIdentifier("canned_response_pref_" + (i + 1), "id", getPackageName());
            mEditText[i] = (EditText)findViewById(resId);
            //zhangcj modfiy beause pref default is null
            String defaultValue = mPrefs.getString(idname, "");
    		if (!TextUtils.isEmpty(defaultValue)) {
    		    mEditText[i].setText(defaultValue);
    		}
    		mEditText[i].addTextChangedListener(prefWatcher);
        }

		initActionbar();
		initToolBar();

		mInputManager = (InputMethodManager) mContext  
                .getSystemService(Context.INPUT_METHOD_SERVICE);  
	}
	
	protected void onPostResume() {
		super.onPostResume();
		mHandler.postDelayed(new Runnable(){
			public void run(){
				showActionMode(true);
		        Editable etable = mEditText[0].getText();
		        mEditText[0].setSelection(etable.length());// 光标置位    
		        mInputManager.showSoftInput(mEditText[0], 0);
			}
		}, 300);


	}
	
    protected void onDestroy() {
    	super.onDestroy();
    	dismissDialog();
    }
		
	private void initActionbar() {
		mActionMode = getActionMode();	
		mActionMode.setTitle(R.string.editing_respond_sms);
	    mActionMode.enableItem(ActionMode.POSITIVE_BUTTON, false);
		setActionModeListener(new ActionModeListener() {

			@Override
			public void onActionItemClicked(Item item) {
				// TODO Auto-generated method stub
				switch (item.getItemId()) {
				case ActionMode.POSITIVE_BUTTON:
				    save();
					break;
				case ActionMode.NAGATIVE_BUTTON:
					cancel();
					break;
				default:
				}

			}

			@Override
			public void onActionModeDismiss(ActionMode arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onActionModeShow(ActionMode arg0) {
				// TODO Auto-generated method stub
			}

		});
	}
	
	private void dismissDialog() {
        if (mSaveConfirmDialog != null) {
        	mSaveConfirmDialog.dismiss();
        	mSaveConfirmDialog = null;
        }
	}
	
	private void cancel() {
		dismissDialog();
		if(mIsNeedSave) {

			mInputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
					InputMethodManager.HIDE_NOT_ALWAYS);
			mSaveConfirmDialog = new AlertDialog.Builder(
					this).setMessage(R.string.need_save_content)
					.setTitle(android.R.string.dialog_alert_title)
					.setPositiveButton(android.R.string.ok,   new DialogInterface.OnClickListener() {
	                    @Override
	                    public void onClick(DialogInterface dialog, int which) {                    	
	                	    save();
	                    	dismissDialog();
	                	    finish();
	                    }})
	                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
	                    @Override
	                    public void onClick(DialogInterface dialog, int which) {
	                    	dismissDialog();
	                	    finish();
	                    }})
					.setOnCancelListener(new DialogInterface.OnCancelListener() {
			            @Override
			            public void onCancel(DialogInterface dialog) {
			            	dismissDialog();
			            }
			        }).create();
	
			mSaveConfirmDialog.show();
		} else {
			finish();
		}
	}
	
	
	private void save() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(QuickResponseUtils.KEY_CANNED_RESPONSE_PREF_1, mEditText[0].getText().toString()).commit();
        editor.putString(QuickResponseUtils.KEY_CANNED_RESPONSE_PREF_2, mEditText[1].getText().toString()).commit();
        editor.putString(QuickResponseUtils.KEY_CANNED_RESPONSE_PREF_3, mEditText[2].getText().toString()).commit();
        editor.commit();
        finish();
	    Toast.makeText(
	    		mContext,
	    		mContext.getResources().getString(R.string.save_respond_sms_complete),
                Toast.LENGTH_LONG).show();
        mIsNeedSave = false;
        mActionMode.enableItem(ActionMode.POSITIVE_BUTTON, false);
	}

	
	private void initToolBar() {
		Toolbar myToolbar = this.getToolbar();
//		myToolbar.setTitle(R.string.respond_via_sms_setting_title_2);		
		myToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	finish();
            }
        });
	}

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
        	 mIsNeedSave = s.length() > 0;
             mActionMode.enableItem(ActionMode.POSITIVE_BUTTON, s.length() > 0);
        	
        	if(s.length() >= 70) {
        	    Toast.makeText(
        	    		mContext,
        	    		mContext.getResources().getString(R.string.max_chars),
                        Toast.LENGTH_LONG).show();
        	}
        }
    };
}