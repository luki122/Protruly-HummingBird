package com.hb.view;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.telecom.CallAudioState;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import com.android.incallui.InCallApp;
import com.android.incallui.Log;
import com.android.incallui.R;

import hb.widget.HbListView;

public class HbPopMenu implements OnDismissListener {
	private Context mContext;

	private static int S_POPMENU_WIDTH = 435;
	private ArrayList<Item> mItemList;

	private HBAudioPopAdapter mAdapter;
	private OnItemSelectedListener mListener;

	private HbListView mListView;

	private PopupWindow mPopupWindow;
	private ViewGroup mMeasureParent;
	private LayoutInflater mInflater;
	private int mSelectedId = -1;

	private int mPaddingBottom ;
	private int mPaddingRight ;
	public HbPopMenu(Context context, int checkId) {
		mContext = context;
		mPaddingRight = (int)context.getResources().getDimension(R.dimen.audio_pop_meun_padding_right);
		mPaddingBottom = (int)context.getResources().getDimension(R.dimen.audio_pop_meun_padding_bottom);
		mInflater = LayoutInflater.from(context);
		mItemList = new ArrayList<Item>();
		View view = LayoutInflater.from(context).inflate(R.layout.hb_audio_pop_menu, null);
		view.setFocusableInTouchMode(true);
		mSelectedId =  checkId;
		mAdapter = new HBAudioPopAdapter();	
		mListView = (HbListView) view.findViewById(R.id.menu_listview);
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Item item = (Item) mAdapter.getItem(position);
				if (mListener != null) {
					mListener.selected(view, item, position);
					mListener.onDismiss();
				}
				mPopupWindow.dismiss();
			}
		});

		mPopupWindow = new PopupWindow(view,
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT, true);
		mPopupWindow.setWidth(S_POPMENU_WIDTH);
		mPopupWindow.setBackgroundDrawable(new ColorDrawable(0xFFF9F9F9));

	}

	public void setAdapter() {
		mListView.setAdapter(mAdapter);
	}
	
	public void addItem(String text, int id) {
		mItemList.add(new Item(text, id));
//		mAdapter.notifyDataSetChanged();
	}

	public void addItem(int resId, int id) {
		addItem(mContext.getString(resId), id);
	}

	public void showAsDropDown(View parent) {
//        mPopupWindow.showAsDropDown(parent, 24, 16);
		mPopupWindow.showAtLocation(parent, Gravity.END, mPaddingRight, mPaddingBottom);
	}

	public void dismiss() {
		mPopupWindow.dismiss();
	}

	public void setOnItemSelectedListener(OnItemSelectedListener listener) {
		mListener = listener;
	}

	public boolean isShowing() {
		return mPopupWindow.isShowing();
	}

	public static class Item {
		public String text;
		public int id;

		public Item(String text, int id) {
			this.text = text;
			this.id = id;
		}

		@Override
		public String toString() {
			return text;
		}
	}

	public static interface OnItemSelectedListener {
		public void selected(View view, Item item, int position);
		public void onDismiss();
	}

	private class HBAudioPopAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mItemList.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return mItemList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.hb_audio_popmenu_list,
						parent, false);
				holder = new ViewHolder();
				holder.title = (TextView) convertView.findViewById(R.id.title);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.title.setText(mItemList.get(position).text);
			if (position == mSelectedId) {
				holder.title.setTextColor(mContext.getResources().getColor(R.color.audio_popmeun_checked_item_color));
			} else {
			    holder.title.setTextColor(Color.BLACK);
			}
			return convertView;
		}
	}

	public final class ViewHolder {
		public TextView title;
	}

	@Override
	public void onDismiss() {
		if (mListener != null) {
			mListener.onDismiss();
		}
	}
}