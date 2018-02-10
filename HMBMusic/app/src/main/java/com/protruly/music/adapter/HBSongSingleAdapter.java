package com.protruly.music.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.ui.HBSongSingle;
import com.protruly.music.util.DialogUtil.OnDeleteFileListener;
import com.protruly.music.util.DialogUtil.OnRemoveFileListener;
import com.protruly.music.util.HBListItem;
import com.protruly.music.util.HBMusicUtil;
import com.protruly.music.util.LogUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import hb.widget.HbListView;

/**
 * Created by hujianwei on 17-9-4.
 */

public class HBSongSingleAdapter  extends BaseAdapter implements OnDeleteFileListener,OnRemoveFileListener{

    private static final String TAG = "HBSongSingleAdapter";
    private List<HBListItem> mData = new ArrayList<HBListItem>();
    private SparseBooleanArray mCheckedMap = new SparseBooleanArray();

    private LayoutInflater mInflater;
    private static String mArtistName;
    private static String mAlbumName;
    private boolean isEditMode = false;
    private boolean mNeedin = false, mNeedout = false;
    private Handler mHandler = new Handler();
    private Handler activityHandler = null;
    private static final String IMAGE_CACHE_DIR = "HBAlbum";
    private Context mContext;
    
    // 点击播放位置
    private int mCurrentPosition = -2;
    public boolean hasDeleted = false;
    private int playListId;
    private boolean isHideSelect = false;
    private boolean isShowText = false;
    private DisplayImageOptions disoptions;
    private Bitmap defaultBitmap=null;

    public HBSongSingleAdapter(Context context, List<HBListItem> list) {
        this(context);
        mData = list;
    }

    public HBSongSingleAdapter(Context context, int playlistid, int startmode) {
        this(context);
        playListId = playlistid;
        if (startmode != 1) {
            isShowText = true;
        } else {
            isShowText = false;
        }
    }

