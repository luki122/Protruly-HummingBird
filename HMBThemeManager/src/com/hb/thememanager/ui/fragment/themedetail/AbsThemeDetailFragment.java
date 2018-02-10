package com.hb.thememanager.ui.fragment.themedetail;

import hb.app.dialog.ProgressDialog;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.hb.thememanager.R;

import android.os.Handler;

import com.hb.thememanager.http.response.DesignerThemeResponse;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.ThemeCommentsResponse;
import com.hb.thememanager.http.response.ThemePkgDetailResponse;
import com.hb.thememanager.model.Comments;
import com.hb.thememanager.model.PreviewTransitionInfo;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.User;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.TLog;
import com.hb.thememanager.utils.ToastUtils;
import com.hb.thememanager.views.ExpandableTextView;
import com.hb.thememanager.views.ThemePreviewDonwloadButton;
import com.hb.thememanager.views.IconImageView;


public abstract class AbsThemeDetailFragment extends AbsDetailFragment implements OnClickListener, AdapterView.OnItemClickListener{
	private static final String TAG = "DetailFragment";
	private static final int MAX_SHOWING_DOWNLOAD_TIME = 1000;
	private static final int TIP_VIEW_SHOW_MAX_COUNT = 3000;
	private static final int DETAIL_PAGE_COMMENT_COUNT = 3;
	private static final String TIP_MAX_COUNT_STR = TIP_VIEW_SHOW_MAX_COUNT+"+";
	protected static final int MAX_DESIGNER_THEME = 3;



	protected TextView mDownloadTimes;
	protected TextView mEmptyView;
	protected TextView mDesignerThemeTitle;
	protected TextView mDesignerThemeMoreBtn;
	protected TextView mThemeScore;
	protected RatingBar mScoreBar;
	protected GridView mDesingerThemeList;
	protected View mLoadingView;
	protected View mDetailParentView;

	protected boolean mHasComment = true;

	protected boolean mStartRequestDesignerTheme = false;

	protected LinearLayout mCommentsParent;

	protected ThemeCommentsResponse mCommentResponse;
	



	private Handler mHandler = new Handler();

	private List<Comments> mComments;




	private Runnable mUpdateCommentsAction = new Runnable() {
		@Override
		public void run() {
			int showCount = mComments.size() > DETAIL_PAGE_COMMENT_COUNT
					?DETAIL_PAGE_COMMENT_COUNT
					:mComments.size();
			for(int i = 0;i<showCount;i++){
				Comments comment = mComments.get(i);
				View commentItem = getInflater().inflate(R.layout.list_item_theme_detail_comments,null);
				setupComment(commentItem,comment);
			}
			updateTipView(false,mComments.size());
		}
	};
	private Runnable mUpdateThemeInfoAction = new Runnable() {
		@Override
		public void run() {
			updateThemeInfo(mCurrentTheme);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Bundle args = getBundle();
		if(args != null){
			mCurrentTheme = args.getParcelable(Config.ActionKey.KEY_THEME_PKG_DETAIL);
			
		}
		
		
	}




	private void updateTipView(boolean clearViews,int commentCount){
		if(!isAdded()){
			return;
		}
		if(clearViews) {
			mCommentsParent.removeAllViews();
		}
		View tipsView = getInflater().inflate(R.layout.comments_tips_view,null);
		TextView tip = (TextView)tipsView.findViewById(R.id.theme_comment_tip);
		View emptyTip = tipsView.findViewById(R.id.theme_comment_empty);
		tip.setOnClickListener(this);
		if(commentCount == 0){
			mHasComment = false;
			tip.setText(R.string.add_comments);
			emptyTip.setVisibility(View.VISIBLE);
		}else{
			emptyTip.setVisibility(View.GONE);
			if(commentCount > TIP_VIEW_SHOW_MAX_COUNT){
				tip.setText(getString(R.string.comment_tip_count,TIP_MAX_COUNT_STR));
			} else{
				tip.setText(getString(R.string.comment_tip_count,commentCount));
			}
		}
		mCommentsParent.addView(tipsView,
				new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT));
	}


