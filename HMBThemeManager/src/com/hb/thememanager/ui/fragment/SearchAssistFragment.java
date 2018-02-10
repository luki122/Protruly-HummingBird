package com.hb.thememanager.ui.fragment;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hb.thememanager.R;
import com.hb.thememanager.http.request.SearchAssistRequest;
import com.hb.thememanager.http.response.SearchAssistResponse;
import com.hb.thememanager.http.response.adapter.SearchAssistBody;
import com.hb.thememanager.manager.SearchHistoryManager;
import com.hb.thememanager.ui.SearchPresenter;
import com.hb.thememanager.ui.SearchView;
import com.hb.thememanager.utils.StringUtils;
import com.hb.thememanager.views.AutoLineLayout;
import com.hb.thememanager.views.ThemeListView;

import java.util.List;

/**
 * 主题包Tab内容页面
 *
 */
public class SearchAssistFragment extends Fragment implements SearchView{
	private static final String TAG = "HomeThemeFragment";

	private OnSearchItemClickListener mListener;

	private View mContentView;
	private ListView mListView;
	private AssistAdapter mAdapter;
	private String searchStr;

	private SearchPresenter mPresenter;
	private SearchAssistRequest mRequest;



	public SearchAssistFragment(){}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		mPresenter = new SearchPresenter(getContext().getApplicationContext());
		mPresenter.attachView(this);

		search(searchStr);

	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		mContentView = inflater.inflate(R.layout.fragment_search_assist, container,false);
		mListView = (ListView) mContentView.findViewById(R.id.list_view);
		mAdapter = new AssistAdapter(getContext());
		mAdapter.setOnSearchItemClickListener(mListener);
		mListView.setAdapter(mAdapter);
		return mContentView;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}


	public void setOnSearchItemClickListener(OnSearchItemClickListener listener){
		mListener = listener;
		if(mAdapter != null){
			mAdapter.setOnSearchItemClickListener(mListener);
		}
	}


	@Override
	public void updateList(Object response) {
		if(response instanceof SearchAssistResponse){
			if(((SearchAssistResponse)response).body != null) {
				List<SearchAssistBody.Key> data = ((SearchAssistResponse) response).body.keyList;
				if (data != null && data.size() > 0) {
					mAdapter.setData(data);
					mAdapter.notifyDataSetChanged();
				}
			}
		}
	}

	@Override
	public void search(String str) {
		mAdapter.setData(null);
		mAdapter.notifyDataSetChanged();
		if(!StringUtils.isEmpty(str)){
			searchStr = str;
			if(isAdded()) {
				if(mRequest == null){
					mRequest = new SearchAssistRequest(getContext());
				}
				mRequest.setKey(str);
				mPresenter.requestTheme(mRequest);
			}
		}
	}

	@Override
	public void showToast(String msg) {

	}

	@Override
	public void showMyDialog(int dialogId) {

	}

	@Override
	public void showEmptyView(boolean show) {

	}

	@Override
	public void showNetworkErrorView(boolean show) {

	}

	private static class AssistAdapter extends BaseAdapter{
		private List<SearchAssistBody.Key> mData;
		private Context mContext;
		private OnSearchItemClickListener mListener;

		public AssistAdapter(Context context){
			mContext = context;
		}

		public void setData(List<SearchAssistBody.Key> data){
			mData = data;
		}

		@Override
		public int getCount() {
			return mData == null ? 0 : mData.size();
		}

		@Override
		public Object getItem(int position) {
			return mData.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		public void setOnSearchItemClickListener(OnSearchItemClickListener listener){
			mListener = listener;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHodler hodler = null;
			if(convertView == null){
				LayoutInflater inflater = LayoutInflater.from(mContext);
				convertView = inflater.inflate(com.hb.R.layout.list_item_1_line, parent, false);
				hodler = new ViewHodler();
				hodler.textView = (TextView) convertView.findViewById(android.R.id.text1);
				convertView.setTag(hodler);
			}else{
				hodler = (ViewHodler) convertView.getTag();
			}
			final SearchAssistBody.Key data = mData.get(position);
			hodler.textView.setText(data.key);

			convertView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(mListener != null){
						mListener.onItemClick(data.key);
					}
				}
			});

			return convertView;
		}

		private class ViewHodler{
			TextView textView;
		}
	}
}
