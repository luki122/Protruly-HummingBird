package com.protruly.music.adapter;

import android.content.Context;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.protruly.music.R;
import com.protruly.music.model.SearchItem;
import com.protruly.music.util.Globals;
import com.protruly.music.widget.StickyListHeadersAdapter;

import java.util.ArrayList;



import hb.widget.HbListView;

/**
 * Created by hujianwei on 17-9-4.
 */

public class HBSearchAdapter extends BaseAdapter implements StickyListHeadersAdapter {
    private static final String IMAGE_CACHE_DIR = "HBAlbum";
    private String mSavePath = Globals.mSavePath;
    private String bitrate = "";
    private String BITMAP_DOWNLOAD_DIR;
    private DisplayImageOptions mOptions;
    private static final String TAG ="HBSearchAdapter";

    // private HBBitmapDownloader bitmapDownlaoder;
    static class ViewHold {
        TextView line1;
        TextView line2;
        ImageView iv_playicon;
        RelativeLayout front;
        CheckBox cb;
        View header;
        ImageView iv_songselected;
        //HbRoundProgressBar image_download;
        ImageView iv_download_ok;
        View view_download_parent;
    }

    public boolean hasDeleted = false;
    private ArrayList<SearchItem> mList;
    private Context mContext;
    private boolean mEditMode = false;
    public static final int WAIT = 0;
    public static final int RUNNING = 1;
    public static final int START = 3;
    public static final int FINISH = 4;
    public static final int EXCEPIONG = 5;
    public static final int EXIST = 200;
    private String prixBitrate = "128";

    public HBSearchAdapter(Context context, ArrayList<SearchItem> list) {
        mList = list;
        mContext = context;
        initImageCacheParams();
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        if (mList != null)
            return mList.size();
        return 0;
    }

    @Override
    public Object getItem(int arg0) {
        // TODO Auto-generated method stub
        if (mList != null)
            return mList.get(arg0);
        return null;
    }

