package com.hb.thememanager.ui.fragment.themedetail;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hb.thememanager.job.loader.ImageLoader;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.ui.LoadMoreActivity;
import com.hb.thememanager.ui.adapter.DesignerFontsAdapter;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.R;
import com.hb.thememanager.utils.IntentUtils;
import com.hb.thememanager.utils.ViewUtils;
import com.hb.thememanager.views.ThemePreviewDonwloadButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexluo on 17-8-9.
 */

public class FontPkgDetailFragment extends AbsThemeDetailFragment {

    private static final int DESIGNER_THEME_COUNT = 4;
    protected DesignerFontsAdapter mDesignerThemeAdapter;
    private int mPreviewWidth;
    private int mPreviewHeight;
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPreviewWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mPreviewHeight = getContext().getResources().getDimensionPixelOffset(R.dimen.font_detail_preview_img_height);
    }


    @Override
    protected void createPreview(Theme theme){

        int imagesCount = theme.previewArrays.size();
        if(theme.isDefaultTheme()){
            imagesCount = Config.DEFAUTL_THEME_PREVIEWS.length;
        }
        if(imagesCount == 0){
            return;
        }
        mPreviewScroller.removeAllViews();
        for(int i = 0;i < imagesCount;i++){
            final ImageView image = new ImageView(getActivity());
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mPreviewWidth, mPreviewHeight);
            mPreviewScroller.addView(image, params);
            image.setId(i);
            if(theme.isDefaultTheme()){
                ImageLoader.loadStringRes(image, Config.DEFAUTL_THEME_PREVIEWS[i], null, null);
            }else{
                ImageLoader.loadStringRes(image, theme.previewArrays.get(i), null,null);
            }

        }

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

        Theme theme = mDesignerThemeAdapter.getItem(position);
        theme.type = Theme.FONTS;
        Intent intent = IntentUtils.buildHomeThemeListIntent(Config.Action.ACTION_HOME_THEME_LIST_ITEM_DETAIL, theme);
        getContext().startActivity(intent);

    }

    @Override
    public int getLayoutRes() {
        return R.layout.font_pkg_detail_layout;
    }

    @Override
    protected void setupDesignerTheme(List<Theme> themes) {
        ViewStub designerTheme = (ViewStub) findViewById(R.id.theme_detail_designer_themes_stub);
        designerTheme.inflate();
        if(mDesingerThemeList == null) {
            mDesingerThemeList = (GridView) findViewById(R.id.designer_theme_list);
        }
        if(mDesignerThemeAdapter == null){
            mDesignerThemeAdapter = new DesignerFontsAdapter(getContext(),R.layout.list_item_designer_fonts);
            mDesingerThemeList.setNumColumns(2);
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
        if(themes.size() <= DESIGNER_THEME_COUNT){
            maxThemeList.addAll(themes);
            mDesignerThemeMoreBtn.setVisibility(View.GONE);
        }else{
            for(int i = 0;i< DESIGNER_THEME_COUNT;i++){
                maxThemeList.add(themes.get(i));
            }
            mDesignerThemeMoreBtn.setVisibility(View.VISIBLE);
        }
        mDesignerThemeAdapter.addThemes(maxThemeList);
        ViewUtils.setGridViewHeightBasedOnChildren(2,mDesingerThemeList,mDesignerThemeAdapter);
        mDesignerThemeAdapter.notifyDataSetChanged();
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
            intent.putExtra(Config.ActionKey.KEY_LOAD_MORE_TYPE,Theme.FONTS);
            intent.putExtra(Config.ActionKey.KEY_LOAD_MORE_NAME,mCurrentTheme.designer);
            intent.putExtra(Config.ActionKey.KEY_LOAD_MORE_DESIGNER_ID,mCurrentTheme.designerId);
            ArrayList<Theme> themes = new ArrayList<Theme>();
            themes.addAll(mDesignerThemeAdapter.getThemes());
            intent.putParcelableArrayListExtra(Config.ActionKey.KEY_LOAD_MORE_LIST,themes);
            getContext().startActivity(intent);
        }

    }

}
