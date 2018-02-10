package com.hb.thememanager.ui.adapter;

import java.util.ArrayList;
import java.util.List;

import hb.utils.DisplayUtils;

import com.hb.imageloader.HbImageLoader;
import com.hb.thememanager.ThemeManager;
import com.hb.thememanager.ThemeManagerImpl;
import com.hb.thememanager.model.Theme;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class AbsLocalBaseAdapter<T> extends BaseAdapter {

	private List<T> mThemes = new ArrayList<T>();
	private SparseArray<T> mSelectedEditThemes = new SparseArray<T>();
	private Context mContext;
	private LayoutInflater mInflater;
	private ThemeManager mThemeManager;
	private boolean mEditMode = false;
	private Object mLock = new Object();
	private HbImageLoader mImageLoader;
	public AbsLocalBaseAdapter(Context context){
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mThemeManager = ThemeManagerImpl.getInstance(context);
	}
	
	public void selectOrUnSelectTheme(int position,boolean select){
		T t = getItem(position);
		if(!editable(t)){
			return;
		}
		if(!select){
			if(mSelectedEditThemes.get(position) != null){
				mSelectedEditThemes.remove(position);
			}
		}else{
			if(mSelectedEditThemes.get(position) == null ){
				mSelectedEditThemes.put(position, getItem(position));
			}
		}
		notifyDataSetChanged();
		
	}
	
	public void unSelectTheme(int position){
		T t = getItem(position);
		if(!editable(t)){
			return;
		}
		if(mSelectedEditThemes.get(position) != null){
			mSelectedEditThemes.remove(position);
		}
		notifyDataSetChanged();
	}

	protected boolean isSystemTheme(int position) {

		return false;
	}
	
	public void selectOrUnselectAll(boolean selectAll){
		if(!selectAll){
			mSelectedEditThemes.clear();
		}else{
			int themeCount = mThemes.size();
			if(themeCount > 0){
				for(int i = 0;i<themeCount;i++){
					T t = getItem(i);
					if(!editable(t)){
						continue;
					}
					mSelectedEditThemes.put(i, getItem(i));
				}
			}
		}
		notifyDataSetChanged();
	}

	public void deleteItems(int[] positions){
		synchronized (mThemes){
			ArrayList<T> deleteThemes = new ArrayList<T>();
			for(int i : positions) {
				deleteThemes.add(mThemes.get(i));

			}

			mThemes.removeAll(deleteThemes);
			notifyDataSetChanged();
		}
	}

	public void setImageLoader(HbImageLoader imageLoader){
		mImageLoader = imageLoader;
	}

	public HbImageLoader getImageLoader(){
		return mImageLoader;
	}
	protected boolean editable(T t){
		return true;
	}
	
	public SparseArray<T> getSelectedItems(){
		return mSelectedEditThemes;
	}
	
	public boolean isSelected(int position){
		return mSelectedEditThemes.get(position) != null;
	}
	
	public void enterEditMode(boolean enter){
		synchronized (mLock) {
			mEditMode = enter;
			if(!enter){
				mSelectedEditThemes.clear();
			}
		}
		notifyDataSetChanged();
	}
	
	public boolean isEditMode(){
		synchronized (mLock) {
			return mEditMode;
		}
	}
	
	
	
	public void addTheme(T theme){
		synchronized (mThemes) {
			if(!mThemes.contains(theme)){
				mThemes.add(theme);
			}
		}
		notifyDataSetChanged();
	}
	
	public void addThemes(List<T> newThemes){
		synchronized (mThemes) {
			for(T t : newThemes){
				if(!mThemes.contains(t)){
					mThemes.add(t);
				}
			}
		}
		notifyDataSetChanged();
	}
	
	public void removeTheme(int position){
		synchronized (mThemes) {
			mThemes.remove(position);
			notifyDataSetChanged();
		}
	}
	
	public void removeTheme(T theme){
		synchronized (mThemes) {
			if(mThemes.contains(theme)){
				mThemes.remove(theme);
				notifyDataSetChanged();
			}
		}
	}
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mThemes.size();
	}
	
	public T getTheme(int position){
		return getItem(position);
	}
	
	public boolean themeApplied(Theme theme){
		return mThemeManager.themeApplied(theme);
	}
	
	public Context getContext(){
		return mContext;
	}
	
	public List<T> getThemes(){
		return mThemes;
	}
	
	public LayoutInflater getInflater(){
		return mInflater;
	}
	
	public View inflate(int resource,ViewGroup root){
		return getInflater().inflate(resource, root);
	}
	
	@Override
	public T getItem(int position) {
		// TODO Auto-generated method stub
		return mThemes.get(position);
	}
	
	public void onDestory(){
		if(mThemes != null && mThemes.size() > 0){
			mThemes.clear();
		}
	}

}
