package com.hb.interception.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Text;

import com.hb.interception.R;
import com.hb.interception.util.YuloreUtil;
import com.hb.tms.MarkManager;

import hb.app.HbActivity;
import hb.app.AlertActivity;
import hb.app.dialog.AlertDialog;
import hb.preference.PreferenceManager;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

public class updateMarkActivity extends AlertActivity {
    private static final int MAX_LENGTH = 20;
	private static final String CUSTOM_MARK_PREFERENCES_NAME = "custom_mark_prefs";
    private static final String CUSTOM_MARK_NAME = "custom_mark_name";
	private SharedPreferences mPrefs;
	private EditText mEdit ; 
	private InputMethodManager mInputManager;  
	private List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
	private String mType;
	private String number;
	private int lastNum;
	private Context mContext;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.update_mark);
		mContext = this;
		Intent intent = this.getIntent();
		number = intent.getStringExtra("mark_number");  
		TextView textView = (TextView) findViewById(R.id.title);
		textView.setText(number);
	    ListView markList = (ListView)findViewById(R.id.mark_list);
	    markList.setAdapter(getAdapter()); 
	    markList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				mType = (String) list.get(position).get("mark_name");
				int listSize = list.size();
				lastNum = listSize -1;
				if (YuloreUtil.isMarked(mContext, number)) {
					lastNum = listSize -2;
					if (position == listSize - 1) {
						YuloreUtil.deleteUserMark(mContext, number);
						finish();
						return;
					}
				}
				if (position == lastNum) {
					showCustomMarkDialog();
					return;
				}
				save();
			}
		});
 } 
	 @Override
	    protected void onNewIntent(Intent intent) {    
	        setIntent(intent);
	        number = intent.getStringExtra("mark_number");  
	    }
	 
	 private List<Map<String, Object>> getData() {
	        list.add(initItem(getString(R.string.mark_name_ad)));
	        list.add(initItem(getString(R.string.mark_name_house)));
	        list.add(initItem(getString(R.string.mark_name_swindle)));
	        list.add(initItem(getString(R.string.mark_name_express)));
	        list.add(initItem(getString(R.string.mark_name_taxi)));
	        list.add(initItem(getString(R.string.mark_name_other)));
	        if (YuloreUtil.isMarked(mContext, number)) {
		        list.add(initItem(getString(R.string.remove_mark)));
	        }
	        return list;
	}
	 private Map<String, Object> initItem(String str) {
		  Map<String, Object> map = new HashMap<String, Object>();
		  map.put("mark_name", str);
		  return map;
	 }
	 
	 private SimpleAdapter getAdapter() {
		 SimpleAdapter adapter = new SimpleAdapter(mContext, getData(),  R.layout.mark_content_view, new String[] { "mark_name" }, new int[] { R.id.mark_name });
		 return adapter;
	 }
	 
	private AlertDialog mDialog  ;
	private android.widget.Button posBtn;
	
	private void showCustomMarkDialog() {
		if (mDialog != null) {
			dialogShow();
			return ;
		}
		mDialog = new AlertDialog.Builder(this)
				.setView(mEdit)
				.setTitle(R.string.mark_name_other_dialog)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mType = mEdit.getText().toString();
								dismissDialog();
								save();
							}
						})
				.setNegativeButton(android.R.string.cancel, null).create();
		dialogShow();
	}

	private void dialogShow() {
		initEditText();
		mDialog.show();
		mDialog.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		mDialog.setCanceledOnTouchOutside(false);
		posBtn = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
		if (mEdit.getText().length() == 0) {
			setPosBtnState(false);
		}
	}
	
	private void initEditText() {
		mEdit.addTextChangedListener(prefWatcher);
		InputFilter[] filters = { new NameLengthFilter(MAX_LENGTH) };
		mEdit.setFilters( filters);
		String defaultValue = mPrefs.getString(CUSTOM_MARK_NAME, "");
		if (!TextUtils.isEmpty(defaultValue)) {
			mEdit.setText(defaultValue);
		}
		Editable etable = mEdit.getText();
        mEdit.setSelection(etable.length());// 光标置位 
	}
	
//	private  void showSoftInput() {
//        mEdit.setFocusable(true);
//        mEdit.setFocusableInTouchMode(true);
//        mEdit.requestFocus();
//        mInputManager.showSoftInput(mEdit, 0);
//	}
	
	@Override
	protected void onResume() {
		super.onResume();
		PreferenceManager pm = new PreferenceManager(this, 100);
		pm.setSharedPreferencesName(CUSTOM_MARK_PREFERENCES_NAME);
		mPrefs = pm.getSharedPreferences();
		mEdit = new EditText(this);
		mInputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
	}
	
	private void save() {
		YuloreUtil.updatetMark(mType, number);
		YuloreUtil.insertUserMark(mContext, number, mType);
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(CUSTOM_MARK_NAME, mEdit.getText().toString()).commit();
        editor.commit();
	    Toast.makeText(
	    		mContext,
	    		mContext.getResources().getString(R.string.mark_name_success_toast),
                Toast.LENGTH_SHORT).show();
	    finish();
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
			int length = s.length();
			if (length == 0) {
				setPosBtnState(false);
			} else {
				setPosBtnState(true);
			}
		}
    };
    
	private void setPosBtnState(boolean visable) {
		if (posBtn != null) {
			posBtn.setClickable(visable);
			posBtn.setEnabled(visable);
		}
	}
	
	private void dismissDialog() {
        if (mDialog != null) {
        	mDialog.dismiss();
//        	mDialog = null;
        }
	}
	
	@Override
    public void finish() {
        super.finish();  
        overridePendingTransition(0, 0);
    }
	
	private class NameLengthFilter implements InputFilter {
		int MAX_EN;// 最大英文/数字长度 一个汉字算两个字母
		String regEx = "[\\u4e00-\\u9fa5]"; // unicode编码，判断是否为汉字

		public NameLengthFilter(int mAX_EN) {
			super();
			MAX_EN = mAX_EN;
		}

		@Override
		public CharSequence filter(CharSequence source, int start, int end,
				Spanned dest, int dstart, int dend) {
			int destCount = dest.toString().length()
					+ getChineseCount(dest.toString());
			int sourceCount = source.toString().length()
					+ getChineseCount(source.toString());
			if (destCount + sourceCount > MAX_EN) {
				Toast.makeText(mContext,
						mContext.getResources().getString(R.string.max_chars),
						Toast.LENGTH_LONG).show();
				return "";
			} else {
				return source;
			}
		}

		private int getChineseCount(String str) {
			int count = 0;
			Pattern p = Pattern.compile(regEx);
			Matcher m = p.matcher(str);
			while (m.find()) {
				for (int i = 0; i <= m.groupCount(); i++) {
					count = count + 1;
				}
			}
			return count;
		}
	}
}