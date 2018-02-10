package com.hb.thememanager.ui.adapter;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.hb.thememanager.R;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.IntentUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 主界面中各个Tab中的数据列表的Adapter需要继承自这个类实现
 * 自己的具体逻辑
 *
 */
public class MyThemeAdapter extends BaseAdapter {

	private static final int THEME_LINE_COUNT = 3;
	private static final int WALLPAPER_LINE_COUNT = 3;
	private static final int FONTS_LINE_COUNT = 2;

	private Context mContext;
	private List<Theme> mData;
	private int mType = Theme.THEME_PKG;

	public MyThemeAdapter(Context context) {
		mContext = context;
		mData = new ArrayList<>();
	}

	public void setData(List<Theme> data){
		mData.clear();
		mData.addAll(data);
	}
	public void addData(List<Theme> data){
		mData.addAll(data);
	}
	public void addData(Theme data){
		mData.add(data);
	}

	public void setType(int type){
		mType = type;
	}

	@Override
	public int getCount() {
		if(mData != null && mData != null){
			int size = mData.size();
			if(mType == Theme.THEME_PKG) {
				return size / THEME_LINE_COUNT + (size % THEME_LINE_COUNT == 0 ? 0 : 1);
			} else if (mType == Theme.RINGTONE){
				return 0;
			} else if (mType == Theme.WALLPAPER){
				return size / WALLPAPER_LINE_COUNT + (size % WALLPAPER_LINE_COUNT == 0 ? 0 : 1);
			} else if (mType == Theme.FONTS){
				return size / FONTS_LINE_COUNT + (size % FONTS_LINE_COUNT == 0 ? 0 : 1);
			}
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
		if(convertView == null){
			int resid = 0;
			switch(mType){
				case Theme.THEME_PKG:
					break;
				case Theme.RINGTONE:
					break;
				case Theme.WALLPAPER:
					break;
				case Theme.FONTS:
					resid = R.layout.list_item_my_fonts;
					break;
			}
			if(resid != 0) {
				LayoutInflater inflater = LayoutInflater.from(mContext);
				convertView = inflater.inflate(resid,parent,false);
			}
			holdView(convertView);
		}
		bindView(position, convertView);
		return convertView;
	}

	private void holdView(View container){
		switch(mType){
			case Theme.THEME_PKG: {
				break;
			}
			case Theme.RINGTONE:

				break;
			case Theme.WALLPAPER: {
				break;
			}
			case Theme.FONTS: {
				FontsViewHolder viewHolder = new FontsViewHolder();
				viewHolder.item1 = container.findViewById(R.id.list_item1);
				viewHolder.item2 = container.findViewById(R.id.list_item2);
				viewHolder.applyImage1 = (ImageView) container.findViewById(R.id.apply_image1);
				viewHolder.applyImage2 = (ImageView) container.findViewById(R.id.apply_image2);
				viewHolder.image1 = (ImageView) viewHolder.item1.findViewById(R.id.theme_cover);
				viewHolder.name1 = (TextView) viewHolder.item1.findViewById(R.id.theme_name);
				viewHolder.price1 = (TextView) viewHolder.item1.findViewById(R.id.theme_price);
				viewHolder.image2 = (ImageView) viewHolder.item2.findViewById(R.id.theme_cover);
				viewHolder.name2 = (TextView) viewHolder.item2.findViewById(R.id.theme_name);
				viewHolder.price2 = (TextView) viewHolder.item2.findViewById(R.id.theme_price);
				container.setTag(viewHolder);
				break;
			}
		}

	}

	private void setPrice(String price,TextView priceTextView){
		if(!TextUtils.isEmpty(price)){
			priceTextView.setText(priceTextView.getContext().getString(R.string.theme_price_suffix,price));
		}else{
			priceTextView.setText(priceTextView.getContext().getString(R.string.theme_price_free));
		}
	}

	private void bindView(final int position, View convertView){
		switch(mType){
			case Theme.THEME_PKG: {
				break;
			}
			case Theme.RINGTONE: {
				break;
			}
			case Theme.WALLPAPER: {
				break;
			}
			case Theme.FONTS: {
				FontsViewHolder viewHolder = (FontsViewHolder) convertView.getTag();
				viewHolder.item1.setVisibility(View.INVISIBLE);
				viewHolder.item2.setVisibility(View.INVISIBLE);
				int lineCount = 0;
				if (position == getCount() - 1) {
					int mod = mData.size() % FONTS_LINE_COUNT;
					lineCount = mod == 0 ? FONTS_LINE_COUNT : mod;
				} else {
					lineCount = FONTS_LINE_COUNT;
				}
				for(int i = 0; i < lineCount; i++){
					final Theme theme = mData.get(position * FONTS_LINE_COUNT + i);
					if(i == 0){
						viewHolder.item1.setVisibility(View.VISIBLE);
						Glide.with(mContext).load(theme.coverUrl).into(viewHolder.image1);
						viewHolder.name1.setText(theme.name);
						setPrice(theme.price,viewHolder.price1);
						viewHolder.item1.setOnClickListener(new View.OnClickListener(){
							@Override
							public void onClick(View view) {
								theme.type = Theme.THEME_PKG;
								Intent intent = IntentUtils.buildHomeThemeListIntent(Config.Action.ACTION_HOME_THEME_LIST_ITEM_DETAIL, theme);
								mContext.startActivity(intent);
							}
						});
					}else if(i == 1){
						viewHolder.item2.setVisibility(View.VISIBLE);
						Glide.with(mContext).load(theme.coverUrl).into(viewHolder.image2);
						viewHolder.name2.setText(theme.name);
						setPrice(theme.price,viewHolder.price2);
						viewHolder.item2.setOnClickListener(new View.OnClickListener(){
							@Override
							public void onClick(View view) {
								theme.type = Theme.THEME_PKG;
								Intent intent = IntentUtils.buildHomeThemeListIntent(Config.Action.ACTION_HOME_THEME_LIST_ITEM_DETAIL, theme);
								mContext.startActivity(intent);
							}
						});
					}
				}
				break;
			}
		}
	}

	private static class FontsViewHolder{
		public View item1;
		public View item2;
		public ImageView applyImage1;
		public ImageView applyImage2;
		public ImageView image1;
		public TextView name1;
		public TextView price1;
		public ImageView image2;
		public TextView name2;
		public TextView price2;
	}


}
