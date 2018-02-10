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
import com.hb.thememanager.ThemeManager;
import com.hb.thememanager.ThemeManagerImpl;
import com.hb.thememanager.model.Fonts;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.TopicDetail;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.ui.mvpview.WallpaperDetailPresenter;
import com.hb.thememanager.utils.IntentUtils;
import com.hb.thememanager.views.HomeThemeListItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 主界面中各个Tab中的数据列表的Adapter需要继承自这个类实现
 * 自己的具体逻辑
 *
 */
public class TopicDetailListAdapter extends BaseAdapter {

	private static final int THEME_LINE_COUNT = 3;
	private static final int WALLPAPER_LINE_COUNT = 3;
	private static final int FONTS_LINE_COUNT = 2;

	private Context mContext;
	private List<TopicDetail> mData;
	private int mCategory;
	private int mType = Theme.THEME_PKG;
	private List<Wallpaper> mWallpapers;
	private int mId;
	private ThemeManager mTm;
	public TopicDetailListAdapter(Context context, int id) {
		mId = id;
		mContext = context;
		mData = new ArrayList<>();
		mTm = ThemeManagerImpl.getInstance(context);
	}

	public void setData(List<TopicDetail> data){
		mData.clear();
		mData.addAll(data);
	}

	public void addData(List<TopicDetail> data){
		mData.addAll(data);
	}
	public void addData(TopicDetail data){
		mData.add(data);
	}

