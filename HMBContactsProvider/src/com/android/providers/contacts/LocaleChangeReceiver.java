/*
 * Copyright (C) 2014 MediaTek Inc.
 * Modification based on code covered by the mentioned copyright
 * and/or permission notice(s).
 */
/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.providers.contacts;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Context;
import android.content.IContentProvider;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.Locale;

import com.android.providers.hb.ContactsContract;
import android.util.Log;

/**
 * Locale change intent receiver that invokes {@link ContactsProvider2#onLocaleChanged} to update
 * the database for the new locale.
 */
public class LocaleChangeReceiver extends BroadcastReceiver {

	private static final String TAG = "LocaleChangeReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "[onReceive] intent:" + intent);

		if(intent!=null && intent.getAction()!=null && intent.getAction().equals("android.intent.action.BOOT_COMPLETED")){
			//add by liyang
			try{
			SharedPreferences sharedPreferences=context.getSharedPreferences("com.android.providers.contacts_preferences_hb", Context.MODE_WORLD_READABLE);
			SharedPreferences.Editor editor=sharedPreferences.edit();
			editor.putBoolean("is_updating_index_for_localechange", false);
			editor.commit();
			}catch(Exception e){
				Log.e(TAG,"e:"+e);
			}
			return;
		}

		IContentProvider iprovider =
				context.getContentResolver().acquireProvider(ContactsContract.AUTHORITY);
		ContentProvider provider = ContentProvider.coerceToLocalContentProvider(iprovider);
		if (provider instanceof ContactsProvider2) {
			Log.d(TAG, "[onReceive] call onLocaleChanged");

			
			Locale l = Locale.getDefault();  
			String language = l.getLanguage();  
			String country = l.getCountry();
			Log.d(TAG,"language:"+language+" country:"+country);
			
			if(TextUtils.equals("zh", language.toLowerCase()) && TextUtils.equals("tw", country.toLowerCase())) Log.d(TAG,"zh_tw");
			else ((ContactsProvider2)provider).onLocaleChanged();
		}
	}
}
