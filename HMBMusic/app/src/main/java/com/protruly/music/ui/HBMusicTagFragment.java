package com.protruly.music.ui;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.util.Globals;
import com.protruly.music.util.HBListItem;
import com.protruly.music.util.HBMusicUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by xiaobin on 17-9-15.
 */

public class HBMusicTagFragment extends Fragment {

    private static final String TAG = "HBMusicTagFragment";

    private static final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

    private View emptyView;
    private ListView musicListView;

    private ArrayList<HBListItem> mArrayList;
    private ArrayList<HBListItem> mList;
    private MusicListAdapter musicListAdapter;

    private LoadMusicListTask loadMusicListTask;

    private List<String> mPathsXml = null;
    private int mWidth = 0;
    private int mHight = 0;

    private int musicCount = 0;
    private long sizeCount = 0;
    private long sizeCountMB = 0;

    private int artistId = -1;

    public static HBMusicTagFragment newInstance() {
        return new HBMusicTagFragment(-1);
    }

    public static HBMusicTagFragment newInstance(int artistId) {
        return new HBMusicTagFragment(artistId);
    }

    public HBMusicTagFragment() {
        super();
    }

    public HBMusicTagFragment(int artistId) {
        super();
        this.artistId = artistId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_music, container, false);
        emptyView = rootView.findViewById(R.id.empty_view);
        musicListView = (ListView) rootView.findViewById(R.id.musicListView);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        musicListView.setAdapter(musicListAdapter);
        musicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playMusic(mArrayList, position);
            }
        });

        loadMusicListTask = new LoadMusicListTask();
        loadMusicListTask.execute();
    }

    private void initData() {
        mArrayList = new ArrayList<HBListItem>();
        mList = new ArrayList<HBListItem>();
        musicListAdapter = new MusicListAdapter(getActivity(), mArrayList);

        mPathsXml = HBMusicUtil.doParseXml(getActivity(), "paths.xml");
        mWidth = (int) getResources().getDimension(R.dimen.hb_albumIcon_size);
        mHight = mWidth;
    }

    private void initAdapter(Cursor cursor) {
        if (cursor == null) {
            return;
        }
        mList.clear();
        sizeCount = 0;
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            int mId = cursor.getInt(0);
            String mTitle = "<unknown>";
            if (cursor.getString(2) != null && !TextUtils.isEmpty(cursor.getString(2)))
                mTitle = cursor.getString(2);
            String mPath = "<unknown>";
            if (cursor.getString(3) != null && !TextUtils.isEmpty(cursor.getString(3)))
                mPath = cursor.getString(3);
            String mAlbumName = "<unknown>";
            if (cursor.getString(4) != null && !TextUtils.isEmpty(cursor.getString(4)))
                mAlbumName = cursor.getString(4);
            String mArtistName = "<unknown>";
            if (cursor.getString(5) != null && !TextUtils.isEmpty(cursor.getString(5)))
                mArtistName = cursor.getString(5);
            int mduration = cursor.getInt(7);
            String mUri = mPath;
            String imgUri = HBMusicUtil.getImgPath(getActivity().getApplication(), HBMusicUtil.MD5(mTitle + mArtistName + mAlbumName));
            long mAlbumId = cursor.getLong(1);
            String mPinyin = MusicUtils.getSpell(mTitle);
            HBListItem listItem = new HBListItem((long) mId, mTitle, mUri, mAlbumName, mAlbumId, mArtistName, 0, imgUri, null, null, -1);
            listItem.setDuration(mduration);
            listItem.setPinyin(mPinyin);
            listItem.setArtistId(cursor.getLong(6));
            listItem.setSize(cursor.getLong(8));
            System.out.println(mTitle + ": " + listItem.getSize());
            sizeCount += listItem.getSize();

            listItem.setItemPicSize(mWidth, mWidth);

            mPath = mPath.substring(0, mPath.lastIndexOf("/"));
            if (!mPathsXml.contains(mPath)) {
                mList.add(listItem);
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        if (mList.size() == 0)
            return;

        musicCount = mList.size();
        sizeCountMB = (long) (sizeCount * 1.0f / 1024 / 1024);

        Collections.sort(mList, new Comparator<HBListItem>() {

            @Override
            public int compare(HBListItem lhs, HBListItem rhs) {
                // TODO Auto-generated method stub
                if (lhs.getPinyin().charAt(0) == rhs.getPinyin().charAt(0) && (65 > lhs.getTitle().toUpperCase().charAt(0) || lhs.getTitle().toUpperCase().charAt(0) > 90)
                        && (rhs.getTitle().toUpperCase().charAt(0) <= 90 && rhs.getTitle().toUpperCase().charAt(0) >= 65)) {
                    return 1;

                } else if (lhs.getPinyin().charAt(0) == rhs.getPinyin().charAt(0) && (65 > rhs.getTitle().toUpperCase().charAt(0) || rhs.getTitle().toUpperCase().charAt(0) > 90)
                        && (lhs.getTitle().toUpperCase().charAt(0) <= 90 && lhs.getTitle().toUpperCase().charAt(0) >= 65)) {
                    return -1;
                }
                return lhs.getPinyin().compareTo(rhs.getPinyin());
            }
        });
    }

    public void playMusic(final ArrayList<HBListItem> arrayList, final int position) {
        Toast.makeText(getActivity(), "position " + position, Toast.LENGTH_SHORT).show();
        MusicUtils.playAll(getActivity(), arrayList, position, 0);
        HBMusicUtil.setCurrentPlaylist(getActivity(), -1);

        Intent intent = new Intent(getActivity(), HBPlayerActivity.class);
        intent.setAction(HBPlayerActivity.ACTION_FROM_MAINACTIVITY);
        startActivity(intent);
    }

    class LoadMusicListTask extends AsyncTask<Cursor, String, Boolean> {

        private long time;

        @Override
        protected Boolean doInBackground(Cursor... params) {
            // TODO Auto-generated method stub
            StringBuilder where = new StringBuilder();
            if (artistId != -1) {
                where.append(MediaStore.Audio.Media.ARTIST_ID + "=" + artistId);
            }

            String[] mCursorCols = new String[]
                    {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ALBUM_ID,
                            MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM,
                            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.DURATION,
                            MediaStore.Audio.Media.SIZE};

            Cursor cursor = getActivity().getContentResolver().query(uri, mCursorCols,
                    TextUtils.isEmpty(where.toString()) ? null : where.toString(), null, null);

            initAdapter(cursor);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // TODO Auto-generated method stub
            if (musicListAdapter != null) {
                mArrayList.clear();
                mArrayList.addAll(mList);

                musicListAdapter.notifyDataSetChanged();
            }

            if (mArrayList.size() > 0) {
                emptyView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.VISIBLE);
            }

            if (getActivity() != null && getActivity() instanceof HBLocalMusicActivity) {
                ((HBLocalMusicActivity) getActivity()).setMusicCountAndSizeCount(musicCount, sizeCountMB);
            }

            super.onPostExecute(result);
        }
    }

    private class MusicListAdapter extends BaseAdapter {

        private LayoutInflater inflater;
        private ArrayList<HBListItem> dataList;

        public MusicListAdapter(Context context, ArrayList<HBListItem> list) {
            inflater = LayoutInflater.from(context);
            dataList = list;
        }

        @Override
        public int getCount() {
            return dataList == null ? 0 : dataList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder = null;
            HBListItem listItem = dataList.get(position);
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.fragment_music_list_item, null);
                holder = new Holder();
                holder.tv_title = (TextView) convertView.findViewById(R.id.tv_title);
                holder.iv_tone_quality = (ImageView) convertView.findViewById(R.id.iv_tone_quality);
                holder.tv_singer = (TextView) convertView.findViewById(R.id.tv_singer);
                holder.iv_playing = (ImageView) convertView.findViewById(R.id.iv_playing);
                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }

            holder.tv_title.setText(listItem.getTitle());
            holder.tv_singer.setText(listItem.getArtistName());
            holder.iv_tone_quality.setVisibility(View.GONE);

            return convertView;
        }

        class Holder {
            TextView tv_title;
            ImageView iv_tone_quality;
            TextView tv_singer;
            ImageView iv_playing;
        }
    }

}
