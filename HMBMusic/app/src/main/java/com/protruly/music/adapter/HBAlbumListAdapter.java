package com.protruly.music.adapter;

import android.app.Activity;
import android.content.res.Resources;
import android.database.CharArrayBuffer;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.ui.album.AlbumListActivity;
import com.protruly.music.util.HBAlbum;
import com.protruly.music.util.HBListItem;

import hb.preference.CheckBoxPreference;
import hb.widget.HbListView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by hujianwei on 17-9-1.
 */

public class HBAlbumListAdapter extends BaseAdapter{
    private ArrayList<HBAlbum> mList;
    LayoutInflater mInflater;
    private boolean mNeedin = false;
    private HashMap<Long, Boolean> mCheckedMap = new HashMap<Long, Boolean>();
    private Activity mActivity;
    private static final int MSG_NEED_IN = 200001;
    private static final int MSG_NEED_OUT = 200002;
    private boolean mNeedout = false;
    private boolean editMode = false;
    DisplayImageOptions mOptions;

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    private static int mCurrentPosition = -1;
    private static final int WAIT_TIME = 50;
    private HashMap<Integer, Integer> mTagMap = new HashMap<Integer, Integer>();
    private boolean deleteAction = false;

    public boolean isDeleteAction() {
        return deleteAction;
    }

    public void setDeleteAction(boolean deleteAction) {
        this.deleteAction = deleteAction;
    }