	private void setupComment(View commentItemView,Comments comment){
		IconImageView icon = (IconImageView)commentItemView.findViewById(R.id.icon);
		TextView name = (TextView)commentItemView.findViewById(R.id.name);
		TextView time = (TextView)commentItemView.findViewById(R.id.time);
		ExpandableTextView detail = (ExpandableTextView)commentItemView.findViewById(R.id.comment);
		RatingBar rating = (RatingBar)commentItemView.findViewById(R.id.rating);
		name.setText(comment.nickname);
		time.setText(comment.getRealTime(getResources()));
		detail.setText(comment.getContent());
		int grade = comment.starLevel;

		rating.setRating(grade);
		mCommentsParent.addView(commentItemView,
				new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT));
	}
	
	public Bundle getBundle() {
		// TODO Auto-generated method stub
		return getArguments();
	}
	
	
	@Override
	public void initialWhenCreateView(View container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub

		initView();
		if(mHasComment){
			getPresenter().requestComments(mCurrentTheme.type);
		}else{
			updateTipView(true,0);
		}
		getPresenter().requestDetail(mCurrentTheme.type);
	}





	protected void initView() {
		// TODO Auto-generated method stub
		mPreviewScroller = (LinearLayout)findViewById(R.id.theme_pkg_detail_preview_scroller);
		mDesigner = (TextView)findViewById(R.id.theme_detail_designer);
		mThemeSize = (TextView)findViewById(R.id.theme_detail_size);
		mDownloadTimes = (TextView)findViewById(R.id.theme_detail_download_time);
		mThemeName = (TextView)findViewById(R.id.theme_detail_name);
		mThemeScore = (TextView)findViewById(R.id.theme_detail_score);
		mDescription = (ExpandableTextView)findViewById(R.id.theme_detail_description);
		mOptionBtn = (ThemePreviewDonwloadButton)findViewById(R.id.theme_detail_option_btn);
		mCommentsParent = (LinearLayout)findViewById(R.id.theme_detail_comments_parent) ;
		mScoreBar = (RatingBar)findViewById(R.id.theme_detail_rating);
		mLoadingView = findViewById(R.id.loading_widget);
		mDetailParentView = findViewById(R.id.theme_detail_parent);
		mDetailParentView.setVisibility(View.GONE);
		mOptionBtn.setVisibility(View.GONE);
		mOptionBtn.setTheme(mCurrentTheme);

		new Handler().post(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				getPresenter().loadThemePreview();
			}
		});
	}
	

	
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mOptionBtn.onResumeTimer();
	}



	@Override
	public void updateThemeInfo(Theme theme) {
		// TODO Auto-generated method stub
		if(!isAdded()){
			return;
		}
		if(theme != null){
			if(mStartRequestDesignerTheme) {
				getPresenter().requestDesignerThemes(mCurrentTheme.type);
				mLoadingView.setVisibility(View.GONE);
				mDetailParentView.setVisibility(View.VISIBLE);
				mOptionBtn.setVisibility(View.VISIBLE);
				mCurrentTheme.designerId = theme.designerId;
				mOptionBtn.setTheme(mCurrentTheme);
			}
			String downloadStr = theme.downloadTimes;
			try{
				final int downloadTime = Integer.valueOf(theme.downloadTimes);
				if(downloadTime > MAX_SHOWING_DOWNLOAD_TIME){
					downloadStr += getString(R.string.theme_download_time_suffix_1);
				}else{
					downloadStr += getString(R.string.theme_download_time_suffix);
				}
				
			}catch(Exception e){
			}
			mDownloadTimes.setText(downloadStr);
			mThemeName.setText(theme.name);
			mDesigner.setText(theme.designer);
			mThemeSize.setText(getString(R.string.theme_size_suffix,theme.size));
			mDescription.setText(theme.description);
			mScoreBar.setRating(theme.grade*mScoreBar.getNumStars()/10);
			mThemeScore.setText(String.valueOf(theme.grade));
			createPreview(theme);
		}
	}

	protected abstract void createPreview(Theme theme);

	protected abstract void handleClickEvents(View v);



	protected abstract void setupDesignerTheme(List<Theme> themes);


	public static Drawable[] getPreviewDrawables(){
		return sPreviewDrawable;
	}
	
	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mOptionBtn.onPausedTimer();
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.theme_comment_tip && !mHasComment){
			int addCommentTips = CommonUtil.getAddCommentTips(getContext(),mCurrentTheme);
			if(addCommentTips != 0){
				ToastUtils.showShortToast(getContext(),addCommentTips);
				return;
			}

			User user = CommonUtil.getUser(getContext());
			if(!user.isLogin()){
				user.jumpLogin(null,getActivity());
				return;
			}

			Intent intent = new Intent(Config.Action.ACTION_ADD_COMMENTS);
			intent.putExtra(Config.ActionKey.KEY_HANDLE_COMMENTS,mCurrentTheme);
			startActivity(intent);
		}else{
			handleClickEvents(v);
		}

	}



	@Override
	public void showEmptyView(boolean show) {

	}

	@Override
	public void showNetworkErrorView(boolean show) {

	}

	@Override
	public void showRequestFailView(boolean show) {

	}



	@Override
	public void update(Response result) {
		if(result == null || Integer.parseInt(result.retCode) == Response.STATUS_CODE_ERROR){
			showRequestFailView(true);
			return;
		}
		if(result instanceof ThemeCommentsResponse){
			mCommentResponse = (ThemeCommentsResponse)result;
			if(mCommentResponse.getComments() != null || mCommentResponse.getComments().size() != 0){
				mComments = mCommentResponse.getComments();
				mHandler.post(mUpdateCommentsAction);
			}
		}else if(result instanceof ThemePkgDetailResponse){
			((ThemePkgDetailResponse)result).updateTheme(mCurrentTheme);
			mStartRequestDesignerTheme = true;
			mHandler.post(mUpdateThemeInfoAction);
		}else if(result instanceof DesignerThemeResponse){

			List<Theme> themes = ((DesignerThemeResponse) result).getThemes(mCurrentTheme.type);
			if(themes != null && themes.size() > 0){
				setupDesignerTheme(themes);
			}
		}
	}
}
