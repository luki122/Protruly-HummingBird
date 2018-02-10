package com.hb.thememanager.ui.adapter;

import android.content.Context;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.hb.thememanager.R;
import com.hb.thememanager.job.loader.ImageLoader;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;

import java.io.File;
import java.util.List;

public class LocalThemeWallpaperListAdapter extends LocalThemeListAdapter {



	public LocalThemeWallpaperListAdapter(Context context) {
		super(context);

	}


	protected void initialItemLayout(SparseArray<Integer> itemLayoutArray){
		itemLayoutArray.put(TYPE_NORMAL,R.layout.list_item_local_theme_wallpaper);
		itemLayoutArray.put(TYPE_UPDATE,R.layout.list_item_local_theme_wallpaper_update);
//		itemLayoutArray.put(TYPE_PAY,R.layout.list_item_local_theme_wallpaper_normal_editable);
		itemLayoutArray.put(TYPE_NORMAL_EDITABLE,R.layout.list_item_local_theme_wallpaper_normal_editable);
	}



}
