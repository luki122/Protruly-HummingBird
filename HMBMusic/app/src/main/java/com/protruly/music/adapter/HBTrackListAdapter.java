package com.protruly.music.adapter;

import android.content.res.Resources;
import android.database.CharArrayBuffer;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
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
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.ui.album.AlbumContent;
import com.protruly.music.ui.album.AlbumDetailActivity;
import com.protruly.music.util.HBAlbum;
import com.protruly.music.util.HBListItem;
import com.protruly.music.util.HBMusicUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import android.widget.CheckBox;
import hb.app.HbActivity;
import hb.widget.HbListView;

/**
 * Created by hujianwei on 17-9-1.
 */

public class HBTrackListAdapter extends BaseAdapter {
    private ArrayList<HBListItem> mList;
    private boolean mNeedin = false;
    private boolean mHeaderNeedin = false;
    private HashMap<Long, Boolean> mCheckedMap = new HashMap<Long, Boolean>();
    private HashMap<Long, Boolean> mAlbumCheckedMap = new HashMap<Long, Boolean>();
    private HbActivity mActivity;
    private static final int MSG_NEED_IN = 200001;
    private static final int MSG_NEED_OUT = 200002;
    private boolean mNeedout = false;
    private boolean mEditMode = false;
    private long currentTrackId;
    private static final int WAIT_TIME = 50;
    private HashMap<Long, HBAlbum> mAlbumMap = new HashMap<Long, HBAlbum>();

    private static final String IMAGE_CACHE_DIR = "HBAlbum";
    LayoutInflater inflater;
    int headerCount = 0;
    boolean mSectionHeaderDisplayEnabled = true;
    DisplayImageOptions mOptions;

    public boolean isSectionHeaderDisplayEnabled() {
        return mSectionHeaderDisplayEnabled;
    }

    public void setSectionHeaderDisplayEnabled(boolean flag) {
        this.mSectionHeaderDisplayEnabled = flag;
    }

    private boolean deleteAction = false;

    public boolean isDeleteAction() {
        return deleteAction;
    }

    public void setDeleteAction(boolean deleteAction) {
        this.deleteAction = deleteAction;
    }

    public long getCurrentTrackId() {
        return currentTrackId;
    }

