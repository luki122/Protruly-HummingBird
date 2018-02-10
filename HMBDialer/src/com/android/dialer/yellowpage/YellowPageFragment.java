package com.android.dialer.yellowpage;

import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import hb.app.dialog.AlertDialog;
import com.cootek.phoneservice.CTUrl;
import com.cootek.phoneservice.CTWebView;
import com.cootek.phoneservice.ICTWebViewListener;
import com.android.dialer.DialtactsActivity;
import com.android.dialer.R;
import com.cootek.smartdialer_oem_module.sdk.CooTekPhoneService;
import com.cootek.smartdialer_oem_module.sdk.SettingsManager;
import com.cootek.touchlife.ITouchLifeMessage;
import com.cootek.utils.AnimationUtil;
import com.mediatek.dialer.util.PhoneInfoUtils;

public class YellowPageFragment extends Fragment {

	private static final String TAG = "YellowPageFragment";
	private IYellowPageNotification mNotificationCallback;
	private CTWebView mWebView;
	private View mLoadingAnimation;
	private View mLoadingContainer;
	private ViewGroup mParent;
	private FrameLayout mWebViewContainer;

	public YellowPageFragment() {
		super();
		mNotificationCallback = null;
	}

	View loading;
	private void addLoadingView() {
		Log.i(TAG, "addLoadingView ");
		if(loading==null) loading = View.inflate(this.getActivity(), R.layout.cootek_comp_loading_container, null);		
		mWebViewContainer.removeView(loading);
		mWebViewContainer.addView(loading);
		if(mLoadingAnimation==null) mLoadingAnimation = loading.findViewById(R.id.cootek_loading_animation);
		if(mLoadingContainer==null) mLoadingContainer = loading.findViewById(R.id.cootek_loading_container);
	}
	
	public void removeView(){
		if(mWebViewContainer!=null && mWebViewContainer.getChildCount()>0){
			mWebViewContainer.removeAllViews();
			loadState=-1;
		}
	}

	
	public void showLoadingView(){
		Log.i(TAG, "showLoadingView ");
		addLoadingView();
		mLoadingContainer.setVisibility(View.VISIBLE);
		AnimationUtil.startLoadingAnimation(mLoadingAnimation);
	}

	private void addWebView() {
		Log.i(TAG, "addWebView ");
		if (CooTekPhoneService.isInitialized()) {
			mWebView = CooTekPhoneService.getInstance().createCTWebViewForTab(this.getActivity(), new CTWebViewListener());
			if (mWebView != null) {
				CTUrl cturl = CooTekPhoneService.getInstance().createCTUrl("content://local.file.provider_online/index.html");
				mWebView.loadUrl(cturl);
				mWebViewContainer.addView(mWebView);
			}
		}
	}

	public void setYellowPageNotification(IYellowPageNotification notificationCallback) {
		mNotificationCallback = notificationCallback;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy");
	}

	//	public int getConnectedType(Context context) {
	//		if (context != null) {
	//			ConnectivityManager mConnectivityManager = (ConnectivityManager) context
	//					.getSystemService(Context.CONNECTIVITY_SERVICE);
	//			NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
	//			if (mNetworkInfo != null && mNetworkInfo.isAvailable()) {
	//				return mNetworkInfo.getType();
	//			}
	//		}
	//		return -1;
	//	}
	//
	//	public boolean isNetworkConnected(Context context) {
	//		if (context != null) {
	//			ConnectivityManager mConnectivityManager = (ConnectivityManager) context
	//					.getSystemService(Context.CONNECTIVITY_SERVICE);
	//			NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
	//			if (mNetworkInfo != null) {
	//				return mNetworkInfo.isAvailable();
	//			}
	//		}
	//		return false;
	//	}

//	public boolean needLaunch=true;
	@Override
	public void onResume() {
		super.onResume();
		Log.i(TAG, "onResume,needLaunch");

		//		if(needLaunch){
		//			load();
		//		}		
	}