	public void setCategory(int category){
		mCategory = category;
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


	private Theme resetTopicStatus(TopicDetail td){
		Theme theme = createTheme(td);
		if(theme != null) {
			CommonUtil.getThemePrice(theme,mContext);
		}

		return theme;
	}

	private void setTopicPrice(HomeThemeListItem item,TopicDetail td){
		Theme theme = resetTopicStatus(td);
		if(!TextUtils.isEmpty(theme.getPrice())){
			item.setPrice(theme.getPrice());
		}else {
			item.setPrice(String.valueOf(td.getPrice()));
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(convertView == null){
			int resid = 0;
			switch(mType){
				case Theme.THEME_PKG:
					resid = R.layout.list_item_theme;
					break;
				case Theme.RINGTONE:
					resid = R.layout.list_item_ringtone;
					break;
				case Theme.WALLPAPER:
					resid = R.layout.list_item_wallpaper;
					break;
				case Theme.FONTS:
					resid = R.layout.list_item_fonts;
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
				ThemeViewHolder viewHolder = new ThemeViewHolder();
				viewHolder.item1 = (HomeThemeListItem) container.findViewById(R.id.list_item1);
				viewHolder.item2 = (HomeThemeListItem) container.findViewById(R.id.list_item2);
				viewHolder.item3 = (HomeThemeListItem) container.findViewById(R.id.list_item3);
				container.setTag(viewHolder);
				break;
			}
			case Theme.RINGTONE:

				break;
			case Theme.WALLPAPER: {
				WallpaperViewHolder viewHolder = new WallpaperViewHolder();
				viewHolder.item1 = (HomeThemeListItem) container.findViewById(R.id.list_item1);
				viewHolder.item2 = (HomeThemeListItem) container.findViewById(R.id.list_item2);
				viewHolder.item3 = (HomeThemeListItem) container.findViewById(R.id.list_item3);
				container.setTag(viewHolder);
				break;
			}
			case Theme.FONTS: {
				FontsViewHolder viewHolder = new FontsViewHolder();
				viewHolder.item1 = container.findViewById(R.id.list_item1);
				viewHolder.item2 = container.findViewById(R.id.list_item2);
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

	private void setPrice(TopicDetail td,TextView priceTextView){
		Theme theme = resetTopicStatus(td);
		if(theme != null) {
			if (!TextUtils.isEmpty(theme.getPrice())) {
				try {
					double newPrice = Double.parseDouble(theme.getPrice());
					priceTextView.setText(priceTextView.getResources()
							.getString(R.string.theme_price_suffix, theme.getPrice()));
				} catch (Exception e) {
					priceTextView.setText(theme.getPrice());
				}
			} else {
				priceTextView.setText(priceTextView.getContext().getString(R.string.theme_price_free));
			}
		}else {
			priceTextView.setText(priceTextView.getContext().getString(R.string.theme_price_free));
		}
	}

	private void bindView(final int position, View convertView){
		switch(mType){
			case Theme.THEME_PKG: {
				ThemeViewHolder viewHolder = (ThemeViewHolder) convertView.getTag();
				viewHolder.item1.setVisibility(View.INVISIBLE);
				viewHolder.item2.setVisibility(View.INVISIBLE);
				viewHolder.item3.setVisibility(View.INVISIBLE);
				int lineCount = 0;
				if (position == getCount() - 1) {
					int mod = mData.size() % THEME_LINE_COUNT;
					lineCount = mod == 0 ? THEME_LINE_COUNT : mod;
				} else {
					lineCount = THEME_LINE_COUNT;
				}
				for(int i = 0; i < lineCount; i++){
					final TopicDetail theme = mData.get(position * THEME_LINE_COUNT + i);
					HomeThemeListItem themeView = null;
					if(i == 0){
						themeView = viewHolder.item1;
					}else if(i == 1){
						themeView = viewHolder.item2;
					}else if(i == 2){
						themeView = viewHolder.item3;
					}
					if(themeView != null) {
						themeView.setVisibility(View.VISIBLE);
						themeView.setIcon(theme.getIcon());
						themeView.setTitle(theme.getName());
						setTopicPrice(themeView,theme);
						themeView.setOnClickListener(new View.OnClickListener(){
							@Override
							public void onClick(View view) {
								gotoThemeDetail(theme);
							}
						});
					}
				}
				break;
			}
			case Theme.RINGTONE:

				break;
			case Theme.WALLPAPER: {
				WallpaperViewHolder viewHolder = (WallpaperViewHolder) convertView.getTag();
				viewHolder.item1.setVisibility(View.INVISIBLE);
				viewHolder.item2.setVisibility(View.INVISIBLE);
				viewHolder.item3.setVisibility(View.INVISIBLE);
				int lineCount = 0;
				if (position == getCount() - 1) {
					int mod = mData.size() % WALLPAPER_LINE_COUNT;
					lineCount = mod == 0 ? WALLPAPER_LINE_COUNT : mod;
				} else {
					lineCount = WALLPAPER_LINE_COUNT;
				}
				
				mWallpapers = new ArrayList<>();
				Wallpaper wallpaper;
				for (int i = 0; i < mData.size(); i++) {
					wallpaper = new Wallpaper();
					TopicDetail theme = mData.get(i);
					wallpaper.coverUrl = theme.getIcon();
					wallpaper.downloadUrl = theme.getDownloadUrl();
					mWallpapers.add(wallpaper);
				}
				for (int i = 0; i < lineCount; i++) {
					final TopicDetail theme = mData.get(position * WALLPAPER_LINE_COUNT + i);
					HomeThemeListItem themeView = null;
					if (i == 0) {
						themeView = viewHolder.item1;
					} else if (i == 1) {
						themeView = viewHolder.item2;
					} else if (i == 2) {
						themeView = viewHolder.item3;
					}
					final int temp = i;
					if (themeView != null) {
						themeView.setVisibility(View.VISIBLE);
						themeView.setIcon(theme.getIcon());
						final ImageView image = (ImageView)themeView.getChildAt(0);
						themeView.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								Intent intent = new Intent(Config.Action.ACTION_WALLPAPER_DETAIL);
								Bundle bundle = new Bundle();

								WallpaperDetailPresenter.setWallpapers(mWallpapers, position * WALLPAPER_LINE_COUNT + temp);
								
								bundle.putString(Config.ActionKey.KEY_WALLPAPER_DETAIL_URL, Config.HttpUrl.getTopicDetailListUrl(Theme.WALLPAPER));
								bundle.putInt(Config.ActionKey.KEY_WALLPAPER_DETAIL_ID, mId);
								
								intent.putExtra(Config.ActionKey.KEY_WALLPAPER_PREVIEW_BUNDLE, bundle);

					if(image.getDrawable() != null) {
						Config.sStaringImageInPreview  = image.getDrawable().getConstantState().newDrawable();
					}
								ActivityOptions ops = ActivityOptions.makeScaleUpAnimation(image, (int)image.getX() / 2,  (int)image.getY() / 2,image.getWidth(),image.getHeight());
								mContext.startActivity(intent, ops.toBundle());
							}
						});
					}
				}
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
					final TopicDetail theme = mData.get(position * FONTS_LINE_COUNT + i);
					if(i == 0){

						viewHolder.item1.setVisibility(View.VISIBLE);
						Glide.with(mContext).load(theme.getIcon()).into(viewHolder.image1);
						viewHolder.name1.setText(theme.getName());
						setPrice(theme,viewHolder.price1);
						viewHolder.item1.setOnClickListener(new View.OnClickListener(){
							@Override
							public void onClick(View view) {
								gotoFontDetail(theme);
							}
						});
					}else if(i == 1){
						viewHolder.item2.setVisibility(View.VISIBLE);
						Glide.with(mContext).load(theme.getIcon()).into(viewHolder.image2);
						viewHolder.name2.setText(theme.getName());
						setPrice(theme,viewHolder.price2);
						viewHolder.item2.setOnClickListener(new View.OnClickListener(){
							@Override
							public void onClick(View view) {
								gotoFontDetail(theme);
							}
						});
					}
				}
				break;
			}
		}
	}

	private Theme createTheme(TopicDetail topicDetail){
		Theme targetTheme = null;
		if(mType == Theme.FONTS){
			targetTheme = new Fonts();
			targetTheme.type = Theme.FONTS;

		}else if(mType == Theme.THEME_PKG){
			targetTheme = new Theme();
			targetTheme.type = Theme.THEME_PKG;
		}

		if(targetTheme != null) {
			targetTheme.id = topicDetail.getId();
			targetTheme.downloadUrl = topicDetail.getDownloadUrl();
			targetTheme.name = topicDetail.getName();
			targetTheme.isCharge = topicDetail.getIsCharge();
			targetTheme.price = topicDetail.getPrice() + "";
		}
		return targetTheme;
	}

	private void gotoFontDetail(TopicDetail topicDetail){
		Theme theme = createTheme(topicDetail);
		if(theme != null) {
			Intent intent = IntentUtils.buildHomeThemeListIntent(Config.Action.ACTION_HOME_THEME_LIST_ITEM_DETAIL, theme);
			mContext.startActivity(intent);
		}
	}



	private void gotoThemeDetail(TopicDetail topicDetail){
		Theme theme = createTheme(topicDetail);
		if(theme != null) {
			Intent intent = IntentUtils.buildHomeThemeListIntent(Config.Action.ACTION_HOME_THEME_LIST_ITEM_DETAIL, theme);
			mContext.startActivity(intent);
		}
	}


	private static class ThemeViewHolder{
		public HomeThemeListItem item1;
		public HomeThemeListItem item2;
		public HomeThemeListItem item3;
	}
	private static class WallpaperViewHolder{
		public HomeThemeListItem item1;
		public HomeThemeListItem item2;
		public HomeThemeListItem item3;
	}
	private static class FontsViewHolder{
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

