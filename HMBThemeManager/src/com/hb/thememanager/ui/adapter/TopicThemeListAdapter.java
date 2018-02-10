package com.hb.thememanager.ui.adapter;

import java.util.ArrayList;
import java.util.List;

import com.bumptech.glide.Glide;
import com.hb.thememanager.http.response.TopicThemeResponse;
import com.hb.thememanager.model.HomeThemeCategory;
import com.hb.thememanager.model.Topic;
import com.hb.thememanager.views.RoundRectImageView;
import com.hb.thememanager.R;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.LinearLayout;

public class TopicThemeListAdapter extends BaseAdapter {
	private static final int LAYOUT_RES_TOPIC = R.layout.list_item_topic;
	private Context mContext;
	private List<Topic> mData;

	public  TopicThemeListAdapter(Context context) {
		mContext = context;
		mData = new ArrayList<Topic>();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TopicViewHolder holder  = null;
		if(convertView == null){
			convertView = LayoutInflater.from(mContext).inflate(LAYOUT_RES_TOPIC, null);
			
			holder = new TopicViewHolder(mContext, this);
			holder.holdConvertView(convertView);
			convertView.setTag(holder);
		}else{
			holder = (TopicViewHolder)convertView.getTag();
		}
		holder.bindDatas(position, mData);
		return convertView;
	}

	public void addTopics(List<Topic> data){
		synchronized (mData) {
			mData.addAll(data);
			notifyDataSetChanged();
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

	public void removeAll(){
		synchronized (mData){
			mData.clear();
			notifyDataSetChanged();
		}
	}


	
	class TopicViewHolder extends AbsViewHolder<Topic> {
		private RoundRectImageView mCover;

		public TopicViewHolder(Context context, ListAdapter adapter) {
			super(context, adapter);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void holdConvertView(View convertView) {
			mCover = (RoundRectImageView)convertView.findViewById(R.id.topic_item_cover);
		}

		@Override
		public void bindDatas(int position, List<Topic> themes) {
			Glide.with(mContext).load(themes.get(position).getIcon()).into(mCover);
		}
	}
}

