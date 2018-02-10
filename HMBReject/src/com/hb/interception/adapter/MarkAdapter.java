package com.hb.interception.adapter;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import hb.app.dialog.AlertDialog;

import com.hb.interception.R;
import com.hb.interception.activity.slideDeleteListener;
import com.hb.interception.util.BlackUtils;
import com.hb.interception.util.InterceptionUtils;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
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

public class MarkAdapter extends InterceptionAdapterBase {


	public MarkAdapter(Context context, Cursor c) {
		super(context, c);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		// TODO Auto-generated method stub
		TextView content = (TextView) view.findViewById(R.id.content);
		content.setText(getCursor().getString(
				getCursor().getColumnIndex("lable")));

		super.bindView(view, context, cursor);

	}

	@Override
	public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {
		// TODO Auto-generated method stub
		SliderView view = (SliderView)mInflater.inflate(R.layout.mark_list_item, null);
		//view.addTextButton(InterceptionUtils.SLIDER_BTN_POSITION_DELETE, arg0.getString(R.string.del));
		return view;
	}

}
