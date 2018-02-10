package com.protruly.music.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.protruly.music.R;
import com.xiami.music.model.RadioInfo;

import java.util.List;

/**
 * Created by hujianwei on 17-8-31.
 */

public class HBPlayRadioAdapter extends BaseAdapter {

    private List<RadioInfo> mDatas;
    private Context mContext;
    private int mPlayingPosition = -1;

    public HBPlayRadioAdapter(Context context, List<RadioInfo> list) {
        mContext = context;
        mDatas = list;
    }

    public void setPlayingPosition(int pos){
        mPlayingPosition = pos;
    }

    public int getCurrentPlayPosition(){
        return mPlayingPosition;
    }
    @Override
    public int getCount() {

        return mDatas.size();
    }

    @Override
    public Object getItem(int i) {

        return mDatas.get(i);
    }

    @Override
    public long getItemId(int i) {

        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewgroup) {

        Holdview holdview;
        if (view == null) {
            holdview = new Holdview();
            view = LayoutInflater.from(mContext).inflate(
                    R.layout.radio_list_item, null);
            holdview.playView = (ImageView) view
                    .findViewById(R.id.id_song_selected);
            holdview.songName = (TextView) view
                    .findViewById(R.id.id_name);
            holdview.songName.setTextColor(Color.WHITE);
            view.setTag(holdview);
        } else {
            holdview = (Holdview)view.getTag();
        }

        RadioInfo info = mDatas.get(i);
        if(info!=null){
            holdview.songName.setText(info.getName());
            if(mPlayingPosition==i){
                holdview.playView.setVisibility(View.VISIBLE);
            }else {
                holdview.playView.setVisibility(View.GONE);
            }
        }
        return view;
    }

    class Holdview {
        ImageView playView;
        TextView songName;
        TextView songAlbum;
    }
}
