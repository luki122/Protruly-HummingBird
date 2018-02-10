package com.hb.thememanager.ui.adapter;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import hb.utils.DisplayUtils;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.DecodeFormat;
import com.hb.thememanager.R;
import com.hb.thememanager.ThemeManagerApplication;
import com.hb.thememanager.job.loader.ImageLoader;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.TLog;

public class LocalThemeListAdapter extends AbsBaseAdapter<Theme> {

	private static int sSelectedMaskColor;
	
	public LocalThemeListAdapter(Context context) {
		super(context);
		sSelectedMaskColor = context.getColor(R.color.item_selected_mask_color);
	}
	

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		ViewHolder holder;
		if(convertView == null){
			convertView = inflate(R.layout.list_item_local_theme_pkg, null);
			holder = new ViewHolder(getContext(),this);
			holder.holdConvertView(convertView);
			convertView.setTag(holder);
		}else{
			holder = (ViewHolder) convertView.getTag();
		}
		holder.bindDatas(position, getThemes());
		return convertView;
	}

	static class ViewHolder extends AbsViewHolder<Theme>{
		
		private AbsBaseAdapter<Theme> adapter;
		public ViewHolder(Context context,AbsBaseAdapter<Theme> adapter) {
			super(context,adapter);
			// TODO Auto-generated constructor stub
			this.adapter = adapter;
		}

		ImageView image;
		TextView title;
		ImageView applyStatus;
//		CheckBox editCheckBox;
		@Override
		public void bindDatas(int position, List<Theme> themes) {
			Theme theme = themes.get(position);
			
			if(theme != null){
				title.setText(theme.name);
			}
			String coverPath = theme.loadedPath+File.separatorChar+Config.LOCAL_THEME_PREVIEW_DIR_NAME;
			File coverFile = new File(coverPath);
			if(coverFile.exists()){
					String[] childrenFiles = coverFile.list();
					if(childrenFiles != null && childrenFiles.length > 0){
						//取预览图列表中第一张作为封面
						final String path = coverPath+childrenFiles[0];
						getAdapter().getImageLoader().loadImage(path,image);
					}else{
						image.setImageResource(R.drawable.img_loading);
					}
					
			}else{
				if(theme.id == Config.DEFAULT_THEME_ID){
					getAdapter().getImageLoader().loadImage(Config.DEFAULT_THEME_COVER,image);
				}else{
					image.setImageResource(R.drawable.img_loading);
				}
				
			}

			if(adapter.themeApplied(theme)){
				applyStatus.setVisibility(View.VISIBLE);
				image.setForeground(new ColorDrawable(sSelectedMaskColor));
			}else{
				applyStatus.setVisibility(View.GONE);
				image.setForeground(null);
			}
		}

		@Override
		public void holdConvertView(View convertView) {
			// TODO Auto-generated method stub
			image = (ImageView)convertView.findViewById(R.id.theme_list_item_image);
			title = (TextView)convertView.findViewById(R.id.theme_list_item_title);
			applyStatus = (ImageView)convertView.findViewById(R.id.theme_list_item_apply_status);
//			editCheckBox = (CheckBox)convertView.findViewById(R.id.theme_list_item_edit_box);
		}

	}



}