    @Override
    public long getItemId(int arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public View getView(int arg0, View arg1, ViewGroup arg2) {
        final int position = arg0;
        final ViewHold vh;
//        if (arg1 == null) {
//            vh = new ViewHold();
//            arg1 = LayoutInflater.from(mContext).inflate(com.hb.R.layout.hb_slid_listview, null);
//            vh.front = (RelativeLayout) arg1.findViewById(com.hb.R.id.hb_listview_front);
//            LayoutInflater.from(mContext).inflate(R.layout.song_listitem, vh.front);
//            RelativeLayout rl_control_padding = (RelativeLayout) arg1.findViewById(com.hb.R.id.control_padding);
//            rl_control_padding.setPadding(0, 0, 0, 0);
//            vh.iv_playicon = (ImageView) arg1.findViewById(R.id.song_playicon);
//            vh.line1 = (TextView) arg1.findViewById(R.id.song_title);
//            vh.line2 = (TextView) arg1.findViewById(R.id.song_artist);
//            vh.iv_songselected = (ImageView) arg1.findViewById(R.id.iv_song_selected);
//            vh.view_download_parent = arg1.findViewById(R.id.hb_btn_recommend_download_parent);
//            vh.image_download = (HbRoundProgressBar) arg1.findViewById(R.id.hb_btn_recommend_download);
//            vh.iv_download_ok = (ImageView) arg1.findViewById(R.id.hb_download_ok);
//            vh.header = LayoutInflater.from(mContext).inflate(R.layout.hb_search_header, null);
//            ((ViewGroup) arg1).addView(vh.header, 0);
//            vh.cb = (CheckBox) arg1.findViewById(com.hb.R.id.hb_list_left_checkbox);
//
//            arg1.setTag(vh);
//        } else {
//            vh = (ViewHold) arg1.getTag();
//        }
//        final SearchItem item = mList.get(arg0);
//        if (item.mMimeType.equals(MediaStore.Audio.Artists.ARTIST) || "net_artist".equals(item.mMimeType)) {
//            if (item.mTag) {
//                vh.header.setVisibility(View.VISIBLE);
//                ((ImageView) vh.header.findViewById(R.id.hb_search_icon)).setImageResource(R.drawable.hb_search_artisttag);
//                ((TextView) vh.header.findViewById(R.id.hb_search_text)).setText(mContext.getResources().getString(R.string.appwidget_artisttitle));
//            } else {
//                vh.header.setVisibility(View.GONE);
//            }
//            vh.view_download_parent.setVisibility(View.GONE);
//            vh.iv_download_ok.setVisibility(View.GONE);
//            vh.line1.setText(mList.get(arg0).getArtistName());
//            vh.line2.setText(mContext.getResources().getString(R.string.num_songs_and_album, mList.get(arg0).mAlbumCount, mList.get(arg0).mSongCount));
//            vh.iv_playicon.setVisibility(View.GONE);
//        } else {
//            if (mList.get(arg0).mTag) {
//                vh.header.setVisibility(View.VISIBLE);
//                ((ImageView) vh.header.findViewById(R.id.hb_search_icon)).setImageResource(R.drawable.hb_search_tracktag);
//                ((TextView) vh.header.findViewById(R.id.hb_search_text)).setText(mContext.getResources().getString(R.string.appwidget_songtitle));
//            } else {
//                vh.header.setVisibility(View.GONE);
//            }
//            String title = item.getTitle();
//            if (title == null || TextUtils.isEmpty(title))
//                title = mContext.getResources().getString(R.string.unknown_track);
//            vh.line1.setText(title);
//            String artist_name = item.getArtistName();
//            if (artist_name == null || TextUtils.isEmpty(artist_name))
//                artist_name = mContext.getResources().getString(R.string.unknown_artist);
//            if (artist_name != null && artist_name.length() > 12) {
//                artist_name = artist_name.substring(0, 12) + "…";
//            }
//            vh.line2.setText(artist_name + " · ");
//            String artist_album = item.getAlbumName();
//            if (artist_album == null || TextUtils.isEmpty(artist_album))
//                artist_album = mContext.getResources().getString(R.string.unknown_album_name);
//            vh.line2.append(artist_album);
//            vh.iv_playicon.setVisibility(View.VISIBLE);
//            if (!item.mMimeType.startsWith("net")) {
//                // mImageResizer.loadImage(item, vh.iv_playicon, 1);
//                ImageLoader.getInstance().displayImage(item.mAlbumImageUri, item.getSongId(), vh.iv_playicon, mOptions, null, null);
//            } else {
//                vh.iv_playicon.setVisibility(View.GONE);
//                if (item.mMimeType.equals("net_artist")) {
//                    vh.view_download_parent.setVisibility(View.GONE);
//                    vh.iv_download_ok.setVisibility(View.GONE);
//                } else {
//                    vh.view_download_parent.setVisibility(View.VISIBLE);
//                }
//            }
//        }
//        if (mEditMode) {
//            HbListView.hbSetCheckBoxVisible(vh.front, vh.cb, true);
//            arg1.setOnClickListener(new View.OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//                    // TODO Auto-generated method stub
//                    if (!((CheckBox) vh.cb).isChecked()) {
//                        ((CheckBox) vh.cb).hbSetChecked(true, true);
//                        if (mContext instanceof HBTrackBrowserActivity) {
//                            if (mList.get(position).mMimeType.equals("artist")) {
//                                ((HBTrackBrowserActivity) mContext).setArtistItemChecked(mList.get(position).getSongId(), true);
//                            } else {
//                                ((HBTrackBrowserActivity) mContext).setItemChecked(mList.get(position).getSongId(), true);
//                                ((HBTrackBrowserActivity) mContext).changeMenuState(1, mList.get(position).getSongId());
//                            }
//                        } else if (mContext instanceof HBArtistBrowserActivity) {
//                            ((HBArtistBrowserActivity) mContext).setItemChecked(mList.get(position).getSongId(), true);
//                            ((HBArtistBrowserActivity) mContext).changeMenuState();
//                        }
//                    } else {
//                        ((CheckBox) vh.cb).hbSetChecked(false, true);
//                        if (mContext instanceof HBTrackBrowserActivity) {
//                            if (mList.get(position).mMimeType.equals("artist")) {
//                                ((HBTrackBrowserActivity) mContext).setArtistItemChecked(mList.get(position).getSongId(), false);
//                            } else {
//                                ((HBTrackBrowserActivity) mContext).setItemChecked(mList.get(position).getSongId(), false);
//                                ((HBTrackBrowserActivity) mContext).changeMenuState(0, mList.get(position).getSongId());
//                            }
//                        } else {
//                            ((HBArtistBrowserActivity) mContext).setItemChecked(mList.get(position).getSongId(), false);
//                            ((HBArtistBrowserActivity) mContext).changeMenuState();
//                        }
//                    }
//                }
//            });
//            if (mContext instanceof HBTrackBrowserActivity) {
//                if (mList.get(position).mMimeType.equals("artist")) {
//                    if (((HBTrackBrowserActivity) mContext).getArtistItemCheckedSongId(mList.get(position).getSongId(), mList.get(position).mSongCount))
//                        ((CheckBox) vh.cb).hbSetChecked(true, false);
//                    else
//                        ((CheckBox) vh.cb).hbSetChecked(false, false);
//                } else {
//                    if (((HBTrackBrowserActivity) mContext).getItemCheckedSongId(mList.get(position).getSongId())) {
//                        ((CheckBox) vh.cb).hbSetChecked(true, false);
//                    } else {
//                        ((CheckBox) vh.cb).hbSetChecked(false, false);
//                    }
//                }
//            } else if (mContext instanceof HBArtistBrowserActivity && ((HBArtistBrowserActivity) mContext).getItemCheckedArtisId(mList.get(position).getSongId())) {
//                ((CheckBox) vh.cb).hbSetChecked(true, false);
//            } else {
//                ((CheckBox) vh.cb).hbSetChecked(false, false);
//            }
//            vh.line1.setTextColor(mContext.getResources().getColor(R.color.black));
//            vh.line2.setTextColor(mContext.getResources().getColor(R.color.hb_item_song_size));
//            if (!mList.get(position).mMimeType.startsWith("net")) {
//
//                ImageLoader.getInstance().displayImage(item.mAlbumImageUri, item.getSongId(), vh.iv_playicon, mOptions, null, null);
//            }
//        } else {
//            arg1.setClickable(false);
//            HbListView.hbSetCheckBoxVisible(vh.front, vh.cb, false);
//            if (!mList.get(position).mMimeType.startsWith("net")) {
//
//                ImageLoader.getInstance().displayImage(item.mAlbumImageUri, item.getSongId(), vh.iv_playicon, mOptions, null, null);
//            }
//            if (!mList.get(position).mMimeType.equals("artist")) {
//                long id = MusicUtils.getCurrentAudioId();
//                if (item.getSongId() == id) {
//                    vh.iv_songselected.setVisibility(View.VISIBLE);
//                } else {
//                    vh.iv_songselected.setVisibility(View.GONE);
//                }
//            } else {
//                vh.iv_songselected.setVisibility(View.GONE);
//            }
//        }
//        arg1.setBackgroundResource(R.drawable.hb_playlist_item_clicked);
        return arg1;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getHeaderId(int position) {
        // TODO Auto-generated method stub
        return 0;
    }

    private void initImageCacheParams() {
        mOptions = new DisplayImageOptions.Builder().imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2).cacheInMemory(true).cacheOnDisk(true).showImageOnLoading(R.drawable.default_music_icon2)
                .showImageForEmptyUri(R.drawable.default_music_icon2).showImageOnFail(R.drawable.default_music_icon2)
                .displayer(new RoundedBitmapDisplayer((int) mContext.getResources().getDimension(R.dimen.hb_album_play_icon_margin_bottom))).build();
    }

    public void setEidtMode(boolean flag) {
        mEditMode = flag;
    }

    public void clearCache() {
    }

}
