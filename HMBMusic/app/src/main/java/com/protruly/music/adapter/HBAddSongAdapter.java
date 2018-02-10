package com.protruly.music.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.model.Playlist;
import com.protruly.music.util.DialogUtil;

import java.util.ArrayList;

/**
 * Created by hujianwei on 17-9-1.
 */

public class HBAddSongAdapter extends BaseAdapter {
    private ArrayList<Playlist> mList;
    private Context mContext;
    class ViewHold{
        ImageView playlist_icon;
        TextView playlist_title;
    }
    public HBAddSongAdapter(Context context,ArrayList<Playlist> list){
        mList = list;
        mContext = context;
    }
    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return mList.size();
    }

    @Override
    public Playlist getItem(int position) {
        // TODO Auto-generated method stub
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        ViewHold vh;
        if(convertView == null){
            vh = new ViewHold();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.hb_addtoplaylist_layout, null);
            vh.playlist_icon = (ImageView)convertView.findViewById(R.id.hb_addsong_icon);
            vh.playlist_title = (TextView)convertView.findViewById(R.id.hb_addsong_title);
            convertView.setTag(vh);
        }else
            vh = (ViewHold)convertView.getTag();
        if(mList.get(position).mPlaylistIcon<=0)
            vh.playlist_icon.setImageResource(R.drawable.create_song_list_default_icon_normal);
        else
            vh.playlist_icon.setImageResource(mList.get(position).mPlaylistIcon);
        if(position==mList.size()-1){
            convertView.setBackgroundResource(R.drawable.hb_corners_bg);
        }else{
            convertView.setBackgroundResource(R.drawable.hb_playlist_item_clicked);
        }
        vh.playlist_title.setText(mList.get(position).mPlaylistName);
        convertView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                long pid = getItem(position).mPlaylistId;
                if (pid == -1) {
                    DialogUtil.initCreatePlayList(mContext);
                    DialogUtil.dismissDialog();
                } else {
                    MusicUtils.addToPlaylist(mContext, DialogUtil.mSongList, pid,
                            getItem(position).mPlaylistName);
                    DialogUtil.dismissDialog();
                    DialogUtil.notifyListener();
                }
            }
        });
        return convertView;
    }
}