    public void setCurrentTrackId(long currentTrackId) {
        this.currentTrackId = currentTrackId;
    }

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_NEED_IN:
                    mNeedin = false;
                    mHeaderNeedin = false;
                    break;
                case MSG_NEED_OUT:
                    mNeedout = false;
                    break;
                default:
                    break;
            }
        };
    };
    private Resources mResources;
    private String mUnknown;
    private String mUnknownAlbum;
    private BitmapDrawable mDefaultAlbumIcon;
    private int mCurrentPosition = -1;
    private HbListView lv;

    static class ViewHolder {
        TextView trackName;// 歌名
        TextView duration;// 时长
        View header;// 分类
        ImageView play_indicator;// 正在播放标识
        CharArrayBuffer buffer1;
        char[] buffer2;
        LinearLayout content;
        RelativeLayout front;
        CheckBox cb;
        CheckBox headerCheckBox;
        RelativeLayout albumInfo;

        public ViewHolder(View convertView) {
            this.trackName = (TextView) convertView.findViewById(R.id.line1);
            this.duration = (TextView) convertView.findViewById(R.id.duration);
            this.play_indicator = (ImageView) convertView.findViewById(R.id.song_playicon);
            this.content = (LinearLayout) convertView.findViewById(com.hb.R.id.content);
            //this.front = (RelativeLayout) convertView.findViewById(com.hb.R.id.hb_listview_front);
            //this.header = (LinearLayout) convertView.findViewById(com.hb.R.id.hb_list_header);
            //this.cb = (CheckBox) convertView.findViewById(com.hb.R.id.hb_list_left_checkbox);
            this.buffer1 = new CharArrayBuffer(100);
            this.buffer2 = new char[200];

            this.headerCheckBox = (CheckBox) convertView.findViewById(R.id.header_checkbox);
            this.albumInfo = (RelativeLayout) convertView.findViewById(R.id.album_info);

        }
    }

    public HBTrackListAdapter(HbActivity activity, ArrayList<HBListItem> list, ArrayList<HBAlbum> albums) {
        mActivity = activity;
        mList = list;
        inflater = mActivity.getLayoutInflater();
        mResources = mActivity.getResources();
        mUnknownAlbum = mResources.getString(R.string.unknown_album_name);
        mUnknown = mResources.getString(R.string.unknown_time);

        Bitmap b = BitmapFactory.decodeResource(mResources, R.drawable.album_art_default);
        mDefaultAlbumIcon = new BitmapDrawable(mActivity.getResources(), b);
        mDefaultAlbumIcon.setFilterBitmap(false);
        mDefaultAlbumIcon.setDither(false);

        initImageCacheParams();

        if (albums != null) {
            mSectionHeaderDisplayEnabled = true;
            mAlbumMap = AlbumContent.ITEM_MAP;
            for (int index = 0; index < albums.size(); index++) {
                mAlbumCheckedMap.put(albums.get(index).getAlbumId(), false);
            }
        } else {
            mSectionHeaderDisplayEnabled = false;
            if (mList != null && !mList.isEmpty()) {
                mAlbumCheckedMap.put(mList.get(0).getAlbumId(), false);
            }
        }

        if (list != null) {
            for (int index = 0; index < list.size(); index++) {
                mCheckedMap.put(list.get(index).getSongId(), false);
            }
        }

    }

    public HBTrackListAdapter(HbActivity activity, ArrayList<HBListItem> list) {
        this(activity, list, null);
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        if (mList != null)
            return mList.size();
        else
            return 0;
    }

    @Override
    public HBListItem getItem(int position) {
        // TODO Auto-generated method stub
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int arg0, View convertView, ViewGroup arg2) {
        final ViewHolder vh;
        final int position = arg0;
        final HBListItem item = mList.get(arg0);

        if (convertView == null) {
            //convertView = LayoutInflater.from(mActivity).inflate(com.hb.R.layout.hb_slid_listview, null);
            //RelativeLayout frontView = (RelativeLayout) convertView.findViewById(com.hb.R.id.hb_listview_front);
            //LinearLayout headView = (LinearLayout) convertView.findViewById(com.hb.R.id.hb_list_header);
            //LayoutInflater.from(mActivity).inflate(R.layout.hb_track_list_item, frontView);
            //LayoutInflater.from(mActivity).inflate(R.layout.hb_album_list_item_with_checkbox, headView);
            vh = new ViewHolder(convertView);
           // RelativeLayout rl_control_padding = (RelativeLayout) convertView.findViewById(com.hb.R.id.control_padding);
            //rl_control_padding.setPadding(0, 0, 0, 0);
//			RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
//			rlp.addRule(RelativeLayout.CENTER_VERTICAL);
//			rlp.setMargins(DisplayUtil.dip2px(mActivity, 12), 0, 0, 0);
//			((View) vh.cb).setLayoutParams(rlp);
            convertView.setTag(vh);
            ((View) vh.cb).setClickable(false);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }

        if (item == null) {
            return convertView;
        }
        String title = item.getTitle();
        if (title == null || title.isEmpty() || title.equals(MediaStore.UNKNOWN_STRING)) {
            title = mUnknown;
        }
        vh.trackName.setText(title);
        int secs = item.getDuration() / 1000;
        if (secs == 0) {
            vh.duration.setText("");
        } else {
            vh.duration.setText(MusicUtils.makeTimeString(mActivity, secs));
        }
        final long section = getSectionForPosition(arg0);
        if (mNeedin) {
            if (isSectionHeaderDisplayEnabled()) {
                //HbListView.hbStartCheckBoxAppearingAnim(vh.albumInfo, vh.headerCheckBox);
            }
            //HbListView.hbStartCheckBoxAppearingAnim(vh.front, vh.cb);
        } else if (!mNeedin && mEditMode) {
            if (isSectionHeaderDisplayEnabled()) {
                //HbListView.hbSetCheckBoxVisible(vh.albumInfo, vh.headerCheckBox, true);
            }
            //HbListView.hbSetCheckBoxVisible(vh.front, vh.cb, true);
        }
        if (mNeedout) {
            if (isSectionHeaderDisplayEnabled()) {
                //HbListView.hbStartCheckBoxDisappearingAnim(vh.albumInfo, vh.headerCheckBox);
            }
            //HbListView.hbStartCheckBoxDisappearingAnim(vh.front, vh.cb);
        } else if (!mEditMode && !mNeedout) {
            if (isSectionHeaderDisplayEnabled()) {
                //HbListView.hbSetCheckBoxVisible(vh.albumInfo, vh.headerCheckBox, false);
            }
            //HbListView.hbSetCheckBoxVisible(vh.front, vh.cb, false);
        }
        if (isEditMode()) {

            vh.content.setClickable(true);
            vh.content.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (((CheckBox) vh.cb).isChecked()) {
                        ((CheckBox) vh.cb).setChecked( false);
                        setCheckedArrayValue(position, false);
                        mAlbumCheckedMap.put(section, false);
                        notifyDataSetChanged();
                        ((AlbumDetailActivity) mActivity).changeMenuState();
                    } else {

                        ((CheckBox) vh.cb).setChecked(true);
                        setCheckedArrayValue(position, true);
                        if (isAlbumAllChecked(section)) {
                            mAlbumCheckedMap.put(section, true);
                            notifyDataSetChanged();
                        }
                        ((AlbumDetailActivity) mActivity).changeMenuState();
                    }
                }
            });
            vh.content.setBackgroundResource(android.R.color.transparent);
        } else {
            vh.content.setClickable(false);
            vh.content.setBackgroundResource(R.drawable.hb_playlist_item_clicked);
        }

        if (isSectionHeaderDisplayEnabled()) {

            if (arg0 == getPositionForSection(section)) {
                long c = mList.get(position).getAlbumId();
                HBAlbum album = mAlbumMap.get(c);
                String name, releaseDate, art;
                boolean unknown;
                if (album == null) {
                    name = mUnknownAlbum;
                    releaseDate = mUnknown;
                    art = null;
                    unknown = true;
                } else {
                    name = album.getAlbumName();
                    name = HBMusicUtil.doAlbumName(album.getUri(), name);
                    unknown = name == null || name.equals(MediaStore.UNKNOWN_STRING);
                    if (unknown) {
                        name = mUnknownAlbum;
                    }
                    releaseDate = album.getReleaseDate();
                    if (releaseDate == null || releaseDate.equals(MediaStore.UNKNOWN_STRING)) {
                        releaseDate = mUnknown;
                        vh.header.findViewById(R.id.album_release_date).setVisibility(View.GONE);
                    }
                    art = album.getAlbumArt();
                }

                ImageView ivAlbumArt = (ImageView) vh.header.findViewById(R.id.album_art);

                final long aid = mList.get(position).getAlbumId();
                if (unknown || art == null || art.length() == 0) {
                    ivAlbumArt.setImageDrawable(null);
                } else {
                    if (MusicUtils.hasArtwork(mActivity, aid)) {
                        Drawable d = MusicUtils.getCachedArtwork(mActivity, aid, mDefaultAlbumIcon);
                        ivAlbumArt.setImageDrawable(d);
                        ivAlbumArt.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    } else {

                        ImageLoader.getInstance().displayImage(item.getAlbumImgUri(), item.getSongId(), ivAlbumArt, mOptions, null, null);
                    }
                }
                ((TextView) vh.header.findViewById(R.id.album_name)).setText(name);
                //((TextView) vh.header.findViewById(R.id.album_numtrack)).setText(mResources.getString(R.string.num_songs_of_album, getSectionAmount(c)));
                ((TextView) vh.header.findViewById(R.id.album_release_date)).setText(mResources.getString(R.string.release_date_of_album, releaseDate));
                vh.header.setVisibility(View.VISIBLE);
                vh.header.findViewById(R.id.play_now).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        MusicUtils.playAll(mActivity, getItemsBySection(aid), 0, 2);

                    }
                });
                vh.headerCheckBox.setClickable(false);
                if (isEditMode()) {
                    boolean checkedState = mAlbumCheckedMap.get(section);
                    if (mNeedin && !checkedState) {
                        //vh.headerCheckBox.SetChecked(checkedState, false);
                    } else {
                        //vh.headerCheckBox.SetChecked(checkedState, true);
                    }
                    vh.headerCheckBox.setVisibility(View.VISIBLE);
                    vh.header.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            CheckBox cBox = (CheckBox) v.findViewById(R.id.header_checkbox);
                            if (cBox.isChecked()) {
                                mAlbumCheckedMap.put(section, false);
                                //cBox.SetChecked(false, true);
                                setSectionCheckState(section, false);
                            } else {
                                mAlbumCheckedMap.put(section, true);
                                //cBox.SetChecked(true, true);
                                setSectionCheckState(section, true);
                            }
                            ((AlbumDetailActivity) mActivity).changeMenuState();
                        }
                    });
                    vh.header.findViewById(R.id.play_now).setVisibility(View.GONE);
                } else {
                    vh.headerCheckBox.setVisibility(View.GONE);
                    vh.header.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View arg0) {
                            //if (lv != null && lv.hbIsRubbishOut()) {
                            //    lv.hbSetRubbishBack();
                            //}
                        }

                    });
                    vh.header.setOnLongClickListener(new View.OnLongClickListener() {

                        @Override
                        public boolean onLongClick(View v) {
                            //if (lv != null && lv.hbIsRubbishOut()) {
                           //     lv.hbSetRubbishBack();
                            //    return true;
                            //} else {
                                mHeaderNeedin = true;
                                CheckBox cBox = (CheckBox) v.findViewById(R.id.header_checkbox);
                                //cBox.SetChecked(true, true);
                                mAlbumCheckedMap.put(mList.get(position).getAlbumId(), true);
                                setSectionCheckState(section, true);
                                return false;
                          //  }

                        }
                    });
                    vh.header.findViewById(R.id.play_now).setVisibility(View.VISIBLE);
                }
                if (isDeleteAction()) {
                    ViewGroup.LayoutParams vl = convertView.getLayoutParams();
                    if (null != vl) {
                        vl.height = mActivity.getResources().getDimensionPixelSize(R.dimen.album_item_height) + mActivity.getResources().getDimensionPixelSize(R.dimen.track_item_height);
                    }
                    convertView.findViewById(com.hb.R.id.content).setAlpha(255);
                }
            } else {
                vh.header.setVisibility(View.GONE);
                vh.header.findViewById(R.id.play_now).setVisibility(View.GONE);
                if (isDeleteAction()) {
                    ViewGroup.LayoutParams vl = convertView.getLayoutParams();
                    if (null != vl) {
                        vl.height = mActivity.getResources().getDimensionPixelSize(R.dimen.track_item_height);
                    }
                    convertView.findViewById(com.hb.R.id.content).setAlpha(255);
                }
            }
        } else {
            vh.header.setVisibility(View.GONE);
            if (isDeleteAction()) {
                ViewGroup.LayoutParams vl = convertView.getLayoutParams();
                if (null != vl) {
                    vl.height = mActivity.getResources().getDimensionPixelSize(R.dimen.track_item_height);
                }
                convertView.findViewById(com.hb.R.id.content).setAlpha(255);
            }
        }

        Boolean checked = mCheckedMap.get(mList.get(position).getSongId());
        if (checked != null && checked && mEditMode) {
            //((CheckBox) vh.cb).SetChecked(true, true);
        } else {
            //((CheckBox) vh.cb).SetChecked(false, true);
        }
        if (mCurrentPosition == position && !mEditMode) {
            convertView.findViewById(R.id.play_indicator).setVisibility(View.VISIBLE);
        } else {
            convertView.findViewById(R.id.play_indicator).setVisibility(View.INVISIBLE);
        }

        // mImageResizer.loadImage(item, vh.play_indicator);
        return convertView;
    }

    public void setNeedin(int position) {
        mNeedin = true;
        selectAll(false);
        long aid = mList.get(position).getAlbumId();
        if (mHeaderNeedin) {
            mAlbumCheckedMap.put(aid, true);
            setSectionCheckState(aid, true);
        } else {
            if (mAlbumMap.get(aid) != null && mAlbumMap.get(aid).getTrackNumber() == 1) {
                mAlbumCheckedMap.put(aid, true);
            }
        }
        mCheckedMap.put(mList.get(position).getSongId(), true);
        mHandler.sendEmptyMessageDelayed(MSG_NEED_IN, WAIT_TIME);
    }

    public void setSelected(int position) {
        mCurrentPosition = position;
    }

    public void setNeedout() {
        mNeedout = true;
        mHandler.sendEmptyMessageDelayed(MSG_NEED_OUT, WAIT_TIME);
    }

    public void selectAll(boolean isAll) {
        for (int i = 0; i < mList.size(); i++) {
            mCheckedMap.put(mList.get(i).getSongId(), isAll);
            mAlbumCheckedMap.put(mList.get(i).getAlbumId(), isAll);
        }
    }

    public void setEditMode(boolean flag) {
        mEditMode = flag;
    }

    public void setListView(HbListView lv) {
        this.lv = lv;
    }

    public boolean isEditMode() {
        return mEditMode;
    }

    public void selectAll() {
        selectAll(true);
    }

    public void selectAllNot() {
        selectAll(false);
    }

    public int getPositionForSection(int section) {
        for (int i = 0; i < mList.size(); i++) {
            char c = mList.get(i).getPinyin().charAt(0);
            if (c > section && section != 35) {
                return -1;
            }
            if (c == section || mList.get(i).getPinyin().charAt(0) < 65 || mList.get(i).getPinyin().charAt(0) > 90) {
                return i;
            }
        }
        return 0;
    }

    private ArrayList<HBListItem> getItemsBySection(long section) {
        ArrayList<HBListItem> result = new ArrayList<HBListItem>();
        if (mList != null) {
            for (int i = 0; i < mList.size(); i++) {
                long c = mList.get(i).getAlbumId();
                if (c == section) {
                    result.add(getItem(i));
                }
            }
        }
        return result;
    }

    public int getCheckedCount() {
        int count = 0;
        Iterator<Boolean> it = mCheckedMap.values().iterator();
        while (it.hasNext()) {
            boolean as = it.next();
            if (as == true) {
                count++;
            }
        }
        return count;
    }

    public HashMap getmCheckedMap() {
        return mCheckedMap;
    }

    // public void deleteItem(int position) {
    // mList.remove(position);
    // mCheckedMap.remove(position);
    // notifyDataSetChanged();
    // }

    private void initImageCacheParams() {
        mOptions = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.hb_online_recommend_default).showImageForEmptyUri(R.drawable.hb_online_recommend_default)
                .showImageOnFail(R.drawable.hb_online_recommend_default).cacheInMemory(true).cacheOnDisk(true).imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)
                .displayer(new SimpleBitmapDisplayer()).build();
    }

    public boolean getCheckedArrayValue(int position) {
        return mCheckedMap.get(mList.get(position).getSongId());
    }

    public void setCheckedArrayValue(int position) {
        setCheckedArrayValue(position, true);
    }

    public void setCheckedArrayValue(int position, boolean isChecked) {
        mCheckedMap.put(mList.get(position).getSongId(), isChecked);
        long albumId = mList.get(position).getAlbumId();
        if (!isChecked) {
            mAlbumCheckedMap.put(albumId, isChecked);
        } else {
            if (isAlbumAllChecked(albumId)) {
                mAlbumCheckedMap.put(albumId, isChecked);
            }
        }
    }

    public HashMap<Long, Boolean> getCheckedMap() {
        return mCheckedMap;
    }

    /**
     * 根据ListView的当前位置获取分类的首字母的Char ascii值
     */
    public long getSectionForPosition(int position) {
        return mList.get(position).getAlbumId();
    }

    public int getPositionForSection(long section) {
        for (int i = 0; i < mList.size(); i++) {
            long c = mList.get(i).getAlbumId();
            if (c == section) {
                return i;
            }
        }
        return -1;
    }

    public int getHeaderCount() {

        if (mList == null || mList.size() == 0) {
            return 0;
        }
        int num = 1;
        long aid = mList.get(0).getAlbumId();
        for (int i = 1; i < mList.size(); i++) {
            long bid = mList.get(i).getAlbumId();
            if (bid != aid) {
                num++;
                aid = bid;
            }
        }
        return num;
    }

    public void setSectionCheckState(long section, boolean isChecked) {
        for (int i = 0; i < mList.size(); i++) {
            if (mList.get(i).getAlbumId() == section) {
                mCheckedMap.put(mList.get(i).getSongId(), isChecked);
            }
            notifyDataSetChanged();
        }
    }

    public boolean isAlbumAllChecked(long section) {
        boolean checkState = false;
        for (int i = 0; i < mList.size(); i++) {
            if (mList.get(i).getAlbumId() == section) {
                checkState = mCheckedMap.get(mList.get(i).getSongId());
                if (!checkState) {
                    return checkState;
                }
            }
        }
        return checkState;
    }

    public int getSectionAmount(long section) {
        int num = 0;
        for (int i = 0; i < mList.size(); i++) {
            long c = mList.get(i).getAlbumId();
            if (c == section) {
                num++;
            }
        }
        return num;
    }

    public int getCurrentPlayPosition() {
        return mCurrentPosition;
    }

    public void setDataList(ArrayList<HBListItem> tracks) {
        mList = tracks;
        if (mCheckedMap != null) {
            mCheckedMap.clear();
        }
        if (!mSectionHeaderDisplayEnabled && mList != null && !mList.isEmpty()) {
            mAlbumCheckedMap.put(mList.get(0).getAlbumId(), false);
        } else if (mSectionHeaderDisplayEnabled && AlbumContent.ITEMS != null) {
            mAlbumMap = AlbumContent.ITEM_MAP;
            for (int index = 0; index < AlbumContent.ITEMS.size(); index++) {
                mAlbumCheckedMap.put(AlbumContent.ITEMS.get(index).getAlbumId(), false);
            }
        }
        if (mList != null) {
            for (int index = 0; index < mList.size(); index++) {
                mCheckedMap.put(mList.get(index).getSongId(), false);
            }
        }
        notifyDataSetChanged();
    }

    public void deleteNotify(int postion) {

        if (postion < 0) {
            return;
        }

        if (mCurrentPosition > postion) {
            mCurrentPosition--;
        }
        notifyDataSetChanged();
    }
}
