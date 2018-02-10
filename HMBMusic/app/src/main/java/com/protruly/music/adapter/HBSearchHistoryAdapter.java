package com.protruly.music.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.protruly.music.R;
import com.protruly.music.util.LogUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hujianwei on 17-9-4.
 */

public class HBSearchHistoryAdapter extends BaseAdapter {

    private static final String TAG = "HBSearchHistoryAdapter";
    private List<String> datas = new ArrayList<String>();
    private Context mContext;
    private boolean isHistory;
    private String keyword=null;

    public HBSearchHistoryAdapter(Context context,boolean is) {
        mContext=context;
        isHistory =is;
    }

    public void addDatas(List<String> list){

        if(list==null){
            return;
        }
        datas=list;
        notifyDataSetChanged();
    }
	


    public void setKeyWord(String str){
        keyword=str;
    }

    @Override
    public int getCount() {

        return datas.size();
    }

    @Override
    public Object getItem(int arg0) {

        return datas.get(arg0);
    }

    @Override
    public long getItemId(int arg0) {

        return arg0;
    }

    @Override
    public View getView(int arg0, View arg1, ViewGroup arg2) {
        HoldView holdView;
//        if (arg1 == null) {
//            arg1 = LayoutInflater.from(mContext).inflate(com.hb.R.layout.hb_slid_listview,
//                    null);
//            RelativeLayout front = (RelativeLayout) arg1
//                    .findViewById(com.hb.R.id.hb_listview_front);
//            LayoutInflater.from(mContext).inflate(R.layout.hb_search_history_item, front);
//            RelativeLayout rl_control_padding = (RelativeLayout) arg1
//                    .findViewById(com.hb.R.id.control_padding);
//            rl_control_padding.setPadding(0, 0, 0, 0);
//            holdView = new HoldView();
//            holdView.name=(TextView) arg1.findViewById(R.id.hb_id_history);
//            holdView.sustion=(TextView) arg1.findViewById(R.id.hb_id_sustion);
//            arg1.setTag(holdView);
//        } else {
//            holdView = (HoldView)arg1.getTag();
//        }
//
//        if(isHistory){
//            holdView.name.setVisibility(View.VISIBLE);
//            holdView.sustion.setVisibility(View.GONE);
//            holdView.name.setText(datas.get(datas.size()-arg0-1));
//        }else{
//            holdView.name.setVisibility(View.GONE);
//            holdView.sustion.setVisibility(View.VISIBLE);
//            String text = datas.get(arg0);
//
//            if(keyword!=null){
//                int index=text.indexOf(keyword);
//                LogUtil.d(TAG, "index:"+index);
//                if(index!=-1){
//                    SpannableStringBuilder style=new SpannableStringBuilder(text);
//                    style.setSpan(new ForegroundColorSpan(Color.parseColor("#019c73")), index, index+keyword.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
//                    holdView.sustion.setText(style);
//                }else{
//                    holdView.sustion.setText(text);
//                }
//            }else{
//                holdView.sustion.setText(text);
//            }
//        }
        return arg1;
    }

    class HoldView{
        TextView name;
        TextView sustion;
    }
}
