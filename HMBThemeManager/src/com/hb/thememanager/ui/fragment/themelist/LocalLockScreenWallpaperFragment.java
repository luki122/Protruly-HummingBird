package com.hb.thememanager.ui.fragment.themelist;

import hb.widget.FloatingActionButton;
import hb.widget.FloatingActionButton.OnFloatActionButtonClickListener;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.hb.thememanager.ThemeManagerApplication;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.ui.SetDesktopWallpaperActivity;
import com.hb.thememanager.ui.SetLockScreenWallpaperActivity;
import com.hb.thememanager.ui.adapter.LocalWallpaperAdapter;
import com.hb.thememanager.ui.fragment.AbsLocalThemeFragment;
import com.hb.thememanager.ui.fragment.themedetail.LockScreenWallpaperPreviewActivity;
import com.hb.thememanager.ui.fragment.themedetail.WallpaperPreviewActivity;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.IntentUtils;
import com.hb.thememanager.R;

public class LocalLockScreenWallpaperFragment extends AbsLocalWallpaperListFrag implements OnItemClickListener,
		OnFloatActionButtonClickListener{

	private static final int MSG_UPDATE_THEME_LISTS = 0x01;
	private LocalWallpaperAdapter mAdapter;
	private GridView mWallpaperList;
	private ThemeManagerApplication mApp;
	private FloatingActionButton mAddWallpaperBtn;
	private boolean mStartedActivity = false;
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			if(msg.what == MSG_UPDATE_THEME_LISTS){
				@SuppressWarnings("unchecked")
				Wallpaper wallpaper  = (Wallpaper) msg.obj;
				wallpaper.type = Wallpaper.LOCKSCREEN_WALLPAPER;
				mAdapter.addTheme(wallpaper);
			}
		};
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mAdapter = new LocalWallpaperAdapter(getContext());
		mAdapter.setImageLoader(getImageLoader());
		mApp = (ThemeManagerApplication) getContext().getApplicationContext();
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if(Config.NEED_UPDATE_LOCKSCREEN_WALLPAPER_LIST){
			mApp.getThemeManager().loadThemesFromDatabase(Theme.LOCKSCREEN_WALLPAPER);
			Config.NEED_UPDATE_LOCKSCREEN_WALLPAPER_LIST = false;
		}
		mStartedActivity =  false;
	}

	protected LocalWallpaperAdapter getAdapter() {
		return mAdapter;
	}

	@Override
	public void showTips(int status) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(mWallpaperList != null) {
			mWallpaperList.setAdapter(null);
		}
	}

	@Override
	public void updateThemeList(Theme theme) {
		// TODO Auto-generated method stub
		Message msg = mHandler.obtainMessage(MSG_UPDATE_THEME_LISTS);
		msg.obj = theme;
		msg.sendToTarget();
	}


	@Override
	protected void initView() {
		// TODO Auto-generated method stub
		mWallpaperList = (GridView)getContentView().findViewById(R.id.wallpaper_list_grid);
		mAddWallpaperBtn = (FloatingActionButton)getContentView().findViewById(R.id.btn_add_wallpaper);
		mAddWallpaperBtn.setOnFloatingActionButtonClickListener(this);
		mWallpaperList.setAdapter(mAdapter);
		mWallpaperList.setOnItemClickListener(this);
		mWallpaperList.setOnItemLongClickListener(this);
		mApp.loadInternalLockScreenWallpaper();
	}


	@Override
	public void updateThemeLists(List theme) {
		// TODO Auto-generated method stub

	}

	private Intent buildPreviewIntent(int currentItem){
		Intent intent = new Intent(getActivity(),WallpaperPreviewActivity.class);
		ArrayList<Wallpaper> wallpapers = (ArrayList<Wallpaper>) mAdapter.getThemes();
		Bundle bundle = new Bundle();
		bundle.putParcelableList(Config.ActionKey.KEY_WALLPAPER_PREVIEW_LIST, wallpapers);
		bundle.putInt(Config.ActionKey.KEY_WALLPAPER_PERVIEW_CURRENT_ITEM, currentItem);
		bundle.putInt(Config.ActionKey.KEY_WALLPAPER_PREVIEW_TYPE, Theme.LOCKSCREEN_WALLPAPER);
		intent.putExtra(Config.ActionKey.KEY_WALLPAPER_PREVIEW_BUNDLE, bundle);
		return intent;
	}


	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {

		Wallpaper wallpaper = mAdapter.getItem(position);
		if(wallpaper.isSystemTheme() || wallpaper.isDefaultTheme()){
			return true;
		}
		return super.onItemLongClick(adapterView, view, position, id);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
							long id) {
		if(isEditMode()){
			Theme theme = (Theme)getAdapter().getTheme(position);
			if(theme.isSystemTheme()){
				return;
			}
			getAdapter().selectOrUnSelectTheme(position,!getAdapter().isSelected(position));
			updateActionModeTitle(getAdapter().getSelectedItems().size());
			return;
		}

		if(!mStartedActivity) {
			mStartedActivity =  true;
			Intent intent = buildPreviewIntent(position);
			ImageView image = (ImageView)view.findViewById(R.id.wallpaper_item);
			Config.sStaringImageInPreview  = image.getDrawable();
			ActivityOptions ops = ActivityOptions.makeScaleUpAnimation(image, (int)image.getX() / 2,  (int)image.getY() / 2,image.getWidth(),image.getHeight());
			startActivity(intent,ops.toBundle());
		}
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
//		startActivityForResult(IntentUtils.buildPickerDesktopWallpaperIntent(), IntentUtils.REQUEST_CODE_ADD_LOCKSCREEN_WALLPAPER);
		startActivity(new Intent(getContext(), SetLockScreenWallpaperActivity.class));
	}

	@Override
	public void showEmptyView(boolean show) {

	}

	@Override
	public void showNetworkErrorView(boolean show) {

	}

//	@Override
//	public void onActivityResult(int requestCode, int resultCode, Intent data) {
//		if (resultCode == Activity.RESULT_OK
//				&& (requestCode == IntentUtils.REQUEST_CODE_ADD_LOCKSCREEN_WALLPAPER)) {
//			if (data != null && data.getData() != null) {
//				Uri uri = data.getData();
//				Intent request = new Intent(getContext(), SetLockScreenWallpaperActivity.class);
//				request.setDataAndType(uri, IntentUtils.IMAGE_TYPE)
//						.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
//				startActivity(request);
//			}
//		}else{
//			super.onActivityResult(requestCode, resultCode, data);
//		}
//
//	}
	
}
