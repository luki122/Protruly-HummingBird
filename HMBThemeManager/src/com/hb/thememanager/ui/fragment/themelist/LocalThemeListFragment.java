package com.hb.thememanager.ui.fragment.themelist;

import hb.app.dialog.ProgressDialog;
import hb.view.menu.bottomnavigation.BottomNavigationView;
import hb.widget.toolbar.Toolbar;
import hb.widget.toolbar.Toolbar.OnMenuItemClickListener;

import java.util.ArrayList;
import java.util.List;
import android.widget.TextView;

import com.hb.imageloader.ImageLoaderConfig;
import com.hb.thememanager.ThemeManager;
import com.hb.thememanager.listener.OnThemeStateChangeListener;
import com.hb.thememanager.listener.PreInflateListener;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.security.MD5Utils;
import com.hb.thememanager.state.StateManager;
import com.hb.thememanager.state.ThemeState;
import com.hb.thememanager.ui.MainActivity;
import com.hb.thememanager.ui.ThemePickerActivity;
import com.hb.thememanager.ui.adapter.LocalThemeListAdapter;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.TLog;
import com.hb.thememanager.utils.ToastUtils;
import com.hb.thememanager.utils.Config.LoadThemeStatus;
import com.hb.thememanager.views.ThemePreviewDonwloadButton;
import com.hb.thememanager.ui.fragment.AbsThemeFragment;
import com.hb.thememanager.ui.fragment.DirectoryFragment;
import com.hb.thememanager.ui.fragment.themedetail.ThemePkgDetailFragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.Toast;

import com.hb.thememanager.R;
import com.hb.thememanager.ThemeManagerApplication;
public class LocalThemeListFragment<ThemeListPresenter> extends AbsThemeFragment implements ThemeListMVPView,
OnItemClickListener,OnMenuItemClickListener {
	private static final String TAG = "ThemeList";
	private static final int MSG_ADD_NEW_THEME = 0;
	private static final int MSG_GOTO_NEXT = 1;
	private static final int THEME_PICKER_REQUEST_CODE = 101;
	private static final String DEFAULT_THEME_SIZE = "3M";
	private GridView mListView;
	private LocalThemeListPresenter mThemeListPresenter;
	private LocalThemeListAdapter mAdapter;
	private ThemeManagerApplication mApp;
	private Toolbar mToolbar;
	private BottomNavigationView mBottomMenu;
	private StateManager mStateManager;
	private boolean mShowThemePickerAction = false;
	private TextView mSkipBtn;
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mApp = (ThemeManagerApplication) getActivity().getApplication();
		mShowThemePickerAction = getResources().getBoolean(R.bool.show_import_local_action);
		Bundle args = getArguments();

	}

	@Override
	protected void initialImageLoader(ImageLoaderConfig.Size size, ImageLoaderConfig config) {
		super.initialImageLoader(size, config);
		size.setHeight(size.getHeight() * 2);
		size.setWidth(size.getWidth() * 2);

	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		mThemeListPresenter = new LocalThemeListPresenter(getContext());
		mThemeListPresenter.attachView(this);
		View content = inflater.inflate(R.layout.local_theme_pkg_layout, container,false);
		mBottomMenu = (BottomNavigationView)content.findViewById(R.id.theme_list_action_menu);
		mListView = (GridView)content.findViewById(android.R.id.list);
		mAdapter = new LocalThemeListAdapter(getContext());
		mAdapter.setImageLoader(getImageLoader());
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);
		initView();
		mApp.loadInternalTheme();
		return content;
	}
	
	@Override
	public void showTips(int status) {
		// TODO Auto-generated method stub
		if(status == Config.LoadThemeStatus.STATUS_FAIL){
			ToastUtils.showShortToast(getContext(), getString(R.string.msg_load_theme_fail));
		}else if(status == LoadThemeStatus.STATUS_THEME_NOT_EXISTS){
			ToastUtils.showShortToast(getContext(), getString(R.string.msg_load_theme_exists));
		}else if(status == LoadThemeStatus.STATUS_THEME_FILE_ERROR){
			ToastUtils.showLongToast(getContext(), getString(R.string.msg_load_theme_error));
		}
	}
	
	@Override
	protected void handleEditMode(boolean editMode, int position) {
		mBottomMenu.setVisibility(editMode?View.VISIBLE:View.GONE);
		mAdapter.enterEditMode(editMode);
		if(editMode){
			mAdapter.selectOrUnSelectTheme(position,true);
		}
		mAdapter.notifyDataSetChanged();
	}
	
	
	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view,
			int position, long id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(mThemeListPresenter != null){
			mThemeListPresenter.onDestory();
		}

	}

	@Override
	public void updateThemeList(Theme theme) {
		// TODO Auto-generated method stub
		
		if(theme != null){
			if(theme.id == Config.DEFAULT_THEME_ID){
				theme.designer = getResources().getString(R.string.default_theme_designer);
				theme.name = getResources().getString(R.string.default_theme_name);
				theme.description = getResources().getString(R.string.default_theme_description);
				theme.size = DEFAULT_THEME_SIZE;
			}else if(theme.isSystemTheme()){
				theme.name = Config.getSystemThemeName(theme.themeFilePath,getActivity().getApplicationContext());
			}
			mAdapter.addTheme(theme);
		}
	}
	
	@Override
	public void updateThemeLists(List theme) {
		// TODO Auto-generated method stub
		
	}
	
	
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// TODO Auto-generated method stub
		if(isEditMode()){
			mAdapter.selectOrUnSelectTheme(position,!mAdapter.isSelected(position));
			mAdapter.notifyDataSetChanged();
		}else{
			Theme theme = mAdapter.getTheme(position);
			if(theme != null){
				Bundle args = new Bundle();
				args.putParcelable(Config.ActionKey.KEY_THEME_PKG_DETAIL, theme);
				startFragment(this, ThemePkgDetailFragment.class.getName(), true, theme.name, 0, args);

			}
		}
		
	}
	
	@Override
	protected void initView() {
		// TODO Auto-generated method stub
		mToolbar = ((MainActivity)getActivity()).getToolbar();
		mToolbar.setOnMenuItemClickListener(this);
		mToolbar.getMenu().clear();
		if(mShowThemePickerAction){
			mToolbar.inflateMenu(R.menu.local_theme_list_add);

		}
	}

	
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		// TODO Auto-generated method stub
		int id = item.getItemId();
		if(R.id.menu_add_local_theme == id){
			Intent intent = new Intent(Config.Action.ACTION_THEME_PICKER);
			startActivityForResult(intent, THEME_PICKER_REQUEST_CODE);
		}
		return false;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if(mShowThemePickerAction){
			if(requestCode == THEME_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK){
				String themeZipPath = data.getStringExtra(Config.KEY_PICK_THEME_FILE_PATH);
				if(TextUtils.isEmpty(themeZipPath)){
					return;
				}
				mThemeListPresenter.loadThemeIntoDatabase(themeZipPath, Theme.THEME_PKG);
			}
		}
		
	}




}
