package com.hb.thememanager.ui.fragment.themedetail;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hb.thememanager.job.loader.ImageLoader;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.R;
import com.hb.thememanager.ui.LoadMoreActivity;
import com.hb.thememanager.ui.adapter.DesignerThemeAdapter;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.IntentUtils;

public class ThemePkgDetailFragment extends AbsThemeDetailFragment{

	private int mPreviewImageWidth;
	private int mPreviewImageHeight;
	private int mImageLeftMargin;
	protected DesignerThemeAdapter mDesignerThemeAdapter;
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mPreviewImageWidth = getResources().getDimensionPixelSize(R.dimen.theme_detail_preview_img_width);
		mPreviewImageHeight = getResources().getDimensionPixelSize(R.dimen.theme_detail_preview_img_height);
		mImageLeftMargin = getResources().getDimensionPixelSize(R.dimen.theme_detail_preview_img_margin_left);
	}



	@Override
	protected void createPreview(Theme theme){
		
		int imagesCount = theme.previewArrays.size();
		final boolean isDefaultTheme = theme.isDefaultTheme();
		if(isDefaultTheme){
			imagesCount = Config.DEFAUTL_THEME_PREVIEWS.length;
		}
		if(imagesCount == 0){
			return;
		}
		mPreviewScroller.removeAllViews();
		for(int i = 0;i < imagesCount;i++){
			final ImageView image = new ImageView(getActivity());
			image.setScaleType(ScaleType.CENTER_CROP);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mPreviewImageWidth,
					mPreviewImageHeight);
			if(i >0){
				params.leftMargin = mImageLeftMargin;
			}
			mPreviewScroller.addView(image, params);
			image.setOnClickListener(this);
			image.setId(i);
			if(isDefaultTheme){
				ImageLoader.loadStringRes(image, Config.DEFAUTL_THEME_PREVIEWS[i], null, null);
			}else{
				ImageLoader.loadStringRes(image, theme.previewArrays.get(i), null,null);
			}
			
		}
		
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		Theme theme = mDesignerThemeAdapter.getItem(position);
		Intent intent = IntentUtils.buildHomeThemeListIntent(Config.Action.ACTION_HOME_THEME_LIST_ITEM_DETAIL,
				theme);
		getContext().startActivity(intent);
	}

	@Override
	public int getLayoutRes() {
		return R.layout.theme_pkg_detail_layout;
	}

	@Override
	protected void setupDesignerTheme(List<Theme> themes) {
		ViewStub designerTheme = (ViewStub) findViewById(R.id.theme_detail_designer_themes_stub);
		designerTheme.inflate();
		if(mDesingerThemeList == null) {
			mDesingerThemeList = (GridView) findViewById(R.id.designer_theme_list);
		}
		if(mDesignerThemeAdapter == null){
			mDesignerThemeAdapter = new DesignerThemeAdapter(getContext(),R.layout.list_item_designer_theme);
			mDesingerThemeList.setAdapter(mDesignerThemeAdapter);
			mDesingerThemeList.setOnItemClickListener(this);
		}

		if(mDesignerThemeTitle == null){
			mDesignerThemeTitle = (TextView)findViewById(R.id.home_theme_category_title);
			mDesignerThemeTitle.setText(R.string.designer_theme_title);
			mDesignerThemeTitle.setPadding(0,0,0,0);
		}

		if(mDesignerThemeMoreBtn == null){
			mDesignerThemeMoreBtn = (TextView)findViewById(R.id.home_theme_category_more);
			mDesignerThemeMoreBtn.setPadding(0,0,0,0);
			mDesignerThemeMoreBtn.setOnClickListener(this);
		}

		ArrayList<Theme> maxThemeList = new ArrayList<Theme>();
		if(themes.size() <= MAX_DESIGNER_THEME){
			maxThemeList.addAll(themes);
			mDesignerThemeMoreBtn.setVisibility(View.GONE);
		}else{
			for(int i = 0;i< MAX_DESIGNER_THEME;i++){
				maxThemeList.add(themes.get(i));
			}
			mDesignerThemeMoreBtn.setVisibility(View.VISIBLE);
		}
		mDesignerThemeAdapter.addThemes(maxThemeList);
	}

	public static Drawable[] getPreviewDrawables(){
		return sPreviewDrawable;
	}
	
	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	public void handleClickEvents(View v) {
		// TODO Auto-generated method stub
		int id = v.getId();
		if(id == R.id.theme_comment_tip){
			Intent commentIntent = new Intent(Config.Action.ACTION_SHOW_COMMENTS);
			commentIntent.putParcelableArrayListExtra(Config.ActionKey.KEY_SHOW_COMMENTS,mCommentResponse.getComments());
			commentIntent.putExtra(Config.ActionKey.KEY_HANDLE_COMMENTS,mCurrentTheme);
			startActivity(commentIntent);
		}else if(id == R.id.home_theme_category_more){
			Intent intent = new Intent(getContext(), LoadMoreActivity.class);
			intent.putExtra(Config.ActionKey.KEY_LOAD_MORE_TYPE,mCurrentTheme.type);
			intent.putExtra(Config.ActionKey.KEY_LOAD_MORE_NAME,mCurrentTheme.designer);
			intent.putExtra(Config.ActionKey.KEY_LOAD_MORE_DESIGNER_ID,mCurrentTheme.designerId);
			ArrayList<Theme> themes = new ArrayList<Theme>();
			themes.addAll(mDesignerThemeAdapter.getThemes());
			intent.putParcelableArrayListExtra(Config.ActionKey.KEY_LOAD_MORE_LIST,themes);
			getContext().startActivity(intent);
		}else{
			startPreviewPage(v);
		}

	}



}
