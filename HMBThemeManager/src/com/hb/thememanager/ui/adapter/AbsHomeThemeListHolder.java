package com.hb.thememanager.ui.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hb.thememanager.ThemeManager;
import com.hb.thememanager.ThemeManagerImpl;
import com.hb.thememanager.model.HomeThemeCategory;
import com.hb.thememanager.model.HomeThemeHeaderCategory;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.ui.LoadMoreActivity;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.IntentUtils;
import com.hb.thememanager.views.BannerView;
import com.hb.thememanager.views.HomeThemeListItem;
import com.hb.thememanager.views.IconFastEntry;
import com.hb.thememanager.R;
/**
 *首页列表在各个Tab下对应的布局都差不多，继承这个类去实现不同的
 *主题列表布局和数据绑定。 
 *
 */
public abstract class AbsHomeThemeListHolder extends AbsViewHolder<HomeThemeCategory>{
	private IconFastEntry mRankIcon;
	private IconFastEntry mCategoryIcon;
	private IconFastEntry mTopicIcon;
	private BannerView mBannerView;
	private TextView mTitle;
	private View mMoreView;
	private int mCategoryType = HomeThemeCategory.TYPE_NONE;
	private int mThemeType = Theme.THEME_NULL;
	public static final int MAX_CATEGORY_ITEM = 6;
	private ViewGroup mThemeList;
	private ThemeManager mTm;
	public AbsHomeThemeListHolder(Context context, ListAdapter adapter,int categoryType,int themeType) {
		super(context, adapter);
		// TODO Auto-generated constructor stub
		mCategoryType = categoryType;
		mThemeType = themeType;
		mTm = ThemeManagerImpl.getInstance(context);
	}
	

	@Override
	public void holdConvertView(View convertView) {
		// TODO Auto-generated method stub
		if(mCategoryType == HomeThemeCategory.TYPE_HEADER){
			inititalBannerView(convertView);
			inititalFastEntryView(convertView);
		}else if(mCategoryType == HomeThemeCategory.TYPE_CATEGORY){
			mTitle = (TextView)convertView.findViewById(R.id.home_theme_category_title);
			mMoreView = convertView.findViewById(R.id.home_theme_category_more);

			initialThemeList(convertView);
		}
	}

	@Override
	public void bindDatas(int position, List<HomeThemeCategory> categories) {
		// TODO Auto-generated method stub
		final HomeThemeCategory category = categories.get(position);
		int categoryType = mCategoryType;

		if(mMoreView != null){
			mMoreView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Intent intent = new Intent(view.getContext(), LoadMoreActivity.class);
					intent.putExtra(Config.ActionKey.KEY_LOAD_MORE_TYPE,mThemeType);
					intent.putExtra(Config.ActionKey.KEY_LOAD_MORE_ID,category.id);
					intent.putExtra(Config.ActionKey.KEY_LOAD_MORE_NAME,category.getName());
					ArrayList<Theme> themes = new ArrayList<Theme>();
					themes.addAll(category.getThemes());
					intent.putParcelableArrayListExtra(Config.ActionKey.KEY_LOAD_MORE_LIST,themes);
					view.getContext().startActivity(intent);
				}
			});
		}
		
		if(category instanceof HomeThemeHeaderCategory && categoryType == HomeThemeCategory.TYPE_HEADER){
			bindHeaderBanner((HomeThemeHeaderCategory)category);
			bindFastEntryView((HomeThemeHeaderCategory)category);
		}else if(categoryType == HomeThemeCategory.TYPE_CATEGORY){
			mTitle.setText(category.getName());
			bindThemeListItem(position, category);
		}
		
		
	}
	
	
	
	private void inititalFastEntryView(View convertView){
		mRankIcon = (IconFastEntry)convertView.findViewById(R.id.icon_rank);
		mCategoryIcon = (IconFastEntry)convertView.findViewById(R.id.icon_category);
		mTopicIcon = (IconFastEntry)convertView.findViewById(R.id.icon_topic);
		mRankIcon.setThemeType(mThemeType);
		mCategoryIcon.setThemeType(mThemeType);
		mTopicIcon.setThemeType(mThemeType);
	}

	/**
	 * 初始化主题列表View
	 * @param convertView
	 */
	protected void initialThemeList(View convertView){
		mThemeList = (ViewGroup) convertView.findViewById(R.id.home_theme_category_list);
	}
	
	protected  void bindThemeListItem(int position, HomeThemeCategory category){
		List<Theme> themes = category.getThemes();
		int size = themes.size()>MAX_CATEGORY_ITEM?MAX_CATEGORY_ITEM:themes.size();
		mMoreView.setVisibility(size <= MAX_CATEGORY_ITEM?View.GONE:View.VISIBLE);
		for(int themePosition = 0; themePosition < size; themePosition++){
			final Theme theme = themes.get(themePosition);
			CommonUtil.getThemePrice(theme,getContext());
			attachItemData(position, themePosition,theme,mThemeList.getChildAt(themePosition), category.id);
		}
	}

	protected abstract void attachItemData(int item, int position,Theme theme,View itemView, int recommendId);
	
	private void inititalBannerView(View convertView){
		mBannerView = (BannerView)convertView.findViewById(R.id.banner);
	}

	private void bindHeaderBanner(HomeThemeHeaderCategory category){
		if(category.getBanners() == null || category.getBanners().size() == 0){
			mBannerView.setVisibility(View.GONE);
		}else{
			mBannerView.bindData(category.getBanners());
			mBannerView.startScroll();
		}

	}
	
	
	private void bindFastEntryView(HomeThemeHeaderCategory category){

	}



}
