package com.hb.thememanager.ui.adapter;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.Glide;
import com.bumptech.glide.MemoryCategory;
import com.bumptech.glide.load.DecodeFormat;
import com.hb.imageloader.HbImageLoader;
import com.hb.thememanager.job.loader.ImageLoader;
import com.hb.thememanager.job.loader.ImageLoaderConfig;
import com.hb.thememanager.job.loader.ImageLoaderConfig.OverrideSize;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.R;
import com.hb.thememanager.ThemeManagerApplication;
public class LocalWallpaperAdapter extends AbsLocalBaseAdapter<Wallpaper> {
	
	private static final int ITEM_LAYOUT_ID = R.layout.list_item_local_wallpaper;
	private HbImageLoader mImageLoader;
	public LocalWallpaperAdapter(Context context) {
		super(context);

	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	protected boolean isSystemTheme(int position) {
		Theme t = getItem(position);
		return t.isSystemTheme();
	}

	@Override
	protected boolean editable(Wallpaper wallpaper) {
		return wallpaper.isSystemTheme()?false:true;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		LocalWallpaperHolder holder = null;
		if(convertView == null){
			convertView = inflate(ITEM_LAYOUT_ID, null);
			holder = new LocalWallpaperHolder(getContext(), this);
			holder.holdConvertView(convertView);
			convertView.setTag(holder);
		}else{
			holder = (LocalWallpaperHolder) convertView.getTag();
		}
		holder.bindDatas(position, getThemes());
		return convertView;
	}
	
	
	static class LocalWallpaperHolder extends AbsViewHolder<Wallpaper>{
		private AbsLocalBaseAdapter<Wallpaper> adapter;
		private ImageView wallpaperImg;
		private ImageView applyStatus;
		private CheckBox editCheckBox;
		
		public LocalWallpaperHolder(Context context,
				AbsLocalBaseAdapter<Wallpaper> adapter) {
			super(context, adapter);
			// TODO Auto-generated constructor stub
			this.adapter = adapter;
		}

		@Override
		public void holdConvertView(View convertView) {
			// TODO Auto-generated method stub
			wallpaperImg = (ImageView)convertView.findViewById(R.id.wallpaper_item);
//			applyStatus  = (ImageView)convertView.findViewById(R.id.wallpaper_item_applied);
			editCheckBox = (CheckBox)convertView.findViewById(R.id.wallpaper_item_select_delete);
		}

		@Override
		public void bindDatas(int position, List<Wallpaper> themes) {
			// TODO Auto-generated method stub
			Wallpaper wallpaper = themes.get(position);
			String wallpaperFileUrl = wallpaper.themeFilePath;
			if(!TextUtils.isEmpty(wallpaperFileUrl)){
				((AbsLocalBaseAdapter)getAdapter()).getImageLoader().loadImage(wallpaperFileUrl, wallpaperImg);

			}
			boolean showEditBox = !((AbsLocalBaseAdapter)getAdapter()).isSystemTheme(position)
					&& ((AbsLocalBaseAdapter)getAdapter()).isEditMode();
			editCheckBox.setVisibility(showEditBox?View.VISIBLE:View.GONE);
			editCheckBox.setChecked(((AbsLocalBaseAdapter)getAdapter()).isSelected(position));
		}
		
	}

}
