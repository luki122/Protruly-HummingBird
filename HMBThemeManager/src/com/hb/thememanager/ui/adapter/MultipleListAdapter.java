package com.hb.thememanager.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;

import com.hb.thememanager.R;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.IntentUtils;
import com.hb.thememanager.views.HomeThemeListItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 主题包首页Adapter
 */
public class MultipleListAdapter extends BaseAdapter {
	private static final String TAG = "MultipleListAdapter";

	private Context mContext;
	private List<BaseAdapter> mAdapters;
	private Map<String, TypeInof> mTypes;
	private SparseArray<AdapterInfo> mAdapterInfos;
	private int mTypeCount = 1;
	private boolean setTypeCountManual = false;

	private static class AdapterInfo{
		public int index;
		public int offset;
		public BaseAdapter adapter;
	}
	private static class TypeInof{
		public int offset;
		public int typeCount;
	}

	private int mOffset = 0;

	public MultipleListAdapter(Context context) {
		mContext = context;
		mAdapters = new ArrayList<>();
		mTypes = new HashMap<>();
		mAdapterInfos = new SparseArray<>();
		setTypeCountManual = false;
	}

	public void addAdapter(BaseAdapter adapter){
		String adapterClassName = adapter.getClass().getName();
		mAdapters.add(adapter);
		android.util.Log.e(TAG,"addAdapter : adapterClassName = "+adapterClassName);
		if(!mTypes.containsKey(adapterClassName)){
			TypeInof ti = new TypeInof();
			ti.typeCount = adapter.getViewTypeCount();
			ti.offset = mOffset;
			mTypes.put(adapterClassName, ti);
			mOffset+=ti.typeCount;
		}
	}

	public void addAdapter(int position, BaseAdapter adapter){
		String adapterClassName = adapter.getClass().getName();
		mAdapters.add(position, adapter);
		android.util.Log.e(TAG,"addAdapter : adapterClassName = "+adapterClassName);
		if(!mTypes.containsKey(adapterClassName)){
			TypeInof ti = new TypeInof();
			ti.typeCount = adapter.getViewTypeCount();
			ti.offset = mOffset;
			mTypes.put(adapterClassName, ti);
			mOffset+=ti.typeCount;
		}
	}

	public void setAdapter(int position, BaseAdapter adapter){
		String adapterClassName = adapter.getClass().getName();
		BaseAdapter originAdapter = mAdapters.get(position);
		String originAdapterClassName = originAdapter.getClass().getName();
		if(adapterClassName.equals(originAdapterClassName)){
			mAdapters.set(position, adapter);
		}
	}

	public void clearAdapter(){
		mAdapters.clear();
		mTypes.clear();
		mAdapterInfos.clear();
		mOffset = 0;
	}

	@Override
	public int getItemViewType(int position) {
		AdapterInfo info = getAdapterInfo(position);
		String key = info.adapter.getClass().getName();
		int type = mTypes.get(key).offset + info.adapter.getItemViewType(position - info.offset);
//		android.util.Log.e(TAG,"getItemViewType : type = "+type + " ; position = "+position);
		return type;
	}

	@Override
	public int getViewTypeCount() {
		if(setTypeCountManual){
			return mTypeCount;
		}else {
			int count = 0;
			for (String key : mTypes.keySet()) {
				count += mTypes.get(key).typeCount;
			}
			return count <= 0 ? 1 : count;
		}
	}

	public void setTypeCount(int count){
		if(count > 0) {
			setTypeCountManual = true;
			mTypeCount = count;
		}
	}

	@Override
	public int getCount() {
		int ret = 0;
		for(BaseAdapter a : mAdapters){
			ret += a.getCount();
		}
		return ret;
	}

	@Override
	public Object getItem(int position) {
		AdapterInfo info = getAdapterInfo(position);
		return info.adapter.getItem(position - info.offset);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		AdapterInfo info = getAdapterInfo(position);
		return info.adapter.getView(position - info.offset, convertView, parent);
	}

	@Override
	public void notifyDataSetChanged() {
		mAdapterInfos.clear();
		super.notifyDataSetChanged();
	}

	private AdapterInfo getAdapterInfo(int position){
		AdapterInfo info = mAdapterInfos.get(position);
		if(info == null){
			boolean already = false;
			info = mAdapterInfos.get(position - 1);
			if(info != null && position - info.offset < info.adapter.getCount() && position - info.offset >= 0){
				mAdapterInfos.put(position, info);
				already = true;
			}else{
				info = mAdapterInfos.get(position + 1);
				if(info != null && position - info.offset < info.adapter.getCount() && position - info.offset >= 0){
					mAdapterInfos.put(position, info);
					already = true;
				}
			}
			if(!already) {
				info = new AdapterInfo();
				int preCount = 0;
				int index = 0;

				AdapterInfo preInfo = mAdapterInfos.get(position - 1);
				if (preInfo != null) {
					index = preInfo.index + 1;
					if (index >= 0 && index < mAdapters.size()) {
						BaseAdapter currentAdapter = mAdapters.get(index);
						if (currentAdapter != null) {
							info.adapter = currentAdapter;
							info.index = index;
							info.offset = preInfo.offset + preInfo.adapter.getCount();
							already = true;
						}
					}
				}else{
					preInfo = mAdapterInfos.get(position + 1);
					if (preInfo != null) {
						index = preInfo.index - 1;
						if (index >= 0 && index < mAdapters.size()) {
							BaseAdapter currentAdapter = mAdapters.get(index);
							if (currentAdapter != null) {
								info.adapter = currentAdapter;
								info.index = index;
								info.offset = preInfo.offset - currentAdapter.getCount();
								already = true;
							}
						}
					}
				}
				if(!already) {
					int count = 0;
					for (BaseAdapter a : mAdapters) {
						preCount = count;
						count += a.getCount();
						if (count > position) {
							info.adapter = a;
							info.offset = preCount;
							info.index = index;
							break;
						}
						index++;
					}
				}
				mAdapterInfos.put(position, info);
			}
		}

		return info;
	}
}
