package com.protruly.music.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.protruly.music.R;
import com.protruly.music.util.LogUtil;
import com.xiami.sdk.entities.Banner;

import java.util.List;
import hb.widget.LinearLayout;

/**
 * Created by hujianwei on 17-9-4.
 */

public class ImagePagerAdapter extends RecyclingPagerAdapter {

    private static final String TAG = "ImagePagerAdapter";
    private Context context;
    private List<Banner> imageIdList;
    private OnBannerClickListener mOnBannerClickListener;
    private int           size;
    private boolean       isInfiniteLoop;

    public static int MAXNUM =5;
    private DisplayImageOptions options;

    public ImagePagerAdapter(Context context, List<Banner> imageIdList,OnBannerClickListener l) {
        this.context = context;
        this.imageIdList = imageIdList;
        if(imageIdList.size()>MAXNUM){
            this.size = MAXNUM;
        }else{
            this.size = imageIdList.size();
        }
        mOnBannerClickListener=l;
        isInfiniteLoop = false;
        initImageCacheParams(context);
    }

    @Override
    public int getCount() {
        // Infinite loop
        return isInfiniteLoop ? Integer.MAX_VALUE : size;
    }

    /**
     * get really position
     *
     * @param position
     * @return
     */
    private int getPosition(int position) {
        return isInfiniteLoop ? position % size : position;
    }

    @Override
    public View getView(int position, View view, ViewGroup container) {
        ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view = LayoutInflater.from(context).inflate(R.layout.hb_online_banner_layout, null);
            holder.imageView=(ImageView)view.findViewById(R.id.hb_img);
            view.setTag(holder);
        } else {
            holder = (ViewHolder)view.getTag();
        }
        final Banner item = imageIdList.get(getPosition(position));
        ((LinearLayout)view).setEnabled(true);
        view.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                LogUtil.d(TAG, "imageview clicked!!");
                if(mOnBannerClickListener!=null){
                    mOnBannerClickListener.onBannerClick(item);
                }
            }
        });
        ImageLoader.getInstance().displayImage(item.getImageUrl(), holder.imageView, options);
        return view;
    }

    private static class ViewHolder {

        ImageView imageView;
    }

    /**
     * @return the isInfiniteLoop
     */
    public boolean isInfiniteLoop() {
        return isInfiniteLoop;
    }

    /**
     * @param isInfiniteLoop the isInfiniteLoop to set
     */
    public ImagePagerAdapter setInfiniteLoop(boolean isInfiniteLoop) {
        this.isInfiniteLoop = isInfiniteLoop;
        return this;
    }

    public interface OnBannerClickListener {
        public void onBannerClick(Banner item);
    }

    private void initImageCacheParams(Context context) {
        options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.hb_online_music_defualt)
                .showImageForEmptyUri(R.drawable.hb_online_music_defualt)
                .showImageOnFail(R.drawable.hb_online_music_defualt)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .displayer(new SimpleBitmapDisplayer())
                .build();
    }

}
