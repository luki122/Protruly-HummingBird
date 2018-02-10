package com.protruly.music.adapter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.downloadex.DownloadInfo;
import com.protruly.music.downloadex.DownloadManager;
import com.protruly.music.util.Globals;
import com.protruly.music.util.HBMainMenuData;
import com.protruly.music.util.HBMusicUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by hujianwei on 17-9-4.
 */

public class HBMainMenuAdapter extends BaseAdapter {

    private static final String TAG = "HBMainMenuAdapter";
    private Context mContext;
    private List<HBMainMenuData> datas = new ArrayList<HBMainMenuData>();
    private int layoutId = -1;
    private SparseIntArray defaultSize = new SparseIntArray();
    private List<String> mPathsXml = new ArrayList<String>();


    public HBMainMenuAdapter(Context context) {
        mContext = context;
    }

    public HBMainMenuAdapter(Context context, int layoutid) {
        mContext = context;
        this.layoutId = layoutid;
    }

    public void addData(HBMainMenuData info) {
        datas.add(info);
        notifyDataSetChanged();
    }

    public void clearData() {
        datas.clear();
    }

    public void addDatas(List<HBMainMenuData> list) {
        if (list == null) {
            return;
        }
        datas.addAll(list);
        notifyDataSetChanged();
    }

    public void deleteData(int position) {
        datas.remove(datas.size() - 1);
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
            if (layoutId != -1) {
                arg1 = LayoutInflater.from(mContext).inflate(layoutId, null);
            } else {
                arg1 = LayoutInflater.from(mContext).inflate(R.layout.hb_first_main_item, null);
            }
            holdView.icon = (ImageView) arg1.findViewById(R.id.id_icon);
            holdView.name = (TextView) arg1.findViewById(R.id.id_name);
            holdView.songsize = (TextView) arg1.findViewById(R.id.id_song_size);
            holdView.more = (ImageView) arg1.findViewById(R.id.id_more);
            holdView.point = (ImageView) arg1.findViewById(R.id.hb_red_point);
            arg1.setTag(holdView);
        } else {
            holdView = (HoldView) arg1.getTag();
        }

        HBMainMenuData info = datas.get(arg0);
        holdView.icon.setImageResource(info.getResouceId());
        holdView.name.setText(info.getName());

        if (info.getSongSizeType() != -1) {
            holdView.songsize.setVisibility(View.VISIBLE);
        } else {
            holdView.songsize.setText("");
            holdView.songsize.setVisibility(View.GONE);
        }
        if (info.getSongSizeType() != 5) {
            holdView.point.setVisibility(View.GONE);
        }
        new ShowSongSizeTask(holdView.songsize, info.getSongSizeType(), info.getPlaylistId(), holdView.point).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        if (info.isShowArrow()) {
            holdView.more.setVisibility(View.VISIBLE);
        } else {
            holdView.more.setVisibility(View.GONE);
            holdView.name.setGravity(Gravity.CENTER_VERTICAL);
        }

