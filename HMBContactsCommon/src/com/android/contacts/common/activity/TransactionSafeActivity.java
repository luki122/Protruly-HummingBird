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

package com.android.contacts.common.activity;

import hb.view.menu.bottomnavigation.BottomNavigationView;
import hb.widget.toolbar.Toolbar;
import hb.widget.ActionMode;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;


/**
 * A common superclass that keeps track of whether an {@link Activity} has saved its state yet or
 * not.
 */
public abstract class TransactionSafeActivity extends hb.app.HbActivity {

	private boolean mIsSafeToCommitTransactions;
	public static final int SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR_ADD_LINE = 0x00000010;
	public Toolbar toolbar;
	public ActionMode actionMode;
	public BottomNavigationView bottomBar;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mIsSafeToCommitTransactions = true;

		//设置底部导航栏背景色 
		getWindow().setNavigationBarColor(android.graphics.Color.WHITE);

		//去掉底部导航栏水平线
		int flag = getWindow().getDecorView().getSystemUiVisibility();
		flag = flag & ~SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR_ADD_LINE;
		getWindow().getDecorView().setSystemUiVisibility(flag);
	}

	public void showFAB(boolean isShow) {
		
	}
	@Override
	protected void onStart() {
		super.onStart();
		mIsSafeToCommitTransactions = true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		mIsSafeToCommitTransactions = true;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mIsSafeToCommitTransactions = false;
	}

	/**
	 * Returns true if it is safe to commit {@link FragmentTransaction}s at this time, based on
	 * whether {@link Activity#onSaveInstanceState} has been called or not.
	 *
	 * Make sure that the current activity calls into
	 * {@link super.onSaveInstanceState(Bundle outState)} (if that method is overridden),
	 * so the flag is properly set.
	 */
	public boolean isSafeToCommitTransactions() {
		return mIsSafeToCommitTransactions;
	}
}
