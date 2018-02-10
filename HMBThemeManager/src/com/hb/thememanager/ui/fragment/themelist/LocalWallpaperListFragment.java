package com.hb.thememanager.ui.fragment.themelist;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;

import com.hb.thememanager.database.DatabaseFactory;
import com.hb.thememanager.database.ThemeDatabaseController;
import com.hb.thememanager.http.downloader.DownloadInfo;
import com.hb.thememanager.http.downloader.DownloadManagerImpl;
import com.hb.thememanager.http.downloader.DownloadService;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.R;
import com.hb.thememanager.ThemeManagerApplication;
import com.hb.thememanager.ui.adapter.LocalThemeListAdapter;
import com.hb.thememanager.ui.adapter.LocalThemeWallpaperListAdapter;
import com.hb.thememanager.ui.fragment.themedetail.LocalFontDetailFragment;
import com.hb.thememanager.ui.mvpview.WallpaperDetailPresenter;
import com.hb.thememanager.utils.Config;

/**
 * Created by alexluo on 17-8-21.
 */

public class LocalWallpaperListFragment extends LocalThemeListFragment {
	private static final String TAG = "LocalWallpaperListFragment";
	private DownloadManagerImpl mDm;
	private List<Theme> mWallpapers;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mShowThemePickerAction = false;
		return super.onCreateView(inflater, container, savedInstanceState);
	}

//	@Override
//	protected void loadUserWallpaper() {
//		loadLocalWallpaper();
//		super.loadUserWallpaper();
//	}
//
//	@Override
//	public void onResume() {
//		ThemeManagerApplication app = (ThemeManagerApplication) getActivity().getApplication();
//		app.loadInternalTheme(Theme.WALLPAPER);
//		super.onResume();
//	}

//	private void loadLocalWallpaper() {
//		mWallpapers = new ArrayList<>();
//		Theme wallpaper;
//		if(mDm == null) {
//			mDm = (DownloadManagerImpl)DownloadService.getDownloadManager(getContext());
//		}
//		List<DownloadInfo> infos = mDm.findAllDownloaded();
//		for (DownloadInfo info : infos) {
//			String filePath = info.getPath();
//			if(filePath.contains(Config.LOCAL_THEME_WALLPAPER_PATH)) {
//				wallpaper = new Theme();
//				wallpaper.downloadUrl = info.getUri();
//				wallpaper.type = Theme.WALLPAPER;
//				wallpaper.lastModifiedTime = info.getCreateAt();
//				wallpaper.id = String.valueOf(info.getId());
//				mWallpapers.add(wallpaper);
//			}
//		}
//		Collections.sort(mWallpapers, new Comparator<Theme>() {
//			@Override
//			public int compare(Theme lhs, Theme rhs) {
//				return (int) (lhs.lastModifiedTime - rhs.lastModifiedTime);
//			}
//		});
//		
//		mAdapter.addThemes(mWallpapers);
//	}
	
	@Override
	protected void deleteSelectTheme() {
		if(mDm == null) {
			mDm = (DownloadManagerImpl)DownloadService.getDownloadManager(getContext());
		}
		SparseArray<Theme> selectThemes = mAdapter.getSelectedItems();
		ArrayList<Theme> deleteThemes = new ArrayList<Theme>();
		for(int i = 0;i< selectThemes.size();i++){
			int key = selectThemes.keyAt(i);
			deleteThemes.add(selectThemes.get(key));
		}
		ThemeDatabaseController controller = DatabaseFactory.createDatabaseController(Theme.LOCKSCREEN_WALLPAPER, getContext());
		for (Theme theme : deleteThemes) {
			if(theme.themeFilePath != null && theme.themeFilePath.length() > 0) {
				DownloadInfo info = mDm.getDownloadByPath(theme.themeFilePath);
				if(info != null) {
					mDm.remove(info);
//					File file = new File(info.getPath());
//					if(file.isFile())
//						file.delete();
				}
			}
			Theme t = controller.getThemeByPath(theme.themeFilePath);
			mThemeManager.deleteTheme(t);
		}
		super.deleteSelectTheme();
	}

	@Override
    public int getLayoutRes() {
        return R.layout.local_theme_pkg_layout;
    }

    @Override
    protected int getThemeType() {
        return Theme.WALLPAPER;
    }

    @Override
    protected LocalThemeListAdapter getAdapter() {
        return new LocalThemeWallpaperListAdapter(getContext());
    }

    @Override
    protected void handleItemClick(AdapterView parent, View view, int position, long id) {
        if(isEditMode()){
            super.handleItemClick(parent, view, position, id);
        }else{
        	List<Wallpaper> wallpapers = new ArrayList<>();
        	Wallpaper wallpaper;
            List<Theme> themes = mAdapter.getThemes();
            for (Theme theme : themes) {
            	wallpaper = new Wallpaper();
            	if(theme.themeFilePath != null && theme.themeFilePath.length() > 0) {
            		wallpaper.themeFilePath = theme.themeFilePath;
            	}else if(theme.downloadUrl != null && theme.downloadUrl.length() > 0) {
            		wallpaper.downloadUrl = theme.downloadUrl;
            	}
            	wallpapers.add(wallpaper);
			}
            
			Intent intent = new Intent(Config.Action.ACTION_WALLPAPER_DETAIL);
			Bundle bundle = new Bundle();

			WallpaperDetailPresenter.setWallpapers(wallpapers, position);
			
			intent.putExtra(Config.ActionKey.KEY_WALLPAPER_PREVIEW_BUNDLE, bundle);

			ImageView image = (ImageView)view.findViewById(R.id.theme_list_item_image);
			if(image.getDrawable() != null) {
				Config.sStaringImageInPreview  = image.getDrawable().getConstantState().newDrawable();
			}
			ActivityOptions ops = ActivityOptions.makeScaleUpAnimation(image, (int)image.getX() / 2,  (int)image.getY() / 2,image.getWidth(),image.getHeight());
			getContext().startActivity(intent, ops.toBundle());
        }
    }
}



