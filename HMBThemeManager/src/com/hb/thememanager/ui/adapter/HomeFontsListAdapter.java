package com.hb.thememanager.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.hb.thememanager.R;
import com.hb.thememanager.model.Fonts;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.ui.LoadMoreActivity;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.IntentUtils;
import com.hb.thememanager.views.HomeThemeListItem;

import java.util.ArrayList;
import java.util.List;


/**
 * 主题包首页Adapter
 */
public class HomeFontsListAdapter extends HFListAdapter {

	private static final int LINE_COUNT = 2;
	private Context mContext;

	private List<Theme> mData;
	private LayoutInflater mInflater;
	private String mTitle;

	public HomeFontsListAdapter(Context context) {
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mData = new ArrayList<>();
		showHeaderView(true);
	}

	public void setData(List<Theme> data){
		mData.clear();
		mData.addAll(data);
	}

	public void addData(Theme data){
		mData.add(data);
	}

	public void addData(List<Theme> data){
		mData.addAll(data);
	}

	public void setHeaderTitle(String title){
		mTitle = title;
	}

	@Override
	public int getBodyCount() {
		if(mData != null){
			int size = mData.size();
			return size / LINE_COUNT + (size % LINE_COUNT == 0 ? 0 : 1);
		}
		return 0;
	}

	@Override
	public View createHeaderView(ViewGroup parent) {
		View convert = mInflater.inflate(R.layout.theme_list_title_bar, parent, false);
		HeaderViewHolder viewHolder = new HeaderViewHolder();
		viewHolder.title = (TextView) convert.findViewById(R.id.home_theme_category_title);
		viewHolder.more = (TextView) convert.findViewById(R.id.home_theme_category_more);
		viewHolder.separate = convert.findViewById(R.id.separate);
		convert.setTag(viewHolder);
		return convert;
	}

	@Override
	public void bindHeaderView(final View convertView, ViewGroup parent) {
		HeaderViewHolder viewHolder = (HeaderViewHolder) convertView.getTag();
		viewHolder.separate.setVisibility(View.VISIBLE);
		viewHolder.title.setText(mTitle);
		if(mData == null || mData.size() < 6){
			viewHolder.more.setVisibility(View.GONE);
		}else {
			viewHolder.more.setVisibility(View.VISIBLE);
			viewHolder.more.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(convertView.getContext(), LoadMoreActivity.class);
					intent.putExtra(Config.ActionKey.KEY_LOAD_MORE_TYPE, Theme.FONTS);
					intent.putExtra(Config.ActionKey.KEY_LOAD_MORE_NAME, mTitle);
					ArrayList<Theme> themes = new ArrayList<Theme>();
					themes.addAll(mData);
					intent.putParcelableArrayListExtra(Config.ActionKey.KEY_LOAD_MORE_LIST, themes);
					convertView.getContext().startActivity(intent);
				}
			});
		}
	}

	@Override
	public View createBodyView(int position, ViewGroup parent) {
		View convert = mInflater.inflate(R.layout.list_item_home_font, parent, false);
		BodyViewHolder viewHolder = new BodyViewHolder();
		viewHolder.item1 = convert.findViewById(R.id.font_list_item1);
		viewHolder.item2 = convert.findViewById(R.id.font_list_item2);
		viewHolder.image1 = (ImageView) viewHolder.item1.findViewById(R.id.theme_cover);
		viewHolder.name1 = (TextView) viewHolder.item1.findViewById(R.id.theme_name);
		viewHolder.price1 = (TextView) viewHolder.item1.findViewById(R.id.theme_price);
		viewHolder.image2 = (ImageView) viewHolder.item2.findViewById(R.id.theme_cover);
		viewHolder.name2 = (TextView) viewHolder.item2.findViewById(R.id.theme_name);
		viewHolder.price2 = (TextView) viewHolder.item2.findViewById(R.id.theme_price);
		convert.setTag(viewHolder);
		return convert;
	}

	@Override
	public void bindBodyView(int position, View convertView, ViewGroup parent) {
		BodyViewHolder viewHolder = (BodyViewHolder) convertView.getTag();
		viewHolder.item1.setVisibility(View.INVISIBLE);
		viewHolder.item2.setVisibility(View.INVISIBLE);
		int lineCount = 0;
		if (position == getBodyCount() - 1) {
			int mod = mData.size() % LINE_COUNT;
			lineCount = mod == 0 ? LINE_COUNT : mod;
		} else {
			lineCount = LINE_COUNT;
		}
		for(int i = 0; i < lineCount; i++){
			final Theme theme = mData.get(position * LINE_COUNT + i);
			CommonUtil.getThemePrice(theme,mContext);
			if(i == 0){
				viewHolder.item1.setVisibility(View.VISIBLE);
				Glide.with(mContext).load(theme.coverUrl).into(viewHolder.image1);
				viewHolder.name1.setText(theme.name);
				setPrice(theme.getPrice(),viewHolder.price1);
				viewHolder.item1.setOnClickListener(new View.OnClickListener(){
					@Override
					public void onClick(View view) {
						theme.type = Theme.FONTS;
						Intent intent = IntentUtils.buildHomeThemeListIntent(Config.Action.ACTION_HOME_THEME_LIST_ITEM_DETAIL, theme);
						mContext.startActivity(intent);
					}
				});
			}else if(i == 1){
				viewHolder.item2.setVisibility(View.VISIBLE);
				Glide.with(mContext).load(theme.coverUrl).into(viewHolder.image2);
				viewHolder.name2.setText(theme.name);
				setPrice(theme.getPrice(),viewHolder.price2);
				viewHolder.item2.setOnClickListener(new View.OnClickListener(){
					@Override
					public void onClick(View view) {
						theme.type = Theme.FONTS;
						Intent intent = IntentUtils.buildHomeThemeListIntent(Config.Action.ACTION_HOME_THEME_LIST_ITEM_DETAIL, theme);
						mContext.startActivity(intent);
					}
				});
			}
		}
	}

	private void setPrice(String price,TextView priceTextView){
		if(!TextUtils.isEmpty(price)){
			try{
				double newPrice = Double.parseDouble(price);
				priceTextView.setText(priceTextView.getContext().getString(R.string.theme_price_suffix, price));
			}catch (Exception e){
				priceTextView.setText(price);
			}
		}else{
			priceTextView.setText(priceTextView.getContext().getString(R.string.theme_price_free));
		}
	}

	@Override
	public Object getItem(int position) {
		return mData.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	private static class HeaderViewHolder{
		public View separate;
		public TextView title;
		public TextView more;
	}

	private static class BodyViewHolder{
		public View item1;
		public View item2;
		public ImageView image1;
		public TextView name1;
		public TextView price1;
		public ImageView image2;
		public TextView name2;
		public TextView price2;
	}

}
