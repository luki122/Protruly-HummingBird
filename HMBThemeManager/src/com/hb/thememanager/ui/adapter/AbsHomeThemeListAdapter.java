package com.hb.thememanager.ui.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

import com.hb.thememanager.R;
import com.hb.thememanager.model.HomeThemeCategory;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;

/**
 * 主界面中各个Tab中的数据列表的Adapter需要继承自这个类实现
 * 自己的具体逻辑
 *
 */
public abstract class AbsHomeThemeListAdapter extends BaseAdapter {


	private static final int MAX_VIEW_TYPE_COUNT = 3;

	
	protected List<HomeThemeCategory> mCategory ;
	protected Context mContext;
	public  AbsHomeThemeListAdapter(Context context) {
		// TODO Auto-generated constructor stub
		mContext = context;
		mCategory = new ArrayList<HomeThemeCategory>();
	}
	
	public void addCategory(HomeThemeCategory category){
		synchronized (mCategory) {
			if(!mCategory.contains(category)){
				mCategory.add(category);
				notifyDataSetChanged();
			}
		}
	}


	@Override
	public void notifyDataSetChanged() {
		Collections.sort(mCategory);
		super.notifyDataSetChanged();
	}

	public void addCategories(List<HomeThemeCategory> categories){
		synchronized (mCategory) {
				mCategory.addAll(categories);
				notifyDataSetChanged();
		}
	}

	public void removeAllCategories(int type){
		synchronized (mCategory){
			if(mCategory.size() > 0){
				ArrayList<HomeThemeCategory> categoriesDelete = new ArrayList<>();
				for(HomeThemeCategory c : mCategory){
					if(c.getType() == type){
						categoriesDelete.add(c);
					}
				}
				mCategory.removeAll(categoriesDelete);
			}

			notifyDataSetChanged();
		}
	}

	public void removeAllCategories(){
		synchronized (mCategory){
			if(mCategory.size() > 0){
				mCategory.clear();
			}

			notifyDataSetChanged();
		}
	}
	
	public Context getContext(){
		return mContext;
	}
	
	@Override
	public boolean areAllItemsEnabled() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isEnabled(int position) {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return getView(position, convertView, parent, getItemLayout(getItemViewType(position)));
	}
	
	public View getView(int position, View convertView, ViewGroup parent,
			int itemLayoutId) {
		// TODO Auto-generated method stub
		AbsHomeThemeListHolder holder  = null;
		if(convertView == null){
			convertView = LayoutInflater.from(mContext).inflate(itemLayoutId, null);
			holder = getViewHolder(mContext, this,getItemViewType(position));
			holder.holdConvertView(convertView);
			convertView.setTag(holder);
		}else{
			holder = (AbsHomeThemeListHolder)convertView.getTag();
		}
		holder.bindDatas(position, mCategory);
		return convertView;
	}
	
	/**
	 * 根据不同的Item布局返回不同的ViewHolder对象
	 * @param context
	 * @param adapter
	 * @param itemViewType
	 * @return
	 */
	protected abstract AbsHomeThemeListHolder getViewHolder(Context context,ListAdapter adapter,int itemViewType);
	

	protected  abstract  int getItemLayout(int itemType);

	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mCategory == null ? 0:mCategory.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return mCategory == null?null:mCategory.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}
	

	@Override
	public int getItemViewType(int position) {
		// TODO Auto-generated method stub
		return mCategory.get(position).getType();
	}
	
	@Override
	public int getViewTypeCount() {
		// TODO Auto-generated method stub
		return MAX_VIEW_TYPE_COUNT;
	}
	


}
