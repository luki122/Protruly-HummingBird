package com.hb.thememanager.views;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.hb.thememanager.R;
import com.hb.thememanager.ThemeManager;
import com.hb.thememanager.ThemeManagerImpl;
import com.hb.thememanager.database.SharePreferenceManager;
import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.downloader.DownloadInfo;
import com.hb.thememanager.http.downloader.DownloadManager;
import com.hb.thememanager.http.downloader.DownloadService;
import com.hb.thememanager.http.downloader.callback.DownloadListener;
import com.hb.thememanager.http.downloader.exception.DownloadException;
import com.hb.thememanager.http.request.RequestBody;
import com.hb.thememanager.http.request.ThemeRequest;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.ThemeResponse;
import com.hb.thememanager.job.pay.Pay;
import com.hb.thememanager.job.pay.PayDialog;
import com.hb.thememanager.job.pay.PayParams;
import com.hb.thememanager.manager.TimerManager;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.User;
import com.hb.thememanager.receiver.TimerReceiver;
import com.hb.thememanager.security.MD5Utils;
import com.hb.thememanager.security.SecurityManager;
import com.hb.thememanager.state.ThemeState;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.DialogUtils;
import com.hb.thememanager.utils.FileUtils;
import com.hb.thememanager.utils.TLog;
import com.hb.thememanager.utils.ToastUtils;


