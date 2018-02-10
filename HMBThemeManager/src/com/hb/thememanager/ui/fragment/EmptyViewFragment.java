package com.hb.thememanager.ui.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.hb.thememanager.R;
import com.hb.thememanager.utils.CommonUtil;


/**
 * 主题包Tab内容页面
 *
 */
public class EmptyViewFragment extends BaseFragment {
	private static final String TAG = "EmptyViewFragment";
	private View mErrorView;
	private View mLoadingView;
	private TextView mEmptyTextView;
	private Button mEmptyButton;
	private ViewGroup mParent;
	private ViewGroup mCustomPanel;
	/**
	 * normal state
	 */
	public static final int EMPTY_STATE_NONE = 0;
	/**
	 * network error
	 */
	public static final int EMPTY_STATE_NO_NETWORK = 1;
	/**
	 * no data
	 */
	public static final int EMPTY_STATE_NO_DATA = 2;

	private int empty_state = EMPTY_STATE_NONE;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if(mParent == null) {
			mParent = (ViewGroup) inflater.inflate(R.layout.empty_panel_layout, container, false);
			mErrorView = mParent.findViewById(R.id.empty_view);
			mLoadingView = mParent.findViewById(R.id.loading_view);
			mCustomPanel = (ViewGroup) mParent.findViewById(R.id.empty_custom);
		}
		View contentView = onCreateNormalView(inflater, mParent, savedInstanceState);

		mErrorView.setVisibility(View.GONE);
		mLoadingView.setVisibility(View.GONE);

		if(-1 == mCustomPanel.indexOfChild(contentView)) {
			Log.e(TAG, "onCreateView : mCustomPanel have no custom view");
			mCustomPanel.removeAllViews();
			mCustomPanel.addView(contentView);
		}
		return mParent;
	}

	public View onCreateNormalView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		return null;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		Log.e(TAG, "onViewCreated -> empty_state = "+empty_state);
		empty_state = EMPTY_STATE_NONE;
		Button button = getEmptyButton();
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onEmptyButtonClick(v, empty_state);
			}
		});

		if(!CommonUtil.hasNetwork(getContext())){
			setState(EMPTY_STATE_NO_NETWORK);
		}else{
			setState(empty_state);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}

	public void setState(int state){
		onStateChanged(empty_state, state);
		empty_state = state;
	}

	protected void onStateChanged(int oldState, int newState){
		if(oldState != newState) {
			switch (newState) {
				case EMPTY_STATE_NONE:
					showNormalView();
					break;
				case EMPTY_STATE_NO_NETWORK:
					showNoNetworkView();
					break;
				case EMPTY_STATE_NO_DATA:
					showNoDataView();
					break;
			}
		}
	}

	public ViewGroup getCustomPanel(){
		return mCustomPanel;
	}

	private void showNormalView(){

		mErrorView.setVisibility(View.GONE);
		mLoadingView.setVisibility(View.GONE);
		mCustomPanel.setVisibility(View.VISIBLE);

	}

	public void showErrorView(){
		mErrorView.setVisibility(View.VISIBLE);
		mLoadingView.setVisibility(View.GONE);
		mCustomPanel.setVisibility(View.GONE);
	}

	public void showLoadingView(boolean show){
		if(show){
			mLoadingView.setVisibility(View.VISIBLE);
			mErrorView.setVisibility(View.GONE);
			mCustomPanel.setVisibility(View.GONE);
		}else{
			switch (empty_state) {
				case EMPTY_STATE_NONE:
					showNormalView();
					break;
				case EMPTY_STATE_NO_NETWORK:
					showNoNetworkView();
					break;
				case EMPTY_STATE_NO_DATA:
					showNoDataView();
					break;
			}
		}
	}

	private void showNoNetworkView(){
		showErrorView();
		TextView textView = getEmptyTextView();
		textView.setText(R.string.msg_no_network);
		getEmptyButton().setText(R.string.click_to_setup_network);
	}

	public void showNoDataView(){
		showErrorView();
		TextView textView = getEmptyTextView();
		textView.setText(R.string.empty_text);
		getEmptyButton().setText(R.string.click_to_refresh);
	}

	public TextView getEmptyTextView(){
		if(mEmptyTextView == null){
			mEmptyTextView = (TextView) mErrorView.findViewById(R.id.error_text);
		}
		return mEmptyTextView;
	}

	public Button getEmptyButton(){
		if(mEmptyButton == null){
			mEmptyButton = (Button) mErrorView.findViewById(R.id.btn_setup_network);
		}
		return mEmptyButton;
	}

	protected void onEmptyButtonClick(View v, int state){
		if(state == EMPTY_STATE_NO_NETWORK){
			startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
		}
	}
}
