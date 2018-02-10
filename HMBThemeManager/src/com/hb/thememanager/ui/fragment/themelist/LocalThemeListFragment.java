package com.hb.thememanager.ui.fragment.themelist;

import hb.app.HbActivity;
import hb.app.dialog.AlertDialog;
import hb.view.menu.bottomnavigation.BottomNavigationView;
import hb.widget.ActionMode;
import hb.widget.ActionModeListener;
import hb.widget.toolbar.Toolbar;
import hb.widget.toolbar.Toolbar.OnMenuItemClickListener;

import java.util.ArrayList;
import java.util.List;

import com.hb.thememanager.database.DatabaseFactory;
import com.hb.thememanager.database.ThemeDatabaseController;
import com.hb.thememanager.database.ThemeWallpaperDbController;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.ui.MainActivity;
import com.hb.thememanager.ui.ThemePickerActivity;
import com.hb.thememanager.ui.adapter.LocalThemeListAdapter;
import com.hb.thememanager.ui.fragment.themedetail.LocalThemeDetailFragment;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.ToastUtils;
import com.hb.thememanager.utils.Config.LoadThemeStatus;
import com.hb.thememanager.ui.fragment.AbsLocalThemeFragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import java.io.File;
import android.util.Log;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import com.hb.thememanager.ThemeManager;
import com.hb.thememanager.R;
import com.hb.thememanager.ThemeManagerApplication;
public abstract class LocalThemeListFragment<ThemeListPresenter> extends AbsLocalThemeFragment implements ThemeListMVPView,
OnItemClickListener,OnMenuItemClickListener,ActionModeListener,
		DialogInterface.OnClickListener,BottomNavigationView.OnNavigationItemSelectedListener{
	private static final String TAG = "ThemeList";
	private static final int MSG_ADD_NEW_THEME = 0;
	private static final int THEME_PICKER_REQUEST_CODE = 101;
	private static final String DEFAULT_THEME_SIZE = "3M";
	private GridView mListView;
	private LocalThemeListPresenter mThemeListPresenter;
	protected LocalThemeListAdapter mAdapter;
	private ThemeManagerApplication mApp;
	private Toolbar mToolbar;
	private BottomNavigationView mBottomMenu;
	private ActionMode mActionMode;
	private AlertDialog mConfirmDeleteDialog;
	public ThemeManager mThemeManager;
	private int mSystemWallpaperCount = -1;
	private int mAllWallpaperCount = -1;
	private int mCurrentLoadWallpaperNum = 0;
	private boolean mSelectAll = true;
	public boolean mShowThemePickerAction = false;
	private Handler mHandler = new Handler(){
	
		public void handleMessage(android.os.Message msg) {
			if(msg.what == MSG_ADD_NEW_THEME){
//				if(mSystemWallpaperCount != -1 && mCurrentLoadWallpaperNum >= mSystemWallpaperCount) {
//					loadUserWallpaper();
//				}else {
//					if(mCurrentLoadWallpaperNum == 0 && getThemeType() == Theme.WALLPAPER) {
//						ThemeWallpaperDbController db = (ThemeWallpaperDbController) DatabaseFactory.createDatabaseController(Theme.WALLPAPER, getContext());
//						List<Wallpaper> themes = db.getThemes();
//						List<Wallpaper> systemThemes = db.getSystemTheme();
//						mAllWallpaperCount = themes.size();
//						mSystemWallpaperCount = systemThemes.size();
//					}
					Theme t = (Theme) msg.obj;
					mAdapter.addTheme(t);
//					mCurrentLoadWallpaperNum++;
				}
//			}
		};
	};


	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mApp = (ThemeManagerApplication) getActivity().getApplication();
		mThemeManager = mApp.getThemeManager();
		mShowThemePickerAction = getResources().getBoolean(R.bool.show_import_local_action);
		mConfirmDeleteDialog = new AlertDialog.Builder(getContext())
				.setTitle(R.string.delete_theme_title)
				.setMessage(R.string.delete_theme_msg)
				.setPositiveButton(R.string.confirm_delete,this)
				.setNegativeButton(R.string.confirm_cancel,this)
				.create();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		mThemeListPresenter = new LocalThemeListPresenter(getContext());
		mThemeListPresenter.attachView(this);
		View content = inflater.inflate(getLayoutRes(), container,false);
		mBottomMenu = (BottomNavigationView)content.findViewById(R.id.theme_list_action_menu);
		mBottomMenu.setNavigationItemSelectedListener(this);
		mListView = (GridView)content.findViewById(android.R.id.list);
		mAdapter = getAdapter();
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);
		initView();
		mApp.loadInternalTheme(getThemeType());
		mActionMode = ((HbActivity)getActivity()).getActionMode();
		initialActionMode();
		return content;
	}

	@Override
	public int getLayoutRes() {
		return super.getLayoutRes();
	}

	protected void loadUserWallpaper(){};

	protected abstract int getThemeType();

	protected abstract LocalThemeListAdapter getAdapter();

	public void setDeleteDialogTitle(int res){
		mConfirmDeleteDialog.setTitle(res);
	}

	public void setDeleteDialogMsg(int msg){
		mConfirmDeleteDialog.setMessage(getResources().getString(msg));
	}


	@Override
	public void onClick(DialogInterface dialogInterface, int which) {
		if(which == DialogInterface.BUTTON_POSITIVE){
			deleteSelectTheme();
		}else if(which == DialogInterface.BUTTON_NEGATIVE){
			dialogInterface.dismiss();
			exitEditMode();
		}
	}

	protected void deleteSelectTheme(){
		SparseArray<Theme> selectThemes = mAdapter.getSelectedItems();
		ArrayList<Theme> deleteThemes = new ArrayList<Theme>();
		int[] keys = new int[selectThemes.size()];
		for(int i = 0;i< selectThemes.size();i++){
			int key = selectThemes.keyAt(i);
			keys[i] = key;
			deleteThemes.add(selectThemes.get(key));
		}
		mThemeManager.deleteTheme(deleteThemes);
		exitEditMode();
		mAdapter.deleteItems(keys);
	}


	private void initialActionMode(){
		mActionMode.setPositiveText(getString(R.string.select_all));
		mActionMode.setNagativeText(getString(R.string.cancel));
		mActionMode.setTitle(getString(R.string.select_number,0));
		mActionMode.bindActionModeListener(this);
	}


	@Override
	public void onActionItemClicked(ActionMode.Item item) {
		if(item.getItemId() == ActionMode.NAGATIVE_BUTTON){
			exitEditMode();
		}else if(item.getItemId() == ActionMode.POSITIVE_BUTTON){
			mAdapter.selectOrUnselectAll(mSelectAll);
			mActionMode.setPositiveText(mSelectAll?
					getString(R.string.unselect_all):
					getString(R.string.select_all));
			mActionMode.setTitle(getString(R.string.select_number,
					mAdapter.getSelectedItems().size()));
			mSelectAll = !mSelectAll;
		}
	}

	@Override
	public boolean onNavigationItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.menu_delete_local_theme){
			if(mAdapter.getSelectedItems().size() > 0) {
				mConfirmDeleteDialog.show();
			}else{
				ToastUtils.showShortToast(getContext(),R.string.tips_select_theme_to_delete);
			}
		}
		return false;
	}

	@Override
	public void onActionModeShow(ActionMode actionMode) {

	}

	@Override
	public void onActionModeDismiss(ActionMode actionMode) {

	}

	@Override
	public void showTips(int status) {
		if(!isAdded()){
			return;
		}
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
		((HbActivity)getActivity()).showActionMode(editMode);
		mBottomMenu.setVisibility(editMode?View.VISIBLE:View.GONE);
		mAdapter.enterEditMode(editMode);
		if(editMode){
			mAdapter.selectOrUnSelectTheme(position,true);
			mActionMode.setTitle(getString(R.string.select_number,mAdapter.getSelectedItems().size()));
		}else{
			mAdapter.selectOrUnselectAll(false);
		}
	}

	@Override
	protected void exitEditMode() {
		super.exitEditMode();
		mSelectAll = false;
		mActionMode.setPositiveText(mSelectAll?
				getString(R.string.unselect_all):
				getString(R.string.select_all));
	}

	@Override
	public void updateThemeList(Theme theme) {
		// TODO Auto-generated method stub
		if(!isAdded()){
			return;
		}
		if(theme != null){
			if(theme.isDefaultTheme()){
				theme.designer = getResources().getString(R.string.default_theme_designer);
				theme.name = getResources().getString(R.string.default_theme_name);
				theme.description = getResources().getString(R.string.default_theme_description);
				theme.size = DEFAULT_THEME_SIZE;
			}
			Message msg = new Message();
			msg.what = MSG_ADD_NEW_THEME;
			msg.obj = theme;
			mHandler.sendMessage(msg);
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
		handleItemClick(parent, view, position, id);
	}

	protected void handleItemClick(AdapterView<?> parent, View view, int position,
								   long id){
		Theme theme = mAdapter.getTheme(position);
		if(isEditMode()){
			mAdapter.selectOrUnSelectTheme(position,!mAdapter.isSelected(position));
			mActionMode.setTitle(getString(R.string.select_number,mAdapter.getSelectedItems().size()));
		}else{

			if(theme != null){
				Bundle args = new Bundle();
				args.putParcelable(Config.ActionKey.KEY_THEME_PKG_DETAIL, theme);
				startFragment(this, LocalThemeDetailFragment.class.getName(), true, theme.name, 0, args);
			}
		}
	}
	
	@Override
	protected void initView() {
		// TODO Auto-generated method stub
		mToolbar = ((MainActivity)getActivity()).getToolbar();
		if(mShowThemePickerAction){
			mToolbar.inflateMenu(R.menu.local_theme_list_add);
			mToolbar.setOnMenuItemClickListener(this);
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
			Intent intent = new Intent();
			intent.setClass(getActivity(), ThemePickerActivity.class);
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
				mThemeListPresenter.loadThemeIntoDatabase(themeZipPath, getThemeType());
			}
		}
		
	}




	@Override
	public void showEmptyView(boolean show) {

	}

	@Override
	public void showNetworkErrorView(boolean show) {

	}
	

}