	Dialog dialog=null;
	public int loadState=-1;//-1 未开始加载；0正在加载；1已加载
	public void realLoad(){
		Log.d(TAG,"realLoad,getActivity():"+getActivity());
		if(getActivity()==null) return;
		loadState=0;
		boolean debugMode = false; //设置成true
		CooTekPhoneService.setDebugMode(getActivity(), debugMode);
		String simNumber=PhoneInfoUtils.getNativePhoneNumber(getActivity());
		String simOperator=PhoneInfoUtils.getSimOperator(getActivity());
		Log.d(TAG,"simNumber:"+simNumber+" simOperator:"+simOperator);

		//如果集成VoIP，设置集成VoIP模块的包名，用于AIDL通信
		//			CooTekPhoneService.setVoipPkgName(voipPkgName);
		//sim卡1、sim卡2的号码
		CooTekPhoneService.initialize(getActivity(), simNumber, "");

		Log.d(TAG,"CooTekPhoneService.isInitialized():"+CooTekPhoneService.isInitialized());
		if (CooTekPhoneService.isInitialized()) {

			SettingsManager sm = CooTekPhoneService.getInstance().getSettingsManager();

			//打开网络开关
			//此值会影响影响所有网络操作
			sm.setNetworkAccessible(true);

			//根据SIM卡状态变化，设置SIM操作码（如中国为"46000"，香港为"45401"，台湾为"46601")
			//此值会影响模式号码识别和归属地
			sm.setSimOperator(simOperator);

			//根据运营商网络状态变化，设置网络操作码（如中国为"46000"，香港为"45401"，台湾为"46601")
			//此值会影响模式号码识别和归属地
			sm.setNetworkOperator(simOperator);

			final SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
			boolean showDataUsagePrompt=mPrefs.getBoolean("showDataUsagePrompt", true);
			Log.d(TAG,"showDataUsagePrompt"+showDataUsagePrompt);
			if(showDataUsagePrompt) {
				if(dialog!=null) dialog.dismiss();
				View view = LayoutInflater.from(getActivity()).inflate(R.layout.hb_enter_yellowpage_dialog_view, null);
				final CheckBox checkBox = (CheckBox)view.findViewById(R.id.check_box);
				AlertDialog.Builder builder=new AlertDialog.Builder(getActivity())
				.setTitle(getActivity().getString(R.string.hb_prompt))
				.setView(view)
				.setCancelable(false)
				.setPositiveButton(getActivity().getString(R.string.hb_agree), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						boolean ischecked = checkBox.isChecked();
						mPrefs.edit().putBoolean("showDataUsagePrompt", !ischecked).apply();
						addWebView();
//						addLoadingView();
						showLoadingView();
						CooTekPhoneService.onResume(getActivity());
						if (mWebView != null) {
							mWebView.onResume();
						}
					}
				})
				.setNegativeButton(getActivity().getString(R.string.hb_disagree), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						mPrefs.edit().putBoolean("showDataUsagePrompt", true).apply();
						((DialtactsActivity)getActivity()).mListsFragment.mViewPager.setCurrentItem(0);
						loadState=-1;
					}
				});
				dialog = builder.show();
			}else{
				addWebView();
//				addLoadingView();
				showLoadingView();
				CooTekPhoneService.onResume(this.getActivity());
				if (mWebView != null) {
					mWebView.onResume();
				}
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView");

		mParent = (ViewGroup) inflater.inflate(R.layout.yellowpage_fragment, container, false);
		mWebViewContainer = (FrameLayout) mParent.findViewById(R.id.webview_layout);

		//        addWebView();
		//        addLoadingView();

		return mParent;
	}

	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onActivityCreated");
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		Log.i(TAG, "onDestroyView");

		if (mWebView != null) {
			mParent.removeView(mWebView);
			if (CooTekPhoneService.isInitialized()) {
				CooTekPhoneService.getInstance().destroyCTWebViewForTab(mWebView);
			}
			mWebView = null;
		}
	}

	public interface IYellowPageNotification {
		void onRedPointReceived();

		void onSwitchToYPTab();
	}

	private class CTWebViewListener implements ICTWebViewListener {

		public void onReceivedError(int errorCode, String description,
				String failingUrl) {
			Log.d(TAG, "onReceivedError url:");
			loadState=-1;
		}

		public void onPageStarted(String url) {
			Log.d(TAG, "onPageStarted url:" + url);
			mLoadingContainer.setVisibility(View.VISIBLE);
			AnimationUtil.startLoadingAnimation(mLoadingAnimation);
		}

		public void onPageFinished(String url) {
			Log.d(TAG, "onPageFinished url:" + url);
			mLoadingContainer.setVisibility(View.INVISIBLE);
			AnimationUtil.stopLoadingAnimation(mLoadingAnimation);
			loadState=1;
		}

		public void onReceivedTitle(String title) {
			Log.d(TAG, "onReceivedTitle title:" + title);
		}

		public void onProgressChanged(int newProgress) {
			Log.d(TAG, "onProgressChanged newProgress:" + newProgress);
		}

		@Override
		public void onMessageReceived(ITouchLifeMessage msg) {
			Log.d(TAG, "onMessageReceived:");
			Handler handler = new Handler(Looper.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					if (mNotificationCallback != null) {
						mNotificationCallback.onRedPointReceived();
					}
				}
			});
		}

		@Override
		public void onSwtichToYPTab() {
			Log.d(TAG, "onSwtichToYPTab:");
			if (mNotificationCallback != null) {
				mNotificationCallback.onSwitchToYPTab();
			}
		}
	}
}
