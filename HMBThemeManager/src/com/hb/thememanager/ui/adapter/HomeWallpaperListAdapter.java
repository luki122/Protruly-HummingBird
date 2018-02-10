package com.hb.thememanager.ui.adapter;

import java.util.ArrayList;
import java.util.List;

import android.app.ActivityOptions;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridLayout;
import android.widget.GridLayout.Spec;
import android.widget.GridLayout.LayoutParams;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.content.Intent;
import com.hb.thememanager.R;
import com.hb.thememanager.model.HomeThemeCategory;
import com.hb.thememanager.model.HomeThemeHeaderCategory;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.IntentUtils;
import com.hb.thememanager.ui.mvpview.WallpaperDetailPresenter;
import com.hb.thememanager.views.BannerView;
import com.hb.thememanager.views.HomeThemeListItem;
import com.hb.thememanager.views.IconFastEntry;

/**
 * 主题包首页Adapter
 */
public class HomeWallpaperListAdapter extends AbsHomeThemeListAdapter {
	private SparseArray<List<Wallpaper>> mWallpapers = new SparseArray<>();

	public  HomeWallpaperListAdapter(Context context) {
		super(context);

	}


	@Override
	protected int getItemLayout(int itemType) {
		if(itemType == HomeThemeCategory.TYPE_ADVERTISIN){
			return R.layout.list_item_advertising;
		}else if(itemType == HomeThemeCategory.TYPE_CATEGORY){
			return R.layout.list_item_home_theme;
		}else if(itemType == HomeThemeCategory.TYPE_HEADER){
			return R.layout.list_header_home;
		}
		return 0;
	}



	@Override
	protected AbsHomeThemeListHolder getViewHolder(Context context, ListAdapter adapter,
			int itemViewType) {
		// TODO Auto-generated method stub
		return new MyViewHolder(mContext, this, itemViewType, Theme.WALLPAPER);
	}
	
	
	
	private  class MyViewHolder extends AbsHomeThemeListHolder{

		public MyViewHolder(Context context, ListAdapter adapter,int categoryType,int themeType) {
			super(context, adapter,categoryType,themeType);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void initialThemeList(View convertView) {
			// TODO Auto-generated method stub
			super.initialThemeList(convertView);
		}

		@Override
		protected void bindThemeListItem(int position,  HomeThemeCategory category) {
			List<Theme> themes = category.getThemes();
			List<Wallpaper> list = new ArrayList<>();
			Wallpaper wallpaper;
			
			for (int i = 0; i < themes.size(); i++) {
				wallpaper = new Wallpaper();
				wallpaper.coverUrl = themes.get(i).coverUrl;
				wallpaper.downloadUrl = themes.get(i).downloadUrl;
				list.add(wallpaper);
			}
			mWallpapers.put(position, list);
			super.bindThemeListItem(position, category);
		}

		@Override
		protected void attachItemData(final int item, final int position, final Theme theme, View convertView, final int recommendId) {
			final HomeThemeListItem itemView = (HomeThemeListItem)convertView;
			itemView.setVisibility(View.VISIBLE);
			itemView.setIcon(theme.coverUrl);
			itemView.setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View view) {
					// TODO Auto-generated method stub
					Intent intent = new Intent(Config.Action.ACTION_WALLPAPER_DETAIL);
					Bundle bundle = new Bundle();

					WallpaperDetailPresenter.setWallpapers(mWallpapers.get(item), position);
					
					bundle.putString(Config.ActionKey.KEY_WALLPAPER_DETAIL_URL, Config.HttpUrl.getHotRecommendUrl(Theme.WALLPAPER));
					bundle.putInt(Config.ActionKey.KEY_WALLPAPER_DETAIL_ID, recommendId);
					
					intent.putExtra(Config.ActionKey.KEY_WALLPAPER_PREVIEW_BUNDLE, bundle);

					ImageView image = (ImageView)itemView.getChildAt(0);
					if(image.getDrawable() != null) {
						Config.sStaringImageInPreview  = image.getDrawable().getConstantState().newDrawable();
					}
					ActivityOptions ops = ActivityOptions.makeScaleUpAnimation(image, (int)image.getX() / 2,  (int)image.getY() / 2,image.getWidth(),image.getHeight());
					getContext().startActivity(intent, ops.toBundle());
				}
			});

		}
	}
}

