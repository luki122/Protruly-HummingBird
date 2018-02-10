package com.hb.interception.adapter;

import java.util.HashSet;
import java.util.Set;

import com.hb.interception.InterceptionApplication;
import com.hb.interception.R;
import com.hb.interception.util.BlackUtils;
import com.hb.interception.util.FormatUtils;
import com.hb.interception.util.InterceptionUtils;
import com.hb.interception.util.YuloreUtil;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.CallLog.Calls;
import android.support.v4.widget.CursorAdapter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView.RecyclerListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import hb.widget.SliderView;

public class SmsAdapter extends InterceptionAdapterBase {

//	private int count;
	
	private int[] cardIcons = {R.drawable.svg_dial_card1, R.drawable.svg_dial_card2, R.drawable.sim_not_found};
	
	private int paddTop = (int) InterceptionApplication.getInstance().getResources().getDimension(R.dimen.sms_item_content_top);
	private int paddingBottom = (int) InterceptionApplication.getInstance().getResources().getDimension(R.dimen.sms_item_content_botten);
	private int paddingRight = (int) InterceptionApplication.getInstance().getResources().getDimension(R.dimen.sms_item_content_right);
	
	public SmsAdapter(Context context, Cursor c) {
		super(context, c);
	}

	@Override
	public void bindView(View arg0, Context arg1, Cursor arg2) {
		MessageHolder mHolder = (MessageHolder) arg0.getTag();
		final String address = arg2.getString(arg2.getColumnIndex("address"));

		mHolder.title.setText(address);
		mHolder.date.setText(FormatUtils.formatTimeStampStringForItem(mContext, Long.parseLong(arg2.getString(arg2.getColumnIndex("date")))));

		final ImageView simView = mHolder.simView;
		final int slotid = arg2.getInt(arg2.getColumnIndex("slotid"));
		String bodyText = arg2.getString(arg2.getColumnIndex("body"));
		int reject = arg2.getInt(arg2.getColumnIndex("reject"));
		
		String rejectTag = null;
		if (reject == 1) {
			rejectTag = mContext.getString(R.string.black);
		} else if (reject == 2) {
			rejectTag = arg2.getString(arg2.getColumnIndex("reject_tag"));
			if (TextUtils.isEmpty(rejectTag)) {
				rejectTag = mContext.getString(R.string.sms_cloud_reject_title);
			}
		} else if (reject == 3) {
			rejectTag = mContext.getString(R.string.sms_keyword_reject_title);
		}
		//mms display "mms reject "only
		if (arg2.getInt(arg2.getColumnIndex("ismms")) == 1) {
			bodyText = mContext.getString(R.string.mms_reject);
		}
		
		StringBuilder build = new StringBuilder();
		if (!TextUtils.isEmpty(rejectTag)) {
			bodyText = build.append("[").append(rejectTag).append("]")
					.append(bodyText).toString();
			SpannableString ss = new SpannableString(bodyText);
			ss.setSpan(new ForegroundColorSpan(Color.RED), 0,
					rejectTag.length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			mHolder.content.setText(ss);
		} else {
			mHolder.content.setText(bodyText);
		}
		
		if (mCheckBoxEnable) {
			simView.setVisibility(View.GONE);
			mHolder.date.setVisibility(View.GONE);
			paddingRight = (int) InterceptionApplication.getInstance().getResources().getDimension(R.dimen.sms_item_content_check_right);
		} else {
			if (slotid == 0 || slotid == 1) {
				simView.setImageResource(cardIcons[slotid]);
				simView.setVisibility(View.VISIBLE);
			} else {
				simView.setVisibility(View.GONE);
			}
        	mHolder.date.setVisibility(View.VISIBLE);
		}
		mHolder.content.setPadding(0, paddTop, paddingRight, paddingBottom);
		super.bindView(arg0, arg1, arg2);
	}

	@Override
	public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {
		// TODO Auto-generated method stub
        View view =  mInflater.inflate(R.layout.sms_list_item_content, null);
		MessageHolder mHolder = new MessageHolder(view);
		view.setTag(mHolder);
		return view;
	}

	private class MessageHolder {
		private TextView title;
//		private ImageView attachment;
		private TextView content;
		private TextView date;
		private ImageView simView;
		private CheckBox cb;
		//private ImageView slideDelete;

		private MessageHolder(View view) {
			title = (TextView) view.findViewById(R.id.sms_title);
//			attachment = (ImageView) view.findViewById(R.id.mms);

			content = (TextView) view.findViewById(R.id.sms_content);
			date = (TextView) view.findViewById(R.id.sms_date);
			simView = (ImageView)view.findViewById(R.id.sim);
			cb = (CheckBox) view.findViewById(R.id.list_item_check_box);
			//slideDelete = (ImageView) view.findViewById(R.id.slidedelete);
		}
	}	

}
