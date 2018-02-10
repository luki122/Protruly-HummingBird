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

import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.util.HBArtist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by xiaobin on 17-9-15.
 */

public class HBArtistTagFragment extends Fragment {

    private static final String TAG = "HBArtistTagFragment";

    private static final Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;

    private View emptyView;
    private ListView artistListView;

    private ArrayList<HBArtist> mArrayList;
    private ArrayList<HBArtist> mList;
    private ArtistListAdapter artistListAdapter;

    private LoadArtistListTask loadArtistListTask;

    private int artistCount = 0;

    public static HBArtistTagFragment newInstance() {
        return new HBArtistTagFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artist, container, false);
        emptyView = rootView.findViewById(R.id.empty_view);
        artistListView = (ListView) rootView.findViewById(R.id.artistListView);
        return rootView;
    }

    private void initData() {
        mArrayList = new ArrayList<HBArtist>();
        mList = new ArrayList<HBArtist>();
        artistListAdapter = new ArtistListAdapter(getActivity(), mArrayList);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        artistListView.setAdapter(artistListAdapter);
        artistListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(getActivity(), HBArtistMusicActivity.class);
                i.putExtra(HBArtistMusicActivity.ARTIST_NAME, mArrayList.get(position).getArtistName());
                i.putExtra(HBArtistMusicActivity.ARTIST_ID, mArrayList.get(position).getArtistId());
                startActivity(i);
            }
        });

        loadArtistListTask = new LoadArtistListTask();
        loadArtistListTask.execute();
    }

    private void initAdapter(Cursor cursor) {
        if (cursor == null) {
            return;
        }
        mList.clear();
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            int mId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID));
            String mTitle = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
            if (mTitle != null && !TextUtils.isEmpty(mTitle)) {
                mTitle = cursor.getString(1);
            } else {
                mTitle = "<unknown>";
            }
            int numOfSong = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS));

            String mPinyin = MusicUtils.getSpell(mTitle);
            HBArtist hbArtist = new HBArtist(mId, mTitle, numOfSong);
            hbArtist.setPinyin(mPinyin);
            mList.add(hbArtist);
        }
        if (cursor != null) {
            cursor.close();
        }
        if (mList.size() == 0)
            return;
        Collections.sort(mList, new Comparator<HBArtist>() {

            @Override
            public int compare(HBArtist lhs, HBArtist rhs) {
                // TODO Auto-generated method stub
                if (lhs.getPinyin().charAt(0) == rhs.getPinyin().charAt(0) && (65 > lhs.getArtistName().toUpperCase().charAt(0) || lhs.getArtistName().toUpperCase().charAt(0) > 90)
                        && (rhs.getArtistName().toUpperCase().charAt(0) <= 90 && rhs.getArtistName().toUpperCase().charAt(0) >= 65)) {
                    return 1;

                } else if (lhs.getPinyin().charAt(0) == rhs.getPinyin().charAt(0) && (65 > rhs.getArtistName().toUpperCase().charAt(0) || rhs.getArtistName().toUpperCase().charAt(0) > 90)
                        && (lhs.getArtistName().toUpperCase().charAt(0) <= 90 && lhs.getArtistName().toUpperCase().charAt(0) >= 65)) {
                    return -1;
                }
                return lhs.getPinyin().compareTo(rhs.getPinyin());
            }
        });

        artistCount = mList.size();
    }

    class LoadArtistListTask extends AsyncTask<Cursor, String, Boolean> {

        private long time;

        @Override
        protected Boolean doInBackground(Cursor... params) {
            // TODO Auto-generated method stub
            String[] mCursorCols = new String[]
                    {MediaStore.Audio.Artists._ID, MediaStore.Audio.Artists.ARTIST,
                            MediaStore.Audio.Artists.NUMBER_OF_TRACKS};

            Cursor cursor = getActivity().getContentResolver().query(uri, mCursorCols, null, null, null);
            initAdapter(cursor);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // TODO Auto-generated method stub
            if (artistListAdapter != null) {
                mArrayList.clear();
                mArrayList.addAll(mList);

                artistListAdapter.notifyDataSetChanged();
            }

            if (mArrayList.size() > 0) {
                emptyView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.VISIBLE);
            }

            if (getActivity() != null && getActivity() instanceof HBLocalMusicActivity) {
                ((HBLocalMusicActivity) getActivity()).setArtistCount(artistCount);
            }

            super.onPostExecute(result);
        }
    }

    private class ArtistListAdapter extends BaseAdapter {

        private Context context;
        private LayoutInflater inflater;
        private ArrayList<HBArtist> dataList;

        public ArtistListAdapter(Context context, ArrayList<HBArtist> list) {
            this.context = context;
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
            HBArtist hbArtist = dataList.get(position);
            Holder holder = null;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.fragment_artist_list_item, null);
                holder = new Holder();
                holder.iv_img = (ImageView) convertView.findViewById(R.id.iv_img);
                holder.tv_artist = (TextView) convertView.findViewById(R.id.tv_artist);
                holder.tv_count = (TextView) convertView.findViewById(R.id.tv_count);
                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }

            holder.iv_img.setImageResource(R.drawable.hb_music_item_defualt);
            holder.tv_count.setText(context.getString(R.string.artist_number_of_songs, hbArtist.getNumberOfTrack()));
            holder.tv_artist.setText(hbArtist.getArtistName());

            return convertView;
        }

        class Holder {
            ImageView iv_img;
            TextView tv_artist;
            TextView tv_count;
        }
    }

}
