package cn.com.protruly.soundrecorder.recordlist;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.com.protruly.soundrecorder.R;
import cn.com.protruly.soundrecorder.util.GlobalUtil;

/**
 * Created by wenwenchao on 17-8-22.
 */

public class TimeGridViewAdapter extends BaseAdapter{

        Context mContext;
        List<Long> mList;

        public TimeGridViewAdapter(Context context,List<Long> list){
            this.mContext = context;
            this.mList = list;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int i) {
            return mList.get(i);
        }

        @Override
        public long getItemId(int i) {return i;}

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View  v;
            GridAdapterViewHolder vh;
            if(convertView==null){
                v = LayoutInflater.from(mContext).inflate(R.layout.item_gridview_layout,parent,false);
                vh = new GridAdapterViewHolder(v);
                v.setTag(vh);
            }else{
                v = convertView;
                vh = (GridAdapterViewHolder)convertView.getTag();
            }
            vh.marktime.setText(GlobalUtil.formatTime_m_s((long)(mList.get(position))));
            return v;
        }



     class GridAdapterViewHolder{
        TextView marktime;
        String mTag;
        public GridAdapterViewHolder(View v) {
            marktime =  (TextView)v.findViewById(R.id.item_grid_time);
        }
        public String getmTag() {
            return mTag;
        }
        public void setmTag(String mTag) {
            this.mTag = mTag;
        }
    }


}
