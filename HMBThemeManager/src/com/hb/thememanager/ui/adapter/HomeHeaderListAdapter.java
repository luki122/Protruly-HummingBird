package com.hb.thememanager.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.bumptech.glide.Glide;
import com.hb.thememanager.model.Advertising;
import com.hb.thememanager.views.BannerView;
import com.hb.thememanager.views.IconFastEntry;
import com.hb.thememanager.R;

import java.util.ArrayList;
import java.util.List;


/**
 * 主题包首页Adapter
 */
public class HomeHeaderListAdapter extends BaseAdapter {

	private Context mContext;

	private LayoutInflater mInflater;
	private int mThemeType;
	private List<Advertising> mAds;

	public HomeHeaderListAdapter(Context context) {
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mAds = new ArrayList<>();
	}

	public void setThemeType(int type){
		mThemeType = type;
	}

	public void setAdvertisings(List<Advertising> data){
		mAds.clear();
		mAds.addAll(data);
	}

	@Override
	public int getCount() {
		return mAds.size() > 0 ? 1 : 0;
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
		ViewHolder viewHolder = null;
		if(convertView == null){
			convertView = mInflater.inflate(R.layout.list_header_home, parent, false);
			viewHolder = new ViewHolder();
			viewHolder.mBannerView = (BannerView) convertView.findViewById(R.id.banner);
			viewHolder.mRankIcon = (IconFastEntry) convertView.findViewById(R.id.icon_rank);
			viewHolder.mCategoryIcon = (IconFastEntry) convertView.findViewById(R.id.icon_category);
			viewHolder.mTopicIcon = (IconFastEntry) convertView.findViewById(R.id.icon_topic);
			convertView.setTag(viewHolder);
		}else{
			viewHolder = (ViewHolder) convertView.getTag();
		}

		viewHolder.mBannerView.bindData(mAds);
		viewHolder.mBannerView.startScroll();


		viewHolder.mRankIcon.setThemeType(mThemeType);
		viewHolder.mCategoryIcon.setThemeType(mThemeType);
		viewHolder.mTopicIcon.setThemeType(mThemeType);

		return convertView;
	}

	private static class ViewHolder{
		public BannerView mBannerView;
		public IconFastEntry mRankIcon;
		public IconFastEntry mCategoryIcon;
		public IconFastEntry mTopicIcon;
	}
}
