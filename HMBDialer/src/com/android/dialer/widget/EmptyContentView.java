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

package com.android.dialer.widget;

import com.mediatek.contacts.simcontact.SimCardUtils;
import hb.view.menu.BottomWidePopupMenu;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.android.dialer.DialerApplication;
import com.android.dialer.R;

public class EmptyContentView extends LinearLayout implements View.OnClickListener {

	public static final int NO_LABEL = 0;
	public static final int NO_IMAGE = 0;
	protected static final String TAG = "EmptyContentView";

	private ImageView mImageView;
	private TextView mDescriptionView;
	private TextView mActionView;
	private Context context;
	private OnEmptyViewActionButtonClickedListener mOnActionButtonClickedListener;

	public interface OnEmptyViewActionButtonClickedListener {
		public void onEmptyViewActionButtonClicked(int id);
	}

	public EmptyContentView(Context context) {
		this(context, null);
	}

	public EmptyContentView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public EmptyContentView(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	private BottomWidePopupMenu bottomWidePopupMenu;
	private Button button1,button2;
	public EmptyContentView(Context context, AttributeSet attrs, int defStyleAttr,
			int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		this.context=context;
		setOrientation(LinearLayout.VERTICAL);
		setGravity(Gravity.CENTER);
		final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.empty_content_view, this);
		// Don't let touches fall through the empty view.
		setClickable(true);
		mImageView = (ImageView) findViewById(R.id.emptyListViewImage);
		mDescriptionView = (TextView) findViewById(R.id.emptyListViewMessage);
		mActionView = (TextView) findViewById(R.id.emptyListViewAction);
		mActionView.setOnClickListener(this);

		button1=(Button)findViewById(R.id.button1); 
		button2=(Button)findViewById(R.id.button2); 
		button1.setOnClickListener(this);
		button2.setOnClickListener(this);

		bottomWidePopupMenu = new BottomWidePopupMenu(getContext());
		bottomWidePopupMenu.inflateMenu(R.menu.hb_import_from_sim_bottom_menu);
		bottomWidePopupMenu.setOnMenuItemClickedListener(new BottomWidePopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onItemClicked(MenuItem item) {
				// TODO Auto-generated method stub
				Log.d(TAG,"onItemClicked Item:"+item.getTitle());
				switch(item.getItemId()){
				case R.id.import_from_sim1_menu:{
					if(!canImport(DialerApplication.getSubId(0))) return true;
					Intent intent=new Intent("android.intent.action.contacts.HbContactImportExportActivity");
					intent.putExtra("source", "importFromSIM1");
					getContext().startActivity(intent);
					break;
				}
				case R.id.import_from_sim2_menu:{
					if(!canImport(DialerApplication.getSubId(1))) return true;
					Intent intent=new Intent("android.intent.action.contacts.HbContactImportExportActivity");
					intent.putExtra("source", "importFromSIM2");
					getContext().startActivity(intent);
					break;
				}
				default:
					break;
				}
				return true;
			}
		});

		
	}


	private boolean canImport(int subId){
		boolean isSimEnable=DialerApplication.isAnySimEnabled(context);
		if(isAirPlaneModeOn()) {
			Toast.makeText(context, context.getString(R.string.hb_airmode_mode_toast), Toast.LENGTH_LONG).show();
			return false;
		}
		if(!isSimEnable) {
			Toast.makeText(context, context.getString(R.string.hb_no_simcard_toast), Toast.LENGTH_LONG).show();
			return false;
		}
		
		if (!SimCardUtils.isPhoneBookReady(subId)) {
			Toast.makeText(context, R.string.icc_phone_book_invalid, Toast.LENGTH_LONG).show();
			Log.i(TAG, "[doImportExport] phb is not ready.");
			return false;
		}
		
		return true;
	}
	private boolean isAirPlaneModeOn(){
		int mode = 0;
		try {
			mode = Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON);
		}catch (SettingNotFoundException e) {
			e.printStackTrace();
		}
		Log.d(TAG,"mode:"+mode);
		return mode == 1;//为1的时候是飞行模式
	}

	public void setDescription(int resourceId) {
		if (resourceId == NO_LABEL) {
			mDescriptionView.setText(null);
			mDescriptionView.setVisibility(View.GONE);
		} else {
			mDescriptionView.setText(resourceId);
			mDescriptionView.setVisibility(View.VISIBLE);
		}
	}

	public void setImage(int resourceId) {
		mImageView.setImageResource(resourceId);
		if (resourceId == NO_LABEL) {
			mImageView.setVisibility(View.GONE);
		} else {
			mImageView.setVisibility(View.VISIBLE);
		}
	}

	public void setActionLabel(int resourceId) {
		if (resourceId == NO_LABEL) {
			mActionView.setText(null);
			mActionView.setVisibility(View.GONE);
		} else {
			mActionView.setText(resourceId);
			mActionView.setVisibility(View.VISIBLE);
		}
	}

	public void showButtons(boolean show){
		button1.setVisibility(show?View.VISIBLE:View.GONE);
		button2.setVisibility(show?View.VISIBLE:View.GONE);
	}

	public boolean isShowingContent() {
		return mImageView.getVisibility() == View.VISIBLE
				|| mDescriptionView.getVisibility() == View.VISIBLE
				|| mActionView.getVisibility() == View.VISIBLE;
	}

	public void setActionClickedListener(OnEmptyViewActionButtonClickedListener listener) {
		mOnActionButtonClickedListener = listener;
	}

	@Override
	public void onClick(View v) {
		        if (mOnActionButtonClickedListener != null) {
		            mOnActionButtonClickedListener.onEmptyViewActionButtonClicked(v.getId());
		        }
		switch (v.getId()) {
		case R.id.button1:{
			Intent intent=new Intent("android.intent.action.contacts.HbContactImportExportActivity");
			intent.putExtra("source", "importFromStorage");
			getContext().startActivity(intent);
			break;
		}

		case R.id.button2:{
			if(DialerApplication.isMultiSimEnabled){
				bottomWidePopupMenu.show();
			}else{
				if(!canImport(DialerApplication.getSingleSubId(context))) return;
				Intent intent=new Intent("android.intent.action.contacts.HbContactImportExportActivity");
				intent.putExtra("source", "importFromSIM");
				getContext().startActivity(intent);
			}

			break;
		}

		default:
			break;
		}
	}
}
