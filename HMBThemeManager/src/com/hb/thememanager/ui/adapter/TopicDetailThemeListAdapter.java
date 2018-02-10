package com.hb.thememanager.ui.adapter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.bumptech.glide.Glide;
import com.hb.thememanager.http.response.TopicThemeResponse;
import com.hb.thememanager.model.HomeThemeCategory;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.TopicDetail;
import com.hb.thememanager.model.TopicDetailHeader;
import com.hb.thememanager.utils.FileUtils;
import com.hb.thememanager.R;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.LinearLayout;

public class TopicDetailThemeListAdapter extends BaseAdapter {
	private static final int LAYOUT_RES_TOPIC_DETAIL_THEME = R.layout.list_item_topic_detail_theme;
	private static final int LAYOUT_RES_TOPIC_DETAIL_WALLPAPER = R.layout.list_item_home_wallpaper_child;
	private static final int LAYOUT_RES_TOPIC_DETAIL_RINGTONE = R.layout.list_item_topic_detail_ringtone;
	private static final int LAYOUT_RES_TOPIC_DETAIL_FONT = R.layout.list_item_home_font_child;
	private Context mContext;
	private List<TopicDetail> mData;
	private int mType;

	public  TopicDetailThemeListAdapter(Context context, int type) {
		mType = type;
		mContext = context;
		mData = new ArrayList<>();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TopicDetailViewHolder holder  = null;
		if(convertView == null){
			switch(mType) {
				case Theme.THEME_PKG:
					convertView = LayoutInflater.from(mContext).inflate(LAYOUT_RES_TOPIC_DETAIL_THEME, null);
					break;
				case Theme.WALLPAPER:
					convertView = LayoutInflater.from(mContext).inflate(LAYOUT_RES_TOPIC_DETAIL_WALLPAPER, null);
					break;
				case Theme.RINGTONE:
					convertView = LayoutInflater.from(mContext).inflate(LAYOUT_RES_TOPIC_DETAIL_RINGTONE, null);
					break;
				case Theme.FONTS:
					convertView = LayoutInflater.from(mContext).inflate(LAYOUT_RES_TOPIC_DETAIL_FONT, null);
					break;
			}
			
			holder = new TopicDetailViewHolder(mContext, this);
			holder.holdConvertView(convertView);
			convertView.setTag(holder);
		}else{
			holder = (TopicDetailViewHolder)convertView.getTag();
		}
		holder.bindDatas(position, mData);
		return convertView;
	}
	
	public void addTopicBody(List<TopicDetail> data){
		synchronized (mData) {
			if(!mData.contains(data)){
				mData.addAll(data);
				notifyDataSetChanged();
			}
		}
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mData.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return mData.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}
	

	class TopicDetailViewHolder extends AbsViewHolder<TopicDetail> {
		private ImageView mPicture;
		private TextView mTitle;
		private TextView mPrice;
		private TextView mMusicTitle;
		private TextView mMusicSinger;

		public TopicDetailViewHolder(Context context, ListAdapter adapter) {
			super(context, adapter);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void holdConvertView(View convertView) {
			switch(mType) {
				case Theme.THEME_PKG:
					mPicture = (ImageView)convertView.findViewById(R.id.topic_detail_theme_cover);
					mTitle = (TextView)convertView.findViewById(R.id.topic_detail_theme_name);
					mPrice = (TextView)convertView.findViewById(R.id.topic_detail_theme_price);
					break;
				case Theme.WALLPAPER:
					mPicture = (ImageView)convertView.findViewById(R.id.theme_cover);
					break;
				case Theme.RINGTONE:
					mTitle = (TextView)convertView.findViewById(R.id.topic_detail_ringtone_title);
					mPrice = (TextView)convertView.findViewById(R.id.topic_detail_ringtone_singer);
					break;
				case Theme.FONTS:
					mPicture = (ImageView)convertView.findViewById(R.id.theme_cover);
					mTitle = (TextView)convertView.findViewById(R.id.theme_name);
					mPrice = (TextView)convertView.findViewById(R.id.theme_price);
					break;
			}
		}

		@Override
		public void bindDatas(int position, List<TopicDetail> datas) {
			switch(mType) {
				case Theme.THEME_PKG:
					Glide.with(mContext).load(datas.get(position).getIcon()).into(mPicture);
					mTitle.setText(datas.get(position).getName());
					mPrice.setText("¥ " + datas.get(position).getPrice());
					break;
				case Theme.WALLPAPER:
					Glide.with(mContext).load(datas.get(position).getIcon()).into(mPicture);
					break;
				case Theme.RINGTONE:
//					mTitle.setText(datas.get(position).getName());
//					mPrice.setText(datas.get(position).getPrice());
					break;
				case Theme.FONTS:
					Glide.with(mContext).load(datas.get(position).getIcon()).into(mPicture);
					mTitle.setText(datas.get(position).getName());
					mPrice.setText("¥ " + datas.get(position).getPrice());
					break;
			}
		}
	}
}


