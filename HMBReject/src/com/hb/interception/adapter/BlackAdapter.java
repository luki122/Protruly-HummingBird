package com.hb.interception.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hb.interception.R;
import com.hb.interception.activity.slideDeleteListener;
import com.hb.interception.util.YuloreUtil;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.Contacts;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.AbsListView.RecyclerListener;
import android.widget.TextView;
import hb.widget.SliderView;
import com.hb.interception.util.InterceptionUtils;

public class BlackAdapter extends InterceptionAdapterBase {
	
	public final static String TAG = "BlackAdapter";

	public BlackAdapter(Context context, Cursor c) {
		super(context, c);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		TextView nameText = (TextView) view.findViewById(R.id.name);
		TextView titleText = (TextView) view.findViewById(R.id.title);
		String name = cursor.getString(cursor.getColumnIndex(mIsWhite ? "white_name" :"black_name"));
		String number = cursor.getString(cursor.getColumnIndex("number"));
		
		nameText.setText(number);
		if (TextUtils.isEmpty(name)) {
			titleText.setText("");
		} else {
			titleText.setText(name);
		}

		super.bindView(view, context, cursor);
	}
	
	public void changeCursor(Cursor cursor) {
		super.changeCursor(cursor);
	}

	@Override
	public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {
		// TODO Auto-generated method stub
//		SliderView view = (SliderView)mInflater.inflate(R.layout.black_name_list_item, null);
//		view.addTextButton(InterceptionUtils.SLIDER_BTN_POSITION_DELETE, arg0.getString(R.string.del));	
		View view = mInflater.inflate(R.layout.black_name_list_item, null);
		return view;
	}

	private static HashMap<Long,  List<String>> mContactNumbers = new HashMap<Long,  List<String>>();
	
	public static HashMap<Long,  List<String>> getContactNumbers() {
		return mContactNumbers;
	}
	
	private boolean mIsWhite = false;
	public void setIsWhite(boolean value) {
		mIsWhite = value;
	}
    
}
