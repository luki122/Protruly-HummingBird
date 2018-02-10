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

package com.android.contacts.activities;

import com.android.contacts.R;
import com.android.contacts.activities.ContactEditorBaseActivity.ContactEditor.SaveMode;
import com.android.contacts.editor.CompactContactEditorFragment;
import com.android.contacts.common.activity.RequestPermissionsActivity;

import com.mediatek.contacts.simservice.SIMEditProcessor;
//import com.mediatek.contacts.util.Log;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;

import com.mediatek.contacts.activities.ActivitiesUtils;
import hb.widget.ActionMode;
import hb.widget.ActionModeListener;
import hb.widget.ActionMode.Item;

/**
 * Contact editor with only the most important fields displayed initially.
 */
public class CompactContactEditorActivity extends ContactEditorBaseActivity {
    private static final String TAG = "CompactContactEditorActivity";
    private static final String TAG_COMPACT_EDITOR = "compact_editor";
    private ActionMode actionMode;
    View mRLAll;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        Log.d(TAG, "[onCreate]");
        if (RequestPermissionsActivity.startPermissionActivity(this)) {
            Log.w(TAG, "[onCreate]no permisson,return!");
            return;
        }

        /// M: Descriptions: can not add contact when in delete or import processing
        if (ActivitiesUtils.isDeleteingContact(this)) {
            Log.i(TAG, "[onCreate]isDeleteingContact,return.");
            return;
        }

        setHbContentView(R.layout.compact_contact_editor_activity);

        mFragment = (CompactContactEditorFragment) getFragmentManager().findFragmentByTag(
                TAG_COMPACT_EDITOR);
        if (mFragment == null) {
            mFragment = new CompactContactEditorFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.compact_contact_editor_fragment_container,
                            (CompactContactEditorFragment) mFragment, TAG_COMPACT_EDITOR)
                    .commit();
        }
        mFragment.setListener(mFragmentListener);

        final String action = getIntent().getAction();
        final Uri uri = Intent.ACTION_EDIT.equals(action) ? getIntent().getData() : null;
        mFragment.load(action, uri, getIntent().getExtras());
        
        getToolbar().setElevation(0f);
        setupActionModeWithDecor(getToolbar());
        actionMode = getActionMode();
        if (actionMode != null) {
	        actionMode.setTitle(Intent.ACTION_EDIT.equals(action) ?R.string.edit_contact:R.string.hb_pickerNewContactHeader);
	        actionMode.setNagativeText(getString(R.string.hb_cancel));
			actionMode.setPositiveText(getString(R.string.hb_ok));
			if (!Intent.ACTION_EDIT.equals(action)) {
				actionMode.enableItem(ActionMode.POSITIVE_BUTTON, false);
			}
			actionMode.bindActionModeListener(new ActionModeListener(){
				public void onActionItemClicked(Item item){
					Log.d(TAG,"onActionItemClicked,itemid:"+item.getItemId());
					switch (item.getItemId()) {
					case ActionMode.POSITIVE_BUTTON:
						mFragment.save(SaveMode.CLOSE, /* backPressed =*/ true);
						break;
					case ActionMode.NAGATIVE_BUTTON:			        
				        hideInputMethod(getCurrentFocus());
						mFragment.revert();
						break;
					}
				}
				
				/**
				 * ActionMode显示的时候触发
				 * @param actionMode
				 */
				public void onActionModeShow(ActionMode actionMode){
	
				}
	
				/**
				 * ActionMode消失的时候触发
				 * @param actionMode
				 */
				public void onActionModeDismiss(ActionMode actionMode){
	
				}
			});
        }
		
		mHandler.postDelayed(mShowActionModeRunnable,300);
		mRLAll = getWindow().getDecorView();
		if (mRLAll != null)
    		mRLAll.getViewTreeObserver().addOnGlobalLayoutListener(
                new OnGlobalLayoutListener() {
                    @SuppressLint("NewApi")
                    public void onGlobalLayout() {
                        //移除布局变化监听
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//                            mRLAll.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//                        } else {
//                            mRLAll.getViewTreeObserver().removeGlobalOnLayoutListener(this);
//                        }
                        //getWindow().getDecorView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        Rect r = new Rect();
                        mRLAll.getWindowVisibleDisplayFrame(r);
                        lent = r.height();
                        Log.i(TAG, "lent = " + (r.height()));
//                        int height = r.height()+r.top;//手机屏幕可见区域高度
//                        Log.i(TAG, "inputIsShow = " + (inputIsShow));
                    }
                });
    }
    
    public void showInputMethod(View view) {
        final InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, 0);
        }
    }

    public void hideInputMethod(View view) {
        final InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "[onResume]");
        if (inputIsShow) {
			final InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

			final View currentFocus = getCurrentFocus();
			if (imm != null && currentFocus != null) {
				imm.showSoftInput(currentFocus, 0);
			}
		}
    }
    /** @}*/

    @Override
    public void onBackPressed() {
        if (mFragment != null) {
            mFragment.revert();
        }
    }
    
    private Handler mHandler = new Handler();
   	private Runnable mShowActionModeRunnable = new Runnable() {

   		@Override
   		public void run() {
   			// TODO Auto-generated method stub
   			showActionMode(true);
   		}
   	};
   	int lent;
   	boolean inputIsShow = false;
   	@Override
	protected void onPause() {
   		if (lent >1600) {
        	inputIsShow = false;
        } else if (lent < 900) {
        	inputIsShow = true;
        }
		super.onPause();
		Log.d(TAG, "[onPause]");

		final InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

		final View currentFocus = getCurrentFocus();
		if (imm != null && currentFocus != null) {
			imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
		}
		Log.i(TAG, "[onPause]inputIsShow = " + (inputIsShow));
	}
}
