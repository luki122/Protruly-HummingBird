package com.protruly.music.adapter;

import android.app.Activity;
import android.content.Context;
import android.database.CharArrayBuffer;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
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
import com.protruly.music.ui.HBTrackBrowserActivity;
import com.protruly.music.util.HBListItem;
import com.protruly.music.util.HBMusicUtil;
import com.protruly.music.widget.StickyListHeadersAdapter;

import java.util.ArrayList;

import hb.widget.HbListView;

/**
 * Created by hujianwei on 17-9-5.
 */

public class HBMusicListAdapter extends BaseAdapter implements StickyListHeadersAdapter {
    private ArrayList<HBListItem> mList;
    private boolean mNeedin = false;
    private Context mContext;
    private static final int MSG_NEED_IN = 200001;
    private static final int MSG_NEED_OUT = 200002;
    private boolean mNeedout = false;
    private boolean mEditMode = false;
    private int mCurrentPosition = -1;
    private static final int WAIT_TIME = 50;
    private static final String IMAGE_CACHE_DIR = "AuroraAlbum";
    public boolean hasDeleted = false;
    private int margin;
    private String unkown,mAlbumName;
    private int playmodeheight;
    private int itemHeight;
    DisplayImageOptions mOptions;
    private Bitmap defaultBitmap = null;

    private SparseBooleanArray mCheckedMap = new SparseBooleanArray();
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

        // 歌名
        TextView line1;

        // 歌手
        TextView line2;

        // 分类
        View header;

        // 专辑封面
        ImageView play_indicator;

        CharArrayBuffer buffer1;

        RelativeLayout front;

        CheckBox cb;

        View hb_content;
        
