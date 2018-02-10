package com.hb.thememanager.ui.fragment.themelist;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import com.hb.imageloader.HbImageLoader;
import com.hb.imageloader.ImageLoaderConfig;
import com.hb.thememanager.R;
import com.hb.thememanager.ThemeManagerApplication;
import com.hb.thememanager.ThemeManagerImpl;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.ui.adapter.AbsBaseAdapter;
import com.hb.thememanager.ui.fragment.AbsThemeFragment;

import hb.app.HbActivity;
import hb.app.dialog.AlertDialog;
import hb.view.menu.bottomnavigation.BottomNavigationView;
import hb.widget.ActionMode;
import hb.widget.ActionModeListener;
import android.widget.Button;
public abstract class AbsLocalWallpaperListFrag extends AbsThemeFragment implements ThemeListMVPView
,DialogInterface.OnClickListener{

	
	
	protected View mContentView;
	private LocalWallpaperListPresenter mPresenter;
	private BottomNavigationView mBottomView;
	private View mActionBtn;
	private AlertDialog mDeleteConfirmDialog;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mPresenter = new LocalWallpaperListPresenter(getContext());
		mPresenter.attachView(this);
		mDeleteConfirmDialog = new AlertDialog.Builder(getContext())
				.setTitle(R.string.delete_wallpaper_title)
				.setMessage(R.string.delete_wallpaper_msg)
				.setPositiveButton(R.string.delete,this)
				.setNegativeButton(R.string.cancel,this)
				.create();
	}


	@Override
	protected void handleEditMode(boolean editMode, int position) {
		super.handleEditMode(editMode, position);
		mBottomView.setVisibility(editMode?View.VISIBLE:View.GONE);
		mActionBtn.setVisibility(editMode?View.GONE:View.VISIBLE);
		((HbActivity)getActivity()).showActionMode(editMode);
		if(getAdapter() != null){
			getAdapter().enterEditMode(editMode);
			if(!editMode){
				return;
			}
			Theme t = (Theme) getAdapter().getItem(position);
			if(editMode && ! t.isSystemTheme()){
				getAdapter().selectOrUnSelectTheme(position,true);
			}
			getAdapter().notifyDataSetChanged();

			updateActionModeTitle(getAdapter().getSelectedItems().size());
		}


	}

	protected abstract AbsBaseAdapter getAdapter();

	protected LocalWallpaperListPresenter getPresenter(){
		return mPresenter;
	}


	@Override
	public void onClick(DialogInterface dialog, int witch) {
			if(witch == DialogInterface.BUTTON_NEGATIVE){
				dialog.dismiss();
				exitEditMode();
			}else if(witch == DialogInterface.BUTTON_POSITIVE){
				deleteSelectedTheme();
			}
	}

	private void deleteSelectedTheme(){
		SparseArray<Theme> selectThemes = getAdapter().getSelectedItems();
		ArrayList<Theme> deleteThemes = new ArrayList<Theme>();
		int[] keys = new int[selectThemes.size()];
		for(int i = 0;i< selectThemes.size();i++){
			int key = selectThemes.keyAt(i);
			keys[i] = key;
			deleteThemes.add(selectThemes.get(key));
		}
		ThemeManagerImpl.getInstance(getContext()).deleteTheme(deleteThemes);
		getAdapter().deleteItems(keys);
		exitEditMode();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mContentView = inflater.inflate(R.layout.activity_local_desktop_wallpaper_list, container, false);
		mBottomView = (BottomNavigationView)mContentView.findViewById(R.id.theme_list_action_menu);
		mBottomView.setNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(MenuItem item) {
				if(item.getItemId() == R.id.menu_delete_local_theme){
					mDeleteConfirmDialog.show();
					Button negativeBtn = mDeleteConfirmDialog.getButton(DialogInterface.BUTTON_POSITIVE);
					if (negativeBtn != null) {
						negativeBtn.setBackgroundResource(com.hb.R.drawable.button_background_hb_delete);
					}
				}

				return false;
			}
		});
		mActionBtn = mContentView.findViewById(R.id.btn_add_wallpaper);
		initView();
		((HbActivity)getActivity()).getActionMode().bindActionModeListener(new ActionModeListener() {
			@Override
			public void onActionItemClicked(ActionMode.Item item) {
				if(item.getItemId() == ActionMode.NAGATIVE_BUTTON){
					exitEditMode();
				}else if(item.getItemId() == ActionMode.POSITIVE_BUTTON){
					mSelectAllState = !mSelectAllState;
					getAdapter().selectOrUnselectAll(mSelectAllState);
					((HbActivity)getActivity()).getActionMode().setPositiveText(mSelectAllState?
							getString(R.string.unselect_all):getString(R.string.select_all));
					updateActionModeTitle(getAdapter().getSelectedItems().size());
				}
			}

			@Override
			public void onActionModeShow(ActionMode actionMode) {

			}

			@Override
			public void onActionModeDismiss(ActionMode actionMode) {

			}
		});
		return mContentView;
	}
	
	protected View getContentView(){
		return mContentView;
	}

	protected void updateActionModeTitle(int count){
		((HbActivity)getActivity()).getActionMode().setTitle(getResources().getString(R.string.select_number,count));
	}
	
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(mPresenter != null){
			mPresenter.onDestory();
			mPresenter = null;
		}

	}
}
