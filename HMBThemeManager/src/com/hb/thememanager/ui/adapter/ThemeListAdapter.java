package com.hb.thememanager.ui.adapter;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.GridLayout.LayoutParams;
import android.widget.GridLayout.Spec;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.hb.thememanager.R;
import com.hb.thememanager.model.HomeThemeCategory;
import com.hb.thememanager.model.RankingCategory;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.ui.mvpview.WallpaperDetailPresenter;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.IntentUtils;
import com.hb.thememanager.views.HomeThemeListItem;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.ThemeManager;
import com.hb.thememanager.ThemeManagerImpl;
import java.util.ArrayList;
import java.util.List;

/**
 * 主界面中各个Tab中的数据列表的Adapter需要继承自这个类实现
 * 自己的具体逻辑
 *
 */
public class ThemeListAdapter extends BaseAdapter {
	public static final int URL_TYPE_CATEGORY_DETAIL = 0;
	public static final int URL_TYPE_RANKING = 1;
	public static final int URL_TYPE_LOAD_MORE_ACTIVITY = 2;
	public static final int URL_TYPE_SEARCH = 3;
	public static final int NULL = -1;
	private int mUrlType = NULL;
	private int mId = -1;
	private int mWallpaperType = -1;
	private String mSearchKey;

	private static final int THEME_LINE_COUNT = 3;
	private static final int WALLPAPER_LINE_COUNT = 3;
	private static final int FONTS_LINE_COUNT = 2;

	private final int MARGIN_TOP = 0;
	private final int MARGIN_RIGHT = 1;
	private final int MARGIN_BOTTOM = 2;
	private final int MARGIN_LEFT = 3;

	private Context mContext;
	private List<Theme> mData;
	private int mCategory;
	private int mType = Theme.THEME_PKG;
	private List<Wallpaper> mWallpapers;
	private ThemeManager mTm;
	private int mHeaderMargin = -1,mFooterMargin = -1;
	
	private boolean mBShowPrice = true;
	
	public ThemeListAdapter(Context context) {
		mContext = context;
		mData = new ArrayList<>();
		mTm = ThemeManagerImpl.getInstance(context);
	}
	
	public void setJumpWallpaperDetailData(int urlType, int id, int wallpaperType, String searchKey) {
		mUrlType = urlType;
		mId = id;
		mWallpaperType = wallpaperType;
		mSearchKey = searchKey;
	}

	public void setData(List<Theme> data){
		mData.clear();
		if(data != null) {
			mData.addAll(data);
		}
	}
	public void addData(List<Theme> data){
		mData.addAll(data);
	}
	public void addData(Theme data){
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

	public void removeAll(){
		synchronized (mData){
			mData.clear();
			notifyDataSetChanged();
		}
	}

	public void setHeaderMargin(int margin){
		mHeaderMargin = margin;
	}

	public void setFooterMargin(int margin){
		mFooterMargin = margin;
	}

	public void showPrice(boolean show) {
		mBShowPrice = show;
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

	private void setPrice(String price,TextView priceTextView){
		if (!mBShowPrice) {
			return;
		}
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

	private void setViewPadding(View v, int p, int margin){
		switch (p){
			case MARGIN_TOP:
				v.setPadding(v.getPaddingLeft(), margin, v.getPaddingRight(), v.getPaddingBottom());
				break;
			case MARGIN_RIGHT:
				v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), margin, v.getPaddingBottom());
				break;
			case MARGIN_BOTTOM:
				v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), margin);
				break;
			case MARGIN_LEFT:
				v.setPadding(margin, v.getPaddingTop(), v.getPaddingRight(), v.getPaddingBottom());
				break;
		}
	}

	private void resetThemePrice(Theme theme){
		CommonUtil.getThemePrice(theme,mContext);
	}

	private void bindView(final int position, View convertView){
		if(mHeaderMargin > -1 && position == 0){
			setViewPadding(convertView, MARGIN_TOP, mHeaderMargin);
		}else if(mFooterMargin > -1 && position == getCount() - 1){
			setViewPadding(convertView, MARGIN_BOTTOM, mFooterMargin);
		}
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
					final Theme theme = mData.get(position * THEME_LINE_COUNT + i);
					resetThemePrice(theme);
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
						themeView.setIcon(theme.coverUrl);
						themeView.setTitle(theme.name);
						if (mBShowPrice) themeView.setPrice(theme.getPrice());
						themeView.setOnClickListener(new View.OnClickListener(){
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
					Theme theme = mData.get(i);
					wallpaper.coverUrl = theme.coverUrl;
					wallpaper.downloadUrl = theme.downloadUrl;
					mWallpapers.add(wallpaper);
				}
				for (int i = 0; i < lineCount; i++) {
					final Theme theme = mData.get(position * WALLPAPER_LINE_COUNT + i);
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
						themeView.setIcon(theme.coverUrl);
						final ImageView image = (ImageView)themeView.getChildAt(0);
						themeView.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								Intent intent = new Intent(Config.Action.ACTION_WALLPAPER_DETAIL);
								Bundle bundle = new Bundle();

								WallpaperDetailPresenter.setWallpapers(mWallpapers, position * WALLPAPER_LINE_COUNT + temp);
								
								switch (mUrlType) {											//CategoryDetailFragment.java(id type)		RankingFragment.java(type)
									case URL_TYPE_CATEGORY_DETAIL:				//SearchResultThemeFragment.java				LoadMoreActivity.java(id)
										bundle.putString(Config.ActionKey.KEY_WALLPAPER_DETAIL_URL, Config.HttpUrl.getCategoryUrl(Theme.WALLPAPER));
										bundle.putInt(Config.ActionKey.KEY_WALLPAPER_DETAIL_ID, mId);
										bundle.putInt(Config.ActionKey.KEY_WALLPAPER_DETAIL_TYPE, mWallpaperType);
										break;
									case URL_TYPE_RANKING:
										bundle.putString(Config.ActionKey.KEY_WALLPAPER_DETAIL_URL, Config.HttpUrl.getRankUrl(Theme.WALLPAPER));
										bundle.putInt(Config.ActionKey.KEY_WALLPAPER_DETAIL_TYPE, mWallpaperType);
										break;
									case URL_TYPE_LOAD_MORE_ACTIVITY:
										bundle.putString(Config.ActionKey.KEY_WALLPAPER_DETAIL_URL, Config.HttpUrl.getHotRecommendUrl(Theme.WALLPAPER));
										bundle.putInt(Config.ActionKey.KEY_WALLPAPER_DETAIL_ID, mId);
										break;
									case URL_TYPE_SEARCH:
										bundle.putString(Config.ActionKey.KEY_WALLPAPER_DETAIL_URL, Config.HttpUrl.SEARCH_URL);
										bundle.putString(Config.ActionKey.KEY_WALLPAPER_DETAIL_SEARCH_KEY, mSearchKey);
										break;
									default:
										break;
								}

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
					final Theme theme = mData.get(position * FONTS_LINE_COUNT + i);
					resetThemePrice(theme);
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
				break;
			}
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