public class ThemePreviewDonwloadButton extends ThemeOptionButton implements View.OnClickListener,
		DownloadListener,DialogInterface.OnClickListener,Runnable {
	private static final String TAG = "DonwloadButton";
	private static final String SUFFIX_NEW_VERSION = "new_version";
	private final static int TIME_DELAY_MS = 1000;
	private View mDownloadBar;
	private View mNormalBar;
	private Button mApplyBtn;
	private Button mPayForBtn;
	private Button mCancelBtn;
	private Button mDownloadOptionBtn;
	private DownloadProgressBar mProgressBar;
	private DialogUtils mDialogUtils;
	private DownloadManager mDownloadManager;
	private DownloadInfo mDownloadInfo;
	private int mTotalSize;
	private long mTotalBytes;
	private int mProgress;
	private String mDownloadFileName;
	private ThemeManager mThemeManager;
	private SharedPreferences mPrefs;
	private boolean mAddedToWindow;
	private boolean mIsTringState = false;
	private boolean mHasNewVersion;
	public ThemePreviewDonwloadButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		mDialogUtils = new DialogUtils();
		mDownloadManager = DownloadService.getDownloadManager(context);
		mThemeManager = ThemeManagerImpl.getInstance(context);
		mPrefs = hb.preference.PreferenceManager.getDefaultSharedPreferences(context);
	}

	@Override
	protected int getLayoutRes() {
		return R.layout.theme_apply_bar;
	}


	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mDownloadBar = findViewById(R.id.download_bar);
		mNormalBar = findViewById(R.id.option_bar);
		mApplyBtn = (Button)findViewById(R.id.btn_apply);
		mPayForBtn = (Button)findViewById(R.id.btn_pay_for);

		mProgressBar = (DownloadProgressBar)findViewById(R.id.download_progress);
		mProgressBar.setMaxProgress(0);

		mCancelBtn = (Button)findViewById(R.id.btn_cancel);
		mDownloadOptionBtn = (Button)findViewById(R.id.btn_download_option);

		mApplyBtn.setOnClickListener(this);
		mPayForBtn.setOnClickListener(this);
		mCancelBtn.setOnClickListener(this);
		mDownloadOptionBtn.setOnClickListener(this);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		mAddedToWindow = true;
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mAddedToWindow = false;
	}

	@Override
	public void onStateChange(ThemeState state) {
		TLog.d(TAG,"onStateChange->"+state.name());

		if(state != ThemeState.STATE_APPLY_SUCCESS){
			mApplyBtn.setEnabled(true);
		}

		if(!mStateManager.isFromTry()){
			setApplyBtnTextColor(R.color.btn_apply_text_color_normal);
		}

		if(state == ThemeState.STATE_APPLY_SUCCESS){
			handleStateSuccess();
		}else if(state == ThemeState.STATE_DOWLOADING){
			handleStateDownloading();
		}else if(state == ThemeState.STATE_CANCEL){
			handleStateCancel();
		}else if(state == ThemeState.STATE_DOWNLOADED){
			handleStateDownloaded();
		}else if(state == ThemeState.STATE_PAUSE){
			handleStatePause();
		}else if(state == ThemeState.STATE_NORMAL){
			handleStateNormal();
		}
	}

	private void handleStateSuccess(){
		mDialogUtils.dismissDialog();
		if(mStateManager.isFromTry()){
			resetApplyBtnWhenFinishTry();
		}else{
			setThemeAppied();
		}

	}

	private void handleStateDownloading(){

		if(mDownloadInfo == null){
			return;
		}
		switchWidgetBar(true);
		if(mProgressBar.getMaxProgress() == 0){
			mProgressBar.setMaxProgress(mTotalSize);
		}
		mProgressBar.setProgress(getResources()
				.getString(R.string.download_downloading,getPercent()),mProgress);


	}

	private void handleStateCancel(){
		DownloadInfo info = mDownloadManager.getDownloadById(getTheme().id.hashCode());
		if(info != null){
			mDownloadManager.remove(mDownloadInfo);
		}
		mProgress = 0;
		switchWidgetBar(false);
		resetProgress();
	}

	private void handleStatePause(){
		mDownloadOptionBtn.setText(R.string.download_continue);
		mProgressBar.setProgress(getResources()
				.getString(R.string.download_paused),mProgress);
	}


	private void handleStateDownloaded(){

		if(!mAddedToWindow){
			return;
		}

		switchWidgetBar(false);
		getTheme().themeFilePath = mDownloadFileName;
		TLog.d(TAG,"is free theme or paid theme->"+canApplyDirectly());
		//如果是免费主题或者已付款主题，则显示立即使用按钮
		if(canApplyDirectly()) {
			mApplyBtn.setText(R.string.apply_now);
		}else{
			//如果是试用主题，下载完成后自动配置主题，试用该主题并开始计时
			tryFor();
		}
	}



	private void handleStateNormal(){
		switchWidgetBar(false);
		//如果是免费主题或者已付款主题，则显示立即使用按钮
		if(canApplyDirectly()) {
			mApplyBtn.setText(R.string.apply_now);
		}else{
			TimerManager.TimerObj timerObj = TimerManager.getTimerFromSharedPrefs(
					mPrefs,String.valueOf(getTheme().type));
			long leftTime = timerObj.updateTimeLeft(false);
			if(timerObj.id.equals(getTheme().id) && leftTime > 0L){
				setupTimerToButton(leftTime);
			}else{
				resetApplyBtnWhenFinishTry();
			}

		}
	}


	private void handleFreeThemeApplied(){
		mPayForBtn.setVisibility(View.GONE);
		setThemeAppied();
	}


	private void handlePaySuccess(){
		TimerManager.TimerObj timerObj = TimerManager.getTimerFromSharedPrefs(
				mPrefs,String.valueOf(getTheme().type));
		if(isTryApplyTheme(timerObj)){
			sendCancelTryThemeMsg(timerObj);
			handleFreeThemeApplied();
			SharePreferenceManager.saveNormalAppliedThemeId(getContext(),getTheme().type,getTheme().id);

		}else{
			mPayForBtn.setVisibility(View.GONE);
			if(mThemeManager.themeExists(getTheme())){
				mApplyBtn.setText(R.string.apply_now);
			}else{
				setDownloadText();
			}
		}
		User user = CommonUtil.getUser(getContext());
		if(user.isLogin()) {
			mThemeManager.savePaiedThemeForUser(getTheme(), Integer.parseInt(user.getId()));
		}

		removeCallbacks(this);

	}

	private void setThemeAppied(){
		mApplyBtn.setText(R.string.theme_applied);
		mApplyBtn.setEnabled(false);
	}



	private String getPercent(){
		float progress = (mProgress / (mTotalSize +0f))*100;
		return Math.round(progress * 10)/10+"%";
	}


	private boolean canApplyDirectly(){
		User user = User.getInstance(getContext());

		return  getTheme().isFree() || (getTheme().isPaid() && user.isLogin());
	}
	private void applyOrTryForIt(){

		final TimerManager.TimerObj timerObj = TimerManager.getTimerFromSharedPrefs(
				mPrefs,String.valueOf(getTheme().type));
		long leftTime = timerObj.updateTimeLeft(false);

		//如果是免费主题或者已付款主题，则显示立即使用按钮
		if(canApplyDirectly()){
			mStateManager.setFromTry(false);
			apply();

			if(leftTime > 0L && !timerObj.id.equals(getTheme().id)){
				sendCancelTryThemeMsg(timerObj);
			}

		}else{
			TLog.d(TAG,"finish try->"+(leftTime > 0L && timerObj.id.equals(getTheme().id) && mIsTringState));
			//如果当前主题正在试用，那么结束试用
			if(leftTime > 0L && timerObj.id.equals(getTheme().id) && mIsTringState){
				sendFinishTryThemeMsg(timerObj);
				mIsTringState = false;
				resetApplyBtnWhenFinishTry();
			}else{
				//如果是试用主题，下载完成后自动配置主题，试用该主题并开始计时
				tryFor();
			}

		}

	}

	private void tryFor(){
		synchronized (ThemePreviewDonwloadButton.this) {
			sendTryApplyThemeMsg(getContext());
		}
	}

	@Override
	public void run() {
		TimerManager.TimerObj timerObj = TimerManager.getTimerFromSharedPrefs(
				mPrefs,String.valueOf(getTheme().type)
		);
		if(timerObj != null){
			long leftTime = timerObj.updateTimeLeft(true);
			if(leftTime <= 0L){
				mIsTringState = false;
				resetApplyBtnWhenFinishTry();

				removeCallbacks(this);
			}else{
				postDelayed(this,TIME_DELAY_MS);
				setupTimerToButton(leftTime);
			}
		}else{
			mIsTringState = false;
			resetApplyBtnWhenFinishTry();
			removeCallbacks(this);
		}
	}

	private void setApplyBtnTextColor(int colorResId){
		mApplyBtn.setTextColor(getResources().getColor(colorResId));
	}


	private void resetApplyBtnWhenFinishTry(){
		mApplyBtn.setText(R.string.try_theme);
		if(!mIsTringState) {
			setApplyBtnTextColor(R.color.btn_apply_text_color_normal);
		}
	}


	private void sendFinishTryThemeMsg(TimerManager.TimerObj timerObj){
		Intent intent = new Intent(TimerReceiver.ACTION_FINISH_TIMER);
		intent.putExtra(TimerReceiver.KEY_TIMER_INTENT_ID,timerObj.id);
		intent.putExtra(TimerReceiver.KEY_TIMER_INTENT_TYPE,timerObj.type);
		intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		getContext().sendBroadcast(intent);

	}




	private void sendCancelTryThemeMsg(TimerManager.TimerObj timerObj){
		Intent intent = new Intent(TimerReceiver.ACTION_CANCEL);
		intent.putExtra(TimerReceiver.KEY_TIMER_INTENT_ID,timerObj.id);
		intent.putExtra(TimerReceiver.KEY_TIMER_INTENT_TYPE,timerObj.type);
		intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		getContext().sendBroadcast(intent);

	}

	private void sendTryApplyThemeMsg(Context context) {
		TimerManager.TimerObj timerObj = new TimerManager.TimerObj(getTheme());
		TimerManager.deleteFromSharedPref(mPrefs,timerObj);
		TimerManager.putTimersInSharedPrefs(mPrefs,timerObj);
		postDelayed(this,TIME_DELAY_MS);
		Intent intent = new Intent(TimerReceiver.ACTION);
		intent.putExtra(Config.ActionKey.KEY_APPLY_THEME_IN_SERVICE,getTheme());
		intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		context.sendBroadcast(intent);
		setApplyBtnTextColor(R.color.btn_apply_text_color_disable);
		mIsTringState = true;
	}


	private boolean isTryApplyTheme(TimerManager.TimerObj timerObj){
		return timerObj.id.equals(getTheme().id);
	}

	public void onPausedTimer(){
		TimerManager.TimerObj timerObj = TimerManager.getTimerFromSharedPrefs(
				mPrefs,String.valueOf(getTheme().type)
		);
		if(!isTryApplyTheme(timerObj)){
			return;
		}
		timerObj.updateTimeLeft(true);
		TimerManager.putTimersInSharedPrefs(mPrefs,timerObj);


		removeCallbacks(this);


	}

	public void onResumeTimer(){
		TimerManager.TimerObj timerObj = TimerManager.getTimerFromSharedPrefs(
				mPrefs,String.valueOf(getTheme().type)
		);

		if(!isTryApplyTheme(timerObj)){
			return;
		}

		long leftTime = 0;
		leftTime = timerObj.updateTimeLeft(false);
		boolean tryFinished = leftTime <= 0;

		if(tryFinished){
			mStateManager.postState(ThemeState.STATE_NORMAL);
		}else{
			setupTimerToButton(leftTime);
			postDelayed(this,TIME_DELAY_MS);
		}
	}

	private void setupTimerToButton(long leftTime){
		mApplyBtn.setText(getResources().getString(R.string.finish_try,TimerManager.getLeftTime(leftTime)));
	}



	private boolean showDownloadBar(ThemeState state){
		return state != ThemeState.STATE_NORMAL;
	}

	private void switchWidgetBar(boolean showDownloadBar){
		if(showDownloadBar){
			mDownloadOptionBtn.setText(R.string.download_pause);
		}
		mDownloadBar.setVisibility(showDownloadBar?View.VISIBLE:View.GONE);
		mNormalBar.setVisibility(showDownloadBar?View.GONE:View.VISIBLE);
	}

	private void resetProgress(){
		mProgressBar.setProgress(getResources()
				.getString(R.string.download_downloading,getPercent()),mProgress);
		FileUtils.deleteFile(mDownloadFileName);
	}

	@Override
	public void onClick(DialogInterface dialogInterface, int which) {
		if(which == DialogInterface.BUTTON_POSITIVE){
			SharePreferenceManager.setBooleanPreference(getContext(),
					SharePreferenceManager.KEY_DOWNLOAD_WITH_MOBILE_NETWORK,true);
			SharePreferenceManager.setIntPreference(getContext(),
					SharePreferenceManager.KEY_TIMES_SHOW_MOBILE_CONFIRM_DIALOG,1);
			startDownload();
		}

		dialogInterface.dismiss();
	}



	@Override
	public void onClick(View view) {

		if(view == mApplyBtn){
			//如果是系统内置资源，直接应用
			if(isSystemTheme() || getTheme().isUserImport()){
				mDialogUtils.showDialog(getContext(),DialogUtils.DIALOG_ID_APPLY_PROGRESS);
				apply();
			}else{
				if(mThemeManager.themeExists(getTheme()) && !mHasNewVersion){
					applyOrTryForIt();
				}else{
					startDownload();
				}

			}
			//如果是取消按钮，就取消下载
		}else if(view == mCancelBtn){
			mStateManager.postState(ThemeState.STATE_CANCEL);
		}else if(view == mDownloadOptionBtn){
			//如果是正在下载状态，就进入暂停状态
			if(mStateManager.getState() == ThemeState.STATE_DOWLOADING){
				mDownloadManager.pause(mDownloadInfo);
				mStateManager.postState(ThemeState.STATE_PAUSE);
				//如果是暂停状态，点击之后就继续下载
			}else if(mStateManager.getState() == ThemeState.STATE_PAUSE){
				//如果在点击继续按钮时没有网络，则弹出Toast提示无网络，否则继续下载
				if(!CommonUtil.hasNetwork(getContext())){
					ToastUtils.showShortToast(getContext(),R.string.no_network);
					return ;
				}
				mDownloadManager.resume(mDownloadInfo);
				mStateManager.postState(ThemeState.STATE_DOWLOADING);
				mDownloadOptionBtn.setText(R.string.download_pause);
			}
		}else if(view == mPayForBtn){//该处处理支付逻辑
			startPay();
		}
	}



	private void startPay(){
		SecurityManager.startPay(getTheme(), getContext(), new Pay.PayResulteListener() {
			@Override
			public void onSuccess() {
				ToastUtils.showShortToast(getContext(),R.string.pay_success);
				getTheme().buyStatus = Theme.PAID;
				mThemeManager.updateThemeinDatabase(getTheme());
				handlePaySuccess();
			}

			@Override
			public void onFailure() {
				ToastUtils.showShortToast(getContext(),R.string.pay_failure);
			}
		});
	}






	private boolean isSystemTheme(){
		return getTheme().isSystemTheme();
	}

	private boolean isDownloaded(){
		return false;
	}

	private boolean isPaid(){
		return false;
	}


	public void setupPriceStatus(){
		if(canApplyDirectly()){
			mPayForBtn.setVisibility(View.GONE);
			if(getTheme().isSystemTheme() || getTheme().isUserImport()
					|| mThemeManager.themeExists(getTheme())){
				mApplyBtn.setText(R.string.download_state_apply);
			}else {
				setDownloadText();
			}
		}else{
			mPayForBtn.setText(getResources().getString(R.string.pay_for,getTheme().price));
		}
	}

	private void setDownloadText(){
		if(mHasNewVersion){
			mApplyBtn.setText(R.string.download_new_version);
		}else{
			mApplyBtn.setText(R.string.download);
		}
	}

	private void startDownload(){
		//如果没有网络就提示无网络
		if(!CommonUtil.hasNetwork(getContext())){
			ToastUtils.showShortToast(getContext(),R.string.no_network);
			return ;
		}

		/**
		 * 如果是移动网络，弹出开启移动网络下载确认框，该弹窗只显示一次，以后不再显示
		 */
		if(showMobileNetworkConfirm()){
			mDialogUtils.showDialog(getContext(),DialogUtils.DIALOG_ID_MOBILE_NETWORK_CONFIM,this);
			return;
		}

		//如果移动网络下载开关没有打开，提示用户去设置中打开移动网络下载开关
		if(!downloadWithMobileNetwork() && CommonUtil.isMobileNetwork(getContext())){
			ToastUtils.showShortToast(getContext(),R.string.msg_open_mobile_download);
			return;
		}

		mDownloadInfo = buildDownloadInfo(getTheme());
		if(mDownloadInfo == null){
			ToastUtils.showShortToast(getContext(),getResources().getString(R.string.download_failed));
			return;
		}
		mDownloadInfo.setDownloadListener(this);

		switchWidgetBar(true);

		mDownloadManager.download(mDownloadInfo);
	}

	private boolean showMobileNetworkConfirm(){

		boolean firstTimeToShow = SharePreferenceManager.getIntPreference(getContext()
				,SharePreferenceManager.KEY_TIMES_SHOW_MOBILE_CONFIRM_DIALOG,0) == 0;
		boolean isMobileNetwork = CommonUtil.isMobileNetwork(getContext());

		return firstTimeToShow && isMobileNetwork;
	}

	private boolean downloadWithMobileNetwork(){
		return SharePreferenceManager.getBooleanPreference(getContext()
				,SharePreferenceManager.KEY_DOWNLOAD_WITH_MOBILE_NETWORK,false);
	}


	private DownloadInfo buildDownloadInfo(Theme theme){
		StringBuilder nameBuilder = new StringBuilder();
		nameBuilder.append(Config.getThemeDownloadPath(theme.type));
		nameBuilder.append(MD5Utils.encryptString(theme.id));
		if(mHasNewVersion){
			nameBuilder.append(SUFFIX_NEW_VERSION);
		}
		mDownloadFileName = nameBuilder.toString();
		try {
			TLog.d(TAG,"create download->"+theme.downloadUrl);
			DownloadInfo info = new DownloadInfo.Builder()
					.setId(theme.id)
					.setUrl(theme.downloadUrl)
					.setPath(mDownloadFileName)
					.build();
			return info;
		}catch(DownloadException e){
			TLog.e(TAG,"download info catch exception->"+e);
			return null;
		}
	}


	@Override
	public void setTheme(Theme theme) {
		super.setTheme(theme);
		mHasNewVersion = theme.hasNewVersion();
		setupPriceStatus();
		if(mThemeManager.themeExists(theme)){
			if(mThemeManager.themeApplied(theme) && (canApplyDirectly())){
				handleFreeThemeApplied();
			}else{
				mStateManager.postState(ThemeState.STATE_NORMAL);
			}

			return;
		}

		/*
		 *
		 *如果是在后台下载的，重新进入下载界面后需要恢复下载界面
		 */
		mDownloadInfo = mDownloadManager.getDownloadById(theme.id.hashCode());
		if(mDownloadInfo != null) {
			mDownloadInfo.setDownloadListener(this);
			restoreDownloadWidget(mDownloadInfo.getStatus());
		}

	}


	private void restoreDownloadWidget(int status){
		switch (status){
			case DownloadInfo.STATUS_COMPLETED:
				 mDownloadManager.remove(mDownloadInfo);
				break;
			case DownloadInfo.STATUS_DOWNLOADING:
				mStateManager.postState(ThemeState.STATE_DOWLOADING);
				mDownloadManager.resume(mDownloadInfo);
				break;
			case DownloadInfo.STATUS_PAUSED:
				switchWidgetBar(true);
				mStateManager.postState(ThemeState.STATE_PAUSE);
				mDownloadManager.pause(mDownloadInfo);
				break;
			case DownloadInfo.STATUS_REMOVED:
				mStateManager.postState(ThemeState.STATE_NORMAL);
				mDownloadManager.remove(mDownloadInfo);
				break;
			case DownloadInfo.STATUS_ERROR:
				mDownloadManager.remove(mDownloadInfo);
				mStateManager.postState(ThemeState.STATE_NORMAL);
			default:
				mStateManager.postState(ThemeState.STATE_NORMAL);
		}
	}


	@Override
	public void onStart() {
		TLog.d("download","onStart");
	}

	@Override
	public void onWaited() {
		TLog.d("download","onWaited");
		if(isAttached()){
			mProgressBar.setCurrentText(getResources().
					getString(R.string.download_onwaited));
		}
	}

	@Override
	public void onPaused() {
		synchronized (ThemePreviewDonwloadButton.this) {
			mStateManager.postState(ThemeState.STATE_PAUSE);
		}
	}

	@Override
	public void onDownloading(long progress, long size) {
		synchronized (ThemePreviewDonwloadButton.this) {
			if (!mAddedToWindow) {
				return;
			}
			if (mTotalBytes == 0L) {
				mTotalBytes = size;
			}
			if (mTotalSize == 0) {
				mTotalSize = (int) size / 1000 ;
			}
			mProgress = (int) progress / 1000 ;
			mStateManager.postState(ThemeState.STATE_DOWLOADING);
		}
	}

	@Override
	public void onRemoved() {

	}

	@Override
	public void onDownloadSuccess() {
		synchronized (ThemePreviewDonwloadButton.this) {
			getTheme().themeFilePath = mDownloadFileName;
			getTheme().downloadStatus = Theme.DOWNLOADED;
			getTheme().size = CommonUtil.getReadableFileSize(mTotalBytes);
			getTheme().hasNewVersion = Theme.NO_NEWVERSION;
			mThemeManager.loadThemeIntoDatabase(getTheme());
			mStateManager.postState(ThemeState.STATE_DOWNLOADED);
			if (mDownloadInfo != null) {
				mDownloadInfo.setDownloadListener(null);
			}
			mDownloadManager.remove(mDownloadInfo);
			postDownload();
		}
	}

	/**
	 * 上报下载次数
	 */
	private void postDownload(){
		Http http = Http.getHttp(getContext());
		DownloadPost post = new DownloadPost(getContext(),getTheme().type);
		post.id = getTheme().id;
		http.post(getContext(),post.getMyUrl(),post.createJsonRequest(),null);
	}

	@Override
	public void onDownloadFailed(DownloadException e) {
		TLog.e(TAG,"download failed->"+e);
		if(isAttached()){
			mProgressBar.setCurrentText(getResources().
					getString(R.string.download_failed));
		}

		if(mDownloadInfo != null){
			mDownloadInfo.setDownloadListener(null);
		}
		mDownloadManager.remove(mDownloadInfo);
	}


	private boolean isAttached(){
		return mAddedToWindow && mDownloadInfo != null;
	}



	static class DownloadPost extends ThemeRequest{

		@JSONField(serialize = false)
		public String id;
		private Context mContext;
		public DownloadPost(Context context, int themeType) {
			super(context, themeType);
			mContext = context.getApplicationContext();
			setUrl(Config.HttpUrl.DOWNLOAD_POST);
		}

		@Override
		public Response parseResponse(String responseStr) {
			return JSON.parseObject(responseStr,DownloadPostResponse.class);
		}


		@Override
		protected void generateRequestBody() {
			User user = User.getInstance(mContext);
			DownloadPostBody body = new DownloadPostBody();
			body.setQlcId(user.isLogin()?Long.parseLong(user.getId()):0);
			body.setId(id);
			body.setType(getThemeType());
			body.setupAvaliableProperties("id","type","qlcId");
			setBody(body);
		}


	}


	static class DownloadPostBody extends RequestBody{
		public long qlcId;

		public long getQlcId() {
			return qlcId;
		}

		public void setQlcId(long qlcId) {
			this.qlcId = qlcId;
		}
	}


	static class DownloadPostResponse extends Response{

		public String errorMsg;

		public String getErrorMsg() {
			return errorMsg;
		}

		public void setErrorMsg(String errorMsg) {
			this.errorMsg = errorMsg;
		}
	}

}
