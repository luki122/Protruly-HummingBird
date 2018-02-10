package com.hb.thememanager.ui.adapter;

import android.content.Context;
import android.util.SparseArray;

import com.hb.thememanager.R;
import com.hb.thememanager.model.Fonts;

public class LocalThemeFontsListAdapter extends LocalThemeListAdapter {

	private Context mContext;

	public LocalThemeFontsListAdapter(Context context) {
		super(context);
		mContext = context;
		addTheme(createDefaultFont());
	}

	private Fonts createDefaultFont(){
		Fonts ret = new Fonts();
		ret.isSystemTheme = 1;
		ret.name=mContext.getResources().getString(R.string.system_font_name);
		return ret;
	}


	protected void initialItemLayout(SparseArray<Integer> itemLayoutArray){
		itemLayoutArray.put(TYPE_NORMAL,R.layout.list_item_local_theme_fonts);
		itemLayoutArray.put(TYPE_UPDATE,R.layout.list_item_local_theme_fonts_update);
		itemLayoutArray.put(TYPE_PAY,R.layout.list_item_local_theme_fonts_pay);
		itemLayoutArray.put(TYPE_NORMAL_EDITABLE,R.layout.list_item_local_theme_fonts_normal_editable);
	}



}