        return arg1;
    }

    class HoldView {
        ImageView icon;
        TextView name;
        TextView songsize;
        ImageView more;
        ImageView point;
    }

    /**
     * 异步获取歌曲数、歌手数、文件夹数
     * @author
     */
    private class ShowSongSizeTask extends AsyncTask<Void, Integer, Integer> {

        private TextView textview;
        private int sizeType;
        private int playlistid;
        private ImageView pointView;

        public ShowSongSizeTask(TextView view, int type, int id, ImageView point) {
            this.textview = view;
            this.sizeType = type;
            this.playlistid = id;
            this.pointView = point;
        }

        @Override
        protected Integer doInBackground(Void... params) {

            Cursor cursor = null;
            List<String> list = new ArrayList<String>();
            int size = 0;
            mPathsXml = HBMusicUtil.doParseXml(mContext, "paths.xml");
            try {
                switch (sizeType) {
                    case 0:
                        Uri uri1 = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistid);

                        cursor = mContext.getContentResolver().query(uri1, new String[] { MediaStore.Audio.Media.DATA }, null, null, null);
                        break;
                    case 1:
                        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                        StringBuilder where = new StringBuilder();
                        where.append(Globals.QUERY_SONG_FILTER);
                        cursor = mContext.getContentResolver().query(uri, new String[] { MediaStore.Audio.Media.DATA }, where.toString(), null, null);
                        break;
                    case 2:
                        cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Audio.Media.ARTIST }, Globals.QUERY_SONG_FILTER, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            do {
                                String artist = cursor.getString(0);
                                if (!list.contains(artist)) {
                                    list.add(artist);
                                }
                            } while (cursor.moveToNext());
                        }
                        break;
                    case 3:
                        StringBuilder where2 = new StringBuilder();
                        where2.append(Globals.QUERY_SONG_FILTER);
                        String[] cons = new String[] { MediaStore.Audio.Media.DATA, };
                        cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cons, where2.toString(), null, null);
                        if (cursor.moveToFirst()) {
                            do {
                                String path = cursor.getString(0);
                                path = path.substring(0, path.lastIndexOf("/"));
                                if (!list.contains(path) && !mPathsXml.contains(path)) {
                                    list.add(path);
                                }
                            } while (cursor.moveToNext());
                        }
                        break;
                    case 4:
                        size = MusicUtils.mSongDb.querySongIdFromFavorites(mPathsXml).size();
                        break;
                    case 5:
                        cursor = MusicUtils.query(mContext, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Audio.Media.DATA }, Globals.QUERY_SONG_FILTER, null,
                                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
                        break;
                    case 6:
                        size = MusicUtils.mSongDb.queryCollectInfo().size();
                        break;
                    case 7:
                        HashMap<Long, DownloadInfo> listmap = DownloadManager.getInstance(mContext.getApplicationContext()).getDownloadingMapData();
                        List<DownloadInfo> datas = new ArrayList<DownloadInfo>();
                        Iterator<DownloadInfo> iterator = listmap.values().iterator();
                        while (iterator.hasNext()) {
                            DownloadInfo info = iterator.next();
                            if (!info.isDownloadOver()) {
                                datas.add(info);
                            }
                        }
                        return datas.size();
                }

                if (cursor != null) {
                    // size = cursor.getCount();
                    if (sizeType == 3 || sizeType == 2 || sizeType == 7) {
                        size = list.size();
                    } else if (cursor.moveToFirst()) {
                        do {
                            String dir = cursor.getString(0);
                            dir = dir.substring(0, dir.lastIndexOf("/"));
                            if (!mPathsXml.contains(dir)) {
                                list.add(dir);
                            }
                        } while (cursor.moveToNext());
                        size = list.size();
                    }
                    // cursor.close();
                }
            } catch (Exception e) {

            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return size;
        }

        @Override
        protected void onPostExecute(Integer result) {

            if (sizeType == -1) {
                return;
            }
            setSizeText(result);
            defaultSize.put(playlistid, result);
            if (sizeType == 5) {

                if (result > MusicUtils.getIntPref(mContext, Globals.PREF_RECENTLY, 0)) {
                    pointView.setVisibility(View.VISIBLE);
                } else {
                    pointView.setVisibility(View.GONE);
                }

                // 防止被删除了，报错当前的歌曲数目
                if (result < MusicUtils.getIntPref(mContext, Globals.PREF_RECENTLY, 0)) {
                    MusicUtils.setIntPref(mContext, Globals.PREF_RECENTLY, result);
                }

            }
        }

        @Override
        protected void onPreExecute() {
            if (sizeType == -1) {
                return;
            }
            int size = defaultSize.get(playlistid);
            setSizeText(size);

        }

        private void setSizeText(int size) {
            switch (sizeType) {

                case 5:
                case 0:
                case 4:
                case 7:
                    textview.setText(mContext.getString(R.string.song_size, size));
                    break;
                case 1:
                    textview.setText(mContext.getString(R.string.hb_playlist_total_songs, size));
                    break;
                case 2:
                    textview.setText(mContext.getString(R.string.hb_playlist_total_singer, size));
                    break;
                case 3:
                    textview.setText(mContext.getString(R.string.hb_playlist_total_fold, size));
                    break;
                case 6:
                    textview.setText(mContext.getString(R.string.hb_collect_size, size));
                    break;
            }
        }
    }
    
}
