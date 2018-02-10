package com.hb.thememanager.ui.fragment;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hb.thememanager.R;
import com.hb.thememanager.manager.SearchHistoryManager;
import com.hb.thememanager.ui.SearchPresenter;
import com.hb.thememanager.ui.SearchView;
import com.hb.thememanager.ui.adapter.SearchResultAdapter;
import com.hb.thememanager.utils.StringUtils;
import com.hb.thememanager.views.AutoLineLayout;

import java.util.ArrayList;

import hb.widget.ViewPager;
import hb.widget.tab.TabLayout;

/**
 * 主题包Tab内容页面
 *
 */
public class SearchHistoryFragment extends Fragment implements View.OnClickListener{
	private static final String TAG = "HomeThemeFragment";

	private OnSearchItemClickListener mListener;

	private View mContentView;
	private TextView mLabel;
	private AutoLineLayout mHistory;
	private ImageView mDelete;

	private AlertDialog mDialog;

	public SearchHistoryFragment(){}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		initHistory();
	}

	public void initHistory(){
		String[] histories = SearchHistoryManager.getInstance().getHistories(getContext());
		if(histories == null || histories.length <= 0){
			mLabel.setVisibility(View.GONE);
			mHistory.setVisibility(View.GONE);
			mDelete.setVisibility(View.GONE);
		}else{
			mLabel.setVisibility(View.VISIBLE);
			mHistory.setVisibility(View.VISIBLE);
			mDelete.setVisibility(View.VISIBLE);
			mHistory.removeAllViews();
			for(final String his : histories){
				TextView historyView = new TextView(getContext());
				historyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,14);
				historyView.setTextColor(0x8A000000);
				historyView.setText(his);
				historyView.setGravity(Gravity.CENTER);
				historyView.setBackgroundResource(R.drawable.rectangle_text_background);
				RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				rlp.leftMargin = getContext().getResources().getDimensionPixelSize(R.dimen.search_history_margin_left);
				rlp.rightMargin = getContext().getResources().getDimensionPixelSize(R.dimen.search_history_margin_right);
				rlp.bottomMargin= getContext().getResources().getDimensionPixelSize(R.dimen.search_history_margin_bottom);
				mHistory.addView(historyView, rlp);
				historyView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if(mListener != null){
							mListener.onItemClick(his);
						}
					}
				});
			}
		}
	}

	private void deleteAllHistory(){
		SearchHistoryManager.getInstance().removeAll(getContext());
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		mContentView = inflater.inflate(R.layout.fragment_search_history, container,false);
		mLabel = (TextView) mContentView.findViewById(R.id.history_label);
		mHistory = (AutoLineLayout) mContentView.findViewById(R.id.history_list);
		mDelete = (ImageView) mContentView.findViewById(R.id.delete_history_icon);
		mDelete.setOnClickListener(this);
		return mContentView;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if(R.id.delete_history_icon == id){
			showDeleteHistoryDialog(true);
		}
	}

	public void setOnSearchItemClickListener(OnSearchItemClickListener listener){
		mListener = listener;
	}

	private void showDeleteHistoryDialog(boolean show){
		if(mDialog == null){
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
			mDialog = dialogBuilder.setMessage(getContext().getResources().getString(R.string.message_delete_search_history))
					.setPositiveButton(getContext().getResources().getString(R.string.confirm_delete), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							deleteAllHistory();
							initHistory();
						}
					})
					.setNegativeButton(getContext().getResources().getString(R.string.confirm_cancel), null)
					.create();
		}
		if(show){
			mDialog.show();
			mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setBackgroundResource(com.hb.R.drawable.button_background_hb_delete);
		}else{
			if(mDialog.isShowing()) {
				mDialog.dismiss();
			}
		}
	}
}