        View song_selected;
    }

    public HBMusicListAdapter(Context context, ArrayList<HBListItem> list) {
        mContext = context;
        mList = list;
        margin = (int) mContext.getResources().getDimension(R.dimen.hb_checkbox_margin);
        unkown = mContext.getString(R.string.unknown);
        mAlbumName = context.getString(R.string.unknown_album_name);
        playmodeheight = mContext.getResources().getDimensionPixelOffset(R.dimen.hb_playmode_height);
        itemHeight = mContext.getResources().getDimensionPixelSize(R.dimen.hb_song_item_height);
        initImageCacheParams();
    }

    private void initImageCacheParams() {
        if (defaultBitmap == null) {
            defaultBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.default_music_icon);
        }
        mOptions = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.default_music_icon2).showImageForEmptyUri(R.drawable.default_music_icon2)
                .showImageOnFail(R.drawable.default_music_icon2).cacheInMemory(true).cacheOnDisk(true).considerExifParams(true).bitmapConfig(Bitmap.Config.RGB_565).displayer(new SimpleBitmapDisplayer())
                .build();
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
    public Object getItem(int arg0) {
        // TODO Auto-generated method stub
        return mList.get(arg0);
    }

    @Override
    public long getItemId(int arg0) {
        // TODO Auto-generated method stub
        return arg0;
    }

    @Override
    public View getView(int arg0, View arg1, ViewGroup arg2) {
        // TODO Auto-generated method stub
        final ViewHolder vh;
        final int position = arg0;

        if (arg1 == null) {
            arg1 = LayoutInflater.from(mContext).inflate(R.layout.hb_slid_listview, null);
            View myView = LayoutInflater.from(mContext).inflate(R.layout.hb_song_listitem, null);
            vh = new ViewHolder();
            vh.line1 = (TextView) myView.findViewById(R.id.song_title);
            vh.line2 = (TextView) myView.findViewById(R.id.song_artist);
            vh.play_indicator = (ImageView) myView.findViewById(R.id.song_playicon);

            RelativeLayout rl_control_padding = (RelativeLayout) arg1.findViewById(R.id.control_padding);
            rl_control_padding.setPadding(0, 0, (int) mContext.getResources().getDimension(R.dimen.checkbox_margin_right_in_listview_with_index), 0);

            vh.front = (RelativeLayout) arg1.findViewById(R.id.hb_listview_front);
            vh.front.addView(myView, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            vh.cb = (CheckBox) arg1.findViewById(R.id.hb_list_left_checkbox);
            vh.hb_content = arg1.findViewById(R.id.content);
            vh.song_selected = arg1.findViewById(R.id.iv_song_selected);
            arg1.setTag(vh);
        } else {

            vh = (ViewHolder) arg1.getTag();
        }
        
        if (mList == null) {
            return null;
        }

        final HBListItem item = mList.get(arg0);
        if (item != null) {
            String title = item.getTitle();
            if (title.equals("<unknown>"))
                title = unkown;
            vh.line1.setText(title);

            String artist_name = item.getArtistName();
            if (TextUtils.isEmpty(artist_name) || artist_name.equals("<unknown>")) {
                artist_name = unkown;
            }

            String album_name = item.getAlbumName();
            album_name = HBMusicUtil.doAlbumName(item.getFilePath(), album_name);
            if (TextUtils.isEmpty(album_name) || album_name.equals("<unknown>"))
                album_name = mAlbumName;

            StringBuffer tBuffer = new StringBuffer();
            tBuffer.append(artist_name + "·" + album_name);
            vh.line2.setText(tBuffer.toString());
        }

        if (arg0 == 0 && !((HBTrackBrowserActivity) mContext).fromAdd) {
            if (vh.header == null)
                vh.header = LayoutInflater.from(mContext).inflate(R.layout.hb_songheader_layout, null);
            ((ViewGroup) arg1).removeView(vh.header);
            ((ViewGroup) arg1).addView(vh.header, 0);
            vh.header.setVisibility(View.VISIBLE);
            ((TextView) vh.header.findViewById(R.id.tv_songnumber)).setText(mContext.getResources().getString(R.string.number_track));
            vh.header.findViewById(R.id.tv_songnumber).setOnClickListener(null);
            if (mEditMode) {
                vh.header.setOnClickListener(null);
                ((TextView) vh.header.findViewById(R.id.tv_playmode)).setEnabled(false);
            } else {
                vh.header.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // TODO Auto-generated method stub
                        ((HBTrackBrowserActivity) mContext).shufflePlay();
                    }
                });
                ((TextView) vh.header.findViewById(R.id.tv_playmode)).setEnabled(true);
            }
            if (hasDeleted) {
                ViewGroup.LayoutParams vl = arg1.getLayoutParams();
                if (null != vl) {
                    vl.height = itemHeight + playmodeheight;
                }
                //vh.hb_content.setAlpha(255);
            }
        } else {
            ((ViewGroup) arg1).removeView(vh.header);
            if (hasDeleted) {
                ViewGroup.LayoutParams vl = arg1.getLayoutParams();
                if (null != vl) {
                    vl.height = itemHeight;
                }
               // vh.hb_content.setAlpha(255);
            }
        }
        if (mNeedin) {
            //HbListView.hbStartCheckBoxAppearingAnim(vh.front, vh.cb);
        } else if (!mNeedin && mEditMode) {
            //HbListView.hbSetCheckBoxVisible(vh.front, vh.cb, true);
        }
        if (mNeedout) {
            //HbListView.hbStartCheckBoxDisappearingAnim(vh.front, vh.cb);
        } else if (!mEditMode && !mNeedout) {
            //HbListView.hbSetCheckBoxVisible(vh.front, vh.cb, false);
        }
        if (mEditMode) {
            arg1.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    if (!((CheckBox) vh.cb).isChecked()) {
                        //((CheckBox) vh.cb).hbSetChecked(true, true);
                        mCheckedMap.put(position, true);
                        ((HBTrackBrowserActivity) mContext).changeMenuState(1, mList.get(position).getSongId());
                    } else {
                        //((CheckBox) vh.cb).hbSetChecked(false, true);
                        mCheckedMap.put(position, false);
                        ((HBTrackBrowserActivity) mContext).changeMenuState(0, mList.get(position).getSongId());
                    }
                }
            });
            vh.song_selected.setVisibility(View.INVISIBLE);
        } else {
            arg1.setClickable(false);
            if (vh.header != null) {
                vh.header.setClickable(true);
            }
            long id = MusicUtils.getCurrentAudioId();
            if (id == -1 && arg0 == mCurrentPosition) {
                vh.song_selected.setVisibility(View.VISIBLE);
            } else if ((mList.get(arg0).getSongId() == id) && mCurrentPosition >= 0) {
                vh.song_selected.setVisibility(View.VISIBLE);
            } else {
                if ( vh == null)
                Log.d("JW" , "error!");

                vh.song_selected.findViewById(R.id.iv_song_selected).setVisibility(View.INVISIBLE);
            }
        }
        Boolean checked = mCheckedMap.get(position);
        if (checked != null && checked && mEditMode) {
            ((CheckBox) vh.cb).setChecked(true);
        } else {
            ((CheckBox) vh.cb).setChecked(false);
        }
		if(!TextUtils.isEmpty(item.getAlbumImgUri())){
        ImageLoader.getInstance().displayImage(item.getAlbumImgUri(), item.getSongId(), vh.play_indicator, mOptions,null, null);
		}else {
			vh.play_indicator.setImageBitmap(defaultBitmap);
		}

        return arg1;
    }

    public void setNeedin() {
        mNeedin = true;
        mHandler.sendEmptyMessageDelayed(MSG_NEED_IN, WAIT_TIME);
    }

    public void setNeedout() {
        mNeedout = true;
        mHandler.sendEmptyMessageDelayed(MSG_NEED_OUT, WAIT_TIME);
    }

    public void changeEidtMode(boolean flag) {
        mEditMode = flag;
    }

    public boolean getEidtMode() {
        return mEditMode;
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
        return -1;
    }

    public void deleteItem(int position) {
        mList.remove(position);
        notifyDataSetChanged();
    }

    public int getCurrentPosition() {
        return mCurrentPosition;
    }

    public void setCurrentPosition(int position) {
        mCurrentPosition = position;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        if (((HBTrackBrowserActivity) mContext).fromAdd) {
            convertView = new View(mContext);
            return convertView;
        }
        if (convertView == null) {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            convertView = inflater.inflate(R.layout.hb_songheader_layout, parent, false);
        }
        ((TextView) convertView.findViewById(R.id.tv_songnumber)).setText(mContext.getResources().getString(R.string.number_track));

        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setItemChecked(int position, boolean flag) {
        mCheckedMap.put(position, flag);
    }

    public boolean isItemChecked(int position) {
        return mCheckedMap.get(position);
    }

    public int getCheckedCount() {
        int count = 0;
        for (int i = 0; i < mCheckedMap.size(); i++) {
            if (mCheckedMap.get(i))
                count++;
        }
        return count;
    }
}
