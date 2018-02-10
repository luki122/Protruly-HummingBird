package com.hb.thememanager.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.hb.thememanager.R;
import com.hb.thememanager.model.Advertising;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.ui.LoadMoreActivity;
import com.hb.thememanager.ui.TopicDetailActivity;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.TLog;

import hb.widget.ViewPager;
import hb.widget.PagerAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BannerAdapter extends PagerAdapter{
    private final static String TAG = "BannerAdapter";
    private Context mContext;
    private ArrayList<Advertising> mData;
    private HashMap<Integer,View> mViews;

    public BannerAdapter(Context context){
        mContext = context;
        mViews = new HashMap<Integer,View>();
        mData = new ArrayList<>();
    }

    public void setData(List<Advertising> data){
        mData.clear();
        mData.addAll(data);
    }

    @Override
    public int getCount() {
        if(mData != null) {
            return mData.size() + 2;
        } else {
            return 0;
        }
    }

    public boolean isEnd(int index){
        if(mData != null) {
            return index == mData.size() + 1;
        }else{
            return false;
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View convert = mViews.get(position);
        Log.e(TAG,"instantiateItem -> position = "+position+" ; convert = "+convert);
        if(convert == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convert = inflater.inflate(R.layout.banner_item, container, false);
            container.addView(convert);
            if(position == 1 || position == mData.size()) {
                mViews.put(position, convert);
            }
        }
        int dataSize = mData.size();
        int index = position == 0 ? dataSize - 1 : (position-1) % dataSize;
        final Advertising adv = mData.get(index);
        try {
            ImageView image = (ImageView) convert.findViewById(R.id.banner_image);
            Glide.with(mContext).load(adv.getIcon()).into(image);
        }catch (Throwable t){
            t.printStackTrace();
        }
        convert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent;
                if(adv.getType() == Advertising.TYPE_HOT_RECOMMEND) {
                    intent = new Intent(view.getContext(), LoadMoreActivity.class);
                }else{
                    intent = new Intent(view.getContext(), TopicDetailActivity.class);
                }
                intent.putExtra(Config.ActionKey.KEY_ADV_DETAIL,adv);
                view.getContext().startActivity(intent);
            }
        });
        return convert;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        Log.e(TAG,"destroyItem -> position = "+position);
        if(object instanceof View && (position != 1 && position != mData.size())) {
            container.removeView((View)object);
        }
    }
}