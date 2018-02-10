package com.hb.thememanager.ui.adapter;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import hb.utils.DisplayUtils;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
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
import com.hb.thememanager.ui.fragment.ringtone.setTo;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.TLog;

public abstract class LocalRingtoneAdapter extends LocalThemeListAdapter {

	private SparseArray<Integer> mItemLayout = new SparseArray<Integer>();

	public LocalRingtoneAdapter(Context context) {
		super(context);
		initialItemLayout(mItemLayout);
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
		
		TextView title;
		TextView setTo;
		CheckBox editCheckBox;
		private AbsLocalBaseAdapter<Theme> adapter;

		public ViewHolder(Context context,AbsLocalBaseAdapter<Theme> adapter) {
			super(context,adapter);
			// TODO Auto-generated constructor stub
			this.adapter = adapter;
		}

		@Override
		public void bindDatas(int position, List<Theme> themes) {
			final Theme theme = themes.get(position);
			
			if(theme != null){
				title.setText(theme.name);
			}

			if (editCheckBox != null) {
				editCheckBox.setVisibility(adapter.isEditMode() ? View.VISIBLE : View.GONE);
				editCheckBox.setChecked(adapter.isSelected(position));
			}
			if (setTo != null) {
				setTo.setVisibility(adapter.isEditMode() ? View.GONE : View.VISIBLE);
				setTo.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
				        // pop menu
				        Intent it = new Intent(getContext(), setTo.class);
				        it.putExtra("wno", theme.id);
				        it.putExtra("uri", theme.coverUrl);
				        it.putExtra("wname", theme.name);
				        Log.e("huliang", "theme:" + theme);
				        getContext().startActivity(it);
					}
				});
			}
		}

		@Override
		public void holdConvertView(View convertView) {
			// TODO Auto-generated method stub
			title = (TextView)convertView.findViewById(R.id.theme_list_item_title);
			setTo = (TextView)convertView.findViewById(R.id.theme_list_item_use_as);
			editCheckBox = (CheckBox)convertView.findViewById(R.id.theme_list_item_edit_box);
		}

	}
}
