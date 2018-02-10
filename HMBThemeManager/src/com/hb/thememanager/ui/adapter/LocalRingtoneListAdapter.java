package com.hb.thememanager.ui.adapter;

import android.content.Context;
import android.util.SparseArray;

import com.hb.thememanager.R;

public class LocalRingtoneListAdapter extends LocalRingtoneAdapter {



	public LocalRingtoneListAdapter(Context context) {
		super(context);

	}


	protected void initialItemLayout(SparseArray<Integer> itemLayoutArray){
		itemLayoutArray.put(TYPE_NORMAL,R.layout.list_item_ringtone);
		itemLayoutArray.put(TYPE_UPDATE,R.layout.list_item_ringtone);
		itemLayoutArray.put(TYPE_PAY,R.layout.list_item_ringtone);
		itemLayoutArray.put(TYPE_NORMAL_EDITABLE,R.layout.list_item_ringtone);
	}



}