    public HBSongSingleAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        mArtistName = context.getString(R.string.unknown);
        mAlbumName = context.getString(R.string.unknown_album_name);
        mContext = context;
        initImageCacheParams();
    }

    public void setSonglist(ArrayList<HBListItem> list) {
        if (mData == null) {
            mData = new ArrayList<HBListItem>();
        }

        if (list == null) {
            return;
        }
        mData = list;
        notifyDataSetChanged();
    }

    public void deleteItem(int position) {
        if (position < 0 || position >= mData.size()) {
            LogUtil.d(TAG, "delete position error:" + position);
            return;
        }

        // 刚好删除正在播放的歌曲，将播放位置置空
        if (mCurrentPosition == position) {
            mCurrentPosition = -2;
        } else if (mCurrentPosition > position) {
            mCurrentPosition--;
        }
        mData.remove(position);
        notifyDataSetChanged();
    }

    public void setHandler(Handler handler) {
        activityHandler = handler;
    }

    public void setNeedIn() {
        mNeedin = true;
        isEditMode = true;
        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                mNeedin = false;
            }
        }, 50);
    }

    public void setNeedIn(int position) {
        mNeedin = true;
        isEditMode = true;
        mCheckedMap.put(position, true);
        notifyDataSetChanged();
        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                mNeedin = false;
            }
        }, 50);
    }

    public void setNeedOut() {
        mNeedout = true;
        isEditMode = false;
        mCheckedMap.clear();
        notifyDataSetChanged();
        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                mNeedout = false;
            }
        }, 50);
    }

    /**
     * 全选
     */
    public void selectAll() {

        for (int i = 0; i < mData.size(); i++) {
            mCheckedMap.put(i, true);
        }
        notifyDataSetChanged();
    }

    /**
     * 反选
     */
    public void selectAllNot() {
        mCheckedMap.clear();
        notifyDataSetChanged();
    }

    /**
     * 选择个数
     * @return
     */
    public int getCheckedCount() {
        return mCheckedMap.size();
    }

    /**
     * 设置选项
     * @param position
     */
    public void setSelected(int position) {
        mCurrentPosition = position;
        if (position > -2) {
            isHideSelect = false;
        }
    }

    public void setNotSelect() {
        isHideSelect = true;
    }

    public List<String> getCheckedId() {
        List<String> list = new ArrayList<String>();
        Iterator<HBListItem> it = mData.iterator();
        int i = 0;
        while (it.hasNext()) {
            HBListItem info = it.next();
            Boolean checked = mCheckedMap.get(i);
            if (checked != null && checked) {
                list.add(String.valueOf(info.getSongId()));
            }
            i++;
        }
        return list;
    }

    private int count;
    public void deletesongs(int startMode) {

        Iterator<HBListItem> it = mData.iterator();
        int i = 0, j = 0;
        count = 0;
        long[] list = new long[mCheckedMap.size()];
        while (it.hasNext()) {
            HBListItem info = it.next();
            Boolean checked = mCheckedMap.get(i);
            if (checked != null && checked) {

                list[j] = info.getSongId();
                j++;
                // 刚好删除正在播放的歌曲，将播放位置置空
                if (mCurrentPosition == i) {
                    mCurrentPosition = -2;
                } else if (mCurrentPosition > i) {
                    count++;
                }
                it.remove();

            }
            i++;
        }
        if (startMode == 0) {
            MusicUtils.removeMediaTracks(mContext, list, this,playListId);
            LogUtil.d(TAG, "HB_ID_DELETE_SONGS removeMediaTracks 1");
        } else if (startMode == 2) {
            MusicUtils.removeTracksFromCurrentPlaylist(mContext, list, playListId);
            MusicUtils.mSongDb.deleteFavoritesById(list);
            HBMusicUtil.showDeleteToast(mContext, mCheckedMap.size(), startMode);
            mCurrentPosition -= count;
            mCheckedMap.clear();
            notifyDataSetChanged();
            LogUtil.d(TAG, "HB_ID_DELETE_SONGS deletesongs 2");
        } else {
            MusicUtils.deleteMediaTracks(mContext, list, this);
            LogUtil.d(TAG, "HB_ID_DELETE_SONGS deletesongs 3");
        }
        LogUtil.d(TAG, "HB_ID_DELETE_SONGS deletesongs startMode:" + startMode);
    }

    @Override
    public void OnRemoveFileSuccess() {
        mCurrentPosition -= count;
        mCheckedMap.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int arg0) {

        return mData.get(arg0);
    }

    @Override
    public long getItemId(int arg0) {

        return arg0;
    }

    @Override
    public View getView(int arg0, View arg1, ViewGroup arg2) {

        final HoldView holdView;
        if (arg1 == null) {
            holdView = new HoldView();
            //arg1 = mInflater.inflate(com.hb.R.layout.hb_slid_listview, null);
            //View myView = mInflater.inflate(R.layout.hb_songsingle_item,
            // null);
            // 获取添加内容的对象
            //RelativeLayout mainUi = (RelativeLayout) arg1.findViewById(com.hb.R.id.hb_listview_front);
            //mInflater.inflate(R.layout.hb_songsingle_item, mainUi);
            // 将要显示的内容添加到mainUi中去
            // mainUi.addView(myView, 0, new LayoutParams(
            // LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            // 设置间距
//            RelativeLayout rl_control_padding = (RelativeLayout) arg1.findViewById(com.hb.R.id.control_padding);
//            rl_control_padding.setPadding(0, 0, 0, 0);
//
//            holdView.icon = (ImageView) mainUi.findViewById(R.id.song_playicon);
//            holdView.songName = (TextView) mainUi.findViewById(R.id.song_title);
//            holdView.songAlbum = (TextView) mainUi.findViewById(R.id.song_artist);
//            holdView.songPlay = (ImageView) mainUi.findViewById(R.id.iv_song_selected);
//            holdView.front = mainUi;
//            holdView.cb = (CheckBox) arg1.findViewById(com.hb.R.id.hb_list_left_checkbox);
//			RelativeLayout.LayoutParams rlp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
//			rlp.addRule(RelativeLayout.CENTER_VERTICAL);
//			rlp.setMargins(DisplayUtil.dip2px(mContext, 13), 0, 0, 0);
//			((View) holdView.cb).setLayoutParams(rlp);

            // 垃圾桶层
            if (isShowText) {
//                LinearLayout listBack = (LinearLayout) arg1.findViewById(com.hb.R.id.hb_listview_back);
//                TextView rubbish = new TextView(mContext);
//                rubbish.setGravity(Gravity.CENTER);
//                rubbish.setTextColor(Color.parseColor("#ffffff"));
//                rubbish.setText(mContext.getString(R.string.hb_remove));
//                listBack.addView(rubbish, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//                View rubishView = arg1.findViewById(com.hb.R.id.hb_rubbish);
//                if (rubishView != null) {
//                    rubishView.setVisibility(View.GONE);
//                }
            }
            holdView.contentView = arg1.findViewById(R.id.hb_item_content);
            arg1.setTag(holdView);
        } else {
            holdView = (HoldView) arg1.getTag();
        }

        if (!isEditMode) {
            arg1.setBackgroundResource(R.drawable.hb_playlist_item_clicked);
        } else {
            arg1.setBackgroundResource(android.R.color.transparent);
        }

        // 操作复选框
        if (mNeedin) {
            //HbListView.hbStartCheckBoxAppearingAnim(holdView.front, holdView.cb);
        } else if (!mNeedin && isEditMode) {
            //HbListView.hbSetCheckBoxVisible(holdView.front, holdView.cb, true);
        }

        if (mNeedout) {
            //HbListView.hbStartCheckBoxDisappearingAnim(holdView.front, holdView.cb);
        } else if (!isEditMode && !mNeedout) {
            //HbListView.hbSetCheckBoxVisible(holdView.front, holdView.cb, false);
        }

        if (hasDeleted) {
            ViewGroup.LayoutParams vl = arg1.getLayoutParams();
            if (null != vl) {
                vl.height = mContext.getResources().getDimensionPixelSize(R.dimen.hb_song_item_height);
            }
            //arg1.findViewById(com.hb.R.id.content).setAlpha(255);
            //arg1.setAlpha(255);
        }

        if (isEditMode) {
            final int position = arg0;
            // arg1.setClickable(true);
            final CheckBox checkbox = (CheckBox) holdView.cb;
            // checkbox.setClickable(true);
            arg1.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {

                    if (!checkbox.isChecked()) {
                        //checkbox.setChecked(true, true);
                        mCheckedMap.put(position, true);
                    } else {
                        //checkbox.setChecked(false, true);
                        mCheckedMap.delete(position);

                    }
                    if (activityHandler != null) {
                        activityHandler.obtainMessage(1).sendToTarget();
                    }
                }
            });
            Boolean checked = mCheckedMap.get(position);
            if (checked != null && checked) {
                ((CheckBox) holdView.cb).setChecked(true);
            } else {
                ((CheckBox) holdView.cb).setChecked(false);
            }

            holdView.songName.setTextColor(mContext.getResources().getColor(R.color.black));
            holdView.songAlbum.setTextColor(mContext.getResources().getColor(R.color.hb_item_song_size));
        } else {
            holdView.songName.setTextColor(mContext.getResources().getColorStateList(R.color.black));
            holdView.songAlbum.setTextColor(mContext.getResources().getColorStateList(R.color.hb_item_song_size));
            arg1.setClickable(false);
        }

        final HBListItem info = mData.get(arg0);
        if (info != null) {
            if (info.getSongId() == MusicUtils.getCurrentAudioId() && !isEditMode) {
                if (isHideSelect) {
                    holdView.songPlay.setVisibility(View.INVISIBLE);
                } else {
                    holdView.songPlay.setVisibility(View.VISIBLE);
                    mCurrentPosition = arg0;
                }

            } else {
                holdView.songPlay.setVisibility(View.INVISIBLE);
            }
            holdView.songName.setText(info.getTitle());

            StringBuffer tBuffer = new StringBuffer();
            String artiststr = info.getArtistName();
            if (artiststr == null || MediaStore.UNKNOWN_STRING.equals(artiststr)) {
                artiststr = mArtistName;
            }

            String albumstr = info.getAlbumName();
            albumstr = HBMusicUtil.doAlbumName(info.getFilePath(), albumstr);
            if (albumstr == null || MediaStore.UNKNOWN_STRING.equals(albumstr)) {
                albumstr = mAlbumName;
            }
            if (artiststr.length() > 12) {
                artiststr = artiststr.substring(0, 12) + "…";
            }
            tBuffer.append(artiststr);
            tBuffer.append("·");
            tBuffer.append(albumstr);
            holdView.songAlbum.setText(tBuffer.toString());

            ImageLoader.getInstance().displayImage(info.getAlbumImgUri(), info.getSongId(), holdView.icon, disoptions, null, null);

        }
        return arg1;
    }

    class HoldView {
        ImageView icon;
        TextView songName;
        TextView songAlbum;
        ImageView songPlay;
        RelativeLayout front;
        CheckBox cb;
        View contentView;
    }

    private void initImageCacheParams() {
        if(defaultBitmap==null){
            defaultBitmap= BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.default_music_icon);
        }
        disoptions = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.default_music_icon).showImageForEmptyUri(R.drawable.default_music_icon)
                .showImageOnFail(R.drawable.default_music_icon).cacheInMemory(true).cacheOnDisk(true).considerExifParams(true).displayer(new SimpleBitmapDisplayer()).build();
    }

    public int getCurrentPlayPosition() {
        return mCurrentPosition + 1;
    }

    @Override
    public void OnDeleteFileSuccess() {
        // mCurrentPosition -= mCount;
        mCheckedMap.clear();
        notifyDataSetChanged();
        ((HBSongSingle) mContext).updateTitleSizeAndExitMode();
    }

    public boolean ischeckedOnline() {

        for (int i = 0; i < mData.size(); i++) {
            if (mCheckedMap == null) {
                return false;
            }
            Boolean checked = mCheckedMap.get(i);
            if (checked != null && checked) {
                HBListItem item = mData.get(i);
                if (item.getIsDownLoadType() == 1) {
                    return true;
                }
            }
        }
        return false;
    }
}
