package com.hb.thememanager.ui.adapter;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import hb.utils.DisplayUtils;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.DecodeFormat;
import com.hb.thememanager.R;
import com.hb.thememanager.job.loader.ImageLoader;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.TLog;

public abstract class LocalThemeListAdapter extends AbsLocalBaseAdapter<Theme> {

	protected static int TYPE_UPDATE = 0;
	protected static int TYPE_NORMAL = 1;
	protected static int TYPE_PAY = 2;
	protected static int TYPE_NORMAL_EDITABLE = 3;
	private SparseArray<Integer> mItemLayout = new SparseArray<Integer>();

	
	public LocalThemeListAdapter(Context context) {
		super(context);
		initialItemLayout(mItemLayout);
	}


	protected abstract void initialItemLayout(SparseArray<Integer> itemLayoutArray);


	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	protected boolean editable(Theme theme) {
		return !(theme.isSystemTheme() || theme.applyStatus == Theme.APPLIED);
	}

	@Override
	public int getItemViewType(int position) {
		Theme theme = getItem(position);
		if(theme.isSystemTheme()){
			return TYPE_NORMAL;
		}else if(theme.isUserImport()){
			return TYPE_NORMAL_EDITABLE;
		}else{

			if(theme.hasNewVersion()){
				if(theme.isPaid() || theme.isFree()) {
					return TYPE_UPDATE;
				}else{
					return TYPE_PAY;
				}
			}else{
				if(theme.isFree()){
					return TYPE_NORMAL_EDITABLE;
				}
				if(!theme.isPaid()){
					return  TYPE_PAY;
				}else{
					return TYPE_NORMAL;
				}
			}

		}

	}

	@Override
	public int getViewTypeCount() {
		return 4;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		ViewHolder holder;
		if(convertView == null){
			convertView = inflate(mItemLayout.get(getItemViewType(position)), null);
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
		
		private AbsLocalBaseAdapter<Theme> adapter;
		public ViewHolder(Context context,AbsLocalBaseAdapter<Theme> adapter) {
			super(context,adapter);
			// TODO Auto-generated constructor stub
			this.adapter = adapter;
		}

		ImageView image;
		TextView title;
		ImageView applyStatus;
		CheckBox editCheckBox;
		@Override
		public void bindDatas(int position, List<Theme> themes) {
			Theme theme = themes.get(position);
			
			if(theme != null && theme.type != Theme.WALLPAPER){
				title.setText(theme.name);
			}
			if(theme.type == Theme.WALLPAPER) {
				if(theme.themeFilePath != null && theme.themeFilePath.length() != 0) {
					ImageLoader.loadStringRes(image, theme.themeFilePath, null, null);
				}else if(theme.downloadUrl != null && theme.downloadUrl.length() != 0) {
					String fileName = Config.LOCAL_THEME_WALLPAPER_PATH + theme.downloadUrl.hashCode();
					if(new File(fileName).exists()){
							ImageLoader.loadStringRes(image, fileName, null, null);
					}else{
						image.setImageResource(R.drawable.ic_launcher);
					}
				}
			}else {
				String coverPath = theme.loadedPath+File.separatorChar+Config.LOCAL_THEME_PREVIEW_DIR_NAME;
				File coverFile = new File(coverPath);
				if(coverFile.exists()){
						String[] childrenFiles = coverFile.list();
						if(childrenFiles != null && childrenFiles.length > 0){
							String path = coverPath+childrenFiles[0];
							ImageLoader.loadStringRes(image,coverPath+childrenFiles[0],null,null);
						}else{
							image.setImageResource(R.drawable.ic_launcher);
						}
				}else{
					if(theme.isDefaultTheme()){
						ImageLoader.loadStringRes(image, Config.DEFAULT_THEME_COVER, null, null);
					}else{
						image.setImageResource(R.drawable.ic_launcher);
					}
				}
			}
			
			if(editCheckBox != null) {
				editCheckBox.setVisibility(adapter.isEditMode() ? View.VISIBLE : View.GONE);
				editCheckBox.setChecked(adapter.isSelected(position));
			}

			if(adapter.isEditMode()){
				applyStatus.setVisibility(View.GONE);
			}else{
				applyStatus.setVisibility(adapter.themeApplied(theme)?View.VISIBLE:View.GONE);
			}
			
		}

		@Override
		public void holdConvertView(View convertView) {
			// TODO Auto-generated method stub
			image = (ImageView)convertView.findViewById(R.id.theme_list_item_image);
			title = (TextView)convertView.findViewById(R.id.theme_list_item_title);
			applyStatus = (ImageView)convertView.findViewById(R.id.theme_list_item_apply_status);
			editCheckBox = (CheckBox)convertView.findViewById(R.id.theme_list_item_edit_box);
		}

	}

}

