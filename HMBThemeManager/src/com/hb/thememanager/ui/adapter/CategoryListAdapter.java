package com.hb.thememanager.ui.adapter;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.hb.thememanager.R;
import com.hb.thememanager.http.response.CategoryResponse;
import com.hb.thememanager.model.Category;
import com.hb.thememanager.model.RankingCategory;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.ui.CategoryDetailActivity;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.IntentUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 主界面中各个Tab中的数据列表的Adapter需要继承自这个类实现
 * 自己的具体逻辑
 *
 */
public class CategoryListAdapter extends BaseAdapter {

	private static final int LINE_COUNT = 3;

	private Context mContext;
	private List<Category> mData;
	private int mType;
	public CategoryListAdapter(Context context, int type) {
		mContext = context;
		mData = new ArrayList<>();
		mType = type;
	}

	public void setData(List<Category> data){
		mData.clear();
		if(data != null) {
			mData.addAll(data);
		}
	}

	public void addData(List<Category> data){
		if(data != null) {
			mData.addAll(data);
		}
	}


	@Override
	public int getCount() {
		if(mData != null && mData != null){
			int size = mData.size();
			return size / LINE_COUNT + (size % LINE_COUNT == 0 ? 0 : 1);
		}
		return 0;
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
		int type = getItemViewType(position);
		MyViewHolder viewholder;
		if(convertView == null){
			LayoutInflater inflater = LayoutInflater.from(mContext);
			convertView = inflater.inflate(R.layout.list_item_category,parent,false);
			viewholder = new MyViewHolder();
			holdView(viewholder, convertView, type);
			convertView.setTag(viewholder);
		}else {
			viewholder = (MyViewHolder) convertView.getTag();
		}
		bindView(viewholder, type, position);
		return convertView;
	}

	private void holdView(MyViewHolder viewholder, View container, int type){
		viewholder.container1 = container.findViewById(R.id.category_list_item1);
		viewholder.container2 = container.findViewById(R.id.category_list_item2);
		viewholder.container3 = container.findViewById(R.id.category_list_item3);
		viewholder.imageView1 = (ImageView) viewholder.container1.findViewById(R.id.image);
		viewholder.titleView1 = (TextView) viewholder.container1.findViewById(R.id.title);
		viewholder.imageView2 = (ImageView) viewholder.container2.findViewById(R.id.image);
		viewholder.titleView2 = (TextView) viewholder.container2.findViewById(R.id.title);
		viewholder.imageView3 = (ImageView) viewholder.container3.findViewById(R.id.image);
		viewholder.titleView3 = (TextView) viewholder.container3.findViewById(R.id.title);
	}

	private void initView(MyViewHolder viewholder, int type, int position){
		viewholder.container1.setVisibility(View.INVISIBLE);
		viewholder.container2.setVisibility(View.INVISIBLE);
		viewholder.container3.setVisibility(View.INVISIBLE);
	}
	private void bindView(MyViewHolder viewholder, int type, int position){
		initView(viewholder, type, position);
		int lineCount = 0;
		if (position == getCount() - 1) {
			int mod = mData.size() % LINE_COUNT;
			lineCount = mod == 0 ? LINE_COUNT : mod;
		} else {
			lineCount = LINE_COUNT;
		}
		for(int i = 0; i < lineCount; i++){
			final Category category = mData.get(position * LINE_COUNT + i);
			View themeView = null;
			if(i == 0){
				themeView = viewholder.container1;
				viewholder.container1.setVisibility(View.VISIBLE);
				Glide.with(mContext).load(category.icon).into(viewholder.imageView1);
				viewholder.titleView1.setText(category.name);
			}else if(i == 1){
				themeView = viewholder.container2;
				viewholder.container2.setVisibility(View.VISIBLE);
				Glide.with(mContext).load(category.icon).into(viewholder.imageView2);
				viewholder.titleView2.setText(category.name);
			}else if(i == 2){
				themeView = viewholder.container3;
				viewholder.container3.setVisibility(View.VISIBLE);
				Glide.with(mContext).load(category.icon).into(viewholder.imageView3);
				viewholder.titleView3.setText(category.name);
			}
			themeView.setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View view) {
					// TODO Auto-generated method stub
					Bundle values = new Bundle();
					values.putString(Config.ActionKey.KEY_JUMP_TITLE,category.name);
					values.putInt(Config.ActionKey.KEY_FAST_ENTRY,mType);
					values.putInt(Config.ActionKey.KEY_CATEGORY_ID,category.id);
					Intent intent = new Intent(mContext, CategoryDetailActivity.class);
					intent.putExtras(values);
					mContext.startActivity(intent);
				}
			});
		}
	}


	class MyViewHolder{
		public View container1;
		public View container2;
		public View container3;

		public ImageView imageView1;
		public TextView titleView1;
		public ImageView imageView2;
		public TextView titleView2;
		public ImageView imageView3;
		public TextView titleView3;

	}






}