    private static final String IMAGE_CACHE_DIR = "HBAlbum";
    private String mUnknownAlbum;
    private Resources mResources;
    private BitmapDrawable mDefaultAlbumIcon;
    private String mUnknown;

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_NEED_IN:
                    mNeedin = false;
                    break;
                case MSG_NEED_OUT:
                    mNeedout = false;
                    break;
                default:
                    break;
            }
        };
    };

    static class ViewHolder {

        // 专辑名
        TextView line1;
        // 多少歌
        TextView line2;

        // 发行时间
        TextView line3;

        // 播放按钮
        ImageView play_ic;
        ImageView icon;
        RelativeLayout front;
        CheckBox cb;
        CharArrayBuffer buffer1;
        char[] buffer2;

        public ViewHolder(View v) {
            this.line1 = (TextView) v.findViewById(R.id.album_name);
            this.line2 = (TextView) v.findViewById(R.id.album_numtrack);
            this.line3 = (TextView) v.findViewById(R.id.album_release_date);
            this.play_ic = (ImageView) v.findViewById(R.id.play_now);
            this.icon = (ImageView) v.findViewById(R.id.album_art);
            //this.front = (RelativeLayout) v.findViewById(com.hb.R.id.hb_listview_front);
            //this.cb = (CheckBox) v.findViewById(com.hb.R.id.hb_list_left_checkbox);
        }
    }

    public HBAlbumListAdapter(Activity activity, ArrayList<HBAlbum> list) {
        mActivity = activity;
        mList = list;
        mInflater = LayoutInflater.from(activity);

        mResources = mActivity.getResources();
        mUnknownAlbum = mResources.getString(R.string.unknown_album_name);
        mUnknown = mResources.getString(R.string.unknown_time);

        Bitmap b = BitmapFactory.decodeResource(mResources, R.drawable.album_art_default);
        mDefaultAlbumIcon = new BitmapDrawable(mActivity.getResources(), b);
        mDefaultAlbumIcon.setFilterBitmap(false);
        mDefaultAlbumIcon.setDither(false);
        if (list != null) {
            for (int index = 0; index < list.size(); index++) {
                mCheckedMap.put(list.get(index).getAlbumId(), false);
            }
        }
        initImageCacheParams();
    }

    @Override
    public int getCount() {
        return (mList == null) ? 0 : mList.size();
    }

    @Override
    public HBAlbum getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int index, View converView, ViewGroup parent) {
        final ViewHolder vh;
        final int position = index;
        final long albumId = mList.get(position).getAlbumId();
        if (converView == null) {
            //converView = mInflater.inflate(com.hb.R.layout.hb_slid_listview, null);
            //RelativeLayout front = (RelativeLayout) converView.findViewById(com.hb.R.id.hb_listview_front);
            //mInflater.inflate(R.layout.hb_album_list_item, front);
            vh = new ViewHolder(converView);
            converView.setTag(vh);
        } else {
            vh = (ViewHolder) converView.getTag();
        }
        //RelativeLayout rl_control_padding = (RelativeLayout) converView.findViewById(com.hb.R.id.control_padding);//DisplayUtil.dip2px(mActivity, 12)
        //rl_control_padding.setPadding(0, 0, 0, 0);//(int) mActivity.getResources().getDimension(com.hb.R.dimen.checkbox_margin_right_in_listview_with_index)
//		RelativeLayout.LayoutParams rlp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
//		rlp.addRule(RelativeLayout.CENTER_VERTICAL);
//		vh.cb.setLayoutParams(rlp);
        vh.buffer1 = new CharArrayBuffer(100);
        vh.buffer2 = new char[200];
        String name = mList.get(position).getAlbumName();
        boolean unknown = name == null || name.equals(MediaStore.UNKNOWN_STRING);
        if (unknown) {
            name = mUnknownAlbum;
        }
        vh.line1.setText(name);
        //vh.line2.setText(mResources.getString(R.string.num_songs_of_album, mList.get(position).getTrackNumber()));

        String releaseDate = mList.get(position).getReleaseDate();
        if (releaseDate == null || releaseDate.equals(MediaStore.UNKNOWN_STRING)) {
            releaseDate = mUnknown;
            vh.line3.setVisibility(View.GONE);
        }
        vh.line3.setText(mResources.getString(R.string.release_date_of_album, releaseDate));

        String art = mList.get(position).getAlbumArt();
        final long aid = mList.get(position).getAlbumId();
        if (unknown || art == null || art.length() == 0) {
            vh.icon.setImageDrawable(null);
        } else {
            if (MusicUtils.hasArtwork(mActivity, aid)) {
                Drawable d = MusicUtils.getCachedArtwork(mActivity, aid, mDefaultAlbumIcon);
                vh.icon.setImageDrawable(d);
                vh.icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                if (mActivity instanceof AlbumListActivity) {
                    HBListItem item = MusicUtils.getFirstSongOfAlbum(mActivity.getApplicationContext(), aid, ((AlbumListActivity) mActivity).getArtistId());
                    if (item != null)
                        ImageLoader.getInstance().displayImage(item.getAlbumImgUri(), item.getSongId(), vh.icon, mOptions, null, null);
                }
            }
        }



        if (mNeedin) {
            //HbListView.hbStartCheckBoxAppearingAnim(vh.front, vh.cb);
        } else if (!mNeedin && isEditMode()) {
            //HbListView.hbSetCheckBoxVisible(vh.front, vh.cb, true);
        }
        if (mNeedout) {
            //HbListView.hbStartCheckBoxDisappearingAnim(vh.front, vh.cb);
        } else if (!isEditMode() && !mNeedout) {
            //HbListView.hbSetCheckBoxVisible(vh.front, vh.cb, false);
        }
        if (isEditMode()) {
            vh.line1.setEnabled(false);
            vh.line2.setEnabled(false);
            vh.line3.setEnabled(false);
            vh.play_ic.setVisibility(View.GONE);
        } else {
            vh.line1.setEnabled(true);
            vh.line2.setEnabled(true);
            vh.line3.setEnabled(true);
            vh.play_ic.setVisibility(View.GONE);
        }

        Boolean checked = mCheckedMap.get(albumId);
        if (checked != null && checked && isEditMode()) {
            //vh.cb.hbSetChecked(true, true);
        } else {
            //vh.cb.hbSetChecked(false, true);
        }

        if (isDeleteAction()) {
            ViewGroup.LayoutParams vl = converView.getLayoutParams();
            if (null != vl) {
                vl.height = mActivity.getResources().getDimensionPixelSize(R.dimen.album_item_height);
            }
            converView.findViewById(com.hb.R.id.content).setAlpha(255);
        }
        // mImageResizer.loadImage(mList.get(arg0), vh.icon);
        // ImageLoader.getInstance().displayImage(mList.get(position).getAlbumImgUri(),mList.get(position).getSongId(),
        // vh.icon, mOptions, null, null);
        return converView;
    }

    public void setNeedin(int position) {
        mNeedin = true;
        selectAllNot();
        mCheckedMap.put(mList.get(position).getAlbumId(), true);
        mHandler.sendEmptyMessageDelayed(MSG_NEED_IN, WAIT_TIME);
    }

    public void setSelected(int position) {
        mCurrentPosition = position;
    }

    public void setNeedout() {
        mNeedout = true;
        mHandler.sendEmptyMessageDelayed(MSG_NEED_OUT, WAIT_TIME);
    }

    public void selectAll() {
        mCheckedMap.clear();
        for (int position = 0; position < getCount(); position++) {
            mCheckedMap.put(mList.get(position).getAlbumId(), true);
        }
    }

    public void selectAllNot() {
        for (int position = 0; position < getCount(); position++) {
            mCheckedMap.put(mList.get(position).getAlbumId(), false);
        }
    }

    public int getCheckedCount() {
        int count = 0;
        for (int position = 0; position < getCount(); position++) {
            long aid = mList.get(position).getAlbumId();
            if (mCheckedMap.get(aid)) {
                count++;
            }
        }
        return count;
    }

    public boolean getCheckedArrayValue(int position) {
        return mCheckedMap.get(mList.get(position).getAlbumId());
    }

    public void setCheckedArrayValue(int position, boolean isChecked) {
        mCheckedMap.put(mList.get(position).getAlbumId(), isChecked);
    }

    public HashMap<Long, Boolean> getCheckedMap() {
        return mCheckedMap;
    }


    private void initImageCacheParams() {
        mOptions = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.default_music_icon2).showImageForEmptyUri(R.drawable.default_music_icon2)
                .showImageOnFail(R.drawable.default_music_icon2).cacheInMemory(true).cacheOnDisk(true).considerExifParams(true).displayer(new SimpleBitmapDisplayer()).build();
    }

}
