package com.protruly.music.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.protruly.music.R;
import com.protruly.music.cache.HBHttpAsyncTask;
import com.protruly.music.model.XiaMiSdkUtils;
import com.xiami.sdk.entities.OnlineAlbum;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by hujianwei on 17-9-4.
 */

public class HBSearchAlbumAdapter extends BaseAdapter {
    private ArrayList<OnlineAlbum> mList;
    private Context mContext;
    private int mImageSize;
    private String IMAGE_CACHE_DIR = "NetAlbum";
    DisplayImageOptions mOptions;
    private final String SONG_COUNT = "song_count";
    private HashMap<Long, HashMap<String, Integer>> mCacheList = new HashMap<Long, HashMap<String, Integer>>();
    class ViewHolder{
        ImageView mIcon;
        TextView mName;
        TextView mCount;
        TextView mDate;
    }
    public HBSearchAlbumAdapter(Context context,ArrayList<OnlineAlbum> list){
        mList = list;
        mContext = context;
        mImageSize = (int)mContext.getResources().getDimension(R.dimen.hb_album_cover_width);
        initImageCacheParams();
    }
    private void initImageCacheParams() {
        mOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true).cacheOnDisk(true).showImageOnLoading(R.drawable.hb_online_recommend_default)
                .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)
                .displayer(new SimpleBitmapDisplayer()).build();
    }
    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return mList.size();
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
    public View getView(int position, View convertView, ViewGroup arg2) {
        // TODO Auto-generated method stub
        final ViewHolder vh;
        if(convertView == null){
            vh = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.hb_search_album, null);
            vh.mIcon = (ImageView)convertView.findViewById(R.id.album_art);
            vh.mName = (TextView)convertView.findViewById(R.id.album_name);
            vh.mCount = (TextView)convertView.findViewById(R.id.album_numtrack);
            vh.mDate = (TextView)convertView.findViewById(R.id.album_release_date);
            convertView.setTag(vh);
        }else{
            vh = (ViewHolder)convertView.getTag();
        }
        OnlineAlbum item = null;
        try {
            item = mList.get(position);
        } catch (Exception e) {
            // TODO: handle exception
        }
        if(item==null){
            return convertView;
        }
        final String pic = item.getImageUrl(220);
        if(!TextUtils.isEmpty(pic)&&!pic.equals((String)vh.mIcon.getTag())){
            ImageLoader.getInstance().displayImage(pic,vh.mIcon, mOptions,new ImageLoadingListener() {

                @Override
                public void onLoadingStarted(String arg0, View arg1) {
                    // TODO Auto-generated method stub
                    vh.mIcon.setTag(pic);
                }

                @Override
                public void onLoadingFailed(String arg0, View arg1, FailReason arg2) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onLoadingComplete(String arg0, View arg1, Bitmap arg2) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onLoadingCancelled(String arg0, View arg1) {
                    // TODO Auto-generated method stub

                }
            });
        }
        if(item.getPublishTime()>0){
            Date date = new Date(item.getPublishTime()*1000);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String time = sdf.format(date);
            vh.mDate.setText(mContext.getResources().getString(R.string.release_date_of_album, time));
        }
        final HashMap<String, Integer> hashMap;
        if (null != mCacheList.get(item.getAlbumId())) {
            hashMap = mCacheList.get(item.getAlbumId());
        } else {
            hashMap = null;
        }
        vh.mCount.setTag(item.getAlbumId());
        if (hashMap == null) {
            HBHttpAsyncTask<Integer, Integer, Integer> task = new HBHttpAsyncTask<Integer, Integer, Integer>() {
                @Override
                protected Integer doInBackground(
                        Integer... params) {
                    // TODO Auto-generated method stub
                    OnlineAlbum album = XiaMiSdkUtils.getAlbumsDetailSync(mContext,getKey());
                    if(null==album){
                        return 0;
                    }
                    return album.getSongCount();
                }

                @Override
                protected void onPostExecute(Integer result) {
                    // TODO Auto-generated method stub
                    if (getKey().equals(getTextView().getTag())) {
                        HashMap<String, Integer> tmMap = new HashMap<String, Integer>();
                        tmMap.put(SONG_COUNT, result);
                        mCacheList.put(getKey(), tmMap);
                        int song_count = mCacheList.get(getKey()).get(
                                SONG_COUNT);
                        setAlbumInfo(getTextView(), song_count);
                    }
                }
            };
            task.setKey(item.getAlbumId());
            task.setTextView(vh.mCount);
            task.executeOnExecutor(HBHttpAsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            setAlbumInfo(vh.mCount, hashMap.get(SONG_COUNT));
        }
        if(!TextUtils.isEmpty(item.getAlbumName())){
            vh.mName.setText(item.getAlbumName());
        }
        return convertView;
    }
    private void setAlbumInfo(TextView textView, int song_count) {
        if (song_count < 0) {
            song_count = 0;
        }
        textView.setText(mContext.getResources().getString(
                R.string.hb_num_songs_of_single, song_count));
    }
}
