package com.protruly.music.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.protruly.music.R;
import com.protruly.music.model.HBCollectPlaylist;
import com.protruly.music.util.HBMusicUtil;
import com.protruly.music.util.LogUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hujianwei on 17-9-4.
 */

public class OnlineRankListAdapter  extends BaseAdapter {

    private static final String TAG = "OnlineRankListAdapter";
    private LayoutInflater mInflater;
    private List<HBCollectPlaylist> datas = new ArrayList<HBCollectPlaylist>();
    private int mIconWidth;
    private int mIconHeight;
    private Context mContext;
    private DisplayImageOptions options;

    public OnlineRankListAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mIconWidth = (int) context.getResources().getDimension(
                R.dimen.hb_album_cover_width);
        mIconHeight = (int) context.getResources().getDimension(
                R.dimen.hb_album_cover_height);
        initImageCacheParams();
    }

    public void addDatas(List<HBCollectPlaylist> list) {
        if (list == null) {
            return;
        }
        datas = list;
        notifyDataSetChanged();
    }

    public void deleteItem(int position){
        datas.remove(position);
        notifyDataSetChanged();
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
        if (arg1 == null) {
            holdView = new HoldView();
//            arg1 = mInflater.inflate(com.hb.R.layout.hb_slid_listview,
//                    null);
//            RelativeLayout front = (RelativeLayout) arg1
//                    .findViewById(com.hb.R.id.hb_listview_front);
//            mInflater.inflate(R.layout.hb_album_list_item, front);
//            RelativeLayout rl_control_padding = (RelativeLayout) arg1
//                    .findViewById(com.hb.R.id.control_padding);
//            rl_control_padding.setPadding(36, 0, 0, 0);
//            holdView.icon = (ImageView) arg1.findViewById(R.id.album_art);
//            holdView.rankname = (TextView) arg1.findViewById(R.id.album_name);
//            holdView.songsize = (TextView) arg1
//                    .findViewById(R.id.album_numtrack);
//            holdView.updatetime = (TextView) arg1
//                    .findViewById(R.id.album_release_date);
//            arg1.findViewById(R.id.play_now).setVisibility(View.GONE);
//            arg1.setTag(holdView);
        } else {
            holdView = (HoldView) arg1.getTag();
        }

        ViewGroup.LayoutParams vl = arg1.getLayoutParams();
        if (null != vl) {
            vl.height = mContext.getResources().getDimensionPixelSize(R.dimen.hb_album_item_height);
        }
//        arg1.findViewById(com.hb.R.id.content).setAlpha(255);
//        arg1.setAlpha(255);

        HBCollectPlaylist info = datas.get(arg0);
        if (info != null) {
            holdView.rankname.setText(info.getPlaylistname());
//            holdView.songsize.setText(mContext.getString(
//                    R.string.num_songs_of_album, info.getSongSize()));
            if (info.getListType() == 2 || info.getListType() == 3) {
                holdView.rankname.setSingleLine(true);
                holdView.updatetime.setVisibility(View.VISIBLE);
                holdView.songsize.setPadding(0, 0, 0, mContext.getResources().getDimensionPixelOffset(R.dimen.hb_album_numsongs_padding_bottom));
                holdView.updatetime.setText(HBMusicUtil.formatCurrentTime(
                        mContext, info.getInfo()));
            }else if(info.getListType() == 1){
                holdView.rankname.setSingleLine(false);
                holdView.rankname.setMaxLines(2);
                holdView.songsize.setPadding(0, 0, 0, 0);
                holdView.updatetime.setVisibility(View.GONE);
            } else {
                holdView.rankname.setSingleLine(true);
                holdView.songsize.setPadding(0, 0, 0, mContext.getResources().getDimensionPixelOffset(R.dimen.hb_album_numsongs_padding_bottom));
                holdView.updatetime.setVisibility(View.VISIBLE);
                holdView.updatetime.setText(info.getInfo());
            }
            // 
            LogUtil.d(TAG, "HBMusicUtilgetPlaylistid--"+info.getPlaylistid());
            if(Integer.valueOf(info.getPlaylistid())==0){
                holdView.icon.setImageResource(R.drawable.hb_xiami_list);
            }else if(Integer.valueOf(info.getPlaylistid())==5){
                holdView.icon.setImageResource(R.drawable.hb_xiami_new_list);
            }else if(Integer.valueOf(info.getPlaylistid())==16){
                holdView.icon.setImageResource(R.drawable.hb_xiami_local_list);
            }else if(Integer.valueOf(info.getPlaylistid())==13){
                holdView.icon.setImageResource(R.drawable.hb_biboard);
            }else {
                ImageLoader.getInstance().displayImage(info.getImgUrl(), holdView.icon,options);
            }
        }
        return arg1;
    }

    class HoldView {
        ImageView icon;
        TextView rankname;
        TextView songsize;
        TextView updatetime;
    }

    private void initImageCacheParams(){
        options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.hb_online_music_defualt_item_bg)
                .showImageForEmptyUri(R.drawable.hb_online_music_defualt_item_bg)
                .showImageOnFail(R.drawable.hb_online_music_defualt_item_bg)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .displayer(new SimpleBitmapDisplayer())
                .build();
    }
}
