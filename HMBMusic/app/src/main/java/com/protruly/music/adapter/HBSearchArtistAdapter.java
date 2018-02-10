package com.protruly.music.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.protruly.music.R;
import com.protruly.music.cache.HBHttpAsyncTask;
import com.protruly.music.model.XiaMiSdkUtils;
import com.protruly.music.util.HBMusicUtil;

import com.xiami.sdk.entities.OnlineArtist;

import java.util.HashMap;
import java.util.List;

/**
 * Created by hujianwei on 17-9-4.
 */

public class HBSearchArtistAdapter extends BaseAdapter {
    private List<OnlineArtist> mList;
    private Context mContext;
    private int mImageSize;
    private String IMAGE_CACHE_DIR = "NetAlbum";
    DisplayImageOptions mOptions;
    OnlineArtist item;
    private final String SONG_COUNT = "song_count";
    private final String ALBUM_COUNT = "album_count";
    class ViewHolder {
        ImageView mIcon;
        TextView mName;
        TextView mInfo;
    }

    private HashMap<Long, HashMap<String, Integer>> mCacheList = new HashMap<Long, HashMap<String, Integer>>();

    public HBSearchArtistAdapter(Context context, List<OnlineArtist> list) {
        mList = list;
        mContext = context;
        mImageSize = (int) mContext.getResources().getDimension(
                R.dimen.hb_netartist_size);
        initImageCacheParams();
    }

    private void initImageCacheParams() {
        mOptions = new DisplayImageOptions.Builder()
                .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)
                .cacheInMemory(true).cacheOnDisk(true)
                .showImageOnLoading(R.drawable.hb_lyric_select_uncheked)
                .displayer(new RoundedBitmapDisplayer((int) mImageSize / 2, 0))
                .build();
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return mList.size();
    }

    @Override
    public Object getItem(int i) {
        // TODO Auto-generated method stub
        return mList.get(i);
    }

    @Override
    public long getItemId(int i) {
        // TODO Auto-generated method stub
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewgroup) {
        // TODO Auto-generated method stub
        final ViewHolder vh;
        if (convertView == null) {
            vh = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(
                    R.layout.hb_search_artist, null);
            vh.mIcon = (ImageView) convertView
                    .findViewById(R.id.hb_netartist_icon);
            vh.mName = (TextView) convertView
                    .findViewById(R.id.hb_artist_name);
            vh.mInfo = (TextView) convertView
                    .findViewById(R.id.hb_artist_info);
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }
        try {
            item = mList.get(position);
        } catch (Exception e) {
        }
        if (item == null) {
            return convertView;
        }
        if (!TextUtils.isEmpty(item.getImageUrl(mImageSize))) {
            ImageLoader.getInstance().displayImage(
                    item.getImageUrl(mImageSize), vh.mIcon, mOptions);
        }
        if (!TextUtils.isEmpty(item.getName())) {
            vh.mName.setText(item.getName());
        }
        final HashMap<String, Integer> hashMap;
        if (null != mCacheList.get(item.getId())) {
            hashMap = mCacheList.get(item.getId());
        } else {
            hashMap = null;
        }
        vh.mInfo.setTag(item.getId());
        if (hashMap == null) {
            HBHttpAsyncTask<Integer, Integer, HashMap<String, Integer>> task = new HBHttpAsyncTask<Integer, Integer, HashMap<String, Integer>>() {
                @Override
                protected HashMap<String, Integer> doInBackground(
                        Integer... params) {
                    // TODO Auto-generated method stub
                    HashMap<String, Integer> tmpHashMap = XiaMiSdkUtils.fetchArtistCountInfoSync(
                            mContext,getKey());
                    return tmpHashMap;
                }

                @Override
                protected void onPostExecute(HashMap<String, Integer> result) {
                    // TODO Auto-generated method stub
                    if (getKey().equals(getTextView().getTag())) {
                        if (result == null) {
                            getTextView()
                                    .setText(
                                            mContext.getResources()
                                                    .getString(
                                                            R.string.num_songs_num_albums,
                                                            0, 0));
                            HashMap<String, Integer> map = new HashMap<String, Integer>();
                            map.put(SONG_COUNT, 0);
                            map.put(ALBUM_COUNT, 0);
                            mCacheList.put(getKey(), map);
                            return;
                        }
                        mCacheList.put(getKey(), result);
                        int song_count = mCacheList.get(getKey()).get(
                                SONG_COUNT);
                        int album_count = mCacheList.get(getKey()).get(
                                ALBUM_COUNT);
                        setArtistInfo(getTextView(), song_count, album_count);
                    }
                }
            };
            task.setKey(item.getId());
            task.setTextView(vh.mInfo);
            task.executeOnExecutor(HBHttpAsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            setArtistInfo(vh.mInfo, hashMap.get(SONG_COUNT),
                    hashMap.get(ALBUM_COUNT));
        }
        return convertView;
    }

    private void setArtistInfo(TextView textView, int song_count,
                               int album_count) {
        if (song_count < 0) {
            song_count = 0;
        }
        if (album_count < 0) {
            album_count = 0;
        }
        textView.setText(mContext.getResources().getString(
                R.string.num_songs_num_albums, song_count, album_count));
    }

    public void clearCacheList() {
        mCacheList.clear();
    }
}
