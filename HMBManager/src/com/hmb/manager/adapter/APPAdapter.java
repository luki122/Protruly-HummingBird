package com.hmb.manager.adapter;

import java.util.List;
import java.util.Map;

import com.hmb.manager.bean.AppInfo;
import com.hmb.manager.utils.TransUtils;
import com.hmb.manager.R;
import android.content.Context;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

public class APPAdapter extends BaseAdapter {
	public List<AppInfo> mlistAppInfo;
	LayoutInflater infater = null;
	public List<AppInfo> getMlistAppInfo() {
		return mlistAppInfo;
	}

	public void setMlistAppInfo(List<AppInfo> mlistAppInfo) {
		this.mlistAppInfo = mlistAppInfo;
	}

	public Map<Integer, Boolean> getmSelectedItems() { 
		return mSelectedItems;
	}

	public void setmSelectedItems(Map<Integer, Boolean> mSelectedItems) {
		this.mSelectedItems = mSelectedItems;
	}

	private Context mContext;
	private Map<Integer, Boolean> mSelectedItems;



	public APPAdapter(Context context, List<AppInfo> list, Map<Integer, Boolean> selectedItems) {
		mlistAppInfo = list;
		mSelectedItems = selectedItems;
		mContext = context;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mlistAppInfo.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return mlistAppInfo.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = LayoutInflater.from(mContext).inflate(R.layout.listview_app, null);

			holder.app_icon = (ImageView) convertView.findViewById(R.id.app_icon);
			holder.app_name = (TextView) convertView.findViewById(R.id.app_name);
			holder.app_size = (TextView) convertView.findViewById(R.id.app_size);
			holder.checkBox = (CheckBox) convertView.findViewById(R.id.app_check_box);
			holder.checkBox.setClickable(false);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		AppInfo info = mlistAppInfo.get(position);
		if (info != null) {
			holder.app_icon.setBackground(info.getAppIcon());
			holder.app_name.setText(info.getAppLabel());
			holder.app_size.setText(TransUtils.transformShortType(info.getPkgSize(), true));
			holder.checkBox.setChecked(mSelectedItems.get(position));
		}
		return convertView;
	}

	public final class ViewHolder {
		public ImageView app_icon;
		public TextView app_name;
		public TextView app_size;
		public CheckBox checkBox;
	}

}
