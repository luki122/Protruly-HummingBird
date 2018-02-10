package com.hb.interception.adapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hb.interception.R;
import com.hb.interception.util.FormatUtils;
import com.hb.interception.util.InterceptionUtils;
import com.hb.interception.util.YuloreUtil;
import com.hb.tms.AreaManager;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.CallLog.Calls;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AbsListView.RecyclerListener;
import android.widget.Toast;
import hb.widget.SliderView;

public class PhoneAdapter extends InterceptionAdapterBase {
    private static final String TAG = "PhoneAdapter";
    private String name;
    private int count;
    private int[] cardIcons = {R.drawable.svg_dial_card1, R.drawable.svg_dial_card2, R.drawable.sim_not_found};

    public PhoneAdapter(Context context, Cursor c) {
        super(context, c);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        CallHolder mHolder = (CallHolder)view.getTag();
        final ImageView simView = mHolder.simView;
        final String number = cursor.getString(cursor.getColumnIndex("number"));
        final String area = cursor.getString(cursor.getColumnIndex("area"));
        final long dateTime = Long.parseLong(cursor.getString(cursor.getColumnIndex("date")));
        name = cursor.getString(cursor.getColumnIndex("name"));
        int rejectMode  =  cursor.getInt(cursor.getColumnIndex("reject"));; 
    	String mark = cursor.getString(cursor.getColumnIndex("mark")); ;
    	String black_name = cursor.getString(cursor.getColumnIndex("black_name"));; 
    	int  user_mark =cursor.getInt(cursor.getColumnIndex("user_mark"));; 
                       
        String date = FormatUtils.formatTimeStampStringForItem(context, dateTime);
        mHolder.date.setText(date);
        simView.setTag(name);
        final int slotid = cursor.getInt(cursor.getColumnIndex("slotid"));
        if (slotid == 0 || slotid == 1) {
            mHolder.simView.setImageResource(cardIcons[slotid]);
            mHolder.simView.setVisibility(View.VISIBLE);
        } else {
            mHolder.simView.setVisibility(View.GONE);
        }
        
        count = cursor.getInt(cursor.getColumnIndex("count"));
        StringBuilder titleStr = new StringBuilder();
        titleStr.append(count > 1 ? context.getResources().getString(R.string.rejected_count_message, count) :context.getResources().getString(R.string.rejected_message));
        titleStr.append(" ");
        titleStr.append(number);
        StringBuilder contentStr = new StringBuilder();
        if(rejectMode == 1) {        	
        	contentStr.append(context.getString(R.string.black_call_listitem_title));
        	if(!TextUtils.isEmpty(name))  {
        		contentStr.append("(" + name+ ")");
        	} else if(!TextUtils.isEmpty(black_name))  {
        		contentStr.append("(" + black_name+ ")");
        	}
        } else {        	
            String countString = user_mark == -1 ? context.getResources().getString(R.string.mark_by_user) :  context.getResources().getString(R.string.mark_count, user_mark);                 
            contentStr.append(mark + "(" + countString + ")"); 
        }      
      
        
        mHolder.content.setText(titleStr.toString());
        mHolder.title.setText(contentStr);
        mHolder.title.setTextColor(Color.RED);
        
        
        if (TextUtils.isEmpty(area)) {
        	String areaFormTms = AreaManager.getArea(number);
        	if (TextUtils.isEmpty(areaFormTms)) {
                mHolder.mArea.setText(context.getResources().getString(R.string.mars));
        	} else {
               mHolder.mArea.setText(areaFormTms);
        	}
        } else {
            mHolder.mArea.setText(area);
        }
        
        if(mCheckBoxEnable) {
        	mHolder.mArea.setVisibility(View.GONE);
        	mHolder.date.setVisibility(View.GONE);
        } else {
        	mHolder.mArea.setVisibility(View.VISIBLE);
        	mHolder.date.setVisibility(View.VISIBLE);
        }
        
        super.bindView(view, context, cursor);
    }

    @Override
    public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {
        // TODO Auto-generated method stub
       // SliderView view = (SliderView)mInflater.inflate(R.layout.call_log_list_item, null);
       // view.addTextButton(InterceptionUtils.SLIDER_BTN_POSITION_DELETE, arg0.getString(R.string.del));
    	View view = mInflater.inflate(R.layout.call_log_list_item_content, null);
        CallHolder mHolder = new CallHolder(view);
        view.setTag(mHolder);
        return view;
    }

    protected void bindSectionHeaderAndDivider(View view, String date, Cursor mCursor) {
    }
    
    private void addDateText(LinearLayout headerUi, String date) {
    }

    private class CallHolder {
        TextView title;
        TextView content;
        TextView mArea;
        ImageView simView;
        CheckBox cb;
        TextView date;
        private CallHolder(View view) {
            title = (TextView)view.findViewById(R.id.title);
            content = (TextView)view.findViewById(R.id.content);
            mArea = (TextView)view.findViewById(R.id.area);
            simView = (ImageView)view.findViewById(R.id.sim);
            cb = (CheckBox)view.findViewById(R.id.list_item_check_box);
            date = (TextView)view.findViewById(R.id.date);
        }
    }
    
}
