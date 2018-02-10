package com.protruly.music.adapter;

import android.content.Context;
import android.graphics.Color;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.util.HBIListItem;
import com.protruly.music.util.HBListItem;
import com.protruly.music.util.HBMusicUtil;
import com.protruly.music.util.LogUtil;

import java.util.ArrayList;
import java.util.Iterator;


/**
 * Created by hujianwei on 17-8-31.
 */

public class HBPlayerListAdapter <T extends HBIListItem> extends BaseAdapter {

    private static final String TAG = "HBPlayerListAdapter";
    private ArrayList<HBListItem> mList = null;
    private LayoutInflater mInflater;

    private static String mArtistName;
    private static String mAlbumName;
    private int mPlayingPosition = -1;
    private int mOldPosition = -1;
    private Context mContext;

    DeleteItemCallBack mCallBack;

    public HBPlayerListAdapter(Context context, ArrayList<HBListItem> list, DeleteItemCallBack mDelteCallBack) {
        if (list == null || context == null) {
            LogUtil.e(TAG, " HBPlayerListAdapter create list failed!");
            return;
        }
        this.mContext = context;
        mInflater = LayoutInflater.from(context);
        mList = list;
        mArtistName = context.getString(R.string.unknown);
        mAlbumName = context.getString(R.string.unknown_album_name);
        if ( mDelteCallBack != null){
            mCallBack = mDelteCallBack;
        }
    }

    public void setPlayingPosition(int position) {
        LogUtil.d(TAG, "setPlayingPosition position:" + position);
        mOldPosition = mPlayingPosition;
        mPlayingPosition = position;
    }

    public int getCurrentPlayPosition() {
        return mPlayingPosition;
    }

    @Override
    public int getCount() {
        if (mList == null) {
            return 0;
        }

        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        if (mList == null) {
            return null;
        }

        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (mList == null) {
            return null;
        }

        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.hb_playlist_item, null);
            holder = new ViewHolder();
            holder.mCurrentPlayView = (ImageView)convertView.findViewById(R.id.hb_currentplay);
            holder.songName = (TextView)convertView.findViewById(R.id.hb_playlist_title);
            holder.songAlbum = (TextView)convertView.findViewById(R.id.hb_playlist_album);
            holder.mDeleteView = (ImageView)convertView.findViewById(R.id.hb_clear_btn);
            holder.mSongType = (ImageView)convertView.findViewById(R.id.hb_song_type);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        if (mPlayingPosition == position) {
            LogUtil.d(TAG, "mPlayingPosition:" + mPlayingPosition);
            holder.mCurrentPlayView.setVisibility(View.VISIBLE);
        }else
        {
            holder.mCurrentPlayView.setVisibility(View.INVISIBLE);
        }

        HBListItem iteminfo = mList.get(position);
        if (iteminfo != null) {
            try {
                holder.songName.setText(iteminfo.getTitle());
                StringBuffer tBuffer = new StringBuffer();

                String artiststr = iteminfo.getArtistName();
                if (MediaStore.UNKNOWN_STRING.equals(artiststr) || artiststr == null) {
                    artiststr = mArtistName;
                }

                String albumstr = iteminfo.getAlbumName();
                albumstr = HBMusicUtil.doAlbumName(iteminfo.getFilePath(), albumstr);
                if (MediaStore.UNKNOWN_STRING.equals(albumstr) || albumstr == null) {
                    albumstr = mAlbumName;
                }
                tBuffer.append(artiststr);
                holder.songAlbum.setText(tBuffer.toString());
            } catch (Exception e) {
                Log.i(TAG, "HBPlayerListAdapter getView set item info failed!");
            }
        }

        if(!iteminfo.isAvailable()){
            holder.songName.setTextColor(Color.parseColor("#b3b3b3"));
            holder.songAlbum.setTextColor(Color.parseColor("#b3b3b3"));

        }else {
            holder.songName.setTextColor(Color.parseColor("#FF000000"));
            holder.songAlbum.setTextColor(Color.parseColor("#4D000000"));
        }

        holder.mSongType.setImageResource(R.drawable.hb_songtype_sq);



        holder.mDeleteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if( mCallBack != null){
                    mCallBack.onDeleteItemClickListener(view, position);
                }

                //deleteSongFromCurrentPlaylist( position);
            }
        });

        return convertView;
    }

    public interface DeleteItemCallBack
    {
        //ImageView 删除回调
        public void onDeleteItemClickListener(View v, int pos) ;
    }

    public static class ViewHolder{
        ImageView	mCurrentPlayView;
        ImageView   mSongType;
        ImageView   mDeleteView;
        TextView	songName;
        TextView 	songAlbum;
    }

    private int count;

    private  void deleteSongFromCurrentPlaylist(int pos){
       mList.remove(pos);
    }
}
