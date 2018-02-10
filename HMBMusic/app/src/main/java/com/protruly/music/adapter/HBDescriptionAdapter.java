package com.protruly.music.adapter;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.protruly.music.R;

import java.util.ArrayList;

/**
 * Created by hujianwei on 17-9-1.
 */

public class HBDescriptionAdapter extends BaseAdapter{
    private ArrayList<String> mArrayList;
    private Context mContext;
    public HBDescriptionAdapter(Context context,ArrayList<String> list){
        mContext = context;
        mArrayList = list;
    }
    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return mArrayList==null ? 0 : mArrayList.size();
    }

    @Override
    public Object getItem(int arg0) {
        // TODO Auto-generated method stub
        return mArrayList==null ? null : mArrayList.get(arg0);
    }

    @Override
    public long getItemId(int arg0) {
        // TODO Auto-generated method stub
        return arg0;
    }

    @Override
    public View getView(int arg0, View arg1, ViewGroup arg2) {
        // TODO Auto-generated method stub
        if(arg1==null){
            TextView textView = new TextView(mContext);
            textView.setPadding((int)mContext.getResources().getDimension(R.dimen.hb_decription_padding), 0, (int)mContext.getResources().getDimension(R.dimen.hb_decription_padding), 0);
            textView.setLineSpacing(mContext.getResources().getDimension(R.dimen.hb_decription_space), 1.0f);
            textView.setTextColor(mContext.getResources().getColor(R.color.hb_decription_color));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,mContext.getResources().getDimension(R.dimen.hb_decription_textsize));
            if(mArrayList.get(arg0)!=null)
                textView.setText(mArrayList.get(arg0));
            arg1 = textView;

        }
        return arg1;
    }
}
