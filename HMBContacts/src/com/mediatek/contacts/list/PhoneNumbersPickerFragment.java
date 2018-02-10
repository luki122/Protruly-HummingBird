/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
package com.mediatek.contacts.list;

import com.android.contacts.common.hb.FragmentCallbacks;

import com.android.contacts.R;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.contacts.common.list.ContactEntryListAdapter;

import com.android.contacts.common.list.ContactListFilter;

public class PhoneNumbersPickerFragment extends DataKindBasePickerFragment {
	public static final String FRAGMENT_ARGS = "intent";
	private static final String TAG = "PhoneNumbersPickerFragment";
	@Override
	protected ContactEntryListAdapter createListAdapter() {
		PhoneNumbersPickerAdapter adapter = new PhoneNumbersPickerAdapter(getActivity(),
				getListView());
		if(hbFilter!=null){
			adapter.setFilter(hbFilter);
		}else{
//			adapter.setFilter(ContactListFilter
//					.createFilterWithType(ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS));
			adapter.setFilter(ContactListFilter.createAccountFilter("Local Phone Account","Phone",null,null,null));
		}
		adapter.setDisplayPhotos(false);
		adapter.setSectionHeaderDisplayEnabled(true);
		adapter.setQuickContactEnabled(false);
		adapter.setHbFilterString(hbFilterString);

		final Intent intent = getArguments().getParcelable(FRAGMENT_ARGS);
		adapter.setExistDataList(intent.getLongArrayExtra("data_ids"));
		return adapter;
	} 

	@Override
	protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
		super.onCreateView(inflater, container);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		nameColumnIndex=4;
		quanpinyinColumnIndex=12;
		jianpinyinColumnIndex=13;
		phonebookbucketColumnIndex=14;
		contactIdIndex=6;
		numberIndex=3;
		snippetColumnIndex=3;
		_PROJECTION=PhoneNumbersPickerAdapter.PHONES_PROJECTION;
	}
}
