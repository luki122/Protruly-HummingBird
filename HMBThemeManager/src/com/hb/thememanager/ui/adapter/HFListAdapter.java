package com.hb.thememanager.ui.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 主题包首页Adapter
 */
public abstract class HFListAdapter extends BaseAdapter {
	private static final String TAG = "HFListAdapter";
	private static final int TYPE_HEADER = 0;
	private static final int TYPE_FOOTER = 1;

	private boolean hasHeader = false;
	private boolean hasFooter = false;

	public void showHeaderView(boolean show){
		hasHeader = show;
	}

	public void showFooterView(boolean show){
		hasFooter = show;
	}

	@Override
	public final int getViewTypeCount() {
		int count = getBodyTypeCount();
		count += 2;
		return count;
	}

	public int getBodyTypeCount(){
		return 1;
	}

	@Override
	public final int getItemViewType(int position) {
		int type = 0;
		if(position == 0 && hasHeader){
			type = TYPE_HEADER;
		}else if(position == getCount() - 1 && hasFooter){
			type = TYPE_FOOTER;
		}else{
			if(hasHeader) {
				type = getBodyItemType(position - 1) + 2;
			}else{
				type = getBodyItemType(position) + 2;
			}
		}
//		android.util.Log.e(TAG, "getItemViewType : type = "+type+" ; position = "+position);
		return type;
	}

	public int getBodyItemType(int position){
		return 0;
	}

	@Override
	public final int getCount() {
		int count = getBodyCount();
		if(hasHeader){
			count++;
		}
		if(hasFooter){
			count++;
		}
		return count;
	}

	public abstract int getBodyCount();

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
//		android.util.Log.e(TAG, "getView : position = "+position);
		int type = getItemViewType(position);
		switch (type){
			case TYPE_HEADER:
				if(convertView == null){
					convertView = createHeaderView(parent);
				}
				bindHeaderView(convertView, parent);
				break;
			case TYPE_FOOTER:
				if(convertView == null){
					convertView = createFooterView(parent);
				}
				bindFooterView(convertView, parent);
				break;
			default:
				int index = position;
				if(hasHeader){
					index = position - 1;
				}
				if(convertView == null){
					convertView = createBodyView(index, parent);
				}
				bindBodyView(index, convertView, parent);
				break;
		}
		return convertView;
	}

	public View createHeaderView(ViewGroup parent){
		return null;
	}
	public void bindHeaderView(View convertView, ViewGroup parent){}
	public abstract View createBodyView(int position, ViewGroup parent);
	public abstract void bindBodyView(int position, View convertView, ViewGroup parent);
	public View createFooterView(ViewGroup parent){
		return null;
	}
	public void bindFooterView(View convertView, ViewGroup parent){}

}
