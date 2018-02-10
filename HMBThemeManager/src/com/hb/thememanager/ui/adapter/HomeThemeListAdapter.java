package com.hb.thememanager.ui.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;
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
import com.bumptech.glide.Glide;
import com.hb.thememanager.R;
import com.hb.thememanager.ThemeManager;
import com.hb.thememanager.model.HomeThemeCategory;
import com.hb.thememanager.model.HomeThemeHeaderCategory;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.IntentUtils;
import com.hb.thememanager.views.BannerView;
import com.hb.thememanager.views.IconFastEntry;
import com.hb.thememanager.views.HomeThemeListItem;
/**
 * 主题包首页Adapter
 */
public class HomeThemeListAdapter extends AbsHomeThemeListAdapter {



	public  HomeThemeListAdapter(Context context) {
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
		return new MyViewHolder(mContext, this, itemViewType,Theme.THEME_PKG);
	}


	private  class MyViewHolder extends AbsHomeThemeListHolder{
		public MyViewHolder(Context context, ListAdapter adapter,int categoryType,int themeType) {
			super(context, adapter,categoryType,themeType);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void initialThemeList(View convertView) {
			super.initialThemeList(convertView);
		}


		@Override
		protected void attachItemData(int item, int position, final Theme theme, View convertView, int recommendId) {
			HomeThemeListItem itemView = (HomeThemeListItem)convertView;
			itemView.setVisibility(View.VISIBLE);
			itemView.setTitle(theme.name);
			itemView.setPrice(theme.getPrice());
			itemView.setIcon(theme.coverUrl);
			itemView.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View view) {
					// TODO Auto-generated method stub
					theme.type = Theme.THEME_PKG;
					Intent intent = IntentUtils.buildHomeThemeListIntent(Config.Action.ACTION_HOME_THEME_LIST_ITEM_DETAIL, theme);
					getContext().startActivity(intent);
				}
			});
		}
	}






}
